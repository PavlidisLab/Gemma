package ubic.gemma.core.search;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchSettings;

import java.util.Collection;

public class SearchSettingsUtils {

    /**
     * Check if a collection of search results is already filled.
     *
     * @return true if the search results are filled and cannot accept more results, false otherwise
     */
    public static <T extends Identifiable> boolean isFilled( Collection<SearchResult<T>> results, SearchSettings settings ) {
        return settings.getMaxResults() > 0 && results.size() >= settings.getMaxResults();
    }
}
