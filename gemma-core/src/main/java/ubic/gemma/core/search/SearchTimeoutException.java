package ubic.gemma.core.search;

import ubic.gemma.model.common.search.SearchSettings;

import java.util.concurrent.TimeoutException;

/**
 * Indicate that the search failed due to a {@link TimeoutException}.
 * This is only raised when the search mode is set to {@link SearchSettings.SearchMode#BALANCED} or {@link SearchSettings.SearchMode#ACCURATE}.
 * @author poirigui
 */
public class SearchTimeoutException extends SearchException {

    private final TimeoutException cause;

    public SearchTimeoutException( String message, TimeoutException cause ) {
        super( message, cause );
        this.cause = cause;
    }

    @Override
    public synchronized TimeoutException getCause() {
        return cause;
    }
}
