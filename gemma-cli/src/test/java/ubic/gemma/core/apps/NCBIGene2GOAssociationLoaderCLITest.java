package ubic.gemma.core.apps;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.TestComponent;

import static org.mockito.Mockito.*;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

@Category(SlowTest.class)
@ContextConfiguration
public class NCBIGene2GOAssociationLoaderCLITest extends BaseCliTest {

    @Configuration
    @TestComponent
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
    }

    @Autowired
    private NCBIGene2GOAssociationLoaderCLI ncbiGene2GOAssociationLoaderCLI;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Test
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