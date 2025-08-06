package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.service.DifferentialExpressionAnalysisResultListFileService;
import ubic.gemma.core.analysis.service.ExpressionAnalysisResultSetFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionExperimentDataFileType;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.BaseJerseyTest;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.Mockito.*;
import static ubic.gemma.rest.DatasetsWebService.TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE;
import static ubic.gemma.rest.util.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class DatasetsWebServiceTest extends BaseJerseyTest {

    @Import(JacksonConfig.class)
    @Configuration
    @TestComponent
    static class DatasetsWebServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public OpenAPI openApi() {
            return new OpenAPI()
                    .info( new Info().version( "1.0.0" ) );
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }

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
        public TaxonArgService taxonArgService() {
            return mock();
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

        @Bean
        public ExpressionAnalysisResultSetService expressionAnalysisResultSetService() {
            return mock();
        }

        @Bean
        public ExpressionAnalysisResultSetFileService expressionAnalysisResultSetFileService() {
            return mock();
        }

        @Bean
        public ExpressionAnalysisResultSetArgService expressionAnalysisResultSetArgService() {
            return mock();
        }

        @Bean
        public DatabaseEntryArgService databaseEntryArgService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentReportService expressionExperimentReportService() {
            return mock();
        }

        @Bean
        public TableMaintenanceUtil tableMaintenanceUtil() {
            return mock();
        }

        @Bean
        public ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService() {
            return mock();
        }

        @Bean
        public DifferentialExpressionAnalysisResultListFileService differentialExpressionAnalysisResultListFileService() {
            return mock();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }

        @Bean
        public AsyncTaskExecutor taskExecutor() {
            return mock( AsyncTaskExecutor.class );
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
    private TaxonArgService taxonArgService;

    @Autowired
    private GeneArgService geneArgService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    private ExpressionExperiment ee;

    @Before
    public void setUpMocks() throws TimeoutException {
        ee = ExpressionExperiment.Factory.newInstance();
        //noinspection unchecked
        Set<String> universe = mock( Set.class );
        when( universe.contains( any( String.class ) ) ).thenReturn( true );
        when( expressionExperimentService.getFilterableProperties() ).thenReturn( universe );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getFiltersWithInferredAnnotations( any(), any(), any(), anyLong(), any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( expressionExperimentService.getSort( any(), any(), any() ) ).thenAnswer( a -> Sort.by( null, a.getArgument( 0 ), a.getArgument( 1 ), a.getArgument( 2 ) ) );
    }

    @After
    public void resetMocks() {
        reset( expressionExperimentService, quantitationTypeService, analyticsProvider, expressionDataFileService, taxonArgService, geneArgService, searchService );
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
    public void testGetDatasetsWithQuery() throws SearchException, TimeoutException {
        List<Long> ids = Arrays.asList( 1L, 3L, 5L );
        List<SearchResult<ExpressionExperiment>> results = ids.stream()
                .map( this::createMockSearchResult )
                .collect( Collectors.toList() );
        SearchService.SearchResultMap map = mock( SearchService.SearchResultMap.class );
        when( map.getByResultObjectType( ExpressionExperiment.class ) )
                .thenReturn( results );
        when( searchService.search( any(), any() ) ).thenReturn( map );
        when( expressionExperimentService.loadIdsWithCache( any(), any() ) ).thenReturn( ids );
        assertThat( target( "/datasets" ).queryParam( "query", "cerebellum" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        ArgumentCaptor<SearchSettings> captor = ArgumentCaptor.forClass( SearchSettings.class );
        verify( searchService ).search( argThat( arg -> arg.getQuery().equals( "cerebellum" ) ), argThat( ctx -> ctx.getHighlighter() == null ) );
        verify( searchService ).search( argThat( arg -> arg.getQuery().equals( "cerebellum" ) ), argThat( ctx -> ctx.getHighlighter() != null ) );
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.empty(), null, Collections.emptySet(), 30, TimeUnit.SECONDS );
        verify( expressionExperimentService ).loadIdsWithCache( Filters.empty(), null );
        verify( expressionExperimentService ).loadValueObjectsByIdsWithRelationsAndCache( ids );
        verifyNoMoreInteractions( expressionExperimentService );
    }

    @Test
    public void testGetDatasetsWithQueryAndSort() throws SearchException, TimeoutException {
        List<Long> ids = Arrays.asList( 1L, 3L, 5L );
        List<SearchResult<ExpressionExperiment>> results = ids.stream()
                .map( this::createMockSearchResult )
                .collect( Collectors.toList() );
        SearchService.SearchResultMap map = mock( SearchService.SearchResultMap.class );
        when( map.getByResultObjectType( ExpressionExperiment.class ) )
                .thenReturn( results );
        when( searchService.search( any(), any() ) ).thenReturn( map );
        when( expressionExperimentService.loadIdsWithCache( any(), any() ) ).thenReturn( ids );
        assertThat( target( "/datasets" ).queryParam( "query", "cerebellum" ).queryParam( "sort", "-lastUpdated" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        ArgumentCaptor<SearchSettings> captor = ArgumentCaptor.forClass( SearchSettings.class );
        verify( searchService, times( 2 ) ).search( captor.capture(), any() );
        assertThat( captor.getAllValues() )
                .hasSize( 2 )
                .satisfiesExactly( s -> {
                    assertThat( s.getQuery() ).isEqualTo( "cerebellum" );
                    assertThat( s.isFillResults() ).isFalse();
                }, s -> {
                    assertThat( s.getQuery() ).isEqualTo( "cerebellum" );
                    assertThat( s.isFillResults() ).isFalse();
                } );
        verify( expressionExperimentService ).getSort( "lastUpdated", Sort.Direction.DESC, Sort.NullMode.LAST );
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.empty(), null, Collections.emptySet(), 30, TimeUnit.SECONDS );
        verify( expressionExperimentService ).loadIdsWithCache( Filters.empty(), Sort.by( null, "lastUpdated", Sort.Direction.DESC, Sort.NullMode.LAST ) );
        verify( expressionExperimentService ).loadValueObjectsByIdsWithRelationsAndCache( ids );
        verifyNoMoreInteractions( expressionExperimentService );
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
        verify( expressionExperimentService ).getSort( "geeq.publicQualityScore", Sort.Direction.ASC, Sort.NullMode.LAST );
        verify( expressionExperimentService )
                .loadValueObjectsWithCache(
                        any(),
                        eq( Sort.by( null, "geeq.publicQualityScore", Sort.Direction.ASC, Sort.NullMode.LAST, "geeq.publicQualityScore" ) ),
                        eq( 0 ),
                        eq( 20 ) );
    }

    @Test
    public void testGetDatasetsWhenInferenceTimeoutThenProduce503ServiceUnavailable() throws TimeoutException {
        //noinspection unchecked
        when( expressionExperimentService.getFilter( eq( "allCharacteristic.valueUri" ), eq( Filter.Operator.in ), anyCollection() ) )
                .thenAnswer( a -> Filter.by( "c", "valueUri", String.class, Filter.Operator.in, a.getArgument( 2, Collection.class ) ) );
        when( expressionExperimentService.getFiltersWithInferredAnnotations( any(), any(), any(), anyLong(), any() ) )
                .thenThrow( new TimeoutException( "Inference timed out!" ) );
        when( expressionExperimentService.loadValueObjectsWithCache( any(), any(), anyInt(), anyInt() ) )
                .thenReturn( new Slice<>( Collections.emptyList(), null, null, null, null ) );
        assertThat( target( "/datasets" ).queryParam( "filter", "allCharacteristic.valueUri in (a, b, c)" ).request().get() )
                .hasStatus( Response.Status.SERVICE_UNAVAILABLE )
                .hasHeaderSatisfying( "Retry-After", values -> assertThat( values ).isNotEmpty() )
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
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.by( f ), null, new HashSet<>(), 30, TimeUnit.SECONDS );
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
                .extracting( "groupBy", list( String.class ) )
                .containsExactly( "classUri", "className", "termUri", "termName" );
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.empty(), Collections.emptySet(), new HashSet<>(), 30000, TimeUnit.MILLISECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), isNull(), isNull(), isNull(), eq( 0 ), eq( Collections.emptySet() ), eq( 100 ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
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
                .extracting( "groupBy", list( String.class ) )
                .containsExactly( "classUri", "className", "termUri", "termName" );
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.empty(), null, new HashSet<>(), 30000, TimeUnit.MILLISECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), isNull(), isNull(), isNull(), eq( 0 ), isNull(), eq( 100 ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
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
        verify( expressionExperimentService ).getFiltersWithInferredAnnotations( Filters.empty(), null, new HashSet<>(), 30000, TimeUnit.MILLISECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), isNull(), isNull(), isNull(), eq( 10 ), isNull(), eq( 5000 ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testGetDatasetsAnnotationsWithLimitIsSupplied() throws TimeoutException {
        assertThat( target( "/datasets/annotations" ).queryParam( "limit", 50 ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "limit", 50 )
                .extracting( "groupBy", list( String.class ) )
                .containsExactly( "classUri", "className", "termUri", "termName" );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), isNull(), isNull(), isNull(), eq( 0 ), isNull(), eq( 50 ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testGetDatasetsAnnotationsForUncategorizedTerms() throws TimeoutException {
        assertThat( target( "/datasets/annotations" ).queryParam( "category", "" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), eq( ExpressionExperimentService.UNCATEGORIZED ), isNull(), isNull(), eq( 0 ), isNull(), eq( 100 ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testGetDatasetsCategories() {
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
    public void testGetDatasetProcessedExpression() throws IOException, URISyntaxException, InterruptedException, TimeoutException, FilteringException {
        when( expressionExperimentService.hasProcessedExpressionData( eq( ee ) ) ).thenReturn( true );
        when( expressionDataFileService.writeOrLocateProcessedDataFile( ee, false, false, 5, TimeUnit.SECONDS ) )
                .thenReturn( Optional.of( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.txt.gz" ) ).toURI() ), true ) ) );
        assertThat( target( "/datasets/1/data/processed" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .hasHeaderWithValue( "Content-Disposition", "attachment; filename=\"data.txt\"" )
                .hasEncoding( "gzip" );
        verify( expressionExperimentService ).hasProcessedExpressionData( ee );
        verify( expressionDataFileService ).writeOrLocateProcessedDataFile( ee, false, false, 5, TimeUnit.SECONDS );
    }

    @Test
    public void testGetDatasetProcessedExpressionWhenNoProcessedVectorsExist() {
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
    public void testGetDatasetRawExpression() throws IOException, URISyntaxException, InterruptedException, TimeoutException {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        when( expressionExperimentService.getPreferredQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( expressionDataFileService.writeOrLocateRawExpressionDataFile( ee, qt, false, 5, TimeUnit.SECONDS ) )
                .thenReturn( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.txt.gz" ) ).toURI() ), true ) );
        assertThat( target( "/datasets/1/data/raw" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .hasHeaderWithValue( "Content-Disposition", "attachment; filename=\"data.txt\"" )
                .hasEncoding( "gzip" );
        verify( expressionExperimentService ).getPreferredQuantitationType( ee );
        verifyNoInteractions( quantitationTypeService );
        verify( expressionDataFileService ).writeOrLocateRawExpressionDataFile( ee, qt, false, 5, TimeUnit.SECONDS );
    }

    @Test
    public void testGetDatasetRawExpressionByQuantitationTypeWhenQtIsNotFromTheDataset() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setId( 12L );
        when( quantitationTypeService.load( 12L ) ).thenReturn( qt );
        when( quantitationTypeService.loadByIdAndVectorType( 12L, ee, RawExpressionDataVector.class ) ).thenReturn( null );
        Response res = target( "/datasets/1/data/raw" )
                .queryParam( "quantitationType", "12" ).request().get();
        verify( quantitationTypeService ).loadByIdAndVectorType( 12L, ee, RawExpressionDataVector.class );
        verifyNoInteractions( expressionDataFileService );
        assertThat( res )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetRawExpressionByQuantitationType() throws IOException, URISyntaxException, InterruptedException, TimeoutException {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setId( 12L );
        when( quantitationTypeService.load( 12L ) ).thenReturn( qt );
        when( quantitationTypeService.loadByIdAndVectorType( 12L, ee, RawExpressionDataVector.class ) ).thenReturn( qt );

        when( expressionDataFileService.writeOrLocateRawExpressionDataFile( ee, qt, false, 5, TimeUnit.SECONDS ) )
                .thenReturn( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.txt.gz" ) ).toURI() ), true ) );
        Response res = target( "/datasets/1/data/raw" )
                .queryParam( "quantitationType", "12" ).request().get();
        verify( quantitationTypeService ).loadByIdAndVectorType( 12L, ee, RawExpressionDataVector.class );
        verify( expressionDataFileService ).writeOrLocateRawExpressionDataFile( ee, qt, false, 5, TimeUnit.SECONDS );
        assertThat( res ).hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .hasHeaderWithValue( "Content-Disposition", "attachment; filename=\"data.txt\"" )
                .hasEncoding( "gzip" );
    }

    @Test
    public void testGetBlacklistedDatasets() {
        when( expressionExperimentService.loadBlacklistedValueObjects( any(), any(), anyInt(), anyInt() ) )
                .thenAnswer( a -> new Slice<>( Collections.emptyList(), a.getArgument( 1 ), a.getArgument( 2 ), a.getArgument( 3 ), 0L ) );
        when( expressionExperimentService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ) ).thenReturn( Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) );
        Response res = target( "/datasets/blacklisted" )
                .queryParam( "filter", "" ).request().get();
        assertThat( res ).hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).loadBlacklistedValueObjects( Filters.empty(), Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ), 0, 20 );
    }

    @Test
    public void testGetDatasetAnnotations() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/annotations" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasHeaderWithValue( "Cache-Control", "max-age=1200" );
        verify( expressionExperimentService ).load( 1L );
        verify( expressionExperimentService ).getAnnotations( ee );
    }

    @Test
    public void testGetDatasetsDifferentialAnalysisResultsExpressionForGene() {
        Gene brca1 = new Gene();
        when( geneArgService.getEntity( any() ) ).thenReturn( brca1 );
        assertThat( target( "/datasets/analyses/differential/results/genes/BRCA1" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" )
                .entity()
                .hasFieldOrPropertyWithValue( "filter", "" )
                .hasFieldOrPropertyWithValue( "sort.direction", "+" )
                .hasFieldOrPropertyWithValue( "sort.orderBy", "sourceExperimentId" )
                .extracting( "groupBy", list( String.class ) )
                .containsExactly( "sourceExperimentId", "experimentAnalyzedId", "resultSetId" );
        verify( differentialExpressionResultService ).findByGeneAndExperimentAnalyzed( eq( brca1 ), any(), any(), any(), any(), anyDouble(), eq( false ), eq( true ) );
    }

    @Test
    public void testGetDatasetsDifferentialAnalysisResultsExpressionForGeneInTaxa() {
        Taxon human = new Taxon();
        Gene brca1 = new Gene();
        when( taxonArgService.getEntity( any() ) ).thenReturn( human );
        when( geneArgService.getEntityWithTaxon( any(), eq( human ) ) ).thenReturn( brca1 );
        assertThat( target( "/datasets/analyses/differential/results/taxa/human/genes/BRCA1" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" )
                .entity()
                .hasFieldOrPropertyWithValue( "filter", "" )
                .hasFieldOrPropertyWithValue( "sort.direction", "+" )
                .hasFieldOrPropertyWithValue( "sort.orderBy", "sourceExperimentId" )
                .extracting( "groupBy", list( String.class ) )
                .containsExactly( "sourceExperimentId", "experimentAnalyzedId", "resultSetId" );
        verify( differentialExpressionResultService ).findByGeneAndExperimentAnalyzed( eq( brca1 ), any(), any(), any(), any(), anyDouble(), eq( false ), eq( true ) );
    }

    @Test
    public void testGetDatasetsAnalysisResultSets() {
        ee.setId( 1L );
        when( expressionAnalysisResultSetService.findByBioAssaySetInAndDatabaseEntryInLimit( any(), isNull(), isNull(), anyInt(), anyInt(), isNull() ) )
                .thenReturn( new Slice<>( Collections.emptyList(), null, null, null, null ) );
        assertThat( target( "/datasets/1/analyses/differential/resultSets" ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    @WithMockUser
    public void testRefreshDataset() {
        ee.setId( 1L );
        when( expressionExperimentService.loadAndThawLiteWithRefreshCacheMode( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadValueObject( ee ) ).thenReturn( new ExpressionExperimentValueObject( ee ) );
        assertThat( target( "/datasets/1/refresh" )
                .queryParam( "refreshVectors", true )
                .queryParam( "refreshReports", true )
                .request().get() )
                .hasStatus( Response.Status.CREATED )
                .hasHeaderSatisfying( "Location", locations -> {
                    assertThat( locations )
                            .hasSize( 1 )
                            .first()
                            .asString()
                            .endsWith( "/datasets/1" );
                } )
                .entity();
        verify( expressionExperimentService ).loadAndThawLiteWithRefreshCacheMode( 1L );
        verify( processedExpressionDataVectorService ).evictFromCache( ee );
        verify( expressionExperimentService ).loadValueObject( ee );
        verify( expressionExperimentReportService ).evictFromCache( 1L );
    }

    @Test
    public void testGetDatasetSingleCellData() throws InterruptedException, TimeoutException, URISyntaxException, IOException {
        QuantitationType qt = new QuantitationType();
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( expressionDataFileService.getDataFile( eq( ee ), eq( qt ), eq( ExpressionExperimentDataFileType.TABULAR ), anyBoolean(), anyLong(), any() ) )
                .thenReturn( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.txt.gz" ) ).toURI() ), true ) );
        assertThat( target( "/datasets/1/data/singleCell" ).request()
                .accept( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE ).get() )
                .hasStatus( Response.Status.OK )
                .hasMediaType( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .hasEncoding( "gzip" )
                .hasHeaderWithValue( "Content-Disposition", "attachment; filename=\"data.txt\"" );
    }

    @Test
    public void testGetDatasetSingleCellDataAsDownload() throws InterruptedException, TimeoutException, URISyntaxException, IOException {
        QuantitationType qt = new QuantitationType();
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( expressionDataFileService.getDataFile( eq( ee ), eq( qt ), eq( ExpressionExperimentDataFileType.TABULAR ), anyBoolean(), anyLong(), any() ) )
                .thenReturn( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.txt.gz" ) ).toURI() ), true ) );
        assertThat( target( "/datasets/1/data/singleCell" ).queryParam( "download", "true" ).request()
                .accept( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE ).get() )
                .hasStatus( Response.Status.OK )
                .hasMediaType( MediaType.APPLICATION_OCTET_STREAM_TYPE )
                .doesNotHaveEncoding( "gzip" )
                .hasHeaderWithValue( "Content-Disposition", "attachment; filename=\"data.txt.gz\"" );
    }

    @Test
    public void testGetDatasetSingleCellDataAsMex() throws InterruptedException, TimeoutException, URISyntaxException, IOException {
        QuantitationType qt = new QuantitationType();
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( expressionDataFileService.getDataFile( eq( ee ), eq( qt ), eq( ExpressionExperimentDataFileType.MEX ), anyBoolean(), anyLong(), any() ) )
                .thenReturn( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.mex" ) ).toURI() ), true ) );
        assertThat( target( "/datasets/1/data/singleCell" ).request()
                .accept( DatasetsWebService.APPLICATION_10X_MEX_TYPE ).get() )
                .hasStatus( Response.Status.OK )
                .hasMediaType( DatasetsWebService.APPLICATION_10X_MEX_TYPE )
                .doesNotHaveEncoding( "gzip" )
                .hasHeaderWithValue( "Content-Disposition", "attachment; filename=\"data.mex.tar\"" )
                .entityAsStream()
                .satisfies( is -> {
                    List<String> files = new ArrayList<>();
                    try ( TarArchiveInputStream tais = new TarArchiveInputStream( is ) ) {
                        TarArchiveEntry entry;
                        while ( ( entry = tais.getNextEntry() ) != null ) {
                            files.add( entry.getName() );
                        }
                    }
                    assertThat( files )
                            .containsExactlyInAnyOrder(
                                    "A/barcodes.tsv.gz",
                                    "A/features.tsv.gz",
                                    "A/matrix.mtx.gz",
                                    "B/barcodes.tsv.gz",
                                    "B/features.tsv.gz",
                                    "B/matrix.mtx.gz",
                                    "C/barcodes.tsv.gz",
                                    "C/features.tsv.gz",
                                    "C/matrix.mtx.gz",
                                    "D/barcodes.tsv.gz",
                                    "D/features.tsv.gz",
                                    "D/matrix.mtx.gz"
                            );
                } );
    }

    @Test
    public void testGetDatasetSubSetGroups() {
        BioAssayDimension bad = new BioAssayDimension();
        List<ExpressionExperimentSubSet> subsets = Collections.singletonList( ExpressionExperimentSubSet.Factory.newInstance( "test", ee ) );
        when( expressionExperimentService.getSubSetsByDimension( ee ) ).thenReturn( Collections.singletonMap( bad, new HashSet<>( subsets ) ) );
        ExperimentalFactor factor = new ExperimentalFactor();
        FactorValue fv = FactorValue.Factory.newInstance( factor );
        when( expressionExperimentService.getSubSetsByFactorValue( ee, bad ) ).thenReturn( Collections.singletonMap( factor, Collections.singletonMap( fv, subsets.iterator().next() ) ) );
        assertThat( target( "/datasets/1/subSetGroups" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetSubSetGroup() {
        BioAssayDimension bad = new BioAssayDimension();
        List<ExpressionExperimentSubSet> subsets = Collections.singletonList( ExpressionExperimentSubSet.Factory.newInstance( "test", ee ) );
        when( expressionExperimentService.getBioAssayDimensionById( ee, 1L ) ).thenReturn( bad );
        when( expressionExperimentService.getSubSetsByDimension( ee ) ).thenReturn( Collections.singletonMap( bad, new HashSet<>( subsets ) ) );
        ExperimentalFactor factor = new ExperimentalFactor();
        FactorValue fv = FactorValue.Factory.newInstance( factor );
        when( expressionExperimentService.getSubSetsByFactorValue( ee, bad ) ).thenReturn( Collections.singletonMap( factor, Collections.singletonMap( fv, subsets.iterator().next() ) ) );
        assertThat( target( "/datasets/1/subSetGroups/1" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }


    @Test
    public void testGetDatasetSubSets() {
        BioAssayDimension bad = new BioAssayDimension();
        List<ExpressionExperimentSubSet> subsets = Collections.singletonList( ExpressionExperimentSubSet.Factory.newInstance( "test", ee ) );
        when( expressionExperimentService.getSubSetsByDimension( ee ) ).thenReturn( Collections.singletonMap( bad, new HashSet<>( subsets ) ) );
        ExperimentalFactor factor = new ExperimentalFactor();
        FactorValue fv = FactorValue.Factory.newInstance( factor );
        when( expressionExperimentService.getSubSetsByFactorValue( ee, bad ) ).thenReturn( Collections.singletonMap( factor, Collections.singletonMap( fv, subsets.iterator().next() ) ) );
        assertThat( target( "/datasets/1/subSets" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetSubSet() {
        BioAssayDimension bad = new BioAssayDimension();
        List<ExpressionExperimentSubSet> subsets = Collections.singletonList( ExpressionExperimentSubSet.Factory.newInstance( "test", ee ) );
        when( expressionExperimentService.getSubSetsByDimension( ee ) ).thenReturn( Collections.singletonMap( bad, new HashSet<>( subsets ) ) );
        when( expressionExperimentService.getSubSetByIdWithCharacteristics( ee, 1L ) ).thenReturn( subsets.iterator().next() );
        assertThat( target( "/datasets/1/subSets/1" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetSubSetSamples() {
        BioAssayDimension bad = new BioAssayDimension();
        when( expressionExperimentService.getBioAssayDimensionById( ee, 1L ) ).thenReturn( bad );
        List<ExpressionExperimentSubSet> subsets = Collections.singletonList( ExpressionExperimentSubSet.Factory.newInstance( "test", ee ) );
        when( expressionExperimentService.getSubSetByIdWithCharacteristicsAndBioAssays( ee, 1L ) ).thenReturn( subsets.iterator().next() );
        ExperimentalFactor factor = new ExperimentalFactor();
        FactorValue fv = FactorValue.Factory.newInstance( factor );
        when( expressionExperimentService.getSubSetsByFactorValue( ee, bad ) ).thenReturn( Collections.singletonMap( factor, Collections.singletonMap( fv, subsets.iterator().next() ) ) );
        assertThat( target( "/datasets/1/subSets/1/samples" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetAllPublications() {
        when( expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( 1L ) ).thenReturn( ee );
        BibliographicReference prim_ref = new BibliographicReference();
        prim_ref.setId( 1L );
        BibliographicReference second_ref = new BibliographicReference();
        second_ref.setId( 2L );
        BibliographicReference third_ref = new BibliographicReference();
        third_ref.setId( 3L );

        Set<BibliographicReference> other_pubs = new HashSet<>();
        other_pubs.add( prim_ref );
        other_pubs.add( second_ref );
        other_pubs.add( third_ref );
        ee.setPrimaryPublication( prim_ref );
        ee.setOtherRelevantPublications( other_pubs );

        assertThat( target( "/datasets/1/publications" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" );
    }

    private static class DummyLockedPath implements LockedPath {

        private final Path path;
        private final boolean shared;

        private DummyLockedPath( Path path, boolean shared ) {
            this.path = path;
            this.shared = shared;
        }

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean isShared() {
            return shared;
        }

        @Override
        public void close() {

        }

        @Override
        public Path closeAndGetPath() {
            return path;
        }

        @Override
        public LockedPath toExclusive() {
            return new DummyLockedPath( path, false );
        }

        @Override
        public LockedPath toExclusive( long timeout, TimeUnit timeUnit ) {
            return new DummyLockedPath( path, false );
        }

        @Override
        public LockedPath toShared() {
            return new DummyLockedPath( path, true );
        }

        @Override
        public LockedPath steal() {
            return this;
        }

        @Override
        public LockedPath stealWithPath( Path path ) {
            return new DummyLockedPath( path, shared );
        }
    }
}