package ubic.gemma.model.common.auditAndSecurity.curation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.jdbc.Work;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.BaseDao;

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
public abstract class AbstractCuratableDao<T extends Curatable> extends HibernateDaoSupport implements BaseDao<T> {

    protected static final Log log = LogFactory.getLog( ExpressionExperimentDao.class.getName() );
    protected static final String ARG_NULL_ERR_MSG = "Argument can not be null";
    protected static final String MULTIPLE_FOUND_ERR_MSG = "Multiple entities found";
    protected String entityName = "ArrayDesign";
    protected String entityImplName = "ArrayDesignImpl";

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
            CurationDetailsDao curationDetailsDao = new CurationDetailsDao( getSessionFactory() );
            entity.setCurationDetails( curationDetailsDao.create( entity.getAuditTrail().getCreationEvent() ) );
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
        final String queryString = "select ad from " + entityImplName + " as ad where ad.id in (:ids) ";
        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ids", ids );

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

        if ( results.size() != 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    MULTIPLE_FOUND_ERR_MSG + "for shortName: " + name );
        }
        return results.iterator().next();
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

    /* ********************************
     * Private methods
     * ********************************/

    private Collection<T> findByParam( final String paramName, final String paramValue ) {
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "from " + entityImplName + " a where a." + paramName + "=:" + paramName + "" )
                .setParameter( paramName, paramValue );

        //noinspection unchecked
        return query.list();
    }

}
