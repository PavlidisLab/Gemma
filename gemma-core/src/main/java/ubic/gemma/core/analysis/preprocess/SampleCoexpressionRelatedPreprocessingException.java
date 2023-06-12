package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

/**
 * Exception raised in preprocessing when there is a problem with sample coexpression analysis.
 * @see SampleCoexpressionAnalysisService
 */
public class SampleCoexpressionRelatedPreprocessingException extends PreprocessingException {

    public SampleCoexpressionRelatedPreprocessingException( ExpressionExperiment ee, Throwable cause ) {
        super( ee, cause );
    }
}
