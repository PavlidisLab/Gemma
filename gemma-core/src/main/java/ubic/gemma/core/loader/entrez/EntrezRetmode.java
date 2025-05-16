package ubic.gemma.core.loader.entrez;

/**
 * Possible values for the Entrez {@code retmode} parameter.
 * @author poirigui
 */
public enum EntrezRetmode {
    XML( "xml" ),
    JSON( "json" ),
    TEXT( "text" );

    private final String value;

    EntrezRetmode( String value ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
