package ubic.gemma.persistence.service;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.Collection;
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
     * you must override {@link AbstractQueryFilteringVoEnabledDao#getValueObjectTransformer()}.
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

    private final TypedResultTransformer<O> DEFAULT_ENTITY_TRANSFORMER = new TypedResultTransformer<O>() {
        @Override
        @Nullable
        public O transformTuple( Object[] tuple, String[] aliases ) {
            //noinspection unchecked
            return ( O ) tuple[0];
        }

        @Override
        public List<O> transformListTyped( List<O> collection ) {
            return collection.stream()
                    .filter( Objects::nonNull )
                    .collect( Collectors.toList() );
        }
    };

    /**
     * Obtain a value object transformer for the result of {@link #getFilteringQuery(Filters, Sort)}.
     * <p>
     * By default, it will process the first element of the tuple with {@link #doLoadValueObjects(Collection)} and then
     * post-process the resulting VOs with {@link #postProcessValueObjects(List)}.
     */
    protected TypedResultTransformer<VO> getValueObjectTransformer() {
        TypedResultTransformer<O> entityTransformer = DEFAULT_ENTITY_TRANSFORMER;
        return new TypedResultTransformer<VO>() {

            @Override
            public VO transformTuple( Object[] tuple, String[] aliases ) {
                return doLoadValueObject( entityTransformer.transformTuple( tuple, aliases ) );
            }

            @Override
            public List<VO> transformListTyped( List<VO> collection ) {
                List<VO> results = collection.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
                postProcessValueObjects( results );
                return results;
            }
        };
    }

    private TypedResultTransformer<VO> getValueObjectTransformer( StopWatch postProcessingStopWatch ) {
        TypedResultTransformer<VO> transformer = getValueObjectTransformer();
        return new TypedResultTransformer<VO>() {
            @Override
            public VO transformTuple( Object[] tuple, String[] aliases ) {
                return transformer.transformTuple( tuple, aliases );
            }

            @Override
            public List<VO> transformListTyped( List<VO> collection ) {
                try {
                    postProcessingStopWatch.start();
                    return transformer.transformListTyped( collection );
                } finally {
                    postProcessingStopWatch.stop();
                }
            }
        };
    }

    @Override
    public List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch timer = StopWatch.createStarted();
        //noinspection unchecked
        List<Long> result = getFilteringIdQuery( filters ).list();
        timer.stop();
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d IDs for %s took %s ms.", result.size(), elementClass.getName(),
                    timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return result;
    }

    @Override
    public List<O> load( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch timer = StopWatch.createStarted();
        //noinspection unchecked
        List<O> result = getFilteringQuery( filters, sort )
                .setResultTransformer( DEFAULT_ENTITY_TRANSFORMER )
                .list();
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d entities for %s took %s ms.", result.size(), elementClass.getName(),
                    timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return result;
    }

    @Override
    public Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch timer = StopWatch.createStarted();
        Query query = this.getFilteringQuery( filters, sort );
        Query totalElementsQuery = getFilteringCountQuery( filters );
        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );
        //noinspection unchecked
        List<O> result = query
                .setResultTransformer( DEFAULT_ENTITY_TRANSFORMER )
                .list();
        Long totalElements = ( Long ) totalElementsQuery.uniqueResult();
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d entities for %s took %s ms.", result.size(), elementClass.getName(),
                    timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return new Slice<>( result, sort, offset, limit, totalElements );
    }

    @Override
    public Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch postProcessingStopWatch = StopWatch.create();

        Query query = this.getFilteringQuery( filters, sort );
        Query totalElementsQuery = getFilteringCountQuery( filters );

        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );

        //noinspection unchecked
        List<VO> list = query
                .setResultTransformer( getValueObjectTransformer( postProcessingStopWatch ) )
                .list();

        StopWatch countingStopWatch = StopWatch.createStarted();
        Long totalElements = ( Long ) totalElementsQuery.uniqueResult();
        countingStopWatch.stop();

        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading and counting %d VOs for %s took %d ms (querying: %d ms, counting: %d ms, post-processing: %d ms).",
                    list.size(),
                    elementClass.getName(),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ) - countingStopWatch.getTime( TimeUnit.MILLISECONDS ) - postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    countingStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return new Slice<>( list, sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
        StopWatch stopWatch = StopWatch.createStarted();
        StopWatch postProcessingStopWatch = StopWatch.create();
        //noinspection unchecked
        List<VO> results = this.getFilteringQuery( filters, sort )
                .setResultTransformer( getValueObjectTransformer( postProcessingStopWatch ) )
                .list();
        stopWatch.stop();
        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.info( String.format( "Loading %d VOs for %s took %dms (querying: %d ms, post-processing: %d ms).",
                    results.size(),
                    elementClass.getName(), stopWatch.getTime( TimeUnit.MILLISECONDS ),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ) - postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    postProcessingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return results;
    }

    @Override
    public long count( @Nullable Filters filters ) {
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
