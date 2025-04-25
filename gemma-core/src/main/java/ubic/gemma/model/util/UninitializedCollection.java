package ubic.gemma.model.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;

/**
 * A collection that is intentionally not initialized.
 * @author poirigui
 */
public class UninitializedCollection<T> implements Collection<T> {

    @Nullable
    private final Integer size;

    public UninitializedCollection() {
        size = null;
    }

    public UninitializedCollection( int size ) {
        this.size = size;
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

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        throw uninitializedException();
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        throw uninitializedException();
    }

    @Nonnull
    @Override
    public <T1> T1[] toArray( @Nonnull T1[] a ) {
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
    public boolean containsAll( @Nonnull Collection<?> c ) {
        throw uninitializedException();
    }

    @Override
    public boolean addAll( @Nonnull Collection<? extends T> c ) {
        throw uninitializedException();
    }

    @Override
    public boolean removeAll( @Nonnull Collection<?> c ) {
        throw uninitializedException();
    }

    @Override
    public boolean retainAll( @Nonnull Collection<?> c ) {
        throw uninitializedException();
    }

    @Override
    public void clear() {
        throw uninitializedException();
    }

    protected UninitializedCollectionException uninitializedException() {
        return new UninitializedCollectionException( "This collection is intentionally not initialized and cannot be read or modified." );
    }
}
