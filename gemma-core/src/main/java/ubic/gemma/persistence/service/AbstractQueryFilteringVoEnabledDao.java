package ubic.gemma.persistence.service;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.FilterQueryUtils;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the Hibernate {@link Query} API.
 *
 * @see FilterQueryUtils for utilities to generate HQL clauses from a {@link Filters}
 * @see ubic.gemma.persistence.util.AclQueryUtils for utilities to generate ACL clauses to filter VOs by ACL at the
 * database-level
 * @see AbstractCriteriaFilteringVoEnabledDao as an alternative
 *
 * @author poirigui
 */
public abstract class AbstractQueryFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledDao<O, VO> {

    protected AbstractQueryFilteringVoEnabledDao( String objectAlias, Class<O> elementClass, SessionFactory sessionFactory ) {
        super( objectAlias, elementClass, sessionFactory );
    }

    /**
     * Produce a query for retrieving value objects after applying a set of filters and a given ordering.
     * <p>
     * Note that if your implementation does not produce a {@link List} of {@link O} when {@link Query#list()} is invoked,
     * you must override {@link AbstractQueryFilteringVoEnabledDao#processFilteringQueryResultToValueObject(Object)}.
     *
     * @return a {@link Query} that produce a list of {@link O}
     */
    protected abstract Query getFilteringQuery( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * Produce a query that will be used to retrieve IDs of {@link #getFilteringQuery(Filters, Sort)}.
     */
    protected Query getFilteringIdQuery( @Nullable Filters filters ) {
        throw new NotImplementedException( "Retrieving IDs for " + elementClass + " is not supported." );
    }

    /**
     * Produce a query that will be used to retrieve the size of {@link #getFilteringQuery(Filters, Sort)}.
     * @return a {@link Query} which must return a single {@link Long} value
     */
    protected Query getFilteringCountQuery( @Nullable Filters filters ) {
        throw new NotImplementedException( "Counting " + elementClass + " is not supported." );
    }

    /**
     * Process a properties from {@link #getFilteringQuery(Filters, Sort)} into a {@link O}.
     * <p>
     * The default is to simply cast the properties to {@link O}, assuming that it is the only return value of the query.
     */
    protected O processFilteringQueryResultToEntity( Object result ) {
        //noinspection unchecked
        return ( O ) result;
    }

    /**
     * Process a properties from {@link #getFilteringQuery(Filters, Sort)} into a {@link VO} value object.
     * <p>
     * The properties is obtained from {@link Query#list()}.
     * <p>
     * By default, it will process the properties with {@link #processFilteringQueryResultToEntity(Object)} and then apply
     * {@link #doLoadValueObject(Identifiable)} to obtain a value object.
     *
     * @return a value object, or null, and it will be ignored when constructing the {@link Slice} in {@link #loadValueObjectsPreFilter(Filters, Sort, int, int)}
     */
    protected VO processFilteringQueryResultToValueObject( Object result ) {
        return doLoadValueObject( processFilteringQueryResultToEntity( result ) );
    }

    /**
     * FIXME: this could be far more efficient with a specialized query
     */
    @Override
    public List<Long> loadIdsPreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch timer = StopWatch.createStarted();
        //noinspection unchecked
        List<Long> result = getFilteringIdQuery( filters ).list();
        timer.stop();
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d IDs for %s took %s ms.", result.size(), elementClass.getName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return result;
    }

    @Override
    public List<O> loadPreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch timer = StopWatch.createStarted();
        //noinspection unchecked
        List<Object> result = getFilteringQuery( filters, sort ).list();
        List<O> r = result.stream()
                .map( this::processFilteringQueryResultToEntity )
                .collect( Collectors.toList() );
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d entities for %s took %s ms.", r.size(), elementClass.getName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return r;
    }

    @Override
    public Slice<O> loadPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch timer = StopWatch.createStarted();
        Query query = this.getFilteringQuery( filters, sort );
        Query totalElementsQuery = getFilteringCountQuery( filters );
        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );
        List<?> result = getFilteringQuery( filters, sort ).list();
        List<O> os = result.stream()
                .map( this::processFilteringQueryResultToEntity )
                .collect( Collectors.toList() );
        Long totalElements = ( Long ) totalElementsQuery.uniqueResult();
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d entities for %s took %s ms.", os.size(), elementClass.getName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return new Slice<>( os, sort, offset, limit, totalElements );
    }

    @Override
    public Slice<VO> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch stopWatch = StopWatch.createStarted();

        Query query = this.getFilteringQuery( filters, sort );
        Query totalElementsQuery = getFilteringCountQuery( filters );

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
                .map( this::processFilteringQueryResultToValueObject )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        postProcessingStopWatch.stop();

        StopWatch countingStopWatch = StopWatch.createStarted();
        Long totalElements = ( Long ) totalElementsQuery.uniqueResult();
        countingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( "Loading and counting VOs for " + elementClass.getName() + " took " + stopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms "
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

        Query query = this.getFilteringQuery( filters, sort );

        queryStopWatch.start();
        List<?> list = query.list();
        queryStopWatch.stop();

        postProcessingStopWatch.start();
        List<VO> vos = list.stream()
                .map( this::processFilteringQueryResultToValueObject )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        postProcessingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.info( "Loading VOs for " + elementClass.getName() + " took " + stopWatch.getTime( TimeUnit.MILLISECONDS ) + "ms ("
                    + "querying: " + queryStopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms, "
                    + "post-processing: " + postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) + " ms)." );
        }

        return vos;
    }

    @Override
    public long countPreFilter( @Nullable Filters filters ) {
        StopWatch timer = StopWatch.createStarted();
        try {
            return ( Long ) this.getFilteringCountQuery( filters ).uniqueResult();
        } finally {
            timer.stop();
            if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
                log.info( String.format( "Count VOs for %s took %d ms.",
                        elementClass.getName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
            }
        }
    }
}
