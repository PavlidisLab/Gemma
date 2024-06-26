package ubic.gemma.core.search;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Highlighter specialized for ontology terms.
 * @author poirigui
 */
public interface OntologyHighlighter extends Highlighter {

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
