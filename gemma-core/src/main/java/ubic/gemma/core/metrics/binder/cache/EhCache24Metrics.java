package ubic.gemma.core.metrics.binder.cache;

import io.micrometer.common.lang.Nullable;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.StatisticsGateway;

import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Metrics for Ehcache 2.x.
 * <p>
 * Originally written against EhCache 2.4's {@code net.sf.ehcache.Statistics}. Renovated to use the 2.5+
 * {@link StatisticsGateway} (which {@link Ehcache#getStatistics()} now returns) so it works against the 2.10.x line
 * pulled in by hibernate-ehcache 5.6.
 *
 * @author poirigui
 * @see io.micrometer.core.instrument.binder.cache.EhCache2Metrics
 */
public class EhCache24Metrics extends CacheMeterBinder<Ehcache> {

    public EhCache24Metrics( Ehcache cache, Iterable<Tag> tags ) {
        super( cache, cache.getName(), tags );
    }

    public static Ehcache monitor( MeterRegistry registry, Ehcache cache, String... tags ) {
        return monitor( registry, cache, Tags.of( tags ) );
    }

    public static Ehcache monitor( MeterRegistry registry, Ehcache cache, Iterable<Tag> tags ) {
        new EhCache24Metrics( cache, tags ).bindTo( registry );
        return cache;
    }

    @Override
    protected Long size() {
        return getOrDefault( StatisticsGateway::getSize, null );
    }

    @Override
    protected long hitCount() {
        return getOrDefault( StatisticsGateway::cacheHitCount, 0L );
    }

    @Override
    protected Long missCount() {
        return getOrDefault( StatisticsGateway::cacheMissCount, null );
    }

    @Override
    protected Long evictionCount() {
        return getOrDefault( StatisticsGateway::cacheEvictedCount, null );
    }

    @Override
    protected long putCount() {
        return 0L;
    }

    @Override
    protected void bindImplementationSpecificMetrics( MeterRegistry registry ) {
    }

    @Nullable
    private StatisticsGateway getStats() {
        Ehcache cache = getCache();
        return cache != null ? cache.getStatistics() : null;
    }

    @Nullable
    private Long getOrDefault( Function<StatisticsGateway, Long> function, @Nullable Long defaultValue ) {
        StatisticsGateway ref = getStats();
        if ( ref != null ) {
            return function.apply( ref );
        }
        return defaultValue;
    }

    private long getOrDefault( ToLongFunction<StatisticsGateway> function, long defaultValue ) {
        StatisticsGateway ref = getStats();
        if ( ref != null ) {
            return function.applyAsLong( ref );
        }
        return defaultValue;
    }
}
