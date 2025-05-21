package ubic.gemma.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple retry implementation with exponential backoff.
 * <p>
 * This is mainly meant for simple use cases, use {@link org.springframework.retry.support.RetryTemplate} otherwise.
 * @author poirigui
 */
public class SimpleRetry<E extends Exception> {

    private final SimpleRetryPolicy retryPolicy;
    private final Class<E> exceptionClass;
    private final Log logger;

    /**
     * Create a new simply retry strategy.
     * @param exceptionClass           the type of exception on which retry happens
     * @param logCategory             log category to use for retry-related messages
     */
    public SimpleRetry( SimpleRetryPolicy retryPolicy, Class<E> exceptionClass, String logCategory ) {
        this.retryPolicy = retryPolicy;
        this.exceptionClass = exceptionClass;
        this.logger = LogFactory.getLog( logCategory );
    }

    /**
     * Execute the given callable with a retry strategy.
     * @param what an object describing what is being retried, it's toString() will be used for logging
     * @throws E the first exception to occur, all other will be included as suppressed via {@link Exception#addSuppressed(Throwable)}.
     */
    public <T> T execute( SimpleRetryCallable<T, E> callable, Object what ) throws E {
        E firstException = null;
        for ( int i = 0; i <= retryPolicy.getMaxRetries(); i++ ) {
            try {
                return callable.call( new SimpleRetryContext( i, i == retryPolicy.getMaxRetries() ) );
            } catch ( Exception e ) {
                if ( exceptionClass.isInstance( e ) ) {
                    if ( firstException == null ) {
                        //noinspection unchecked
                        firstException = ( E ) e;
                    } else {
                        firstException.addSuppressed( e );
                    }
                } else {
                    throw e;
                }
                if ( i < retryPolicy.getMaxRetries() ) {
                    long backoffDelay = ( long ) ( retryPolicy.getRetryDelayMillis() * Math.pow( retryPolicy.getExponentialBackoffFactor(), i ) );
                    logger.warn( what + " could not be processed successfully, it will be retried after " + backoffDelay + " ms.", e );
                    try {
                        Thread.sleep( backoffDelay );
                    } catch ( InterruptedException e1 ) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException( e1 );
                    }
                }
            }
        }
        logger.warn( "Maximum number of retries reached for " + what + ", raising the first exception." );
        assert firstException != null;
        throw firstException;
    }
}
