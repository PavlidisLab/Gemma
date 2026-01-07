package ubic.gemma.web.controller.util;

/**
 * Exception raised when a service is unavailable.
 * <p>
 * This is translated to a 503 error by the exception resolver.
 * @author poirigui
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException( String message ) {
        super( message );
    }

    public ServiceUnavailableException( String message, Throwable cause ) {
        super( message, cause );
    }
}
