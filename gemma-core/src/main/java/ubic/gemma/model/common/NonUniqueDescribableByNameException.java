package ubic.gemma.model.common;

/**
 * An exception that can be raised when trying to create a describable entity or add one to a collection with a name
 * that is already being used.
 *
 * @author poirigui
 */
public class NonUniqueDescribableByNameException extends IllegalArgumentException {

    public NonUniqueDescribableByNameException( String message ) {
        super( message );
    }

    public NonUniqueDescribableByNameException( String message, Throwable cause ) {
        super( message, cause );
    }
}
