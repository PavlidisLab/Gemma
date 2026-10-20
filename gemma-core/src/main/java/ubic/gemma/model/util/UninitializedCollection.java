package ubic.gemma.model.util;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Iterator;

/**
 * A collection that is intentionally not initialized.
 *
 * @author poirigui
 */
public abstract class UninitializedCollection<T> implements Collection<T> {

    @Nullable
    private final Integer size;

    protected UninitializedCollection() {
        size = null;
    }

    protected UninitializedCollection( int size ) {
        this.size = size;
    }

    public boolean sized() {
        return size != null;
    }

    @Override
    public int size() {
        if ( size != null ) {
            return size;
        }
        throw uninitializedException();
    }

    @Override
    public boolean isEmpty() {
        if ( size != null ) {
            return size == 0;
        }
        throw uninitializedException();
    }

    @Override
    public boolean contains( Object o ) {
        throw uninitializedException();
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        throw uninitializedException();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        throw uninitializedException();
    }

    @NonNull
    @Override
    public <T1> T1[] toArray( @NonNull T1[] a ) {
        throw uninitializedException();
    }

    @Override
    public boolean add( T t ) {
        throw uninitializedException();
    }

    @Override
    public boolean remove( Object o ) {
        throw uninitializedException();
    }

    @Override
    public boolean containsAll( @NonNull Collection<?> c ) {
        throw uninitializedException();
    }

    @Override
    public boolean addAll( @NonNull Collection<? extends T> c ) {
        throw uninitializedException();
    }

    @Override
    public boolean removeAll( @NonNull Collection<?> c ) {
        throw uninitializedException();
    }

    @Override
    public boolean retainAll( @NonNull Collection<?> c ) {
        throw uninitializedException();
    }

    @Override
    public void clear() {
        throw uninitializedException();
    }

    @Override
    public int hashCode() {
        throw uninitializedException();
    }

    @Override
    public boolean equals( Object obj ) {
        throw uninitializedException();
    }

    protected UninitializedCollectionException uninitializedException() {
        return new UninitializedCollectionException( "This collection is intentionally not initialized and cannot be read or modified." );
    }
}
