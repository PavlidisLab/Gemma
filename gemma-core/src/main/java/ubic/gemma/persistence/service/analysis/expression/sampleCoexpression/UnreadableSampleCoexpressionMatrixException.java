package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

/**
 * A stored sample correlation matrix whose blob cannot be decoded for its dimension: its length fits neither the full
 * nor the lower-triangle format for the dimension's number of bioassays. The row is corrupt rather than absent;
 * recomputing the matrix replaces it.
 * <p>
 * An {@link IllegalArgumentException}, as decoding failures were before, so callers that already handle those are
 * unaffected.
 */
public class UnreadableSampleCoexpressionMatrixException extends IllegalArgumentException {

    public UnreadableSampleCoexpressionMatrixException( String message, Throwable cause ) {
        super( message, cause );
    }
}
