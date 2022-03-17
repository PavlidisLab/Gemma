package ubic.gemma.persistence.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Collection;
import java.util.List;

/**
 * Base for all services handling DAO access.
 *
 * @param <O> the Identifiable Object type that this service is handling.
 * @author tesarst
 */
public abstract class AbstractService<O extends Identifiable> implements BaseService<O> {

    /**
     * @deprecated define your own logger
     */
    @Deprecated
    protected static final Log log = LogFactory.getLog( AbstractService.class );

    private final BaseDao<O> mainDao;

    protected AbstractService( BaseDao<O> mainDao ) {
        this.mainDao = mainDao;
    }

    @Override
    @Transactional(readOnly = true)
    public O find( O entity ) {
        return mainDao.find( entity );
    }

    @Override
    @Transactional
    public O findOrCreate( O entity ) {
        return mainDao.findOrCreate( entity );
    }

    @Override
    @Transactional
    public List<O> create( Collection<O> entities ) {
        return mainDao.create( entities );
    }

    @Override
    @Transactional
    public O create( O entity ) {
        return mainDao.create( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public List<O> load( Collection<Long> ids ) {
        return mainDao.load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public O load( Long id ) {
        return mainDao.load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public List<O> loadAndThaw( Collection<Long> ids ) {
        List<O> identifiable = this.load( ids );
        if ( identifiable != null ) {
            mainDao.thaw( identifiable );
        }
        return identifiable;
    }

    @Override
    @Transactional(readOnly = true)
    public O loadAndThaw( Long id ) {
        O identifiable = this.load( id );
        if ( identifiable != null ) {
            mainDao.thaw( identifiable );
        }
        return identifiable;
    }

    @Override
    @Transactional(readOnly = true)
    public List<O> loadAll() {
        return mainDao.loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return this.mainDao.countAll();
    }

    @Override
    @Transactional
    public void remove( Collection<O> entities ) {
        mainDao.remove( entities );
    }

    @Override
    @Transactional
    public void remove( Long id ) {
        mainDao.remove( id );
    }

    @Override
    @Transactional
    public void remove( O entity ) {
        mainDao.remove( entity );
    }

    @Override
    @Transactional
    public void removeAll() {
        mainDao.removeAll();
    }

    @Override
    @Transactional
    public void update( Collection<O> entities ) {
        mainDao.update( entities );
    }

    @Override
    @Transactional
    public void update( O entity ) {
        mainDao.update( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public List<O> thaw( final Collection<O> identifiables ) {
        List<O> result = this.load( EntityUtils.getIds( identifiables ) );
        mainDao.thaw( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public O thaw( O identifiable ) {
        O result = this.load( identifiable.getId() );
        if ( result != null ) {
            mainDao.thaw( result );
        }
        return result;
    }
}
