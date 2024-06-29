package ubic.gemma.core.loader.expression.geo.service;

import ubic.gemma.core.lang.Nullable;

import java.io.IOException;

/**
 * Represents an {@link IOException} with a chain of retries.
 * @author poirigui
 */
public class IOExceptionWithRetry extends IOException {

    @Nullable
    private final IOExceptionWithRetry errorFromPreviousAttempt;

    public IOExceptionWithRetry( IOException cause, @Nullable IOExceptionWithRetry errorFromPreviousAttempt ) {
        super( cause );
        this.errorFromPreviousAttempt = errorFromPreviousAttempt;
    }

    @Nullable
    public IOExceptionWithRetry getErrorFromPreviousAttempt() {
        return errorFromPreviousAttempt;
    }
}
