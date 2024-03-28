package ubic.gemma.core.search.lucene;

import org.apache.lucene.queryParser.ParseException;
import ubic.gemma.core.search.SearchException;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class LuceneParseSearchException extends SearchException {

    @Nullable
    private final ParseException originalParseException;

    public LuceneParseSearchException( String message, ParseException cause ) {
        super( message, cause );
        this.originalParseException = null;
    }

    public LuceneParseSearchException( String message, ParseException cause, ParseException originalParseException ) {
        super( message, cause );
        this.originalParseException = originalParseException;
    }

    /**
     * The original {@link ParseException} if this query was reparsed without special characters.
     */
    @Nullable
    public ParseException getOriginalParseException() {
        return originalParseException;
    }
}
