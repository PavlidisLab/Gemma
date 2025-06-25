package ubic.gemma.model.util;

import java.util.*;

import static ubic.gemma.model.util.SparseListUtils.getSparseArrayElement;
import static ubic.gemma.model.util.SparseListUtils.validateSparseArray;

/**
 * A sparse array backed by an {@link ArrayList}.
 * <p>
 * This array uses a common encoding for sparse encoding such as CSR and CSC. Data points are stored in two vectors:
 * elements and indices. To make operations efficient, the indices are kept in-order.
 * @param <T> the type of elements in the array
 * @author poirigui
 */
public class SparseArrayList<T> extends AbstractList<T> implements SparseList<T> {

    private final ArrayList<T> array;
    private final int[] indices;
    private final int numberOfElements;
    private final T defaultValue;

    public SparseArrayList( Collection<T> collection, T defaultValue ) {
        ArrayList<T> array = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        int i = 0;
        for ( T element : collection ) {
            if ( !Objects.equals( element, defaultValue ) ) {
                array.add( element );
                indices.add( i );
            }
            i++;
        }
        int[] indicesV = new int[indices.size()];
        for ( int j = 0; j < indices.size(); j++ ) {
            indicesV[j] = indices.get( j );
        }
        this.array = array;
        this.indices = indicesV;
        this.numberOfElements = i;
        this.defaultValue = defaultValue;
    }

    /**
     * Create a new sparse array from a given collection, {@code null} is used as default value.
     */
    public SparseArrayList( Collection<T> collection ) {
        this( collection, null );
    }

    public SparseArrayList( List<T> array, int[] indices, int numberOfElements, T defaultValue ) {
        validateSparseArray( array, indices, numberOfElements, defaultValue );
        this.array = new ArrayList<>( array );
        this.indices = Arrays.copyOf( indices, indices.length );
        this.numberOfElements = numberOfElements;
        this.defaultValue = defaultValue;
    }

    /**
     * Create a new sparse array, {@code null} is used as default value.
     */
    public SparseArrayList( List<T> array, int[] indices, int numberOfElements ) {
        this( array, indices, numberOfElements, null );
    }

    /**
     * Create an empty sparse array.
     */
    public SparseArrayList() {
        this.array = new ArrayList<>();
        this.indices = new int[0];
        this.numberOfElements = 0;
        this.defaultValue = null;
    }

    @Override
    public T get( int i ) {
        return getSparseArrayElement( array, indices, numberOfElements, i, defaultValue );
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
        return i != -1 ? indices[i] : -1;
    }

    @Override
    public int lastIndexOf( Object o ) {
        int i = array.lastIndexOf( o );
        return i != -1 ? indices[i] : -1;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof SparseArrayList ) {
            SparseArrayList<?> that = ( SparseArrayList<?> ) o;
            return numberOfElements == that.numberOfElements
                    && array.equals( that.array )
                    && Arrays.equals( indices, that.indices );
        } else {
            return super.equals( o );
        }
    }
}
