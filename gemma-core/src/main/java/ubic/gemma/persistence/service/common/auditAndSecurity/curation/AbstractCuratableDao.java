package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import gemma.gsec.util.SecurityUtil;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.VoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDaoImpl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by tesarst on 07/03/17.
 * DAO covering methods common to all Curatable objects.
 *
 * @author tesarst
 */
public abstract class AbstractCuratableDao<C extends Curatable, VO extends AbstractCuratableValueObject<C>>
        extends VoEnabledDao<C, VO> implements CuratableDao<C, VO> {

    public AbstractCuratableDao( Class<C> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    @Override
    public Collection<C> create( final Collection<C> entities ) {
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) throws SQLException {
                for ( C entity : entities ) {
                    create( entity );
                }
            }
        } );
        return entities;
    }

    @Override
    public C create( final C entity ) {
        super.create( entity );
        if ( entity.getCurationDetails() == null ) {
            CurationDetailsDao curationDetailsDao = new CurationDetailsDaoImpl( getSessionFactory() );
            entity.setCurationDetails( curationDetailsDao.create() );
        }
        return entity;
    }

    /**
     * Finds an entity by given name.
     *
     * @param name name of the entity to be found.
     * @return entity with given name, or null if such entity does not exist.
     */
    @Override
    public Collection<C> findByName( String name ) {
        return this.findByProperty( "name", name );
    }

    /**
     * Finds an entity by given short name.
     *
     * @param name short name of the entity to be found.
     * @return entity with given short name, or null if such entity does not exist.
     */
    @Override
    public C findByShortName( String name ) {
        return this.findOneByProperty( "shortName", name );
    }

    public abstract Integer countNotTroubled();

    public abstract Map<Taxon, Long> getPerTaxonCount();

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

    /**
     * Attaches all curation events to the given value object.
     * Note that the events can still be null, as the value-change events they represent might have not occurred since
     * the curatable object was created, and they are not initialised with a creation event.
     *
     * @param vo             the value object that needs its curation events populated.
     * @param attentionEvent the last event that updated the needs attention property
     * @param noteEvent      the last event that updated the curation note property
     * @param troubleEvent   the last event that updated the troubled property
     */
    protected void addCurationEvents( AbstractCuratableValueObject vo, AuditEvent noteEvent, AuditEvent attentionEvent,
            AuditEvent troubleEvent ) {
        if ( noteEvent != null && SecurityUtil.isUserAdmin() ) {
            vo.setLastNoteUpdateEvent( new AuditEventValueObject( noteEvent ) );
        }
        if ( attentionEvent != null ) {
            vo.setLastNeedsAttentionEvent( new AuditEventValueObject( attentionEvent ) );
        }
        if ( troubleEvent != null ) {
            vo.setLastTroubledEvent( new AuditEventValueObject( troubleEvent ) );
        }
    }

}
