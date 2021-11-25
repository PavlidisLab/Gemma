package ubic.gemma.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities and algorithms for {@link List}.
 * @author poirigui
 */
public class ListUtils {

    /**
     * Get a mapping of element to their first occurrence in a {@link List}.
     *
     * This of this as an efficient way of calling {@link List#indexOf(Object)} in a loop, since it will reduce the
     * complexity to O(n) instead of O(n^2).
     *
     * I couldn't find this algorithm in Guava nor Apache Collections, but if you do, let me know!
     */
    public static <T> Map<T, Integer> indexOfElements( List<T> list ) {
        Map<T, Integer> element2position = new HashMap<>( list.size() );
        for ( int i = 0; i < list.size(); i++ ) {
            T element = list.get( i );
            if ( !element2position.containsKey( element ) ) {
                element2position.put( element, i );
            }
        }
        return element2position;
    }
}
