package ubic.gemma.core.metrics.binder.cache;

import io.micrometer.common.lang.Nullable;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;

import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Metrics for Ehcache 2.4 series.
 *
 * @author poirigui
 * @see io.micrometer.core.instrument.binder.cache.EhCache2Metrics
 */
public class EhCache24Metrics extends CacheMeterBinder<Ehcache> {

    public EhCache24Metrics( Ehcache cache, Iterable<Tag> tags ) {
        super( cache, cache.getName(), tags );
    }

    /**
     * Record metrics on an EhCache cache.
     * @param registry The registry to bind metrics to.
     * @param cache The cache to instrument.
     * @param tags Tags to apply to all recorded metrics. Must be an even number of
     * arguments representing key/value pairs of tags.
     * @return The instrumented cache, unchanged. The original cache is not wrapped or
     * proxied in any way.
     */
    public static Ehcache monitor( MeterRegistry registry, Ehcache cache, String... tags ) {
        return monitor( registry, cache, Tags.of( tags ) );
    }

    /**
     * Record metrics on an EhCache cache.
     * @param registry The registry to bind metrics to.
     * @param cache The cache to instrument.
     * @param tags Tags to apply to all recorded metrics.
     * @return The instrumented cache, unchanged. The original cache is not wrapped or
     * proxied in any way.
     */
    public static Ehcache monitor( MeterRegistry registry, Ehcache cache, Iterable<Tag> tags ) {
        new EhCache24Metrics( cache, tags ).bindTo( registry );
        return cache;
    }

    @Override
    protected Long size() {
        return getOrDefault( Statistics::getObjectCount, null );
    }

    @Override
    protected long hitCount() {
        return getOrDefault( Statistics::getCacheHits, 0L );
    }

    @Override
    protected Long missCount() {
        return getOrDefault( Statistics::getCacheMisses, null );
    }

    @Override
    protected Long evictionCount() {
        return getOrDefault( Statistics::getEvictionCount, null );
    }

    @Override
    protected long putCount() {
        return 0L;
    }

    @Override
    protected void bindImplementationSpecificMetrics( MeterRegistry registry ) {
    }

    @Nullable
    private Statistics getStats() {
        Ehcache cache = getCache();
        return cache != null ? cache.getStatistics() : null;
    }

    @Nullable
    private Long getOrDefault( Function<Statistics, Long> function, @Nullable Long defaultValue ) {
        Statistics ref = getStats();
        if ( ref != null ) {
            return function.apply( ref );
        }

        return defaultValue;
    }

    private long getOrDefault( ToLongFunction<Statistics> function, long defaultValue ) {
        Statistics ref = getStats();
        if ( ref != null ) {
            return function.applyAsLong( ref );
        }

        return defaultValue;
    }

}
