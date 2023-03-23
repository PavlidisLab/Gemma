package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.core.datastructure.matrix.QuantitationMismatchException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class QuantitationMismatchPreprocessingException extends PreprocessingException {

    public QuantitationMismatchPreprocessingException( ExpressionExperiment ee, QuantitationMismatchException cause ) {
        super( ee, cause );
    }

    @Override
    public synchronized QuantitationMismatchException getCause() {
        return ( QuantitationMismatchException ) super.getCause();
    }
}
