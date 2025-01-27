package ubic.gemma.core.analysis.expression.diff;

/**
 * Exception raised when no sample is left for analysis.
 * @author poirigui
 */
public class NoSampleLeftForAnalysisException extends AnalysisException {

    public NoSampleLeftForAnalysisException( String message, DifferentialExpressionAnalysisConfig config ) {
        super( message, config );
    }
}
