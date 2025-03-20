package ubic.gemma.core.loader.util.anndata;

/**
 * Exception raised when a HDF5 file/group/dataset is lacking a {@code encoding-type} and/or {@code encoding-version}
 * attribute.
 * @author poirigui
 */
public class MissingEncodingAttributeException extends AnnDataException {

    public MissingEncodingAttributeException( String message ) {
        super( message );
    }
}
