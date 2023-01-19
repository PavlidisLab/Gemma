package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import gemma.gsec.util.SecurityUtil;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.AbstractQueryFilteringVoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tesarst on 07/03/17.
 * DAO covering methods common to all Curatable objects.
 *
 * @author tesarst
 */
public abstract class AbstractCuratableDao<C extends Curatable, VO extends AbstractCuratableValueObject<C>>
        extends AbstractQueryFilteringVoEnabledDao<C, VO> implements CuratableDao<C, VO> {

    /**
     * HQL alias for {@link Curatable#getCurationDetails()}.
     */
    protected static final String CURATION_DETAILS_ALIAS = "s";

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
    protected void addNonTroubledFilter( Filters filters, @Nullable String objectAlias ) {
        if ( !SecurityUtil.isUserAdmin() ) {
            filters.and( objectAlias, "curationDetails.troubled", Boolean.class, Filter.Operator.eq, false );
        }
    }

    @Override
    public Set<String> getFilterableProperties() {
        Set<String> result = new HashSet<>( super.getFilterableProperties() );
        result.addAll( Arrays.asList( "lastUpdated", "troubled", "needsAttention" ) );
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Include filtering by {@code lastUpdated}, {@code troubled} and {@code needsAttention} from the associated
     * curation details.
     */
    @Override
    protected FilterablePropertyMeta getFilterablePropertyMeta( String propertyName ) throws IllegalArgumentException {
        if ( propertyName.equals( "lastUpdated" ) || propertyName.equals( "troubled" ) || propertyName.equals( "needsAttention" ) ) {
            return getFilterablePropertyMeta( CURATION_DETAILS_ALIAS, propertyName, CurationDetails.class )
                    .withDescription( "alias for curationDetails." + propertyName );
        }
        return super.getFilterablePropertyMeta( propertyName );
    }
}
