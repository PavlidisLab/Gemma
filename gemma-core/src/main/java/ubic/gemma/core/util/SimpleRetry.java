package ubic.gemma.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * A simple retry implementation with exponential backoff.
 * <p>
 * This is mainly meant for simple use cases, use {@link org.springframework.retry.support.RetryTemplate} otherwise.
 * @author poirigui
 */
public class SimpleRetry<T, E extends Exception> {

    private final Object what;
    private final int maxRetries;
    private final int retryDelayMillis;
    private final double exponentialBackoffFactor;
    private final Class<E> exceptionClass;
    private final Log logger;

    /**
     * Create a new simply retry strategy.
     * @param what                     an object describing what is being retried, it's toString() will be used for
     *                                 logging
     * @param maxRetries               maximum number of retries
     * @param retryDelayMillis         delay to wait after a failed attempt
     * @param exponentialBackoffFactor factor by which the retry delay is increased after each failed attempt
     * @param exceptionClass           the type of exception on which retry happens
     * @param logCategory             log category to use for retry-related messages
     */
    public SimpleRetry( Object what, int maxRetries, int retryDelayMillis, double exponentialBackoffFactor, Class<E> exceptionClass, String logCategory ) {
        Assert.isTrue( maxRetries >= 0, "Maximum number of retries must be zero or greater." );
        Assert.isTrue( retryDelayMillis >= 0, "Retry delay must be zero or greater." );
        Assert.isTrue( exponentialBackoffFactor >= 1, "Exponential backoff must be one or greater." );
        this.what = what;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.exponentialBackoffFactor = exponentialBackoffFactor;
        this.exceptionClass = exceptionClass;
        this.logger = LogFactory.getLog( logCategory );
    }

    /**
     * Execute the given callable with a retry strategy.
     */
    public T execute( SimpleRetryCallable<T, E> callable ) throws E {
        E lastException = null;
        for ( int i = 0; i <= maxRetries; i++ ) {
            try {
                return callable.call( i, i == maxRetries );
            } catch ( Exception e ) {
                if ( exceptionClass.isInstance( e ) ) {
                    //noinspection unchecked
                    lastException = ( E ) e;
                } else {
                    throw e;
                }
                if ( i < maxRetries ) {
                    long backoffDelay = ( long ) ( retryDelayMillis * Math.pow( exponentialBackoffFactor, i ) );
                    logger.warn( what + " could not be processed successfully, it will be retried after " + backoffDelay + " ms.", e );
                    try {
                        Thread.sleep( backoffDelay );
                    } catch ( InterruptedException e1 ) {
                        throw new RuntimeException( e1 );
                    }
                }
            }
        }
        logger.error( "Maximum number of retries reached for " + what + ", raising the last exception." );
        assert lastException != null;
        throw lastException;
    }
}
