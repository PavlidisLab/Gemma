package ubic.gemma.core.util;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Arrays.binarySearch;

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
     * Get an element of a sparse range array.
     * @param array            collection of elements applying for the ranges
     * @param offsets          starting offsets of the ranges
     * @param numberOfElements the size of the original array
     * @param index            a position to retrieve
     * @throws ArrayIndexOutOfBoundsException if the index is out of bounds
     * @throws IllegalArgumentException       if the array and offsets do not have the same size
     * @see #validateSparseRangeArray(List, int[], int)
     */
    public static <T> T getSparseRangeArrayElement( List<T> array, int[] offsets, int numberOfElements, int index ) {
        Assert.isTrue( array.size() == offsets.length,
                String.format( "Invalid size for offsets array, it must contain %d indices.", array.size() ) );
        if ( index < 0 ) {
            // FIXME: add support for negative indexing
            throw new ArrayIndexOutOfBoundsException( "Negative indexing of sparse range arrays is not allowed." );
        }
        if ( index >= numberOfElements ) {
            throw new ArrayIndexOutOfBoundsException( "The index exceeds the upper bound of the array." );
        }
        int offset = binarySearch( offsets, index );
        if ( offset < 0 ) {
            return array.get( -offset - 2 );
        }
        return array.get( offset );
    }

    /**
     * Validate a sparse range array.
     * @param array            collection of elements applying for the ranges
     * @param offsets          starting offsets of the ranges
     * @param numberOfElements the size of the original array
     * @throws IllegalArgumentException if the sparse range array is invalid
     */
    public static void validateSparseRangeArray( List<?> array, int[] offsets, int numberOfElements ) throws IllegalArgumentException {
        Assert.isTrue( array.size() == offsets.length,
                "There must be as many offsets as entries in the corresponding array." );
        int k = 0;
        int lastI = -1;
        Object lastObject = null;
        for ( int i : offsets ) {
            Assert.isTrue( i > lastI, "Offsets must be monotonously increasing." );
            Assert.isTrue( i < numberOfElements, "Offsets are invalid: indices must not exceed the number of cells." );
            Object o = array.get( k );
            if ( k == 0 && i != 0 ) {
                throw new IllegalArgumentException( "The first offset must be zero." );
            }
            // not using equality because one might want to use object identity
            if ( k > 0 && o == lastObject ) {
                throw new IllegalArgumentException(
                        String.format( "Successive ranges [%d, %d[ and [%d, %d[ cannot be for the same object: %s.",
                                offsets[k - 1], offsets[k], offsets[k], k < offsets.length - 1 ? offsets[k + 1] : numberOfElements, o ) );
            }
            lastI = i;
            lastObject = o;
            k++;
        }
    }
}
