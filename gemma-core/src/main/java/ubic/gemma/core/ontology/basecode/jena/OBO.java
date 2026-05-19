/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

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
    public static final Property hasSynonym = property( "hasSynonym" );
    public static final Property hasExactSynonym = property( "hasExactSynonym" );
    public static final Property hasBroadSynonym = property( "hasBroadSynonm" );
    public static final Property hasNarrowSynonym = property( "hasNarrowSynonym" );
    public static final Property hasRelatedSynonym = property( "hasRelatedSynonym" );
    public static final Resource ObsoleteClass = ResourceFactory.createResource( "http://www.geneontology.org/formats/oboInOwl#ObsoleteClass" );
    public static final Property ObsoleteProperty = property( "ObsoleteProperty" );
}
