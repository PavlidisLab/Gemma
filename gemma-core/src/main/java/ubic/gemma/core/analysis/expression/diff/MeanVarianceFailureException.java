package ubic.gemma.core.analysis.expression.diff;

/**
 * Exception raised when {@link ubic.gemma.core.util.math.linearmodels.MeanVarianceEstimator} fails.
 *
 * @author poirigui
 */
public class MeanVarianceFailureException extends AnalysisException {

    public MeanVarianceFailureException( DifferentialExpressionAnalysisConfig config, Exception cause ) {
        super( config, cause );
    }
}
