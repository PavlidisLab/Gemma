package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import gemma.gsec.util.SecurityUtil;
import org.hibernate.SessionFactory;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.persistence.service.AbstractQueryFilteringVoEnabledDao;
import ubic.gemma.persistence.util.*;

import org.springframework.lang.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by tesarst on 07/03/17.
 * DAO covering methods common to all Curatable objects.
 *
 * @author tesarst
 */
public abstract class AbstractCuratableDao<C extends Curatable, VO extends AbstractCuratableValueObject<C>>
        extends AbstractQueryFilteringVoEnabledDao<C, VO> implements CuratableDao<C> {

    /**
     * HQL alias for {@link Curatable#getCurationDetails()}.
     */
    protected static final String CURATION_DETAILS_ALIAS = "s";

    private final String objectAlias;

    protected AbstractCuratableDao( String objectAlias, Class<C> elementClass, SessionFactory sessionFactory ) {
        super( objectAlias, elementClass, sessionFactory );
        this.objectAlias = objectAlias;
    }

    @Override
    public List<Long> loadTroubledIds() {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select c.id from " + getElementClass().getSimpleName() + " c "
                        + "join c.curationDetails cd where cd.troubled = true" )
                .list();
    }

    @Override
    public void updateCurationDetailsFromAuditEvent( C curatable, AuditEvent auditEvent ) {
        Assert.notNull( curatable.getId(), "Cannot update curation details for a transient entity." );

        if ( curatable.getCurationDetails() == null ) {
            log.info( curatable + " has no curation details, creating a new one..." );
            curatable.setCurationDetails( new CurationDetails() );
        }

        CurationDetails curationDetails = curatable.getCurationDetails();

        // Update the lastUpdated property to match the event date
        curationDetails.setLastUpdated( auditEvent.getDate() );

        // Update other curationDetails properties, if the event updates them.
        if ( auditEvent.getEventType() instanceof CurationDetailsEvent ) {
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
    protected String distinctIfNecessary() {
        if ( AclQueryUtils.requiresCountDistinct() ) {
            return "distinct ";
        } else {
            return "";
        }
    }

    /**
     * Similar logic to {@link #distinctIfNecessary()}, but using a group by since it's more efficient. It does
     * not work for the counting queries, however.
     */
    @Nullable
    protected String groupByIfNecessary( @Nullable Sort sort, String... oneToManyAliases ) {
        if ( FiltersUtils.containsAnyAlias( null, sort, oneToManyAliases ) || AclQueryUtils.requiresGroupBy() ) {
            return objectAlias;
        } else {
            return null;
        }
    }

    /**
     * Form a non-troubled clause.
     */
    protected String formNonTroubledClause( String objectAlias, Class<? extends Curatable> clazz ) {
        String entityName = ubic.gemma.persistence.hibernate.HibernateUtils.getEntityName( getSessionFactory(), clazz );
        if ( !SecurityUtil.isUserAdmin() ) {
            //language=HQL
            return " and " + objectAlias + " not in (select c from " + entityName + " c join c.curationDetails cd where cd.troubled = true)";
        } else {
            return "";
        }
    }

    /**
     * Form a native non-troubled clause.
     * <p>
     * Hibernate 6 removed {@code SessionFactory.getClassMetadata}; the table and column names
     * used to be looked up via {@code SingleTableEntityPersister}. The curatable entities are
     * a small fixed set, so we hard-code their physical mapping here.
     */
    protected String formNativeNonTroubledClause( String idColumn, Class<? extends Curatable> clazz ) {
        if ( SecurityUtil.isUserAdmin() ) {
            return "";
        }
        String table;
        if ( ExpressionExperiment.class.isAssignableFrom( clazz ) ) {
            table = "INVESTIGATION";
        } else if ( ArrayDesign.class.isAssignableFrom( clazz ) ) {
            table = "ARRAY_DESIGN";
        } else {
            throw new IllegalArgumentException( "No physical-table mapping known for " + clazz );
        }
        // both curatable tables use the same FK column name for curation details
        String columnName = "CURATION_DETAILS_FK";
        //language=SQL
        return " and " + idColumn + " not in (select c.ID from " + table
                + " c join CURATION_DETAILS cd on c." + columnName + " = cd.ID where cd.TROUBLED)";
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
    protected FilterablePropertyMeta.FilterablePropertyMetaBuilder resolveFilterablePropertyMeta( String propertyName ) throws IllegalArgumentException {
        if ( propertyName.equals( "lastUpdated" ) || propertyName.equals( "troubled" ) || propertyName.equals( "needsAttention" ) ) {
            return resolveFilterablePropertyMeta( CURATION_DETAILS_ALIAS, CurationDetails.class, propertyName )
                    .description( "alias for curationDetails." + propertyName );
        }
        return super.resolveFilterablePropertyMeta( propertyName );
    }
}
