package ubic.gemma.web.services.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.ws.rs.BadRequestException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebAppConfiguration
@ContextConfiguration
public class SearchWebServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    public static class SearchWebServiceTestContextConfiguration {

        @Bean
        public SearchWebService searchWebService() {
            return new SearchWebService();
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public TaxonService taxonService() {
            return mock( TaxonService.class );
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
        }
    }

    @Autowired
    private SearchWebService searchWebService;
    @Autowired
    private SearchService searchService;

    /* fixtures */
    private Gene gene;
    private Taxon taxon;

    @Before
    public void setUp() {
        gene = new Gene();
        gene.setOfficialSymbol( "BRCA1" );
        taxon = new Taxon();
        gene.setTaxon( taxon );
        when( searchService.search( any() ) ).thenReturn( Collections.singletonMap( Gene.class, Collections.singletonList( new SearchResult( gene ) ) ) );
    }

    @Test
    public void testSearchEverything() {
        ResponseDataObject<List<SearchWebService.SearchResultValueObject>> searchResults = searchWebService.search( "BRCA1", null, null, null );
        assertThat( searchResults.getData() )
                .hasSize( 1 )
                .first()
                .extracting( "resultObject" )
                .hasFieldOrPropertyWithValue( "officialSymbol", gene.getOfficialSymbol() );
    }

    @Test(expected = BadRequestException.class)
    public void testSearchWhenUnsupportedResultTypeIsProvided() {
        searchWebService.search( "brain", null, null, Arrays.asList( "ubic.gemma.model.expression.designElement.CompositeSequence2" ) );
    }
}