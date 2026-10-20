package ubic.gemma.core.analysis.expression.diff;

/**
 * Raised when a factor carries more than one explicitly marked baseline and the analysis was not configured with a
 * subset factor.
 * <p>
 * More than one baseline on a factor is legitimate — a dataset holding two experiments has a reference level per
 * experiment — but it means the factor cannot be analyzed as a single contrast: there is no one level the others
 * are measured against. Such a design has to be split with a subset factor, and running it whole would silently
 * pick whichever baseline came first in iteration order and report a result nobody could interpret.
 *
 * @author phase 3 baseline handling
 */
public class MultipleBaselinesRequireSubsetException extends AnalysisException {

    public MultipleBaselinesRequireSubsetException( String message, DifferentialExpressionAnalysisConfig config ) {
        super( message, config );
    }
}
