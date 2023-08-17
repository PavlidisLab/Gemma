package ubic.gemma.core.search;

import ubic.gemma.model.common.Identifiable;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is a special kind of set designed for holding {@link SearchResult}.
 * <p>
 * If a better result is added to the set, it replaces the existing one. If the original result had a non-null
 * {@link SearchResult#getResultObject()}, it is transferred over so that it won't need to be filled later on if needed.
 *
 * @author poirigui
 */
public class SearchResultSet<T extends Identifiable> extends AbstractSet<SearchResult<T>> {

    private final Map<SearchResult<T>, SearchResult<T>> results;

    public SearchResultSet() {
        results = new HashMap<>();
    }

    @Override
    public Iterator<SearchResult<T>> iterator() {
        return results.values().iterator();
    }

    @Override
    public int size() {
        return results.size();
    }

    @Override
    public boolean add( SearchResult<T> t ) {
        SearchResult<T> previousValue = results.get( t );
        if ( previousValue == null || t.getScore() > previousValue.getScore() ) {
            results.put( t, t );
            // retain the result object to avoid fetching it again in the future
            if ( previousValue != null && previousValue.getResultObject() != null && t.getResultObject() == null ) {
                t.setResultObject( previousValue.getResultObject() );
            }
            // merge highlights
            if ( previousValue != null && previousValue.getHighlights() != null ) {
                Map<String, String> mergedHighlights = new HashMap<>( previousValue.getHighlights() );
                if ( t.getHighlights() != null ) {
                    mergedHighlights.putAll( t.getHighlights() );
                }
                t.setHighlights( mergedHighlights );
            }
            return true;
        }
        return false;
    }
}
