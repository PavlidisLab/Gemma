package ubic.gemma.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.TestComponent;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebAppConfiguration
@ContextConfiguration
public class SearchWebServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
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

        @Bean
        public TaxonArgService taxonArgService( TaxonService taxonService ) {
            return new TaxonArgService( taxonService );
        }

        @Bean
        public PlatformArgService platformArgService( ArrayDesignService arrayDesignService ) {
            return new PlatformArgService( arrayDesignService );
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
    public void setUp() {
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
    public void tearDown() {
        reset( searchService, taxonService, arrayDesignService );
    }

    @Test
    public void testSearchEverything() throws SearchException {
        ArgumentCaptor<SearchSettings> searchSettingsArgumentCaptor = ArgumentCaptor.forClass( SearchSettings.class );
        SearchService.SearchResultMap srm = mock( SearchService.SearchResultMap.class );
        when( srm.toList() ).thenReturn( Collections.singletonList( SearchResult.from( Gene.class, gene, 1.0, "test object" ) ) );
        when( searchService.search( searchSettingsArgumentCaptor.capture() ) ).thenReturn( srm );
        when( searchService.loadValueObjects( any() ) ).thenAnswer( args -> {
            //noinspection unchecked
            Collection<SearchResult<Gene>> searchResult = args.getArgument( 0, Collection.class );
            return searchResult.stream()
                    .map( sr -> SearchResult.from( sr, new GeneValueObject( sr.getResultObject() ) ) )
                    .collect( Collectors.toList() );
        } );
        when( searchService.getSupportedResultTypes() ).thenReturn( Collections.singleton( Gene.class ) );

        SearchWebService.SearchResultsResponseDataObject searchResults = searchWebService.search( "BRCA1", null, null, null, LimitArg.valueOf( "20" ), null );

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
        when( srm.getByResultObjectType( Gene.class ) ).thenReturn( Collections.singletonList( SearchResult.from( Gene.class, gene, 1.0, "test object" ) ) );
        when( searchService.search( any() ) ).thenReturn( srm );
        when( searchService.loadValueObject( any() ) ).thenAnswer( args -> {
            //noinspection unchecked
            SearchResult<Gene> searchResult = args.getArgument( 0, SearchResult.class );
            SearchResult<GeneValueObject> sr = SearchResult.from( searchResult.getResultType(), searchResult.getResultId(), searchResult.getScore(), "test object" );
            searchResult.setHighlights( searchResult.getHighlights() );
            if ( searchResult.getResultObject() != null ) {
                sr.setResultObject( new GeneValueObject( searchResult.getResultObject() ) );
            }
            return sr;
        } );
        searchWebService.search( "BRCA1", TaxonArg.valueOf( "9606" ), null, null, LimitArg.valueOf( "20" ), null );
        verify( taxonService ).findByNcbiId( 9606 );
    }

    @Test
    public void testSearchByArrayDesign() throws SearchException {
        SearchService.SearchResultMap srm = mock( SearchService.SearchResultMap.class );
        when( srm.getByResultObjectType( Gene.class ) ).thenReturn( Collections.singletonList( SearchResult.from( Gene.class, gene, 1.0, "test object" ) ) );
        when( searchService.search( any() ) ).thenReturn( srm );
        when( searchService.loadValueObject( any() ) ).thenAnswer( args -> {
            //noinspection unchecked
            SearchResult<Gene> searchResult = args.getArgument( 0, SearchResult.class );
            SearchResult<GeneValueObject> sr = SearchResult.from( searchResult.getResultType(), searchResult.getResultId(), searchResult.getScore(), "test object" );
            sr.setHighlights( searchResult.getHighlights() );
            if ( searchResult.getResultObject() != null ) {
                sr.setResultObject( new GeneValueObject( searchResult.getResultObject() ) );
            }
            return sr;
        } );
        searchWebService.search( "BRCA1", null, PlatformArg.valueOf( "1" ), null, LimitArg.valueOf( "20" ), null );
        verify( arrayDesignService ).load( 1L );
    }

    @Test(expected = BadRequestException.class)
    public void testSearchWhenQueryIsMissing() {
        searchWebService.search( null, null, null, null, LimitArg.valueOf( "20" ), null );
    }

    @Test(expected = BadRequestException.class)
    public void testSearchWhenQueryIsEmpty() {
        searchWebService.search( null, null, null, null, LimitArg.valueOf( "20" ), null );
    }

    @Test(expected = NotFoundException.class)
    public void testSearchWhenUnknownTaxonIsProvided() {
        searchWebService.search( "brain", TaxonArg.valueOf( "9607" ), null, null, LimitArg.valueOf( "20" ), null );
    }

    @Test(expected = NotFoundException.class)
    public void testSearchWhenUnknownPlatformIsProvided() {
        searchWebService.search( "brain", null, PlatformArg.valueOf( "2" ), null, LimitArg.valueOf( "20" ), null );
    }

    @Test(expected = BadRequestException.class)
    public void testSearchWhenUnsupportedResultTypeIsProvided() {
        searchWebService.search( "brain", null, null, Collections.singletonList( "ubic.gemma.model.expression.designElement.CompositeSequence2" ), LimitArg.valueOf( "20" ), null );
    }
}