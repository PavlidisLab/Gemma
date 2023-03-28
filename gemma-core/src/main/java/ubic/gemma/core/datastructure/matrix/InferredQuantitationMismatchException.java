package ubic.gemma.core.datastructure.matrix;

/**
 * Exception raised when inferred quantitation mismatch from {@link ExpressionDataDoubleMatrix}'s quantitation type.
 */
public class InferredQuantitationMismatchException extends QuantitationMismatchException {
    public InferredQuantitationMismatchException( String message ) {
        super( message );
    }
}
