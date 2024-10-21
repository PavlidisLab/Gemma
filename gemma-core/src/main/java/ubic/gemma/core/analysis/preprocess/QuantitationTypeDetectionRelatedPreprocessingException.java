package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * An exception that wraps a {@link QuantitationTypeDetectionException}.
 * @author poirigui
 */
public class QuantitationTypeDetectionRelatedPreprocessingException extends PreprocessingException {

    private final QuantitationTypeDetectionException cause;

    public QuantitationTypeDetectionRelatedPreprocessingException( ExpressionExperiment ee, QuantitationTypeDetectionException cause ) {
        super( ee, cause );
        this.cause = cause;
    }

    @Override
    public QuantitationTypeDetectionException getCause() {
        return cause;
    }
}
