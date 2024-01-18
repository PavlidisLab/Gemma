package ubic.gemma.rest.util.args;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ContextConfiguration
public class GeneArgServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class GeneArgServiceTestTestContextConfig {

        @Bean
        public GeneArgService geneArgService( GeneService geneService ) {
            return new GeneArgService( geneService, mock( GeneOntologyService.class ) );
        }

        @Bean
        public GeneService geneService() {
            return mock( GeneService.class );
        }
    }

    @Autowired
    private GeneArgService geneArgService;

    @Autowired
    private GeneService geneService;

    @Before
    public void setupMocks() {
        when( geneService.findByNCBIId( any() ) ).thenReturn( new Gene() );
        when( geneService.findByEnsemblId( any() ) ).thenReturn( new Gene() );
        when( geneService.findByOfficialSymbol( any() ) ).thenReturn( Collections.singleton( new Gene() ) );
        when( geneService.findByOfficialSymbol( any(), any() ) ).thenReturn( new Gene() );
    }

    @Test
    public void testNcbiId() {
        geneArgService.getEntity( new GeneNcbiIdArg( 1 ) );
        verify( geneService ).findByNCBIId( 1 );
    }

    @Test
    public void testEnsemblId() {
        geneArgService.getEntity( new GeneEnsemblIdArg( "ENSG00001" ) );
        verify( geneService ).findByEnsemblId( "ENSG00001" );
    }

    @Test
    public void testFindByOfficialSymbol() {
        geneArgService.getEntity( new GeneSymbolArg( "BRCA1" ) );
        verify( geneService ).findByOfficialSymbol( "BRCA1" );
    }

    @Test
    public void testFindByOfficialSymbolAndTaxon() {
        Taxon taxon = new Taxon();
        geneArgService.getEntityWithTaxon( new GeneSymbolArg( "BRCA1" ), taxon );
        verify( geneService ).findByOfficialSymbol( "BRCA1", taxon );
    }
}