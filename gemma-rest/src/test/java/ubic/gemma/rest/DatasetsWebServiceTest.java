package ubic.gemma.rest;

import org.assertj.core.api.InstanceOfAssertFactories;
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
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.*;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.BaseJerseyTest;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.GeneArgService;
import ubic.gemma.rest.util.args.QuantitationTypeArgService;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static ubic.gemma.rest.util.Assertions.assertThat;

@ContextConfiguration
public class DatasetsWebServiceTest extends BaseJerseyTest {

    @Import(JacksonConfig.class)
    @Configuration
    @TestComponent
    static class DatasetsWebServiceTestContextConfiguration {

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return mock( ExpressionDataFileService.class );
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
        }

        @Bean
        public ProcessedExpressionDataVectorService processedExpressionDataVectorService() {
            return mock( ProcessedExpressionDataVectorService.class );
        }

        @Bean
        public SVDService svdService() {
            return mock( SVDService.class );
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock( DifferentialExpressionAnalysisService.class );
        }

        @Bean
        public DifferentialExpressionResultService differentialExpressionResultService() {
            return mock( DifferentialExpressionResultService.class );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock( QuantitationTypeService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public DatasetArgService datasetArgService( ExpressionExperimentService expressionExperimentService, SearchService searchService ) {
            return new DatasetArgService( expressionExperimentService, searchService, mock( ArrayDesignService.class ), mock( BioAssayService.class ), mock( OutlierDetectionService.class ) );
        }

        @Bean
        public QuantitationTypeArgService quantitationTypeArgService( QuantitationTypeService quantitationTypeService ) {
            return new QuantitationTypeArgService( quantitationTypeService );
        }

        @Bean
        public GeneArgService geneArgService() {
            return mock( GeneArgService.class );
        }

        @Bean
        public AnalyticsProvider analyticsProvider() {
            return mock( AnalyticsProvider.class );
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock( AccessDecisionManager.class );
        }

        @Bean
        public OntologyService ontologyService() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private AnalyticsProvider analyticsProvider;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GeneArgService geneArgService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    private ExpressionExperiment ee;

    @Before
    public void setUpMocks() throws TimeoutException {
        ee = ExpressionExperiment.Factory.newInstance();
        //noinspection unchecked
        Set<String> universe = mock( Set.class );
        when( universe.contains( any( String.class ) ) ).thenReturn( true );
        when( expressionExperimentService.getFilterableProperties() ).thenReturn( universe );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getFiltersWithInferredAnnotations( any(), any(), anyLong(), any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( expressionExperimentService.getSort( any(), any() ) ).thenAnswer( a -> Sort.by( null, a.getArgument( 0 ), a.getArgument( 1 ), a.getArgument( 0 ) ) );
    }

    @After
    public void resetMocks() throws Exception {
        super.tearDown();
        reset( expressionExperimentService, quantitationTypeService, analyticsProvider, expressionDataFileService );
    }

    @Test
    public void testGetDatasets() {
        when( expressionExperimentService.loadValueObjectsWithCache( any(), any(), anyInt(), anyInt() ) )
                .thenAnswer( a -> new Slice<>( Collections.emptyList(), a.getArgument( 1 ), a.getArgument( 2 ), a.getArgument( 3 ), 0L ) );
        assertThat( target( "/datasets" ).request().acceptLanguage( Locale.CANADA_FRENCH ).get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" )
                .entity()
                .hasFieldOrPropertyWithValue( "sort.orderBy", "id" )
                .hasFieldOrPropertyWithValue( "sort.direction", "+" )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 )
                .hasFieldOrPropertyWithValue( "totalElements", 0 );
        //noinspection unchecked
        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass( Map.class );
        verify( analyticsProvider ).sendEvent( eq( "gemma_rest_api_access" ), any( Date.class ), params.capture() );
        assertThat( params.getValue() )
                .containsOnlyKeys( "method", "endpoint", "user_agent", "language" )
                .containsEntry( "method", "GET" )
                .containsEntry( "endpoint", "/datasets" )
                .containsEntry( "language", "fr-ca" );
    }

    @Test
    public void testGetDatasetsWithQuery() throws SearchException {
        List<Long> ids = Arrays.asList( 1L, 3L, 5L );
        List<SearchResult<ExpressionExperiment>> results = ids.stream()
                .map( this::createMockSearchResult )
                .collect( Collectors.toList() );
        SearchService.SearchResultMap map = mock( SearchService.SearchResultMap.class );
        when( map.getByResultObjectType( ExpressionExperiment.class ) )
                .thenReturn( results );
        when( searchService.search( any() ) ).thenReturn( map );
        when( expressionExperimentService.loadIdsWithCache( any(), any() ) ).thenReturn( ids );
        when( expressionExperimentService.getFilter( "id", Long.class, Filter.Operator.in, new HashSet<>( ids ) ) )
                .thenReturn( Filter.by( "ee", "id", Long.class, Filter.Operator.in, new HashSet<>( ids ) ) );
        when( expressionExperimentService.getSort( "id", Sort.Direction.ASC ) )
                .thenReturn( Sort.by( "ee", "id", Sort.Direction.ASC ) );
        assertThat( target( "/datasets" ).queryParam( "query", "cerebellum" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        ArgumentCaptor<SearchSettings> captor = ArgumentCaptor.forClass( SearchSettings.class );
        verify( searchService, times( 2 ) ).search( captor.capture() );
        assertThat( captor.getAllValues() )
                .hasSize( 2 )
                .satisfiesExactly( s -> {
                    assertThat( s.getQuery() ).isEqualTo( "cerebellum" );
                    assertThat( s.isFillResults() ).isFalse();
                    assertThat( s.getHighlighter() ).isNull();
                }, s -> {
                    assertThat( s.getQuery() ).isEqualTo( "cerebellum" );
                    assertThat( s.isFillResults() ).isFalse();
                    assertThat( s.getHighlighter() ).isNotNull();
                } );
        verify( expressionExperimentService ).loadIdsWithCache( Filters.empty(), Sort.by( "ee", "id", Sort.Direction.ASC ) );
        verify( expressionExperimentService ).loadValueObjectsByIdsWithRelationsAndCache( ids );
    }

    @Test
    public void testGetDatasetsWithEmptyQuery() {
        assertThat( target( "/datasets" ).queryParam( "query", " " ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    private SearchResult<ExpressionExperiment> createMockSearchResult( Long id ) {
        return SearchResult.from( ExpressionExperiment.class, id, 0, null, "test result object" );
    }

    @Test
    public void testGetDatasetsWhenSliceHasNoLimit() {
        when( expressionExperimentService.loadValueObjectsWithCache( any(), any(), anyInt(), anyInt() ) )
                .thenAnswer( a -> new Slice<>( Collections.emptyList(), null, null, null, null ) );
        assertThat( target( "/datasets" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "sort", null )
                .hasFieldOrPropertyWithValue( "offset", null )
                .hasFieldOrPropertyWithValue( "limit", null )
                .hasFieldOrPropertyWithValue( "totalElements", null );
    }

    @Test
    public void testGetDatasetsOrderedByGeeqScore() {
        when( expressionExperimentService.loadValueObjectsWithCache( any(), any(), anyInt(), anyInt() ) )
                .thenAnswer( a -> new Slice<>( Collections.emptyList(), a.getArgument( 1 ), a.getArgument( 2 ), a.getArgument( 3 ), null ) );
        assertThat( target( "/datasets" ).queryParam( "sort", "+geeq.publicQualityScore" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "sort.orderBy", "geeq.publicQualityScore" )
                .hasFieldOrPropertyWithValue( "sort.direction", "+" )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 )
                .hasFieldOrPropertyWithValue( "totalElements", null );
        verify( expressionExperimentService ).getSort( "geeq.publicQualityScore", Sort.Direction.ASC );
        verify( expressionExperimentService )
                .loadValueObjectsWithCache(
                        any(),
                        eq( Sort.by( null, "(case when geeq.publicQualityScore is not null then 0 else 1 end)", Sort.Direction.ASC )
                                .andThen( Sort.by( null, "geeq.publicQualityScore", Sort.Direction.ASC, "geeq.publicQualityScore" ) ) ),
                        eq( 0 ),
                        eq( 20 ) );
    }

    @Test
    public void testGetDatasetsWhenInferenceTimeoutThenProduce503ServiceUnavailable() throws TimeoutException {
        //noinspection unchecked
        when( expressionExperimentService.getFilter( eq( "allCharacteristic.valueUri" ), eq( Filter.Operator.in ), anyCollection() ) )
                .thenAnswer( a -> Filter.by( "c", "valueUri", String.class, Filter.Operator.in, a.getArgument( 2, Collection.class ) ) );
        when( expressionExperimentService.getFiltersWithInferredAnnotations( any(), any(), anyLong(), any() ) )
                .thenThrow( new TimeoutException( "Inference timed out!" ) );
        when( expressionExperimentService.loadValueObjectsWithCache( any(), any(), anyInt(), anyInt() ) )
                .thenReturn( new Slice<>( Collections.emptyList(), null, null, null, null ) );
        assertThat( target( "/datasets" ).queryParam( "filter", "allCharacteristic.valueUri in (a, b, c)" ).request().get() )
                .hasStatus( Response.Status.SERVICE_UNAVAILABLE )
                .hasHeaderSatisfying( "Retry-After", values -> {
                    assertThat( values ).isNotEmpty();
                } )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetsPlatformsUsageStatistics() throws TimeoutException {
        Filter f = Filter.by( "ee", "id", Long.class, Filter.Operator.lessThan, 10L, "id" );
        when( expressionExperimentService.getFilter( "id", Filter.Operator.lessThan, "10" ) )
                .thenReturn( f );
        assertThat( target( "/datasets/platforms" ).queryParam( "filter", "id < 10" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" );
        verify( expressionExperimentService ).getFilter( "id", Filter.Operator.lessThan, "10" );
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.by( f ), null, 30, TimeUnit.SECONDS );
        verify( expressionExperimentService ).getArrayDesignUsedOrOriginalPlatformUsageFrequency( Filters.by( f ), null, 50 );
    }

    @Test
    public void testGetDatasetsAnnotationsWithRetainMentionedTerms() throws TimeoutException {
        assertThat( target( "/datasets/annotations" ).queryParam( "retainMentionedTerms", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" )
                .entity()
                .hasFieldOrPropertyWithValue( "limit", 100 )
                .hasFieldOrPropertyWithValue( "sort.orderBy", "numberOfExpressionExperiments" )
                .hasFieldOrPropertyWithValue( "sort.direction", "-" )
                .extracting( "groupBy", InstanceOfAssertFactories.list( String.class ) )
                .containsExactly( "classUri", "className", "termUri", "termName" );
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.empty(), Collections.emptySet(), 30, TimeUnit.SECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( Filters.empty(), null, null, null, null, 0, Collections.emptySet(), 100 );
    }

    @Test
    public void testGetDatasetsAnnotations() throws TimeoutException {
        assertThat( target( "/datasets/annotations" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" )
                .entity()
                .hasFieldOrPropertyWithValue( "limit", 100 )
                .hasFieldOrPropertyWithValue( "sort.orderBy", "numberOfExpressionExperiments" )
                .hasFieldOrPropertyWithValue( "sort.direction", "-" )
                .extracting( "groupBy", InstanceOfAssertFactories.list( String.class ) )
                .containsExactly( "classUri", "className", "termUri", "termName" );
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.empty(), null, 30, TimeUnit.SECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( Filters.empty(), null, null, null, null, 0, null, 100 );
    }

    @Test
    public void testGetDatasetsAnnotationWhenLimitExceedHardCap() {
        assertThat( target( "/datasets/annotations" ).queryParam( "limit", 10000 ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verifyNoInteractions( expressionExperimentService );
    }

    @Test
    public void testGetDatasetsAnnotationsWhenMaxFrequencyIsSuppliedLimitMustUseMaximum() throws TimeoutException {
        assertThat( target( "/datasets/annotations" ).queryParam( "minFrequency", "10" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "limit", 5000 );
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.empty(), null, 30, TimeUnit.SECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( Filters.empty(), null, null, null, null, 10, null, 5000 );
    }

    @Test
    public void testGetDatasetsAnnotationsWithLimitIsSupplied() {
        assertThat( target( "/datasets/annotations" ).queryParam( "limit", 50 ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "limit", 50 )
                .extracting( "groupBy", InstanceOfAssertFactories.list( String.class ) )
                .containsExactly( "classUri", "className", "termUri", "termName" );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( Filters.empty(), null, null, null, null, 0, null, 50 );
    }

    @Test
    public void testGetDatasetsAnnotationsForUncategorizedTerms() {
        assertThat( target( "/datasets/annotations" ).queryParam( "category", "" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( Filters.empty(), null, ExpressionExperimentService.UNCATEGORIZED, null, null, 0, null, 100 );
    }

    @Test
    public void testGetDatasetsCategories() throws SearchException {
        assertThat( target( "/datasets/categories" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).getCategoriesUsageFrequency( Filters.empty(), null, null, null, null, 20 );
    }

    @Test
    public void testGetDatasetQuantitationTypes() {
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getQuantitationTypeValueObjects( ee ) ).thenReturn( Collections.emptyList() );
        assertThat( target( "/datasets/1/quantitationTypes" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).load( 1L );
        verify( expressionExperimentService ).getQuantitationTypeValueObjects( ee );
    }

    @Test
    public void testGetDatasetProcessedExpression() throws IOException {
        when( expressionExperimentService.hasProcessedExpressionData( eq( ee ) ) ).thenReturn( true );
        assertThat( target( "/datasets/1/data/processed" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" );
        verify( expressionExperimentService ).hasProcessedExpressionData( eq( ee ) );
        verify( expressionDataFileService ).writeProcessedExpressionData( eq( ee ), any() );
    }

    @Test
    public void testGetDatasetProcessedExpressionWhenNoProcessedVectorsExist() throws IOException {
        when( expressionExperimentService.hasProcessedExpressionData( eq( ee ) ) ).thenReturn( false );
        assertThat( target( "/datasets/1/data/processed" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).load( 1L );
        verify( expressionExperimentService ).hasProcessedExpressionData( eq( ee ) );
        verifyNoMoreInteractions( expressionExperimentService );
        verifyNoInteractions( expressionDataFileService );
    }

    @Test
    public void testGetDatasetRawExpression() throws IOException {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        when( expressionExperimentService.getPreferredQuantitationType( ee ) )
                .thenReturn( qt );
        assertThat( target( "/datasets/1/data/raw" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" );
        verify( expressionExperimentService ).getPreferredQuantitationType( ee );
        verifyNoInteractions( quantitationTypeService );
        verify( expressionDataFileService ).writeRawExpressionData( eq( ee ), eq( qt ), any() );
    }

    @Test
    public void testGetDatasetRawExpressionByQuantitationTypeWhenQtIsNotFromTheDataset() throws IOException {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setId( 12L );
        when( quantitationTypeService.load( 12L ) ).thenReturn( qt );
        when( quantitationTypeService.existsByExpressionExperimentAndVectorType( qt, ee, RawExpressionDataVector.class ) ).thenReturn( false );
        Response res = target( "/datasets/1/data/raw" )
                .queryParam( "quantitationType", "12" ).request().get();
        verify( quantitationTypeService ).load( 12L );
        verify( quantitationTypeService ).existsByExpressionExperimentAndVectorType( qt, ee, RawExpressionDataVector.class );
        verifyNoInteractions( expressionDataFileService );
        assertThat( res )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetRawExpressionByQuantitationType() throws IOException {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setId( 12L );
        when( quantitationTypeService.load( 12L ) ).thenReturn( qt );
        when( quantitationTypeService.existsByExpressionExperimentAndVectorType( qt, ee, RawExpressionDataVector.class ) ).thenReturn( true );
        Response res = target( "/datasets/1/data/raw" )
                .queryParam( "quantitationType", "12" ).request().get();
        verify( quantitationTypeService ).load( 12L );
        verify( quantitationTypeService ).existsByExpressionExperimentAndVectorType( qt, ee, RawExpressionDataVector.class );
        verify( expressionDataFileService ).writeRawExpressionData( eq( ee ), eq( qt ), any() );
        assertThat( res ).hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" );
    }

    @Test
    public void testGetBlacklistedDatasets() {
        when( expressionExperimentService.loadBlacklistedValueObjects( any(), any(), anyInt(), anyInt() ) )
                .thenAnswer( a -> new Slice<>( Collections.emptyList(), a.getArgument( 1 ), a.getArgument( 2 ), a.getArgument( 3 ), 0L ) );
        when( expressionExperimentService.getSort( "id", Sort.Direction.ASC ) ).thenReturn( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) );
        Response res = target( "/datasets/blacklisted" )
                .queryParam( "filter", "" ).request().get();
        assertThat( res ).hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).loadBlacklistedValueObjects( Filters.empty(), Sort.by( "ee", "id", Sort.Direction.ASC, "id" ), 0, 20 );
    }

    @Test
    public void testGetDatasetAnnotations() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/annotations" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasHeader( "Cache-Control", "max-age=1200" );
        verify( expressionExperimentService ).load( 1L );
        verify( expressionExperimentService ).getAnnotationsById( 1L );
    }

    @Test
    public void testGetDatasetsDifferentialAnalysisResultsExpressionForGene() {
        Gene brca1 = new Gene();
        when( geneArgService.getEntity( any() ) ).thenReturn( brca1 );
        assertThat( target( "/datasets/analyses/differential/results/gene/BRCA1" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "filter", "" )
                .hasFieldOrPropertyWithValue( "sort", "+pValue" );
        verify( differentialExpressionResultService ).findBestResultByGeneAndExperimentAnalyzedGroupedBySourceExperimentId( eq( brca1 ), any(), any() );
    }
}