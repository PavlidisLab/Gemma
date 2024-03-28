package ubic.gemma.core.search.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.highlight.Formatter;
import ubic.gemma.core.search.Highlighter;

import java.util.Map;

/**
 * Highlighter with additional capabilities for Lucene.
 */
public interface LuceneHighlighter extends Highlighter {

    /**
     * Obtain a formatter for highlights.
     */
    Formatter getFormatter();

    /**
     * Highlight a given Lucene document.
     */
    Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer );
}
