package ubic.gemma.core.loader.expression.geo.service;

public abstract class GeoException extends RuntimeException {

    private final String geoAccession;

    protected GeoException( String geoAccession, String message, Throwable cause ) {
        super( String.format( "%s: %s", geoAccession, message ), cause );
        this.geoAccession = geoAccession;
    }

    public String getGeoAccession() {
        return geoAccession;
    }
}
