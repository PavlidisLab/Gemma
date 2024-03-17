package ubic.gemma.core.util;

import org.springframework.util.Assert;

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
        fillMap( element2position, list );
        return element2position;
    }

    /**
     * Get a case-insensitive mapping of string elements to their first occurrence in a {@link List}.
     * @see #indexOfElements(List)
     */
    public static Map<String, Integer> indexOfCaseInsensitiveStringElements( List<String> list ) {
        TreeMap<String, Integer> element2position = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        fillMap( element2position, list );
        return element2position;
    }

    private static <T> void fillMap( Map<T, Integer> element2position, List<T> list ) {
        int size = list.size();
        for ( int i = 0; i < size; i++ ) {
            T element = list.get( i );
            if ( !element2position.containsKey( element ) ) {
                element2position.put( element, i );
            }
        }
    }

    /**
     * Pad a collection to the next power of 2 with the given element.
     */
    public static <T> Collection<T> padToNextPowerOfTwo( Collection<T> list, T elementForPadding ) {
        int k = Integer.highestOneBit( list.size() );
        if ( list.size() == k ) {
            return list; // already a power of 2
        }
        return pad( list, elementForPadding, k << 1 );
    }

    /**
     * Pad a collection with the given element.
     */
    public static <T> Collection<T> pad( Collection<T> list, T elementForPadding, int size ) {
        Assert.isTrue( size >= list.size(), "Target size must be greater or equal to the collection size." );
        if ( list.size() == size ) {
            return list;
        }
        List<T> paddedList = new ArrayList<>( size );
        paddedList.addAll( list );
        for ( int j = list.size(); j < size; j++ ) {
            paddedList.add( elementForPadding );
        }
        return paddedList;
    }
}
