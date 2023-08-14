package ubic.gemma.core.loader.expression.geo.service;

public class LikelyNonPublicGeoRecordException extends GeoException {
    public LikelyNonPublicGeoRecordException( String geoAccession, Throwable cause ) {
        super( geoAccession, "The GEO record is likely not public.", cause );
    }
}
