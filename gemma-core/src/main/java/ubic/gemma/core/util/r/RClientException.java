package ubic.gemma.core.util.r;

public class RClientException extends RuntimeException {

    public RClientException( String message ) {
        super( message );
    }

    public RClientException( Throwable cause ) {
        super( cause );
    }
}
