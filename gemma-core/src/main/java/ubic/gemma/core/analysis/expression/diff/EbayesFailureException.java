package ubic.gemma.core.analysis.expression.diff;

/**
 * Occurs when empirical bayes estimation fails.
 * @author poirigui
 */
public class EbayesFailureException extends AnalysisException {

    public EbayesFailureException( Throwable cause, DifferentialExpressionAnalysisConfig config ) {
        super( cause, config );
    }
}
