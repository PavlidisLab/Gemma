package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.persistence.BaseDao;

import java.util.Collection;

/**
 * Created by tesarst on 13/03/17.
 */
public interface CuratableDao<T> extends BaseDao<T> {

    Collection<T> findByName( String name );

    T findOrCreate( T entity );

    T findByShortName( String name );

    Integer countAll();

    Collection<T> load( Collection<Long> ids );

    Collection<T> loadAll();
}
