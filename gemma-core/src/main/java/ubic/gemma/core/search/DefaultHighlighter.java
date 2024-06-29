package ubic.gemma.core.search;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.core.search.lucene.LuceneHighlighter;
import ubic.gemma.core.search.lucene.SimpleHTMLFormatter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@CommonsLog
public class DefaultHighlighter implements LuceneHighlighter, OntologyHighlighter {

    private final Formatter formatter;

    public DefaultHighlighter() {
        this( new SimpleHTMLFormatter() );
    }

    public DefaultHighlighter( Formatter formatter ) {
        this.formatter = formatter;
    }

    @Override
    public Map<String, String> highlight( String value, String field ) {
        return Collections.singletonMap( field, value );
    }

    @Override
    public Map<String, String> highlightTerm( @Nullable String termUri, String termLabel, String field ) {
        return Collections.singletonMap( field, termLabel );
    }

    @Override
    public Formatter getFormatter() {
        return formatter;
    }

    @Override
    public Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer ) {
        Map<String, String> highlights = new HashMap<>();
        for ( Fieldable field : document.getFields() ) {
            if ( !field.isTokenized() || field.isBinary() ) {
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
