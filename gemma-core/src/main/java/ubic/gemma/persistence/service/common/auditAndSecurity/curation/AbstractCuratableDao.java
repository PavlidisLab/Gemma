package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import gemma.gsec.util.SecurityUtil;
import org.hibernate.SessionFactory;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.persistence.service.AbstractQueryFilteringVoEnabledDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created by tesarst on 07/03/17.
 * DAO covering methods common to all Curatable objects.
 *
 * @author tesarst
 */
public abstract class AbstractCuratableDao<C extends Curatable, VO extends AbstractCuratableValueObject<C>>
        extends AbstractQueryFilteringVoEnabledDao<C, VO> implements CuratableDao<C> {

    protected AbstractCuratableDao( String objectAlias, Class<C> elementClass, SessionFactory sessionFactory ) {
        super( objectAlias, elementClass, sessionFactory );
    }

    @Override
    public C create( C entity ) {
        if ( entity.getCurationDetails() == null ) {
            CurationDetails cd = new CurationDetails( new Date(), null, true, null, false, null, null );
            getSessionFactory().getCurrentSession().persist( cd );
            entity.setCurationDetails( cd );
        }
        return super.create( entity );
    }
   
    @Override
    public void updateCurationDetailsFromAuditEvent( Curatable curatable, AuditEvent auditEvent ) {
        if ( curatable.getId() == null ) {
            throw new IllegalArgumentException( "Cannot update curation details for a transient entity." );
        }

        if ( curatable.getCurationDetails() == null ) {
            curatable.setCurationDetails( new CurationDetails() );
        }

        CurationDetails curationDetails = curatable.getCurationDetails();

        // Update the lastUpdated property to match the event date
        curationDetails.setLastUpdated( auditEvent.getDate() );

        // Update other curationDetails properties, if the event updates them.
        if ( auditEvent.getEventType() != null
                && CurationDetailsEvent.class.isAssignableFrom( auditEvent.getEventType().getClass() ) ) {
            CurationDetailsEvent eventType = ( CurationDetailsEvent ) auditEvent.getEventType();
            eventType.updateCurationDetails( curationDetails, auditEvent );
        }

        curatable.setCurationDetails( ( CurationDetails ) getSessionFactory().getCurrentSession().merge( curationDetails ) );
    }

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
     * Restrict results to non-troubled curatable entities for non-administrators
     */
    protected void addNonTroubledFilter( Filters filters, @Nullable String objectAlias ) {
        if ( !SecurityUtil.isUserAdmin() ) {
            filters.add( ObjectFilter.parseObjectFilter( objectAlias, "curationDetails.troubled", Boolean.class, ObjectFilter.Operator.eq, "false" ) );
        }
    }
}
