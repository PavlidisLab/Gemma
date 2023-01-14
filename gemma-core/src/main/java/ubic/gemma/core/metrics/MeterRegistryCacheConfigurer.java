package ubic.gemma.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import net.sf.ehcache.Ehcache;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import ubic.gemma.core.metrics.binder.cache.EhCache24Metrics;

public class MeterRegistryCacheConfigurer implements InitializingBean {

    private final MeterRegistry registry;
    private final CacheManager cacheManager;

    public MeterRegistryCacheConfigurer( MeterRegistry registry, CacheManager cacheManager ) {
        this.registry = registry;
        this.cacheManager = cacheManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for ( String cacheName : cacheManager.getCacheNames() ) {
            Cache cache = cacheManager.getCache( cacheName );
            if ( cache.getNativeCache() instanceof Ehcache ) {
                EhCache24Metrics.monitor( registry, ( Ehcache ) cache.getNativeCache() );
            }
        }
    }
}
