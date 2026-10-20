package ubic.gemma.persistence.service.blacklist;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.blacklist.BlacklistedPlatform;
import ubic.gemma.core.context.TestComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ContextConfiguration
public class BlacklistedEntityDaoImplTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class BlacklistedEntityDaoContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public BlacklistedEntityDao blacklistedEntityDao( SessionFactory sessionFactory ) {
            return new BlacklistedEntityDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private BlacklistedEntityDao blacklistedEntityDao;

    @Test
    public void testDeleteAll() {
        BlacklistedPlatform bp = new BlacklistedPlatform();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance( "test", DatabaseType.OTHER );
        sessionFactory.getCurrentSession().persist( ed );
        bp.setExternalAccession( DatabaseEntry.Factory.newInstance( ed ) );
        bp = ( BlacklistedPlatform ) blacklistedEntityDao.create( bp );
        assertEquals( 1, blacklistedEntityDao.removeAll() );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        assertNull( sessionFactory.getCurrentSession().get( BlacklistedPlatform.class, bp.getId() ) );
        assertNull( sessionFactory.getCurrentSession().get( DatabaseEntry.class, bp.getExternalAccession().getId() ) );
    }
}