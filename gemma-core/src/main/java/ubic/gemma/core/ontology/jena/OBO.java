package ubic.gemma.core.ontology.jena;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

class OBO {

    private static final String NS = "http://www.geneontology.org/formats/oboInOwl" + "#";

    private static Property property( String name ) {
        return ResourceFactory.createProperty( NS + name );
    }

    public static final Property id = property( "id" );
    public static final Property hasDbXref = property( "hasDbXref" );
    /**
     * Qualifies an assertion, most usefully the {@code owl:Axiom} reifying a {@link #hasDbXref} — that
     * is where MONDO records whether a cross-reference is an equivalence or something looser.
     */
    public static final Property source = property( "source" );
    public static final Property hasSynonym = property( "hasSynonym" );
    public static final Property hasExactSynonym = property( "hasExactSynonym" );
    public static final Property hasBroadSynonym = property( "hasBroadSynonm" );
    public static final Property hasNarrowSynonym = property( "hasNarrowSynonym" );
    public static final Property hasRelatedSynonym = property( "hasRelatedSynonym" );
    public static final Resource ObsoleteClass = ResourceFactory.createResource( "http://www.geneontology.org/formats/oboInOwl#ObsoleteClass" );
    public static final Property ObsoleteProperty = property( "ObsoleteProperty" );
}
