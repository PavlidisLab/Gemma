package ubic.gemma.persistence;

import java.util.Collection;

public interface BaseDao<T> {

    public final static int TRANSFORM_NONE = 0;

    public Collection<T> create( Collection<T> entities );

    public T create( T entity );

    public T load( Long id );

//    public Collection<T> load( Collection<Long> ids );

    public Collection<T> loadAll();

    public void remove( Long id );

    public void remove( Collection<T> entities );

    public void remove( T entity );

    public void update( Collection<T> entities );

    public void update( T entity );

}