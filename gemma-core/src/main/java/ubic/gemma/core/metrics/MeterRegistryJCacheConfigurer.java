package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;

import javax.cache.Cache;
import javax.cache.CacheManager;

/**
 * Bind per-cache JSR-107 (JCache) metrics from a {@link CacheManager} to a {@link MeterRegistry}.
 * <p>
 * Replaces the Ehcache 2.x {@code MeterRegistryEhcacheConfigurer} that was retired together with
 * the legacy Ehcache 2 stack in Phase 2. The current cache backend is Ehcache 3 via JSR-107
 * (see {@link ubic.gemma.persistence.cache.EhcacheConfig}), so per-cache metrics are now
 * surfaced through Micrometer's {@link JCacheMetrics} binder which reads {@code CacheMXBean}
 * statistics from the underlying JCache provider.
 * <p>
 * Each named cache registered with the supplied {@link CacheManager} at context-refresh time
 * is monitored. Caches created later (none in current code paths) would not be picked up;
 * if that changes, switch to a {@link javax.cache.event.CacheEntryListener} or per-cache
 * lazy registration.
 *
 * @author poirigui
 * @see JCacheMetrics
 * @see ubic.gemma.persistence.cache.EhcacheConfig
 */
public class MeterRegistryJCacheConfigurer extends AbstractMeterRegistryConfigurer {

    private final CacheManager cacheManager;

    public MeterRegistryJCacheConfigurer( MeterRegistry registry, CacheManager cacheManager ) {
        super( registry );
        this.cacheManager = cacheManager;
    }

    @Override
    protected void configure( MeterRegistry registry ) {
        for ( String cacheName : cacheManager.getCacheNames() ) {
            Cache<?, ?> cache = cacheManager.getCache( cacheName );
            if ( cache != null ) {
                JCacheMetrics.monitor( registry, cache, Tags.empty() );
            }
        }
    }
}
