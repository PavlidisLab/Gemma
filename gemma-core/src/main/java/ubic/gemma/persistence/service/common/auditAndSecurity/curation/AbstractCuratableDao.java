package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.jdbc.Work;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDaoImpl;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;
import ubic.gemma.model.genome.Taxon;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by tesarst on 07/03/17.
 * DAO covering methods common to all Curatable objects.
 *
 * @author tesarst
 */
public abstract class AbstractCuratableDao<T extends Curatable> extends HibernateDaoSupport implements CuratableDao<T> {

    protected static final Log log = LogFactory.getLog( ExpressionExperimentDaoImpl.class.getName() );
    protected static final String ARG_NULL_ERR_MSG = "Argument can not be null";
    protected static final String MULTIPLE_FOUND_ERR_MSG = "Multiple entities found";
    protected String entityName;

    /* ********************************
     * Abstract methods
     * ********************************/

    public abstract T find( T entity );

    public abstract Map<Taxon, Long> getPerTaxonCount();

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    public Collection<? extends T> create( final Collection<? extends T> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( ARG_NULL_ERR_MSG + ": entities" );
        }
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) throws SQLException {
                for ( T entity : entities ) {
                    create( entity );
                }
            }
        } );
        return entities;
    }

    @Override
    public T create( final T entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( ARG_NULL_ERR_MSG + ": entity" );
        }
        this.getSessionFactory().getCurrentSession().saveOrUpdate( entity );

        if ( entity.getCurationDetails() == null ) {
            CurationDetailsDao curationDetailsDao = new CurationDetailsDaoImpl( getSessionFactory() );
            entity.setCurationDetails( curationDetailsDao.create() );
        }

        return entity;
    }

    @Override
    public Collection<T> loadAll() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( entityName ).list();
    }

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( ARG_NULL_ERR_MSG + ": id" );
        }
        T entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    @Override
    public void remove( Collection<? extends T> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( ARG_NULL_ERR_MSG + ": entities" );
        }
        throw new NotYetImplementedException(
                " Removing all Curatables of certain type is not implemented, but are you sure that it is something you would want to do?" );
    }

    @Override
    public void update( final Collection<? extends T> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( ARG_NULL_ERR_MSG + ": entities" );
        }
        for ( T e : entities ) {
            update( e );
        }
    }

    /**
     * Updates the given entity in the database.
     *
     * @param entity the entity to update in database.
     */
    public void update( T entity ) {
        if ( entity == null ) {
            throw new IllegalArgumentException( ARG_NULL_ERR_MSG + ": entities" );
        }
        this.getSessionFactory().getCurrentSession().update( entity );
    }

    /**
     * Loads a curatable entity with given id from database.
     *
     * @param id the id of entity to be loaded.
     * @return entity from database. This method uses the hibernate get method, so the returned entity always represents
     * a row in the database, not proxy.
     */
    public T load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( ARG_NULL_ERR_MSG + ": id" );
        }

        //noinspection unchecked
        return ( T ) this.getSessionFactory().getCurrentSession().get( this.entityName, id );
    }

    /**
     * Loads a collection of curatable entities from database.
     *
     * @param ids ids of entities to be loaded.
     * @return collection of entities from database. This method uses the hibernate get method, so the returned entities always represent
     * a row in the database, not proxy.
     */
    public Collection<T> load( Collection<Long> ids ) {
        if ( ids == null || ids.isEmpty() ) {
            return new HashSet<>();
        }
        final String queryString = "select ad from " + entityName + " as ad where ad.id in (:ids) ";
        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids );

        //noinspection unchecked
        return query.list();
    }

    /**
     * @return the count of all entities of this curatable type.
     */
    public Integer countAll() {
        return this.loadAll().size();
    }

    /**
     * Finds an entity by given name.
     *
     * @param name name of the entity to be found.
     * @return entity with given name, or null if such entity does not exist.
     */
    public Collection<T> findByName( final String name ) {
        return this.findByParam( "name", name );
    }

    /**
     * Finds an entity by given short name.
     *
     * @param name short name of the entity to be found.
     * @return entity with given short name, or null if such entity does not exist.
     */
    public T findByShortName( final String name ) {
        //noinspection unchecked
        Collection<T> results = this.findByParam( "shortName", name );
        return this.checkAndReturn( results, "shortName", name );
    }

    public T findOrCreate( T entity ) {
        T existingEntity = this.find( entity );
        if ( existingEntity != null ) {
            return existingEntity;
        }
        log.debug( "Creating new entity: " + entity.toString() );
        return create( entity );
    }

    /* ********************************
     * Protected methods
     * ********************************/

    protected void addEventsToMap( Map<Long, Collection<AuditEvent>> eventMap, Long id, AuditEvent event ) {
        if ( eventMap.containsKey( id ) ) {

            Collection<AuditEvent> events = eventMap.get( id );
            events.add( event );
        } else {
            Collection<AuditEvent> events = new ArrayList<>();
            events.add( event );
            eventMap.put( id, events );
        }
    }

    protected T checkAndReturn( Collection<T> results, String err_arg, String err_val ) {
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    MULTIPLE_FOUND_ERR_MSG + " for " + err_arg + ": " + err_val );
        } else if ( results.size() < 1 ) {
            return null;
        }
        return results.iterator().next();
    }

    /**
     * Attaches all curation events to the given value object.
     * Note that the events can still be null, as the value-change events they represent might have not occurred since
     * the curatable object was created, and they are not initialised with a creation event.
     *
     * @param vo the value object that needs its curation events populated.
     * @return the same value object, enriched by all the curation events.
     */
    protected AbstractCuratableValueObject addCurationEvents( AbstractCuratableValueObject vo ) {
        Long id = vo.getId();
        Curatable curatable = this.load( id );
        CurationDetails details = curatable.getCurationDetails();
        if ( details.getLastNoteUpdateEvent() != null ) {
            vo.setLastNoteUpdateEvent( new AuditEventValueObject( details.getLastNoteUpdateEvent() ) );
        }
        if ( details.getLastNeedsAttentionEvent() != null ) {
            vo.setLastNeedsAttentionEvent( new AuditEventValueObject( details.getLastNeedsAttentionEvent() ) );
        }
        if ( details.getLastTroubledEvent() != null ) {
            vo.setLastTroubledEvent( new AuditEventValueObject( details.getLastTroubledEvent() ) );
        }
        return vo;
    }


    /* ********************************
     * Private methods
     * ********************************/

    private Collection<T> findByParam( final String paramName, final String paramValue ) {
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "from " + entityName + " a where a." + paramName + "=:" + paramName + "" )
                .setParameter( paramName, paramValue );

        //noinspection unchecked
        return query.list();
    }

}
