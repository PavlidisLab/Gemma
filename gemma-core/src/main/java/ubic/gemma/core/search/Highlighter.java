package ubic.gemma.core.search;

import ubic.gemma.model.common.search.SearchResult;

import java.util.Map;

/**
 * Custom highlighter for search results.
 * @see SearchResult#getHighlights()
 * @author poirigui
 */
public interface Highlighter {

    /**
     * Produce a highlight for a given field.
     */
    Map<String, String> highlight( String value, String field );
}
