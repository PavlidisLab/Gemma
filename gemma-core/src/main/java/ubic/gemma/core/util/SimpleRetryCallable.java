package ubic.gemma.core.util;

/**
 * An interface for work that can be retried.
 * @author poirigui
 */
@FunctionalInterface
public interface SimpleRetryCallable<T, E extends Exception> {

    T call( SimpleRetryContext retryContext ) throws E;
}
