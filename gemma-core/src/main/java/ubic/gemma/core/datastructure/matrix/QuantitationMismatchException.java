package ubic.gemma.core.datastructure.matrix;

/**
 * Exception raised when an {@link ExpressionDataDoubleMatrix} does not meet the expectations set by a given
 * {@link ubic.gemma.model.common.quantitationtype.QuantitationType}.
 * <p>
 * There are two main kind of problems:
 * <ul>
 * <li>{@link InferredQuantitationMismatchException} when the quantitation type does not match the one inferred from data (see )</li>
 * <li>{@link SuspiciousValuesForQuantitationException} when data looks suspicious</li>
 * </ul>
 * @author poirigui
 */
public abstract class QuantitationMismatchException extends Exception {
    protected QuantitationMismatchException( String message ) {
        super( message );
    }
}
