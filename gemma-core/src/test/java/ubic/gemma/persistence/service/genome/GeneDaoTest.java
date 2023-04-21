package ubic.gemma.persistence.service.genome;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.TestComponent;

@ContextConfiguration
public class GeneDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class GeneDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        public GeneDao geneDao( SessionFactory sessionFactory, CacheManager cacheManager ) {
            return new GeneDaoImpl( sessionFactory, cacheManager );
        }
    }

    @Autowired
    private GeneDao geneDao;

    @Test
    public void testRemove() {
        Gene g = geneDao.create( Gene.Factory.newInstance() );
        geneDao.remove( g );
    }
}