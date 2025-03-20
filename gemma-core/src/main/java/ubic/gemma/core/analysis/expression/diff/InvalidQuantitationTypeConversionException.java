package ubic.gemma.core.analysis.expression.diff;

import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;

/**
 * Exception raised when a log2 conversion fails.
 */
public class InvalidQuantitationTypeConversionException extends AnalysisException {

    public InvalidQuantitationTypeConversionException( QuantitationTypeConversionException e, DifferentialExpressionAnalysisConfig config ) {
        super( e, config );
    }
}
