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
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
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
import ubic.gemma.model.expression.experiment.StatementValueObject;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.PublicationAssociation;
import ubic.gemma.model.common.description.PublicationAssociationSource;
import ubic.gemma.persistence.service.common.description.PublicationAssertion;
import ubic.gemma.persistence.service.common.description.PublicationAssociationConflictException;
import ubic.gemma.persistence.service.common.description.PublicationAssociationService;
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
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.Writer;
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
        public DatasetArgService datasetArgService( ExpressionExperimentService expressionExperimentService, SearchService searchService,
                PublicationAssociationService publicationAssociationService, ArrayDesignService arrayDesignService,
                BioAssayService bioAssayService, OutlierDetectionService outlierDetectionService ) {
            // 🛑 Take the arrayDesignService and bioAssayService BEANS, not fresh mocks. They used to be
            // constructed here, so the instance tests autowire and stub was not the instance this service
            // called — a stub could look set up and be inert, which is a silent way for a test to assert
            // nothing. The BioAssayService bean below already existed while this constructed its own; the
            // samples route reads through this one, so a test stubbing the bean stubbed nothing.
            // The OutlierDetectionService is taken as a bean for the same reason: a test that verifies the
            // correlation matrix is NOT loaded has to hold the instance this service actually calls.
            return new DatasetArgService( expressionExperimentService, searchService, arrayDesignService, bioAssayService, outlierDetectionService,
                    publicationAssociationService );
        }

        @Bean
        public PublicationAssociationService publicationAssociationService() {
            return mock( PublicationAssociationService.class );
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
        public ubic.gemma.persistence.service.genome.gene.GeneService geneService() {
            return mock( ubic.gemma.persistence.service.genome.gene.GeneService.class );
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
        public TicketsWebService ticketsWebService( TicketService ticketService, UserManager userManager,
                UserReadService userReadService, ExpressionExperimentService expressionExperimentService ) {
            return new TicketsWebService( ticketService, userManager, userReadService, expressionExperimentService,
                    mock( ubic.gemma.persistence.service.expression.experiment.PreboardedExperimentService.class ) );
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

        /**
         * Triage and lock are mocked rather than wired: this context builds the web service over mocks, so a
         * new @Autowired collaborator on AnnotationSetsWebService or DatasetsWebService fails context init for
         * every test in the class until it is declared here. That is what happened when the triage and lock
         * REST routes landed -- 190 errors, all one missing bean.
         */
        @Bean
        public ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetTriageService annotationSetTriageService() {
            return mock( ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetTriageService.class );
        }

        @Bean
        public ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetDispositionService annotationSetDispositionService() {
            return mock( ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetDispositionService.class );
        }

        @Bean
        public ubic.gemma.persistence.service.common.auditAndSecurity.curation.CurationDecisionService curationDecisionService() {
            return mock( ubic.gemma.persistence.service.common.auditAndSecurity.curation.CurationDecisionService.class );
        }

        @Bean
        public ubic.gemma.persistence.service.common.auditAndSecurity.curation.CurationLockService curationLockService() {
            return mock( ubic.gemma.persistence.service.common.auditAndSecurity.curation.CurationLockService.class );
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
        public ubic.gemma.core.analysis.preprocess.qc.SequencingQcMetricsService sequencingQcMetricsService() {
            return mock( ubic.gemma.core.analysis.preprocess.qc.SequencingQcMetricsService.class );
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
    private ubic.gemma.persistence.service.genome.gene.GeneService geneService;

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
    private BioAssayService bioAssayService;

    @Autowired
    private OutlierDetectionService outlierDetectionService;

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

    @Autowired
    private ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetTriageService annotationSetTriageService;

    @Autowired
    private ubic.gemma.persistence.service.common.auditAndSecurity.curation.CurationLockService curationLockService;

    @Autowired
    private ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService annotationSetService;

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
        // Every curation commit now reads the dataset before it writes, to keep what the commit displaces as a
        // SNAPSHOT. An unstubbed thaw returns null and the whole commit path 500s on it, which reads as a bug in
        // whichever section the test was actually exercising. Stubbed here rather than per-test because it is no
        // longer a per-section concern: the snapshot is taken whatever the commit touches.
        when( expressionExperimentService.thawBioAssays( any() ) ).thenReturn( ee );
    }

    @AfterEach
    public void resetMocks() {
        reset( expressionExperimentService, quantitationTypeService, analyticsProvider, expressionDataFileService, taxonArgService, geneArgService, searchService, auditEventService, auditTrailService, securityService, geeqService, taskRunningService, differentialExpressionAnalysisService, userManager, ticketService, sampleCoexpressionAnalysisService, svdService, processedExpressionDataVectorService, expressionExperimentReportService, arrayDesignService, bibliographicReferenceService, ontologyTermValidator, curationLockService, annotationSetService, bioAssayService );
    }

    private static final String HALLUCINATED_TAG_BODY = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"t7\","
            + "\"value\":{\"label\":\"has_genotype\",\"uri\":\"http://purl.obolibrary.org/obo/TGEMO_00166\"}}]}}";

    /** A tag whose label doesn't match its URI is rejected with a structured, per-slot 400. */
    @Test
    public void testCommitRejectsUngroundedTerm() {
        // Two-arg form: the commit path calls validateAndCanonicalize(c, canonSink). Stubbing the
        // one-arg default instead leaves the real call unstubbed, so the mock returns no
        // violations, the reject gate never fires, and the request runs on to commitCuration.
        when( ontologyTermValidator.validateAndCanonicalize( any(), any() ) ).thenReturn( Collections.singletonList(
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
        when( ontologyTermValidator.validateAndCanonicalize( any(), any() ) ).thenAnswer( inv -> {
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

    /**
     * A {@code deletedIds} entry naming a statement that is not on that factor value is refused.
     * <p>
     * The delete is a suppression of the carry-forward, so an id that is not among the factor value's
     * current statements suppresses nothing and the commit answered 200 with {@code deleted: 0} — which
     * reads exactly like a delete that worked. A caller recorded eight such deletions against eid 6146 on
     * 2026-09-01; the ids were real {@code CHARACTERISTIC} rows on no factor value of that dataset.
     */
    @Test
    public void testCommitRefusesDeletedIdThatIsNotOnThatFactorValue() {
        when( expressionExperimentService.thawBioAssays( any() ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( any() ) )
                .thenReturn( designWithOneStatement( 10L, 20L, 30L ) );
        when( expressionExperimentService.previewDesignChange( any(), any() ) ).thenReturn( new DesignPreflightReport() );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":10,"
                + "\"factorValues\":{\"items\":[{\"gemmaId\":20,"
                + "\"statements\":{\"items\":[],\"deletedIds\":[999]}}]}}]}}}";
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) ).contains( "999" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /** The id that IS on the factor value is accepted, so the refusal is about membership, not about deleting. */
    @Test
    public void testCommitAcceptsDeletedIdThatIsOnThatFactorValue() {
        when( expressionExperimentService.thawBioAssays( any() ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( any() ) )
                .thenReturn( designWithOneStatement( 10L, 20L, 30L ) );
        when( expressionExperimentService.previewDesignChange( any(), any() ) ).thenReturn( new DesignPreflightReport() );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":10,"
                + "\"factorValues\":{\"items\":[{\"gemmaId\":20,"
                + "\"statements\":{\"items\":[],\"deletedIds\":[30]}}]}}]}}}";
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isNotEqualTo( 400 );
        }
    }

    /** A factor {@code deletedIds} naming a factor of another dataset is refused the same way. */
    @Test
    public void testCommitRefusesFactorDeletedIdThatIsNotOnThisDataset() {
        when( expressionExperimentService.thawBioAssays( any() ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( any() ) )
                .thenReturn( designWithOneStatement( 10L, 20L, 30L ) );
        when( expressionExperimentService.previewDesignChange( any(), any() ) ).thenReturn( new DesignPreflightReport() );

        String body = "{\"design\":{\"factors\":{\"items\":[],\"deletedIds\":[77]}}}";
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) ).contains( "77" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /**
     * The baseline-relevance hint is accepted on the write side and reaches the design mapper.
     * <p>
     * It was published on {@code ExperimentalFactor} and {@code ExperimentalFactorValueObject} and absent from
     * {@code FactorCommit}, so the curation UI's "Tick to override: no baseline" checkbox and its reason box had
     * nowhere to land: every preflight carrying them came back
     * {@code 400 Unrecognized field "baselineRelevance" … not marked as ignorable} — a readable, renderable,
     * unsettable field (cab, 2026-09-04, GSE32473 factor 13474).
     */
    @Test
    public void testPreflightAcceptsTheBaselineRelevanceHint() {
        when( expressionExperimentService.thawBioAssays( any() ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( any() ) )
                .thenReturn( designWithOneStatement( 10L, 20L, 30L ) );
        when( expressionExperimentService.previewDesignChange( any(), any() ) ).thenReturn( new DesignPreflightReport() );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":10,"
                + "\"baselineRelevance\":\"not_applicable\","
                + "\"baselineRelevanceReason\":\"one individual per group; no reference level\"}]}}}";
        try ( Response r = target( "/datasets/1/curation/preflight" ).request().post( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isNotEqualTo( 400 );
        }

        ArgumentCaptor<ExperimentalDesignValueObject> captor = ArgumentCaptor.forClass( ExperimentalDesignValueObject.class );
        verify( expressionExperimentService ).previewDesignChange( any(), captor.capture() );
        ExperimentalDesignValueObject.ExperimentalFactorEntry f = captor.getValue().getExperimentalFactors().stream()
                .filter( e -> Long.valueOf( 10L ).equals( e.getId() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "factor 10 did not reach the mapper" ) );
        assertThat( f.getBaselineRelevance() ).isEqualTo( "not_applicable" );
        assertThat( f.getBaselineRelevanceReason() ).isEqualTo( "one individual per group; no reference level" );
    }

    /**
     * A word outside the three in use round-trips instead of 400ing. The hint's vocabulary has moved once
     * already, and a closed {@code allowableValues} would make the next word a schema change and a deploy
     * (cab, 2026-09-04: "don't lock us into any kind of enums").
     */
    @Test
    public void testPreflightAcceptsAnUnfamiliarBaselineRelevanceValue() {
        when( expressionExperimentService.thawBioAssays( any() ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( any() ) )
                .thenReturn( designWithOneStatement( 10L, 20L, 30L ) );
        when( expressionExperimentService.previewDesignChange( any(), any() ) ).thenReturn( new DesignPreflightReport() );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":10,"
                + "\"baselineRelevance\":\"deferred_to_curator\"}]}}}";
        try ( Response r = target( "/datasets/1/curation/preflight" ).request().post( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isNotEqualTo( 400 );
        }
        ArgumentCaptor<ExperimentalDesignValueObject> captor = ArgumentCaptor.forClass( ExperimentalDesignValueObject.class );
        verify( expressionExperimentService ).previewDesignChange( any(), captor.capture() );
        assertThat( captor.getValue().getExperimentalFactors().get( 0 ).getBaselineRelevance() )
                .isEqualTo( "deferred_to_curator" );
    }

    /** One factor, one factor value, one statement — the current state the mapper carries forward from. */
    private static ExperimentalDesignValueObject designWithOneStatement( Long factorId, Long fvId, Long stmtId ) {
        StatementValueObject stmt = new StatementValueObject();
        stmt.setId( stmtId );
        stmt.setSubject( "astrocyte" );
        ubic.gemma.model.expression.experiment.FactorValueBasicValueObject fv =
                new ubic.gemma.model.expression.experiment.FactorValueBasicValueObject();
        fv.setId( fvId );
        fv.setStatements( Collections.singletonList( stmt ) );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = new ExperimentalDesignValueObject.ExperimentalFactorEntry();
        factor.setId( factorId );
        factor.setName( "cell type" );
        factor.setValues( Collections.singletonList( fv ) );
        ExperimentalDesignValueObject design = new ExperimentalDesignValueObject();
        design.setExperimentalFactors( Collections.singletonList( factor ) );
        return design;
    }

    /** Preflight enforces the same gate, so a client catches the failure on the dry run. */
    @Test
    public void testPreflightRejectsUngroundedTerm() {
        // Two-arg form: the commit path calls validateAndCanonicalize(c, canonSink). Stubbing the
        // one-arg default instead leaves the real call unstubbed, so the mock returns no
        // violations, the reject gate never fires, and the request runs on to commitCuration.
        when( ontologyTermValidator.validateAndCanonicalize( any(), any() ) ).thenReturn( Collections.singletonList(
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
                .containsExactly( "categoryUri", "category", "valueUri", "value" );
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
                .containsExactly( "categoryUri", "category", "valueUri", "value" );
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
                .containsExactly( "categoryUri", "category", "valueUri", "value" );
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
        // Everything the curator wrote, grounded or not. A caller cannot tell an incomplete list
        // from a complete one by inspecting it, so the complete one is the default.
        verify( expressionExperimentService ).getAnnotations( ee, true );
    }

    @Test
    public void testGetDatasetAnnotationsExcludingFreeText() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/annotations" ).queryParam( "includeFreeText", "false" ).request().get() )
                .hasStatus( Response.Status.OK );
        // The grounded-only view is still reachable, it is just no longer what you get by accident.
        verify( expressionExperimentService ).getAnnotations( ee, false );
    }

    /*
     * The single-tag add / remove verbs on /datasets/{id}/annotations. These were declared on
     * AnnotationsWebService under its class-level @Path("/annotations"), which put them at
     * /annotations/datasets/{id}/annotations and left POST /datasets/{id}/annotations answering
     * 405 Method Not Allowed on production (gemma2 build af1f519bf081) — Jersey picks the resource
     * class by path, and the datasets resource carried only GET and PUT. Asserting a real status
     * here is what pins the routing: a 405 is what a missing route looks like.
     */

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotationTag() {
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        when( expressionExperimentService.addAnnotation( eq( ee ), any( Characteristic.class ) ) )
                .thenAnswer( a -> {
                    Characteristic vc = a.getArgument( 1, Characteristic.class );
                    vc.setId( 42L );
                    return vc;
                } );
        String body = "{\"category\":\"organism part\",\"categoryUri\":\"http://purl.obolibrary.org/obo/UBERON_0000479\","
                + "\"value\":\"liver\",\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0002107\","
                + "\"evidenceCode\":\"IEA\"}";
        Response res = target( "/datasets/1/annotations" ).request().post( Entity.json( body ) );
        assertThat( res.getStatus() ).isNotEqualTo( Response.Status.METHOD_NOT_ALLOWED.getStatusCode() );
        assertThat( res )
                .hasStatus( Response.Status.CREATED )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        ArgumentCaptor<Characteristic> captor = ArgumentCaptor.forClass( Characteristic.class );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), captor.capture() );
        Characteristic sent = captor.getValue();
        assertThat( sent.getCategory() ).isEqualTo( "organism part" );
        assertThat( sent.getValue() ).isEqualTo( "liver" );
        assertThat( sent.getValueUri() ).isEqualTo( "http://purl.obolibrary.org/obo/UBERON_0002107" );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotationTagDuplicateReturnsConflict() {
        // The duplicate is what proves the request reached the service rather than being turned
        // away by the router: the service's IllegalArgumentException is what maps to 409.
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        when( expressionExperimentService.addAnnotation( eq( ee ), any( Characteristic.class ) ) )
                .thenThrow( new IllegalArgumentException( "duplicate" ) );
        String body = "{\"category\":\"organism part\",\"value\":\"liver\","
                + "\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0002107\"}";
        assertThat( target( "/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CONFLICT );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), any( Characteristic.class ) );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotationTagAcceptsAndDropsAnnotationSetId() {
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        when( expressionExperimentService.addAnnotation( eq( ee ), any( Characteristic.class ) ) )
                .thenAnswer( a -> a.getArgument( 1, Characteristic.class ) );
        String body = "{\"category\":\"organism part\",\"value\":\"liver\"}";
        assertThat( target( "/datasets/1/annotations" ).queryParam( "annotationSetId", "7" )
                .request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CREATED );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), any( Characteristic.class ) );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testRemoveDatasetAnnotationTag() {
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        Characteristic c = Characteristic.Factory.newInstance();
        c.setId( 42L );
        c.setCategory( "organism part" );
        c.setValue( "liver" );
        when( expressionExperimentService.removeAnnotation( ee, 42L ) ).thenReturn( c );
        Response res = target( "/datasets/1/annotations/42" ).request().delete();
        assertThat( res.getStatus() ).isNotEqualTo( Response.Status.METHOD_NOT_ALLOWED.getStatusCode() );
        assertThat( res ).hasStatus( Response.Status.NO_CONTENT );
        verify( expressionExperimentService ).removeAnnotation( ee, 42L );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testRemoveDatasetAnnotationTagNotFound() {
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        when( expressionExperimentService.removeAnnotation( ee, 999L ) ).thenReturn( null );
        assertThat( target( "/datasets/1/annotations/999" ).request().delete() )
                .hasStatus( Response.Status.NOT_FOUND );
        // An unrouted DELETE also answers 404, so the status alone cannot tell "no such annotation"
        // apart from "no such route". The service call is what separates them.
        verify( expressionExperimentService ).removeAnnotation( ee, 999L );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotations() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee, true ) ).thenReturn( Collections.emptySet() );
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
        // The write echoes unmapped tags too — it accepts them, so it must not answer with a list
        // that silently omits what was just written.
        verify( expressionExperimentService ).getAnnotations( ee, true );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetAnnotationsAcceptsEmptyListAsClear() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee, true ) ).thenReturn( Collections.emptySet() );
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
        when( expressionExperimentService.getAnnotations( ee, true ) ).thenReturn( Collections.emptySet() );
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
        when( expressionExperimentService.getAnnotations( ee, true ) ).thenReturn( Collections.emptySet() );
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
        when( expressionExperimentService.getAnnotations( ee, true ) ).thenReturn( Collections.emptySet() );
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
        when( expressionExperimentService.getAnnotations( ee, true ) ).thenReturn( Collections.emptySet() );
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
        when( expressionExperimentService.getAnnotations( ee, true ) ).thenReturn( Collections.emptySet() );
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

    /**
     * Cold cache: the tabular single-cell data is generated ONCE, streaming to the caller and
     * populating the cache file in the same pass — never by racing a fire-and-forget background
     * build against an in-band stream of the same data, which on this endpoint meant two
     * concurrent full scans of the largest payloads in the system.
     */
    @Test
    public void testGetDatasetSingleCellDataWhenCacheIsCold() throws Exception {
        ee.setShortName( "GSE1" ); // the cold path derives the cache filename from the EE short name...
        QuantitationType qt = new QuantitationType();
        qt.setName( "counts" ); // ...and the QT name
        when( singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee ) )
                .thenReturn( Optional.of( qt ) );
        when( expressionDataFileService.getDataFile( eq( ee ), eq( qt ), eq( ExpressionExperimentDataFileType.TABULAR ), anyBoolean(), anyLong(), any() ) )
                .thenReturn( new DummyLockedPath( Paths.get( "/nonexistent/sc-data.tsv.gz" ), true ) );
        doAnswer( a -> {
            Writer w = a.getArgument( 5 );
            w.write( "probe\tvalue\ncs1\t1.0\n" );
            return null;
        } ).when( expressionDataFileService ).streamAndWriteTabularSingleCellExpressionData(
                eq( ee ), eq( qt ), anyInt(), anyBoolean(), anyBoolean(), any(), anyBoolean() );

        assertThat( target( "/datasets/1/data/singleCell" ).request()
                .accept( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE ).get() )
                .hasStatus( Response.Status.OK )
                .hasMediaType( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE );

        verify( expressionDataFileService ).streamAndWriteTabularSingleCellExpressionData(
                eq( ee ), eq( qt ), anyInt(), anyBoolean(), eq( false ), any(), anyBoolean() );
        // the point: no duplicate background build alongside the stream
        verify( expressionDataFileService, never() ).writeOrLocateTabularSingleCellExpressionDataAsync(
                any(), any(), anyInt(), anyBoolean(), anyBoolean() );
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

    /**
     * A sample assay carrying one bare statement, for the payload-shape tests below.
     */
    private BioAssayValueObject sampleAssay() {
        BioMaterialValueObject sample = new BioMaterialValueObject( 10L );
        sample.getStatements().add( new StatementValueObject() );
        BioAssayValueObject assay = new BioAssayValueObject( 3000L );
        assay.setSample( sample );
        return assay;
    }

    /**
     * 🛑 The route was not compressed, and that — not any single field — was the size of the problem.
     * <p>
     * {@code GET /datasets/3937/samples} sent 5,381,688 bytes for 278 samples with no
     * {@code Content-Encoding}, and the same body gzips to 144,390 — a 37x reduction with no client
     * change and no field removed, larger than every trim in this commit put together. Compression here
     * is opt-in per endpoint via {@code @GZIP}, so a heavy new route is uncompressed by default and
     * nothing says so; the annotation is the whole gate. Measured on production, {@code b5c6747f68}.
     */
    @Test
    public void testGetDatasetSamplesIsCompressed() {
        when( bioAssayService.loadValueObjects( any(), any(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.singletonList( sampleAssay() ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/samples" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" );
    }

    /**
     * {@code sample.statements} is 21.5% of the samples response and carries the same rows as
     * {@code sample.characteristics} plus a predicate and object, so a client that renders only
     * subjects can decline it. Excluded means absent, not empty — see
     * {@link BioMaterialValueObject#getStatements()}.
     */
    @Test
    public void testGetDatasetSamplesCanExcludeStatements() {
        when( bioAssayService.loadValueObjects( any(), any(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.singletonList( sampleAssay() ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );

        assertThat( target( "/datasets/1/samples" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entityAsString().asInstanceOf( json() )
                .hasPath( "$.data[0].sample.statements" );

        assertThat( target( "/datasets/1/samples" ).queryParam( "exclude", "sample.statements" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entityAsString().asInstanceOf( json() )
                .doesNotHavePath( "$.data[0].sample.statements" );
    }

    /**
     * 🛑 {@code limit} was bound by the route and then used only on the cursor branch, so a client asking
     * for a page got the whole dataset instead, with nothing in the body saying so.
     * <p>
     * Measured on production ({@code gemma2}, dataset 7332 = GSE2109, 2158 samples):
     * {@code ?limit=20} returned all 2158 assays, byte-identical to the no-parameter call. The legacy
     * response has no {@code totalElements} and no {@code nextCursor}, so silently truncating to 20 would
     * lose 2138 rows just as invisibly; the parameter is refused instead. {@code offset} on this route was
     * already a 400 — it is not declared at all, so {@code UnknownQueryParameterFilter} catches it. That
     * filter cannot see this case, because {@code limit} *is* declared; the route binds it and then has
     * nothing to do with it.
     */
    @Test
    public void testGetDatasetSamplesRejectsALimitItCannotHonour() {
        // Stub the listing so the refusal is a decision, not a missing fixture.
        when( bioAssayService.loadValueObjects( any(), any(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.singletonList( sampleAssay() ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        // The mocks are context-scoped singletons; only this test's calls are the subject here.
        clearInvocations( bioAssayService );

        assertThat( target( "/datasets/1/samples" ).queryParam( "limit", "20" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );

        // The sibling subset listing has the same shape and answers the same way.
        assertThat( target( "/datasets/1/subSets/1/samples" ).queryParam( "limit", "20" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );

        // The listing itself must not have run — a 400 that still paid for the query would be pointless.
        verify( bioAssayService, never() ).loadValueObjects( any(), any(), anyBoolean(), anyBoolean() );
    }

    /** An exclusion the route does not offer is a 400, not a silently ignored parameter. */
    @Test
    public void testGetDatasetSamplesRejectsAnUnsupportedExclusion() {
        when( bioAssayService.loadValueObjects( any(), any(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.singletonList( sampleAssay() ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/samples" ).queryParam( "exclude", "sample.characteristics" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    /**
     * The predicted-outlier flag costs the dataset's whole N&times;N sample-correlation matrix to compute,
     * which is why it is opt-in: the cost is set by the correlation analysis, not the page size, and on
     * the largest datasets it exceeds the request timeout. The assertion is that the detection service is
     * not consulted at all by default — a cheaper-but-still-called path would still load the matrix.
     */
    @Test
    public void testGetDatasetSamplesDoesNotComputePredictedOutliersByDefault() {
        when( bioAssayService.loadValueObjects( any(), any(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.singletonList( sampleAssay() ) );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        // The mock is a context-scoped singleton, so /sample-correlation's own test leaves invocations
        // on it. Only this test's calls are the subject here.
        clearInvocations( outlierDetectionService );

        // Absent, not false: a client must be able to tell "not computed" from "the algorithm ran and
        // cleared this assay". Serializing false for both is the shape that misleads.
        assertThat( target( "/datasets/1/samples" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entityAsString().asInstanceOf( json() )
                .doesNotHavePath( "$.data[0].predictedOutlier" );
        verify( outlierDetectionService, never() ).getOutlierDetails( any() );

        when( outlierDetectionService.getOutlierDetails( ee ) ).thenReturn( Optional.of( Collections.emptyList() ) );
        assertThat( target( "/datasets/1/samples" ).queryParam( "includePredictedOutliers", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entityAsString().asInstanceOf( json() )
                .hasPath( "$.data[0].predictedOutlier" );
        verify( outlierDetectionService ).getOutlierDetails( ee );
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

    /**
     * {@code curationPending} is the curation lock's unexpired lease and nothing else: it must read true while a
     * lock is held, and the response must still name nobody. The holder is served by
     * {@code /datasets/{id}/curation/lock}, which is authenticated; this field is readable by anyone who can read
     * the dataset.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetCurationDetailsReportsCurationPendingWhileLocked() {
        ee.setCurationDetails( new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails() );
        ubic.gemma.model.common.auditAndSecurity.curation.CurationLock lock =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationLock();
        lock.setLockedBy( "curator-jane" );
        lock.setExpiresAt( new Date( System.currentTimeMillis() + 600000L ) );
        when( curationLockService.current( any( ubic.gemma.model.analysis.Investigation.class ) ) )
                .thenReturn( Optional.of( lock ) );

        try ( Response r = target( "/datasets/1/curationDetails" ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            String body = r.readEntity( String.class );
            assertThat( body ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data.curationPending", true )
                    .doesNotHavePath( "$.data.lockedBy" )
                    .doesNotHavePath( "$.data.runId" )
                    .doesNotHavePath( "$.data.agentName" )
                    .doesNotHavePath( "$.data.expiresAt" );
            // the holder's identity must not reach this response by any spelling
            assertThat( body ).doesNotContain( "curator-jane" );
        }
    }

    /* ---- PATCH /datasets/{id}/quantitationTypes/{qtId} ---- */

    private QuantitationType linearRmaQt() {
        QuantitationType qt = new QuantitationType();
        qt.setId( 77L );
        qt.setName( "rma value" );
        qt.setGeneralType( ubic.gemma.model.common.quantitationtype.GeneralType.QUANTITATIVE );
        qt.setType( ubic.gemma.model.common.quantitationtype.StandardQuantitationType.AMOUNT );
        qt.setScale( ubic.gemma.model.common.quantitationtype.ScaleType.LINEAR );
        qt.setRepresentation( ubic.gemma.model.common.quantitationtype.PrimitiveType.DOUBLE );
        qt.setIsRatio( false );
        return qt;
    }

    /**
     * The case this was expanded for: a pre-2018 Affymetrix `rma value` recorded as LINEAR when RMA output is
     * log2. The correction changes the record, not the numbers, so it must reach
     * {@code quantitationTypeService.update} and say what moved.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPatchQuantitationTypeCorrectsTheScale() {
        QuantitationType qt = linearRmaQt();
        when( quantitationTypeService.loadById( 77L, ee ) ).thenReturn( qt );

        assertThat( target( "/datasets/1/quantitationTypes/77" ).request()
                .method( "PATCH", Entity.json( "{\"scale\":\"LOG2\"}" ) ) )
                .hasStatus( Response.Status.OK );

        assertThat( qt.getScale() ).isEqualTo( ubic.gemma.model.common.quantitationtype.ScaleType.LOG2 );
        verify( quantitationTypeService ).update( qt );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), contains( "scale LINEAR -> LOG2" ) );
    }

    /**
     * Preference did not change, so no preferred-data event may be emitted for it. Routing a descriptive patch
     * through updateQuantitationType would emit one, because that path reads the preferred flag and cannot tell
     * "already preferred" from "just became preferred".
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPatchQuantitationTypeDoesNotTouchPreference() {
        QuantitationType qt = linearRmaQt();
        when( quantitationTypeService.loadById( 77L, ee ) ).thenReturn( qt );

        assertThat( target( "/datasets/1/quantitationTypes/77" ).request()
                .method( "PATCH", Entity.json( "{\"scale\":\"LOG2\"}" ) ) )
                .hasStatus( Response.Status.OK );

        verify( expressionExperimentService, never() ).updateQuantitationType( any(), any(), any() );
    }

    /**
     * representation describes the stored values rather than how to read them, so patching it would misdescribe
     * the vectors. Refused with a reason rather than silently ignored.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPatchQuantitationTypeRefusesRepresentation() {
        assertThat( target( "/datasets/1/quantitationTypes/77" ).request()
                .method( "PATCH", Entity.json( "{\"representation\":\"INT\"}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( quantitationTypeService, never() ).update( any( QuantitationType.class ) );
        verify( auditTrailService, never() ).addUpdateEvent( any(), anyString() );
    }

    /**
     * A patch that asks for what the record already says changes nothing, so it is a bad request rather than a
     * 200 with an audit event nobody can interpret.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPatchQuantitationTypeRejectsANoOp() {
        QuantitationType qt = linearRmaQt();
        when( quantitationTypeService.loadById( 77L, ee ) ).thenReturn( qt );

        assertThat( target( "/datasets/1/quantitationTypes/77" ).request()
                .method( "PATCH", Entity.json( "{\"scale\":\"LINEAR\"}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( quantitationTypeService, never() ).update( any( QuantitationType.class ) );
        verify( auditTrailService, never() ).addUpdateEvent( any(), anyString() );
    }

    /**
     * A non-administrator does not learn that curation is under way. The lock is consulted either way -- what
     * changes is whether the answer is kept -- so the field reads null rather than false, which would assert
     * something untrue. Null is how curationNote already behaves for a non-administrator.
     */
    @Test
    @WithMockUser
    public void testGetDatasetCurationDetailsHidesCurationPendingFromNonAdmins() {
        ee.setCurationDetails( new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails() );
        ubic.gemma.model.common.auditAndSecurity.curation.CurationLock lock =
                new ubic.gemma.model.common.auditAndSecurity.curation.CurationLock();
        lock.setLockedBy( "curator-jane" );
        lock.setExpiresAt( new Date( System.currentTimeMillis() + 600000L ) );
        when( curationLockService.current( any( ubic.gemma.model.analysis.Investigation.class ) ) )
                .thenReturn( Optional.of( lock ) );

        try ( Response r = target( "/datasets/1/curationDetails" ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            String body = r.readEntity( String.class );
            assertThat( body ).asInstanceOf( json() ).hasPathWithValue( "$.data.curationPending", null );
            assertThat( body ).doesNotContain( "curator-jane" );
        }
    }

    /**
     * A free dataset reads false, not null: the GET always consults the lock, so the reader can tell "nobody is
     * curating" from "this path did not look".
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetCurationDetailsReportsNoCurationPendingWhenUnlocked() {
        ee.setCurationDetails( new ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails() );
        when( curationLockService.current( any( ubic.gemma.model.analysis.Investigation.class ) ) )
                .thenReturn( Optional.empty() );

        try ( Response r = target( "/datasets/1/curationDetails" ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data.curationPending", false );
        }
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
        // Private before the flip (guard reads false), public after (response VO reads true).
        when( securityService.isPublic( ee ) ).thenReturn( false, true );
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
        // MakePublicEvent is emitted by SecurityServiceImpl.makePublic, which is mocked here, so there is
        // nothing to assert about it at this layer. That the event is written -- once, on the transition
        // only -- is covered by SecurityServiceTest.makePublicRecordsTheTransitionOnceAndNotAgain, which
        // runs against a real context and reads the trail back.
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetPermissionsMakePublicOnAlreadyPublicRecordsNoEvent() {
        // Already public: guard reads true, no flip, no event -- keeps the audit trail honest.
        when( securityService.isPublic( ee ) ).thenReturn( true );
        when( securityService.isShared( ee ) ).thenReturn( false );

        DatasetsWebService.PermissionsUpdateRequest body = new DatasetsWebService.PermissionsUpdateRequest();
        body.setIsPublic( true );

        assertThat( target( "/datasets/1/permissions" ).request().put( jakarta.ws.rs.client.Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.isPublic", true );

        verify( securityService, never() ).makePublic( ee );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testUpdateDatasetPermissionsMakesPrivate() {
        // Public before the flip (guard reads true), private after (response VO reads false).
        when( securityService.isPublic( ee ) ).thenReturn( true, false );
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
        verify( auditTrailService ).addUpdateEvent( eq( ee ),
                eq( ubic.gemma.model.common.auditAndSecurity.eventType.MakePrivateEvent.class ), anyString() );
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
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testMakeDatasetPublicFlipsTheAcl() {
        // Private before the flip (guard reads false), public after (response VO reads true).
        when( securityService.isPublic( ee ) ).thenReturn( false, true );
        when( securityService.isShared( ee ) ).thenReturn( false );

        assertThat( target( "/datasets/1/makePublic" ).request().post( null ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.isPublic", true );

        verify( securityService ).makePublic( ee );
        // See testUpdateDatasetPermissionsMakesPublic: the event comes from the mocked SecurityService.
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testMakeDatasetPublicOnAlreadyPublicIsNoOpNoEvent() {
        when( securityService.isPublic( ee ) ).thenReturn( true );
        when( securityService.isShared( ee ) ).thenReturn( false );

        assertThat( target( "/datasets/1/makePublic" ).request().post( null ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.isPublic", true );

        verify( securityService, never() ).makePublic( ee );
        verifyNoInteractions( auditTrailService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testMakeDatasetPrivateRecordsMakePrivateEvent() {
        // Public before the flip (guard reads true), private after (response VO reads false).
        when( securityService.isPublic( ee ) ).thenReturn( true, false );
        when( securityService.isShared( ee ) ).thenReturn( false );

        assertThat( target( "/datasets/1/makePrivate" ).request().post( null ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.isPublic", false );

        verify( securityService ).makePrivate( ee );
        verify( auditTrailService ).addUpdateEvent( eq( ee ),
                eq( ubic.gemma.model.common.auditAndSecurity.eventType.MakePrivateEvent.class ), anyString() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testMakeDatasetPrivateOnAlreadyPrivateIsNoOpNoEvent() {
        when( securityService.isPublic( ee ) ).thenReturn( false );
        when( securityService.isShared( ee ) ).thenReturn( false );

        assertThat( target( "/datasets/1/makePrivate" ).request().post( null ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.isPublic", false );

        verify( securityService, never() ).makePrivate( ee );
        verifyNoInteractions( auditTrailService );
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
                .hasFieldOrPropertyWithValue( "data.datasetId", 1 )
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
                    org.assertj.core.api.Assertions.assertThat( pp.get( "eventType" ) ).isEqualTo( "ProcessedVectorComputationEvent" );
                } );
    }

    /**
     * The sample-correlation step carries the filter attrition recorded when the matrix was computed.
     * <p>
     * The JSON is produced the way {@code AuditedAspect} produces it -- through the polymorphic
     * {@code AuditEventPayload} type, so the {@code @type} discriminator is present. That is the half that
     * breaks silently: a reader whose mapper has not been told about the subtype cannot resolve the type id,
     * and the endpoint would answer with the step present and the attrition quietly absent.
     */
    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusCarriesFilterAttrition() throws Exception {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );

        ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload payload =
                new ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload(
                        new ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload.FilterConfig(
                                true, false, true, true, 0.2, 1.0, 0.5, 0.5, 0.3, 7 ),
                        java.util.Arrays.asList(
                                new ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload.FilterStage(
                                        "noSequences", true, 900, null ),
                                new ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload.FilterStage(
                                        "outliers", false, 900, 12 ) ),
                        1000, 12, 850, 12 );
        com.fasterxml.jackson.databind.ObjectMapper aspectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = aspectMapper.writeValueAsString( ( ubic.gemma.core.security.audit.AuditEventPayload ) payload );
        org.assertj.core.api.Assertions.assertThat( json ).contains( "@type" );

        AuditEvent event = AuditEvent.Factory.newInstance( new Date( 1_700_000_000_000L ), AuditAction.UPDATE, "ok", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent(), json );
        stubLastEvents( Collections.singletonMap(
                ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent.class, event ) );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    Map<String, Object> sc = findStep( steps, "sampleCorrelation" );
                    org.assertj.core.api.Assertions.assertThat( sc.get( "status" ) ).isEqualTo( "ok" );
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attrition = ( Map<String, Object> ) sc.get( "filterAttrition" );
                    org.assertj.core.api.Assertions.assertThat( attrition )
                            .as( "the recorded attrition, parsed back out of the audit payload" )
                            .isNotNull()
                            .containsEntry( "startingRows", 1000 )
                            .containsEntry( "finalRows", 850 )
                            .containsEntry( "finalColumns", 12 );
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = ( Map<String, Object> ) attrition.get( "config" );
                    org.assertj.core.api.Assertions.assertThat( config )
                            .as( "the settings the counts are only interpretable against" )
                            .isNotNull()
                            .containsEntry( "requireSequences", true )
                            .containsEntry( "maskOutliers", false );
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> stages = ( List<Map<String, Object>> ) attrition.get( "stages" );
                    org.assertj.core.api.Assertions.assertThat( stages ).hasSize( 2 );
                    org.assertj.core.api.Assertions.assertThat( stages.get( 0 ) )
                            .containsEntry( "filter", "noSequences" )
                            .containsEntry( "applied", true )
                            .containsEntry( "rowsAfter", 900 );
                    org.assertj.core.api.Assertions.assertThat( stages.get( 1 ) )
                            .as( "a skipped stage still reports its row count, so the funnel reads continuously" )
                            .containsEntry( "applied", false )
                            .containsEntry( "columnsAfter", 12 );
                } );
    }

    /**
     * The preprocess step carries what the processed-vector creation did to the data. This payload has been
     * written since the Phase C audit migration and nothing served it, which is why the diagnostics footer had
     * nothing to show for "normalization".
     * <p>
     * It also guards the reader against a trap the single-payload version had: the status read walks the latest
     * event of every step, so a mapper registered for only one payload record cannot resolve any of the others
     * and turns each into a parse failure -- silently, since the step still reports.
     */
    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusCarriesProcessedVectorDetails() throws Exception {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );

        ubic.gemma.core.security.audit.payload.ProcessedVectorComputationPayload payload =
                new ubic.gemma.core.security.audit.payload.ProcessedVectorComputationPayload(
                        "Counts", "log2cpm", 4, 0, true, null );
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString( ( ubic.gemma.core.security.audit.AuditEventPayload ) payload );

        AuditEvent event = AuditEvent.Factory.newInstance( new Date( 1_700_000_000_000L ), AuditAction.UPDATE, "ok", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent(), json );
        stubLastEvents( Collections.singletonMap(
                ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent.class, event ) );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    Map<String, Object> pp = findStep( steps, "preprocess" );
                    org.assertj.core.api.Assertions.assertThat( pp.get( "status" ) ).isEqualTo( "ok" );
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pv = ( Map<String, Object> ) pp.get( "processedVectors" );
                    org.assertj.core.api.Assertions.assertThat( pv )
                            .as( "what preprocessing did to the data, read back out of the audit payload" )
                            .isNotNull()
                            .containsEntry( "rawQuantitationType", "Counts" )
                            .containsEntry( "processedQuantitationType", "log2cpm" )
                            .containsEntry( "numberOfMaskedMissingValues", 4 )
                            .containsEntry( "quantileNormalized", true );
                    org.assertj.core.api.Assertions.assertThat( pp.get( "filterAttrition" ) )
                            .as( "a payload of another type must not be reported as filter attrition" )
                            .isNull();
                } );
    }

    /**
     * Every correlation matrix computed before the payload existed carries no payload at all. That reads as
     * absent, not as an error and not as "nothing was filtered" -- and the rest of the step still reports.
     */
    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusWithoutFilterAttritionStillReportsTheStep() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        AuditEvent event = AuditEvent.Factory.newInstance( new Date( 1_700_000_000_000L ), AuditAction.UPDATE, "ok", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent() );
        stubLastEvents( Collections.singletonMap(
                ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent.class, event ) );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    Map<String, Object> sc = findStep( steps, "sampleCorrelation" );
                    org.assertj.core.api.Assertions.assertThat( sc.get( "status" ) ).isEqualTo( "ok" );
                    org.assertj.core.api.Assertions.assertThat( sc.get( "eventType" ) )
                            .isEqualTo( "SampleCorrelationAnalysisEvent" );
                    org.assertj.core.api.Assertions.assertThat( sc.get( "filterAttrition" ) ).isNull();
                } );
    }

    /**
     * A payload that does not parse must not take the status read down with it.
     */
    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusSurvivesAnUnparseableAuditPayload() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        AuditEvent event = AuditEvent.Factory.newInstance( new Date( 1_700_000_000_000L ), AuditAction.UPDATE, "ok", null, null,
                new ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent(), "{not json" );
        stubLastEvents( Collections.singletonMap(
                ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent.class, event ) );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    Map<String, Object> sc = findStep( steps, "sampleCorrelation" );
                    org.assertj.core.api.Assertions.assertThat( sc.get( "status" ) ).isEqualTo( "ok" );
                    org.assertj.core.api.Assertions.assertThat( sc.get( "filterAttrition" ) ).isNull();
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
                    org.assertj.core.api.Assertions.assertThat( pca.get( "eventType" ) ).isEqualTo( "FailedPCAAnalysisEvent" );
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
    /**
     * A DEA that succeeded and then had the design change under it is still there, and its own event still
     * says it succeeded — but it no longer describes the design it was computed from. `stale` is that state.
     * <p>
     * 🛑 Not `notRun`: the analysis was not deleted. A design change that invalidates an analysis deletes it,
     * and the step then reads `notRun` with nothing left to describe. This is the surviving case.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPipelineStatusDeaGoesStaleWhenTheDesignChangedAfterIt() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent() ) );
        stubLastEvents( latest );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> org.assertj.core.api.Assertions
                        .assertThat( findStep( steps, "dea" ).get( "status" ) ).isEqualTo( "stale" ) );
    }

    /** A design change BEFORE the analysis is the normal order: the DEA already reflects it. */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPipelineStatusDeaStaysOkWhenTheDesignChangedBeforeIt() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent() ) );
        stubLastEvents( latest );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> org.assertj.core.api.Assertions
                        .assertThat( findStep( steps, "dea" ).get( "status" ) ).isEqualTo( "ok" ) );
    }

    /**
     * Every step but batchInfo is computed from the samples and the design, so a design change makes all of
     * them stale -- not just the DEA. Paul, 2026-08-27: *"everything you mention needs to be redone if
     * sample-sets and experimental designs are changed, but we want to do it later"*. This test used to
     * assert the opposite; DesignChangeEvent is emitted only for a real change (the no-op branch suppresses
     * it), so a relabel does not reach here.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPipelineStatusEveryDataDerivedStepGoesStaleOnADesignChange() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent() ) );
        stubLastEvents( latest );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "pca" ).get( "status" ) ).isEqualTo( "stale" );
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "preprocess" ).get( "status" ) ).isEqualTo( "stale" );
                } );
    }

    /**
     * Flagging an outlier changes the analyzed sample set and no longer reprocesses the dataset inline, so
     * the SampleRemovalEvent it records is what marks the computed results as owed a re-run.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPipelineStatusGoesStaleWhenAnOutlierWasFlaggedAfterTheRun() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent() ) );
        stubLastEvents( latest );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> {
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "pca" ).get( "status" ) ).isEqualTo( "stale" );
                    org.assertj.core.api.Assertions.assertThat( findStep( steps, "dea" ).get( "status" ) ).isEqualTo( "stale" );
                } );
    }

    /**
     * batchInfo comes from scan dates and file headers. Neither a design edit nor an outlier flag touches
     * those, so it must not be swept up when the rule widened to every other step.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPipelineStatusBatchInfoIsNeverStale() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        // the descriptor asks for the abstract parent, so the stub has to be keyed by it
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent() ) );
        stubLastEvents( latest );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> org.assertj.core.api.Assertions
                        .assertThat( findStep( steps, "batchInfo" ).get( "status" ) ).isEqualTo( "ok" ) );
    }

    /**
     * A DEA that FAILED and was then followed by a design change stays `failed`. Re-running is the move
     * either way, and `stale` would hide that the last attempt did not succeed.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testPipelineStatusAFailedDeaStaysFailedAfterADesignChange() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent() ) );
        stubLastEvents( latest );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data.steps", list( Map.class ) )
                .satisfies( steps -> org.assertj.core.api.Assertions
                        .assertThat( findStep( steps, "dea" ).get( "status" ) ).isEqualTo( "failed" ) );
    }

    /**
     * The corpus-wide read of the same {@code stale} rule the per-dataset route applies: which datasets owe
     * pipeline work, and which steps. A PCA that succeeded and then had the design change under it is the
     * canonical row.
     */
    @Test
    @WithMockUser
    public void testStaleStepsListsADatasetWhoseRunPredatesTheDesignChange() {
        stubStaleScanCandidate();
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent() ) );
        stubLastEventsForStaleScan( latest );

        assertThat( target( "/datasets/staleSteps" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .satisfies( rows -> {
                    org.assertj.core.api.Assertions.assertThat( rows ).hasSize( 1 );
                    @SuppressWarnings("unchecked")
                    Map<String, Object> row = ( Map<String, Object> ) rows.get( 0 );
                    org.assertj.core.api.Assertions.assertThat( ( ( Number ) row.get( "datasetId" ) ).longValue() ).isEqualTo( 1L );
                    org.assertj.core.api.Assertions.assertThat( row.get( "shortName" ) ).isEqualTo( "GSE0001" );
                    org.assertj.core.api.Assertions.assertThat( row.get( "invalidatedBy" ) ).isEqualTo( "DesignChangeEvent" );
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> staleSteps = ( List<Map<String, Object>> ) row.get( "staleSteps" );
                    org.assertj.core.api.Assertions.assertThat( staleSteps )
                            .isNotEmpty()
                            .allSatisfy( step -> org.assertj.core.api.Assertions.assertThat( step.get( "status" ) ).isEqualTo( "stale" ) )
                            .extracting( step -> step.get( "step" ) )
                            .contains( "pca" )
                            .doesNotContain( "batchInfo" );
                } );
    }

    /**
     * The disconfirming half: the same two events in the other order. The design change happened BEFORE the
     * run, so the run already reflects it and nothing is owed. Without this the route could report every
     * dataset that has ever had a design change and still look right.
     */
    @Test
    @WithMockUser
    public void testStaleStepsOmitsADatasetWhoseRunPostdatesTheDesignChange() {
        stubStaleScanCandidate();
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> latest = new LinkedHashMap<>();
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent.class,
                AuditEvent.Factory.newInstance( new Date( 1_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.DesignChangeEvent() ) );
        latest.put( ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent.class,
                AuditEvent.Factory.newInstance( new Date( 2_000_000_000_000L ), AuditAction.UPDATE, null, null, null,
                        new ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent() ) );
        stubLastEventsForStaleScan( latest );

        assertThat( target( "/datasets/staleSteps" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .satisfies( rows -> org.assertj.core.api.Assertions.assertThat( rows ).isEmpty() );
    }

    /**
     * The candidate ids come from an audit query that has no ACL clause, so what keeps a private dataset out
     * of the response is the ACL-filtered load it is passed through. Here the load returns nothing for the
     * candidate: the row must not appear, and the audit fan-out must not even be attempted for it.
     */
    @Test
    @WithMockUser
    public void testStaleStepsDropsACandidateTheCallerCannotRead() {
        ee.setId( 1L );
        when( auditEventService.getIdsHavingEvent( eq( ExpressionExperiment.class ), anyCollection() ) )
                .thenReturn( new LinkedHashSet<>( Collections.singletonList( 1L ) ) );
        when( expressionExperimentService.load( any( Filters.class ), any() ) )
                .thenReturn( Collections.emptyList() );

        assertThat( target( "/datasets/staleSteps" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .satisfies( rows -> org.assertj.core.api.Assertions.assertThat( rows ).isEmpty() );
        verify( auditEventService, never() ).getLastEvents( anyCollection(), anySet() );
    }

    /** One candidate dataset the caller can read, ready for {@link #stubLastEventsForStaleScan(Map)}. */
    private void stubStaleScanCandidate() {
        ee.setId( 1L );
        ee.setShortName( "GSE0001" );
        when( auditEventService.getIdsHavingEvent( eq( ExpressionExperiment.class ), anyCollection() ) )
                .thenReturn( new LinkedHashSet<>( Collections.singletonList( 1L ) ) );
        when( expressionExperimentService.load( any( Filters.class ), any() ) )
                .thenReturn( Collections.singletonList( ee ) );
    }

    /**
     * Sibling of {@link #stubLastEvents(Map)} for the corpus scan, which hands the batched call a List of the
     * datasets its ACL-filtered load returned rather than a singleton Set.
     */
    private void stubLastEventsForStaleScan( Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> eventsByType ) {
        Map<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, Map<ExpressionExperiment, AuditEvent>> result = new LinkedHashMap<>();
        for ( Map.Entry<Class<? extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType>, AuditEvent> e : eventsByType.entrySet() ) {
            result.put( e.getKey(), Collections.singletonMap( ee, e.getValue() ) );
        }
        // eq() on the exact list, not anyCollection(): it pins the generic to ExpressionExperiment for
        // thenReturn, and it asserts the route hands the batched call the list its ACL-filtered load returned.
        when( auditEventService.getLastEvents( eq( Collections.singletonList( ee ) ), anySet() ) )
                .thenReturn( result );
    }

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
                .hasFieldOrPropertyWithValue( "data.hasBatchInformation", true )
                .hasFieldOrPropertyWithValue( "data.needsAttention", true )
                .hasFieldOrPropertyWithValue( "data.isPublic", true );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetDatasetPipelineStatusAdminSeesCurationNote() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        ee.getCurationDetails().setCurationNote( "admin only" );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.curationNote", "admin only" );
    }

    @Test
    @WithMockUser
    public void testGetDatasetPipelineStatusNonAdminDoesNotSeeCurationNote() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        ee.getCurationDetails().setCurationNote( "hidden" );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.curationNote", null );
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
        when( sampleCoexpressionAnalysisService.loadRegressedMatrix( ee ) ).thenReturn( matrix );
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
        verify( sampleCoexpressionAnalysisService ).loadRegressedMatrix( ee );
    }

    @Test
    public void testGetDatasetSampleCorrelationWhenNoneIs404() {
        when( sampleCoexpressionAnalysisService.loadRegressedMatrix( ee ) ).thenReturn( null );
        when( sampleCoexpressionAnalysisService.loadFullMatrix( ee ) ).thenReturn( null );
        assertThat( target( "/datasets/1/sample-correlation" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( sampleCoexpressionAnalysisService ).loadRegressedMatrix( ee );
    }

    /**
     * Three decimals, and a masked cell stays masked. {@code Math.round} on NaN yields 0.0, which would read
     * as "these two samples do not correlate" — the opposite of "we do not know".
     */
    @Test
    public void testSampleCorrelationValuesAreRoundedAndKeepNaN() {
        BioAssay a1 = BioAssay.Factory.newInstance( "BA1" );
        a1.setId( 100L );
        BioAssay a2 = BioAssay.Factory.newInstance( "BA2" );
        a2.setId( 101L );
        List<BioAssay> assays = Arrays.asList( a1, a2 );
        DenseDoubleMatrix<BioAssay, BioAssay> matrix = new DenseDoubleMatrix<>( new double[][] {
                { 1.0, 0.8231947345733643 },
                { 0.8231947345733643, Double.NaN }
        } );
        matrix.setRowNames( assays );
        matrix.setColumnNames( assays );
        when( sampleCoexpressionAnalysisService.loadRegressedMatrix( ee ) ).thenReturn( matrix );
        ee.getBioAssays().clear();
        ee.getBioAssays().addAll( assays );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );

        try ( Response r = target( "/datasets/1/sample-correlation" ).request().get() ) {
            String body = r.readEntity( String.class );
            assertThat( body ).contains( "0.823" ).doesNotContain( "0.8231947" );
            assertThat( body ).contains( "NaN" );
        }
    }

    /** The default is `best`, and the response says which of the two it actually got. */
    @Test
    public void testSampleCorrelationSaysWhichMatrixItReturned() {
        BioAssay a1 = BioAssay.Factory.newInstance( "BA1" );
        a1.setId( 100L );
        List<BioAssay> assays = Collections.singletonList( a1 );
        DenseDoubleMatrix<BioAssay, BioAssay> matrix = new DenseDoubleMatrix<>( new double[][] { { 1.0 } } );
        matrix.setRowNames( assays );
        matrix.setColumnNames( assays );
        ee.getBioAssays().clear();
        ee.getBioAssays().addAll( assays );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );

        when( sampleCoexpressionAnalysisService.loadRegressedMatrix( ee ) ).thenReturn( matrix );
        try ( Response r = target( "/datasets/1/sample-correlation" ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            assertThat( r.readEntity( String.class ) ).contains( "\"matrix\":\"regressed\"" );
        }
    }

    /** No regressed matrix: `best` falls back to the full one and says so, rather than implying regression. */
    @Test
    public void testSampleCorrelationBestFallsBackToFullAndSaysSo() {
        BioAssay a1 = BioAssay.Factory.newInstance( "BA1" );
        a1.setId( 100L );
        List<BioAssay> assays = Collections.singletonList( a1 );
        DenseDoubleMatrix<BioAssay, BioAssay> matrix = new DenseDoubleMatrix<>( new double[][] { { 1.0 } } );
        matrix.setRowNames( assays );
        matrix.setColumnNames( assays );
        ee.getBioAssays().clear();
        ee.getBioAssays().addAll( assays );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );

        when( sampleCoexpressionAnalysisService.loadRegressedMatrix( ee ) ).thenReturn( null );
        when( sampleCoexpressionAnalysisService.loadFullMatrix( ee ) ).thenReturn( matrix );
        try ( Response r = target( "/datasets/1/sample-correlation" ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            assertThat( r.readEntity( String.class ) ).contains( "\"matrix\":\"full\"" );
        }
    }

    /** `matrix=full` takes the full one even when a regressed one exists — that is the whole point. */
    @Test
    public void testSampleCorrelationFullIsServedOnRequestEvenWhenRegressedExists() {
        BioAssay a1 = BioAssay.Factory.newInstance( "BA1" );
        a1.setId( 100L );
        List<BioAssay> assays = Collections.singletonList( a1 );
        DenseDoubleMatrix<BioAssay, BioAssay> full = new DenseDoubleMatrix<>( new double[][] { { 1.0 } } );
        full.setRowNames( assays );
        full.setColumnNames( assays );
        ee.getBioAssays().clear();
        ee.getBioAssays().addAll( assays );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        when( sampleCoexpressionAnalysisService.loadFullMatrix( ee ) ).thenReturn( full );

        try ( Response r = target( "/datasets/1/sample-correlation" ).queryParam( "matrix", "full" ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            assertThat( r.readEntity( String.class ) ).contains( "\"matrix\":\"full\"" );
        }
        verify( sampleCoexpressionAnalysisService, never() ).loadRegressedMatrix( any() );
    }

    /** `matrix=regressed` never silently substitutes the full one; a dataset without one 404s. */
    @Test
    public void testSampleCorrelationRegressedDoesNotFallBack() {
        when( sampleCoexpressionAnalysisService.loadRegressedMatrix( ee ) ).thenReturn( null );
        assertThat( target( "/datasets/1/sample-correlation" ).queryParam( "matrix", "regressed" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
        verify( sampleCoexpressionAnalysisService, never() ).loadFullMatrix( any() );
    }

    /**
     * 🛑 Temporary, pending the per-cell-type design. A single-cell dataset's correlation matrix is the
     * pseudo-bulk grid (samples &times; cell types), so its correlations are taken across cell types and
     * the median-correlation outlier rule reads a rare cell type as an outlier. The assertion that
     * matters is that the matrix is never even loaded: returning it and letting the caller decide is
     * exactly what we are stopping, and a 404 reached after the load would still cost ~100 MB.
     */
    @Test
    public void testGetDatasetSampleCorrelationIsWithheldForSingleCell() {
        when( expressionExperimentService.isSingleCell( ee ) ).thenReturn( true );
        assertThat( target( "/datasets/1/sample-correlation" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( sampleCoexpressionAnalysisService, never() ).loadRegressedMatrix( any() );
        verify( sampleCoexpressionAnalysisService, never() ).loadFullMatrix( any() );
    }

    /** The same dataset, not single-cell, still serves the matrix — the gate is the flag, not the route. */
    @Test
    public void testGetDatasetSampleCorrelationStillServedWhenNotSingleCell() {
        BioAssay a1 = BioAssay.Factory.newInstance( "BA1" );
        a1.setId( 100L );
        List<BioAssay> assays = Collections.singletonList( a1 );
        DenseDoubleMatrix<BioAssay, BioAssay> matrix = new DenseDoubleMatrix<>( new double[][] { { 1.0 } } );
        matrix.setRowNames( assays );
        matrix.setColumnNames( assays );
        when( expressionExperimentService.isSingleCell( ee ) ).thenReturn( false );
        when( sampleCoexpressionAnalysisService.loadRegressedMatrix( ee ) ).thenReturn( matrix );
        ee.getBioAssays().clear();
        ee.getBioAssays().addAll( assays );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        assertThat( target( "/datasets/1/sample-correlation" ).request().get() )
                .hasStatus( Response.Status.OK );
        verify( sampleCoexpressionAnalysisService ).loadRegressedMatrix( ee );
    }

    @Test
    public void testGetDatasetSampleCorrelationWhenDatasetMissingIs404() {
        assertThat( target( "/datasets/999/sample-correlation" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
        verify( sampleCoexpressionAnalysisService, never() ).loadRegressedMatrix( any() );
        verify( sampleCoexpressionAnalysisService, never() ).loadFullMatrix( any() );
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

    // --- JSON precision: four significant digits on the bulk floating-point payloads -----

    /**
     * Mean-variance is the heaviest diagnostics payload — one mean and one variance per probe. It has no
     * precision opt-out.
     * <p>
     * The second half of the assertion is the part that matters: {@code MeanVarianceRelation.getMeans()}
     * hands back the loaded entity's own array, so rounding in place would corrupt it for every later reader.
     */
    @Test
    public void testGetDatasetMeanVarianceIsRoundedAndLeavesTheEntityArrayAlone() {
        double[] means = { 7.607533048115258, 0.0, Double.NaN };
        double[] variances = { 5.7612345678901e-4, 3.0812345678901, Double.POSITIVE_INFINITY };
        ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation mvr =
                ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation.Factory.newInstance( means, variances );
        ee.setMeanVarianceRelation( mvr );
        when( expressionExperimentService.loadWithMeanVarianceRelation( ee.getId() ) ).thenReturn( ee );

        try ( Response r = target( "/datasets/1/mean-variance" ).request().get() ) {
            assertThat( r ).hasStatus( Response.Status.OK );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data.means[0]", 7.608 )
                    .hasPathWithValue( "$.data.means[1]", 0.0 )
                    // 5.76e-4 is one order of magnitude off a 0.001 floor; a decimal-places rounding would
                    // flatten the informative low-variance end of the plot to zero.
                    .hasPathWithValue( "$.data.variances[0]", 5.761E-4 )
                    .hasPathWithValue( "$.data.variances[1]", 3.081 );
        }

        assertThat( mvr.getMeans()[0] ).isEqualTo( 7.607533048115258 );
        assertThat( mvr.getVariances()[0] ).isEqualTo( 5.7612345678901e-4 );
    }

    // --- Mean-variance decimation -------------------------------------------------------

    /**
     * Two points that land in the same grid cell collapse to the first of them, and the pairing survives:
     * the fixture makes every variance twice its mean, so an entry dropped from one array and not the other
     * would re-pair every point after it and break that relation.
     */
    @Test
    public void testGetDatasetMeanVarianceKeepsOnePointPerCellAndStaysIndexParallel() {
        // (0, 0) and (0.1, 0.2) both land in cell (0, 0) of the grid laid over means [0, 100] /
        // variances [0, 200]; the second is the duplicate.
        double[] means = { 0.0, 0.1, 50.0, 100.0 };
        double[] variances = { 0.0, 0.2, 100.0, 200.0 };
        DatasetsWebService.MeanVarianceValueObject vo = new DatasetsWebService.MeanVarianceValueObject(
                ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation.Factory.newInstance( means, variances ) );

        assertThat( vo.getMeans() ).containsExactly( 0.0, 50.0, 100.0 );
        assertThat( vo.getVariances() ).containsExactly( 0.0, 100.0, 200.0 );
        assertThat( vo.getVariances() ).hasSameSizeAs( vo.getMeans() );
        for ( int i = 0; i < vo.getMeans().length; i++ ) {
            assertThat( vo.getVariances()[i] ).isEqualTo( 2 * vo.getMeans()[i] );
        }
    }

    /**
     * Points that each get their own cell come back untouched — thinning only ever removes a point that
     * would be drawn on top of one already sent.
     */
    @Test
    public void testGetDatasetMeanVarianceLeavesWellSeparatedPointsAlone() {
        double[] means = { 1.0, 2.0, 3.0, 4.0 };
        double[] variances = { 0.1, 0.4, 0.9, 1.6 };
        DatasetsWebService.MeanVarianceValueObject vo = new DatasetsWebService.MeanVarianceValueObject(
                ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation.Factory.newInstance( means, variances ) );

        assertThat( vo.getMeans() ).containsExactly( 1.0, 2.0, 3.0, 4.0 );
        assertThat( vo.getVariances() ).containsExactly( 0.1, 0.4, 0.9, 1.6 );
    }

    /**
     * A point whose mean or variance is not finite has no position on the scatter, so it is dropped rather
     * than keyed into the grid. Both arrays lose it together.
     */
    @Test
    public void testGetDatasetMeanVarianceDropsNonFinitePoints() {
        double[] means = { 1.0, Double.NaN, 2.0, Double.POSITIVE_INFINITY, 3.0 };
        double[] variances = { 10.0, 5.0, Double.NEGATIVE_INFINITY, 6.0, 30.0 };
        DatasetsWebService.MeanVarianceValueObject vo = new DatasetsWebService.MeanVarianceValueObject(
                ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation.Factory.newInstance( means, variances ) );

        assertThat( vo.getMeans() ).containsExactly( 1.0, 3.0 );
        assertThat( vo.getVariances() ).containsExactly( 10.0, 30.0 );
    }

    /**
     * The size guard: 30,000 points drawn from ten distinct coordinates come back as ten. This is the shape
     * of the real saving — eid 1 sends 22,283 points of which 93% land where one has already been painted.
     */
    @Test
    public void testGetDatasetMeanVarianceCollapsesAHeavilyOverplottedDataset() {
        double[] means = new double[30000];
        double[] variances = new double[30000];
        for ( int i = 0; i < means.length; i++ ) {
            means[i] = i % 10;
            variances[i] = i % 10;
        }
        DatasetsWebService.MeanVarianceValueObject vo = new DatasetsWebService.MeanVarianceValueObject(
                ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation.Factory.newInstance( means, variances ) );

        assertThat( vo.getMeans() ).containsExactly( 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 );
        assertThat( vo.getVariances() ).hasSameSizeAs( vo.getMeans() );
    }

    /**
     * The thinned arrays are what actually goes on the wire.
     */
    @Test
    public void testGetDatasetMeanVarianceIsThinnedOnTheWire() {
        double[] means = { 0.0, 0.1, 50.0, 100.0 };
        double[] variances = { 0.0, 0.2, 100.0, 200.0 };
        ee.setMeanVarianceRelation(
                ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation.Factory.newInstance( means, variances ) );
        when( expressionExperimentService.loadWithMeanVarianceRelation( ee.getId() ) ).thenReturn( ee );

        try ( Response r = target( "/datasets/1/mean-variance" ).request().get() ) {
            assertThat( r ).hasStatus( Response.Status.OK );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data.means[0]", 0.0 )
                    .hasPathWithValue( "$.data.means[1]", 50.0 )
                    .hasPathWithValue( "$.data.means[2]", 100.0 )
                    .doesNotHavePath( "$.data.means[3]" )
                    .hasPathWithValue( "$.data.variances[1]", 100.0 )
                    .doesNotHavePath( "$.data.variances[3]" );
        }
    }

    /**
     * SVD loadings serialize at a mean of ~20 characters each and nothing consumes the digits below it, so this route
     * rounds with no opt-out. {@code SVDResult.getVariances()} / {@code getVMatrix().getRawMatrix()} are the
     * result's own arrays, hence the non-mutation half.
     */
    @Test
    public void testGetDatasetSvdIsRoundedAndLeavesTheResultArraysAlone() {
        BioAssay a1 = BioAssay.Factory.newInstance( "BA1" );
        a1.setId( 200L );
        ubic.gemma.model.expression.biomaterial.BioMaterial m1 =
                ubic.gemma.model.expression.biomaterial.BioMaterial.Factory.newInstance();
        m1.setId( 300L );
        double[][] rawV = { { 0.123456789, -0.987654321 } };
        DenseDoubleMatrix<ubic.gemma.model.expression.biomaterial.BioMaterial, Integer> vMatrix =
                new DenseDoubleMatrix<>( rawV );
        vMatrix.setRowNames( Collections.singletonList( m1 ) );
        vMatrix.setColumnNames( Arrays.asList( 0, 1 ) );
        double[] rawVariances = { 0.4567890123456789 };
        ubic.gemma.core.analysis.preprocess.svd.SVDResult svd =
                mock( ubic.gemma.core.analysis.preprocess.svd.SVDResult.class );
        when( svd.getBioAssays() ).thenReturn( Collections.singletonList( a1 ) );
        when( svd.getBioMaterials() ).thenReturn( Collections.singletonList( m1 ) );
        when( svd.getVariances() ).thenReturn( rawVariances );
        when( svd.getVMatrix() ).thenReturn( vMatrix );
        when( svdService.getSvd( ee ) ).thenReturn( svd );

        try ( Response r = target( "/datasets/1/svd" ).request().get() ) {
            assertThat( r ).hasStatus( Response.Status.OK );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data.variances[0]", 0.4568 )
                    // "vmatrix", not "vMatrix": Jackson's legacy getter naming lowercases the whole leading
                    // uppercase run of getVMatrix(). Pre-existing wire name, asserted so it stays put.
                    .hasPathWithValue( "$.data.vmatrix[0][0]", 0.1235 )
                    .hasPathWithValue( "$.data.vmatrix[0][1]", -0.9877 );
        }

        assertThat( rawVariances[0] ).isEqualTo( 0.4567890123456789 );
        assertThat( rawV[0][0] ).isEqualTo( 0.123456789 );
    }

    /**
     * Expression levels are a data-download surface, so rounding here is the default and {@code precise=true}
     * is the opt-out. Both branches are asserted on the wire.
     */
    @Test
    public void testGetDatasetsExpressionLevelsForGeneIsRoundedUnlessPreciseIsAsked() {
        when( geneArgService.getEntity( any() ) ).thenReturn( new Gene() );
        when( expressionExperimentService.loadIdsWithCache( any(), any( Sort.class ) ) )
                .thenAnswer( a -> new ArrayList<>( Collections.singletonList( 1L ) ) );
        when( processedExpressionDataVectorService.getExpressionLevelsByIds( any(), any(), anyBoolean(), any() ) )
                .thenAnswer( a -> Collections.singletonList( expressionLevelsFixture() ) );

        try ( Response r = target( "/datasets/expressions/genes/BRCA1" ).request().get() ) {
            assertThat( r ).hasStatus( Response.Status.OK );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data[0].geneExpressionLevels[0].vectors[0].bioAssayExpressionLevels.BA1", 7.608 )
                    .hasPathWithValue( "$.data[0].geneExpressionLevels[0].correctedPvalue", 1.235E-7 )
                    .hasPathWithValue( "$.data[0].geneExpressionLevels[0].log2FoldChange", 2.718 );
        }

        try ( Response r = target( "/datasets/expressions/genes/BRCA1" ).queryParam( "precise", true ).request().get() ) {
            assertThat( r ).hasStatus( Response.Status.OK );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data[0].geneExpressionLevels[0].vectors[0].bioAssayExpressionLevels.BA1", 7.607533048115258 )
                    .hasPathWithValue( "$.data[0].geneExpressionLevels[0].correctedPvalue", 1.2345678901234E-7 )
                    .hasPathWithValue( "$.data[0].geneExpressionLevels[0].log2FoldChange", 2.718281828459045 );
        }
    }

    private static ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject expressionLevelsFixture() {
        ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject vo =
                new ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject();
        ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject.GeneElementExpressionsValueObject gene =
                new ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject.GeneElementExpressionsValueObject(
                        "BRCA1", "breast cancer 1", 672, "ENSG00000012048",
                        1.2345678901234E-7, 9.8765432109876E-11, 2.718281828459045,
                        null, false, null );
        Map<String, Double> levels = new LinkedHashMap<>();
        levels.put( "BA1", 7.607533048115258 );
        gene.getVectors().add(
                new ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject.VectorElementValueObject( "probe_a", levels ) );
        vo.getGeneExpressionLevels().add( gene );
        return vo;
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
        // Vectors carry the probe's gene IDs (GENE2CS-populated on the way out of the processed
        // cache); that's what the endpoint resolves gene refs from. probe_a is non-specific (two
        // genes), probe_b maps to one, probe_c has no vector at all.
        ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject v1 = mock( ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject.class );
        when( v1.getGenes() ).thenReturn( Arrays.asList( 300L, 301L ) );
        ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject v2 = mock( ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject.class );
        when( v2.getGenes() ).thenReturn( Collections.singletonList( 302L ) );
        Map<ubic.gemma.model.analysis.expression.pca.ProbeLoading, ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject> stored = new LinkedHashMap<>();
        stored.put( pl1, v1 );
        stored.put( pl2, v2 );
        stored.put( pl3, null );
        when( svdService.getTopLoadedVectors( eq( ee ), anyInt(), anyInt() ) ).thenReturn( stored );

        Gene g300 = Gene.Factory.newInstance();
        g300.setId( 300L );
        g300.setOfficialSymbol( "ZZZ3" );
        g300.setNcbiGeneId( 26009 );
        Gene g301 = Gene.Factory.newInstance();
        g301.setId( 301L );
        g301.setOfficialSymbol( "AAA1" );
        // no NCBI id: the ref must still serialize, just without the field
        Gene g302 = Gene.Factory.newInstance();
        g302.setId( 302L );
        g302.setOfficialSymbol( "BRCA1" );
        g302.setNcbiGeneId( 672 );
        when( geneService.loadThawedLiter( anyCollection() ) ).thenReturn( Arrays.asList( g300, g301, g302 ) );

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

        try ( Response r = target( "/datasets/1/svd/loadings" ).queryParam( "pc", 1 ).queryParam( "top", 2 ).request().get() ) {
            assertThat( r )
                    .hasStatus( Response.Status.OK )
                    .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
            // default direction=both sorts by |loading| desc: pl1 (0.9), pl2 (-0.7), pl3 (0.3) →
            // first two are 0.9, -0.7. probe_a is non-specific, so its row carries both genes in
            // the same `genes` shape heatmap-data rows use; probe_b carries one.
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data.pc", 1 )
                    .doesNotHavePath( "$.data.rows[2]" )
                    .hasPathWithValue( "$.data.rows[0].genes[0].officialSymbol", "ZZZ3" )
                    .hasPathWithValue( "$.data.rows[0].genes[0].ncbiId", 26009 )
                    .hasPathWithValue( "$.data.rows[0].genes[1].officialSymbol", "AAA1" )
                    .doesNotHavePath( "$.data.rows[0].genes[1].ncbiId" )
                    .doesNotHavePath( "$.data.rows[0].genes[2]" )
                    .hasPathWithValue( "$.data.rows[1].genes[0].officialSymbol", "BRCA1" )
                    .hasPathWithValue( "$.data.rows[1].genes[0].ncbiId", 672 )
                    .doesNotHavePath( "$.data.rows[1].genes[1]" );
        }
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
        ArgumentCaptor<PublicationAssertion> primaryCaptor = ArgumentCaptor.forClass( PublicationAssertion.class );
        ArgumentCaptor<Collection<PublicationAssertion>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updatePublications( eq( ee ), primaryCaptor.capture(), captor.capture(),
                isNull() );
        assertThat( primaryCaptor.getValue().getPublication() ).isEqualTo( prim );
        // A body that states no source is the ordinary curator edit this endpoint exists for, and is
        // recorded as such -- which is exactly why an agent has to say "agent" out loud.
        assertThat( primaryCaptor.getValue().getSource() ).isEqualTo( PublicationAssociationSource.CURATOR );
        assertThat( captor.getValue() ).singleElement()
                .satisfies( a -> assertThat( a.getPublication() ).isEqualTo( other ) );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsCarriesEvidence() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( 1L ) ).thenReturn( ee );
        BibliographicReference prim = new BibliographicReference();
        prim.setId( 10L );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "38165001" ) ).thenReturn( prim );

        String body = "{\"primaryPublication\":{\"pubMedId\":\"38165001\",\"source\":\"agent\","
                + "\"evidence\":\"the series title names this paper almost verbatim\","
                + "\"evidenceCode\":\"IC\",\"confidence\":0.9,\"assertedBy\":\"pub_finder/run-42\"},"
                + "\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<PublicationAssertion> captor = ArgumentCaptor.forClass( PublicationAssertion.class );
        verify( expressionExperimentService ).updatePublications( eq( ee ), captor.capture(),
                argThat( Collection::isEmpty ), isNull() );
        PublicationAssertion a = captor.getValue();
        assertThat( a.getSource() ).isEqualTo( PublicationAssociationSource.AGENT );
        assertThat( a.getEvidence() ).isEqualTo( "the series title names this paper almost verbatim" );
        assertThat( a.getEvidenceCode() ).isEqualTo( GOEvidenceCode.IC );
        assertThat( a.getConfidence() ).isEqualTo( 0.9 );
        assertThat( a.getAssertedBy() ).isEqualTo( "pub_finder/run-42" );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsRecordsRejection() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( 1L ) ).thenReturn( ee );
        BibliographicReference wrong = new BibliographicReference();
        wrong.setId( 40L );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "38088204" ) ).thenReturn( wrong );

        // GSE227854's shape: GEO's own !Series_pubmed_id names the wrong one of the submitter's two
        // NAR 2024 papers, and there is no correct paper being set in the same breath.
        String body = "{\"primaryPublication\":null,\"otherRelevantPublications\":[],"
                + "\"rejectedPublications\":[{\"pubMedId\":\"38088204\",\"source\":\"curator\","
                + "\"evidence\":\"GEO links this, but the series title names a different NAR 2024 paper by the same lab\"}]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<Collection<PublicationAssertion>> captor = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService ).updatePublications( eq( ee ), isNull(),
                argThat( Collection::isEmpty ), captor.capture() );
        assertThat( captor.getValue() ).singleElement().satisfies( a -> {
            assertThat( a.getPublication() ).isEqualTo( wrong );
            assertThat( a.getSource() ).isEqualTo( PublicationAssociationSource.CURATOR );
            assertThat( a.getEvidence() ).contains( "the series title names a different NAR 2024 paper" );
        } );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsOmittingRejectedLeavesThemUntouched() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( 1L ) ).thenReturn( ee );
        BibliographicReference right = new BibliographicReference();
        right.setId( 41L );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "38165001" ) ).thenReturn( right );

        // What a client sends after reading the dataset back: the plain GET does not return rejections,
        // so this body is everything it saw. Coerced to an empty list it used to clear GSE227854's
        // curator rejection of GEO's wrong !Series_pubmed_id -- a ruling this caller never laid eyes on.
        String body = "{\"primaryPublication\":{\"pubMedId\":\"38165001\"},\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( expressionExperimentService ).updatePublications( eq( ee ), any( PublicationAssertion.class ),
                argThat( Collection::isEmpty ), isNull() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsEmptyRejectedListStillClearsThem() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( 1L ) ).thenReturn( ee );

        // Present-but-empty is a caller that has considered the rejections and wants none of them; it
        // must still reach the service as a clear, or there is no way to overturn one through the API.
        String body = "{\"primaryPublication\":null,\"otherRelevantPublications\":[],\"rejectedPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        verify( expressionExperimentService ).updatePublications( eq( ee ), isNull(),
                argThat( Collection::isEmpty ), argThat( Collection::isEmpty ) );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsRejectedByHigherAuthorityIsConflict() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        BibliographicReference wrong = new BibliographicReference();
        wrong.setId( 40L );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "38088204" ) ).thenReturn( wrong );
        doThrow( new PublicationAssociationConflictException( "rejected by curator", new PublicationAssociation() ) )
                .when( expressionExperimentService ).updatePublications( eq( ee ), any(), any(), any() );

        String body = "{\"primaryPublication\":{\"pubMedId\":\"38088204\",\"source\":\"agent\"},\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.CONFLICT );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsRejectsUnknownSource() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        BibliographicReference prim = new BibliographicReference();
        prim.setId( 10L );
        when( bibliographicReferenceService.findOrCreateByPubMedId( "111" ) ).thenReturn( prim );
        // Not silently defaulted to curator: that would hand the highest rank to a typo.
        String body = "{\"primaryPublication\":{\"pubMedId\":\"111\",\"source\":\"robot\"},\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updatePublications( any(), any(), any(), any() );
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
        ArgumentCaptor<PublicationAssertion> preprintCaptor = ArgumentCaptor.forClass( PublicationAssertion.class );
        verify( expressionExperimentService ).updatePublications( eq( ee ), preprintCaptor.capture(), argThat( Collection::isEmpty ), isNull() );
        assertThat( preprintCaptor.getValue().getPublication() ).isEqualTo( preprint );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsRejectsBothPubMedIdAndDoi() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"primaryPublication\":{\"pubMedId\":\"111\",\"doi\":\"10.1101/x\"},\"otherRelevantPublications\":[]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updatePublications( any(), any( PublicationAssertion.class ), any(), any() );
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

        // "Clear all" clears the dataset's publications, which is what this body names. It does not
        // reach the rejections: those are cleared only by sending an explicit empty rejectedPublications
        // (see testUpdateDatasetPublicationsEmptyRejectedListStillClearsThem).
        verify( expressionExperimentService ).updatePublications( eq( ee ), isNull(), argThat( Collection::isEmpty ), isNull() );
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
        verify( expressionExperimentService, never() ).updatePublications( any(), any( PublicationAssertion.class ), any(), any() );
    }

    @Test
    @WithMockUser
    public void testUpdateDatasetPublicationsRejectsBlankPubMedId() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"otherRelevantPublications\":[{\"pubMedId\":\"  \"}]}";
        assertThat( target( "/datasets/1/publications" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).updatePublications( any(), any( PublicationAssertion.class ), any(), any() );
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
        verify( expressionExperimentService, never() ).updatePublications( any(), any( PublicationAssertion.class ), any(), any() );
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

        // The basis travels with the identifier: these fields are on PublicationEntry wherever it is accepted,
        // and this path used to resolve them away, so a paper committed with a stated reason was stored as an
        // unexplained curator claim.
        String body = "{\"publications\":{\"primary\":{\"pubMedId\":\"111\","
                + "\"source\":\"geo_submitter_link\",\"evidence\":\"the series names it\"},"
                + "\"otherRelevant\":[]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().isPublicationsPresent() ).isTrue();
        assertThat( cap.getValue().getPrimaryPublication() ).isNotNull();
        assertThat( cap.getValue().getPrimaryPublication().getPublication() ).isEqualTo( ref );
        assertThat( cap.getValue().getPrimaryPublication().getSource() )
                .isEqualTo( ubic.gemma.model.common.description.PublicationAssociationSource.GEO_SUBMITTER_LINK );
        assertThat( cap.getValue().getPrimaryPublication().getEvidence() ).isEqualTo( "the series names it" );
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

    /**
     * A tag carrying nested statements must reach the commit as a {@link Statement}, not a bare
     * Characteristic. cab reported it accepted and silently dropped: preflight said created=1, the commit
     * returned 200 and minted a snapshot, and the stored row had PREDICATE NULL (GSE104324,
     * CHARACTERISTIC 56965512, discriminator NULL = a plain Characteristic).
     */
    @Test
    @WithMockUser
    public void testCommitCurationTagWithStatementsReachesTheCommitAsAStatement() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"tag-0\","
                + "\"category\":{\"label\":\"cell type\"},"
                + "\"value\":{\"label\":\"Schwann cell\"},"
                + "\"statements\":{\"items\":[{"
                + "\"category\":{\"label\":\"cell type\"},"
                + "\"subject\":{\"label\":\"Schwann cell\"},"
                + "\"predicate\":{\"label\":\"derives from part of\"},"
                + "\"object\":{\"label\":\"sciatic nerve\"}}]},"
                + "\"evidenceCode\":\"IEA\"}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        Characteristic ch = cap.getValue().getTagsToAdd().get( 0 ).getCharacteristic();
        assertThat( ch ).isInstanceOf( Statement.class );
        Statement st = ( Statement ) ch;
        assertThat( st.getSubject() ).isEqualTo( "Schwann cell" );
        assertThat( st.getPredicate() ).isEqualTo( "derives from part of" );
        assertThat( st.getObject() ).isEqualTo( "sciatic nerve" );
    }

    /**
     * A tag carrying two statements must store BOTH pairs. The converter used to read {@code items[0]}
     * and discard the rest, so a two-statement tag silently became a one-statement tag — cab lost six
     * statements across five tags that way (2026-08-31) and only found it by reading SECOND_PREDICATE
     * in the database, because a tag that lost a claim looks exactly like one that never made it.
     */
    @Test
    @WithMockUser
    public void testCommitCurationTagKeepsBothStatements() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"tag-0\","
                + "\"category\":{\"label\":\"cell type\"},\"value\":{\"label\":\"retinal cell\"},"
                + "\"statements\":{\"items\":["
                + "{\"subject\":{\"label\":\"retinal cell\"},\"predicate\":{\"label\":\"derives from cell line\"},\"object\":{\"label\":\"H9 cell\"}},"
                + "{\"subject\":{\"label\":\"retinal cell\"},\"predicate\":{\"label\":\"has modifier\"},\"object\":{\"label\":\"organoid\"}}"
                + "]}}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap =
                ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        Statement st = ( Statement ) cap.getValue().getTagsToAdd().get( 0 ).getCharacteristic();
        assertThat( st.getPredicate() ).isEqualTo( "derives from cell line" );
        assertThat( st.getObject() ).isEqualTo( "H9 cell" );
        assertThat( st.getSecondPredicate() ).isEqualTo( "has modifier" );
        assertThat( st.getSecondObject() ).isEqualTo( "organoid" );
    }

    /** A row holds two pairs, so a third claim is refused rather than silently dropped. */
    @Test
    @WithMockUser
    public void testCommitCurationRejectsAThirdStatementOnATag() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );

        String one = "{\"subject\":{\"label\":\"pyramidal neuron\"},\"predicate\":{\"label\":\"p\"},\"object\":{\"label\":\"o\"}}";
        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"tag-0\","
                + "\"category\":{\"label\":\"cell type\"},\"value\":{\"label\":\"pyramidal neuron\"},"
                + "\"statements\":{\"items\":[" + one + "," + one + "," + one + "]}}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    /**
     * An experiment tag with no {@code value.uri} is refused unless the caller says the free text is
     * deliberate. Paul's ruling, 2026-09-01: reject, but give the client a way to declare intent — an
     * ungrounded tag is usually an oversight, and after the fact it cannot be told apart from a
     * grounding somebody meant to do and forgot.
     */
    @Test
    @WithMockUser
    public void testCommitCurationRejectsAnUndeclaredUngroundedTag() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );

        String body = "{\"tags\":{\"items\":[{\"clientRef\":\"t1\","
                + "\"category\":{\"label\":\"cell line\",\"uri\":\"http://www.ebi.ac.uk/efo/EFO_0000322\"},"
                + "\"value\":{\"label\":\"HT22\"}}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /** Declaring it accepts the same tag — the gate is the declaration, not the absence of a URI. */
    @Test
    @WithMockUser
    public void testCommitCurationAcceptsADeclaredFreeTextTag() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"t1\","
                + "\"category\":{\"label\":\"cell line\",\"uri\":\"http://www.ebi.ac.uk/efo/EFO_0000322\"},"
                + "\"value\":{\"label\":\"HT22\"},\"freeTextIntended\":true}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        verify( expressionExperimentService ).commitCuration( eq( ee ), any(), eq( false ) );
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

        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"t1\",\"category\":{\"label\":\"disease\"},"
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

    /**
     * A tags item bearing a gemmaId is a keep-marker: the section is add/delete only, so the mapper reads the id
     * and nothing else. Decorating one used to be a 200 for an edit that never happened — a client that set
     * {@code supportingEvidence} on an existing tag was told it had, and had not — so any other field is now a
     * 400. Every offending field is named in one response: a caller told about them one at a time strips its
     * payload one round trip at a time, and this fires mid-campaign.
     */
    @Test
    @WithMockUser
    public void testCommitRejectsDecoratedTagKeepMarker() {
        stubCommitOk();
        String body = "{\"tags\":{\"items\":[{\"gemmaId\":42,\"category\":{\"label\":\"disease\"},"
                + "\"value\":{\"label\":\"glioma\"},\"statements\":{\"items\":[{\"clientRef\":\"s1\"}]},"
                + "\"supportingEvidence\":[{\"quote\":\"glioblastoma multiforme\"}],\"evidenceCode\":\"IEA\"}]}}";
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) )
                    .contains( "tags[gemmaId=42] carries category, value, statements, supportingEvidence, evidenceCode" )
                    // the remedy, since the message is the whole diagnosis a mid-campaign caller gets
                    .contains( "deletedIds" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /**
     * The preflight shares the mapper, so it refuses exactly what the commit refuses. A dry run that accepted a
     * payload the commit rejects would stop being a rehearsal.
     */
    @Test
    @WithMockUser
    public void testPreflightRejectsDecoratedTagKeepMarker() {
        stubCommitOk();
        String body = "{\"tags\":{\"items\":[{\"gemmaId\":42,\"supportingEvidence\":[{\"quote\":\"q\"}]}]}}";
        try ( Response r = target( "/datasets/1/curation/preflight" ).request().post( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) ).contains( "tags[gemmaId=42] carries supportingEvidence" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /**
     * sampleCharacteristics has the same add/delete-only shape and discarded decoration the same way, so it gets
     * the same refusal — {@code bioassayShortName} included, which on a keep-marker reads like "move this
     * characteristic to that sample" and does nothing. Both sections report in one response, the way term
     * violations do.
     */
    @Test
    @WithMockUser
    public void testCommitRejectsDecoratedKeepMarkersInEverySectionAtOnce() {
        stubCommitOk();
        String body = "{\"tags\":{\"items\":[{\"gemmaId\":42,\"value\":{\"label\":\"glioma\"}}]},"
                + "\"sampleCharacteristics\":{\"items\":[{\"gemmaId\":91,\"bioassayShortName\":\"GSM999\","
                + "\"category\":{\"label\":\"organism part\"}}]}}";
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) )
                    .contains( "tags[gemmaId=42] carries value" )
                    .contains( "sampleCharacteristics[gemmaId=91] carries bioassayShortName, category" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /**
     * A restore replays a SNAPSHOT, and a snapshot records the content of every row — including the rows the
     * restore only has to leave alone. The reconciliation strips that content off the items that keep their id,
     * so the document the restore builds for itself is legal under the keep-marker rule the commit enforces.
     * Nothing is lost: a decorated keep-marker's content was never applied.
     * <p>
     * Guards the one way this rule could break Gemma's own writes rather than a client's.
     */
    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testRestoreOfASnapshotWithSurvivingTagsIsNotRefusedAsDecorated() {
        stubCommitOk();
        ubic.gemma.model.common.description.AnnotationValueObject tag =
                new ubic.gemma.model.common.description.AnnotationValueObject();
        tag.setId( 42L );
        tag.setObjectClass( "ExperimentTag" );
        tag.setCategory( "disease" );
        tag.setValue( "glioma" );
        tag.setEvidenceCode( GOEvidenceCode.IEA.name() );
        when( expressionExperimentService.getAnnotations( any( ExpressionExperiment.class ), anyBoolean() ) )
                .thenReturn( Collections.singleton( tag ) );

        ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet set =
                new ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet();
        set.setId( 5L );
        set.setRole( ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole.SNAPSHOT );
        set.setInvestigation( ee );
        // The tag survives, so the reconciliation keeps its id -- and has to drop the content beside it.
        set.setPayloadJson( "{\"tags\":{\"items\":[{\"gemmaId\":42,\"category\":{\"label\":\"disease\"},"
                + "\"value\":{\"label\":\"glioma\"},\"evidenceCode\":\"IEA\"}],\"deletedIds\":[]}}" );
        when( annotationSetService.load( 5L ) ).thenReturn( set );

        assertThat( target( "/datasets/1/annotation-sets/5/restore" ).request().post( Entity.json( "" ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap = curationCaptor();
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().getTagsUnchanged() ).isEqualTo( 1 );
        assertThat( cap.getValue().getTagsToAdd() ).isEmpty();
    }

    /** Stub the load + commit a tags-section test needs, and return the captor for the request the mapper built. */
    private ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> curationCaptor() {
        return ArgumentCaptor.forClass( ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest.class );
    }

    private void stubCommitOk() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );
    }

    /**
     * The tag's {@code supportingEvidence} reaches the Characteristic handed to the service. The section had no
     * coverage at all for this field — the only evidence guard was on design statements — so a mapper that
     * accepted it and built a Characteristic without it would have been invisible here.
     */
    @Test
    @WithMockUser
    public void testCommitCurationTagCarriesSupportingEvidence() {
        stubCommitOk();
        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"t1\",\"category\":{\"label\":\"disease\"},"
                + "\"value\":{\"label\":\"glioma\"},\"supportingEvidence\":[{\"quote\":\"glioblastoma multiforme\","
                + "\"source\":\"characteristic\",\"location\":\"GSM1\"}]}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap = curationCaptor();
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().getTagsToAdd().get( 0 ).getCharacteristic().getSupportingEvidence() )
                .contains( "glioblastoma multiforme" );
    }

    /** A stated evidence code reaches the Characteristic instead of being left to the add path's default. */
    @Test
    @WithMockUser
    public void testCommitCurationTagCarriesEvidenceCode() {
        stubCommitOk();
        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"t1\",\"category\":{\"label\":\"disease\"},"
                + "\"value\":{\"label\":\"glioma\"},\"evidenceCode\":\"IEA\"}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap = curationCaptor();
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().getTagsToAdd().get( 0 ).getCharacteristic().getEvidenceCode() )
                .isEqualTo( GOEvidenceCode.IEA );
    }

    /**
     * Omitting the field leaves the Characteristic's code null, which is what hands the row to
     * {@code ExpressionExperimentWriteServiceImpl#addCharacteristic}'s {@code IC} fallback — the code every tag
     * written through this route has carried. The guard is that the mapper stamps NOTHING of its own: a server
     * that picked a code here (from the caller's identity, say) would put a value nobody chose on the row.
     */
    @Test
    @WithMockUser
    public void testCommitCurationTagWithoutEvidenceCodeLeavesItUnset() {
        stubCommitOk();
        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"t1\",\"category\":{\"label\":\"disease\"},"
                + "\"value\":{\"label\":\"glioma\"}}]}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap = curationCaptor();
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().getTagsToAdd().get( 0 ).getCharacteristic().getEvidenceCode() ).isNull();
    }

    /**
     * An unknown code is a 400, not a silent drop. Dropping it would leave the row on the server default while
     * the caller believed it had set one — the failure this field exists to end.
     */
    @Test
    @WithMockUser
    public void testCommitCurationRejectsUnknownEvidenceCode() {
        stubCommitOk();
        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"t7\",\"category\":{\"label\":\"disease\"},"
                + "\"value\":{\"label\":\"glioma\"},\"evidenceCode\":\"BOGUS\"}]}}";
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) ).contains( "tags[clientRef=t7].evidenceCode" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /** The preflight enforces the same gate, so a client catches a bad code on the dry run. */
    @Test
    @WithMockUser
    public void testPreflightRejectsUnknownEvidenceCode() {
        stubCommitOk();
        String body = "{\"tags\":{\"items\":[{\"freeTextIntended\":true,\"clientRef\":\"t7\",\"category\":{\"label\":\"disease\"},"
                + "\"value\":{\"label\":\"glioma\"},\"evidenceCode\":\"BOGUS\"}]}}";
        assertThat( target( "/datasets/1/curation/preflight" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
    }

    /**
     * A design factor-value statement carries its own code, normalized to the enum name. Sent lowercase here:
     * the apply compares the proposed code against the stored uppercase one, so an un-normalized {@code "iea"}
     * would read as a change on every re-send.
     */
    @Test
    @WithMockUser
    public void testCommitCurationDesignStatementCarriesEvidenceCode() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( currentDesign() );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );
        when( expressionExperimentService.previewDesignChange( eq( ee ), any() ) )
                .thenReturn( new ubic.gemma.model.expression.experiment.DesignPreflightReport() );
        when( expressionExperimentService.commitCuration( eq( ee ), any(), eq( false ) ) )
                .thenReturn( new ubic.gemma.persistence.service.expression.experiment.CurationCommitResult() );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"clientRef\":\"F1\",\"name\":\"genotype\","
                + "\"category\":{\"label\":\"genotype\"},\"factorValues\":{\"items\":[{\"clientRef\":\"FV1\","
                + "\"statements\":{\"items\":[{\"clientRef\":\"S1\",\"subject\":{\"label\":\"Utrn\"},"
                + "\"evidenceCode\":\"iea\"}]}}]}}]}}}";
        assertThat( target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );

        ArgumentCaptor<ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest> cap = curationCaptor();
        verify( expressionExperimentService ).commitCuration( eq( ee ), cap.capture(), eq( false ) );
        assertThat( cap.getValue().getProposedDesign().getExperimentalFactors() )
                .filteredOn( f -> "genotype".equals( f.getName() ) )
                .singleElement()
                .satisfies( f -> assertThat( f.getValues().get( 0 ).getStatements() ).singleElement()
                        .satisfies( s -> assertThat( s.getEvidenceCode() ).isEqualTo( "IEA" ) ) );
    }

    /** A bad code inside the design tree is rejected too, located in the design tree. */
    @Test
    @WithMockUser
    public void testCommitCurationRejectsUnknownEvidenceCodeOnADesignStatement() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getExperimentalDesignValueObject( ee ) ).thenReturn( currentDesign() );
        when( expressionExperimentService.thawBioAssays( ee ) ).thenReturn( ee );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"clientRef\":\"F1\",\"name\":\"genotype\","
                + "\"category\":{\"label\":\"genotype\"},\"factorValues\":{\"items\":[{\"clientRef\":\"FV1\","
                + "\"statements\":{\"items\":[{\"clientRef\":\"S1\",\"subject\":{\"label\":\"Utrn\"},"
                + "\"evidenceCode\":\"BOGUS\"}]}}]}}]}}}";
        try ( Response r = target( "/datasets/1/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( 400 );
            assertThat( r.readEntity( String.class ) )
                    .contains( "design.factors[clientRef=F1].factorValues[clientRef=FV1].statements[clientRef=S1].evidenceCode" );
        }
        verify( expressionExperimentService, never() ).commitCuration( any(), any(), anyBoolean() );
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

    /**
     * The bulk route lives at the LITERAL `/datasets/pipelineStatus` with ids in a query param.
     * `/datasets/{datasets}/pipelineStatus` would be the same JAX-RS template as the
     * single-dataset route — a path parameter's name does not distinguish it — so this test also
     * pins that the two coexist rather than shadowing one another.
     */
    @Test
    @WithMockUser
    public void testGetDatasetsPipelineStatusReturnsAnEntryPerDatasetAlongsideTheSingleRoute() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );

        assertThat( target( "/datasets/pipelineStatus" ).queryParam( "datasets", "1" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .satisfies( rows -> org.assertj.core.api.Assertions
                        .assertThat( rows.get( 0 ).get( "datasetId" ) ).isEqualTo( 1 ) );

        // the single-dataset route still answers on its own template
        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    @WithMockUser
    public void testPipelineStatusCarriesTheEffectiveTriageVerdictAndJudgeKind() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage triage =
                new ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage();
        triage.setVerdict( ubic.gemma.model.common.auditAndSecurity.curation.TriageVerdict.MustFix );
        triage.setJudgeKind( ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind.CURATOR );
        when( annotationSetTriageService.effectiveForInvestigationIds( any() ) )
                .thenReturn( Collections.singletonMap( 1L, triage ) );

        assertThat( target( "/datasets/pipelineStatus" ).queryParam( "datasets", "1" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .satisfies( rows -> {
                    org.assertj.core.api.Assertions.assertThat( rows.get( 0 ).get( "triageVerdict" ) ).isEqualTo( "must_fix" );
                    org.assertj.core.api.Assertions.assertThat( rows.get( 0 ).get( "triageJudgeKind" ) ).isEqualTo( "curator" );
                } );
    }

    /**
     * Nothing triaged leaves both fields null rather than defaulting — that is how a caller tells
     * "not triaged" from "triaged Fine", which a boolean could not express.
     */
    @Test
    @WithMockUser
    public void testPipelineStatusTriageIsNullWhenNothingHasBeenTriaged() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        when( annotationSetTriageService.effectiveForInvestigationIds( any() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.triageVerdict", null )
                .hasFieldOrPropertyWithValue( "data.triageJudgeKind", null );
    }

    /**
     * needsAttention is the pre-agent curator flag and must NOT move with triage: the two are
     * separate signals, and collapsing them was the thing this design turned down.
     */
    @Test
    @WithMockUser
    public void testTriageDoesNotTouchTheCuratorNeedsAttentionFlag() {
        mockPipelineFixture( ubic.gemma.model.expression.arrayDesign.TechnologyType.ONECOLOR );
        ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage triage =
                new ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage();
        triage.setVerdict( ubic.gemma.model.common.auditAndSecurity.curation.TriageVerdict.MustFix );
        triage.setJudgeKind( ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind.AGENT );
        when( annotationSetTriageService.effectiveForInvestigationIds( any() ) )
                .thenReturn( Collections.singletonMap( 1L, triage ) );

        assertThat( target( "/datasets/1/pipelineStatus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.triageVerdict", "must_fix" )
                .hasFieldOrPropertyWithValue( "data.needsAttention", false );
    }

    /**
     * `?original=true` must actually route. A declared parameter that is quietly ignored is the failure mode
     * uib caught on element search — the caller sees a 200 and a plausible body, and cannot tell that the
     * question they asked was dropped.
     */
    @Test
    @WithMockUser
    public void testPlatformsOriginalRoutesToTheOriginalPlatforms() {
        ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject used =
                new ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject( 1L );
        used.setShortName( "GPL_GENERIC" );
        ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject original =
                new ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject( 2L );
        original.setShortName( "GPL_SUBMITTED" );
        when( arrayDesignService.loadValueObjectsForEE( any() ) ).thenReturn( Collections.singletonList( used ) );
        when( arrayDesignService.loadOriginalPlatformValueObjectsForEE( any() ) ).thenReturn( Collections.singletonList( original ) );

        try ( Response r = target( "/datasets/1/platforms" ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data[0].shortName", "GPL_GENERIC" );
        }

        try ( Response r = target( "/datasets/1/platforms" ).queryParam( "original", true ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data[0].shortName", "GPL_SUBMITTED" );
        }
    }

    /** An unswitched dataset answers with an empty list, never with its current platform. */
    @Test
    @WithMockUser
    public void testPlatformsOriginalIsEmptyRatherThanEchoingTheCurrentPlatform() {
        when( arrayDesignService.loadOriginalPlatformValueObjectsForEE( any() ) ).thenReturn( Collections.emptyList() );

        try ( Response r = target( "/datasets/1/platforms" ).queryParam( "original", true ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( 200 );
            assertThat( r.readEntity( String.class ) ).asInstanceOf( json() )
                    .hasPathWithValue( "$.data", Collections.emptyList() )
                    // the point of the test: not merely absent, but not the current platform either
                    .doesNotHavePath( "$.data[0]" );
        }
    }

    /**
     * The document is served as an object, not as the string it is stored as. A string would make
     * every consumer parse it themselves, and the envelope around it is already JSON.
     */
    @Test
    public void testGetDatasetSourceMetadata() {
        ee.setId( 1L );
        ee.setShortName( "GSE0001" );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getSourceMetadata( ee ) ).thenReturn(
                "{\"schemaVersion\":1,\"source\":\"GEO\",\"sampleCount\":2,"
                        + "\"samples\":[{\"accession\":\"GSM1\",\"characteristics\":{\"tissue\":\"Hypothalamus\"}}]}" );

        try ( Response r = target( "/datasets/1/sourceMetadata" ).request().get() ) {
            assertThat( r ).hasStatus( Response.Status.OK );
            JsonNode data = r.readEntity( JsonNode.class ).get( "data" );
            assertThat( data.isObject() )
                    .withFailMessage( "the document must arrive as an object, not a quoted string" )
                    .isTrue();
            assertThat( data.get( "sampleCount" ).asInt() ).isEqualTo( 2 );
            assertThat( data.get( "samples" ).get( 0 ).get( "characteristics" ).get( "tissue" ).asText() )
                    .isEqualTo( "Hypothalamus" );
        }
    }

    /**
     * 🛑 Nothing harvested is the normal state for most of the corpus, not an error. A 404 here
     * would be indistinguishable from a dataset that does not exist, and would make an ordinary
     * experiment look broken in the curation UI.
     */
    @Test
    public void testGetDatasetSourceMetadataWhenNoneHasBeenHarvested() {
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getSourceMetadata( ee ) ).thenReturn( null );

        try ( Response r = target( "/datasets/1/sourceMetadata" ).request().get() ) {
            assertThat( r ).hasStatus( Response.Status.OK );
            assertThat( r.readEntity( JsonNode.class ).get( "data" ).isNull() ).isTrue();
        }
    }

    /** A dataset that does not exist is still a 404, which is what the null case must not look like. */
    @Test
    public void testGetDatasetSourceMetadataForAnUnknownDataset() {
        when( expressionExperimentService.load( 404L ) ).thenReturn( null );
        assertThat( target( "/datasets/404/sourceMetadata" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    /**
     * 🛑 A subset with no factor values is normal, not an error. Single-cell subsets are cut from a
     * cell-level characteristic and never carry one, so the map lookup feeding the VO misses and
     * hands over null — which made /subSetGroups a 500 on exactly those datasets (44580 failed,
     * factor-cut 38390 succeeded).
     */
    @Test
    public void testSubsetWithNoFactorValuesYieldsAnEmptyListNotANullPointer() {
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( 42L );
        subset.setName( "GSE1 - astrocyte" );
        subset.setSourceExperiment( ee );

        DatasetsWebService.ExpressionExperimentSubsetWithFactorValuesObject vo =
                new DatasetsWebService.ExpressionExperimentSubsetWithFactorValuesObject(
                        subset, null, null, false, null );

        assertThat( vo.getFactorValues() ).isEmpty();
    }
}
