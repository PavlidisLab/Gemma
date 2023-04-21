package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * Exception raised when the quantitation of an {@link ExpressionDataMatrix} does not agree with the one inferred.
 */
public class InferredQuantitationMismatchException extends QuantitationMismatchException {

    private final QuantitationType inferredQuantitationType;

    public InferredQuantitationMismatchException( QuantitationType qt, QuantitationType inferredQuantitationType, String message ) {
        super( qt, message );
        this.inferredQuantitationType = inferredQuantitationType;
    }

    public QuantitationType getInferredQuantitationType() {
        return inferredQuantitationType;
    }
}
