/*
 * The baseCode project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import ubic.gemma.core.ontology.basecode.model.AnnotationProperty;
import ubic.gemma.core.ontology.basecode.model.OntologyIndividual;
import ubic.gemma.core.ontology.basecode.model.OntologyRestriction;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a class in an ontology
 *
 * @author Paul
 */
class OntologyTermImpl extends AbstractOntologyResource implements OntologyTerm {

    private static final String HAS_ALTERNATE_ID = "http://www.geneontology.org/formats/oboInOwl#hasAlternativeId";

    /**
     * Ontology class underlying this term.
     */
    private final OntClass ontResource;

    /**
     * Extra sets of properties to use when navigating parents and children of a term.
     */
    private final Set<Restriction> additionalRestrictions;

    public OntologyTermImpl( OntClass resource, Set<Restriction> additionalRestrictions ) {
        super( resource );
        this.ontResource = resource;
        this.additionalRestrictions = additionalRestrictions;
    }

    @Override
    public Collection<String> getAlternativeIds() {
        return getAnnotations( HAS_ALTERNATE_ID ).stream().map( AnnotationProperty::getContents ).collect( Collectors.toSet() );
    }

    @Override
    public Collection<AnnotationProperty> getAnnotations() {
        Collection<AnnotationProperty> annots = new HashSet<>();
        StmtIterator iterator = ontResource.listProperties();
        // this is a little slow because we have to go through all statements for the term.
        while ( iterator.hasNext() ) {
            Statement state = iterator.next();
            JenaUtils.as( state.getPredicate(), org.apache.jena.ontology.AnnotationProperty.class )
                .map( r -> new AnnotationPropertyImpl( r, state.getObject() ) )
                .ifPresent( annots::add );
        }
        return annots;
    }

    @Override
    public Collection<AnnotationProperty> getAnnotations( String propertyUri ) {
        Collection<AnnotationProperty> annots = new HashSet<>();
        Property alternate = ResourceFactory.createProperty( propertyUri );
        StmtIterator it = this.ontResource.listProperties( alternate );
        while ( it.hasNext() ) {
            Statement state = it.next();
            JenaUtils.as( state.getPredicate(), org.apache.jena.ontology.AnnotationProperty.class )
                .map( r -> new AnnotationPropertyImpl( r, state.getObject() ) )
                .ifPresent( annots::add );
        }
        return annots;
    }

    @Nullable
    @Override
    public AnnotationProperty getAnnotation( String propertyUri ) {
        Statement state = ontResource.getProperty( ResourceFactory.createProperty( propertyUri ) );
        if ( state != null ) {
            return JenaUtils.as( state.getPredicate(), org.apache.jena.ontology.AnnotationProperty.class )
                .map( r -> new AnnotationPropertyImpl( r, state.getObject() ) )
                .orElse( null );
        }
        return null;
    }

    @Override
    public Collection<OntologyTerm> getChildren( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return JenaUtils.getChildren( ontResource.getOntModel(), Collections.singleton( ontResource ), direct, includeAdditionalProperties ? additionalRestrictions : null )
            .stream()
            .map( o -> new OntologyTermImpl( o, additionalRestrictions ) )
            .filter( o -> keepObsoletes || !o.isObsolete() )
            .collect( Collectors.toSet() );
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.ontology.OntologyTerm#getComment()
     */
    @Override
    public String getComment() {
        String comment = this.ontResource.getComment( null );
        return comment == null ? "" : comment;
    }

    @Override
    public Collection<OntologyIndividual> getIndividuals( boolean direct ) {
        return this.ontResource.listInstances( direct )
            .filterKeep( new PredicateFilter<>( OntResource::isIndividual ) )
            .mapWith( r -> ( OntologyIndividual ) new OntologyIndividualImpl( r.asIndividual(), additionalRestrictions ) )
            .toSet();
    }

    @Override
    public Collection<OntologyTerm> getParents( boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return JenaUtils.getParents( ontResource.getOntModel(), Collections.singleton( ontResource ), direct, includeAdditionalProperties ? additionalRestrictions : null )
            .stream()
            .map( o -> new OntologyTermImpl( o, additionalRestrictions ) )
            .filter( o -> keepObsoletes || !o.isObsolete() )
            .collect( Collectors.toSet() );
    }

    /**
     *
     */
    @Override
    public Collection<OntologyRestriction> getRestrictions() {
        /*
         * Remember that restrictions are superclasses.
         */
        Collection<OntologyRestriction> result = new HashSet<>();
        ExtendedIterator<OntClass> iterator = ontResource.listSuperClasses( false );
        while ( iterator.hasNext() ) {
            OntClass c = iterator.next();
            if ( c.isRestriction() ) {
                result.add( RestrictionFactory.asRestriction( c.asRestriction(), additionalRestrictions ) );
            }
        }

        // Check superclasses for any ADDITIONAL restrictions.
        iterator = ontResource.listSuperClasses( false );
        while ( iterator.hasNext() ) {
            OntClass c = iterator.next();

            try {
                c.asRestriction(); // throw it away, we already processed it above.
            } catch ( Exception e ) {
                // not a restriction, but a superclass that might have restrictions
                ExtendedIterator<OntClass> supClassesIt = c.listSuperClasses( false );
                loop:
                while ( supClassesIt.hasNext() ) {
                    OntClass sc = supClassesIt.next();
                    Restriction sr;
                    try {
                        sr = sc.asRestriction();

                        // only add it if the class doesn't already have one.
                        OntologyRestriction candidateRestriction = RestrictionFactory.asRestriction( sr, additionalRestrictions );
                        for ( OntologyRestriction restr : result ) {
                            if ( restr.getRestrictionOn().equals( candidateRestriction.getRestrictionOn() ) )
                                continue loop;
                        }
                        result.add( candidateRestriction );

                    } catch ( Exception ex ) {
                        // superclass isn't a restriction.
                    }
                }
            }

        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.analysis.ontology.OntologyTerm#getTerm()
     */
    @Override
    public String getTerm() {
        String res = getLabel();
        if ( res == null ) {
            res = ontResource.toString();
        }
        return res;
    }

    @Override
    public boolean isObsolete() {
        return super.isObsolete() || ontResource.hasSuperClass( OBO.ObsoleteClass );
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.analysis.ontology.OntologyTerm#isRoot()
     */
    @Override
    public boolean isRoot() {
        return getParents( true, true, true ).isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.ontology.basecode.model.OntologyTerm#isTermObsolete()
     */
    @Override
    public boolean isTermObsolete() {
        return isObsolete();
    }

    public OntClass getOntClass() {
        return ontResource;
    }
}
