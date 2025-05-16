package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class FilteringRelatedPreprocessingException extends PreprocessingException {

    private final FilteringException cause;

    public FilteringRelatedPreprocessingException( ExpressionExperiment ee, FilteringException cause ) {
        super( ee, cause );
        this.cause = cause;
    }

    @Override
    public FilteringException getCause() {
        return cause;
    }
}
