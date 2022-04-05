package ubic.gemma.core.util;

import java.util.*;

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

    /**
     * Efficiently add new elements from a collection to a list that are not already present.
     *
     * The naïve way of doing this takes a O(n*m) time complexity because the original list has to be traversed every
     * single time a new element is to be added. This strategy works in O(n+m) by first constructing a set of already
     * present elements and using that set to match elements to add to the list.
     *
     * @param list        a list containing elements
     * @param newElements a collection of elements to add to the list
     * @param <T>
     */
    public static <T> void addAllNewElements( List<T> list, Collection<T> newElements ) {
        // O(n)
        Set<T> alreadyIn = new HashSet<>( list );
        // O(m)
        for ( T elem : newElements ) {
            if ( !alreadyIn.contains( elem ) ) {
                list.add( elem );
                alreadyIn.add( elem );
            }
        }
    }
}
