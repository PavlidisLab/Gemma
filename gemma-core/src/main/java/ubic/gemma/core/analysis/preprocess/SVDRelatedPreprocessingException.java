package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.core.analysis.preprocess.svd.SVDException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Exception raised if a SVD cannot be computed.
 * @author poirigui
 * @see SVDException
 */
public class SVDRelatedPreprocessingException extends PreprocessingException {

    private final SVDException cause;

    public SVDRelatedPreprocessingException( ExpressionExperiment ee, SVDException cause ) {
        super( ee, cause );
        this.cause = cause;
    }

    @Override
    public synchronized SVDException getCause() {
        return cause;
    }
}
