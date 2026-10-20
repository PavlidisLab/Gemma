package ubic.gemma.core.search.source;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;

import java.util.Collection;

/**
 * Shared utilities for {@link ubic.gemma.core.search.SearchSource}s.
 *
 * <p>Restored from the pre-strip Gemma search code (ed93c2f023^^) during the
 * Phase 3 search restoration; the original was deleted in the
 * "stub/delete search subsystem cascade" commit.
 */
class SearchSourceUtils {

    /**
     * Check if a collection of search results is already filled.
     *
     * @return true if the search results are filled and cannot accept more results, false otherwise
     */
    public static <T extends Identifiable> boolean isFilled( Collection<SearchResult<T>> results, SearchSettings settings ) {
        return settings.getMaxResults() > 0 && results.size() >= settings.getMaxResults();
    }
}
