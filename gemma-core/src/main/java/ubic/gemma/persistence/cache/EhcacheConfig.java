package ubic.gemma.persistence.cache;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EhcacheConfig {

    private static final String EHCACHE_DISK_STORE_DIR = "ehcache.disk.store.dir";

    /**
     * The ehcache.xml assume that {@code -Dehcache.disk.store.dir} is set. If not set explicitly as a JVM option, we
     * retrieve the value from the {@code gemma.cache.dir} configuration.
     * <p>
     * The Ehcache cache manager is shared so that it can be reused by Hibernate.
     * <p>
     * This definition is also reused by gsec.
     */
    @Bean
    public FactoryBean<net.sf.ehcache.CacheManager> ehcache( @Value("${gemma.cache.dir}") String gemmaCacheDir ) {
        String cacheDir;
        if ( ( cacheDir = System.getProperty( EHCACHE_DISK_STORE_DIR ) ) == null ) {
            System.setProperty( EHCACHE_DISK_STORE_DIR, gemmaCacheDir );
            cacheDir = gemmaCacheDir;
        }
        if ( StringUtils.isBlank( cacheDir ) ) {
            throw new RuntimeException( String.format( "The cache directory is set, either provide it via -D%s or set gemma.cache.dir in your Gemma.properties.",
                    EHCACHE_DISK_STORE_DIR ) );
        }
        EhCacheManagerFactoryBean bean = new EhCacheManagerFactoryBean();
        bean.setShared( true );
        return bean;
    }
}
