package ubic.gemma.core.util.test;

import lombok.extern.apachecommons.CommonsLog;
import net.sf.ehcache.Cache;
import org.hibernate.SessionFactory;
import org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory;
import org.hibernate.cache.ehcache.internal.nonstop.NonstopAwareEntityRegionAccessStrategy;
import org.hibernate.cache.ehcache.internal.strategy.ReadOnlyEhcacheEntityRegionAccessStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.util.TestComponent;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.Assert.*;

@CommonsLog
@Category(SlowTest.class)
@ContextConfiguration
public class HibernateConfigTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class HibernateConfigTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public FactoryBean<net.sf.ehcache.CacheManager> cacheManager() {
            EhCacheManagerFactoryBean bean = new EhCacheManagerFactoryBean();
            bean.setShared( true );
            return bean;
        }

        @Override
        public FactoryBean<SessionFactory> sessionFactory( DataSource dataSource ) {
            FactoryBean<SessionFactory> factory = super.sessionFactory( dataSource );
            ( ( LocalSessionFactoryBean ) factory ).getHibernateProperties()
                    .setProperty( "hibernate.cache.region.factory_class", SingletonEhCacheRegionFactory.class.getName() );
            ( ( LocalSessionFactoryBean ) factory ).getHibernateProperties()
                    .setProperty( "hibernate.cache.use_second_level_cache", "true" );
            return factory;
        }
    }

    @Autowired
    private net.sf.ehcache.CacheManager cacheManager;

    @Test
    public void test() {
        Settings settings = ( ( SessionFactoryImpl ) sessionFactory ).getSettings();
        assertEquals( 3, settings.getMaximumFetchDepth().intValue() );
        assertEquals( 100, settings.getDefaultBatchFetchSize() );
        assertTrue( settings.isJdbcBatchVersionedData() );
        assertTrue( settings.isOrderInsertsEnabled() );
        assertTrue( settings.isOrderUpdatesEnabled() );
    }

    @Test
    public void testCacheConfigurations() {
        for ( Map.Entry<String, ClassMetadata> entry : sessionFactory.getAllClassMetadata().entrySet() ) {
            String name = entry.getKey();
            ClassMetadata metadata = entry.getValue();
            if ( metadata instanceof EntityPersister && ( ( EntityPersister ) metadata ).hasCache() ) {
                EntityPersister entityPersister = ( EntityPersister ) metadata;
                Cache cache = cacheManager.getCache( metadata.getEntityName() );
                if ( cache == null ) {
                    continue;
                }
                assertNotNull( String.format( "%s is cacheable and lacks an entry in ehcache.xml", name ),
                        cache );
                assertTrue( String.format( "%s is mutable, its cache shouldn't be eternal.", name ),
                        !metadata.isMutable() || !cache.getCacheConfiguration().isEternal() );
                assertTrue( String.format( "%s is immutable, its cache should be eternal.", name ),
                        metadata.isMutable() || cache.getCacheConfiguration().isEternal() );
            }
        }
    }
}
