package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;
import org.hibernate.search.util.impl.PassThroughAnalyzer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.search.lucene.LuceneParseSearchException;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.Assertions;
import ubic.gemma.rest.util.BaseJerseyTest;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class SearchWebServiceTest extends BaseJerseyTest {

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    public static class SearchWebServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public Future<OpenAPI> openApi() {
            return constantFuture( mock() );
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }

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

        @Bean
        public TaxonArgService taxonArgService( TaxonService taxonService ) {
            return new TaxonArgService( taxonService, mock( ChromosomeService.class ), mock( GeneService.class ) );
        }

        @Bean
        public PlatformArgService platformArgService( ArrayDesignService arrayDesignService ) {
            return new PlatformArgService( arrayDesignService, mock( ExpressionExperimentService.class ), mock( CompositeSequenceService.class ) );
        }

        @Bean
        public AnalyticsProvider analyticsProvider() {
            return mock();
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock();
        }
    }

    @Autowired
    private SearchWebService searchWebService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private ArrayDesignService arrayDesignService;

    /* fixtures */
    private Gene gene;

    @Before
    public void setUpMocks() {
        gene = new Gene();
        gene.setId( 1L );
        gene.setOfficialSymbol( "BRCA1" );
        Taxon taxon = new Taxon();
        gene.setTaxon( taxon );
        taxon.setNcbiId( 9606 );
        ArrayDesign arrayDesign = new ArrayDesign();
        arrayDesign.setId( 1L );
        when( taxonService.findByNcbiId( 9606 ) ).thenReturn( taxon );
        when( arrayDesignService.load( 1L ) ).thenReturn( arrayDesign );
    }

    @After
    public void resetMocks() {
        reset( searchService, taxonService, arrayDesignService );
    }

    @Test
    public void testSearchEverything() throws SearchException {
        ArgumentCaptor<SearchSettings> searchSettingsArgumentCaptor = ArgumentCaptor.forClass( SearchSettings.class );
        SearchService.SearchResultMap srm = mock( SearchService.SearchResultMap.class );
        when( srm.toList() ).thenReturn( Collections.singletonList( SearchResult.from( Gene.class, gene, 1.0, null, "test object" ) ) );
        when( searchService.search( searchSettingsArgumentCaptor.capture(), any() ) ).thenReturn( srm );
        when( searchService.loadValueObjects( any() ) ).thenAnswer( args -> {
            //noinspection unchecked
            Collection<SearchResult<Gene>> searchResult = args.getArgument( 0, Collection.class );
            return searchResult.stream()
                    .map( sr -> sr.withResultObject( new GeneValueObject( sr.getResultObject() ) ) )
                    .collect( Collectors.toList() );
        } );
        when( searchService.getSupportedResultTypes() ).thenReturn( Collections.singleton( Gene.class ) );

        SearchWebService.SearchResultsResponseDataObject searchResults = searchWebService.search( QueryArg.valueOf( "BRCA1" ), null, null, null, null, LimitArg.valueOf( "20" ), null );

        assertThat( searchSettingsArgumentCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "query", "BRCA1" )
                .hasFieldOrPropertyWithValue( "resultTypes", Collections.singleton( Gene.class ) )
                .hasFieldOrPropertyWithValue( "maxResults", 20 );

        assertThat( searchResults.getData() )
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "resultId", gene.getId() )
                .hasFieldOrPropertyWithValue( "resultType", gene.getClass().getName() )
                .extracting( "resultObject" )
                .hasFieldOrPropertyWithValue( "officialSymbol", gene.getOfficialSymbol() );

        assertThat( searchResults.getSearchSettings() )
                .hasFieldOrPropertyWithValue( "query", "BRCA1" )
                .hasFieldOrPropertyWithValue( "resultTypes", Collections.singleton( Gene.class.getName() ) );
    }

    @Test
    public void testSearchByTaxon() throws SearchException {
        SearchService.SearchResultMap srm = mock( SearchService.SearchResultMap.class );
        when( srm.getByResultObjectType( Gene.class ) ).thenReturn( Collections.singletonList( SearchResult.from( Gene.class, gene, 1.0, null, "test object" ) ) );
        when( searchService.search( any(), any() ) ).thenReturn( srm );
        when( searchService.loadValueObject( any() ) ).thenAnswer( args -> {
            //noinspection unchecked
            SearchResult<Gene> searchResult = args.getArgument( 0, SearchResult.class );
            SearchResult<GeneValueObject> sr = SearchResult.from( searchResult.getResultType(), searchResult.getResultId(), searchResult.getScore(), searchResult.getHighlights(), "test object" );
            if ( searchResult.getResultObject() != null ) {
                sr.setResultObject( new GeneValueObject( searchResult.getResultObject() ) );
            }
            return sr;
        } );
        searchWebService.search( QueryArg.valueOf( "BRCA1" ), null, TaxonArg.valueOf( "9606" ), null, null, LimitArg.valueOf( "20" ), null );
        verify( taxonService ).findByNcbiId( 9606 );
    }

    @Test
    public void testSearchByArrayDesign() throws SearchException {
        SearchService.SearchResultMap srm = mock( SearchService.SearchResultMap.class );
        when( srm.getByResultObjectType( Gene.class ) ).thenReturn( Collections.singletonList( SearchResult.from( Gene.class, gene, 1.0, null, "test object" ) ) );
        when( searchService.search( any(), any() ) ).thenReturn( srm );
        when( searchService.loadValueObject( any() ) ).thenAnswer( args -> {
            //noinspection unchecked
            SearchResult<Gene> searchResult = args.getArgument( 0, SearchResult.class );
            SearchResult<GeneValueObject> sr = SearchResult.from( searchResult.getResultType(), searchResult.getResultId(), searchResult.getScore(), searchResult.getHighlights(), "test object" );
            if ( searchResult.getResultObject() != null ) {
                sr.setResultObject( new GeneValueObject( searchResult.getResultObject() ) );
            }
            return sr;
        } );
        searchWebService.search( QueryArg.valueOf( "BRCA1" ), null, null, PlatformArg.valueOf( "1" ), null, LimitArg.valueOf( "20" ), null );
        verify( arrayDesignService ).load( 1L );
    }

    @Test(expected = BadRequestException.class)
    public void testSearchWhenQueryIsMissing() {
        searchWebService.search( null, null, null, null, null, LimitArg.valueOf( "20" ), null );
    }

    @Test(expected = BadRequestException.class)
    public void testSearchWhenQueryIsEmpty() {
        QueryArg.valueOf( "" );
    }

    @Test(expected = NotFoundException.class)
    public void testSearchWhenUnknownTaxonIsProvided() {
        searchWebService.search( QueryArg.valueOf( "brain" ), null, TaxonArg.valueOf( "9607" ), null, null, LimitArg.valueOf( "20" ), null );
    }

    @Test(expected = NotFoundException.class)
    public void testSearchWhenUnknownPlatformIsProvided() {
        searchWebService.search( QueryArg.valueOf( "brain" ), null, null, PlatformArg.valueOf( "2" ), null, LimitArg.valueOf( "20" ), null );
    }

    @Test(expected = BadRequestException.class)
    public void testSearchWhenUnsupportedResultTypeIsProvided() {
        searchWebService.search( QueryArg.valueOf( "brain" ), null, null, null, Collections.singletonList( "ubic.gemma.model.expression.designElement.CompositeSequence2" ), LimitArg.valueOf( "20" ), null );
    }

    @Test
    public void testSearchWithInvalidQuery() throws SearchException {
        when( searchService.search( any(), any() ) ).thenAnswer( a -> {
            try {
                new QueryParser( Version.LUCENE_36, "", new PassThroughAnalyzer( Version.LUCENE_36 ) )
                        .parse( a.getArgument( 0, SearchSettings.class ).getQuery() );
            } catch ( ParseException e ) {
                throw new LuceneParseSearchException( "\"", e.getMessage(), e );
            }
            return mock();
        } );
        Assertions.assertThat( target( "/search" ).queryParam( "query", "\"" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST )
                .entity()
                .hasFieldOrPropertyWithValue( "error.code", 400 )
                .hasFieldOrPropertyWithValue( "error.message", "Invalid search query: \"" );
    }
}