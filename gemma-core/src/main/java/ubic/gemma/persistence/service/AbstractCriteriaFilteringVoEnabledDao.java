package ubic.gemma.persistence.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.SessionFactory;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.JpaOrder;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.util.FilterJpaUtils;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the JPA Criteria API.
 * <p>
 * Pre-Phase-2 this class was built on Hibernate's {@code org.hibernate.Criteria}, which was removed
 * entirely in Hibernate 6. Phase 2 Step 3 left a stub that threw
 * {@link UnsupportedOperationException} from every method. This is the Phase 2 Step 7 port back to
 * a functional implementation, this time on {@code jakarta.persistence.criteria}.
 * <p>
 * What's currently supported:
 * <ul>
 *   <li>Filtering: eq / notEq (incl. null), like / notLike, lessThan / greaterThan,
 *       lessOrEq / greaterOrEq, in / notIn — see {@link FilterJpaUtils}.</li>
 *   <li>Sorting: dot-walked property paths, ASC/DESC. Null-precedence is currently ignored
 *       (JPA Criteria's {@code Order} doesn't expose it; Hibernate-specific extension TODO).</li>
 *   <li>Counting: {@code count(distinct id)} via the JPA Criteria.</li>
 *   <li>{@code .size}-suffix filters via {@link jakarta.persistence.criteria.CriteriaBuilder#size}.</li>
 *   <li>Subquery filters (inSubquery / notInSubquery) via {@link jakarta.persistence.criteria.Subquery}.</li>
 * </ul>
 * <p>
 * The pre-Phase-2 {@code FilterablePropertyCriteriaAlias} introspection of the underlying
 * {@code CriteriaImpl.Subcriteria} is gone — JPA Criteria joins are explicit, so subclasses that
 * need alias resolution should register them via the
 * {@link FilterablePropertiesConfigurer#registerObjectAlias(String, String, Class, String, int)}
 * path on construction. The fragile reflection on Hibernate internals is not re-introduced.
 *
 * @author poirigui (Phase 2 port)
 */
public abstract class AbstractCriteriaFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractFilteringVoEnabledDao<O, VO> {

    protected AbstractCriteriaFilteringVoEnabledDao( Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        // null objectAlias matches the pre-Phase-2 default: legacy Hibernate Criteria used null to refer to
        // the root entity. With JPA Criteria the alias is irrelevant since we resolve everything off Root.
        super( null, elementClass, sessionFactory );
    }

    /**
     * Holder for the JPA Criteria primitives needed to assemble a filtering query. Returned by
     * {@link #getFilteringCriteria(CriteriaBuilder, Class, Filters)} so that subclasses can build
     * select/order on top of the same root + restriction predicate.
     */
    protected static final class CriteriaContext<T, R> {
        public final CriteriaQuery<T> query;
        public final Root<R> root;

        public CriteriaContext( CriteriaQuery<T> query, Root<R> root ) {
            this.query = query;
            this.root = root;
        }
    }

    /**
     * Build a {@link CriteriaQuery} of the given result type rooted at the element class and apply
     * the filter restriction. Override in subclasses if you need to add joins, fetches, or extra
     * predicates (e.g. for ACL constraints).
     */
    protected <T> CriteriaContext<T, O> getFilteringCriteria( CriteriaBuilder cb, Class<T> resultType, @Nullable Filters filters ) {
        CriteriaQuery<T> q = cb.createQuery( resultType );
        @SuppressWarnings("unchecked")
        Root<O> root = q.from( (Class<O>) getElementClass() );
        q.where( FilterJpaUtils.formRestrictionClause( cb, q, root, filters ) );
        return new CriteriaContext<>( q, root );
    }

    @Override
    public List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch sw = StopWatch.createStarted();
        CriteriaBuilder cb = getSessionFactory().getCurrentSession().getCriteriaBuilder();
        CriteriaContext<Long, O> ctx = getFilteringCriteria( cb, Long.class, filters );
        ctx.query.select( ctx.root.get( getIdentifierPropertyName() ).as( Long.class ) ).distinct( true );
        if ( sort != null ) {
            ctx.query.orderBy( buildOrders( cb, ctx.root, sort ) );
        }
        List<Long> result = getSessionFactory().getCurrentSession().createQuery( ctx.query ).getResultList();
        reportSlow( sw, "Loading " + result.size() + " IDs" );
        return result;
    }

    @Override
    public List<O> load( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch sw = StopWatch.createStarted();
        CriteriaBuilder cb = getSessionFactory().getCurrentSession().getCriteriaBuilder();
        @SuppressWarnings("unchecked")
        CriteriaContext<O, O> ctx = getFilteringCriteria( cb, (Class<O>) getElementClass(), filters );
        ctx.query.select( ctx.root ).distinct( true );
        if ( sort != null ) {
            ctx.query.orderBy( buildOrders( cb, ctx.root, sort ) );
        }
        List<O> result = getSessionFactory().getCurrentSession().createQuery( ctx.query ).getResultList();
        reportSlow( sw, "Loading " + result.size() + " entities" );
        return result;
    }

    @Override
    public Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch sw = StopWatch.createStarted();
        CriteriaBuilder cb = getSessionFactory().getCurrentSession().getCriteriaBuilder();
        @SuppressWarnings("unchecked")
        CriteriaContext<O, O> ctx = getFilteringCriteria( cb, (Class<O>) getElementClass(), filters );
        ctx.query.select( ctx.root ).distinct( true );
        if ( sort != null ) {
            ctx.query.orderBy( buildOrders( cb, ctx.root, sort ) );
        }
        Query<O> q = getSessionFactory().getCurrentSession().createQuery( ctx.query );
        if ( offset > 0 ) q.setFirstResult( offset );
        if ( limit > 0 ) q.setMaxResults( limit );
        List<O> results = q.getResultList();
        Long totalElements = ( limit > 0 && ( results.isEmpty() || results.size() == limit ) )
                ? count( filters )
                : offset + ( long ) results.size();
        reportSlow( sw, "Loading and counting " + totalElements + " entities" );
        return new Slice<>( results, sort, offset, limit, totalElements );
    }

    @Override
    public Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch sw = StopWatch.createStarted();
        CriteriaBuilder cb = getSessionFactory().getCurrentSession().getCriteriaBuilder();
        @SuppressWarnings("unchecked")
        CriteriaContext<O, O> ctx = getFilteringCriteria( cb, (Class<O>) getElementClass(), filters );
        ctx.query.select( ctx.root ).distinct( true );
        if ( sort != null ) {
            ctx.query.orderBy( buildOrders( cb, ctx.root, sort ) );
        }
        Query<O> q = getSessionFactory().getCurrentSession().createQuery( ctx.query );
        if ( offset > 0 ) q.setFirstResult( offset );
        if ( limit > 0 ) q.setMaxResults( limit );
        List<VO> results = doLoadValueObjects( q.getResultList() );
        Long totalElements = ( limit >= 0 && results.size() >= limit )
                ? count( filters )
                : ( long ) results.size();
        reportSlow( sw, "Loading and counting " + totalElements + " VOs" );
        return new Slice<>( results, sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch sw = StopWatch.createStarted();
        CriteriaBuilder cb = getSessionFactory().getCurrentSession().getCriteriaBuilder();
        @SuppressWarnings("unchecked")
        CriteriaContext<O, O> ctx = getFilteringCriteria( cb, (Class<O>) getElementClass(), filters );
        ctx.query.select( ctx.root ).distinct( true );
        if ( sort != null ) {
            ctx.query.orderBy( buildOrders( cb, ctx.root, sort ) );
        }
        List<O> entities = getSessionFactory().getCurrentSession().createQuery( ctx.query ).getResultList();
        List<VO> results = doLoadValueObjects( entities );
        reportSlow( sw, "Loading " + results.size() + " VOs" );
        return results;
    }

    @Override
    public long count( @Nullable Filters filters ) {
        StopWatch sw = StopWatch.createStarted();
        CriteriaBuilder cb = getSessionFactory().getCurrentSession().getCriteriaBuilder();
        CriteriaContext<Long, O> ctx = getFilteringCriteria( cb, Long.class, filters );
        ctx.query.select( cb.countDistinct( ctx.root.get( getIdentifierPropertyName() ) ) );
        Long result = getSessionFactory().getCurrentSession().createQuery( ctx.query ).getSingleResult();
        reportSlow( sw, "Counting " + result + " entities" );
        return result == null ? 0L : result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<Order> buildOrders( CriteriaBuilder cb, Root<O> root, Sort sort ) {
        List<Order> orders = new ArrayList<>();
        for ( ; sort != null; sort = sort.getAndThen() ) {
            String propertyName = sort.getPropertyName();
            jakarta.persistence.criteria.Expression<?> expr;
            if ( propertyName.endsWith( ".size" ) ) {
                String collectionPath = propertyName.substring( 0, propertyName.length() - ".size".length() );
                expr = cb.size( ( jakarta.persistence.criteria.Expression ) FilterJpaUtils.resolvePath( root, collectionPath ) );
            } else {
                expr = FilterJpaUtils.resolvePath( root, propertyName );
            }
            Order order = sort.getDirection() == Sort.Direction.DESC ? cb.desc( expr ) : cb.asc( expr );
            // JPA's Order has no null-precedence accessor, but in Hibernate 6 the Order returned by
            // CriteriaBuilder.asc/desc is actually a JpaOrder, which exposes nullPrecedence(...).
            if ( sort.getNullMode() != null && sort.getNullMode() != Sort.NullMode.DEFAULT && order instanceof JpaOrder ) {
                switch ( sort.getNullMode() ) {
                    case FIRST:
                        order = ( ( JpaOrder ) order ).nullPrecedence( NullPrecedence.FIRST );
                        break;
                    case LAST:
                        order = ( ( JpaOrder ) order ).nullPrecedence( NullPrecedence.LAST );
                        break;
                    default:
                        // DEFAULT handled above; nothing to do.
                        break;
                }
            }
            orders.add( order );
        }
        return orders;
    }

    private void reportSlow( StopWatch sw, String what ) {
        if ( sw.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "%s for %s took %d ms.", what, getElementClass().getName(), sw.getTime( TimeUnit.MILLISECONDS ) ) );
        }
    }
}
