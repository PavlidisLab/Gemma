package ubic.gemma.core.search;

import org.apache.lucene.search.highlight.Formatter;
import org.springframework.context.MessageSourceResolvable;

import javax.annotation.Nullable;
import java.util.Map;

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
     * @param clazz     the identifiable type associated to the ontology term
     * @return a suitable highlight, or null if none is found
     */
    @Nullable
    default String highlightTerm( String termUri, String termLabel, MessageSourceResolvable className ) {
        return null;
    }

    /**
     * Obtain a formatter for Lucene hits.
     */
    @Nullable
    default Formatter getLuceneFormatter() {
        return null;
    }
}
