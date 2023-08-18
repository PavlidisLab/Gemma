package ubic.gemma.core.search;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.springframework.context.MessageSourceResolvable;
import ubic.gemma.core.search.lucene.SimpleHTMLFormatter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CommonsLog
public class DefaultHighlighter implements Highlighter {

    @Nullable
    @Override
    public String highlightTerm( String termUri, String termLabel, MessageSourceResolvable className ) {
        return null;
    }

    @Nullable
    @Override
    public org.apache.lucene.search.highlight.Highlighter createLuceneHighlighter( Query query ) {
        return new org.apache.lucene.search.highlight.Highlighter( new SimpleHTMLFormatter(), new QueryScorer( query ) );
    }

    @Override
    public Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer, Set<String> fields ) {
        Map<String, String> highlights = new HashMap<>();
        for ( Fieldable field : document.getFields() ) {
            if ( !field.isTokenized() || field.isBinary() || !fields.contains( field.name() ) ) {
                continue;
            }
            try {
                String bestFragment = highlighter.getBestFragment( analyzer, field.name(), field.stringValue() );
                if ( bestFragment != null ) {
                    highlights.put( field.name(), bestFragment );
                }
            } catch ( IOException | InvalidTokenOffsetsException e ) {
                log.warn( String.format( "Failed to highlight field %s.", field.name() ) );
            }
        }
        return highlights;
    }
}
