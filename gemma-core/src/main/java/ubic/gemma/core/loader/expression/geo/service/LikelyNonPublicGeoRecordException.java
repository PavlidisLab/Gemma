package ubic.gemma.core.loader.expression.geo.service;

public class LikelyNonPublicGeoRecordException extends RuntimeException {
    public LikelyNonPublicGeoRecordException( Throwable cause ) {
        super( "The GEO record is likely not public.", cause );
    }
}
