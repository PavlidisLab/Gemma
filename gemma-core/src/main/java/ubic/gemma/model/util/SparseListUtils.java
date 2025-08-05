package ubic.gemma.model.util;

import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Arrays.binarySearch;

/**
 * Utilities for dealing with sparse {@link List}
 * @author poirigui
 */
public class SparseListUtils {

    /**
     * Get an element of a sparse array.
     */
    public static <T> T getSparseArrayElement( List<T> array, int[] indices, int numberOfElements, int index, T defaultValue ) {
        if ( array.size() != indices.length ) {
            throw new IllegalArgumentException( String.format( "Invalid size for sparse array, it must contain %d indices.", array.size() ) );
        }
        // special case for dense array
        if ( indices.length == numberOfElements ) {
            return array.get( index );
        }
        if ( index < 0 ) {
            // FIXME: add support for negative indexing
            throw new UnsupportedOperationException( "Negative indexing of sparse range arrays is not supported." );
        }
        if ( index >= numberOfElements ) {
            throw new IndexOutOfBoundsException( "The index exceeds the upper bound of the array." );
        }
        int offset = binarySearch( indices, index );
        if ( offset < 0 ) {
            return defaultValue;
        }
        return array.get( offset );
    }

    public static <T> void validateSparseArray( List<T> array, int[] indices, int numberOfElements, @Nullable T defaultValue ) {
        Assert.isTrue( array.size() <= numberOfElements, "Array can contain at most " + numberOfElements + " elements." );
        Assert.isTrue( array.size() == indices.length,
                String.format( "Invalid size for sparse array, it must contain %d elements and indices.", array.size() ) );
        Assert.isTrue( !array.contains( defaultValue ), "Array may not contain the default value." );
        int lastIndex = -1;
        for ( int i : indices ) {
            Assert.isTrue( i < numberOfElements, "Indices must be in the [0, " + numberOfElements + "[ range." );
            Assert.isTrue( i > lastIndex, "Indices must be strictly increasing." );
            lastIndex = i;
        }
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
        if ( array.size() != offsets.length ) {
            throw new IllegalArgumentException( String.format( "Invalid size for sparse range array, it must contain %d indices.", array.size() ) );
        }
        if ( !array.isEmpty() && offsets[0] != 0 ) {
            throw new IllegalArgumentException( "The first offset of a non-empty sparse range array must be zero." );
        }
        if ( index < 0 ) {
            // FIXME: add support for negative indexing
            throw new UnsupportedOperationException( "Negative indexing of sparse range arrays is not supported." );
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
        int k = 0;
        int lastI = -1;
        Object lastObject = null;
        for ( int i : offsets ) {
            Assert.isTrue( i > lastI, "Offsets must be monotonously increasing." );
            Assert.isTrue( i < numberOfElements, "Offsets are invalid: indices must not exceed the number of elements." );
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
