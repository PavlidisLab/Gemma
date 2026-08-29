package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.core.security.util.SecurityUtil;
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
import java.util.Objects;
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
     * Restrict results to non-troubled curatable entities for non-administrators.
     * <p>
     * 🛑 Unless the caller asked about trouble themselves. This filter is editorial, not access
     * control — a troubled dataset is not secret, it is one we are telling ordinary users not to
     * rely on — and ANDing it onto a query that already says {@code troubled = true} produces a
     * contradiction that returns an empty list rather than an error. A curator asking Gemma which
     * of their datasets are troubled was being told "none", which is the one answer that is never
     * useful and never obviously wrong.
     * <p>
     * So: the default still hides them, and a caller who names {@code curationDetails.troubled} in
     * their own filter gets what they asked for.
     */
    protected void addNonTroubledFilter( Filters filters, String objectAlias ) {
        if ( shouldHideTroubled( filters, objectAlias ) ) {
            filters.and( objectAlias, "curationDetails.troubled", Boolean.class, Filter.Operator.eq, false );
        }
    }

    /**
     * The decision itself, separated so it can be tested without a session factory: a filtered query
     * for a non-administrator also carries the ACL EXISTS clause, and test-created entities have no
     * ACL rows, so a DAO-level test of this rule would pass on an empty result either way.
     */
    static boolean shouldHideTroubled( Filters filters, String objectAlias ) {
        return !SecurityUtil.isUserAdmin() && !mentionsTroubled( filters, objectAlias );
    }

    /**
     * Whether the caller's own filters already say something about the troubled flag on this alias.
     */
    private static boolean mentionsTroubled( Filters filters, String objectAlias ) {
        for ( List<Filter> clause : filters ) {
            for ( Filter f : clause ) {
                if ( f != null && "curationDetails.troubled".equals( f.getPropertyName() )
                        && Objects.equals( objectAlias, f.getObjectAlias() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If the sort refers to one of the one-to-many relations, multiple rows will be returned per entity, so the query
     * has to use a {@code group by} clause to keep pagination correct.
     * <p>
     * Returns the object alias to group by, or {@code null} when no grouping is needed.
     * <p>
     * Note: post the ACL EXISTS rewrite (Session 2), the ACL filter no longer multiplies rows, so the only remaining
     * trigger is sort-driven joins on one-to-many associations.
     */
    @Nullable
    protected String groupByIfNecessary( @Nullable Sort sort, String... oneToManyAliases ) {
        if ( FiltersUtils.containsAnyAlias( null, sort, oneToManyAliases ) ) {
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
