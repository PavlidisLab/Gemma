package ubic.gemma.core.search;

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
     * @param termUri   a URI for the term or null for a full-text term
     * @param termLabel a label for the term
     * @param field     an object path through which the term was found
     * @return a suitable highlight, or null if none is found
     */
    Map<String, String> highlightTerm( @Nullable String termUri, String termLabel, String field );
}
