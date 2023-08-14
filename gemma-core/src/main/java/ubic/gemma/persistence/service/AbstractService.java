package ubic.gemma.persistence.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Base for all services handling DAO access.
 *
 * @param <O> the Identifiable Object type that this service is handling.
 * @author tesarst
 */
public abstract class AbstractService<O extends Identifiable> implements BaseService<O> {

    protected static final Log log = LogFactory.getLog( AbstractService.class );

    private final BaseDao<O> mainDao;

    protected AbstractService( BaseDao<O> mainDao ) {
        this.mainDao = mainDao;
    }

    @Override
    public Class<? extends O> getElementClass() {
        return mainDao.getElementClass();
    }

    @Override
    @Transactional(readOnly = true)
    public O find( O entity ) {
        return mainDao.find( entity );
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public O findOrFail( O entity ) {
        return requireNonNull( mainDao.find( entity ),
                String.format( "No %s matching %s could be found.", mainDao.getElementClass().getName(), entity ) );
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
    public Collection<O> save( Collection<O> entities ) {
        return mainDao.save( entities );
    }

    @Override
    @Transactional
    @OverridingMethodsMustInvokeSuper
    public O save( O entity ) {
        return mainDao.save( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<O> load( Collection<Long> ids ) {
        return mainDao.load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public O load( Long id ) {
        return mainDao.load( id );
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public O loadOrFail( Long id ) {
        return requireNonNull( mainDao.load( id ),
                String.format( "No %s with ID %d.", mainDao.getElementClass().getName(), id ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<O> loadAll() {
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
    @OverridingMethodsMustInvokeSuper
    public void remove( O entity ) {
        mainDao.remove( entity );
    }

    @Override
    @Transactional
    public void removeAllInBatch() {
        mainDao.removeAllInBatch();
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

    /**
     * Ensure that a given entity is in the current session.
     * <p>
     * If not found in the current session, it will be retrieved from the DAO.
     */
    @CheckReturnValue
    protected O ensureInSession( O entity ) {
        Long id = entity.getId();
        if ( id == null )
            return entity; // transient
        return requireNonNull( mainDao.load( id ),
                String.format( "No %s with ID %d.", mainDao.getElementClass().getName(), id ) );
    }

    /**
     * Ensure that a collection of entities are in the current session.
     * <p>
     * Implementation note: if all entities are already in the session - or are transient, this call is very fast and
     * does not involve any database interaction, otherwise the persistent entities are fetched in bulk.
     *
     * @see #ensureInSession(Identifiable)
     */
    @CheckReturnValue
    protected Collection<O> ensureInSession( Collection<O> entities ) {
        List<O> result = new ArrayList<>();
        Iterator<O> it = entities.iterator();
        boolean allEntitiesAlreadyInSession = true;
        while ( it.hasNext() ) {
            O e = it.next();
            O se = ensureInSession( e );
            result.add( se );
            if ( e != se ) {
                allEntitiesAlreadyInSession = false;
                break;
            }
        }

        // no need to sort or fetch anything, just return the input
        if ( allEntitiesAlreadyInSession )
            return entities;

        // bulk load the remaining persistent entities (if any)
        Set<Long> ids = new HashSet<>( entities.size() );
        it.forEachRemaining( f -> {
            if ( f.getId() == null ) {
                result.add( f ); // transient
            } else {
                ids.add( f.getId() );
            }
        } );
        if ( !ids.isEmpty() ) {
            result.addAll( mainDao.load( ids ) );
        }

        Map<O, Integer> ix;
        try {
            ix = ListUtils.indexOfElements( entities instanceof List ? ( List<O> ) entities : new ArrayList<>( entities ) );
        } catch ( RuntimeException e ) {
            log.warn( String.format( "Failed to create index of elements for a collection of %d %s; the resulting output will not be sorted.",
                    entities.size(), mainDao.getElementClass().getName() ), e );
            return result;
        }

        // sort entries according to the input collection order
        result.sort( Comparator.comparingInt( ix::get ) );

        return result;
    }
}