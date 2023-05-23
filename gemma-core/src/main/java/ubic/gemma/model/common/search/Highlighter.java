package ubic.gemma.model.common.search;

import org.compass.core.impl.DefaultCompassHighlightedText;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom highlighter for search results.
 * @see ubic.gemma.core.search.SearchResult#setHighlights(Map)
 * @author poirigui
 */
public interface Highlighter extends Serializable {

    @Nullable
    default Map<String, String> highlightCompassHits( DefaultCompassHighlightedText compassHighlights ) {
        Map<String, String> result = new HashMap<>( compassHighlights.size() );
        //noinspection unchecked
        for ( Map.Entry<String, String> e : ( ( Map<String, String> ) compassHighlights ).entrySet() ) {
            result.put( e.getKey(), e.getValue().trim() );
        }
        return result;
    }

    /**
     * Produce a highlight for a given ontology term.
     *
     * @param termUri   a URI for the term
     * @param termLabel a label for the term
     * @param clazz     the identifiable type associated to the ontology term
     * @return a suitable highlight, or null if none is found
     */
    @Nullable
    default String highlightTerm( String termUri, String termLabel, Class<? extends Identifiable> clazz ) {
        return null;
    }
}
