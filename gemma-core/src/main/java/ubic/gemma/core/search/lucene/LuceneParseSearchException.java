package ubic.gemma.core.search.lucene;

import org.apache.lucene.queryParser.ParseException;
import ubic.gemma.core.search.ParseSearchException;

/**
 * @author poirigui
 */
public class LuceneParseSearchException extends ParseSearchException {

    public LuceneParseSearchException( String query, String message, ParseException cause ) {
        super( message, cause );
    }

    public LuceneParseSearchException( String query, String message, ParseException cause, LuceneParseSearchException originalParseException ) {
        super( message, cause, originalParseException );
    }
}
