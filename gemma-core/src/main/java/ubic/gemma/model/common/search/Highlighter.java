package ubic.gemma.model.common.search;

import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Custom highlighter for search results.
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
    String highlightTerm( String termUri, String termLabel, Class<? extends Identifiable> clazz );

    /**
     * Produce a highlight for a given mapping of highlighted properties.
     */
    @Nullable
    String highlightProperties( Map<String, String> fragments );
}
