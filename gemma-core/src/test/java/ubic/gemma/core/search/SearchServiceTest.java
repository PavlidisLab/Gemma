package ubic.gemma.core.search;

import org.junit.After;
import org.junit.Test;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.source.OntologySearchSource;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class SearchServiceTest extends AbstractJUnit4SpringContextTests {

    private static final Taxon rat = Taxon.Factory.newInstance( "Rattus norvegicus", "rat", 192, false );

    @Configuration
    @TestComponent
    static class SearchServiceImplTestContextConfiguration extends SearchServiceTestContextConfiguration {

        @Bean
        @Override
        public OntologySearchSource ontologySearchSource() {
            return new OntologySearchSource();
        }

        @Bean
        @Override
        public TaxonService taxonService() {
            // this needs to be setup early because SearchService initializes its taxon mapping in afterPropertiesSet()
            TaxonService ts = super.taxonService();
            when( ts.loadAll() ).thenReturn( Collections.singletonList( rat ) );
            return ts;
        }

        @Bean
        public SearchSource fieldAwareSearchSource() {
            return mock( FieldAwareSearchSource.class );
        }
    }

    @Autowired
    private SearchService searchService;

    @Autowired
    @Qualifier("databaseSearchSource")
    private SearchSource databaseSearchSource;

    @Autowired
    @Qualifier("fieldAwareSearchSource")
    private SearchSource fieldAwareSearchSource;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CharacteristicService characteristicService;

    @After
    public void tearDown() {
        reset( databaseSearchSource, ontologyService );
    }

    @Test
    public void testGetFields() {
        when( ( ( FieldAwareSearchSource ) fieldAwareSearchSource ).getFields( ExpressionExperiment.class ) )
                .thenReturn( Collections.singleton( "shortName" ) );
        assertThat( searchService.getFields( ExpressionExperiment.class ) )
                .contains( "shortName" );
        verify( ( FieldAwareSearchSource ) fieldAwareSearchSource ).getFields( ExpressionExperiment.class );
    }

    @Test
    public void test_whenTaxonIsNameIsUsedInQuery_thenAddTaxonToSearchSettings() throws SearchException {
        when( databaseSearchSource.accepts( any() ) ).thenReturn( true );
        SearchSettings settings = SearchSettings.builder()
                .resultType( Gene.class )
                .query( "the best rat in the universe" )
                .build();
        searchService.search( settings );
        verify( databaseSearchSource ).accepts( settings.withTaxon( rat ) );
        verify( databaseSearchSource ).searchGene( settings.withTaxon( rat ) );
    }

    @Test
    public void searchExpressionExperiment_whenQueryHasMultipleClauses_thenParseAccordingly() throws SearchException {
        SearchSettings settings = SearchSettings.expressionExperimentSearch( "cancer AND liver" );
        searchService.search( settings );
        verify( ontologyService ).findTerms( eq( "cancer" ), eq( 5000 ), longThat( l -> l > 0 && l <= 30000L ), eq( TimeUnit.MILLISECONDS ) );
        // cancer returns no result, so we should not bother querying liver
        verifyNoMoreInteractions( ontologyService );
    }

    @Test
    public void searchExpressionExperimentsByUri_whenQueryIsAUri_thenEnsureTheUriIsUsedDirectly() throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .query( "http://purl.obolibrary.org/obo/DOID_14602" )
                .resultType( ExpressionExperiment.class )
                .maxResults( 10 )
                .build();
        searchService.search( settings );
        verify( ontologyService ).getTerm( "http://purl.obolibrary.org/obo/DOID_14602" );
        verifyNoMoreInteractions( ontologyService );
        verify( characteristicService ).findExperimentsByUris( Collections.singleton( "http://purl.obolibrary.org/obo/DOID_14602" ), null, 10, true, false );
    }

    @Test
    @WithMockUser
    public void searchExpressionExperiment() throws SearchException {
        when( databaseSearchSource.accepts( any() ) ).thenReturn( true );
        SearchSettings settings = SearchSettings.builder()
                .query( "http://purl.obolibrary.org/obo/DOID_14602" )
                .resultType( ExpressionExperiment.class )
                .fillResults( false )
                .build();
        ExpressionExperiment ee = mock( ExpressionExperiment.class );
        when( characteristicService.findExperimentsByUris( any(), any(), anyInt(), eq( false ), anyBoolean() ) )
                .thenReturn( Collections.singletonMap( ExpressionExperiment.class,
                        Collections.singletonMap( "test", Collections.singleton( ee ) ) ) );
        SearchService.SearchResultMap results = searchService.search( settings );
        verify( databaseSearchSource ).accepts( settings );
        verify( databaseSearchSource ).searchExpressionExperiment( settings );
        verify( characteristicService ).findExperimentsByUris( Collections.singleton( "http://purl.obolibrary.org/obo/DOID_14602" ), null, 5000, false, false );
        assertNull( results.getByResultObjectType( ExpressionExperiment.class ).iterator().next().getResultObject() );
        // since EE is a proxy, only its ID should be accessed
        verify( ee, VerificationModeFactory.atLeastOnce() ).getId();
        verifyNoMoreInteractions( ee );
    }
}