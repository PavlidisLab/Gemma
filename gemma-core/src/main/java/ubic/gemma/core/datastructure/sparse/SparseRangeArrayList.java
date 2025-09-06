package ubic.gemma.core.datastructure.sparse;

import java.util.*;

import static ubic.gemma.core.datastructure.sparse.SparseListUtils.getSparseRangeArrayElement;
import static ubic.gemma.core.datastructure.sparse.SparseListUtils.validateSparseRangeArray;

/**
 * A sparse range array is a data structure that efficiently stores arrays of repeated elements by encoding their
 * starting offsets.
 * <p>
 * For example, a sequence of characters {@code AAAAABBBBCC} is stored as {@code ABC} with the following offsets
 * {@code 0, 5, 9}.
 * @author poirigui
 * @see SparseArrayList
 */
public class SparseRangeArrayList<T> extends AbstractList<T> implements SparseList<T> {

    private final ArrayList<T> array;
    private final int[] offsets;
    private final int numberOfElements;

    public SparseRangeArrayList( Collection<T> collection ) {
        ArrayList<T> array = new ArrayList<>();
        ArrayList<Integer> offsets = new ArrayList<>();
        int i = 0;
        T lastElement = null;
        for ( T element : collection ) {
            if ( i == 0 || !Objects.equals( element, lastElement ) ) {
                array.add( element );
                offsets.add( i );
            }
            lastElement = element;
            i++;
        }
        int[] offsetsV = new int[offsets.size()];
        for ( int j = 0; j < offsets.size(); j++ ) {
            offsetsV[j] = offsets.get( j );
        }
        this.array = array;
        this.offsets = offsetsV;
        this.numberOfElements = i;
    }

    public SparseRangeArrayList( List<T> array, int[] offsets, int numberOfElements ) {
        validateSparseRangeArray( array, offsets, numberOfElements );
        this.array = new ArrayList<>( array );
        this.offsets = Arrays.copyOf( offsets, offsets.length );
        this.numberOfElements = numberOfElements;
    }

    /**
     * Create an empty sparse range array.
     */
    public SparseRangeArrayList() {
        this.array = new ArrayList<>();
        this.offsets = new int[0];
        this.numberOfElements = 0;
    }

    @Override
    public T get( int i ) {
        return getSparseRangeArrayElement( array, offsets, numberOfElements, i );
    }

    @Override
    public int storageSize() {
        return array.size();
    }

    @Override
    public int size() {
        return numberOfElements;
    }

    @Override
    public int indexOf( Object o ) {
        int i = array.indexOf( o );
        return i != -1 ? offsets[i] : -1;
    }

    @Override
    public int lastIndexOf( Object o ) {
        int i = array.lastIndexOf( o );
        return i != -1 ? offsets[i] : -1;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof SparseRangeArrayList ) {
            SparseRangeArrayList<?> that = ( SparseRangeArrayList<?> ) o;
            return numberOfElements == that.numberOfElements
                    && array.equals( that.array )
                    && Arrays.equals( offsets, that.offsets );
        } else {
            return super.equals( o );
        }
    }
}
