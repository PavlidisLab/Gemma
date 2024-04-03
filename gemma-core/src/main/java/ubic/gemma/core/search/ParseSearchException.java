package ubic.gemma.core.search;

import org.apache.lucene.queryParser.ParseException;

import javax.annotation.Nullable;

/**
 * An exception that indicate that the search query could not be parsed.
 * <p>
 * When that occurs, we typically reattempt to parse the query.
 */
public class ParseSearchException extends SearchException {

    @Nullable
    private final ParseSearchException originalParseException;

    public ParseSearchException( String message, Throwable cause ) {
        super( message, cause );
        this.originalParseException = null;
    }

    public ParseSearchException( String message, Throwable cause, ParseSearchException originalParseException ) {
        super( message, cause );
        this.originalParseException = originalParseException;
    }

    /**
     * The original {@link ParseException} if this query was reattempted.
     */
    @Nullable
    public ParseSearchException getOriginalParseException() {
        return originalParseException;
    }
}
