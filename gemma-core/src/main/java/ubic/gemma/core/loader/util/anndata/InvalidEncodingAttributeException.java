package ubic.gemma.core.loader.util.anndata;

/**
 * Exception raised when an {@code encoding-type} attribute has an unexpected value.
 * @author poirigui
 * @see MissingEncodingAttributeException
 */
public class InvalidEncodingAttributeException extends AnnDataException {

    public InvalidEncodingAttributeException( String message ) {
        super( message );
    }
}
