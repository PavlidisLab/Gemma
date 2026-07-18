package ubic.gemma.rest;

import ubic.gemma.core.security.SecurityService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.service.DifferentialExpressionAnalysisResultListFileService;
import ubic.gemma.core.analysis.service.ExpressionAnalysisResultSetFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionExperimentDataFileType;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.ontology.OntologyTermValidator;
import ubic.gemma.core.ontology.TermViolation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.DesignPreflightReport;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.*;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.BaseJerseyTest5;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.args.*;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static ubic.gemma.rest.util.JsonAssert.json;
import static org.mockito.Mockito.*;
import static ubic.gemma.rest.DatasetsWebService.TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE;
import static ubic.gemma.rest.util.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class DatasetsWebServiceTest extends BaseJerseyTest5 {

    @Import(JacksonConfig.class)
    @Configuration
    @TestComponent
    static class DatasetsWebServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080", "gemma.ontology.validation.olsFailClosed=true" );
        }

        @Bean
        public Future<OpenAPI> openApi() {
            return constantFuture( new OpenAPI()
                    .info( new Info().version( "1.0.0" ) ) );
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
        public SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService() {
            return mock( SampleCoexpressionAnalysisService.class );
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
        public AuditTrailService auditTrailService() {
            return mock( AuditTrailService.class );
        }

        @Bean
        public SecurityService securityService() {
            return mock( SecurityService.class );
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
        public ubic.gemma.core.ontology.OntologyTermValidator ontologyTermValidator() {
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
        public TaskRunningService taskRunningService() {
            return mock();
        }

        @Bean
        public GeeqService geeqService() {
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

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "http://localhost:8080" );
        }

        // Phase B-1 ticket layer dependencies — DatasetsWebService injects
        // TicketsWebService (for the /datasets/{id}/tickets read shim) plus
        // TicketService + UserManager (for the legacy /curationDetails write
        // shim, which now routes troubled/needsAttention flips through
        // TicketService.openTicket / transition).
        @Bean
        public TicketService ticketService() {
            return mock( TicketService.class );
        }

        @Bean
        public UserManager userManager() {
            return mock( UserManager.class );
        }

        @Bean
        public UserReadService userReadService() {
            return mock( UserReadService.class );
        }

        @Bean
        public TicketsWebService ticketsWebService( TicketService ticketService, UserManager userManager, UserReadService userReadService ) {
            return new TicketsWebService( ticketService, userManager, userReadService );
        }

        // DatasetsWebService also @Autowires CurationWebService, GroupsWebService,
        // BioAssayService, and OutlierFlaggingService. Provide them here so the
        // Spring context loads. CurationWebService is a concrete @Service with
        // its own @Autowired fields, so we also expose its transitive deps
        // (DatasetArgService already present; AnnotationSetService added below)
        // and register the real class — letting Spring autowire mock fields onto
        // it is cleaner than mocking the class itself, which would cause Spring
        // to re-run AutowiredAnnotationBeanPostProcessor against unsatisfied
        // dependencies.
        @Bean
        public ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService annotationSetService() {
            return mock( ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService.class );
        }

        @Bean
        public AnnotationSetsWebService annotationSetsWebService() {
            return new AnnotationSetsWebService();
        }

        @Bean
        public GroupsWebService groupsWebService( UserManager userManager, UserReadService userReadService ) {
            return new GroupsWebService( userManager, userReadService );
        }

        @Bean
        public BioAssayService bioAssayService() {
            return mock( BioAssayService.class );
        }

        @Bean
        public ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService bioMaterialService() {
            return mock( ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService.class );
        }

        @Bean
        public ubic.gemma.core.analysis.service.OutlierFlaggingService outlierFlaggingService() {
            return mock( ubic.gemma.core.analysis.service.OutlierFlaggingService.class );
        }

        @Bean
        public ubic.gemma.core.analysis.preprocess.OutlierDetectionService outlierDetectionService() {
            return mock( ubic.gemma.core.analysis.preprocess.OutlierDetectionService.class );
        }

        @Bean
        public ubic.gemma.persistence.service.expression.experiment.FactorValueService factorValueService() {
            return mock( ubic.gemma.persistence.service.expression.experiment.FactorValueService.class );
        }

        @Bean
        public ubic.gemma.persistence.service.expression.experiment.FactorValueNeedsAttentionService factorValueNeedsAttentionService() {
            return mock( ubic.gemma.persistence.service.expression.experiment.FactorValueNeedsAttentionService.class );
        }

        @Bean
        public ubic.gemma.core.analysis.service.ExpressionDataDeleterService expressionDataDeleterService() {
            return mock( ubic.gemma.core.analysis.service.ExpressionDataDeleterService.class );
        }

        @Bean
        public ubic.gemma.persistence.service.common.description.BibliographicReferenceService bibliographicReferenceService() {
            return mock( ubic.gemma.persistence.service.common.description.BibliographicReferenceService.class );
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ubic.gemma.persistence.service.common.description.BibliographicReferenceService bibliographicReferenceService;

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

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

    @Autowired
    private GeeqService geeqService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private TaskRunningService taskRunningService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private UserManager userManager;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private OntologyTermValidator ontologyTermValidator;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUpMocks() throws TimeoutException {
        ee = ExpressionExperiment.Factory.newInstance();
        //noinspection unchecked
        Set<String> universe = mock( Set.class );
        when( universe.contains( any( String.class ) ) ).thenReturn( true );
        when( expressionExperimentService.getFilterableProperties() ).thenReturn( universe );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getEnhancedFilters( any(), any(), any(), anyLong(), any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        when( expressionExperimentService.getSort( any(), any(), any() ) ).thenAnswer( a -> Sort.by( null, a.getArgument( 0 ), a.getArgument( 1 ), a.getArgument( 2 ) ) );
        // Curation-details PUT routes troubled/needsAttention flips through TicketService, which requires
        // a non-null current user. @WithMockUser populates the SecurityContext but the UserManager mock
        // returns null by default; stub a User so applyFlagViaTickets clears its "No authenticated user"
        // guard.
        User actor = mock( User.class );
        when( userManager.getCurrentUser() ).thenReturn( actor );
    }

    @AfterEach
    public void resetMocks() {
        reset( expressionExperimentService, quantitationTypeService, analyticsProvider, expressionDataFileService, taxonArgService, geneArgService, searchService, auditEventService, auditTrailService, securityService, geeqService, taskRunningService, differentialExpressionAnalysisService, userManager, ticketService, sampleCoexpressionAnalysisService, svdService, processedExpressionDataVectorService, expressionExperimentReportService, arrayDesignService, bibliographicReferenceService, ontologyTermValidator );
    }

    private static final String HALLUCINATED_TAG_BODY = "{\"tags\":{\"items\":[{\"clientRef\":\"t7\","
            + "\"value\":{\"label\":\"has_genotype\",\"uri\":\"http://purl.obolibrary.org/obo/TGEMO_00166\"}}]}}";

    /** A tag whose label doesn't match its URI is rejected with a structured, per-slot 400. */
    @Test
    public void testCommitRejectsUngroundedTerm() {
        when( ontologyTermValidator.validateAndCanonicalize( any() ) ).thenReturn( Collections.singletonList(
                new TermViolation( "value", "has_genotype", "http://purl.obolibrary.org/obo/TGEMO_00166", "delivered at dose", TermViolation.Reason.LABEL_MISMATCH ) ) );
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( HALLUCINATED_TAG_BODY ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.error.errors[0].reason", "LABEL_MISMATCH" )
                    .hasPathWithValue( "$.error.errors[0].location", "tags[clientRef=t7].value" )
                    .hasPathWithValue( "$.error.errors[0].locationType", "BODY" );
        }
        // nothing was persisted
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /** A design-section factor-value statement with an ungrounded term is rejected, located in the design tree. */
    @Test
    public void testCommitRejectsUngroundedDesignStatementTerm() {
        when( expressionExperimentService.thawBioAssays( any() ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( any() ) ).thenReturn( new ExperimentalDesignValueObject() );
        when( expressionExperimentService.previewDesignChange( any(), any() ) ).thenReturn( new DesignPreflightReport() );
        // only the statement (a Statement entity) fails; the factor category passes
        when( ontologyTermValidator.validateAndCanonicalize( any() ) ).thenAnswer( inv -> {
            Characteristic c = inv.getArgument( 0 );
            return ( c instanceof Statement )
                    ? Collections.singletonList( new TermViolation( "object", "Heterozygous", "http://purl.obolibrary.org/obo/TGEMO_00003", null, TermViolation.Reason.URI_UNRESOLVED ) )
                    : Collections.emptyList();
        } );

        String body = "{\"design\":{\"factors\":{\"items\":[{"
                + "\"clientRef\":\"F1\",\"name\":\"genotype\",\"category\":{\"label\":\"genotype\"},"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"FV1\",\"statements\":{\"items\":[{"
                + "\"clientRef\":\"S1\",\"subject\":{\"label\":\"Utrn\",\"uri\":\"http://x/subj\"},"
                + "\"object\":{\"label\":\"Heterozygous\",\"uri\":\"http://purl.obolibrary.org/obo/TGEMO_00003\"}"
                + "}]}}]}}]}}}";
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.error.errors[0].reason", "URI_UNRESOLVED" )
                    .hasPathWithValue( "$.error.errors[0].location", "design.factors[clientRef=F1].factorValues[clientRef=FV1].statements[clientRef=S1].object" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /** Preflight enforces the same gate, so a client catches the failure on the dry run. */
    @Test
    public void testPreflightRejectsUngroundedTerm() {
        when( ontologyTermValidator.validateAndCanonicalize( any() ) ).thenReturn( Collections.singletonList(
                new TermViolation( "value", "has_genotype", "http://purl.obolibrary.org/obo/TGEMO_00166", "delivered at dose", TermViolation.Reason.LABEL_MISMATCH ) ) );
        try ( Response r = target( "/datasets/1/curation/preflight" ).request().post( Entity.json( HALLUCINATED_TAG_BODY ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.error.errors[0].reason", "LABEL_MISMATCH" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
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
        verify( expressionExperimentService ).getEnhancedFilters( Filters.empty(), null, Collections.emptySet(), 30, TimeUnit.SECONDS );
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
        verify( expressionExperimentService ).getEnhancedFilters( Filters.empty(), null, Collections.emptySet(), 30, TimeUnit.SECONDS );
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
        when( expressionExperimentService.getEnhancedFilters( any(), any(), any(), anyLong(), any() ) )
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
        verify( expressionExperimentService ).getEnhancedFilters( Filters.by( f ), null, new HashSet<>(), 30, TimeUnit.SECONDS );
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
        verify( expressionExperimentService ).getEnhancedFilters( Filters.empty(), Collections.emptySet(), new HashSet<>(), 30000, TimeUnit.MILLISECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), isNull(), isNull(), isNull(), eq( 0 ), eq( Collections.emptySet() ), eq( 100 ), eq( false ), eq( false ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
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
        verify( expressionExperimentService ).getEnhancedFilters( Filters.empty(), null, new HashSet<>(), 30000, TimeUnit.MILLISECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), isNull(), isNull(), isNull(), eq( 0 ), isNull(), eq( 100 ), eq( false ), eq( false ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
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
        verify( expressionExperimentService ).getEnhancedFilters( Filters.empty(), null, new HashSet<>(), 30000, TimeUnit.MILLISECONDS );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), isNull(), isNull(), isNull(), eq( 10 ), isNull(), eq( 5000 ), eq( false ), eq( false ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
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
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), isNull(), isNull(), isNull(), eq( 0 ), isNull(), eq( 50 ), eq( false ), eq( false ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testGetDatasetsAnnotationsForUncategorizedTerms() throws TimeoutException {
        assertThat( target( "/datasets/annotations" ).queryParam( "category", "" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).getAnnotationsUsageFrequency( eq( Filters.empty() ), isNull(), eq( ExpressionExperimentService.UNCATEGORIZED ), isNull(), isNull(), eq( 0 ), isNull(), eq( 100 ), eq( false ), eq( false ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
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
        // New async-build flow: endpoint probes the cache via getDataFile(filename, false, 5, SECONDS),
        // sendfile-s the path when it exists, otherwise streams in-band while the cache is being built.
        ee.setShortName( "GSE1" );
        when( expressionExperimentService.hasProcessedExpressionData( eq( ee ) ) ).thenReturn( true );
        when( expressionDataFileService.getDataFile( anyString(), eq( false ), eq( 5L ), eq( TimeUnit.SECONDS ) ) )
                .thenReturn( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.txt.gz" ) ).toURI() ), true ) );
        assertThat( target( "/datasets/1/data/processed" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .hasHeaderWithValue( "Content-Disposition", "attachment; filename=\"data.txt\"" )
                .hasEncoding( "gzip" );
        verify( expressionExperimentService ).hasProcessedExpressionData( ee );
        verify( expressionDataFileService ).getDataFile( anyString(), eq( false ), eq( 5L ), eq( TimeUnit.SECONDS ) );
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
        ee.setShortName( "GSE1" );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "raw" );
        when( expressionExperimentService.getPreferredQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        // New async-build flow: cache probe is via getDataFile(filename, false, 5, SECONDS).
        when( expressionDataFileService.getDataFile( anyString(), eq( false ), eq( 5L ), eq( TimeUnit.SECONDS ) ) )
                .thenReturn( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.txt.gz" ) ).toURI() ), true ) );
        assertThat( target( "/datasets/1/data/raw" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .hasHeaderWithValue( "Content-Disposition", "attachment; filename=\"data.txt\"" )
                .hasEncoding( "gzip" );
        verify( expressionExperimentService ).getPreferredQuantitationType( ee );
        verifyNoInteractions( quantitationTypeService );
        verify( expressionDataFileService ).getDataFile( anyString(), eq( false ), eq( 5L ), eq( TimeUnit.SECONDS ) );
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
        ee.setShortName( "GSE1" );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setId( 12L );
        qt.setName( "raw" );
        when( quantitationTypeService.load( 12L ) ).thenReturn( qt );
        when( quantitationTypeService.loadByIdAndVectorType( 12L, ee, RawExpressionDataVector.class ) ).thenReturn( qt );

        when( expressionDataFileService.getDataFile( anyString(), eq( false ), eq( 5L ), eq( TimeUnit.SECONDS ) ) )
                .thenReturn( new DummyLockedPath( Paths.get( requireNonNull( getClass().getResource( "/data.txt.gz" ) ).toURI() ), true ) );
        Response res = target( "/datasets/1/data/raw" )
                .queryParam( "quantitationType", "12" ).request().get();
        verify( quantitationTypeService ).loadByIdAndVectorType( 12L, ee, RawExpressionDataVector.class );
        verify( expressionDataFileService ).getDataFile( anyString(), eq( false ), eq( 5L ), eq( TimeUnit.SECONDS ) );
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
    @WithMockUser
    public void testUpdateDatasetAnnotations() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        String body = "{\"annotations\":[{\"category\":\"organism part\",\"categoryUri\":\"http://purl.obolibrary.org/obo/UBERON_0000479\","
                + "\"value\":\"liver\",\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0002107\"}]}";
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        ArgumentCaptor<Collection<ubic.gemma.model.common.description.Characteristic>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updateAnnotations( eq( ee ), captor.capture() );
        Collection<ubic.gemma.model.common.description.Characteristic> sent = captor.getValue();
        assertThat( sent ).hasSize( 1 );
        ubic.gemma.model.common.description.Characteristic c = sent.iterator().next();
        assertThat( c.getCategory() ).isEqualTo( "organism part" );
        assertThat( c.getValue() ).isEqualTo( "liver" );
        assertThat( c.getValueUri() ).isEqualTo( "http://purl.obolibrary.org/obo/UBERON_0002107" );
        verify( expressionExperimentService ).getAnnotations( ee );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsAcceptsEmptyListAsClear() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( "{\"annotations\":[]}" ) ) )
                .hasStatus( Response.Status.OK );
        verify( expressionExperimentService ).updateAnnotations( eq( ee ), argThat( Collection::isEmpty ) );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsMissingBody() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        // empty/missing JSON body -- body is null on the handler side
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( "{}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updateAnnotations( any(), any() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsRejectsBlankCategory() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"annotations\":[{\"category\":\"\",\"value\":\"liver\"}]}";
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updateAnnotations( any(), any() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsRejectsBlankValue() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"annotations\":[{\"category\":\"organism part\",\"value\":\"  \"}]}";
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updateAnnotations( any(), any() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsConstructsStatementWhenPredicateAndObjectPresent() {
        // Statement-shaped tag write: value/valueUri are the subject; predicate / object pair makes
        // the row a Statement (not a plain Characteristic). The captured collection must contain a
        // Statement instance with subject + predicate + object populated, and the URIs must round
        // through as-is (the wire shape mirrors AnnotationValueObject's read-side fields).
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        String body = "{\"annotations\":[{"
                + "\"category\":\"genotype\","
                + "\"categoryUri\":\"http://www.ebi.ac.uk/efo/EFO_0000513\","
                + "\"value\":\"Abca4\","
                + "\"valueUri\":\"http://purl.org/commons/record/ncbi_gene/11304\","
                + "\"predicate\":\"has_genotype\","
                + "\"predicateUri\":\"http://gemma.msl.ubc.ca/ont/TGEMO_00166\","
                + "\"object\":\"Homozygous negative\","
                + "\"objectUri\":\"http://purl.obolibrary.org/obo/TGEMO_00001\""
                + "}]}";
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<Collection<ubic.gemma.model.common.description.Characteristic>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updateAnnotations( eq( ee ), captor.capture() );
        Collection<ubic.gemma.model.common.description.Characteristic> sent = captor.getValue();
        assertThat( sent ).hasSize( 1 );
        ubic.gemma.model.common.description.Characteristic c = sent.iterator().next();
        assertThat( c ).isInstanceOf( Statement.class );
        Statement s = ( Statement ) c;
        assertThat( s.getCategory() ).isEqualTo( "genotype" );
        assertThat( s.getCategoryUri() ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000513" );
        assertThat( s.getSubject() ).isEqualTo( "Abca4" );
        assertThat( s.getSubjectUri() ).isEqualTo( "http://purl.org/commons/record/ncbi_gene/11304" );
        assertThat( s.getPredicate() ).isEqualTo( "has_genotype" );
        assertThat( s.getPredicateUri() ).isEqualTo( "http://gemma.msl.ubc.ca/ont/TGEMO_00166" );
        assertThat( s.getObject() ).isEqualTo( "Homozygous negative" );
        assertThat( s.getObjectUri() ).isEqualTo( "http://purl.obolibrary.org/obo/TGEMO_00001" );
        assertThat( s.getSecondPredicate() ).isNull();
        assertThat( s.getSecondObject() ).isNull();
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsConstructsStatementWithSecondPair() {
        // Compound Statement: secondPredicate + secondObject (e.g. "HFD for 12 weeks" — predicate
        // "delivered_at_dose" with object "30%", second predicate "for" with second object
        // "12 weeks"). Verifies the second pair propagates through the wire and ends up on the
        // Statement instance.
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        String body = "{\"annotations\":[{"
                + "\"category\":\"treatment\",\"categoryUri\":\"http://www.ebi.ac.uk/efo/EFO_0000727\","
                + "\"value\":\"high fat diet\",\"valueUri\":null,"
                + "\"predicate\":\"delivered_at_dose\",\"object\":\"30%\","
                + "\"secondPredicate\":\"for\",\"secondObject\":\"12 weeks\""
                + "}]}";
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<Collection<ubic.gemma.model.common.description.Characteristic>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updateAnnotations( eq( ee ), captor.capture() );
        ubic.gemma.model.common.description.Characteristic c = captor.getValue().iterator().next();
        assertThat( c ).isInstanceOf( Statement.class );
        Statement s = ( Statement ) c;
        assertThat( s.getPredicate() ).isEqualTo( "delivered_at_dose" );
        assertThat( s.getObject() ).isEqualTo( "30%" );
        assertThat( s.getSecondPredicate() ).isEqualTo( "for" );
        assertThat( s.getSecondObject() ).isEqualTo( "12 weeks" );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsStaysPlainCharacteristicWhenNoStatementFields() {
        // Regression: a plain-shape tag (no predicate / object / second pair) MUST stay a plain
        // Characteristic and NOT be promoted to a Statement. Asserts the existing wire shape
        // round-trips unchanged after the statement-aware widening.
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        String body = "{\"annotations\":[{\"category\":\"organism part\",\"value\":\"liver\","
                + "\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0002107\"}]}";
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<Collection<ubic.gemma.model.common.description.Characteristic>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updateAnnotations( eq( ee ), captor.capture() );
        ubic.gemma.model.common.description.Characteristic c = captor.getValue().iterator().next();
        assertThat( c ).isNotInstanceOf( Statement.class );
        assertThat( c.getValue() ).isEqualTo( "liver" );
        assertThat( c.getValueUri() ).isEqualTo( "http://purl.obolibrary.org/obo/UBERON_0002107" );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsCarriesSupportingEvidence() {
        // A curated tag arriving with a supporting_evidence array (the agents-side FindingEvidence
        // shape) must round-trip onto the persisted Characteristic as opaque JSON — Gemma stores it
        // verbatim, it is not parsed or restructured.
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        String body = "{\"annotations\":[{\"category\":\"strain\",\"value\":\"C57BL/6J\","
                + "\"supportingEvidence\":[{\"quote\":\"strain: C57BL/6J\",\"source\":\"characteristic\","
                + "\"location\":\"strain (all 24 samples)\"}]}]}";
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<Collection<ubic.gemma.model.common.description.Characteristic>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updateAnnotations( eq( ee ), captor.capture() );
        ubic.gemma.model.common.description.Characteristic c = captor.getValue().iterator().next();
        assertThat( c.getSupportingEvidence() )
                .contains( "\"quote\":\"strain: C57BL/6J\"" )
                .contains( "\"source\":\"characteristic\"" )
                .contains( "\"location\":\"strain (all 24 samples)\"" );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsNoEvidenceLeavesSupportingEvidenceNull() {
        // A tag with no supporting_evidence must NOT stamp an empty/blank value — it stays null so a
        // set-replace update doesn't clobber evidence already on a matched tag.
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        String body = "{\"annotations\":[{\"category\":\"organism part\",\"value\":\"liver\"}]}";
        assertThat( target( "/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<Collection<ubic.gemma.model.common.description.Characteristic>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updateAnnotations( eq( ee ), captor.capture() );
        assertThat( captor.getValue().iterator().next().getSupportingEvidence() ).isNull();
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
        verify( differentialExpressionResultService ).findByGeneAndExperimentAnalyzedIds( eq( brca1 ), eq( true ), eq( false ), any(), eq( true ), any(), any(), any(), anyDouble(), eq( true ) );
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
        verify( differentialExpressionResultService ).findByGeneAndExperimentAnalyzedIds( eq( brca1 ), eq( true ), eq( false ), any(), eq( true ), any(), any(), any(), anyDouble(), eq( true ) );
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
    @WithMockUser
    public void testRefreshDatasetWithDefaultsDoesNotEvictCaches() {
        ee.setId( 1L );
        when( expressionExperimentService.loadAndThawLiteWithRefreshCacheMode( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadValueObject( ee ) ).thenReturn( new ExpressionExperimentValueObject( ee ) );
        assertThat( target( "/datasets/1/refresh" ).request().get() )
                .hasStatus( Response.Status.CREATED );
        verify( expressionExperimentService ).loadAndThawLiteWithRefreshCacheMode( 1L );
        verify( expressionExperimentService ).loadValueObject( ee );
        verify( processedExpressionDataVectorService, never() ).evictFromCache( any() );
        verify( expressionExperimentReportService, never() ).evictFromCache( anyLong() );
    }

    @Test
    @WithMockUser
    public void testRefreshDatasetVectorsOnly() {
        ee.setId( 1L );
        when( expressionExperimentService.loadAndThawLiteWithRefreshCacheMode( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadValueObject( ee ) ).thenReturn( new ExpressionExperimentValueObject( ee ) );
        assertThat( target( "/datasets/1/refresh" )
                .queryParam( "refreshVectors", true )
                .request().get() )
                .hasStatus( Response.Status.CREATED );
        verify( processedExpressionDataVectorService ).evictFromCache( ee );
        verify( expressionExperimentReportService, never() ).evictFromCache( anyLong() );
    }

    @Test
    @WithMockUser
    public void testRefreshDatasetReportsOnly() {
        ee.setId( 1L );
        when( expressionExperimentService.loadAndThawLiteWithRefreshCacheMode( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadValueObject( ee ) ).thenReturn( new ExpressionExperimentValueObject( ee ) );
        assertThat( target( "/datasets/1/refresh" )
                .queryParam( "refreshReports", true )
                .request().get() )
                .hasStatus( Response.Status.CREATED );
        verify( expressionExperimentReportService ).evictFromCache( 1L );
        verify( processedExpressionDataVectorService, never() ).evictFromCache( any() );
    }

    @Test
    @WithMockUser
    public void testRefreshDatasetNotFound() {
        when( expressionExperimentService.loadAndThawLiteWithRefreshCacheMode( 1L ) ).thenReturn( null );
        assertThat( target( "/datasets/1/refresh" )
                .queryParam( "refreshVectors", true )
                .queryParam( "refreshReports", true )
                .request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
        verify( expressionExperimentService ).loadAndThawLiteWithRefreshCacheMode( 1L );
        verify( processedExpressionDataVectorService, never() ).evictFromCache( any() );
        verify( expressionExperimentReportService, never() ).evictFromCache( anyLong() );
        verify( expressionExperimentService, never() ).loadValueObject( any() );
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
    public void testPreviewDatasetDesignChangeNoOp() {
        DesignPreflightReport report = new DesignPreflightReport();
        report.getSummary().setBiomaterialsWithChangedAssignments( 0 );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) ).thenReturn( report );

        ExperimentalDesignValueObject payload = new ExperimentalDesignValueObject();
        assertThat( target( "/datasets/1/designPreflight" ).request().post( jakarta.ws.rs.client.Entity.json( payload ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" )
                .hasFieldOrPropertyWithValue( "data.summary.factorsToDelete", 0 )
                .hasFieldOrPropertyWithValue( "data.summary.factorValuesToDelete", 0 )
                .hasFieldOrPropertyWithValue( "data.summary.differentialExpressionAnalysesToDelete", 0 )
                .hasFieldOrPropertyWithValue( "data.summary.biomaterialsWithChangedAssignments", 0 );

        ArgumentCaptor<ExperimentalDesignValueObject> captor = ArgumentCaptor.forClass( ExperimentalDesignValueObject.class );
        verify( expressionExperimentService ).previewDesignChange( eq( ee ), captor.capture() );
        verify( expressionExperimentService ).load( 1L );
    }

    @Test
    public void testPreviewDatasetDesignChangeWithBlockers() {
        DesignPreflightReport report = new DesignPreflightReport();
        DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker(
                "ASSIGNMENT_REFERENCES_UNKNOWN_FV",
                "Biomaterial 42 is assigned to factor value 999 which is not present in the proposed design." );
        b.setBioMaterialId( 42L );
        b.setFactorValueId( 999L );
        report.getBlockers().add( b );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) ).thenReturn( report );

        assertThat( target( "/datasets/1/designPreflight" ).request().post( jakarta.ws.rs.client.Entity.json( new ExperimentalDesignValueObject() ) ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.blockers", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "type", "ASSIGNMENT_REFERENCES_UNKNOWN_FV" )
                .hasFieldOrPropertyWithValue( "bioMaterialId", 42 )
                .hasFieldOrPropertyWithValue( "factorValueId", 999 );
    }

    @Test
    public void testPreviewDatasetDesignChangeReportsDeletions() {
        DesignPreflightReport report = new DesignPreflightReport();
        report.getFactorsToDelete().add( new DesignPreflightReport.EntityRef( 7L, "treatment" ) );
        report.getFactorValuesToDelete().add( new DesignPreflightReport.EntityRef( 70L, "control" ) );
        report.getDifferentialExpressionAnalysesToDelete().add( new DesignPreflightReport.AnalysisRef( 500L, "control vs treatment", null ) );
        report.getSummary().setFactorsToDelete( 1 );
        report.getSummary().setFactorValuesToDelete( 1 );
        report.getSummary().setDifferentialExpressionAnalysesToDelete( 1 );
        report.getSummary().setBiomaterialsWithChangedAssignments( 3 );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) ).thenReturn( report );

        assertThat( target( "/datasets/1/designPreflight" ).request().post( jakarta.ws.rs.client.Entity.json( new ExperimentalDesignValueObject() ) ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.summary.factorsToDelete", 1 )
                .hasFieldOrPropertyWithValue( "data.summary.factorValuesToDelete", 1 )
                .hasFieldOrPropertyWithValue( "data.summary.differentialExpressionAnalysesToDelete", 1 )
                .hasFieldOrPropertyWithValue( "data.summary.biomaterialsWithChangedAssignments", 3 )
                .extracting( "data.differentialExpressionAnalysesToDelete", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", 500 )
                .hasFieldOrPropertyWithValue( "name", "control vs treatment" );
    }

    @Test
    public void testPreviewDatasetDesignChangeWithEmptyBodyIs400() {
        assertThat( target( "/datasets/1/designPreflight" ).request().post( jakarta.ws.rs.client.Entity.json( null ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).previewDesignChange( any(), any() );
    }

    @Test
    public void testPreviewDatasetDesignChangeWithUnknownDatasetIs404() {
        // expressionExperimentService.load( 1L ) returns ee per setUpMocks(); load for an unknown id returns null.
        assertThat( target( "/datasets/999/designPreflight" ).request().post( jakarta.ws.rs.client.Entity.json( new ExperimentalDesignValueObject() ) ) )
                .hasStatus( Response.Status.NOT_FOUND );
        verify( expressionExperimentService, never() ).previewDesignChange( any(), any() );
    }

    @Test
    public void testGetDatasetAuditEvents() {
        Date when = new Date( 1_700_000_000_000L );
        AuditEvent created = AuditEvent.Factory.newInstance( when, AuditAction.CREATE, "created", "detail-c", null, null );
        AuditEvent updated = AuditEvent.Factory.newInstance( new Date( when.getTime() + 1000L ), AuditAction.UPDATE, "updated", "detail-u", null, null );
        when( auditEventService.getEvents( ee ) ).thenReturn( Arrays.asList( created, updated ) );

        assertThat( target( "/datasets/1/auditEvents" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfiesExactly(
                        a -> assertThat( a )
                                .containsEntry( "action", "C" )
                                .containsEntry( "note", "created" )
                                .containsEntry( "detail", "detail-c" ),
                        a -> assertThat( a )
                                .containsEntry( "action", "U" )
                                .containsEntry( "note", "updated" )
                                .containsEntry( "detail", "detail-u" ) );

        verify( expressionExperimentService ).load( 1L );
        verify( auditEventService ).getEvents( ee );
    }

    @Test
    public void testGetDatasetAuditEventsWhenEmpty() {
        when( auditEventService.getEvents( ee ) ).thenReturn( Collections.emptyList() );

        assertThat( target( "/datasets/1/auditEvents" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .extracting( "data", list( Map.class ) )
                .isEmpty();

        verify( auditEventService ).getEvents( ee );
    }

    @Test
    public void testGetDatasetAuditEventsWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/999/auditEvents" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
        verify( auditEventService, never() ).getEvents( any() );
    }

    @Test
    public void testGetDatasetAuditEventsCompactFalseReturnsFullList() {
        Date when = new Date( 1_700_000_000_000L );
        AuditEvent a = AuditEvent.Factory.newInstance( when, AuditAction.UPDATE, "n1", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        AuditEvent b = AuditEvent.Factory.newInstance( new Date( when.getTime() + 1000L ), AuditAction.UPDATE, "n2", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        when( auditEventService.getEvents( ee ) ).thenReturn( Arrays.asList( a, b ) );

        // compact=false explicitly — should NOT carry collapsedCount
        assertThat( target( "/datasets/1/auditEvents" ).queryParam( "compact", "false" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfiesExactly(
                        e -> assertThat( e ).doesNotContainKey( "collapsedCount" ),
                        e -> assertThat( e ).doesNotContainKey( "collapsedCount" ) );

        verify( auditEventService ).getEvents( ee );
    }

    @Test
    public void testGetDatasetAuditEventsCompactCollapsesConsecutiveSameTypeAndPerformer() {
        Date d1 = new Date( 1_700_000_000_000L );
        Date d2 = new Date( 1_700_000_001_000L );
        Date d3 = new Date( 1_700_000_002_000L );
        ubic.gemma.model.common.auditAndSecurity.User performer = ubic.gemma.model.common.auditAndSecurity.User.Factory.newInstance( "alice" );
        AuditEvent e1 = AuditEvent.Factory.newInstance( d1, AuditAction.UPDATE, "first", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        AuditEvent e2 = AuditEvent.Factory.newInstance( d2, AuditAction.UPDATE, "second", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        AuditEvent e3 = AuditEvent.Factory.newInstance( d3, AuditAction.UPDATE, "third", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        when( auditEventService.getEvents( ee ) ).thenReturn( Arrays.asList( e1, e2, e3 ) );

        assertThat( target( "/datasets/1/auditEvents" ).queryParam( "compact", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .satisfiesExactly(
                        entry -> {
                            assertThat( entry )
                                    .containsEntry( "collapsedCount", 3 )
                                    .containsEntry( "note", "first" )
                                    .containsKey( "lastOccurrence" );
                            // lastOccurrence carries the LAST event's date; serialized as ISO string
                            // -- assert it's distinct from the head event's date (= d1).
                            assertThat( entry.get( "lastOccurrence" ) ).isNotEqualTo( entry.get( "date" ) );
                        } );
    }

    @Test
    public void testGetDatasetAuditEventsCompactAlternatingTypesProducesAllSolo() {
        Date base = new Date( 1_700_000_000_000L );
        ubic.gemma.model.common.auditAndSecurity.User performer = ubic.gemma.model.common.auditAndSecurity.User.Factory.newInstance( "alice" );
        AuditEvent a1 = AuditEvent.Factory.newInstance( new Date( base.getTime() ), AuditAction.UPDATE, "a1", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        AuditEvent b1 = AuditEvent.Factory.newInstance( new Date( base.getTime() + 1000L ), AuditAction.UPDATE, "b1", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.DatasetPublishedEvent() );
        AuditEvent a2 = AuditEvent.Factory.newInstance( new Date( base.getTime() + 2000L ), AuditAction.UPDATE, "a2", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        AuditEvent b2 = AuditEvent.Factory.newInstance( new Date( base.getTime() + 3000L ), AuditAction.UPDATE, "b2", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.DatasetPublishedEvent() );
        when( auditEventService.getEvents( ee ) ).thenReturn( Arrays.asList( a1, b1, a2, b2 ) );

        assertThat( target( "/datasets/1/auditEvents" ).queryParam( "compact", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 4 )
                .satisfiesExactly(
                        e -> assertThat( e ).containsEntry( "collapsedCount", 1 ).containsEntry( "note", "a1" ),
                        e -> assertThat( e ).containsEntry( "collapsedCount", 1 ).containsEntry( "note", "b1" ),
                        e -> assertThat( e ).containsEntry( "collapsedCount", 1 ).containsEntry( "note", "a2" ),
                        e -> assertThat( e ).containsEntry( "collapsedCount", 1 ).containsEntry( "note", "b2" ) );
    }

    @Test
    public void testGetDatasetAuditEventsCompactMixedAABA() {
        Date base = new Date( 1_700_000_000_000L );
        ubic.gemma.model.common.auditAndSecurity.User performer = ubic.gemma.model.common.auditAndSecurity.User.Factory.newInstance( "alice" );
        AuditEvent a1 = AuditEvent.Factory.newInstance( new Date( base.getTime() ), AuditAction.UPDATE, "a1", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        Date a2Date = new Date( base.getTime() + 1000L );
        AuditEvent a2 = AuditEvent.Factory.newInstance( a2Date, AuditAction.UPDATE, "a2", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        AuditEvent b1 = AuditEvent.Factory.newInstance( new Date( base.getTime() + 2000L ), AuditAction.UPDATE, "b1", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.DatasetPublishedEvent() );
        AuditEvent a3 = AuditEvent.Factory.newInstance( new Date( base.getTime() + 3000L ), AuditAction.UPDATE, "a3", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        when( auditEventService.getEvents( ee ) ).thenReturn( Arrays.asList( a1, a2, b1, a3 ) );

        assertThat( target( "/datasets/1/auditEvents" ).queryParam( "compact", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 3 )
                .satisfiesExactly(
                        e -> {
                            assertThat( e )
                                    .containsEntry( "collapsedCount", 2 )
                                    .containsEntry( "note", "a1" )
                                    .containsKey( "lastOccurrence" );
                            // lastOccurrence == LAST event's date, distinct from head's date
                            assertThat( e.get( "lastOccurrence" ) ).isNotEqualTo( e.get( "date" ) );
                        },
                        e -> {
                            assertThat( e ).containsEntry( "collapsedCount", 1 ).containsEntry( "note", "b1" );
                            // solo event: lastOccurrence == date
                            assertThat( e.get( "lastOccurrence" ) ).isEqualTo( e.get( "date" ) );
                        },
                        e -> {
                            assertThat( e ).containsEntry( "collapsedCount", 1 ).containsEntry( "note", "a3" );
                            assertThat( e.get( "lastOccurrence" ) ).isEqualTo( e.get( "date" ) );
                        } );
    }

    @Test
    public void testGetDatasetAuditEventsCompactOnEmptyIsEmpty() {
        when( auditEventService.getEvents( ee ) ).thenReturn( Collections.emptyList() );

        assertThat( target( "/datasets/1/auditEvents" ).queryParam( "compact", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .isEmpty();

        verify( auditEventService ).getEvents( ee );
    }

    @Test
    public void testGetDatasetAuditEventsExcludeEmptyDropsTypelessBlankEvents() {
        Date base = new Date( 1_700_000_000_000L );
        ubic.gemma.model.common.auditAndSecurity.User performer =
                ubic.gemma.model.common.auditAndSecurity.User.Factory.newInstance( "alice" );
        // Boring: no eventType, blank note + detail
        AuditEvent boring = AuditEvent.Factory.newInstance( base, AuditAction.UPDATE, null, null, performer, null );
        // Meaningful: typed event
        AuditEvent typed = AuditEvent.Factory.newInstance( new Date( base.getTime() + 1000L ),
                AuditAction.UPDATE, null, null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        // Meaningful: untyped but with a note
        AuditEvent noted = AuditEvent.Factory.newInstance( new Date( base.getTime() + 2000L ),
                AuditAction.UPDATE, "saw something", null, performer, null );
        when( auditEventService.getEvents( ee ) ).thenReturn( Arrays.asList( boring, typed, noted ) );

        assertThat( target( "/datasets/1/auditEvents" ).queryParam( "excludeEmpty", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 );
        verify( auditEventService ).getEvents( ee );
    }

    @Test
    public void testGetDatasetAuditEventsExcludeEmptyComposesWithCompact() {
        // excludeEmpty filters first; compact then collapses over the survivors.
        // Input: BORING, TYPED-A, TYPED-A, TYPED-B
        // After excludeEmpty: TYPED-A, TYPED-A, TYPED-B
        // After compact:      [A run of 2], [B solo]
        Date base = new Date( 1_700_000_000_000L );
        ubic.gemma.model.common.auditAndSecurity.User performer =
                ubic.gemma.model.common.auditAndSecurity.User.Factory.newInstance( "alice" );
        AuditEvent boring = AuditEvent.Factory.newInstance( base, AuditAction.UPDATE, null, null, performer, null );
        AuditEvent a1 = AuditEvent.Factory.newInstance( new Date( base.getTime() + 1000L ),
                AuditAction.UPDATE, "a1", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        AuditEvent a2 = AuditEvent.Factory.newInstance( new Date( base.getTime() + 2000L ),
                AuditAction.UPDATE, "a2", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent() );
        AuditEvent b1 = AuditEvent.Factory.newInstance( new Date( base.getTime() + 3000L ),
                AuditAction.UPDATE, "b1", null, performer,
                new ubic.gemma.model.common.auditAndSecurity.eventType.DatasetPublishedEvent() );
        when( auditEventService.getEvents( ee ) ).thenReturn( Arrays.asList( boring, a1, a2, b1 ) );

        assertThat( target( "/datasets/1/auditEvents" )
                .queryParam( "excludeEmpty", "true" )
                .queryParam( "compact", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfiesExactly(
                        e -> assertThat( e ).containsEntry( "collapsedCount", 2 ).containsEntry( "note", "a1" ),
                        e -> assertThat( e ).containsEntry( "collapsedCount", 1 ).containsEntry( "note", "b1" ) );
    }

    @Test
    @WithMockUser
    public void testGetDatasetCurationDetails() {
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        cd.setId( 7L );
        cd.setTroubled( true );
        cd.setNeedsAttention( false );
        cd.setCurationNote( "look here" );
        ee.setCurationDetails( cd );

        assertThat( target( "/datasets/1/curationDetails" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "data.troubled", true )
                .hasFieldOrPropertyWithValue( "data.needsAttention", false );

        verify( expressionExperimentService ).load( 1L );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetCurationDetailsExposesCurationNoteForAdmin() {
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        cd.setCurationNote( "admin only" );
        ee.setCurationDetails( cd );

        assertThat( target( "/datasets/1/curationDetails" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.curationNote", "admin only" );
    }

    @Test
    @WithMockUser
    public void testGetDatasetCurationDetailsWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/999/curationDetails" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsSetsTroubled() {
        ee.setId( 1L );
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        cd.setTroubled( false );
        ee.setCurationDetails( cd );
        when( ticketService.findOpenForTarget(
                ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) )
                .thenReturn( Collections.emptyList() );

        DatasetsWebService.CurationDetailsUpdateRequest body = new DatasetsWebService.CurationDetailsUpdateRequest();
        body.setTroubled( true );
        body.setNote( "data quality issue" );

        assertThat( target( "/datasets/1/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( ticketService ).openTicket( any( ubic.gemma.model.common.auditAndSecurity.User.class ),
                eq( ubic.gemma.model.common.auditAndSecurity.curation.TicketType.QUALITY_REVIEW ),
                eq( "data quality issue" ),
                anyCollection() );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsClearsTroubled() {
        ee.setId( 1L );
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        cd.setTroubled( true );
        ee.setCurationDetails( cd );
        ubic.gemma.model.common.auditAndSecurity.curation.Ticket open = mock( ubic.gemma.model.common.auditAndSecurity.curation.Ticket.class );
        when( open.getType() ).thenReturn( ubic.gemma.model.common.auditAndSecurity.curation.TicketType.QUALITY_REVIEW );
        when( open.getState() ).thenReturn( ubic.gemma.model.common.auditAndSecurity.curation.TicketState.OPEN );
        when( ticketService.findOpenForTarget(
                ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) )
                .thenReturn( Collections.singletonList( open ) );

        DatasetsWebService.CurationDetailsUpdateRequest body = new DatasetsWebService.CurationDetailsUpdateRequest();
        body.setTroubled( false );

        assertThat( target( "/datasets/1/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( ticketService ).transition( eq( open ),
                eq( ubic.gemma.model.common.auditAndSecurity.curation.TicketState.RESOLVED ),
                any( ubic.gemma.model.common.auditAndSecurity.User.class ),
                isNull() );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsSkipsNoOpTroubled() {
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        cd.setTroubled( true );
        ee.setCurationDetails( cd );

        DatasetsWebService.CurationDetailsUpdateRequest body = new DatasetsWebService.CurationDetailsUpdateRequest();
        body.setTroubled( true );

        assertThat( target( "/datasets/1/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( auditTrailService, never() ).addUpdateEvent( any(), any( Class.class ), any() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsSetsNeedsAttention() {
        ee.setId( 1L );
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        cd.setNeedsAttention( false );
        ee.setCurationDetails( cd );
        when( ticketService.findOpenForTarget(
                ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) )
                .thenReturn( Collections.emptyList() );

        DatasetsWebService.CurationDetailsUpdateRequest body = new DatasetsWebService.CurationDetailsUpdateRequest();
        body.setNeedsAttention( true );
        body.setNote( "needs review" );

        assertThat( target( "/datasets/1/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( ticketService ).openTicket( any( ubic.gemma.model.common.auditAndSecurity.User.class ),
                eq( ubic.gemma.model.common.auditAndSecurity.curation.TicketType.GENERIC ),
                eq( "needs review" ),
                anyCollection() );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsClearsNeedsAttention() {
        ee.setId( 1L );
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        cd.setNeedsAttention( true );
        ee.setCurationDetails( cd );
        ubic.gemma.model.common.auditAndSecurity.curation.Ticket open = mock( ubic.gemma.model.common.auditAndSecurity.curation.Ticket.class );
        when( open.getType() ).thenReturn( ubic.gemma.model.common.auditAndSecurity.curation.TicketType.GENERIC );
        when( open.getState() ).thenReturn( ubic.gemma.model.common.auditAndSecurity.curation.TicketState.OPEN );
        when( ticketService.findOpenForTarget(
                ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) )
                .thenReturn( Collections.singletonList( open ) );

        DatasetsWebService.CurationDetailsUpdateRequest body = new DatasetsWebService.CurationDetailsUpdateRequest();
        body.setNeedsAttention( false );

        assertThat( target( "/datasets/1/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( ticketService ).transition( eq( open ),
                eq( ubic.gemma.model.common.auditAndSecurity.curation.TicketState.RESOLVED ),
                any( ubic.gemma.model.common.auditAndSecurity.User.class ),
                isNull() );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsUpdatesCurationNote() {
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        ee.setCurationDetails( cd );

        DatasetsWebService.CurationDetailsUpdateRequest body = new DatasetsWebService.CurationDetailsUpdateRequest();
        body.setCurationNote( "updated note" );

        assertThat( target( "/datasets/1/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( auditTrailService ).addUpdateEvent( ee,
                ubic.gemma.model.common.auditAndSecurity.eventType.CurationNoteUpdateEvent.class,
                "updated note" );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsAppliesMultipleChanges() {
        ee.setId( 1L );
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        cd.setTroubled( false );
        cd.setNeedsAttention( false );
        ee.setCurationDetails( cd );
        when( ticketService.findOpenForTarget(
                ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) )
                .thenReturn( Collections.emptyList() );

        DatasetsWebService.CurationDetailsUpdateRequest body = new DatasetsWebService.CurationDetailsUpdateRequest();
        body.setTroubled( true );
        body.setNeedsAttention( true );
        body.setCurationNote( "flagged" );
        body.setNote( "bad batch" );

        assertThat( target( "/datasets/1/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        // troubled=true -> QUALITY_REVIEW ticket, needsAttention=true -> GENERIC ticket
        verify( ticketService ).openTicket( any( ubic.gemma.model.common.auditAndSecurity.User.class ),
                eq( ubic.gemma.model.common.auditAndSecurity.curation.TicketType.QUALITY_REVIEW ),
                eq( "bad batch" ),
                anyCollection() );
        verify( ticketService ).openTicket( any( ubic.gemma.model.common.auditAndSecurity.User.class ),
                eq( ubic.gemma.model.common.auditAndSecurity.curation.TicketType.GENERIC ),
                eq( "bad batch" ),
                anyCollection() );
        // curationNote still flows through legacy auditTrailService event path
        verify( auditTrailService ).addUpdateEvent( ee,
                ubic.gemma.model.common.auditAndSecurity.eventType.CurationNoteUpdateEvent.class, "flagged" );
        verifyNoMoreInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsWithEmptyBodyIs400() {
        // PUT requires a non-null entity at the Jersey client layer; send the JSON literal "null" so the
        // resource method receives a null body and triggers the 400 path.
        assertThat( target( "/datasets/1/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( "null" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetCurationDetailsWithUnknownDatasetIs404() {
        DatasetsWebService.CurationDetailsUpdateRequest body = new DatasetsWebService.CurationDetailsUpdateRequest();
        body.setTroubled( true );
        assertThat( target( "/datasets/999/curationDetails" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.NOT_FOUND );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetPermissionsMakesPublic() {
        when( securityService.isPublic( ee ) ).thenReturn( true );
        when( securityService.isShared( ee ) ).thenReturn( false );

        DatasetsWebService.PermissionsUpdateRequest body = new DatasetsWebService.PermissionsUpdateRequest();
        body.setIsPublic( true );

        assertThat( target( "/datasets/1/permissions" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "data.isPublic", true )
                .hasFieldOrPropertyWithValue( "data.isShared", false );

        verify( securityService ).makePublic( ee );
        verify( securityService, never() ).makePrivate( ee );
        verify( securityService ).isPublic( ee );
        verify( securityService ).isShared( ee );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetPermissionsMakesPrivate() {
        when( securityService.isPublic( ee ) ).thenReturn( false );
        when( securityService.isShared( ee ) ).thenReturn( true );

        DatasetsWebService.PermissionsUpdateRequest body = new DatasetsWebService.PermissionsUpdateRequest();
        body.setIsPublic( false );

        assertThat( target( "/datasets/1/permissions" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.isPublic", false )
                .hasFieldOrPropertyWithValue( "data.isShared", true );

        verify( securityService ).makePrivate( ee );
        verify( securityService, never() ).makePublic( ee );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetPermissionsReturnsCurrentStateWhenIsPublicOmitted() {
        when( securityService.isPublic( ee ) ).thenReturn( false );
        when( securityService.isShared( ee ) ).thenReturn( false );

        // Body with no fields → both make* calls skipped.
        DatasetsWebService.PermissionsUpdateRequest body = new DatasetsWebService.PermissionsUpdateRequest();

        assertThat( target( "/datasets/1/permissions" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.isPublic", false )
                .hasFieldOrPropertyWithValue( "data.isShared", false );

        verify( securityService, never() ).makePublic( any( ubic.gemma.core.security.model.Securable.class ) );
        verify( securityService, never() ).makePrivate( any( ubic.gemma.core.security.model.Securable.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetPermissionsWithEmptyBodyIs400() {
        assertThat( target( "/datasets/1/permissions" ).request().put( jakarta.ws.rs.client.Entity.json( "null" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( securityService, never() ).makePublic( any( ubic.gemma.core.security.model.Securable.class ) );
        verify( securityService, never() ).makePrivate( any( ubic.gemma.core.security.model.Securable.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetPermissionsWithUnknownDatasetIs404() {
        DatasetsWebService.PermissionsUpdateRequest body = new DatasetsWebService.PermissionsUpdateRequest();
        body.setIsPublic( true );
        assertThat( target( "/datasets/999/permissions" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.NOT_FOUND );
        verifyNoInteractions( securityService );
    }

    @Autowired
    private ubic.gemma.core.analysis.service.ExpressionDataDeleterService expressionDataDeleterService;

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testDeleteDatasetRawDataHappyPath() {
        ee.setId( 1L );
        QuantitationType preferredQt = mock( QuantitationType.class );
        when( expressionExperimentService.getPreferredQuantitationType( ee ) ).thenReturn( Optional.of( preferredQt ) );

        assertThat( target( "/datasets/1/data/raw" ).queryParam( "confirm", true ).request().delete() )
                .hasStatus( Response.Status.NO_CONTENT );

        verify( expressionDataDeleterService ).deleteRawData( ee, preferredQt );
        reset( expressionDataDeleterService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testDeleteDatasetRawDataWithoutConfirmIs400() {
        ee.setId( 1L );
        assertThat( target( "/datasets/1/data/raw" ).request().delete() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verifyNoInteractions( expressionDataDeleterService );
        reset( expressionDataDeleterService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testDeleteDatasetProcessedDataHappyPath() {
        ee.setId( 1L );

        assertThat( target( "/datasets/1/data/processed" ).queryParam( "confirm", true ).request().delete() )
                .hasStatus( Response.Status.NO_CONTENT );

        verify( expressionDataDeleterService ).deleteProcessedData( ee );
        reset( expressionDataDeleterService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testDeleteDatasetProcessedDataWithoutConfirmIs400() {
        ee.setId( 1L );
        assertThat( target( "/datasets/1/data/processed" ).request().delete() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verifyNoInteractions( expressionDataDeleterService );
        reset( expressionDataDeleterService );
    }

    private void mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType techType ) {
        ee.setId( 1L );
        ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails cd =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails();
        ee.setCurationDetails( cd );
        ubic.gemma.model.expression.arrayDesign.ArrayDesign ad =
                ubic.gemma.model.expression.arrayDesign.ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( techType );
        when( expressionExperimentService.getArrayDesignsUsed( ee ) ).thenReturn( Collections.singletonList( ad ) );
        when( expressionExperimentReportService.generateSummary( 1L ) ).thenReturn( null );
        when( expressionExperimentBatchInformationService.checkHasBatchInfo( ee ) ).thenReturn( false );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findStep( List<?> steps, String key ) {
        for ( Object s : steps ) {
            Map<String, Object> m = ( Map<String, Object> ) s;
            if ( key.equals( m.get( "step" ) ) ) {
                return m;
            }
        }
        throw new AssertionError( "step " + key + " missing" );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusReturnsAllStepsWithNotRunOrNotApplicable() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "data.dataset_id", 1 )
                .extracting( "data.steps", list( Map.class ) )
                .isNotEmpty()
                .satisfies( steps -> {
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "preprocess" ).get( "status" ) ).isEqualTo( "notRun" );
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "missingValue" ).get( "status" ) ).isEqualTo( "notApplicable" );
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "batchInfo" ).get( "status" ) ).isEqualTo( "notRun" );
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "pca" ).get( "status" ) ).isEqualTo( "notRun" );
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "dea" ).get( "status" ) ).isEqualTo( "notRun" );
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "coexpression" ).get( "status" ) ).isEqualTo( "notRun" );
                } );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusMissingValueApplicableForTwoColor() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.TWOCOLOR );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "missingValue" ).get( "status" ) ).isEqualTo( "notRun" );
                } );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusPreprocessOk() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        AuditEvent event = AuditEvent.Factory.newInstance( new Date( 1_700_000_000_000L ), AuditAction.UPDATE, "ok", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent() );
        stubLastEvents( Collections.singletonMap(
                ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent.class, event ) );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    Map<String, Object> pp = findStep( steps, "preprocess" );
                    org.assertj.core.api.Assertions.assertThat( pp.get( "status" ) ).isEqualTo( "ok" );
                    org.assertj.core.api.Assertions.assertThat( pp.get( "event_type" ) ).isEqualTo( "ProcessedVectorComputationEvent" );
                } );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusPcaFailed() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        AuditEvent failed = AuditEvent.Factory.newInstance( new Date( 1_700_000_000_000L ), AuditAction.UPDATE, "boom", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.FailedPCAAnalysisEvent() );
        stubLastEvents( Collections.singletonMap(
                ubic.gemma.model.common.auditAndSecurity.eventType.FailedPCAAnalysisEvent.class, failed ) );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    Map<String, Object> pca = findStep( steps, "pca" );
                    org.assertj.core.api.Assertions.assertThat( pca.get( "status" ) ).isEqualTo( "failed" );
                    org.assertj.core.api.Assertions.assertThat( pca.get( "event_type" ) ).isEqualTo( "FailedPCAAnalysisEvent" );
                    org.assertj.core.api.Assertions.assertThat( pca.get( "details" ) ).isEqualTo( "boom" );
                } );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusPicksLatestEvent() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        // Older success, newer failure → newer wins, state="failed".
        AuditEvent oldSuccess = AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent() );
        AuditEvent newFailure = AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, "retry-failed", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.FailedPCAAnalysisEvent() );
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent.class, oldSuccess );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.FailedPCAAnalysisEvent.class, newFailure );
        stubLastEvents( latest );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "pca" ).get( "status" ) ).isEqualTo( "failed" );
                } );
    }

    /**
     * Production batches all per-step lookups through {@link AuditEventService#getLastEvents(java.util.Collection, java.util.Set)}.
     * The returned map shape is {@code Class<? extends AuditEventType> -> ee -> AuditEvent}; an empty inner
     * map yields {@code notRun}.
     */
    private void stubLastEvents( Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> eventsByType ) {
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, Map<ExpressionExperiment, AuditEvent>> result = new LinkedHashMap<>();
        for ( Map.Entry<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> e : eventsByType.entrySet() ) {
            result.put( e.getKey(), Collections.singletonMap( ee, e.getValue() ) );
        }
        when( auditEventService.getLastEvents( eq( Collections.singleton( ee ) ), anySet() ) )
                .thenReturn( result );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusIncludesConvenienceFields() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        ee.getCurationDetails().setNeedsAttention( true );
        when( expressionExperimentBatchInformationService.checkHasBatchInfo( ee ) ).thenReturn( true );
        when( securityService.isPublic( ee ) ).thenReturn( true );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.has_batch_information", true )
                .hasFieldOrPropertyWithValue( "data.needs_attention", true )
                .hasFieldOrPropertyWithValue( "data.is_public", true );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetPipelineStatusAdminSeesCurationNote() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        ee.getCurationDetails().setCurationNote( "admin only" );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.curation_note", "admin only" );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusNonAdminDoesNotSeeCurationNote() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        ee.getCurationDetails().setCurationNote( "hidden" );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.curation_note", null );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/999/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetGeeq() {
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        ubic.gemma.model.expression.experiment.Geeq geeq = new ubic.gemma.model.expression.experiment.Geeq();
        ee.setGeeq( geeq );
        when( expressionExperimentService.thawLiter( ee ) ).thenReturn( ee );
        Date computedAt = new Date( 1_700_000_000_000L );
        AuditEvent geeqEvent = AuditEvent.Factory.newInstance( computedAt, AuditAction.UPDATE, null, null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent() );
        when( auditEventService.getLastEvent( eq( ee ),
                eq( ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent.class ) ) )
                .thenReturn( geeqEvent );

        assertThat( target( "/datasets/1/geeq" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" )
                .hasFieldOrProperty( "data.lastComputed" );

        verify( expressionExperimentService ).thawLiter( ee );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetGeeqWithoutEvent() {
        ee.setId( 1L );
        ee.setGeeq( new ubic.gemma.model.expression.experiment.Geeq() );
        when( expressionExperimentService.thawLiter( ee ) ).thenReturn( ee );

        assertThat( target( "/datasets/1/geeq" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.lastComputed", null );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetGeeqWhenNotComputedIs404() {
        ee.setShortName( "GSE1" );
        ee.setGeeq( null );
        when( expressionExperimentService.thawLiter( ee ) ).thenReturn( ee );

        assertThat( target( "/datasets/1/geeq" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );

        verify( auditEventService, never() ).getLastEvent( eq( ee ), any( Class.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetGeeqWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/999/geeq" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
        verifyNoInteractions( geeqService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRecomputeDatasetGeeqDefaultModeIsAll() {
        ee.setId( 1L );
        ubic.gemma.model.expression.experiment.Geeq updated = new ubic.gemma.model.expression.experiment.Geeq();
        when( geeqService.calculateScore( eq( ee ), any( ubic.gemma.persistence.service.expression.experiment.GeeqService.ScoreMode.class ) ) )
                .thenReturn( updated );

        assertThat( target( "/datasets/1/geeq" ).request().put( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );

        verify( geeqService ).calculateScore( ee, ubic.gemma.persistence.service.expression.experiment.GeeqService.ScoreMode.all );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRecomputeDatasetGeeqWithSpecificMode() {
        ee.setId( 1L );
        when( geeqService.calculateScore( eq( ee ), any( ubic.gemma.persistence.service.expression.experiment.GeeqService.ScoreMode.class ) ) )
                .thenReturn( new ubic.gemma.model.expression.experiment.Geeq() );

        assertThat( target( "/datasets/1/geeq" ).queryParam( "mode", "batch" ).request()
                .put( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.OK );

        verify( geeqService ).calculateScore( ee, ubic.gemma.persistence.service.expression.experiment.GeeqService.ScoreMode.batch );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRecomputeDatasetGeeqIncludesLastComputed() {
        ee.setId( 1L );
        when( geeqService.calculateScore( eq( ee ), any( ubic.gemma.persistence.service.expression.experiment.GeeqService.ScoreMode.class ) ) )
                .thenReturn( new ubic.gemma.model.expression.experiment.Geeq() );
        Date computedAt = new Date( 1_700_000_000_000L );
        AuditEvent geeqEvent = AuditEvent.Factory.newInstance( computedAt, AuditAction.UPDATE, null, null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent() );
        when( auditEventService.getLastEvent( eq( ee ),
                eq( ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent.class ) ) )
                .thenReturn( geeqEvent );

        assertThat( target( "/datasets/1/geeq" ).request().put( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrProperty( "data.lastComputed" );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRecomputeDatasetGeeqWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/999/geeq" ).request().put( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.NOT_FOUND );
        verifyNoInteractions( geeqService );
    }

    private void mockTaskSubmission( String taskId ) {
        when( taskRunningService.submitTaskCommand( any() ) ).thenReturn( taskId );
        ubic.gemma.core.job.SubmittedTask task = mock( ubic.gemma.core.job.SubmittedTask.class );
        when( task.getTaskId() ).thenReturn( taskId );
        when( task.getStatus() ).thenReturn( ubic.gemma.core.job.SubmittedTask.Status.QUEUED );
        when( taskRunningService.getSubmittedTask( taskId ) ).thenReturn( task );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetPreprocess() {
        ee.setId( 1L );
        mockTaskSubmission( "task-1" );

        assertThat( target( "/datasets/1/tasks/preprocess" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.ACCEPTED )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasHeaderSatisfying( "Location", values ->
                        assertThat( values ).singleElement().asString().endsWith( "/tasks/task-1" ) )
                .entity()
                .hasFieldOrPropertyWithValue( "data.taskId", "task-1" );

        ArgumentCaptor<ubic.gemma.core.tasks.analysis.expression.PreprocessTaskCommand> cmd =
                ArgumentCaptor.forClass( ubic.gemma.core.tasks.analysis.expression.PreprocessTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getValue().diagnosticsOnly() ).isFalse();
        verify( expressionExperimentReportService, atLeastOnce() ).evictFromCache( 1L );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetPreprocessWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/999/tasks/preprocess" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.NOT_FOUND );
        verifyNoInteractions( taskRunningService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetDiagnosticsSetsFlag() {
        ee.setId( 1L );
        mockTaskSubmission( "task-diag" );

        assertThat( target( "/datasets/1/tasks/diagnostics" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.ACCEPTED );

        ArgumentCaptor<ubic.gemma.core.tasks.analysis.expression.PreprocessTaskCommand> cmd =
                ArgumentCaptor.forClass( ubic.gemma.core.tasks.analysis.expression.PreprocessTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getValue().diagnosticsOnly() ).isTrue();
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetBatchInformationFetch() {
        ee.setId( 1L );
        mockTaskSubmission( "task-batch" );

        assertThat( target( "/datasets/1/tasks/batchInfo" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.ACCEPTED )
                .hasHeaderSatisfying( "Location", values ->
                        assertThat( values ).singleElement().asString().endsWith( "/tasks/task-batch" ) );

        verify( taskRunningService ).submitTaskCommand( any( ubic.gemma.core.tasks.analysis.expression.BatchInfoFetchTaskCommand.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetGeeqWithModeBatch() {
        ee.setId( 1L );
        mockTaskSubmission( "task-geeq" );

        assertThat( target( "/datasets/1/tasks/geeq" ).queryParam( "mode", "batch" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.ACCEPTED )
                .hasHeaderSatisfying( "Location", values ->
                        assertThat( values ).singleElement().asString().endsWith( "/tasks/task-geeq" ) )
                .entity()
                .hasFieldOrPropertyWithValue( "data.taskId", "task-geeq" );

        ArgumentCaptor<ubic.gemma.core.tasks.analysis.expression.GeeqTaskCommand> cmd =
                ArgumentCaptor.forClass( ubic.gemma.core.tasks.analysis.expression.GeeqTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getValue().getExpressionExperiment() ).isSameAs( ee );
        assertThat( cmd.getValue().getMode() )
                .isEqualTo( ubic.gemma.persistence.service.expression.experiment.GeeqService.ScoreMode.batch );
        verify( expressionExperimentReportService, atLeastOnce() ).evictFromCache( 1L );
        verifyNoInteractions( geeqService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetSwitchPlatformWithTargetShortName() {
        ee.setId( 1L );
        ubic.gemma.model.expression.arrayDesign.ArrayDesign target = ubic.gemma.model.expression.arrayDesign.ArrayDesign.Factory.newInstance();
        target.setId( 42L );
        target.setShortName( "GPL570" );
        when( arrayDesignService.findByShortName( "GPL570" ) ).thenReturn( target );
        mockTaskSubmission( "task-switch" );

        assertThat( target( "/datasets/1/tasks/switch-platform" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "{\"targetArrayDesignName\":\"GPL570\"}" ) ) )
                .hasStatus( Response.Status.ACCEPTED )
                .hasHeaderSatisfying( "Location", values ->
                        assertThat( values ).singleElement().asString().endsWith( "/tasks/task-switch" ) )
                .entity()
                .hasFieldOrPropertyWithValue( "data.taskId", "task-switch" );

        ArgumentCaptor<ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentPlatformSwitchTaskCommand> cmd =
                ArgumentCaptor.forClass( ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentPlatformSwitchTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getValue().getExpressionExperiment() ).isSameAs( ee );
        assertThat( cmd.getValue().getTargetArrayDesign() ).isSameAs( target );
        verify( expressionExperimentReportService, atLeastOnce() ).evictFromCache( 1L );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetSwitchPlatformWithoutBodyAutoDetectsMerged() {
        ee.setId( 1L );
        mockTaskSubmission( "task-switch-auto" );

        assertThat( target( "/datasets/1/tasks/switch-platform" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "{}" ) ) )
                .hasStatus( Response.Status.ACCEPTED );

        ArgumentCaptor<ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentPlatformSwitchTaskCommand> cmd =
                ArgumentCaptor.forClass( ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentPlatformSwitchTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getValue().getExpressionExperiment() ).isSameAs( ee );
        assertThat( cmd.getValue().getTargetArrayDesign() ).isNull();
        verifyNoInteractions( arrayDesignService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetSwitchPlatformUnknownShortNameIs400() {
        ee.setId( 1L );
        when( arrayDesignService.findByShortName( "GPL_NOSUCH" ) ).thenReturn( null );

        assertThat( target( "/datasets/1/tasks/switch-platform" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "{\"targetArrayDesignName\":\"GPL_NOSUCH\"}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );

        verifyNoInteractions( taskRunningService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetDifferentialAnalysisWithoutBodyUsesAllNonBatchFactors() {
        ee.setId( 1L );
        ExperimentalDesign design = new ExperimentalDesign();
        ExperimentalFactor regular = ExperimentalFactor.Factory.newInstance();
        regular.setId( 10L );
        regular.setType( ubic.gemma.model.expression.experiment.FactorType.CATEGORICAL );
        regular.setCategory( ubic.gemma.model.common.description.Characteristic.Factory.newInstance() );
        design.getExperimentalFactors().add( regular );
        ee.setExperimentalDesign( design );
        when( expressionExperimentService.thawLite( ee ) ).thenReturn( ee );
        when( expressionExperimentService.isRNASeq( ee ) ).thenReturn( false );
        mockTaskSubmission( "task-dea" );

        assertThat( target( "/datasets/1/tasks/differential" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "{}" ) ) )
                .hasStatus( Response.Status.ACCEPTED );

        ArgumentCaptor<ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand> cmd =
                ArgumentCaptor.forClass( ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand.class );
        verify( taskRunningService ).submitTaskCommand( cmd.capture() );
        assertThat( cmd.getValue().getFactors() ).containsExactly( regular );
        assertThat( cmd.getValue().isIncludeInteractions() ).isTrue();
        assertThat( cmd.getValue().getSubsetFactor() ).isNull();
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetDifferentialAnalysisWithoutDesignIs400() {
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        ee.setExperimentalDesign( null );
        when( expressionExperimentService.thawLite( ee ) ).thenReturn( ee );

        assertThat( target( "/datasets/1/tasks/differential" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "{}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verifyNoInteractions( taskRunningService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetDifferentialAnalysisRejectsUnknownFactorId() {
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        ExperimentalDesign design = new ExperimentalDesign();
        ExperimentalFactor regular = ExperimentalFactor.Factory.newInstance();
        regular.setId( 10L );
        design.getExperimentalFactors().add( regular );
        ee.setExperimentalDesign( design );
        when( expressionExperimentService.thawLite( ee ) ).thenReturn( ee );

        assertThat( target( "/datasets/1/tasks/differential" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "{\"factorIds\":[999]}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verifyNoInteractions( taskRunningService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRunDatasetDifferentialAnalysisRejectsSubsetFactorInFactorIds() {
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        ExperimentalDesign design = new ExperimentalDesign();
        ExperimentalFactor f = ExperimentalFactor.Factory.newInstance();
        f.setId( 10L );
        design.getExperimentalFactors().add( f );
        ee.setExperimentalDesign( design );
        when( expressionExperimentService.thawLite( ee ) ).thenReturn( ee );

        assertThat( target( "/datasets/1/tasks/differential" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "{\"factorIds\":[10],\"subsetFactorId\":10}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verifyNoInteractions( taskRunningService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRedoDatasetDifferentialAnalysis() {
        ee.setId( 1L );
        mockTaskSubmission( "task-redo" );
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis dea =
                ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis.Factory.newInstance();
        dea.setId( 500L );
        when( differentialExpressionAnalysisService.findByExperimentAndAnalysisId( ee, true, 500L ) ).thenReturn( dea );

        assertThat( target( "/datasets/1/tasks/redo/500" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.ACCEPTED )
                .hasHeaderSatisfying( "Location", values ->
                        assertThat( values ).singleElement().asString().endsWith( "/tasks/task-redo" ) );

        verify( taskRunningService ).submitTaskCommand( any( ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRedoDatasetDifferentialAnalysisWithUnknownAnalysisIs404() {
        ee.setShortName( "GSE1" );
        when( differentialExpressionAnalysisService.findByExperimentAndAnalysisId( ee, true, 999L ) ).thenReturn( null );

        assertThat( target( "/datasets/1/tasks/redo/999" ).request()
                .post( jakarta.ws.rs.client.Entity.json( "" ) ) )
                .hasStatus( Response.Status.NOT_FOUND );
        verifyNoInteractions( taskRunningService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRemoveDatasetDifferentialAnalysis() {
        ee.setId( 1L );
        mockTaskSubmission( "task-remove" );
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis dea =
                ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis.Factory.newInstance();
        dea.setId( 500L );
        when( differentialExpressionAnalysisService.findByExperimentAndAnalysisId( ee, true, 500L ) ).thenReturn( dea );

        assertThat( target( "/datasets/1/tasks/differential/500" ).request().delete() )
                .hasStatus( Response.Status.ACCEPTED )
                .hasHeaderSatisfying( "Location", values ->
                        assertThat( values ).singleElement().asString().endsWith( "/tasks/task-remove" ) );

        verify( taskRunningService ).submitTaskCommand( any( ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisRemoveTaskCommand.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRemoveDatasetDifferentialAnalysisWithUnknownAnalysisIs404() {
        ee.setShortName( "GSE1" );
        when( differentialExpressionAnalysisService.findByExperimentAndAnalysisId( ee, true, 999L ) ).thenReturn( null );

        assertThat( target( "/datasets/1/tasks/differential/999" ).request().delete() )
                .hasStatus( Response.Status.NOT_FOUND );
        verifyNoInteractions( taskRunningService );
    }

    @Test
    public void testGetDatasetDesignJson() {
        ExperimentalDesignValueObject vo = new ExperimentalDesignValueObject();
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( vo );

        assertThat( target( "/datasets/1/design" ).request( MediaType.APPLICATION_JSON ).get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" );

        verify( expressionExperimentService ).getExperimentalDesignValueObject( ee );
    }

    @Test
    public void testGetDatasetDesignDefaultIsJson() {
        ExperimentalDesignValueObject vo = new ExperimentalDesignValueObject();
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( vo );

        // No Accept header → server returns the highest-q producer; the @Produces TSV variant is qs=0.9,
        // so JSON wins.
        assertThat( target( "/datasets/1/design" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetDesignJsonReturnsNotFoundWhenDesignMissing() {
        ee.setShortName( "GSE1" );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( null );

        assertThat( target( "/datasets/1/design" ).request( MediaType.APPLICATION_JSON ).get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    @Test
    public void testGetDatasetDesignJsonWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/999/design" ).request( MediaType.APPLICATION_JSON ).get() )
                .hasStatus( Response.Status.NOT_FOUND );
        verify( expressionExperimentService, never() ).getExperimentalDesignValueObject( any() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testReplaceDatasetDesignHappyPath() {
        DesignPreflightReport clean = new DesignPreflightReport();
        when( expressionExperimentService.previewDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) )
                .thenReturn( clean );
        DesignApplyOutcome updated = new DesignApplyOutcome( true, new ExperimentalDesignValueObject(), new DesignPreflightReport() );
        when( expressionExperimentService.applyDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) )
                .thenReturn( updated );

        assertThat( target( "/datasets/1/design" ).request()
                .put( jakarta.ws.rs.client.Entity.json( new ExperimentalDesignValueObject() ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" );

        verify( expressionExperimentService ).applyDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testReplaceDatasetDesignReturnsBadRequestOnBlockers() {
        DesignPreflightReport report = new DesignPreflightReport();
        DesignPreflightReport.Blocker b = new DesignPreflightReport.Blocker( "UNKNOWN_FACTOR_VALUE_ID", "bad payload" );
        report.getBlockers().add( b );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) )
                .thenReturn( report );

        assertThat( target( "/datasets/1/design" ).request()
                .put( jakarta.ws.rs.client.Entity.json( new ExperimentalDesignValueObject() ) ) )
                .hasStatus( Response.Status.BAD_REQUEST )
                .entity()
                .extracting( "data.blockers", list( Map.class ) )
                .hasSize( 1 );

        verify( expressionExperimentService, never() ).applyDesignChange( any(), any() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testReplaceDatasetDesignReturns409WhenForceRequired() {
        DesignPreflightReport report = new DesignPreflightReport();
        report.getDifferentialExpressionAnalysesToDelete().add(
                new DesignPreflightReport.AnalysisRef( 500L, "vs control", null ) );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) )
                .thenReturn( report );

        assertThat( target( "/datasets/1/design" ).request()
                .put( jakarta.ws.rs.client.Entity.json( new ExperimentalDesignValueObject() ) ) )
                .hasStatus( Response.Status.CONFLICT )
                .entity()
                .extracting( "data.differentialExpressionAnalysesToDelete", list( Map.class ) )
                .hasSize( 1 );

        verify( expressionExperimentService, never() ).applyDesignChange( any(), any() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testReplaceDatasetDesignWithForceAppliesEvenWithCascade() {
        DesignPreflightReport report = new DesignPreflightReport();
        report.getDifferentialExpressionAnalysesToDelete().add(
                new DesignPreflightReport.AnalysisRef( 500L, "vs control", null ) );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) )
                .thenReturn( report );
        when( expressionExperimentService.applyDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) ) )
                .thenReturn( new DesignApplyOutcome( true, new ExperimentalDesignValueObject(), new DesignPreflightReport() ) );

        assertThat( target( "/datasets/1/design" ).queryParam( "force", "true" ).request()
                .put( jakarta.ws.rs.client.Entity.json( new ExperimentalDesignValueObject() ) ) )
                .hasStatus( Response.Status.OK );

        verify( expressionExperimentService ).applyDesignChange( eq( ee ), any( ExperimentalDesignValueObject.class ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testReplaceDatasetDesignWithEmptyBodyIs400() {
        assertThat( target( "/datasets/1/design" ).request()
                .put( jakarta.ws.rs.client.Entity.json( "null" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).previewDesignChange( any(), any() );
        verify( expressionExperimentService, never() ).applyDesignChange( any(), any() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testReplaceDatasetDesignWithUnknownDatasetIs404() {
        assertThat( target( "/datasets/999/design" ).request()
                .put( jakarta.ws.rs.client.Entity.json( new ExperimentalDesignValueObject() ) ) )
                .hasStatus( Response.Status.NOT_FOUND );
        verify( expressionExperimentService, never() ).applyDesignChange( any(), any() );
    }

    // --- Diagnostics: sample-correlation -------------------------------------------------

    @Test
    public void testGetDatasetSampleCorrelation() {
        BioAssay a1 = BioAssay.Factory.newInstance( "BA1" );
        a1.setId( 100L );
        BioAssay a2 = BioAssay.Factory.newInstance( "BA2" );
        a2.setId( 101L );
        BioAssay a3 = BioAssay.Factory.newInstance( "BA3" );
        a3.setId( 102L );
        List<BioAssay> assays = Arrays.asList( a1, a2, a3 );
        DenseDoubleMatrix<BioAssay, BioAssay> matrix = new DenseDoubleMatrix<>( new double[][] {
                { 1.0, 0.5, 0.1 },
                { 0.5, 1.0, 0.2 },
                { 0.1, 0.2, 1.0 }
        } );
        matrix.setRowNames( assays );
        matrix.setColumnNames( assays );
        when( sampleCoexpressionAnalysisService.loadBestMatrix( ee ) ).thenReturn( matrix );
        // Handler now thaws bioassays + reads outlier flags. Stub so the thawed EE
        // surfaces with the same BioAssays the matrix is keyed on, so the actualOutlierBioAssayIds
        // path can iterate without NPE.
        ee.getBioAssays().clear();
        ee.getBioAssays().addAll( assays );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/sample-correlation" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" )
                .extracting( "data.bioAssayIds", list( Integer.class ) )
                .containsExactly( 100, 101, 102 );
        verify( sampleCoexpressionAnalysisService ).loadBestMatrix( ee );
    }

    @Test
    public void testGetDatasetSampleCorrelationWhenNoneIs404() {
        when( sampleCoexpressionAnalysisService.loadBestMatrix( ee ) ).thenReturn( null );
        assertThat( target( "/datasets/1/sample-correlation" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( sampleCoexpressionAnalysisService ).loadBestMatrix( ee );
    }

    @Test
    public void testGetDatasetSampleCorrelationWhenDatasetMissingIs404() {
        assertThat( target( "/datasets/999/sample-correlation" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
        verify( sampleCoexpressionAnalysisService, never() ).loadBestMatrix( any() );
    }

    // --- Diagnostics: mean-variance ------------------------------------------------------

    @Test
    public void testGetDatasetMeanVariance() {
        double[] means = { 1.0, 2.0, 3.0, 4.0 };
        double[] variances = { 0.1, 0.4, 0.9, 1.6 };
        ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation mvr = ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation.Factory.newInstance( means, variances );
        ee.setMeanVarianceRelation( mvr );
        // Handler now re-loads via loadWithMeanVarianceRelation (LazyInit fix a70e3dc8f6).
        when( expressionExperimentService.loadWithMeanVarianceRelation( ee.getId() ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/mean-variance" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" )
                .extracting( "data.means", list( Double.class ) )
                .containsExactly( 1.0, 2.0, 3.0, 4.0 );
    }

    @Test
    public void testGetDatasetMeanVarianceWhenNoneIs404() {
        ee.setMeanVarianceRelation( null );
        when( expressionExperimentService.loadWithMeanVarianceRelation( ee.getId() ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/mean-variance" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetMeanVarianceWhenDatasetMissingIs404() {
        assertThat( target( "/datasets/999/mean-variance" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    // --- Preprocessing metadata files ----------------------------------------------------

    @Test
    public void testListDatasetMetadataFilesEmpty() throws IOException {
        when( expressionDataFileService.getMetadataFile( eq( ee ),
                any( ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType.class ),
                eq( false ) ) ).thenReturn( Optional.empty() );
        assertThat( target( "/datasets/1/metadata" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" )
                .extracting( "data", list( Object.class ) )
                .isEmpty();
    }

    @Test
    public void testListDatasetMetadataFilesReturnsAvailable() throws IOException {
        // Make BASE_METADATA present on disk; everything else absent. Probe must point at a
        // readable file so the isReadable() filter keeps it.
        java.nio.file.Path probe = java.nio.file.Files.createTempFile( "ee.base.metadata", ".txt" );
        java.nio.file.Files.writeString( probe, "stub" );
        LockedPath lp = mock( LockedPath.class );
        when( lp.getPath() ).thenReturn( probe );
        when( expressionDataFileService.getMetadataFile( eq( ee ),
                any( ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType.class ),
                eq( false ) ) ).thenReturn( Optional.empty() );
        when( expressionDataFileService.getMetadataFile( eq( ee ),
                eq( ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType.BASE_METADATA ),
                eq( false ) ) ).thenReturn( Optional.of( lp ) );

        assertThat( target( "/datasets/1/metadata" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Object.class ) )
                .singleElement()
                .extracting( "type" )
                .isEqualTo( "BASE_METADATA" );
    }

    @Test
    public void testGetDatasetMetadataFileWithUnknownTypeIs400() {
        assertThat( target( "/datasets/1/metadata/NOT_A_REAL_TYPE" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    @Test
    public void testGetDatasetMetadataFileForDirectoryTypeIs400() {
        // ADDITIONAL_PIPELINE_CONFIGURATIONS has isDirectory=true; should not be downloadable directly.
        assertThat( target( "/datasets/1/metadata/ADDITIONAL_PIPELINE_CONFIGURATIONS" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    @Test
    public void testGetDatasetMetadataFileWhenAbsentIs404() throws IOException {
        when( expressionDataFileService.getMetadataFile( eq( ee ),
                eq( ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType.BASE_METADATA ),
                eq( false ) ) ).thenReturn( Optional.empty() );
        assertThat( target( "/datasets/1/metadata/BASE_METADATA" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    @Test
    public void testGetDatasetMetadataFileServesPayload() throws IOException {
        java.nio.file.Path payload = java.nio.file.Files.createTempFile( "ee.base.metadata", ".txt" );
        java.nio.file.Files.writeString( payload, "alignment summary contents" );
        LockedPath lp = mock( LockedPath.class );
        when( lp.getPath() ).thenReturn( payload );
        when( expressionDataFileService.getMetadataFile( eq( ee ),
                eq( ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType.BASE_METADATA ),
                eq( false ) ) ).thenReturn( Optional.of( lp ) );

        assertThat( target( "/datasets/1/metadata/BASE_METADATA" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.TEXT_PLAIN_TYPE );
    }

    @Test
    public void testGetDatasetMetadataFileWhenDatasetMissingIs404() {
        assertThat( target( "/datasets/999/metadata/BASE_METADATA" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    // --- Diagnostics: svd loadings -------------------------------------------------------

    @Test
    public void testGetDatasetSvdLoadings() {
        // Stub: ee has an SVD; topLoadedVectors returns 3 probes on PC1.
        when( svdService.hasSvd( ee ) ).thenReturn( true );
        ubic.gemma.model.expression.designElement.CompositeSequence p1 = ubic.gemma.model.expression.designElement.CompositeSequence.Factory.newInstance( "probe_a" );
        p1.setId( 10L );
        ubic.gemma.model.expression.designElement.CompositeSequence p2 = ubic.gemma.model.expression.designElement.CompositeSequence.Factory.newInstance( "probe_b" );
        p2.setId( 11L );
        ubic.gemma.model.expression.designElement.CompositeSequence p3 = ubic.gemma.model.expression.designElement.CompositeSequence.Factory.newInstance( "probe_c" );
        p3.setId( 12L );
        ubic.gemma.model.analysis.expression.pca.ProbeLoading pl1 = ubic.gemma.model.analysis.expression.pca.ProbeLoading.Factory.newInstance( 1, 0.9, 1, p1 );
        ubic.gemma.model.analysis.expression.pca.ProbeLoading pl2 = ubic.gemma.model.analysis.expression.pca.ProbeLoading.Factory.newInstance( 1, -0.7, 2, p2 );
        ubic.gemma.model.analysis.expression.pca.ProbeLoading pl3 = ubic.gemma.model.analysis.expression.pca.ProbeLoading.Factory.newInstance( 1, 0.3, 3, p3 );
        Map<ubic.gemma.model.analysis.expression.pca.ProbeLoading, ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject> stored = new LinkedHashMap<>();
        stored.put( pl1, null );
        stored.put( pl2, null );
        stored.put( pl3, null );
        when( svdService.getTopLoadedVectors( eq( ee ), anyInt(), anyInt() ) ).thenReturn( stored );

        // SVDResult with a 2×2 vMatrix; column 0 (PC1) gives bioAssay scores.
        BioAssay a1 = BioAssay.Factory.newInstance( "BA1" );
        a1.setId( 200L );
        BioAssay a2 = BioAssay.Factory.newInstance( "BA2" );
        a2.setId( 201L );
        ubic.gemma.model.expression.biomaterial.BioMaterial m1 = ubic.gemma.model.expression.biomaterial.BioMaterial.Factory.newInstance();
        ubic.gemma.model.expression.biomaterial.BioMaterial m2 = ubic.gemma.model.expression.biomaterial.BioMaterial.Factory.newInstance();
        DenseDoubleMatrix<ubic.gemma.model.expression.biomaterial.BioMaterial, Integer> vMatrix = new DenseDoubleMatrix<>( new double[][] {
                { 0.5, 0.1 },
                { -0.5, 0.2 }
        } );
        vMatrix.setRowNames( Arrays.asList( m1, m2 ) );
        vMatrix.setColumnNames( Arrays.asList( 0, 1 ) );
        ubic.gemma.core.analysis.preprocess.svd.SVDResult svd = mock( ubic.gemma.core.analysis.preprocess.svd.SVDResult.class );
        when( svd.getVMatrix() ).thenReturn( vMatrix );
        when( svd.getBioAssays() ).thenReturn( Arrays.asList( a1, a2 ) );
        when( svdService.getSvd( ee ) ).thenReturn( svd );

        assertThat( target( "/datasets/1/svd/loadings" ).queryParam( "pc", 1 ).queryParam( "top", 2 ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrProperty( "data" )
                .hasFieldOrPropertyWithValue( "data.pc", 1 )
                .extracting( "data.rows", list( Map.class ) )
                .hasSize( 2 );
        // default direction=both sorts by |loading| desc: pl1 (0.9), pl2 (-0.7), pl3 (0.3) → first two are 0.9, -0.7.
        verify( svdService ).hasSvd( ee );
        verify( svdService ).getTopLoadedVectors( eq( ee ), eq( 1 ), anyInt() );
        verify( svdService ).getSvd( ee );
    }

    @Test
    public void testGetDatasetSvdLoadingsWhenNoSvdIs404() {
        when( svdService.hasSvd( ee ) ).thenReturn( false );
        assertThat( target( "/datasets/1/svd/loadings" ).queryParam( "pc", 1 ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( svdService ).hasSvd( ee );
        verify( svdService, never() ).getTopLoadedVectors( any(), anyInt(), anyInt() );
    }

    @Test
    public void testGetDatasetSvdLoadingsWithMissingPcIs400() {
        assertThat( target( "/datasets/1/svd/loadings" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verifyNoInteractions( svdService );
    }

    @Test
    public void testGetDatasetSvdLoadingsWithPcZeroIs400() {
        assertThat( target( "/datasets/1/svd/loadings" ).queryParam( "pc", 0 ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verifyNoInteractions( svdService );
    }

    @Test
    public void testGetDatasetSvdLoadingsWithTopOverCapIs400() {
        assertThat( target( "/datasets/1/svd/loadings" ).queryParam( "pc", 1 ).queryParam( "top", 600 ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verifyNoInteractions( svdService );
    }

    @Test
    public void testGetDatasetSvdLoadingsWithUnknownDirectionIs400() {
        // Jersey enum-coerces the @QueryParam — an unknown value becomes a 404 from a NotFoundException
        // raised by the param converter (this is the documented Jersey behaviour for enum @QueryParam).
        // Accept either 404 (Jersey default) or 400 to keep the test framework-version-tolerant.
        Response.StatusType status = target( "/datasets/1/svd/loadings" )
                .queryParam( "pc", 1 )
                .queryParam( "direction", "sideways" )
                .request().get().getStatusInfo();
        assertThat( status.getStatusCode() ).isIn( 400, 404 );
        verifyNoInteractions( svdService );
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

    @Test
    @WithMockUser
    public void testUpdateDatasetPublications() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( 1L ) ).thenReturn( ee );
        BibliographicReference prim = new BibliographicReference();
        prim.setId( 10L );
        BibliographicReference other = new BibliographicReference();
        other.setId( 20L );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "111" ) ).thenReturn( prim );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "222" ) ).thenReturn( other );

        String body = "{\"primaryPublication\":{\"pubMedId\":\"111\"},\"otherRelevantPublications\":[{\"pubMedId\":\"222\"}]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );

        verify( bibliographicReferenceService ).findOrCreateByPubMedId( "111" );
        verify( bibliographicReferenceService ).findOrCreateByPubMedId( "222" );
        ArgumentCaptor<Collection<BibliographicReference>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updatePublications( eq( ee ), eq( prim ), captor.capture() );
        assertThat( captor.getValue() ).containsExactly( other );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsByDoi() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( 1L ) ).thenReturn( ee );
        BibliographicReference preprint = new BibliographicReference();
        preprint.setId( 30L );
        when( bibliographicReferenceService.findOrCreateByDoi( "10.1101/2025.01.02.634567" ) ).thenReturn( preprint );

        String body = "{\"primaryPublication\":{\"doi\":\"10.1101/2025.01.02.634567\"},\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( bibliographicReferenceService ).findOrCreateByDoi( "10.1101/2025.01.02.634567" );
        verify( expressionExperimentService ).updatePublications( eq( ee ), eq( preprint ), argThat( Collection::isEmpty ) );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsRejectsBothPubMedIdAndDoi() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"primaryPublication\":{\"pubMedId\":\"111\",\"doi\":\"10.1101/x\"},\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updatePublications( any(), any(), any() );
        verifyNoInteractions( bibliographicReferenceService );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsClearAll() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( 1L ) ).thenReturn( ee );

        String body = "{\"primaryPublication\":null,\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( expressionExperimentService ).updatePublications( eq( ee ), isNull(), argThat( Collection::isEmpty ) );
        verifyNoInteractions( bibliographicReferenceService );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsMissingOtherList() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        // empty object -- otherRelevantPublications is null, which is rejected to avoid silently wiping publications
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( "{}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updatePublications( any(), any(), any() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsRejectsBlankPubMedId() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"otherRelevantPublications\":[{\"pubMedId\":\"  \"}]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updatePublications( any(), any(), any() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsUnresolvablePubMedIdIsBadRequest() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "999" ) )
                .thenThrow( new IllegalStateException( "No PubMed record found for id=999." ) );
        String body = "{\"primaryPublication\":{\"pubMedId\":\"999\"},\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updatePublications( any(), any(), any() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetBasics() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.updateNameAndDescription( eq( ee ), eq( "New title" ), eq( "New description" ) ) )
                .thenReturn( true );
        String body = "{\"name\":\"New title\",\"description\":\"New description\"}";
        assertThat( target( "/datasets/1" ).request().method( "PATCH", Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).updateNameAndDescription( ee, "New title", "New description" );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), anyString() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetBasicsDescriptionOnly() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.updateNameAndDescription( eq( ee ), isNull(), eq( "Only desc" ) ) )
                .thenReturn( true );
        assertThat( target( "/datasets/1" ).request().method( "PATCH", Entity.json( "{\"description\":\"Only desc\"}" ) ) )
                .hasStatus( Response.Status.OK );
        verify( expressionExperimentService ).updateNameAndDescription( ee, null, "Only desc" );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetBasicsRejectsEmptyBody() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        assertThat( target( "/datasets/1" ).request().method( "PATCH", Entity.json( "{}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updateNameAndDescription( any(), any(), any() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetBasicsRejectsBlankName() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        assertThat( target( "/datasets/1" ).request().method( "PATCH", Entity.json( "{\"name\":\"   \"}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updateNameAndDescription( any(), any(), any() );
    }

    @Test
    @WithMockUser
    public void testCommitCurationPublications() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        BibliographicReference ref = new BibliographicReference();
        ref.setId( 10L );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "111" ) ).thenReturn( ref );
        ubic.gemma.persistence.service.expression.experiment.CurationCommitResult res =
                new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult();
        res.setPublicationsCreated( 1 );
        when( expressionExperimentService.commitCuration( eq( ee ), any( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class ), eq( false ) ) )
                .thenReturn( res );

        String body = "{\"publications\":{\"primary\":{\"pubMedId\":\"111\"},\"otherRelevant\":[]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().isPublicationsPresent() ).isTrue();
        assertThat( cap.getValue().getPrimaryPublication() ).isEqualTo( ref );
    }

    @Test
    @WithMockUser
    public void testCommitCurationBasics() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        ubic.gemma.persistence.service.expression.experiment.CurationCommitResult res =
                new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult();
        res.setBasicsChanged( true );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) ).thenReturn( res );

        String body = "{\"basics\":{\"name\":\"New name\",\"description\":\"d\"}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().isBasicsPresent() ).isTrue();
        assertThat( cap.getValue().getName() ).isEqualTo( "New name" );
    }

    @Test
    @WithMockUser
    public void testCommitCurationRejectsUnknownSection() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        // All six sections are supported now; the strict document root rejects an unknown field.
        String body = "{\"bogusSection\":{\"items\":[]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /** Build a minimal current design VO with the given existing factor ids (no FVs, no assignments). */
    private ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject currentDesign( Long... factorIds ) {
        ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject d =
                new ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject();
        d.setId( 3L );
        List<ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.ExperimentalFactorEntry> factors = new ArrayList<>();
        for ( Long id : factorIds ) {
            ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.ExperimentalFactorEntry f =
                    new ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.ExperimentalFactorEntry();
            f.setId( id );
            f.setName( "factor" + id );
            factors.add( f );
        }
        d.setExperimentalFactors( factors );
        return d;
    }

    @Test
    @WithMockUser
    public void testCommitCurationDesignCreatesFactor() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( currentDesign( 5L ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any() ) )
                .thenReturn( new ubic.gemma.model.expression.experiment.DesignPreflightReport() );
        ubic.gemma.persistence.service.expression.experiment.CurationCommitResult res =
                new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult();
        res.setDesignCreated( 1 );
        res.setDesignIdMap( java.util.Map.of( "f1", 7L ) );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) ).thenReturn( res );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"clientRef\":\"f1\",\"name\":\"genotype\","
                + "\"category\":{\"label\":\"genotype\"},\"factorValues\":{\"items\":[{\"clientRef\":\"fv1\","
                + "\"freeTextLabel\":\"WT\"}]}}]}}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest req = cap.getValue();
        assertThat( req.isDesignPresent() ).isTrue();
        // The new factor (id null) plus the carried-forward existing factor 5.
        List<ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.ExperimentalFactorEntry> factors =
                req.getProposedDesign().getExperimentalFactors();
        assertThat( factors ).hasSize( 2 );
        assertThat( factors ).anyMatch( f -> f.getId() == null && "genotype".equals( f.getName() ) );
        assertThat( factors ).anyMatch( f -> Long.valueOf( 5L ).equals( f.getId() ) );
        assertThat( req.getDesignPlan().getNewFactorClientRefs() ).containsExactly( "f1" );
    }

    @Test
    @WithMockUser
    public void testCommitCurationDesignDeletesByDeletedIds() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( currentDesign( 5L, 6L ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any() ) )
                .thenReturn( new ubic.gemma.model.expression.experiment.DesignPreflightReport() );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        String body = "{\"design\":{\"factors\":{\"items\":[],\"deletedIds\":[6]}}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        List<ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.ExperimentalFactorEntry> factors =
                cap.getValue().getProposedDesign().getExperimentalFactors();
        // Deleted factor 6 is omitted (deleted by absence); untouched factor 5 is carried forward.
        assertThat( factors ).hasSize( 1 );
        assertThat( factors.get( 0 ).getId() ).isEqualTo( 5L );
    }

    /** Current design with factor 5 → FV 10, and biomaterial 100 assigned to FV 10. */
    private ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject currentDesignWithAssignment() {
        ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject d =
                new ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject();
        d.setId( 3L );
        ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.ExperimentalFactorEntry f5 =
                new ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.ExperimentalFactorEntry();
        f5.setId( 5L );
        f5.setName( "factor5" );
        f5.setValues( new ArrayList<>( List.of( new ubic.gemma.model.expression.experiment.FactorValueBasicValueObject( 10L ) ) ) );
        d.setExperimentalFactors( new ArrayList<>( List.of( f5 ) ) );
        d.setBioMaterialAssignments( new ArrayList<>( List.of(
                new ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.BioMaterialFactorValueAssignment(
                        100L, "bm", new ArrayList<>( List.of( 10L ) ) ) ) ) );
        return d;
    }

    @Test
    @WithMockUser
    public void testCommitCurationDesignNullSamplesLeavesAssignmentUntouched() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( currentDesignWithAssignment() );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any() ) )
                .thenReturn( new ubic.gemma.model.expression.experiment.DesignPreflightReport() );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        // Edit FV 10's label but OMIT biomaterialShortNames (null → leave assignments untouched).
        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":5,\"name\":\"factor5\","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":10,\"freeTextLabel\":\"newlabel\"}]}}]}}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a =
                cap.getValue().getProposedDesign().getBioMaterialAssignments().stream()
                        .filter( x -> Long.valueOf( 100L ).equals( x.getBioMaterialId() ) )
                        .findFirst().orElseThrow( () -> new AssertionError( "assignment missing" ) );
        assertThat( a.getFactorValueIds() ).as( "null samples must not unassign" ).contains( 10L );
    }

    @Test
    @WithMockUser
    public void testCommitCurationDesignEmptySamplesClearsAssignment() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( currentDesignWithAssignment() );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any() ) )
                .thenReturn( new ubic.gemma.model.expression.experiment.DesignPreflightReport() );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        // Explicit empty biomaterialShortNames → clear FV 10's assignments.
        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":5,\"name\":\"factor5\","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":10,\"freeTextLabel\":\"x\",\"biomaterialShortNames\":[]}]}}]}}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a =
                cap.getValue().getProposedDesign().getBioMaterialAssignments().stream()
                        .filter( x -> Long.valueOf( 100L ).equals( x.getBioMaterialId() ) )
                        .findFirst().orElseThrow( () -> new AssertionError( "assignment missing" ) );
        assertThat( a.getFactorValueIds() ).as( "empty list clears assignment" ).doesNotContain( 10L );
    }

    @Test
    @WithMockUser
    public void testCommitCurationDesignForceGateIs409() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( currentDesign( 5L ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        ubic.gemma.model.expression.experiment.DesignPreflightReport report =
                new ubic.gemma.model.expression.experiment.DesignPreflightReport();
        report.getDifferentialExpressionAnalysesToDelete()
                .add( new ubic.gemma.model.expression.experiment.DesignPreflightReport.AnalysisRef( 1L, "dea", null ) );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any() ) ).thenReturn( report );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":5,\"name\":\"f\",\"category\":{\"label\":\"g\"}}]}}}";
        // No ?force and non-admin → 409, and nothing is committed.
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.CONFLICT );
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    @Test
    @WithMockUser
    public void testPreflightCurationDesignIsDryRun() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( currentDesign( 5L ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any() ) )
                .thenReturn( new ubic.gemma.model.expression.experiment.DesignPreflightReport() );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( true ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":5,\"name\":\"f\",\"category\":{\"label\":\"g\"}}]}}}";
        assertThat( target( "/datasets/1/curation/preflight" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        // Dry run: the service is invoked with dryRun=true and no real commit path runs.
        verify( expressionExperimentService ).commitCuration( eq( ee ), any(), eq( true ) );
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), eq( false ) );
    }

    @Test
    @WithMockUser
    public void testCommitCurationTags() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        String body = "{\"tags\":{\"items\":[{\"clientRef\":\"t1\",\"category\":{\"label\":\"disease\"},"
                + "\"value\":{\"label\":\"glioma\"}},{\"gemmaId\":42}],\"deletedIds\":[7]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest req = cap.getValue();
        assertThat( req.isTagsPresent() ).isTrue();
        assertThat( req.getTagsToAdd() ).hasSize( 1 );
        assertThat( req.getTagsToAdd().get( 0 ).getClientRef() ).isEqualTo( "t1" );
        assertThat( req.getTagsToAdd().get( 0 ).getCharacteristic().getValue() ).isEqualTo( "glioma" );
        assertThat( req.getTagsToDelete() ).containsExactly( 7L );
        assertThat( req.getTagsUnchanged() ).isEqualTo( 1 ); // the gemmaId item
    }

    @Test
    @WithMockUser
    public void testCommitCurationSampleCharacteristics() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        // one sample with a resolvable short name → biomaterial id 100
        ubic.gemma.model.expression.biomaterial.BioMaterial bm = ubic.gemma.model.expression.biomaterial.BioMaterial.Factory.newInstance();
        bm.setId( 100L );
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setShortName( "GSM1" );
        ba.setSampleUsed( bm );
        ee.getBioAssays().add( ba );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        String body = "{\"sampleCharacteristics\":{\"items\":[{\"clientRef\":\"s1\",\"bioassayShortName\":\"GSM1\","
                + "\"category\":{\"label\":\"genotype\"},\"value\":{\"label\":\"WT\"}}],\"deletedIds\":[9]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest req = cap.getValue();
        assertThat( req.isSampleCharsPresent() ).isTrue();
        assertThat( req.getSampleCharsToAdd() ).hasSize( 1 );
        assertThat( req.getSampleCharsToAdd().get( 0 ).getBioMaterialId() ).isEqualTo( 100L );
        assertThat( req.getSampleCharsToDelete() ).containsExactly( 9L );
    }

    @Test
    @WithMockUser
    public void testCommitCurationSampleCharacteristicsUnknownSampleIs400() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee ); // no bioassays → nothing resolves
        String body = "{\"sampleCharacteristics\":{\"items\":[{\"clientRef\":\"s1\",\"bioassayShortName\":\"GSM_MISSING\","
                + "\"value\":{\"label\":\"WT\"}}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    @Test
    @WithMockUser
    public void testCommitCurationDetailsNote() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );
        String body = "{\"curationDetails\":{\"curationNote\":\"reviewed 2026\"}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().isCurationDetailsPresent() ).isTrue();
        assertThat( cap.getValue().getCurationDetailsNote() ).isEqualTo( "reviewed 2026" );
    }

    @Test
    @WithMockUser
    public void testCommitCurationDetailsFlagsAre400() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"curationDetails\":{\"troubled\":true}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    @Test
    @WithMockUser
    public void testPreflightCurationIsDryRun() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( true ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );
        String body = "{\"basics\":{\"name\":\"x\"}}";
        assertThat( target( "/datasets/1/curation/preflight" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        verify( expressionExperimentService ).commitCuration( eq( ee ), any(), eq( true ) );
    }

    @Test
    @WithMockUser
    public void testCommitCurationStaleBaselineIs409() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenThrow( new org.springframework.dao.OptimisticLockingFailureException( "moved" ) );
        String body = "{\"basics\":{\"name\":\"x\"},\"baseline\":{\"lastModified\":\"123\"}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.CONFLICT );
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