package ubic.gemma.core.apps;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.TestComponent;

import static org.mockito.Mockito.*;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class NCBIGene2GOAssociationLoaderCLITest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class NCBIGene2GOAssociationLoaderCLITestContextConfiguration {

        @Bean
        public NCBIGene2GOAssociationLoaderCLI ncbiGene2GOAssociationLoaderCLI() {
            return new NCBIGene2GOAssociationLoaderCLI();
        }

        @Bean
        public TaxonService taxonService() {
            return mock( TaxonService.class );
        }

        @Bean
        public Gene2GOAssociationService gene2GOAssociationService() {
            return mock( Gene2GOAssociationService.class );
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock( ExternalDatabaseService.class );
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public PersisterHelper persisterHelper() {
            return mock();
        }
    }

    @Autowired
    private NCBIGene2GOAssociationLoaderCLI ncbiGene2GOAssociationLoaderCLI;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Test
    @Ignore("This test is too slow, see https://github.com/PavlidisLab/Gemma/issues/1056 for details")
    @Category(SlowTest.class)
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void test() throws Exception {
        assumeThatResourceIsAvailable( "ftp://ftp.ncbi.nih.gov/gene/DATA/gene2go.gz" );
        ExternalDatabase gene2go = ExternalDatabase.Factory.newInstance( "go", DatabaseType.OTHER );
        when( externalDatabaseService.findByNameWithAuditTrail( "go" ) ).thenReturn( gene2go );
        ncbiGene2GOAssociationLoaderCLI.executeCommand( new String[] {} );
        verify( gene2GOAssociationService ).removeAll();
        verify( externalDatabaseService ).findByNameWithAuditTrail( "go" );
        verify( externalDatabaseService ).updateReleaseLastUpdated( same( gene2go ), isNull(), any() );
    }
}