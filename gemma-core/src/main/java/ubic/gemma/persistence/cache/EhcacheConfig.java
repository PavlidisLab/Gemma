package ubic.gemma.persistence.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration. EhCache 2 was retired during the Spring 6 / Hibernate 6
 * migration; this stub exposes a Spring {@link ConcurrentMapCacheManager}
 * keyed by the bean name {@code ehcache} so that legacy XML wiring that
 * declares {@code depends-on="ehcache"} still resolves. A proper EhCache 3
 * (via JCache) or Caffeine integration will replace this in a follow-up phase.
 */
@Configuration
public class EhcacheConfig {

    @Bean(name = "ehcache")
    public CacheManager ehcache() {
        return new ConcurrentMapCacheManager();
    }
}
