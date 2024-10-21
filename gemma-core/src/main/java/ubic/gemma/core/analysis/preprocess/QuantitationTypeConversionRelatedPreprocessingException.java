package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class QuantitationTypeConversionRelatedPreprocessingException extends PreprocessingException {

    private final QuantitationTypeConversionException cause;

    public QuantitationTypeConversionRelatedPreprocessingException( ExpressionExperiment ee, QuantitationTypeConversionException cause ) {
        super( ee, cause );
        this.cause = cause;
    }

    @Override
    public QuantitationTypeConversionException getCause() {
        return cause;
    }
}
