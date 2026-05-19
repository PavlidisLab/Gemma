package ubic.gemma.persistence.service;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.hibernate.TypedResultTransformer;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on Hibernate {@link Query}.
 *
 * @author poirigui
 */
public abstract class AbstractQueryFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractFilteringVoEnabledDao<O, VO> implements CachedFilteringVoEnabledDao<O, VO> {

    protected AbstractQueryFilteringVoEnabledDao( String objectAlias, Class<O> elementClass, SessionFactory sessionFactory ) {
        super( objectAlias, elementClass, sessionFactory );
    }

    /**
     * Produce a query for retrieving filtered entities or VOs after applying filters and ordering.
     * <p>
     * The query is expected to return a {@link List} of {@link O}. Implementations that return tuples should also
     * override {@link #getValueObjectTransformer()}.
     */
    protected abstract Query<?> getFilteringQuery( @Nullable Filters filters, @Nullable Sort sort );

    /**
     * Initialize a result from {@link #getFilteringQuery(Filters, Sort)} retrieved from cache. Lazy-loaded relations
     * fetched by the filtering query must be initialized here so that VOs populated from the second-level cache have
     * the expected fields.
     */
    protected abstract void initializeCachedFilteringResult( O cachedEntity );

    protected Query<?> getFilteringIdQuery( @Nullable Filters filters, @Nullable Sort sort ) {
        throw new NotImplementedException( "Retrieving IDs for " + getElementClass() + " is not supported." );
    }

    protected Query<?> getFilteringCountQuery( @Nullable Filters filters ) {
        throw new NotImplementedException( "Counting " + getElementClass() + " is not supported." );
    }

    /**
     * Name of the Hibernate L2 query-cache region to use for filtered-VO queries from this DAO,
     * or {@code null} to use Hibernate's default {@code StandardQueryCache} region.
     * <p>
     * Subclasses with a hot filter path (e.g. {@code ExpressionExperimentDaoImpl}) override this
     * to shard their cache entries into a dedicated, separately-bounded region declared in
     * {@code EhcacheConfig}. Shared sharding here is safe because each subclass's
     * {@link #getFilteringQuery(Filters, Sort)} already produces type-specific HQL — there is no
     * risk of two DAO types colliding on the same cache key.
     * <p>
     * Default: {@code null} (fall back to {@code StandardQueryCache}). See
     * {@code HIBERNATE_L2_CACHE_AUDIT.md} recommendation #4 for context.
     */
    @Nullable
    protected String getQueryCacheRegion() {
        return null;
    }

    /**
     * Apply {@link Query#setCacheable(boolean)} and, when a region is declared via
     * {@link #getQueryCacheRegion()}, {@link Query#setCacheRegion(String)} so the cached
     * result lands in the sharded region rather than the shared default.
     */
    private <Q extends Query<?>> Q applyCacheRegion( Q query, boolean cacheable ) {
        query.setCacheable( cacheable );
        if ( cacheable ) {
            String region = getQueryCacheRegion();
            if ( region != null ) {
                query.setCacheRegion( region );
            }
        }
        return query;
    }

    private final TypedResultTransformer<O> DEFAULT_ENTITY_TRANSFORMER = new TypedResultTransformer<O>() {
        @Override
        @Nullable
        public O transformTuple( Object[] tuple, String[] aliases ) {
            //noinspection unchecked
            O entity = ( O ) tuple[0];
            if ( entity != null ) {
                initializeCachedFilteringResult( entity );
            }
            return entity;
        }

        @Override
        public List<O> transformListTyped( List<O> collection ) {
            return collection.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
        }
    };

    protected TypedResultTransformer<O> getEntityTransformer() {
        return DEFAULT_ENTITY_TRANSFORMER;
    }

    protected TypedResultTransformer<VO> getValueObjectTransformer() {
        TypedResultTransformer<O> entityTransformer = getEntityTransformer();
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

    @Override
    public List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort ) {
        return doLoadIdsWithCache( filters, sort, false );
    }

    @Override
    public List<Long> loadIdsWithCache( @Nullable Filters filters, @Nullable Sort sort ) {
        return doLoadIdsWithCache( filters, sort, true );
    }

    @Override
    public List<O> load( @Nullable Filters filters, @Nullable Sort sort ) {
        return doLoadWithCache( filters, sort, false );
    }

    @Override
    public Slice<O> loadWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return doLoadWithCache( filters, sort, offset, limit, true );
    }

    @Override
    public Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return doLoadWithCache( filters, sort, offset, limit, false );
    }

    @Override
    public List<O> loadWithCache( @Nullable Filters filters, @Nullable Sort sort ) {
        return doLoadWithCache( filters, sort, true );
    }

    @Override
    public Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return doLoadValueObjectsWithCache( filters, sort, offset, limit, false );
    }

    @Override
    public Slice<VO> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return doLoadValueObjectsWithCache( filters, sort, offset, limit, true );
    }

    @Override
    public List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
        return doLoadValueObjectsWithCache( filters, sort, false );
    }

    @Override
    public List<VO> loadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort ) {
        return doLoadValueObjectsWithCache( filters, sort, true );
    }

    @Override
    public long count( @Nullable Filters filters ) {
        return doCountWithCache( filters, false );
    }

    @Override
    public long countWithCache( @Nullable Filters filters ) {
        return doCountWithCache( filters, true );
    }

    private List<Long> doLoadIdsWithCache( @Nullable Filters filters, @Nullable Sort sort, boolean cacheable ) {
        StopWatch timer = StopWatch.createStarted();
        //noinspection unchecked
        List<Long> result = ( List<Long> ) applyCacheRegion( getFilteringIdQuery( filters, sort ), cacheable ).list();
        timer.stop();
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d IDs for %s took %s ms.", result.size(), getElementClass().getName(),
                    timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return result;
    }

    private List<O> doLoadWithCache( @Nullable Filters filters, @Nullable Sort sort, boolean cacheable ) {
        StopWatch timer = StopWatch.createStarted();
        List<?> rows = applyCacheRegion( getFilteringQuery( filters, sort ), cacheable ).list();
        List<O> result = getEntityTransformer().applyTo( rows );
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d entities for %s took %s ms.", result.size(), getElementClass().getName(),
                    timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return result;
    }

    private Slice<O> doLoadWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit, boolean cacheable ) {
        StopWatch timer = StopWatch.createStarted();
        Query<?> query = this.getFilteringQuery( filters, sort );
        Query<?> totalElementsQuery = getFilteringCountQuery( filters );
        if ( offset > 0 ) query.setFirstResult( offset );
        if ( limit > 0 ) query.setMaxResults( limit );
        List<?> rows = applyCacheRegion( query, cacheable ).list();
        List<O> result = getEntityTransformer().applyTo( rows );
        StopWatch countingStopWatch = StopWatch.createStarted();
        Long totalElements;
        if ( limit > 0 && ( result.isEmpty() || result.size() == limit ) ) {
            totalElements = ( Long ) applyCacheRegion( totalElementsQuery, cacheable ).uniqueResult();
        } else {
            totalElements = offset + ( long ) result.size();
        }
        countingStopWatch.stop();
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading and counting %d entities for %s took %s ms (querying: %d ms, counting: %d ms).",
                    result.size(), getElementClass().getName(), timer.getTime( TimeUnit.MILLISECONDS ),
                    timer.getTime( TimeUnit.MILLISECONDS ) - countingStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    countingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return new Slice<>( result, sort, offset, limit, totalElements );
    }

    private List<VO> doLoadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, boolean cacheable ) {
        StopWatch stopWatch = StopWatch.createStarted();
        List<?> rows = applyCacheRegion( this.getFilteringQuery( filters, sort ), cacheable ).list();
        List<VO> results = getValueObjectTransformer().applyTo( rows );
        stopWatch.stop();
        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading %d VOs for %s took %dms.", results.size(), getElementClass().getName(),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return results;
    }

    private Slice<VO> doLoadValueObjectsWithCache( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit, boolean cacheable ) {
        StopWatch stopWatch = StopWatch.createStarted();
        Query<?> query = this.getFilteringQuery( filters, sort );
        Query<?> totalElementsQuery = getFilteringCountQuery( filters );
        if ( offset > 0 ) query.setFirstResult( offset );
        if ( limit > 0 ) query.setMaxResults( limit );
        List<?> rows = applyCacheRegion( query, cacheable ).list();
        List<VO> list = getValueObjectTransformer().applyTo( rows );

        StopWatch countingStopWatch = StopWatch.createStarted();
        Long totalElements;
        if ( limit > 0 && ( list.isEmpty() || list.size() == limit ) ) {
            totalElements = ( Long ) applyCacheRegion( totalElementsQuery, cacheable ).uniqueResult();
        } else {
            totalElements = offset + ( long ) list.size();
        }
        countingStopWatch.stop();
        stopWatch.stop();

        if ( stopWatch.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "Loading and counting %d VOs for %s took %d ms (querying: %d ms, counting: %d ms).",
                    list.size(), getElementClass().getName(), stopWatch.getTime( TimeUnit.MILLISECONDS ),
                    stopWatch.getTime( TimeUnit.MILLISECONDS ) - countingStopWatch.getTime( TimeUnit.MILLISECONDS ),
                    countingStopWatch.getTime( TimeUnit.MILLISECONDS ) ) );
        }
        return new Slice<>( list, sort, offset, limit, totalElements );
    }

    private long doCountWithCache( @Nullable Filters filters, boolean cacheable ) {
        StopWatch timer = StopWatch.createStarted();
        try {
            return ( Long ) requireNonNull( applyCacheRegion( this.getFilteringCountQuery( filters ), cacheable ).uniqueResult(),
                    String.format( "Counting query for %s returned null.", getElementClass().getName() ) );
        } finally {
            timer.stop();
            if ( timer.getTime( TimeUnit.MILLISECONDS ) > REPORT_SLOW_QUERY_AFTER_MS ) {
                log.warn( String.format( "Count VOs for %s took %d ms.", getElementClass().getName(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
            }
        }
    }
}
