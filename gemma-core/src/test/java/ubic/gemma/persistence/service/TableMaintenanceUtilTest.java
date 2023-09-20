package ubic.gemma.persistence.service;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.model.Gene2CsStatus;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.TestComponent;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class TableMaintenanceUtilTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
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

    private final ExternalDatabase gene2csDatabaseEntry = ExternalDatabase.Factory.newInstance( "gene2cs", DatabaseType.OTHER );

    private Session session;

    private SQLQuery query;

    private Path gene2csInfoPath;

    @Before
    public void setUp() throws IOException {
        when( externalDatabaseService.findByNameWithAuditTrail( "gene2cs" ) ).thenReturn( gene2csDatabaseEntry );
        query = mock( SQLQuery.class, RETURNS_SELF );
        session = mock( Session.class );
        when( session.createSQLQuery( any() ) ).thenReturn( query );
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
        gene2csInfoPath = Files.createTempDirectory( "DBReport" ).resolve( "gene2cs.info" );
        tableMaintenanceUtil.setGene2CsInfoPath( gene2csInfoPath );
    }

    @After
    public void tearDown() {
        reset( externalDatabaseService, sessionFactory, session, query );
        File f = gene2csInfoPath.toFile();
        if ( f.exists() ) {
            assertThat( f.delete() ).isTrue();
            assertThat( f.getParentFile().delete() ).isTrue();
        }
    }

    @Test
    public void test() {
        tableMaintenanceUtil.updateGene2CsEntries();
        // verify write to disk
        assertThat( gene2csInfoPath ).exists();
        verify( session ).createSQLQuery( startsWith( "REPLACE INTO GENE2CS" ) );
        verify( query ).addSynchronizedQuerySpace( "GENE2CS" );
        verify( query ).executeUpdate();
        verify( externalDatabaseService ).findByNameWithAuditTrail( "gene2cs" );
        verify( externalDatabaseService ).updateReleaseLastUpdated( eq( gene2csDatabaseEntry ), eq( "" ), any() );
        verify( mailEngine ).send( any() );
    }

    @Test
    public void testUpdateWhenTableIsFresh() throws IOException {
        Gene2CsStatus status = new Gene2CsStatus();
        status.setLastUpdate( new Date() ); // now! so nothing can be newer
        File statusFile = gene2csInfoPath.toFile();
        try ( ObjectOutputStream out = new ObjectOutputStream( Files.newOutputStream( statusFile.toPath() ) ) ) {
            out.writeObject( status );
        }
        tableMaintenanceUtil.updateGene2CsEntries();
        verifyNoInteractions( session );
        verifyNoInteractions( externalDatabaseService );
        verifyNoInteractions( mailEngine );
    }
}