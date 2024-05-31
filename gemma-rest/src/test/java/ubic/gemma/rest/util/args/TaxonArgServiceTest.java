package ubic.gemma.rest.util.args;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.core.context.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class TaxonArgServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class TaxonArgServiceTestContextConfiguration {

        @Bean
        public TaxonArgService taxonArgService( TaxonService taxonService ) {
            return new TaxonArgService( taxonService, mock( ChromosomeService.class ), mock( GeneService.class ) );
        }

        @Bean
        public TaxonService taxonService() {
            return mock( TaxonService.class );
        }
    }

    @Autowired
    private TaxonArgService taxonArgService;

    @Autowired
    private TaxonService taxonService;

    @After
    public void tearDown() {
        reset( taxonService );
    }

    @Test
    public void testGetFilters() {
        when( taxonService.getFilter( any(), any(), any(), any( String.class ) ) )
                .thenAnswer( a -> Filter.by( null, a.getArgument( 0 ), String.class, a.getArgument( 2 ), a.getArgument( 3, String.class ) ) );
        Filters filters = taxonArgService.getFilters( TaxonNameArg.valueOf( "Homo sapiens" ) );
        assertThat( filters ).hasSize( 1 )
                .hasToString( "commonName = \"Homo sapiens\" or scientificName = \"Homo sapiens\"" );
        verify( taxonService ).getFilter( "commonName", String.class, Filter.Operator.eq, "Homo sapiens" );
        verify( taxonService ).getFilter( "scientificName", String.class, Filter.Operator.eq, "Homo sapiens" );
    }

}