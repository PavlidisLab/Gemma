package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.core.util.test.TestAuthenticationUtilsImpl;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import static org.mockito.Mockito.*;

@ContextConfiguration
public class NCBIGene2GOAssociationLoaderCLITest extends BaseCliTest {

    @Configuration
    static class NCBIGene2GOAssociationLoaderCLITestContextConfiguration extends BaseCliTestContextConfiguration {

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
        public TestAuthenticationUtils testAuthenticationUtils() {
            return new TestAuthenticationUtilsImpl();
        }
    }

    @Autowired
    private NCBIGene2GOAssociationLoaderCLI ncbiGene2GOAssociationLoaderCLI;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    @Test
    public void test() {
        testAuthenticationUtils.runAsAdmin();
        ExternalDatabase gene2go = ExternalDatabase.Factory.newInstance( "go", DatabaseType.OTHER );
        when( externalDatabaseService.findByNameWithAuditTrail( "go" ) ).thenReturn( gene2go );
        ncbiGene2GOAssociationLoaderCLI.executeCommand( new String[] {} );
        verify( gene2GOAssociationService ).removeAll();
        verify( externalDatabaseService ).findByNameWithAuditTrail( "go" );
        verify( externalDatabaseService ).updateReleaseLastUpdated( same( gene2go ), isNull(), any() );
    }
}