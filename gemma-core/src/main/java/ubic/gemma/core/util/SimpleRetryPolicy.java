package ubic.gemma.core.util;

import lombok.Value;
import org.springframework.util.Assert;

@Value
public class SimpleRetryPolicy {

    int maxRetries;
    int retryDelayMillis;
    double exponentialBackoffFactor;

    /**
     * @param maxRetries               maximum number of retries
     * @param retryDelayMillis         delay to wait after a failed attempt
     * @param exponentialBackoffFactor factor by which the retry delay is increased after each failed attempt
     */
    public SimpleRetryPolicy( int maxRetries, int retryDelayMillis, double exponentialBackoffFactor ) {
        Assert.isTrue( maxRetries >= 0, "Maximum number of retries must be zero or greater." );
        Assert.isTrue( retryDelayMillis >= 0, "Retry delay must be zero or greater." );
        Assert.isTrue( exponentialBackoffFactor >= 1, "Exponential backoff must be one or greater." );
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.exponentialBackoffFactor = exponentialBackoffFactor;
    }
}
