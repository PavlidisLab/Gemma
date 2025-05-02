package ubic.gemma.core.loader.entrez;

public enum EntrezRetmode {
    XML( "xml" ),
    JSON( "json" );

    private final String value;

    EntrezRetmode( String value ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
