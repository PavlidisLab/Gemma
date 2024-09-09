package ubic.gemma.core.util;

import org.springframework.util.Assert;

import java.util.*;

import static java.util.Arrays.binarySearch;

/**
 * Utilities and algorithms for {@link List}.
 *
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
     *
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
     * Get an element of a sparse array.
     */
    public static <T> T getSparseArrayElement( T[] array, int[] indices, int numberOfElements, int index, T defaultValue ) {
        Assert.isTrue( array.length == indices.length,
                String.format( "Invalid size for sparse array, it must contain %d indices.", array.length ) );
        // special case for dense array
        if ( indices.length == numberOfElements ) {
            return array[index];
        }
        if ( index < 0 ) {
            // FIXME: add support for negative indexing
            throw new IndexOutOfBoundsException( "Negative indexing of sparse range arrays is not allowed." );
        }
        if ( index >= numberOfElements ) {
            throw new IndexOutOfBoundsException( "The index exceeds the upper bound of the array." );
        }
        int offset = binarySearch( indices, index );
        if ( offset < 0 ) {
            return defaultValue;
        }
        return array[offset];
    }

    /**
     * Get an element of a sparse range array.
     *
     * @param array            collection of elements applying for the ranges
     * @param offsets          starting offsets of the ranges
     * @param numberOfElements the size of the original array
     * @param index            a position to retrieve
     * @throws IndexOutOfBoundsException if the requested index is out of bounds
     * @throws IllegalArgumentException  if the array is empty or its size differs from offsets
     * @see #validateSparseRangeArray(List, int[], int)
     */
    public static <T> T getSparseRangeArrayElement( List<T> array, int[] offsets, int numberOfElements, int index ) throws IllegalArgumentException, IndexOutOfBoundsException {
        Assert.isTrue( array.size() == offsets.length,
                String.format( "Invalid size for sparse range array, it must contain %d indices.", array.size() ) );
        Assert.isTrue( offsets[0] == 0, "The first offset of a sparse range array must be zero." );
        if ( index < 0 ) {
            // FIXME: add support for negative indexing
            throw new IndexOutOfBoundsException( "Negative indexing of sparse range arrays is not allowed." );
        }
        if ( index >= numberOfElements ) {
            throw new IndexOutOfBoundsException( "The index exceeds the upper bound of the array." );
        }
        int offset = binarySearch( offsets, index );
        if ( offset < 0 ) {
            return array.get( -offset - 2 );
        }
        return array.get( offset );
    }

    /**
     * Validate a sparse range array.
     *
     * @param array            collection of elements applying for the ranges
     * @param offsets          starting offsets of the ranges
     * @param numberOfElements the size of the original array
     * @throws IllegalArgumentException if the sparse range array is invalid
     */
    public static void validateSparseRangeArray( List<?> array, int[] offsets, int numberOfElements ) throws IllegalArgumentException {
        Assert.isTrue( numberOfElements == 0 || !array.isEmpty(),
                "A non-empty sparse range array must have at least one element." );
        Assert.isTrue( array.size() == offsets.length,
                "There must be as many offsets as entries in the corresponding array." );
        Assert.isTrue( offsets[0] == 0, "The first offset of a sparse range array must be zero." );
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

    /**
     * Pad a collection to the next power of 2 with the given element.
     */
    public static <T> List<T> padToNextPowerOfTwo( List<T> list, T elementForPadding ) {
        int k = Integer.highestOneBit( list.size() );
        if ( list.size() == k ) {
            return list; // already a power of 2
        }
        return pad( list, elementForPadding, k << 1 );
    }

    /**
     * Pad a collection with the given element.
     */
    public static <T> List<T> pad( List<T> list, T elementForPadding, int size ) {
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

    public static <T> List<List<T>> batch( List<T> list, int batchSize ) {
        if ( batchSize == -1 ) {
            return Collections.singletonList( list );
        }
        int numberOfBatches = ( list.size() / batchSize ) + ( list.size() % batchSize > 0 ? 1 : 0 );
        int size = numberOfBatches * batchSize;
        List<T> paddedList = pad( list, list.get( list.size() - 1 ), size );
        return org.apache.commons.collections4.ListUtils.partition( paddedList, batchSize );
    }
}
