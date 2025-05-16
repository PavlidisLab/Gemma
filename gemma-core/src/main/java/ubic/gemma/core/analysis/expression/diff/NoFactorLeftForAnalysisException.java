package ubic.gemma.core.analysis.expression.diff;

/**
 * Exception raised when suitable factor is left for analysis.
 * @author poirigui
 */
public class NoFactorLeftForAnalysisException extends AnalysisException {

    public NoFactorLeftForAnalysisException( String message, DifferentialExpressionAnalysisConfig config ) {
        super( message, config );
    }
}
