package ubic.gemma.core.search;

import org.apache.lucene.queryParser.ParseException;

import javax.annotation.Nullable;

/**
 * An exception that indicate that the search query could not be parsed.
 * <p>
 * When that occurs, we typically reattempt to parse the query.
 */
public class ParseSearchException extends SearchException {

    private final String query;

    @Nullable
    private final ParseSearchException originalParseException;

    public ParseSearchException( String query, Throwable cause ) {
        super( cause );
        this.query = query;
        this.originalParseException = null;
    }

    public ParseSearchException( String query, String message, Throwable cause ) {
        super( message, cause );
        this.query = query;
        this.originalParseException = null;
    }

    public ParseSearchException( String query, String message, Throwable cause, ParseSearchException originalParseException ) {
        super( message, cause );
        this.query = query;
        this.originalParseException = originalParseException;
    }

    public String getQuery() {
        return query;
    }

    /**
     * The original {@link ParseException} if this query was reattempted.
     */
    @Nullable
    public ParseSearchException getOriginalParseException() {
        return originalParseException;
    }
}
