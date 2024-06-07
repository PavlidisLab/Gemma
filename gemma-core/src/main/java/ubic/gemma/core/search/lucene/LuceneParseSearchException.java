package ubic.gemma.core.search.lucene;

import org.apache.lucene.queryParser.ParseException;
import ubic.gemma.core.search.ParseSearchException;

/**
 * @author poirigui
 */
public class LuceneParseSearchException extends ParseSearchException {

    public LuceneParseSearchException( String query, ParseException cause ) {
        super( query, cause );
    }

    public LuceneParseSearchException( String query, String message, ParseException cause ) {
        super( query, message, cause );
    }

    public LuceneParseSearchException( String query, String message, ParseException cause, LuceneParseSearchException originalParseException ) {
        super( query, message, cause, originalParseException );
    }
}
