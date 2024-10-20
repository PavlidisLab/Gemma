package ubic.gemma.core.analysis.preprocess.detect;

import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * Exception raised when an {@link ExpressionDataMatrix} does not meet the expectations set by a given
 * {@link ubic.gemma.model.common.quantitationtype.QuantitationType}.
 * <p>
 * There are two main kind of problems:
 * <ul>
 * <li>{@link InferredQuantitationMismatchException} when the quantitation type does not match the one inferred from data</li>
 * <li>{@link SuspiciousValuesForQuantitationException} when data looks suspicious given its quantitation type</li>
 * </ul>
 * @author poirigui
 */
public abstract class QuantitationMismatchException extends QuantitationTypeDetectionException {

    private final QuantitationType quantitationType;

    protected QuantitationMismatchException( QuantitationType qt, String message ) {
        super( message );
        this.quantitationType = qt;
    }

    /**
     * Quantitation type for the data.
     */
    public QuantitationType getQuantitationType() {
        return quantitationType;
    }
}
