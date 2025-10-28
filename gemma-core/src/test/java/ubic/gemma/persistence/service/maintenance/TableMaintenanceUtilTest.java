package ubic.gemma.persistence.service.maintenance;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.mail.MailEngine;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class TableMaintenanceUtilTest extends BaseTest {

    @Configuration
    @TestComponent
    static class TableMaintenanceUtilTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
            Path gene2csInfoPath = Files.createTempDirectory( "DBReport" ).resolve( "gene2cs.info" );
            return new TestPropertyPlaceholderConfigurer( "gemma.gene2cs.path=" + gene2csInfoPath );
        }

        /**
         * Needed to convert {@link String} to {@link Path}.
         */
        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

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

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
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

    @Value("${gemma.gene2cs.path}")
    private Path gene2csInfoPath;

    private final ExternalDatabase gene2csDatabaseEntry = ExternalDatabase.Factory.newInstance( ExternalDatabases.GENE2CS, DatabaseType.OTHER );

    private Session session;

    private SQLQuery query;

    @Before
    public void setUp() throws IOException {
        when( externalDatabaseService.findByNameWithAuditTrail( ExternalDatabases.GENE2CS ) ).thenReturn( gene2csDatabaseEntry );
        query = mock( SQLQuery.class, RETURNS_SELF );
        session = mock( Session.class );
        when( session.createSQLQuery( any() ) ).thenReturn( query );
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
    }

    @After
    public void tearDown() throws IOException {
        reset( externalDatabaseService, sessionFactory, session, query );
        Path f = gene2csInfoPath;
        if ( Files.exists( f ) ) {
            Files.delete( f );
            Files.delete( f.getParent() );
        }
    }

    @Test
    public void test() {
        tableMaintenanceUtil.updateGene2CsEntries();
        // verify write to disk
        assertThat( gene2csInfoPath ).exists();
        verify( session ).createSQLQuery( startsWith( "insert into GENE2CS" ) );
        verify( query ).addSynchronizedQuerySpace( "GENE2CS" );
        verify( query ).executeUpdate();
        verify( externalDatabaseService ).findByNameWithAuditTrail( ExternalDatabases.GENE2CS );
        verify( externalDatabaseService ).updateReleaseLastUpdated( eq( gene2csDatabaseEntry ), eq( "No Gene2Cs status exists on disk." ), any() );
        verify( mailEngine ).sendMessageToAdmin( any(), any() );
    }

    @Test
    public void testUpdateWhenTableIsFresh() throws IOException {
        Gene2CsStatus status = new Gene2CsStatus();
        status.setLastUpdate( new Date() ); // now! so nothing can be newer
        try ( ObjectOutputStream out = new ObjectOutputStream( Files.newOutputStream( gene2csInfoPath ) ) ) {
            out.writeObject( status );
        }
        tableMaintenanceUtil.updateGene2CsEntries();
        verifyNoInteractions( session );
        verifyNoInteractions( externalDatabaseService );
        verifyNoInteractions( mailEngine );
    }
}