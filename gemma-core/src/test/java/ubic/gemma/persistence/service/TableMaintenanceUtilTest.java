package ubic.gemma.persistence.service;

import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.Settings;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class TableMaintenanceUtilTest {

    @Configuration
    static class TableMaintenanceUtilTestContextConfiguration {

        @Bean
        public TableMaintenanceUtil tableMaintenanceUtil() {
            return new TableMaintenanceUtilImpl();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock( ExternalDatabaseService.class );
        }

        @Bean
        public MailEngine mailEngine() {
            return mock( MailEngine.class );
        }

        @Bean
        public SessionFactory sessionFactory() {
            return mock( SessionFactory.class );
        }
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private MailEngine mailEngine;

    private final ExternalDatabase gene2csDatabaseEntry = ExternalDatabase.Factory.newInstance( "gene2cs", DatabaseType.OTHER );

    @Mock
    private org.hibernate.classic.Session session;

    @Before
    public void setUp() {
        when( externalDatabaseService.findByName( "gene2cs" ) ).thenReturn( gene2csDatabaseEntry );
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
        when( session.createSQLQuery( any() ) ).thenReturn( mock( SQLQuery.class ) );
    }

    @Test
    public void test() {
        tableMaintenanceUtil.updateGene2CsEntries();
        assertThat( gene2csDatabaseEntry.getAuditTrail() ).isNotNull();
        // verify write to disk
        assertThat( Paths.get( Settings.getString( "gemma.appdata.home" ), "DbReports", "gene2cs.info" ) )
                .exists();
        verify( externalDatabaseService ).update( gene2csDatabaseEntry );
        verify( mailEngine ).send( any() );
    }

}