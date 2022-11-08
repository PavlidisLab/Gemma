package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import gemma.gsec.util.SecurityUtil;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.persistence.service.AbstractQueryFilteringVoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDaoImpl;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;

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
        extends AbstractQueryFilteringVoEnabledDao<C, VO> implements CuratableDao<C, VO> {

    @Autowired
    private CurationDetailsDao curationDetailsDao;

    protected AbstractCuratableDao( String objectAlias, Class<C> elementClass, SessionFactory sessionFactory ) {
        super( objectAlias, elementClass, sessionFactory );
    }

    @Override
    public C create( C entity ) {
        if ( entity.getCurationDetails() == null ) {
            entity.setCurationDetails( curationDetailsDao.create() );
        }
        return super.create( entity );
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
    protected void addNonTroubledFilter( Filters filters, String objectAlias ) {
        if ( !SecurityUtil.isUserAdmin() ) {
            filters.add( ObjectFilter.parseObjectFilter( objectAlias, "curationDetails.troubled", Boolean.class, ObjectFilter.Operator.eq, "false" ) );
        }
    }
}
