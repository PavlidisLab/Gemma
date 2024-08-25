package ubic.gemma.core.loader.expression.geo.service;

public enum GeoSearchField {
    ACCESSION( "acc" ),
    ORGANISM( "ORGN" );

    private final String field;

    GeoSearchField( String field ) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
