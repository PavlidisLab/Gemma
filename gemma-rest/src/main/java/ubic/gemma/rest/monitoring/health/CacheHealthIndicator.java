package ubic.gemma.rest.monitoring.health;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache health probe. Confirms the Spring {@link CacheManager} bean is reachable and reports
 * how many caches it currently knows about. Does not probe individual caches (that would skew
 * hit/miss ratios visible in metrics).
 *
 * @author Phase 3 actuator wiring
 */
@Component
@CommonsLog
public class CacheHealthIndicator implements HealthIndicator {

    private final CacheManager cacheManager;

    @Autowired
    public CacheHealthIndicator( CacheManager cacheManager ) {
        this.cacheManager = cacheManager;
    }

    @Override
    public String getName() {
        return "cache";
    }

    @Override
    public HealthResult check() {
        try {
            Collection<String> names = cacheManager.getCacheNames();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put( "cacheManager", cacheManager.getClass().getSimpleName() );
            details.put( "caches", names != null ? names.size() : 0 );
            return HealthResult.up( details );
        } catch ( RuntimeException e ) {
            return HealthResult.down( "CacheManager probe failed: " + e.getMessage() );
        }
    }
}
