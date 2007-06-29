/**
 * 
 */
package ubic.gemma.ontology;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.description.ExternalDatabase;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.RDFNode;
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

    private static final String NOTHING = "http://www.w3.org/2002/07/owl#Nothing";
    OntClass ontResource = null;
    String label = null;

    String localName = null;

    public Object getModel() {
        return ontResource.getModel();
    }

    public String getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        if ( ontResource == null ) return 0;
        return ontResource.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !super.equals( obj ) ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final OntologyTermImpl other = ( OntologyTermImpl ) obj;
        if ( ontResource == null ) {
            if ( other.ontResource != null ) return false;
        } else if ( !ontResource.equals( other.ontResource ) ) return false;
        return true;
    }

    public OntologyTermImpl( OntClass resource, ExternalDatabase source ) {
        this.ontResource = resource;
        this.sourceOntology = source;
        this.label = ontResource.getLabel( null );
        this.localName = ontResource.getLocalName();
    }

    public Collection<AnnotationProperty> getAnnotations() {
        Collection<AnnotationProperty> annots = new HashSet<AnnotationProperty>();
        StmtIterator iterator = ontResource.listProperties();
        // this is a little slow because we have to go through all statements for the term.
        while ( iterator.hasNext() ) {
            Statement state = ( Statement ) iterator.next();
            OntResource res = ( OntResource ) state.getPredicate().as( OntResource.class );
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
     * 
     * @see ubic.gemma.analysis.ontology.OntologyTerm#getChildren(boolean)
     */
    public Collection<OntologyTerm> getChildren( boolean direct ) {
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        ExtendedIterator iterator = ontResource.listSubClasses( direct );
        while ( iterator.hasNext() ) {
            OntClass c = ( OntClass ) iterator.next();
            // some reasoners will infer owl#Nothing as a subclass of everything
            if ( c.getURI().equals( NOTHING ) ) continue;
            result.add( this.fromOntClass( c ) );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyTerm#getComment()
     */
    public String getComment() {
        String comment = this.ontResource.getComment( null );
        return comment == null ? "" : comment;
    }

    /**
     * @param direct
     * @return
     */
    public Collection<OntologyIndividual> getIndividuals( boolean direct ) {
        Collection<OntologyIndividual> inds = new HashSet<OntologyIndividual>();
        ExtendedIterator iterator = this.ontResource.listInstances( direct );
        while ( iterator.hasNext() ) {
            Individual i = ( Individual ) iterator.next();
            inds.add( new OntologyIndividualImpl( i, this.sourceOntology ) );
        }
        return inds;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.OntologyTerm#getIndividuals()
     */
    public Collection<OntologyIndividual> getIndividuals() {
        return getIndividuals( true );
    }

    public Collection<OntologyRestriction> getRestrictions() {
        Collection<OntologyRestriction> result = new HashSet<OntologyRestriction>();
        ExtendedIterator iterator = ontResource.listSuperClasses( false );
        while ( iterator.hasNext() ) {
            OntClass c = ( OntClass ) iterator.next();
            Restriction r = null;
            try {
                r = c.asRestriction();
                result.add( RestrictionFactory.asRestriction( r, sourceOntology ) );
            } catch ( Exception e ) {
                // not a restriction, but a superclass that might have restrictions
                ExtendedIterator supClassesIt = c.listSuperClasses( true );
                while ( supClassesIt.hasNext() ) {
                    OntClass sc = ( OntClass ) supClassesIt.next();
                    Restriction sr = null;
                    try {
                        sr = sc.asRestriction();
                        result.add( RestrictionFactory.asRestriction( sr, sourceOntology ) );
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
     * @see ubic.gemma.analysis.ontology.OntologyTerm#getParents(boolean)
     */
    public Collection<OntologyTerm> getParents( boolean direct ) {
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        ExtendedIterator iterator = ontResource.listSuperClasses( direct );
        while ( iterator.hasNext() ) {
            OntClass c = ( OntClass ) iterator.next();
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

    /*
     * (non-Javadoc)
     * 
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

    /*
     * (non-Javadoc)
     * 
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

    @Override
    public String getUri() {
        return this.ontResource.getURI();
    }
}
