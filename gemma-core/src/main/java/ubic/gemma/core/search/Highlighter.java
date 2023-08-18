package ubic.gemma.core.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.springframework.context.MessageSourceResolvable;

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
     * @param termUri   a URI for the term
     * @param termLabel a label for the term
     * @param className a resolvable message for the type associated to the ontology term
     * @return a suitable highlight, or null if none is found
     */
    @Nullable
    String highlightTerm( String termUri, String termLabel, MessageSourceResolvable className );

    /**
     * Obtain a highlighter for Lucene hits.
     */
    @Nullable
    org.apache.lucene.search.highlight.Highlighter createLuceneHighlighter( Query query );

    /**
     * Highlight a given Lucene document.
     */
    Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer, Set<String> fields );
}
