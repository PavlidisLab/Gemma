package ubic.gemma.model.common.search;

import ubic.gemma.model.common.Identifiable;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is a special kind of set designed for holding {@link SearchResult}.
 * <p>
 * If a better result is added to the set, it replaces the existing one. If the original result had a non-null
 * {@link SearchResult#getResultObject()}, it is transferred over so that it won't need to be filled later on if needed.
 * <p>
 * The collection also honor the {@link SearchSettings#getMaxResults()} value, rejecting any new result unless replacing
 * an existing one.
 *
 * @author poirigui
 */
public class SearchResultSet<T extends Identifiable> extends AbstractSet<SearchResult<T>> {

    private final Map<SearchResult<T>, SearchResult<T>> results;
    private final int maxResults;

    public SearchResultSet( SearchSettings settings ) {
        this.results = new HashMap<>();
        this.maxResults = settings.getMaxResults();
    }

    @Override
    public Iterator<SearchResult<T>> iterator() {
        // SearchResult.compareTo is descending by score, so natural-order sort puts the highest
        // score first. Exact identifier matches (DatabaseSearchSource score 1.0 for
        // findByOfficialSymbol) thus land above inexact / full-text matches (HibernateSearchSource
        // caps at FULL_TEXT_SCORE_PENALTY = 0.9). HashMap iteration without this sort returned
        // results in bucket order, burying exact-symbol hits below substring matches.
        List<SearchResult<T>> sorted = new ArrayList<>( results.values() );
        sorted.sort( null );
        return sorted.iterator();
    }

    @Override
    public int size() {
        return results.size();
    }

    @Override
    public boolean add( SearchResult<T> t ) {
        SearchResult<T> previousValue = results.get( t );
        if ( previousValue == t ) {
            // no need to copy or merge anything if its the same object
            return false;
        }
        SearchResult<T> newValue;
        boolean replaced;
        if ( previousValue == null ) {
            if ( maxResults > 0 && size() >= maxResults ) {
                // max size reached and not replacing a previous value
                return false;
            }
            newValue = t;
            replaced = true;
        } else {
            if ( t.getScore() > previousValue.getScore() ) {
                newValue = t;
                replaced = true;
            } else {
                // new value is unchanged, so treat the passed argument as the previous value for copy-over purposes
                newValue = previousValue;
                previousValue = t;
                replaced = false;
            }
            // copy-over the previous result object if necessary
            if ( previousValue.getResultObject() != null && newValue.getResultObject() == null ) {
                newValue = newValue.withResultObject( previousValue.getResultObject() );
            }
            // merge highlights if necessary
            if ( previousValue.getHighlights() != null ) {
                if ( newValue.getHighlights() != null ) {
                    Map<String, String> mergedHighlights = new HashMap<>( previousValue.getHighlights() );
                    mergedHighlights.putAll( newValue.getHighlights() );
                    newValue = newValue.withHighlights( mergedHighlights );
                } else {
                    newValue = newValue.withHighlights( previousValue.getHighlights() );
                }
            }
        }
        results.put( newValue, newValue );
        return replaced;
    }
}
