package ubic.gemma.core.search;

import org.apache.lucene.queryParser.ParseException;

public class LuceneSearchException extends SearchException {
    public LuceneSearchException( ParseException e ) {
        super( e.getMessage(), e );
    }
}
