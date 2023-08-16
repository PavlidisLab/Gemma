package ubic.gemma.model.common.search;

import org.springframework.context.MessageSourceResolvable;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;

/**
 * Custom highlighter for search results.
 * @see ubic.gemma.core.search.SearchResult#setHighlights(Map)
 * @author poirigui
 */
public interface Highlighter extends Serializable {

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
}
