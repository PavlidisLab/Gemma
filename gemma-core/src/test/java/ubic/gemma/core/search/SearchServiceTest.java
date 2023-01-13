package ubic.gemma.core.search;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ContextConfiguration
public class SearchServiceTest extends AbstractJUnit4SpringContextTests {

    private static final Taxon rat = Taxon.Factory.newInstance( "Rattus norvegicus", "rat", 192, false );

    @Configuration
    @TestComponent
    static class SearchServiceImplTestContextConfiguration extends SearchServiceTestContextConfiguration {

        @Bean
        @Override
        public TaxonService taxonService() {
            // this needs to be setup early because SearchService initializes its taxon mapping in afterPropertiesSet()
            TaxonService ts = super.taxonService();
            when( ts.loadAll() ).thenReturn( Collections.singletonList( rat ) );
            return ts;
        }
    }

    @Autowired
    private SearchService searchService;

    @Autowired
    @Qualifier("databaseSearchSource")
    private SearchSource databaseSearchSource;

    @Autowired
    private OntologyService ontologyService;

    @After
    public void tearDown() {
        reset( databaseSearchSource, ontologyService );
    }

    @Test
    public void test_whenTaxonIsNameIsUsedInQuery_thenAddTaxonToSearchSettings() throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .resultType( Gene.class )
                .query( "the best rat in the universe" )
                .build();
        searchService.search( settings );
        verify( databaseSearchSource ).searchGene( settings.withTaxon( rat ) );
    }

    @Test
    public void searchExpressionExperiment_whenQueryHasMultipleClauses_thenParseAccordingly() throws SearchException, OntologySearchException {
        SearchSettings settings = SearchSettings.expressionExperimentSearch( "cancer AND liver" );
        searchService.search( settings );
        verify( ontologyService ).findIndividuals( "cancer" );
        verify( ontologyService ).findTerms( "cancer" );
        verify( ontologyService ).findIndividuals( "liver" );
        verify( ontologyService ).findTerms( "liver" );
    }
}