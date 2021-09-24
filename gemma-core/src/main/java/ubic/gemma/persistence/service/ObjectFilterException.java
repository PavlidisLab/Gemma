package ubic.gemma.persistence.service;

public class ObjectFilterException extends Exception {

    public ObjectFilterException( String message ) {
        super( message );
    }

    public ObjectFilterException( String message, IllegalArgumentException e ) {
        super( message, e );
    }
}
