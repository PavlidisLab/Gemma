package ubic.gemma.core.analysis.preprocess.svd;

import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class FailedToComputeSingularValueDecomposition extends PreprocessingException {

    public FailedToComputeSingularValueDecomposition( ExpressionExperiment ee, SVDException cause ) {
        super( ee, cause );
    }
}
