/**
 * 
 */
package ubic.gemma.ontology;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.ObjectUtils;

import ubic.gemma.model.common.description.ExternalDatabase;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Represents a class in an ontology
 * 
 * @author Paul
 * @version $Id$
 */
public class OntologyTermImpl extends AbstractOntologyResource implements OntologyTerm {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String NOTHING = "http://www.w3.org/2002/07/owl#Nothing";
    private static final String HAS_ALTERNATE_ID = "http://www.geneontology.org/formats/oboInOwl#hasAlternativeId";
    OntClass ontResource = null;
    String label = null;

    String localName = null;

    public OntologyTermImpl( OntClass resource, ExternalDatabase source ) {
        this.ontResource = resource;
        this.sourceOntology = source;
        this.label = ontResource.getLabel( null );
        this.localName = ontResource.getLocalName();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;

        if ( !super.equals( obj ) ) return false;
        if ( getClass() != obj.getClass() ) return false;

        final OntologyTermImpl that = ( OntologyTermImpl ) obj;
        if ( ontResource == null ) {
            log.warn( "ontResource is null in equals()" );
        }
        if ( that.ontResource == null ) {
            log.warn( "argument ontResource is null in equals()" );
        }

        if ( this.getUri() != null ) {
            return ObjectUtils.equals( this.getUri(), that.getUri() );
        } else {
            return ObjectUtils.equals( this.getTerm(), that.getTerm() );
        }

    }

    public Collection<String> getAlternativeIds() {
        Collection<String> results = new HashSet<String>();

        Property alternate = ResourceFactory.createProperty( HAS_ALTERNATE_ID );
        for ( StmtIterator it = this.ontResource.listProperties( alternate ); it.hasNext(); ) {
            Statement statement = it.next();
            results.add( statement.asTriple().getMatchObject().getLiteralLexicalForm() );
        }

        return results;

    }

    public Collection<AnnotationProperty> getAnnotations() {
        Collection<AnnotationProperty> annots = new HashSet<AnnotationProperty>();
        StmtIterator iterator = ontResource.listProperties();
        // this is a little slow because we have to go through all statements for the term.
        while ( iterator.hasNext() ) {
            Statement state = iterator.next();
            OntResource res = state.getPredicate().as( OntResource.class );
            if ( res.isAnnotationProperty() ) {
                com.hp.hpl.jena.ontology.AnnotationProperty p = res.asAnnotationProperty();
                RDFNode n = state.getObject();
                annots.add( new AnnotationPropertyImpl( p, this.sourceOntology, n ) );
            }
        }
        return annots;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.analysis.ontology.OntologyTerm#getChildren(boolean)
     */
    public Collection<OntologyTerm> getChildren( boolean direct ) {
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        ExtendedIterator<OntClass> iterator = ontResource.listSubClasses( direct );
        while ( iterator.hasNext() ) {
            OntClass c = iterator.next();
            // some reasoners will infer owl#Nothing as a subclass of everything
            if ( c.getURI().equals( NOTHING ) ) continue;
            result.add( this.fromOntClass( c ) );
            // log.info( c );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.ontology.OntologyTerm#getComment()
     */
    public String getComment() {
        String comment = this.ontResource.getComment( null );
        return comment == null ? "" : comment;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.ontology.OntologyTerm#getIndividuals()
     */
    public Collection<OntologyIndividual> getIndividuals() {
        return getIndividuals( true );
    }

    /**
     * @param direct
     * @return
     */
    public Collection<OntologyIndividual> getIndividuals( boolean direct ) {
        Collection<OntologyIndividual> inds = new HashSet<OntologyIndividual>();
        ExtendedIterator<? extends OntResource> iterator = this.ontResource.listInstances( direct );
        while ( iterator.hasNext() ) {
            Individual i = ( Individual ) iterator.next();
            inds.add( new OntologyIndividualImpl( i, this.sourceOntology ) );
        }
        return inds;
    }

    public String getLabel() {
        return label;
    }

    public Object getModel() {
        return ontResource.getModel();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.analysis.ontology.OntologyTerm#getParents(boolean)
     */
    public Collection<OntologyTerm> getParents( boolean direct ) {
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        ExtendedIterator<OntClass> iterator = ontResource.listSuperClasses( direct );
        while ( iterator.hasNext() ) {
            OntClass c = iterator.next();
            try {
                c.asRestriction();
                continue;
            } catch ( Exception e ) {
                // not a restriction.
                result.add( this.fromOntClass( c ) );
            }

        }
        return result;
    }

    /**
     * 
     */
    public Collection<OntologyRestriction> getRestrictions() {
        /*
         * Remember that restrictions are superclasses.
         */
        Collection<OntologyRestriction> result = new HashSet<OntologyRestriction>();
        ExtendedIterator<OntClass> iterator = ontResource.listSuperClasses( false );
        while ( iterator.hasNext() ) {
            OntClass c = iterator.next();
            Restriction r = null;
            try {
                r = c.asRestriction();
                result.add( RestrictionFactory.asRestriction( r, sourceOntology ) );
            } catch ( Exception e ) {

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
                loop: while ( supClassesIt.hasNext() ) {
                    OntClass sc = supClassesIt.next();
                    Restriction sr = null;
                    try {
                        sr = sc.asRestriction();

                        // only add it if the class doesn't already have one.
                        OntologyRestriction candidateRestriction = RestrictionFactory
                                .asRestriction( sr, sourceOntology );
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
     * @see ubic.gemma.analysis.ontology.OntologyTerm#getTerm()
     */
    public String getTerm() {
        String res = null;
        if ( this.label != null ) {
            res = this.label;
        } else if ( this.localName != null ) {
            res = localName;
        } else if ( this.getUri() != null ) {
            res = this.getUri();
        } else {
            res = ontResource.toString();
        }
        return res;
    }

    @Override
    public String getUri() {
        return this.ontResource.getURI();
    }

    @Override
    public int hashCode() {
        if ( ontResource == null ) {
            log.warn( "ontResource is null in hashCode()" );
            return 0;
        }
        // assert this.getUri() != null : "No URI for " + this.getTerm();
        if ( this.getUri() != null ) {
            return this.getUri().hashCode();
        } else {
            return this.getTerm().hashCode();
        }
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.analysis.ontology.OntologyTerm#isRoot()
     */
    public boolean isRoot() {
        return !this.ontResource.listSuperClasses( true ).hasNext();
    }

    @Override
    public String toString() {
        String res = null;
        if ( this.getTerm() != null ) {
            res = this.getTerm();
            if ( this.localName != null && !this.getTerm().equals( this.localName ) ) {
                res = res + " [" + this.localName + "]";
            }
        } else if ( this.localName != null ) {
            res = localName;
        } else if ( this.getUri() != null ) {
            res = this.getUri();
        } else {
            res = ontResource.toString();
        }
        return res;
    }

    protected OntologyTerm fromOntClass( OntClass ontClass ) {
        return new OntologyTermImpl( ontClass, this.sourceOntology );
    }

}
