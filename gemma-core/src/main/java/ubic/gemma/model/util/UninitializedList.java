package ubic.gemma.model.util;

import org.springframework.lang.NonNull;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * A list that is intentionally not initialized.
 * @author poirigui
 */
public class UninitializedList<T> extends UninitializedCollection<T> implements List<T> {

    public UninitializedList() {
        super();
    }

    public UninitializedList( int size ) {
        super( size );
    }

    @Override
    public boolean addAll( int index, @NonNull Collection<? extends T> c ) {
        throw uninitializedException();
    }

    @Override
    public T get( int index ) {
        throw uninitializedException();
    }

    @Override
    public T set( int index, T element ) {
        throw uninitializedException();
    }

    @Override
    public void add( int index, T element ) {
        throw uninitializedException();
    }

    @Override
    public T remove( int index ) {
        throw uninitializedException();
    }

    @Override
    public int indexOf( Object o ) {
        throw uninitializedException();
    }

    @Override
    public int lastIndexOf( Object o ) {
        throw uninitializedException();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator() {
        throw uninitializedException();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator( int index ) {
        throw uninitializedException();
    }

    @NonNull
    @Override
    public List<T> subList( int fromIndex, int toIndex ) {
        throw uninitializedException();
    }
}
