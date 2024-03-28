package ubic.gemma.core.search.lucene;

import org.apache.lucene.queryParser.ParseException;
import ubic.gemma.core.search.SearchException;

/**
 * @author poirigui
 */
public class LuceneParseSearchException extends SearchException {
    public LuceneParseSearchException( ParseException e ) {
        super( e.getMessage(), e );
    }
}
