package ubic.gemma.persistence.service;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the Hibernate {@link Criteria} API.
 *
 * @see ubic.gemma.persistence.util.ObjectFilterQueryUtils for utilities to generate HQL clauses from
 * @see ubic.gemma.persistence.util.AclQueryUtils for utilities to generate ACL clauses to filter VOs by ACL at the
 * database-level
 * @see AbstractCriteriaFilteringVoEnabledDao as an alternative
 */
public abstract class AbstractQueryFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledDao<O, VO> {

    /**
     * Enumeration of hints that can be used to tune HQL queries.
     */
    protected enum QueryHint {
        /**
         * Indicate that all elements are fetched (i.e. no offset/limit will be applied on the query).
         *
         * This is useful to queries that want to use 'join fetch' on one-to-many or many-to-many relationships.
         */
        FETCH_ALL,
    }

    protected AbstractQueryFilteringVoEnabledDao( Class<O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * Produce a query for retrieving value objects after applying a set of filters and a given ordering.
     *
     * Note that if your implementation does not produce a {@link List <O>} when {@link Query#list()} is invoked, you
     * must override {@link FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort, int, int)}.
     *
     * @return a {@link Query} that produce a list of {@link O}
     */
    protected abstract Query getLoadValueObjectsQuery( Filters filters, Sort sort, EnumSet<QueryHint> hints );

    /**
     * Produce a query that will be used to retrieve the size of {@link #getLoadValueObjectsQuery(Filters, Sort, EnumSet)}.
     * @param filters
     * @return a {@link Query} which must return a single {@link Long} value
     */
    protected Query getCountValueObjectsQuery( Filters filters ) {
        throw new NotImplementedException( "Counting " + elementClass + " is not supported." );
    }

    /**
     * Process a result from Hibernate into a value object.
     *
     * The result is obtained from {@link Query#list()}.
     *
     * By default, it will cast the result into a {@link O} and then apply {@link #loadValueObject(Identifiable)} to
     * obtain a value object.
     *
     * @return a value object, or null, and it will be ignored when constructing the {@link Slice} in {@link #loadValueObjectsPreFilter(Filters, Sort, int, int)}
     */
    protected VO processLoadValueObjectsQueryResult( Object result ) {
        return loadValueObject( ( O ) result );
    }

    @Override
    public Slice<VO> loadValueObjectsPreFilter( Filters filters, Sort sort, int offset, int limit ) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        EnumSet<QueryHint> hints = EnumSet.noneOf( QueryHint.class );
        if ( offset <= 0 && limit <= 0 ) {
            hints.add( QueryHint.FETCH_ALL );
        }

        Query query = this.getLoadValueObjectsQuery( filters, sort, hints );
        Query totalElementsQuery = getCountValueObjectsQuery( filters );

        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );

        //noinspection unchecked
        List<?> list = query.list();

        List<VO> vos = list.stream()
                .map( this::processLoadValueObjectsQueryResult )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );

        Long totalElements = ( Long ) totalElementsQuery.uniqueResult();

        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > 20 ) {
            log.info( "Loading and counting VOs for " + elementClass.getName() + " took " + stopWatch.getTime( TimeUnit.MILLISECONDS ) + "ms." );
        }

        return new Slice<>( vos, sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjectsPreFilter( Filters filters, Sort sort ) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Query query = this.getLoadValueObjectsQuery( filters, sort, EnumSet.of( QueryHint.FETCH_ALL ) );

        //noinspection unchecked
        List<?> list = query.list();

        try {
            return list.stream()
                    .map( this::processLoadValueObjectsQueryResult )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toList() );
        } finally {
            stopWatch.stop();
            if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > 20 ) {
                log.info( "Loading VOs for " + elementClass.getName() + " took " + stopWatch.getTime( TimeUnit.MILLISECONDS ) + "ms." );
            }
        }
    }
}
