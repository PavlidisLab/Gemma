package ubic.gemma.core.ontology.jena;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * The SKOS mapping predicates, which are the second way an OBO ontology states a cross-reference —
 * resource-valued and qualified by the predicate itself, rather than a string qualified by an axiom
 * annotation.
 */
class SKOS {

    private static final String NS = "http://www.w3.org/2004/02/skos/core#";

    private static Property property( String name ) {
        return ResourceFactory.createProperty( NS + name );
    }

    public static final Property exactMatch = property( "exactMatch" );
    public static final Property closeMatch = property( "closeMatch" );
    public static final Property narrowMatch = property( "narrowMatch" );
    public static final Property broadMatch = property( "broadMatch" );
    public static final Property relatedMatch = property( "relatedMatch" );
}
