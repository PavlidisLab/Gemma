package ubic.gemma.core.util;

/**
 * An interface for work that can be retried.
 * @author poirigui
 */
@FunctionalInterface
public interface SimpleRetryCallable<T, E extends Exception> {

    /**
     * @param attempt     indicate the attempt number (zero for first, 1 for second, etc.)
     * @param lastAttempt indicate if this is the last attempt, any raised exception will bubble up to
     *                    {@link SimpleRetry#execute(SimpleRetryCallable, Object what)}
     */
    T call( SimpleRetryContext retryContext ) throws E;
}
