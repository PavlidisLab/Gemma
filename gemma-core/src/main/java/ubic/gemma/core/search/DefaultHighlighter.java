package ubic.gemma.core.search;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import ubic.gemma.core.search.lucene.SimpleHTMLFormatter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CommonsLog
public class DefaultHighlighter implements Highlighter {

    @Override
    public Map<String, String> highlightTerm( @Nullable String termUri, String termLabel, String field ) {
        return Collections.emptyMap();
    }

    @Override
    public org.apache.lucene.search.highlight.Highlighter createLuceneHighlighter( QueryScorer queryScorer ) {
        return new org.apache.lucene.search.highlight.Highlighter( new SimpleHTMLFormatter(), queryScorer );
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
