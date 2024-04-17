package ubic.gemma.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.*;
import ubic.gemma.rest.util.QueriedAndFilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.SortValueObject;
import ubic.gemma.rest.util.args.*;

import java.util.Collections;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author poirigui
 */
@ActiveProfiles("web")
@WebAppConfiguration
@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class AnnotationsWebServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    public static class AnnotationsWebServiceContextConfiguration {

        @Bean
        public OntologyService ontologyService() {
            return mock( OntologyService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public CharacteristicService characteristicService() {
            return mock( CharacteristicService.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public TaxonService taxonService() {
            return mock( TaxonService.class );
        }

        @Bean
        public DatasetArgService datasetRestService( ExpressionExperimentService service, SearchService searchService ) {
            return new DatasetArgService( service, searchService, mock( ArrayDesignService.class ), mock( BioAssayService.class ), mock( OutlierDetectionService.class ) );
        }

        @Bean
        public TaxonArgService taxonArgService( TaxonService taxonService ) {
            return new TaxonArgService( taxonService, mock( ChromosomeService.class ), mock( GeneService.class ) );
        }

        @Bean
        public AnnotationsWebService annotationsWebService( OntologyService ontologyService, SearchService searchService,
                CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
                DatasetArgService datasetRestService, TaxonArgService taxonArgService ) {
            return new AnnotationsWebService( ontologyService, searchService, characteristicService, expressionExperimentService, datasetRestService, taxonArgService );
        }
    }

    @Autowired
    private AnnotationsWebService annotationsWebService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Before
    public void setUp() {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setId( 1L );
        when( taxonService.findByCommonName( "human" ) ).thenReturn( taxon );
    }

    @After
    public void tearDown() {
        reset( searchService, taxonService );
    }

    @Test
    @WithMockUser
    public void testSearchTaxonDatasets() throws SearchException, TimeoutException {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        SearchService.SearchResultMap mockedSrMap = mock( SearchService.SearchResultMap.class );
        when( mockedSrMap.getByResultObjectType( ExpressionExperiment.class ) )
                .thenReturn( Collections.singletonList( SearchResult.from( ExpressionExperiment.class, ee, 1.0, null, "test object" ) ) );
        when( searchService.search( any( SearchSettings.class ) ) )
                .thenReturn( mockedSrMap );
        when( taxonService.getFilter( eq( "commonName" ), eq( String.class ), eq( Filter.Operator.eq ), any( String.class ) ) )
                .thenAnswer( a -> Filter.by( "t", "commonName", String.class, Filter.Operator.eq, a.getArgument( 3, String.class ), a.getArgument( 0 ) ) );
        when( taxonService.getFilter( eq( "scientificName" ), eq( String.class ), eq( Filter.Operator.eq ), any( String.class ) ) )
                .thenAnswer( a -> Filter.by( "t", "scientificName", String.class, Filter.Operator.eq, a.getArgument( 3, String.class ), a.getArgument( 0 ) ) );
        when( expressionExperimentService.getIdentifierPropertyName() ).thenReturn( "id" );
        when( expressionExperimentService.getFilter( "id", Filter.Operator.eq, "1" ) ).thenReturn( Filter.by( "ee", "id", Long.class, Filter.Operator.in, Collections.singleton( 1L ), "id" ) );
        when( expressionExperimentService.getSort( "id", Sort.Direction.ASC ) ).thenReturn( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) );
        when( expressionExperimentService.loadValueObjects( any( Filters.class ), eq( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) ), eq( 0 ), eq( 20 ) ) )
                .thenAnswer( a -> new Slice<>( Collections.singletonList( new ExpressionExperimentValueObject( ee ) ), a.getArgument( 1 ), a.getArgument( 2, Integer.class ), a.getArgument( 3, Integer.class ), 10000L ) );
        when( expressionExperimentService.getFiltersWithInferredAnnotations( any(), any(), anyLong(), any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> payload = annotationsWebService.searchTaxonDatasets(
                TaxonArg.valueOf( "human" ),
                StringArrayArg.valueOf( "bipolar" ),
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ) );
        assertThat( payload )
                .hasFieldOrPropertyWithValue( "query", "bipolar" )
                .hasFieldOrPropertyWithValue( "filter", "commonName = human or scientificName = human" )
                .hasFieldOrPropertyWithValue( "sort", new SortValueObject( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) ) )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 )
                .hasFieldOrPropertyWithValue( "totalElements", 10000L );
        verify( searchService ).search( any( SearchSettings.class ) );
        verify( taxonService ).getFilter( "commonName", String.class, Filter.Operator.eq, "human" );
        verify( taxonService ).getFilter( "scientificName", String.class, Filter.Operator.eq, "human" );
        verify( expressionExperimentService ).getFilter( "id", Filter.Operator.eq, "1" );
        verify( expressionExperimentService ).getSort( "id", Sort.Direction.ASC );
        verify( expressionExperimentService ).loadValueObjects( any( Filters.class ), eq( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) ), eq( 0 ), eq( 20 ) );
    }

}