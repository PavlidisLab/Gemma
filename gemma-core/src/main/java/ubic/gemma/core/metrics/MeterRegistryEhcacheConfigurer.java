package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.EhCache2Metrics;
import net.sf.ehcache.Ehcache;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Add metrics from each available {@link Ehcache} in the given {@link CacheManager} to the supplied meter registry.
 * @author poirigui
 * @see EhCache2Metrics
 */
public class MeterRegistryEhcacheConfigurer extends AbstractMeterRegistryConfigurer {

    private final CacheManager cacheManager;

    public MeterRegistryEhcacheConfigurer( MeterRegistry registry, CacheManager cacheManager ) {
        super( registry );
        this.cacheManager = cacheManager;
    }

    @Override
    public void configure( MeterRegistry registry ) {
        for ( String cacheName : cacheManager.getCacheNames() ) {
            Cache cache = cacheManager.getCache( cacheName );
            if ( cache.getNativeCache() instanceof Ehcache ) {
                EhCache2Metrics.monitor( registry, ( Ehcache ) cache.getNativeCache() );
            }
        }
    }
}
