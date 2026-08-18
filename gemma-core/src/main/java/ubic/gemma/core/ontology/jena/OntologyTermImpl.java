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
package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyIndividual;
import ubic.gemma.core.ontology.model.OntologyRestriction;
import ubic.gemma.core.ontology.model.OntologyTerm;

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
        String comment = JenaUtils.getFirstLiteral( this.ontResource, RDFS.comment );
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

    /** {@code RO:0002162 in_taxon} — the OBO relation MONDO uses to declare a species constraint. */
    private static final String IN_TAXON_URI = "http://purl.obolibrary.org/obo/RO_0002162";

    /** {@code http://purl.obolibrary.org/obo/NCBITaxon_9940} → 9940. */
    private static final java.util.regex.Pattern NCBI_TAXON_URI =
            java.util.regex.Pattern.compile( ".*/NCBITaxon_(\\d+)$" );

    /**
     * One pass over the DIRECT superclasses, testing {@code isRestriction()} rather than catching an
     * exception from {@code asRestriction()}, and stopping at the first {@code in_taxon}.
     * <p>
     * {@code in_taxon} is asserted directly on the term in MONDO, so there is nothing to gain by
     * walking to the grandparents the way {@link #getRestrictions()} does — and a great deal to lose:
     * that method's second pass throws and catches an exception for every superclass that is not a
     * restriction, which is most of them. This runs per search hit during top-N enrichment, so it
     * has to cost roughly nothing.
     */
    @Nullable
    @Override
    public TaxonConstraint getTaxonConstraint() {
        ExtendedIterator<OntClass> it = ontResource.listSuperClasses( true );
        try {
            while ( it.hasNext() ) {
                OntClass c = it.next();
                if ( !c.isRestriction() ) {
                    continue;
                }
                Restriction r = c.asRestriction();
                OntProperty on = r.getOnProperty();
                if ( on == null || !IN_TAXON_URI.equals( on.getURI() ) || !r.isSomeValuesFromRestriction() ) {
                    continue;
                }
                Resource filler = r.asSomeValuesFromRestriction().getSomeValuesFrom();
                if ( filler == null || filler.getURI() == null ) {
                    continue;
                }
                String uri = filler.getURI();
                Integer id = null;
                java.util.regex.Matcher m = NCBI_TAXON_URI.matcher( uri );
                if ( m.matches() ) {
                    try {
                        id = Integer.valueOf( m.group( 1 ) );
                    } catch ( NumberFormatException ignored ) {
                        // an NCBITaxon id too large for an int is not a thing; leave it null
                    }
                }
                // Null whenever NCBITaxon is not loaded and the referencing ontology declared no
                // label for the class. The id carries the meaning; the label is decoration.
                String label = filler.canAs( OntClass.class )
                        ? filler.as( OntClass.class ).getLabel( null )
                        : null;
                return new TaxonConstraint( uri, id, label );
            }
        } finally {
            it.close();
        }
        return null;
    }

    /**
     * One pass over the DIRECT superclasses. See {@link OntologyTerm#getDirectRestrictions()} for why
     * the closure walk is both unnecessary and unstable here.
     */
    @Override
    public Collection<OntologyRestriction> getDirectRestrictions() {
        Collection<OntologyRestriction> result = new HashSet<>();
        ExtendedIterator<OntClass> it = ontResource.listSuperClasses( true );
        try {
            while ( it.hasNext() ) {
                OntClass c = it.next();
                // isRestriction() rather than catching what asRestriction() throws: most superclasses
                // are not restrictions, and an exception per miss is what makes the other method cost
                // what it costs
                if ( c.isRestriction() ) {
                    result.add( RestrictionFactory.asRestriction( c.asRestriction(), additionalRestrictions ) );
                }
            }
        } finally {
            it.close();
        }
        return result;
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
     * @see ubic.gemma.core.ontology.model.OntologyTerm#isTermObsolete()
     */
    @Override
    public boolean isTermObsolete() {
        return isObsolete();
    }

    public OntClass getOntClass() {
        return ontResource;
    }
}
