package ubic.gemma.core.ontology.jena;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

class RO {

    private static final String NS = "http://purl.obolibrary.org/obo/";

    private static Property property( String localName ) {
        return ResourceFactory.createProperty( NS + localName );
    }

    public static final Property partOf = property( "BFO_0000050" );
    public static final Property activeIngredientIn = property( "RO_0002249" );
    public static final Property boundingLayerOf = property( "RO_0002007" );
    public static final Property branchingPartOf = property( "RO_0002380" );
    public static final Property determinedBy = property( "RO_0002507" );
    public static final Property ends = property( "RO_0002229" );
    public static final Property isSubsequenceOf = property( "RO_0002525" );
    public static final Property isEndSequenceOf = property( "RO_0002519" );
    public static final Property isStartSequenceOf = property( "RO_0002517" );
    public static final Property lumenOf = property( "RO_0002571" );
    public static final Property luminalSpaceOf = property( "RO_0002572" );
    public static final Property mainStemOf = property( "RO_0002381" );
    public static final Property memberOf = property( "RO_0002350" );
    public static final Property occurrentPartOf = property( "RO_0002012" );
    public static final Property skeletonOf = property( "RO_0002576" );
    public static final Property starts = property( "RO_0002223" );
    public static final Property subclusterOf = property( "RO_0015003" );

    /**
     * RO:0000087 "has role" — links a chemical (or other material entity) to a role it bears.
     * CHEBI uses this to connect chemicals to their pharmacological roles ("drug",
     * "antineoplastic agent", "kinase inhibitor", etc.). Not in the default property set
     * because the chemistry-based {@code subClassOf} hierarchy doesn't capture roles —
     * estradiol is-a steroid is-a lipid in CHEBI, not estradiol is-a hormone. The role
     * relationship is what makes "give me all chemicals with the antineoplastic role"
     * answerable. Enabled per-ontology in {@code ChebiOntologyService}.
     */
    public static final Property hasRole = property( "RO_0000087" );

    /**
     * This term is still used in older ontologies.
     */
    @Deprecated
    public static final Property properPartOf = ResourceFactory.createProperty( "http://www.obofoundry.org/ro/ro.owl#proper_part_of" );
}
