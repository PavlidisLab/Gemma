package ubic.gemma.core.util;

/**
 * An interface for work that can be retried.
 * @author poirigui
 */
@FunctionalInterface
public interface SimpleRetryCallable<T, E extends Exception> {

    /**
     * @param lastAttempt indicate if this is the last attempt, any raised exception will bubble up to
     * {@link SimpleRetry#execute(SimpleRetryCallable)}
     */
    T call( boolean lastAttempt ) throws E;
}
