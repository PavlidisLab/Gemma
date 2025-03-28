package ubic.gemma.persistence.hibernate;

import lombok.extern.apachecommons.CommonsLog;
import net.sf.ehcache.Cache;
import org.apache.commons.io.file.PathUtils;
import org.hibernate.SessionFactory;
import org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory;
import org.hibernate.cfg.Settings;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.cache.EhcacheConfig;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.*;

@CommonsLog
@ContextConfiguration
@Category(SlowTest.class)
public class HibernateConfigTest extends BaseDatabaseTest {

    private static Path cacheDir;

    @BeforeClass
    public static void createCacheDirectory() throws IOException {
        cacheDir = Files.createTempDirectory( "gemma-cache" );
    }

    @AfterClass
    public static void removeCacheDirectory() throws IOException {
        PathUtils.deleteDirectory( cacheDir );
    }

    @Import(EhcacheConfig.class)
    @Configuration
    @TestComponent
    static class HibernateConfigTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.cache.dir=" + cacheDir );
        }

        @Override
        @DependsOn("ehcache")
        public LocalSessionFactoryBean sessionFactory( DataSource dataSource ) {
            LocalSessionFactoryBean factory = super.sessionFactory( dataSource );
            factory.getHibernateProperties()
                    .setProperty( "hibernate.cache.region.factory_class", SingletonEhCacheRegionFactory.class.getName() );
            factory.getHibernateProperties()
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
        assertEquals( 128, settings.getDefaultBatchFetchSize() );
        assertEquals( 128, settings.getJdbcFetchSize().intValue() );
        assertEquals( 32, settings.getJdbcBatchSize() );
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
