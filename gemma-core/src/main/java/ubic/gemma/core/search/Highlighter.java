package ubic.gemma.core.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.highlight.Formatter;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * Custom highlighter for search results.
 * @see ubic.gemma.core.search.SearchResult#setHighlights(Map)
 * @author poirigui
 */
public interface Highlighter {

    /**
     * Produce a highlight for a given ontology term.
     *
     * @param termUri   a URI for the term or null for a full-text term
     * @param termLabel a label for the term
     * @param field     an object path through which the term was found
     * @return a suitable highlight, or null if none is found
     */
    Map<String, String> highlightTerm( @Nullable String termUri, String termLabel, String field );

    /**
     * Obtain a formatter for highlights.
     */
    Formatter getFormatter();

    /**
     * Highlight a given Lucene document.
     */
    Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer );
}
