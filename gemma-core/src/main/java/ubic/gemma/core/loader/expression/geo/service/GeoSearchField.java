package ubic.gemma.core.loader.expression.geo.service;

/**
 * Enumeration of possible fields for searching GEO records.
 * <p>
 * See <a href="https://www.ncbi.nlm.nih.gov/geo/info/qqtutorial.html">Querying GEO DataSets and GEO Profiles</a> which
 * has a table of all available fields.
 * @author poirigui
 */
public enum GeoSearchField {

    /**
     * This is the default if omitted.
     */
    ALL( "ALL" ),
    AUTHOR( "AUTH" ),
    DATASET_TYPE( "GTYP" ),
    DESCRIPTION( "DESC" ),
    ENTRY_TYPE( "ETYP" ),
    FILTER( "FILT" ),
    GEO_ACCESSION( "ACCN" ),
    MESH_TERMS( "MESH" ),
    NUMBER_OF_PLATFORM_PROBES( "NPRO" ),
    NUMBER_OF_SAMPLES( "NSAM" ),
    ORGANISM( "ORGN" ),
    PLATFORM_TECHNOLOGY_TYPE( "PTYP" ),
    PROJECT( "PROJ" ),
    PUBLICATION_DATE( "PDAT" ),
    RELATED_PLATFORMS( "RGPL" ),
    RELATED_SERIES( "RGSE" ),
    REPORTER_IDENTIFIER( "D" ),
    SAMPLE_SOURCE( "SRC" ),
    SAMPLE_TYPE( "STYP" ),
    SAMPLE_VALUE_TYPE( "VTYP" ),
    SUBMITTER_INSTUTUTE( "INST" ),
    SUBSET_DESCRIPTION( "SSDE" ),
    SUBSET_VARIABLE_TYPE( "SSTP" ),
    SUPPLEMENTARY_FILES( "SFIL" ),
    TAG_LENGTH( "TAGL" ),
    TITLE( "TITL" ),
    UPDATE_DATE( "UDAT" );

    private final String alias;

    GeoSearchField( String alias ) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public String toString() {
        return alias;
    }
}
