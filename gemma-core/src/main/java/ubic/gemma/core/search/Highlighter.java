package ubic.gemma.core.search;

import java.util.Map;

/**
 * Custom highlighter for search results.
 * @see ubic.gemma.core.search.SearchResult#setHighlights(Map)
 * @author poirigui
 */
public interface Highlighter {

    /**
     * Produce a highlight for a given field.
     */
    Map<String, String> highlight( String value, String field );
}
