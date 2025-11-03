package ubic.gemma.core.search;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchSettings;

import java.util.Set;

/**
 * Search source that can retrieve results matching specific fields.
 *
 * @author poirigui
 */
public interface FieldAwareSearchSource extends SearchSource {

    /**
     * Obtain a list of fields that can be searched on.
     *
     * @param resultType type of result being searched
     * @param searchMode the search mode being used, which might influence fields that are searched
     */
    Set<String> getFields( Class<? extends Identifiable> resultType, SearchSettings.SearchMode searchMode );
}
