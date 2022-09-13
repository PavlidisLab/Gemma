package ubic.gemma.persistence.service;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the Hibernate {@link Query} API.
 *
 * @see ubic.gemma.persistence.util.ObjectFilterQueryUtils for utilities to generate HQL clauses from a {@link Filters}
 * @see ubic.gemma.persistence.util.AclQueryUtils for utilities to generate ACL clauses to filter VOs by ACL at the
 * database-level
 * @see AbstractCriteriaFilteringVoEnabledDao as an alternative
 *
 * @author poirigui
 */
@ParametersAreNonnullByDefault
public abstract class AbstractQueryFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledDao<O, VO> {

    /**
     * Enumeration of hints that can be used to tune HQL queries.
     */
    protected enum QueryHint {
        /**
         * Indicate that the elements of the query will be paginated with {@link Query#setFirstResult(int)} and {@link Query#setMaxResults(int)}.
         *
         * This is useful to queries that perform 'join fetch' on one-to-many and many-to-many relationships.
         */
        PAGINATED
    }

    protected AbstractQueryFilteringVoEnabledDao( String objectAlias, Class<O> elementClass, SessionFactory sessionFactory ) {
        super( objectAlias, elementClass, sessionFactory );
    }

    /**
     * Produce a query for retrieving value objects after applying a set of filters and a given ordering.
     *
     * Note that if your implementation does not produce a {@link List <O>} when {@link Query#list()} is invoked, you
     * must override {@link FilteringVoEnabledDao#loadValueObjectsPreFilter(Filters, Sort, int, int)}.
     *
     * @return a {@link Query} that produce a list of {@link O}
     */
    protected abstract Query getLoadValueObjectsQuery( @Nullable Filters filters, @Nullable Sort sort, EnumSet<QueryHint> hints );

    /**
     * Produce a query that will be used to retrieve the size of {@link #getLoadValueObjectsQuery(Filters, Sort, EnumSet)}.
     * @param filters
     * @return a {@link Query} which must return a single {@link Long} value
     */
    protected Query getCountValueObjectsQuery( @Nullable Filters filters ) {
        throw new NotImplementedException( "Counting " + elementClass + " is not supported." );
    }

    /**
     * Process a result from Hibernate into a value object.
     *
     * The result is obtained from {@link Query#list()}.
     *
     * By default, it will cast the result into a {@link O} and then apply {@link #doLoadValueObject(Identifiable)} to
     * obtain a value object.
     *
     * @return a value object, or null, and it will be ignored when constructing the {@link Slice} in {@link #loadValueObjectsPreFilter(Filters, Sort, int, int)}
     */
    @Nullable
    protected VO processLoadValueObjectsQueryResult( Object result ) {
        //noinspection unchecked
        return doLoadValueObject( ( O ) result );
    }

    @Override
    public Slice<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch stopWatch = StopWatch.createStarted();

        EnumSet<QueryHint> hints = EnumSet.noneOf( QueryHint.class );
        if ( offset > 0 || limit > 0 ) {
            hints.add( QueryHint.PAGINATED );
        }

        Query query = this.getLoadValueObjectsQuery( filters, sort, hints );
        Query totalElementsQuery = getCountValueObjectsQuery( filters );

        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );

        StopWatch queryStopWatch = StopWatch.createStarted();
        List<?> list = query.list();
        queryStopWatch.stop();

        StopWatch postProcessingStopWatch = StopWatch.createStarted();
        List<VO> vos = list.stream()
                .map( this::processLoadValueObjectsQueryResult )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        postProcessingStopWatch.stop();

        StopWatch countingStopWatch = StopWatch.createStarted();
        Long totalElements = ( Long ) totalElementsQuery.uniqueResult();
        countingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > 20 ) {
            log.info( "Loading and counting VOs for " + elementClass.getName() + " took " + stopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms "
                    + "(querying: " + queryStopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms, "
                    + "counting: " + countingStopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms, "
                    + "post-processing: " + postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms)." );
        }

        return new Slice<>( vos, sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch queryStopWatch = StopWatch.create();
        StopWatch postProcessingStopWatch = StopWatch.create();

        Query query = this.getLoadValueObjectsQuery( filters, sort, EnumSet.noneOf( QueryHint.class ) );

        queryStopWatch.start();
        List<?> list = query.list();
        queryStopWatch.stop();

        postProcessingStopWatch.start();
        List<VO> vos = list.stream()
                .map( this::processLoadValueObjectsQueryResult )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        postProcessingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > 20 ) {
            log.info( "Loading VOs for " + elementClass.getName() + " took " + stopWatch.getTime( TimeUnit.MILLISECONDS ) + "ms ("
                    + "querying: " + queryStopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms, "
                    + "post-processing: " + postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms)." );
        }

        return vos;
    }
}
