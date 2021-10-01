package ubic.gemma.persistence.service;

public class ObjectFilterException extends Exception {

    public ObjectFilterException( String message ) {
        super( message );
    }

    public ObjectFilterException( String message, Throwable cause ) {
        super( message, cause );
    }
}
