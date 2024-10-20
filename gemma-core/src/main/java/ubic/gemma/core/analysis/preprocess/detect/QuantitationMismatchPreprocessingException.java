package ubic.gemma.core.analysis.preprocess.detect;

import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * An exception that wraps a
 */
public class QuantitationMismatchPreprocessingException extends PreprocessingException {

    private final QuantitationMismatchException cause;

    public QuantitationMismatchPreprocessingException( ExpressionExperiment ee, QuantitationMismatchException cause ) {
        super( ee, cause );
        this.cause = cause;
    }

    @Override
    public QuantitationMismatchException getCause() {
        return cause;
    }
}
