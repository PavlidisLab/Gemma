package ubic.gemma.core.analysis.preprocess.filter;

/**
 * Exception raised when no design elements (rows) are left after filtering the expression data matrix.
 * @author poirigui
 */
public class NoDesignElementsException extends InsufficientDesignElementsException {

    public NoDesignElementsException( String message ) {
        super( message );
    }
}
