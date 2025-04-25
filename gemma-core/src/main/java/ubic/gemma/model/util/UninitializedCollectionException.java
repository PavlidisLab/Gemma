package ubic.gemma.model.util;

/**
 * Exception raised when an operation is performed on an intentionally uninitialized collection.
 * @author poirigui
 */
public class UninitializedCollectionException extends RuntimeException {

    public UninitializedCollectionException( String message ) {
        super( message );
    }
}
