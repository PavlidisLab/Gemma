package ubic.gemma.web.controller.search;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.util.BaseWebTest;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration
public class GeneralSearchControllerTest extends BaseWebTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ArrayDesignService arrayDesignService;

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.gemBrow.url=http://localhost:8080/browse" );
        }

        @Bean
        public GeneralSearchController generalSearchController() {
            return new GeneralSearchController();
        }

        @Bean
        public SearchService searchService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public TaxonService taxonService() {
            return mock();
        }

        @Bean
        public MessageSource messageSource() {
            return mock();
        }

        @Bean
        public WebEntityUrlBuilder entityUrlBuilder( ServletContext servletContext ) {
            return new WebEntityUrlBuilder( "http://localhost:8080", servletContext );
        }
    }

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    @Test
    public void testSearch() throws Exception {
        SearchService.SearchResultMap srm = mock();
        SearchResult<?> r1 = SearchResult.from( ExpressionExperiment.class, 1L, 1.0, null, "test" );
        SearchResult<?> r2 = SearchResult.from( ExpressionExperiment.class, 2L, 0.9, null, "test" );
        SearchResult<?> r3 = SearchResult.from( ExpressionExperiment.class, 3L, 0.5, null, "test" );
        SearchResult<?> r4 = SearchResult.from( ExpressionExperiment.class, 4L, 0.3, null, "test" );
        when( srm.getByResultType( ExpressionExperiment.class ) ).thenReturn( Arrays.asList( r1, r2, r3, r4 ) );
        when( searchService.search( any() ) )
                .thenReturn( srm );
        Taxon humanTaxon = Taxon.Factory.newInstance( "human" );
        humanTaxon.setScientificName( "Homo sapiens" );
        when( taxonService.findByCommonName( "human" ) ).thenReturn( humanTaxon );

        // blank
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "scope", "E" )
                .param( "taxon", "human" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "generalSearch" ) )
                .andExpect( model().attribute( "scope", "E" ) )
                .andExpect( model().attribute( "searchTaxon", "Homo sapiens" ) )
                .andExpect( model().attributeDoesNotExist( "SearchString", "SearchURI" ) );

        // by query
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "query", "brain" )
                .param( "scope", "E" )
                .param( "taxon", "human" )
                .param( "noRedirect", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "generalSearch" ) )
                .andExpect( model().attribute( "SearchString", "brain" ) )
                .andExpect( model().attribute( "scope", "E" ) )
                .andExpect( model().attribute( "searchTaxon", "Homo sapiens" ) )
                .andExpect( model().attributeDoesNotExist( "SearchURI" ) );

        // by URI
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "termUri", "http://example.com" )
                .param( "scope", "E" )
                .param( "taxon", "human" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "generalSearch" ) )
                .andExpect( model().attribute( "SearchURI", "http://example.com" ) )
                .andExpect( model().attribute( "scope", "E" ) )
                .andExpect( model().attribute( "searchTaxon", "Homo sapiens" ) )
                .andExpect( model().attributeDoesNotExist( "SearchString" ) );
    }

    @Test
    public void testSearchByDataset() throws Exception {
        SearchService.SearchResultMap srm = mock();
        SearchResult<?> r1 = SearchResult.from( ExpressionExperiment.class, 1L, 1.0, null, "test" );
        SearchResult<?> r2 = SearchResult.from( ExpressionExperiment.class, 2L, 0.9, null, "test" );
        SearchResult<?> r3 = SearchResult.from( ExpressionExperiment.class, 3L, 0.5, null, "test" );
        SearchResult<?> r4 = SearchResult.from( ExpressionExperiment.class, 4L, 0.3, null, "test" );
        when( srm.getByResultType( ExpressionExperiment.class ) ).thenReturn( Arrays.asList( r1, r2, r3, r4 ) );
        when( searchService.search( any(), any() ) )
                .thenReturn( srm );
        ExpressionExperiment dataset = ExpressionExperiment.Factory.newInstance();
        dataset.setShortName( "GSE1092" );
        when( expressionExperimentService.findByShortName( "GSE1092" ) ).thenReturn( dataset );
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "query", "brain" )
                .param( "scope", "E" )
                .param( "dataset", "GSE1092" )
                .param( "noRedirect", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "generalSearch" ) )
                .andExpect( model().attribute( "SearchString", "brain" ) )
                .andExpect( model().attributeDoesNotExist( "SearchURI" ) )
                .andExpect( model().attribute( "scope", "E" ) );
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "query", "brain" )
                .param( "scope", "E" )
                .param( "dataset", "GSE1092" ) )
                .andExpect( status().isFound() )
                .andExpect( redirectedUrl( "http://localhost:8080/browse/#/q/brain" ) );
    }

    @Test
    public void testSearchByPlatform() throws Exception {
        SearchService.SearchResultMap srm = mock();
        SearchResult<?> r1 = SearchResult.from( ExpressionExperiment.class, 1L, 1.0, null, "test" );
        SearchResult<?> r2 = SearchResult.from( ExpressionExperiment.class, 2L, 0.9, null, "test" );
        SearchResult<?> r3 = SearchResult.from( ExpressionExperiment.class, 3L, 0.5, null, "test" );
        SearchResult<?> r4 = SearchResult.from( ExpressionExperiment.class, 4L, 0.3, null, "test" );
        when( srm.getByResultType( ExpressionExperiment.class ) ).thenReturn( Arrays.asList( r1, r2, r3, r4 ) );
        when( searchService.search( any(), any() ) )
                .thenReturn( srm );
        ArrayDesign platform = ArrayDesign.Factory.newInstance();
        platform.setShortName( "GPL1029" );
        when( arrayDesignService.findByShortName( "GPL1029" ) ).thenReturn( platform );
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "query", "brain" )
                .param( "scope", "E" )
                .param( "platform", "GPL1029" )
                .param( "noRedirect", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( model().attributeExists( "SearchString" ) )
                .andExpect( model().attribute( "scope", "E" ) )
                .andExpect( model().attribute( "searchPlatform", platform ) )
                .andExpect( model().attributeDoesNotExist( "SearchURI" ) );
    }

    @Test
    public void testSearchWithQuickRedirect() throws Exception {
        SearchService.SearchResultMap srm = mock();
        SearchResult<?> singleResult = SearchResult.from( ExpressionExperiment.class, 1L, 1.0, null, "test" );
        when( srm.toList() ).thenReturn( Collections.singletonList( singleResult ) );
        when( searchService.search( any(), any() ) )
                .thenReturn( srm );

        // no scope requested, we search for everything, but redirect if there is a single result
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "query", "GSE19283" ) )
                .andExpect( status().isFound() )
                .andExpect( redirectedUrl( "/expressionExperiment/showExpressionExperiment.html?id=1" ) );

        // a single scope is requested, redirection is fine
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "query", "GSE19283" )
                .param( "scope", "E" ) )
                .andExpect( status().isFound() )
                .andExpect( redirectedUrl( "/expressionExperiment/showExpressionExperiment.html?id=1" ) );

        // two scopes explicitly requested, never redirect, user is expecting multiple results
        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "query", "GSE19283" )
                .param( "scope", "EP" ) )
                .andExpect( status().isOk() );

        perform( MockMvcRequestBuilders.get( "/searcher.html" )
                .param( "query", "GSE19283" )
                .param( "scope", "E" )
                .param( "noRedirect", "true" ) )
                .andExpect( status().isOk() );
    }
}