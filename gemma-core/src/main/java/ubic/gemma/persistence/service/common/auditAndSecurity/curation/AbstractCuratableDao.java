package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import gemma.gsec.util.SecurityUtil;
import org.hibernate.SessionFactory;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.persistence.service.AbstractQueryFilteringVoEnabledDao;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.FiltersUtils;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

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

    private final String objectAlias;

    protected AbstractCuratableDao( String objectAlias, Class<C> elementClass, SessionFactory sessionFactory ) {
        super( objectAlias, elementClass, sessionFactory );
        this.objectAlias = objectAlias;
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
            filters.and( objectAlias, "curationDetails.troubled", Boolean.class, Filter.Operator.eq, false );
        }
    }

    /**
     * If the filters or sort refer to one of the one-to-many relations, multiple rows will be returned per datasets, so
     * the query has to use a "distinct" clause make pagination work properly.
     * <p>
     * Using "distinct" otherwise has a steep performance penalty when combined with "order by".
     * <p>
     * Note that non-admin users always need a group by because of the jointure on ACL entries.
     */
    protected String distinctIfNecessary( @Nullable Filters filters, String... oneToManyAliases ) {
        if ( FiltersUtils.containsAnyAlias( filters, null, oneToManyAliases ) || !SecurityUtil.isUserAdmin() ) {
            return "distinct ";
        } else {
            return "";
        }
    }

    /**
     * Similar logic to {@link #distinctIfNecessary(Filters, String...)}, but using a group by since it's more efficient. It does
     * not work for the counting queries, however.
     */
    @Nullable
    protected String groupByIfNecessary( @Nullable Filters filters, @Nullable Sort sort, String... oneToManyAliases ) {
        if ( FiltersUtils.containsAnyAlias( filters, sort, oneToManyAliases ) || !SecurityUtil.isUserAdmin() ) {
            return objectAlias;
        } else {
            return null;
        }
    }

    /**
     * Format a non-troubled filter for an HQL query.
     * <p>
     * For filtering queries, use {@link #addNonTroubledFilter(Filters, String)} instead.
     *
     * @param objectAlias an alias for a {@link Curatable} entity
     */
    protected String formNonTroubledClause( String objectAlias ) {
        //language=HQL
        return SecurityUtil.isUserAdmin() ? "" : " and " + objectAlias + ".curationDetails.troubled = false";
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void configureFilterableProperties( FilterablePropertiesConfigurer configurer ) {
        super.configureFilterableProperties( configurer );
        configurer.registerProperties( "lastUpdated", "troubled", "needsAttention" );
        configurer.unregisterProperty( "curationDetails.id" );
        configurer.unregisterEntity( "curationDetails.lastNeedsAttentionEvent.", AuditEvent.class );
        configurer.unregisterEntity( "curationDetails.lastNoteUpdateEvent.", AuditEvent.class );
        configurer.unregisterEntity( "curationDetails.lastTroubledEvent.", AuditEvent.class );
        // remove audit trails
        configurer.unregisterProperties( Pattern.compile( "auditTrail\\..+$" ).asPredicate() );
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
