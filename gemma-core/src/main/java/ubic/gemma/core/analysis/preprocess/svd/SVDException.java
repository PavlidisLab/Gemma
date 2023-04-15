package ubic.gemma.core.analysis.preprocess.svd;

/**
 * Exception raised when the SVD of a given expression data matrix cannot be computed.
 * @author poirigui
 */
public class SVDException extends Exception {
    public SVDException( String message ) {
        super( message );
    }
}
