package ubic.gemma.persistence.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.genome.taxon.TaxonServiceImpl;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Collection;

/**
 * Base for all services handling DAO access.
 *
 * @param <O> the Identifiable Object type that this service is handling.
 * @author tesarst
 */
public abstract class AbstractService<O extends Identifiable> implements BaseService<O> {

    protected static final Log log = LogFactory.getLog( TaxonServiceImpl.class );

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
    public Collection<O> create( Collection<O> entities ) {
        return mainDao.create( entities );
    }

    @Override
    @Transactional
    @OverridingMethodsMustInvokeSuper
    public O create( O entity ) {
        return mainDao.create( entity );
    }

    @Override
    @Transactional
    public Collection<O> load( Collection<Long> ids ) {
        return mainDao.load( ids );
    }

    @Override
    @Transactional
    public O load( Long id ) {
        return mainDao.load( id );
    }

    @Override
    @Transactional
    public Collection<O> loadAll() {
        return mainDao.loadAll();
    }

    @Override
    @Transactional
    public int countAll() {
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
    @OverridingMethodsMustInvokeSuper
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
    @OverridingMethodsMustInvokeSuper
    public void update( O entity ) {
        mainDao.update( entity );
    }

}
