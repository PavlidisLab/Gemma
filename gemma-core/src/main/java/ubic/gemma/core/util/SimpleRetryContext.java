package ubic.gemma.core.util;

import lombok.Value;

/**
 * Holds the state of a retry attempt.
 * @author poirigui
 */
@Value
public class SimpleRetryContext {

    /**
     * Indicate the attempt number (zero for first, 1 for second, etc.)
     */
    public int attempt;

    /**
     * Indicate if this is the last attempt, any raised exception will bubble up to
     * {@link SimpleRetry#execute(SimpleRetryCallable, Object)}
     */
    public boolean lastAttempt;
}
