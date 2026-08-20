/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import ubic.gemma.core.security.SecurityService;
import ubic.gemma.core.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchConfound;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.filter.NoDesignElementsException;
import ubic.gemma.core.analysis.preprocess.svd.SVDResult;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.service.OutlierFlaggingService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueNeedsAttentionService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisRemoveTaskCommand;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.BatchInfoFetchTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentPlatformSwitchTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.GeeqTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.SvdTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentLoadTaskCommand;
import ubic.gemma.core.tasks.analysis.expression.PreprocessTaskCommand;
import ubic.gemma.core.analysis.service.DifferentialExpressionAnalysisResultListFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionExperimentDataFileType;
import ubic.gemma.core.loader.expression.singleCell.metadata.CellLevelCharacteristicsWriter;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.ontology.OntologyTermValidator;
import ubic.gemma.core.ontology.TermCanonicalization;
import ubic.gemma.core.ontology.TermViolation;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.analysis.CellTypeAssignmentValueObject;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetailsValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchCorrectionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationNoteUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DatasetShortNameChangedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedLinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedMeanVarianceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedMissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedPCAAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedSampleCorrelationAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MeanVarianceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleCorrelationAnalysisEvent;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.description.DatasetPublicationValueObject;
import ubic.gemma.model.common.description.PublicationAssociationSource;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayUtils;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.core.analysis.service.ExpressionDataDeleterService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.common.description.PublicationAssertion;
import ubic.gemma.persistence.service.common.description.PublicationAssociationConflictException;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.CurationCommitRequest;
import ubic.gemma.persistence.service.expression.experiment.CurationCommitResult;
import ubic.gemma.persistence.service.expression.experiment.DesignCommitPlan;
import ubic.gemma.model.common.measurement.MeasurementValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.*;
import ubic.gemma.rest.annotations.CacheControl;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.util.*;
import ubic.gemma.rest.util.args.*;

import org.springframework.lang.Nullable;
import org.springframework.lang.NonNullApi;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectUtils.getBatchEffectType;
import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.*;
import static ubic.gemma.persistence.util.IdentifiableUtils.toIdentifiableSet;
import static ubic.gemma.rest.util.MediaTypeUtils.negotiate;
import static ubic.gemma.rest.util.MediaTypeUtils.withQuality;
import static ubic.gemma.rest.util.Responders.paginateByCursor;
import static ubic.gemma.rest.util.Responders.respond;
import static ubic.gemma.rest.util.Responders.sendfile;

/**
 * RESTful interface for datasets.
 *
 * @author tesarst
 */
@Service
@Path("/datasets")
@Slf4j
public class DatasetsWebService {

    public static final String TEXT_TAB_SEPARATED_VALUES_UTF8 = "text/tab-separated-values; charset=UTF-8";
    public static final MediaType TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE = new MediaType( "text", "tab-separated-values", "UTF-8" );

    /**
     * <a href="https://www.10xgenomics.com/support/software/cell-ranger/latest/analysis/outputs/cr-outputs-mex-matrices">Cell Ranger Feature Barcode Matrices (MEX Format)</a>
     */
    public static final String APPLICATION_10X_MEX = "application/vnd.10xgenomics.mex";
    public static final MediaType APPLICATION_10X_MEX_TYPE = new MediaType( "application", "vnd.10xgenomics.mex" );

    private static final String SEARCH_TIMEOUT_DESCRIPTION = "The search has timed out. This can only occur if the `search` parameter is provided. It can generally be resolved by reattempting the search 30 seconds later. Lookup the `Retry-After` header for the recommended delay.";

    private static final int MAX_DATASETS_CATEGORIES = 200;
    private static final int MAX_DATASETS_ANNOTATIONS = 5000;

    // fields allowed to be excluded
    private static final Set<String> SCD_ALLOWED_EXCLUDE_FIELDS = new HashSet<>( Arrays.asList( "cellIds", "bioAssayIds", "cellTypeAssignments.cellTypeIds", "cellLevelCharacteristics.characteristicIds" ) );
    private static final Set<String> ANNOTATION_ALLOWED_EXCLUDE_FIELDS = Collections.singleton( "parentTerms" );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private QuantitationTypeArgService quantitationTypeArgService;
    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;
    @Autowired
    private DatasetArgService datasetArgService;
    @Autowired
    private TaxonArgService taxonArgService;
    @Autowired
    private GeneArgService geneArgService;
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;
    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;
    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;
    @Autowired
    private DifferentialExpressionAnalysisResultListFileService differentialExpressionAnalysisResultListFileService;
    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;
    @Autowired
    private AccessDecisionManager accessDecisionManager;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private EntityUrlBuilder entityUrlBuilder;
    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private AnnotationSetsWebService annotationSetsWebService;
    /** Used by the snapshot/restore pair to load a stored payload; the delegating routes go through the web service above. */
    @Autowired
    private AnnotationSetService annotationSetService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private GeeqService geeqService;
    @Autowired
    private TicketsWebService ticketsWebService;
    @Autowired
    private GroupsWebService groupsWebService;
    @Autowired
    private TicketService ticketService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private BioAssayService bioAssayService;
    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private OutlierFlaggingService outlierFlaggingService;
    @Autowired
    private OutlierDetectionService outlierDetectionService;
    @Autowired
    private FactorValueService factorValueService;
    @Autowired
    private FactorValueNeedsAttentionService factorValueNeedsAttentionService;
    @Autowired
    private ExpressionDataDeleterService expressionDataDeleterService;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    @Autowired
    private OntologyTermValidator ontologyTermValidator;

    /**
     * When {@code true} (default), a term URI that resolves nowhere (Gemma nor OLS) because OLS could not be
     * reached is treated as a blocking validation failure. When {@code false}, such unverified terms are
     * allowed through (a transient OLS outage does not block curators), while genuine mismatches and
     * fabricated URIs are still rejected.
     */
    @org.springframework.beans.factory.annotation.Value("${gemma.ontology.validation.olsFailClosed}")
    private boolean ontologyValidationOlsFailClosed;

    @Context
    private UriInfo uriInfo;

    @GZIP
    @GET
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all datasets", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentWithSearchResultValueObject> getDatasets( // Params:
            @Parameter(description = "If specified, `sort` will default to `-searchResult.score` instead of `+id`. Note that sorting by `searchResult.score` is only valid if a query is specified.") @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg, // Optional, default 20
            @Parameter(schema = @Schema(defaultValue = "+id")) @QueryParam("sort") SortArg<ExpressionExperiment> sortArg // Optional, default +id
    ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filterArg, null, inferredTerms );
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        Slice<ExpressionExperimentWithSearchResultValueObject> payload;
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        if ( query != null ) {
            List<Long> ids;
            Sort sort;
            if ( sortArg == null || sortArg.getValue().getOrderBy().equals( "searchResult.score" ) ) {
                Sort.Direction direction;
                if ( sortArg != null ) {
                    direction = sortArg.getValue().getDirection() == SortArg.Sort.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
                } else {
                    direction = Sort.Direction.DESC;
                }
                sort = Sort.by( null, "searchResult.score", direction, Sort.NullMode.LAST );
                ids = new ArrayList<>( expressionExperimentService.loadIdsWithCache( filters, null ) );
                Map<Long, Double> scoreById = new HashMap<>();
                ids.retainAll( datasetArgService.getIdsForSearchQuery( query, scoreById, warnings ) );
                // sort is stable, so the order of IDs with the same score is preserved
                ids.sort( Comparator.comparing( scoreById::get, direction == Sort.Direction.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder() ) );
            } else {
                sort = datasetArgService.getSort( sortArg );
                ids = new ArrayList<>( expressionExperimentService.loadIdsWithCache( filters, sort ) );
                ids.retainAll( datasetArgService.getIdsForSearchQuery( query, warnings ) );
            }

            // slice the ranked IDs
            List<Long> idsSlice = sliceIds( ids, offset, limit );

            // now highlight the results in the slice. With the Phase-3 Step-5 wiring, the
            // HibernateSearchSource projects the projectable text fields of each hit and routes
            // them through this Highlighter; DefaultHighlighter returns the matched value verbatim
            // under its field name (no span tagging yet — that pairs with the Step-6 reindex when
            // we flip fields to highlightable = Highlightable.ANY for the HS 7 native projection).
            ubic.gemma.core.search.Highlighter highlighter = new ubic.gemma.core.search.DefaultHighlighter();
            List<SearchResult<ExpressionExperiment>> results = datasetArgService.getResultsForSearchQuery( query, highlighter, warnings );
            Map<Long, SearchResult<ExpressionExperiment>> resultById = results.stream().collect( Collectors.toMap( SearchResult::getResultId, e -> e ) );

            List<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjectsByIdsWithRelationsAndCache( idsSlice );
            payload = new Slice<>( vos, sort, offset, limit, ( long ) ids.size() )
                    .map( vo -> {
                        EntityUrlBuilder.EntityUrl<?> entityUrl = getResultObjectUrlSafely( resultById.get( vo.getId() ) );
                        return new ExpressionExperimentWithSearchResultValueObject( vo, resultById.get( vo.getId() ), entityUrl.toUriString(), entityUrl.isExternal() );
                    } );
        } else {
            Sort sort = sortArg != null ? datasetArgService.getSort( sortArg ) : datasetArgService.getSort( SortArg.valueOf( "+id" ) );
            payload = expressionExperimentService.loadValueObjectsWithCache( filters, sort, offset, limit )
                    .map( ExpressionExperimentWithSearchResultValueObject::new );
        }
        return paginate( payload, query != null ? query.getValue() : null, filters, new String[] { "id" }, inferredTerms )
                .addWarnings( warnings, "query", LocationType.QUERY );
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class ExpressionExperimentWithSearchResultValueObject extends ExpressionExperimentValueObject {

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        SearchWebService.SearchResultValueObject<ExpressionExperimentValueObject> searchResult;

        public ExpressionExperimentWithSearchResultValueObject( ExpressionExperimentValueObject vo ) {
            super( vo );
            this.searchResult = null;
        }

        public ExpressionExperimentWithSearchResultValueObject( ExpressionExperimentValueObject vo, @Nullable SearchResult<ExpressionExperiment> result, String resultObjectUrl, boolean resultObjectUrlExternal ) {
            super( vo );
            if ( result != null ) {
                this.searchResult = new SearchWebService.SearchResultValueObject<>( result.withResultObject( null ), resultObjectUrl, resultObjectUrlExternal );
            } else {
                this.searchResult = null;
            }
        }
    }

    @Nullable
    private EntityUrlBuilder.EntityUrl<?> getResultObjectUrlSafely( SearchResult<?> searchResult ) {
        try {
            return entityUrlBuilder
                    .fromHostUrl()
                    .entity( searchResult.getResultType(), searchResult.getResultId() )
                    .rest();
        } catch ( UnsupportedEntityUrlException e ) {
            return null;
        }
    }

    /**
     * Typeahead-style search for datasets. Wraps the global {@link ubic.gemma.core.search.SearchService}
     * filtered to {@link ExpressionExperiment} and projects each hit to a thin
     * {@link DatasetSearchHitValueObject}. Intended for the curation-UI browser import dialog
     * (see {@code GEMMA_UI_ENDPOINT_GAP.md} §3i).
     *
     * @deprecated redundant with {@code GET /datasets?query=...} (the paginated catalogue runs the
     * same search), which every known client uses instead — no caller of this endpoint was found
     * across gemma-ui, gemma-curation-agents, or gemma.R. Scheduled for removal in the 2.10 release.
     */
    @Deprecated
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Typeahead search for datasets by short name, accession, or title",
            deprecated = true,
            description = "Deprecated: use `GET /datasets?query=...` instead (same search, paginated); scheduled for removal in 2.10. "
                    + "Returns a thin list of dataset hits ranked by search score.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The query parameter is missing or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<List<DatasetSearchHitValueObject>> searchDatasets(
            @Parameter(description = "The search query (e.g. a short name, accession, or fragment of the title). Required.", required = true) @QueryParam("query") QueryArg query,
            @Parameter(description = "Maximum number of hits to return.", schema = @Schema(type = "integer", defaultValue = "20", minimum = "1")) @QueryParam("limit") @DefaultValue("20") LimitArg limit
    ) {
        if ( query == null ) {
            throw new BadRequestException( "A query must be supplied." );
        }
        int max = limit.getValue( SearchWebService.MAX_SEARCH_RESULTS );
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        // SearchService already returns hits ordered by descending score.
        List<SearchResult<ExpressionExperiment>> results = datasetArgService.getResultsForSearchQuery( query, null, warnings );
        List<Long> idsRanked = results.stream()
                .map( SearchResult::getResultId )
                .limit( max )
                .collect( Collectors.toList() );
        if ( idsRanked.isEmpty() ) {
            return respond( Collections.<DatasetSearchHitValueObject>emptyList() )
                    .addWarnings( warnings, "query", LocationType.QUERY );
        }
        Map<Long, Double> scoreById = new HashMap<>();
        for ( SearchResult<ExpressionExperiment> r : results ) {
            scoreById.putIfAbsent( r.getResultId(), r.getScore() );
        }
        // ACL filtering is applied by the @Secured annotation on this loader; results the
        // caller cannot read are silently dropped, matching the global /search behaviour.
        List<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjectsByIds( idsRanked, true );
        List<DatasetSearchHitValueObject> hits = vos.stream()
                .map( vo -> new DatasetSearchHitValueObject( vo, scoreById.get( vo.getId() ) ) )
                .collect( Collectors.toList() );
        return respond( hits ).addWarnings( warnings, "query", LocationType.QUERY );
    }

    /**
     * Thin dataset projection for the typeahead {@code /datasets/search} endpoint. Carries
     * only the fields the curation-UI browser import dialog needs to render a result chip
     * (short name + title + taxon + score), to keep the payload small enough for
     * keystroke-rate fetching.
     */
    @Value
    public static class DatasetSearchHitValueObject {

        Long id;
        @Nullable
        String shortName;
        @Nullable
        String name;
        @Nullable
        String accession;
        /** Common name of the taxon, suitable for a typeahead chip. */
        @Nullable
        String taxon;
        @Nullable
        Long taxonId;
        /** Search relevance score; higher is better. May be null if the hit was not directly scored. */
        @Nullable
        Double score;

        public DatasetSearchHitValueObject( ExpressionExperimentValueObject vo, @Nullable Double score ) {
            this.id = vo.getId();
            this.shortName = vo.getShortName();
            this.name = vo.getName();
            this.accession = vo.getAccession();
            this.taxon = vo.getTaxon();
            this.taxonId = vo.getTaxonObject() != null ? vo.getTaxonObject().getId() : null;
            this.score = score;
        }
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count datasets matching the provided query and filter", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public ResponseDataObject<Long> getNumberOfDatasets(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter
    ) {
        Filters filters = datasetArgService.getFilters( filter );
        Set<Long> extraIds;
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query, warnings );
        } else {
            extraIds = null;
        }
        return respond( expressionExperimentService.countWithCache( filters, extraIds ) )
                .addWarnings( warnings, "query", LocationType.QUERY );
    }

    @GET
    @Path("/samples/count")
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count distinct biomaterials (samples) across datasets matching the provided filter",
            description = "Corpus-wide sample-count aggregate, used by the public home page tile. "
                    + "Same `filter` grammar as `GET /datasets`. Cached on the same TTL as the other usage-stats endpoints.")
    public ResponseDataObject<Long> getNumberOfSamples(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter
    ) {
        Filters filters = datasetArgService.getFilters( filter );
        return respond( expressionExperimentService.countBioMaterials( filters ) );
    }

    public interface UsageStatistics {
        Long getNumberOfExpressionExperiments();
    }

    @GZIP
    @GET
    @Path("/platforms")
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve usage statistics of platforms among datasets matching the provided query and filter",
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public QueriedAndFilteredAndInferredAndLimitedResponseDataObject<ArrayDesignWithUsageStatisticsValueObject> getDatasetsPlatformsUsageStatistics(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("limit") @DefaultValue("50") LimitArg limit
    ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, null, inferredTerms );
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query, warnings );
        } else {
            extraIds = null;
        }
        Integer l = limit.getValueNoMaximum();
        Map<TechnologyType, Long> tts = expressionExperimentService.getTechnologyTypeUsageFrequency( filters, extraIds );
        Map<ArrayDesign, Long> ads = expressionExperimentService.getArrayDesignUsedOrOriginalPlatformUsageFrequency( filters, extraIds, l );
        List<ArrayDesignValueObject> adsVos = arrayDesignService.loadValueObjects( ads.keySet() );
        Map<Long, Long> countsById = ads.entrySet().stream().collect( Collectors.toMap( e -> e.getKey().getId(), Map.Entry::getValue ) );
        List<ArrayDesignWithUsageStatisticsValueObject> results =
                adsVos.stream()
                        .map( e -> new ArrayDesignWithUsageStatisticsValueObject( e, countsById.get( e.getId() ), tts.getOrDefault( TechnologyType.valueOf( e.getTechnologyType() ), 0L ) ) )
                        .sorted( Comparator.comparing( UsageStatistics::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) )
                        .collect( Collectors.toList() );
        return top( results, query != null ? query.getValue() : null, filters, new String[] { "id" }, Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, Sort.NullMode.LAST, "numberOfExpressionExperiments" ), l, inferredTerms )
                .addWarnings( warnings, "query", LocationType.QUERY );
    }

    @GET
    @Path("/platforms/refresh")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Retrieve refreshed experiment-to-platform associations.",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = { @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(ref = "QueriedAndFilteredAndInferredAndLimitedResponseDataObjectArrayDesignWithUsageStatisticsValueObject"))) })
    public Response refreshDatasetsPlatforms(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("limit") @DefaultValue("50") LimitArg limit
    ) {
        tableMaintenanceUtil.evictEe2AdQueryCache();
        return Response.created( URI.create( "/datasets/platforms" ) )
                .entity( getDatasetsPlatformsUsageStatistics( query, filter, limit ) )
                .build();
    }

    @Value
    public static class CategoryWithUsageStatisticsValueObject implements UsageStatistics {
        String classUri;
        String className;
        Long numberOfExpressionExperiments;
    }

    @GET
    @Path("/categories")
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve usage statistics of categories among datasets matching the provided query and filter",
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndInferredAndLimitedResponseDataObject<CategoryWithUsageStatisticsValueObject> getDatasetsCategoriesUsageStatistics(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit,
            @Parameter(description = "Excluded category URIs.", hidden = true) @QueryParam("excludedCategories") StringArrayArg excludedCategoryUris,
            @Parameter(description = "Exclude free-text categories (i.e. those with null URIs).", hidden = true) @QueryParam("excludeFreeTextCategories") @DefaultValue("false") Boolean excludeFreeTextCategories,
            @Parameter(description = "Excluded term URIs; this list is expanded with subClassOf inference.", hidden = true) @QueryParam("excludedTerms") StringArrayArg excludedTermUris,
            @Parameter(description = "Exclude free-text terms (i.e. those with null URIs).", hidden = true) @QueryParam("excludeFreeTextTerms") @DefaultValue("false") Boolean excludeFreeTextTerms,
            @Parameter(description = "Exclude uncategorized terms.", hidden = true) @QueryParam("excludeUncategorizedTerms") @DefaultValue("false") Boolean excludeUncategorizedTerms,
            @Parameter(description = "Retain the categories applicable to terms mentioned in the `filter` parameter even if they are excluded by `excludedCategories` or `excludedTerms`.", hidden = true) @QueryParam("retainMentionedTerms") @DefaultValue("false") Boolean retainMentionedTerms
    ) {
        // ensure that implied terms are retained in the usage frequency
        Collection<OntologyTerm> mentionedTerms = retainMentionedTerms ? new HashSet<>() : null;
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, mentionedTerms, inferredTerms );
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query, warnings );
        } else {
            extraIds = null;
        }
        int maxResults = limit.getValue( MAX_DATASETS_CATEGORIES );
        List<CategoryWithUsageStatisticsValueObject> results = expressionExperimentService.getCategoriesUsageFrequency(
                        filters,
                        extraIds,
                        datasetArgService.getExcludedUris( excludedCategoryUris, excludeFreeTextCategories, excludeUncategorizedTerms ),
                        datasetArgService.getExcludedUris( excludedTermUris, excludeFreeTextTerms, excludeUncategorizedTerms ),
                        mentionedTerms != null ? mentionedTerms.stream().map( OntologyTerm::getUri ).collect( Collectors.toSet() ) : null,
                        maxResults )
                .entrySet()
                .stream()
                .map( e -> new CategoryWithUsageStatisticsValueObject( e.getKey().getCategoryUri(), e.getKey().getCategory(), e.getValue() ) )
                .sorted( Comparator.comparing( UsageStatistics::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) )
                .collect( Collectors.toList() );
        return top( results, query != null ? query.getValue() : null, filters, new String[] { "classUri", "className" }, Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, Sort.NullMode.LAST, "numberOfExpressionExperiments" ), maxResults, inferredTerms )
                .addWarnings( warnings, "query", LocationType.QUERY );
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties({ "expressionExperimentCount", "numberOfSwitchedExpressionExperiments" })
    public static class ArrayDesignWithUsageStatisticsValueObject extends ArrayDesignValueObject implements UsageStatistics {

        Long numberOfExpressionExperimentsForTechnologyType;

        public ArrayDesignWithUsageStatisticsValueObject( ArrayDesignValueObject arrayDesign, Long numberOfExpressionExperiments, Long numberOfExpressionExperimentsForTechnologyType ) {
            super( arrayDesign );
            setExpressionExperimentCount( numberOfExpressionExperiments );
            this.numberOfExpressionExperimentsForTechnologyType = numberOfExpressionExperimentsForTechnologyType;
        }
    }

    @GET
    @GZIP
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Path("/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve usage statistics of annotations among datasets matching the provided query and filter",
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndInferredAndLimitedResponseDataObject<AnnotationWithUsageStatisticsValueObject> getDatasetsAnnotationsUsageStatistics(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @Parameter(description = "List of fields to exclude from the payload. Only `parentTerms` can be excluded.") @QueryParam("exclude") ExcludeArg<AnnotationWithUsageStatisticsValueObject> exclude,
            @Parameter(description = "Maximum number of annotations to returned; capped at " + MAX_DATASETS_ANNOTATIONS + ".", schema = @Schema(type = "integer", minimum = "1", maximum = "" + MAX_DATASETS_ANNOTATIONS)) @QueryParam("limit") LimitArg limitArg,
            @Parameter(description = "Minimum number of associated datasets to report an annotation. If used, the limit will default to " + MAX_DATASETS_ANNOTATIONS + ".") @QueryParam("minFrequency") Integer minFrequency,
            @Parameter(description = "A category URI to restrict reported annotations. If unspecified, annotations from all categories are reported. If empty, uncategorized terms are reported.") @QueryParam("category") String category,
            @Parameter(description = "Excluded category URIs.", hidden = true) @QueryParam("excludedCategories") StringArrayArg excludedCategoryUris,
            @Parameter(description = "Exclude free-text categories (i.e. those with null URIs).", hidden = true) @QueryParam("excludeFreeTextCategories") @DefaultValue("false") Boolean excludeFreeTextCategories,
            @Parameter(description = "Excluded term URIs; this list is expanded with subClassOf inference.", hidden = true) @QueryParam("excludedTerms") StringArrayArg excludedTermUris,
            @Parameter(description = "Exclude free-text terms (i.e. those with null URIs).", hidden = true) @QueryParam("excludeFreeTextTerms") @DefaultValue("false") Boolean excludeFreeTextTerms,
            @Parameter(description = "Exclude uncategorized terms.", hidden = true) @QueryParam("excludeUncategorizedTerms") @DefaultValue("false") Boolean excludeUncategorizedTerms,
            @Parameter(description = "Retain terms mentioned in the `filter` parameter even if they don't meet the `minFrequency` threshold or are excluded via `excludedCategories` or `excludedTerms`.", hidden = true) @QueryParam("retainMentionedTerms") @DefaultValue("false") Boolean retainMentionedTerms,
            @Parameter(description = "Include statement predicates in usage statistics.", hidden = true) @QueryParam("includePredicates") @DefaultValue("false") Boolean includePredicates,
            @Parameter(description = "Include statement objects in usage statistics.", hidden = true) @QueryParam("includeObjects") @DefaultValue("false") Boolean includeObjects
    ) {
        boolean excludeParentTerms = exclude != null && exclude.getValue( ANNOTATION_ALLOWED_EXCLUDE_FIELDS ).contains( "parentTerms" );
        // if a minFrequency is requested, use the hard cap, otherwise use 100 as a reasonable default
        int limit = limitArg != null ? limitArg.getValue( MAX_DATASETS_ANNOTATIONS ) : minFrequency != null ? MAX_DATASETS_ANNOTATIONS : 100;
        if ( minFrequency != null && minFrequency < 0 ) {
            throw new BadRequestException( "Minimum frequency must be positive." );
        }
        // ensure that implied terms are retained in the usage frequency
        Collection<OntologyTerm> mentionedTerms = retainMentionedTerms ? new HashSet<>() : null;
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        List<Throwable> queryWarnings = new ArrayList<>();
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query, queryWarnings );
        } else {
            extraIds = null;
        }
        if ( category != null && category.isEmpty() ) {
            category = ExpressionExperimentService.UNCATEGORIZED;
        }
        int timeoutMs = 30000;
        StopWatch timer = StopWatch.createStarted();
        Filters filters;
        List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> initialResults;
        try {
            filters = datasetArgService.getFilters( filter, mentionedTerms, inferredTerms, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
            initialResults = expressionExperimentService.getAnnotationsUsageFrequency(
                    filters,
                    extraIds,
                    category,
                    datasetArgService.getExcludedUris( excludedCategoryUris, excludeFreeTextCategories, excludeUncategorizedTerms ),
                    datasetArgService.getExcludedUris( excludedTermUris, excludeFreeTextTerms, excludeUncategorizedTerms ),
                    minFrequency != null ? minFrequency : 0,
                    mentionedTerms != null ? mentionedTerms.stream().map( OntologyTerm::getUri ).collect( Collectors.toSet() ) : null,
                    limit,
                    includePredicates, includeObjects,
                    Math.max( timeoutMs - timer.getTime(), 0 ),
                    TimeUnit.MILLISECONDS );
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( DateUtils.addSeconds( new Date(), 30 ), e );
        }
        List<AnnotationWithUsageStatisticsValueObject> results = new ArrayList<>();
        if ( !excludeParentTerms ) {
            // cache for visited parents (if two term share the same parent, we can save significant time generating the ancestors)
            Map<OntologyTerm, Set<OntologyTermValueObject>> visited = new HashMap<>();
            for ( ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm e : initialResults ) {
                Set<OntologyTermValueObject> parentTerms;
                if ( e.getTerm() != null && timer.getTime() < timeoutMs ) {
                    try {
                        parentTerms = getParentTerms( e.getTerm(), visited, Math.max( timeoutMs - timer.getTime(), 0 ) );
                    } catch ( TimeoutException ex ) {
                        log.warn( "Populating parent terms timed out, will stop populating those for the remaining results.", ex );
                        parentTerms = null;
                    }
                } else {
                    parentTerms = null;
                }
                results.add( new AnnotationWithUsageStatisticsValueObject( e.getCharacteristic(), e.getNumberOfExpressionExperiments(), parentTerms ) );
            }
        } else {
            for ( ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm e : initialResults ) {
                results.add( new AnnotationWithUsageStatisticsValueObject( e.getCharacteristic(), e.getNumberOfExpressionExperiments(), null ) );
            }
        }
        return top( results, query != null ? query.getValue() : null, filters, new String[] { "classUri", "className", "termUri", "termName" },
                Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, Sort.NullMode.LAST, "numberOfExpressionExperiments" ),
                limit, inferredTerms )
                .addWarnings( queryWarnings, "query", LocationType.QUERY );
    }

    private Set<OntologyTermValueObject> getParentTerms( OntologyTerm c, Map<OntologyTerm, Set<OntologyTermValueObject>> visited, long timeoutMs ) throws TimeoutException {
        return getParentTerms( c, new LinkedHashSet<>(), visited, timeoutMs, StopWatch.createStarted() );
    }

    private Set<OntologyTermValueObject> getParentTerms( OntologyTerm c, LinkedHashSet<OntologyTerm> stack, Map<OntologyTerm, Set<OntologyTermValueObject>> visited, long timeoutMs, StopWatch timer ) throws TimeoutException {
        Set<OntologyTermValueObject> results = new HashSet<>();
        for ( OntologyTerm t : ontologyService.getParents( Collections.singleton( c ), true, true, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ) ) {
            Set<OntologyTermValueObject> parentVos;
            if ( stack.contains( t ) ) {
                log.debug( "Detected a cycle when visiting " + t + ": " + stack.stream()
                        .map( ot -> ot.equals( t ) ? ot + "*" : ot.toString() )
                        .collect( Collectors.joining( " -> " ) ) + " -> " + t + "*" );
                continue;
            } else if ( visited.containsKey( t ) ) {
                parentVos = visited.get( t );
            } else {
                stack.add( t );
                parentVos = getParentTerms( t, stack, visited, timeoutMs, timer );
                stack.remove( t );
                visited.put( t, parentVos );
            }
            results.add( new OntologyTermValueObject( t, parentVos ) );
        }
        return results;
    }

    @Value
    @EqualsAndHashCode(of = { "uri" })
    public static class OntologyTermValueObject {

        String uri;
        String name;
        Set<OntologyTermValueObject> parentTerms;

        public OntologyTermValueObject( OntologyTerm ontologyTerm, Set<OntologyTermValueObject> parentTerms ) {
            this.uri = ontologyTerm.getUri();
            this.name = ontologyTerm.getLabel();
            this.parentTerms = parentTerms;
        }
    }

    /**
     * This is an aggregated entity across value URI and value, thus the {@code id} and {@code objectClass} are omitted.
     */
    @Value
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(value = { "id", "objectClass" })
    public static class AnnotationWithUsageStatisticsValueObject extends AnnotationValueObject implements UsageStatistics {

        /**
         * Number of times the characteristic is mentioned among matching datasets.
         */
        Long numberOfExpressionExperiments;

        /**
         * URIs of parent terms, or null if excluded.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Set<OntologyTermValueObject> parentTerms;

        public AnnotationWithUsageStatisticsValueObject( Characteristic c, Long numberOfExpressionExperiments, @Nullable Set<OntologyTermValueObject> parentTerms ) {
            super( c );
            this.numberOfExpressionExperiments = numberOfExpressionExperiments;
            this.parentTerms = parentTerms;
        }
    }

    @GET
    @Path("/annotations/count")
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count distinct annotation terms in use, optionally constrained by category",
            description = "Returns the number of distinct value+valueUri annotation terms seen across datasets matching the filter. "
                    + "Pass `category` as either an ontology URI (e.g. `http://purl.obolibrary.org/obo/UBERON_0001062`) "
                    + "or a category label (e.g. `disease`, `organism part`, `cell type`, `treatment`); empty means uncategorized. "
                    + "By default free-text characteristics (those without a `valueUri`) are counted as distinct terms; pass "
                    + "`excludeFreeText=true` to restrict the count to ontology-backed terms — typically the right answer "
                    + "for an \"ontology terms in use\" tile (curator-submitted strings like `lung tissue` / `Lung` / "
                    + "`lung biopsy from patient 3` inflate the count otherwise). "
                    + "Cheaper than walking `GET /datasets/annotations` and counting the payload — backed by the same usage-frequency "
                    + "query with `maxResults=0` (unlimited).",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<Long> getNumberOfAnnotations(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @Parameter(description = "Annotation category URI or label; empty for uncategorized; omitted for all categories.")
            @QueryParam("category") String category,
            @Parameter(description = "Minimum number of associated datasets per term (default 1).")
            @QueryParam("minFrequency") @DefaultValue("1") Integer minFrequency,
            @Parameter(description = "Exclude free-text characteristics (those with a null `valueUri`) from the count. Default false.")
            @QueryParam("excludeFreeText") @DefaultValue("false") Boolean excludeFreeText
    ) {
        if ( minFrequency < 0 ) {
            throw new BadRequestException( "Minimum frequency must be non-negative." );
        }
        if ( category != null && category.isEmpty() ) {
            category = ExpressionExperimentService.UNCATEGORIZED;
        }
        int timeoutMs = 30000;
        StopWatch timer = StopWatch.createStarted();
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Set<Long> extraIds;
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query, warnings );
        } else {
            extraIds = null;
        }
        // Same plumbing /datasets/annotations uses for `excludeFreeTextTerms`: appends the
        // FREE_TEXT sentinel to excludedTermUris so the DAO drops rows with null valueUri.
        Collection<String> excludedTermUris = datasetArgService.getExcludedUris( null, excludeFreeText, false );
        try {
            Filters filters = datasetArgService.getFilters( filter, null, inferredTerms, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
            List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> terms = expressionExperimentService.getAnnotationsUsageFrequency(
                    filters,
                    extraIds,
                    category,
                    null,
                    excludedTermUris,
                    minFrequency,
                    null,
                    0, // unlimited — we count rather than render
                    false, false,
                    Math.max( timeoutMs - timer.getTime(), 0 ),
                    TimeUnit.MILLISECONDS );
            return respond( ( long ) terms.size() )
                    .addWarnings( warnings, "query", LocationType.QUERY );
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( DateUtils.addSeconds( new Date(), 30 ), e );
        }
    }

    @GET
    @Path("/annotations/refresh")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve refreshed dataset annotations.",
            responses = {
                    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(ref = "QueriedAndFilteredAndInferredAndLimitedResponseDataObjectAnnotationWithUsageStatisticsValueObject")))
            })
    public Response refreshDatasetsAnnotations(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @Parameter(description = "List of fields to exclude from the payload. Only `parentTerms` can be excluded.") @QueryParam("exclude") ExcludeArg<AnnotationWithUsageStatisticsValueObject> exclude,
            @Parameter(description = "Maximum number of annotations to returned; capped at " + MAX_DATASETS_ANNOTATIONS + ".", schema = @Schema(type = "integer", minimum = "1", maximum = "" + MAX_DATASETS_ANNOTATIONS)) @QueryParam("limit") LimitArg limitArg,
            @Parameter(description = "Minimum number of associated datasets to report an annotation. If used, the limit will default to " + MAX_DATASETS_ANNOTATIONS + ".") @QueryParam("minFrequency") Integer minFrequency,
            @Parameter(description = "A category URI to restrict reported annotations. If unspecified, annotations from all categories are reported. If empty, uncategorized terms are reported.") @QueryParam("category") String category,
            @Parameter(description = "Excluded category URIs.", hidden = true) @QueryParam("excludedCategories") StringArrayArg excludedCategoryUris,
            @Parameter(description = "Exclude free-text categories (i.e. those with null URIs).", hidden = true) @QueryParam("excludeFreeTextCategories") @DefaultValue("false") Boolean excludeFreeTextCategories,
            @Parameter(description = "Excluded term URIs; this list is expanded with subClassOf inference.", hidden = true) @QueryParam("excludedTerms") StringArrayArg excludedTermUris,
            @Parameter(description = "Exclude free-text terms (i.e. those with null URIs).", hidden = true) @QueryParam("excludeFreeTextTerms") @DefaultValue("false") Boolean excludeFreeTextTerms,
            @Parameter(description = "Exclude uncategorized terms.", hidden = true) @QueryParam("excludeUncategorizedTerms") @DefaultValue("false") Boolean excludeUncategorizedTerms,
            @Parameter(description = "Retain terms mentioned in the `filter` parameter even if they don't meet the `minFrequency` threshold or are excluded via `excludedCategories` or `excludedTerms`.", hidden = true) @QueryParam("retainMentionedTerms") @DefaultValue("false") Boolean retainMentionedTerms,
            @Parameter(description = "Include statement predicates in usage statistics.", hidden = true) @QueryParam("includePredicates") @DefaultValue("false") Boolean includePredicates,
            @Parameter(description = "Include statement objects in usage statistics.", hidden = true) @QueryParam("includeObjects") @DefaultValue("false") Boolean includeObjects
    ) {
        tableMaintenanceUtil.evictEe2CQueryCache();
        return Response.created( URI.create( "/datasets/annotations" ) )
                .entity( getDatasetsAnnotationsUsageStatistics( query, filter, exclude, limitArg, minFrequency, category, excludedCategoryUris, excludeFreeTextCategories, excludedTermUris, excludeFreeTextTerms, excludeUncategorizedTerms, retainMentionedTerms, includePredicates, includeObjects ) )
                .build();
    }

    @GET
    @Path("/taxa")
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve taxa usage statistics for datasets matching the provided query and filter", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public QueriedAndFilteredAndInferredResponseDataObject<TaxonWithUsageStatisticsValueObject> getDatasetsTaxaUsageStatistics(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg
    ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filterArg, null, inferredTerms );
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query, warnings );
        } else {
            extraIds = null;
        }
        List<TaxonWithUsageStatisticsValueObject> payload = expressionExperimentService.getTaxaUsageFrequency( filters, extraIds )
                .entrySet().stream()
                .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                .map( e -> new TaxonWithUsageStatisticsValueObject( e.getKey(), e.getValue() ) )
                .collect( Collectors.toList() );
        return all( payload, query != null ? query.getValue() : null, filters, new String[] { "id" },
                Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, Sort.NullMode.LAST, "numberOfExpressionExperiments" ),
                inferredTerms )
                .addWarnings( warnings, "query", LocationType.QUERY );
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class TaxonWithUsageStatisticsValueObject extends TaxonValueObject implements UsageStatistics {

        Long numberOfExpressionExperiments;

        public TaxonWithUsageStatisticsValueObject( Taxon taxon, Long numberOfExpressionExperiments ) {
            super( taxon );
            this.numberOfExpressionExperiments = numberOfExpressionExperiments;
        }
    }

    /**
     * Retrieves all datasets matching the given identifiers.
     *
     * @param datasetsArg a list of identifiers, separated by commas (','). Identifiers can either be the
     *                    ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                    is more efficient.
     *                    <p>
     *                    Only datasets that user has access to will be available.
     *                    </p>
     *                    <p>
     *                    Do not combine different identifiers in one query.
     *                    </p>
     */
    @GET
    @Path("/{dataset}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets by their identifiers",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive -- passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (the user `sort` arg is currently ignored, pending the indexed-column audit in phase B); "
                    + "the path-derived dataset-id constraint is preserved on top of the user-supplied `?filter=`; `totalElements` is `null` by default (no count query per request). "
                    + "Mirrors GET /datasets/blacklisted step 1t.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    FilteredAndInferredAndPaginatedResponseDataObjectExpressionExperimentValueObject.class,
                                    FilteredAndInferredAndCursorPaginatedResponseDataObjectExpressionExperimentValueObject.class
                            }))),
            })
    public Object getDatasetsByIds( // Params:
            @PathParam("dataset") DatasetArrayArg datasetsArg, // Optional
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort, // Optional, default +id
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.")
            @QueryParam("cursor") CursorArg cursorArg
    ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, null, inferredTerms ).and( datasetArgService.getFilters( datasetsArg ) );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels step 1d /taxa/{taxon}/datasets and step 1t
            // /datasets/blacklisted). In cursor mode we currently force a +id sort
            // (DatasetArgService.getDatasetsByCursor) -- the DAO restricts cursors to
            // single-component id sorts until the index audit lands. The path-derived
            // dataset-id constraint composed into `filters` above still applies, so the
            // {dataset} path scope is enforced identically in both modes.
            CursorPage<ExpressionExperimentValueObject> page = datasetArgService.getDatasetsByCursor(
                    filters, cursorArg.getValue(), limit.getValue() );
            return new FilteredAndInferredAndCursorPaginatedResponseDataObject<>( page, filters, new String[] { "id" }, inferredTerms );
        }
        return paginate( expressionExperimentService::loadValueObjectsWithCache, filters, new String[] { "id" },
                datasetArgService.getSort( sort ), offset.getValue(), limit.getValue(), inferredTerms );
    }

    /**
     * Browse blacklisted datasets.
     */
    @GET
    @Path("/blacklisted")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retrieve all blacklisted datasets", hidden = true,
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination and consistency under writes): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive -- passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the result is always sorted by ascending `id` (the user `sort` arg is currently ignored, pending the indexed-column audit in phase B); "
                    + "the blacklist short-name/accession predicate is preserved on top of the user-supplied `?filter=`; `totalElements` is `null` by default (no count query per request). "
                    + "Mirrors GET /platforms/blacklisted step 1h.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    FilteredAndInferredAndPaginatedResponseDataObjectExpressionExperimentValueObject.class,
                                    FilteredAndInferredAndCursorPaginatedResponseDataObjectExpressionExperimentValueObject.class
                            }))),
            })
    public Object getBlacklistedDatasets(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit,
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.")
            @QueryParam("cursor") CursorArg cursorArg ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filterArg, null, inferredTerms );
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels step 1h /platforms/blacklisted). In cursor
            // mode we currently force a +id sort (DatasetArgService.getBlacklistedDatasetsByCursor) --
            // the DAO restricts cursors to single-component id sorts until the index audit lands.
            // The blacklist short-name/accession predicate is composed inside the DAO so the
            // blacklist scope is enforced identically in both modes.
            CursorPage<ExpressionExperimentValueObject> page = datasetArgService.getBlacklistedDatasetsByCursor(
                    filters, cursorArg.getValue(), limit.getValue() );
            return new FilteredAndInferredAndCursorPaginatedResponseDataObject<>( page, filters, new String[] { "id" }, inferredTerms );
        }
        return paginate( expressionExperimentService::loadBlacklistedValueObjects,
                filters, new String[] { "id" }, datasetArgService.getSort( sortArg ),
                offset.getValue(), limit.getValue(), inferredTerms );
    }

    /**
     * Retrieves platforms for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the platforms of a dataset", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<ArrayDesignValueObject>> getDatasetPlatforms( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        return respond( datasetArgService.getPlatforms( datasetArg ) );
    }

    /**
     * Retrieves the samples for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the samples of a dataset",
            description = "Legacy mode (no `cursor` parameter): returns the full unpaginated assay list in the existing shape. "
                    + "Cursor mode (available for consistency; a dataset's assay list stays small — single-cell size is in cells, not assays): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field along with a `limit`. "
                    + "In cursor mode the result is always sorted by ascending `id` (cursor mode forces a single-component id sort pending the indexed-column audit in phase B); "
                    + "the path-derived `expressionExperiment.id = ?` constraint is preserved; `totalElements` is `null` by default (no count query per request). "
                    + "The `quantitationType` and `useProcessedQuantitationType` query parameters narrow the assays to a specific `BioAssayDimension` and intentionally remain offset-mode "
                    + "(they sort by assay name and apply a dimension restriction that is not expressible as an `id`-only cursor); supplying `cursor` together with either of those is a `400`.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    ResponseDataObjectListBioAssayValueObject.class,
                                    CursorPaginatedResponseDataObjectBioAssayValueObject.class
                            }))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Object getDatasetSamples( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg,
            @QueryParam("useProcessedQuantitationType") boolean useProcessedQuantitationType,
            @Parameter(description = "Opaque keyset-pagination cursor token; not supported in combination with `quantitationType` or `useProcessedQuantitationType`.")
            @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Page size for cursor mode (ignored when no `cursor` is supplied).")
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        if ( cursorArg != null ) {
            // Mutual-exclusion: the QT-narrowed variants apply a BioAssayDimension restriction and sort
            // by assay name (see DatasetArgService.getSamples(DatasetArg, QuantitationType)); neither is
            // expressible as an id-only cursor under the step 1b restriction, so refuse instead of silently
            // ignoring the user's request.
            if ( quantitationTypeArg != null || useProcessedQuantitationType ) {
                throw new BadRequestException( "Cursor pagination is not supported together with quantitationType / "
                        + "useProcessedQuantitationType; either drop the cursor or drop the QT parameters." );
            }
            CursorPage<BioAssayValueObject> page = datasetArgService.getSamplesByCursor( datasetArg, cursorArg.getValue(), limitArg.getValue() );
            return paginateByCursor( page, new String[] { "id" } );
        }
        if ( quantitationTypeArg != null ) {
            ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
            QuantitationType qt = quantitationTypeArgService.getEntity( quantitationTypeArg, ee );
            return respond( datasetArgService.getSamples( datasetArg, qt ) );
        }
        if ( useProcessedQuantitationType ) {
            QuantitationType qt = datasetArgService.getPreferredQuantitationType( datasetArg );
            return respond( datasetArgService.getSamples( datasetArg, qt ) );
        }
        return respond( datasetArgService.getSamples( datasetArg ) );
    }

    /**
     * Request body for {@link #markDatasetSampleOutlier}. {@code outlier=true} flags the sample as an
     * outlier (delegating to {@link OutlierFlaggingService#markAsMissing(Collection)}); {@code outlier=false}
     * reverts an existing flag (delegating to {@link OutlierFlaggingService#unmarkAsMissing(Collection)}).
     */
    public static class SampleOutlierRequest {
        @Nullable
        private Boolean outlier;

        @Nullable
        public Boolean getOutlier() {
            return outlier;
        }

        public void setOutlier( @Nullable Boolean outlier ) {
            this.outlier = outlier;
        }
    }

    /**
     * Mark (or unmark) a BioAssay as a sample outlier.
     * <p>
     * Curation-UI workflow-step endpoint: the experiment-page "flag/unflag outlier" buttons call this. Flagging
     * sets the assay's processed-data values to missing via {@link OutlierFlaggingService#markAsMissing}; the
     * inverse reverts that. The endpoint validates that the supplied bioAssay belongs to the path-derived
     * dataset before mutating, returning {@code 400} otherwise.
     */
    @PUT
    @Path("/{dataset}/samples/{bioAssayId}/outlier")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Mark or unmark a BioAssay as a sample outlier",
            description = "Body: `{\"outlier\": true|false}`. `true` flags the assay as an outlier (its processed-data "
                    + "values are set to missing); `false` reverts that. Returns the updated `BioAssayValueObject` "
                    + "for the flipped assay. The bioAssay must belong to the path-derived dataset; otherwise a "
                    + "`400` is returned.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The request body is missing the `outlier` field, or the bioAssay does not belong to the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset or bioAssay does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<BioAssayValueObject> markDatasetSampleOutlier(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("bioAssayId") Long bioAssayId,
            @Nullable SampleOutlierRequest body
    ) {
        if ( body == null || body.getOutlier() == null ) {
            throw new BadRequestException( "A request body with a non-null `outlier` field is required." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        ee = expressionExperimentService.thawBioAssays( ee );
        BioAssay target = null;
        for ( BioAssay ba : ee.getBioAssays() ) {
            if ( bioAssayId.equals( ba.getId() ) ) {
                target = ba;
                break;
            }
        }
        if ( target == null ) {
            throw new BadRequestException( "BioAssay " + bioAssayId + " does not belong to dataset " + ee.getShortName() + "." );
        }
        Collection<BioAssay> assays = Collections.singleton( target );
        if ( body.getOutlier() ) {
            outlierFlaggingService.markAsMissing( assays );
        } else {
            outlierFlaggingService.unmarkAsMissing( assays );
        }
        // Reload the assay so the VO reflects the flip (markAsMissing toggles isOutlier on the BioAssay).
        BioAssay refreshed = bioAssayService.loadOrFail( bioAssayId );
        refreshed = bioAssayService.thaw( refreshed );
        BioAssayValueObject vo = new BioAssayValueObject( refreshed, false );
        return respond( vo );
    }

    /**
     * Request body for {@link #batchMarkSampleOutliers}. {@code mark} flips the listed
     * assays to outlier=true; {@code unmark} flips the listed assays to outlier=false.
     * Both arrays are optional; null/empty means "no change in that direction". Ids that
     * appear in BOTH arrays are rejected with 400 (caller bug).
     */
    public static class BatchOutlierRequest {
        @Nullable
        public List<Long> mark;
        @Nullable
        public List<Long> unmark;
    }

    /**
     * Batch outlier mark / unmark. Pairs with {@code GET /sample-correlation} — the UI
     * builds up mark/unmark deltas as the curator clicks samples, then sends a single
     * request to persist the change. Delta semantics (not declarative replacement) so a
     * filtered UI view can't accidentally unflag samples the user couldn't see.
     */
    @POST
    @Path("/{dataset}/samples/outliers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Batch mark or unmark sample outliers",
            description = "Body: `{\"mark\": [bioAssayId,...], \"unmark\": [bioAssayId,...]}`. The listed assays must all belong to the path-derived dataset; otherwise a 400 is returned and NOTHING is mutated (validation runs before any service call). Returns the updated full outlier set (across the dataset).",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "An id appears in both mark and unmark, or doesn't belong to the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<BatchOutlierResponse> batchMarkSampleOutliers(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable BatchOutlierRequest body
    ) {
        List<Long> mark = body != null && body.mark != null ? body.mark : Collections.emptyList();
        List<Long> unmark = body != null && body.unmark != null ? body.unmark : Collections.emptyList();
        // Reject ambiguous deltas — caller bug.
        Set<Long> markSet = new HashSet<>( mark );
        for ( Long id : unmark ) {
            if ( markSet.contains( id ) ) {
                throw new BadRequestException( "BioAssay id " + id + " appears in both `mark` and `unmark`." );
            }
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        ee = expressionExperimentService.thawBioAssays( ee );
        // Build (and validate) the assay sets up front; touch the service only after every id is known-valid.
        Map<Long, BioAssay> byId = new HashMap<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            byId.put( ba.getId(), ba );
        }
        List<BioAssay> toMark = new ArrayList<>( mark.size() );
        List<BioAssay> toUnmark = new ArrayList<>( unmark.size() );
        for ( Long id : mark ) {
            BioAssay ba = byId.get( id );
            if ( ba == null ) {
                throw new BadRequestException( "BioAssay " + id + " does not belong to dataset " + ee.getShortName() + "." );
            }
            toMark.add( ba );
        }
        for ( Long id : unmark ) {
            BioAssay ba = byId.get( id );
            if ( ba == null ) {
                throw new BadRequestException( "BioAssay " + id + " does not belong to dataset " + ee.getShortName() + "." );
            }
            toUnmark.add( ba );
        }
        if ( !toMark.isEmpty() ) {
            outlierFlaggingService.markAsMissing( toMark );
        }
        if ( !toUnmark.isEmpty() ) {
            outlierFlaggingService.unmarkAsMissing( toUnmark );
        }
        // Rebuild outlier set from authoritative state.
        ExpressionExperiment refreshed = expressionExperimentService.thawBioAssays(
                expressionExperimentService.loadOrFail( ee.getId(), NotFoundException::new, "ee gone after outlier batch?" ) );
        List<Long> outlierIds = new ArrayList<>();
        for ( BioAssay ba : refreshed.getBioAssays() ) {
            if ( ba.getIsOutlier() ) {
                outlierIds.add( ba.getId() );
            }
        }
        outlierIds.sort( Comparator.naturalOrder() );
        BatchOutlierResponse out = new BatchOutlierResponse();
        out.outlierBioAssayIds = outlierIds;
        out.markedCount = toMark.size();
        out.unmarkedCount = toUnmark.size();
        return respond( out );
    }

    public static class BatchOutlierResponse {
        /** Full sorted list of outlier-flagged bioAssay ids in this dataset after the mutation. */
        public List<Long> outlierBioAssayIds;
        public int markedCount;
        public int unmarkedCount;
    }

    /**
     * Request body for {@link #markFactorValueNeedsAttention} and
     * {@link #clearFactorValueNeedsAttention}. {@code note} is the human-readable
     * reason recorded on the Ticket the service opens / resolves.
     */
    public static class FactorValueNeedsAttentionRequest {
        @Nullable
        private String note;

        @Nullable
        public String getNote() {
            return note;
        }

        public void setNote( @Nullable String note ) {
            this.note = note;
        }
    }

    /**
     * Open a "needs attention" ticket against a factor value.
     * <p>
     * Replaces the legacy gemma-web {@code ExperimentalDesignController.markFactorValuesAsNeedsAttention}
     * AJAX call. Routes through {@link FactorValueNeedsAttentionService}, which opens a
     * {@code TicketType.GENERIC} ticket targeting both the factor value and its owning EE.
     */
    @POST
    @Path("/{dataset}/factor-values/{factorValueId}/needs-attention")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_USER')")
    @Operation(summary = "Mark a factor value as needing curator attention",
            description = "Opens a curator ticket against the factor value. Body: `{\"note\": \"...\"}` — the note is the ticket title suffix. The factor value must belong to the path-derived dataset; otherwise a 400 is returned. Idempotency is enforced by the underlying service: marking an already-flagged factor value throws 409.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_USER" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_USER" }) },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Ticket opened."),
                    @ApiResponse(responseCode = "400", description = "The factor value does not belong to the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset or factor value does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "The factor value already has an open needs-attention ticket.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response markFactorValueNeedsAttention(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("factorValueId") Long factorValueId,
            @Nullable FactorValueNeedsAttentionRequest body
    ) {
        FactorValue fv = resolveFactorValueForDataset( datasetArg, factorValueId );
        String note = body != null && body.getNote() != null ? body.getNote() : "";
        try {
            factorValueNeedsAttentionService.markAsNeedsAttention( fv, note );
        } catch ( IllegalArgumentException e ) {
            // Service throws IAE when the FV already needs attention (see service contract).
            throw new ClientErrorException( e.getMessage(), Response.Status.CONFLICT );
        }
        return Response.noContent().build();
    }

    /**
     * Resolve every open needs-attention ticket on a factor value.
     */
    @DELETE
    @Path("/{dataset}/factor-values/{factorValueId}/needs-attention")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_USER')")
    @Operation(summary = "Clear the needs-attention flag on a factor value",
            description = "Resolves every open ticket targeting the factor value. `note` is recorded as the resolution reason. 409 if the factor value is not currently flagged.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_USER" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_USER" }) },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Tickets resolved."),
                    @ApiResponse(responseCode = "400", description = "The factor value does not belong to the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset or factor value does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "The factor value has no open needs-attention ticket.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response clearFactorValueNeedsAttention(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("factorValueId") Long factorValueId,
            @Parameter(description = "Resolution reason; recorded on every ticket transition.") @QueryParam("note") @Nullable String note
    ) {
        FactorValue fv = resolveFactorValueForDataset( datasetArg, factorValueId );
        try {
            factorValueNeedsAttentionService.clearNeedsAttentionFlag( fv, note != null ? note : "" );
        } catch ( IllegalArgumentException e ) {
            throw new ClientErrorException( e.getMessage(), Response.Status.CONFLICT );
        }
        return Response.noContent().build();
    }

    private FactorValue resolveFactorValueForDataset( DatasetArg<?> datasetArg, Long factorValueId ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        FactorValue fv = factorValueService.loadWithExperimentalFactorOrFail( factorValueId, NotFoundException::new );
        if ( fv.getExperimentalFactor() == null
                || fv.getExperimentalFactor().getExperimentalDesign() == null
                || ee.getExperimentalDesign() == null
                || !ee.getExperimentalDesign().getId().equals( fv.getExperimentalFactor().getExperimentalDesign().getId() ) ) {
            throw new BadRequestException( "FactorValue " + factorValueId
                    + " does not belong to dataset " + ee.getShortName() + "." );
        }
        return fv;
    }

    @GET
    @Path("/{dataset}/publications")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all publications associated with a dataset",
            description = "Each publication carries an `association` object recording why it is attached: "
                    + "the authority behind the claim (`curator`, `geo_submitter_link`, `agent`, …), the "
                    + "one-line evidence, an evidence code, and when it was asserted. A null `association` "
                    + "means the link exists but nothing was recorded about where it came from. "
                    + "Set `includeRejected=true` to also list the publications ruled out for this dataset — "
                    + "the \"do not re-propose\" set a publication finder should read before it starts. They "
                    + "are excluded by default because a rejection is a record of a decision, not a "
                    + "publication of the dataset.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<DatasetPublicationValueObject>> getDatasetAllPublications(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Also list the publications that were considered for this dataset and ruled out.")
            @QueryParam("includeRejected") @DefaultValue("false") Boolean includeRejected
    ) {

        List<DatasetPublicationValueObject> out = datasetArgService.getPublications( datasetArg,
                Boolean.TRUE.equals( includeRejected ) );
        return respond( out );

    }

    @PUT
    @Path("/{dataset}/publications")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Replace the publications associated with a dataset",
            description = "Idempotent set-replace for the dataset's primary, other-relevant and rejected "
                    + "publications. Each publication is an object carrying exactly one of `pubMedId` or "
                    + "`doi`, e.g. `{\"pubMedId\":\"22438826\"}` or `{\"doi\":\"10.1101/2025.01.02.634567\"}`. "
                    + "`primaryPublication` sets the primary publication, or clears it when null/omitted; "
                    + "`otherRelevantPublications` replaces the other-relevant set (an empty list clears it, "
                    + "and the field is required so a partial body can't silently wipe publications). "
                    + "Identifiers not already held by Gemma are fetched and persisted on demand: PubMed ids "
                    + "from PubMed; DOIs via PubMed-by-DOI then CrossRef (which covers bioRxiv / medRxiv "
                    + "preprints PubMed doesn't index). An identifier that resolves nowhere yields a 400. "
                    + "A publication given both as the primary and in the other-relevant list is kept only as "
                    + "the primary; one given as both accepted and rejected is a 400.\n\n"
                    + "Every entry may carry the evidence behind it — `source`, `evidence`, "
                    + "`supportingEvidence`, `evidenceCode`, `confidence`, `assertedBy` — which is stored "
                    + "against the (dataset, publication) pair and returned under `association` by the GET.\n\n"
                    + "`rejectedPublications` records the papers considered for this dataset and ruled out. "
                    + "This is not the same as leaving them out: a rejection is enforced, so a later writer "
                    + "of lower authority (a GEO re-fetch, a publication finder) that re-proposes the paper is "
                    + "refused, whereas a paper merely dropped from the lists can be re-attached by anyone. "
                    + "Precedence runs curator > geo_submitter_link / external_import > agent > legacy. "
                    + "An accepted publication that stands rejected by an authority the caller's `source` does "
                    + "not outrank yields a 409. **Omit the field entirely to leave the standing rejections "
                    + "untouched**; send a list (an empty one included) to replace them wholesale. The default "
                    + "is deliberate: rejections are not in what the plain GET returns, so a client writing "
                    + "back what it read has not seen them and its silence must not delete them.\n\n"
                    + "Returns the dataset's publication list, same shape as the GET (rejections excluded — "
                    + "read them back with `?includeRejected=true`). Requires `ACL_SECURABLE_EDIT` on the "
                    + "dataset. Replaces the retired gemma-web `updatePubMed` / `removePrimaryPublication` "
                    + "controller methods, and the workaround of noting a preprint in a curation comment.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The request body is missing or malformed, a PubMed id could not be resolved, or a publication was given as both accepted and rejected.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "403", description = "The caller lacks edit permission on the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "A publication being accepted was rejected for this dataset by an authority the caller's source does not outrank.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<DatasetPublicationValueObject>> updateDatasetPublications(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable PublicationsUpdateRequest body
    ) {
        if ( body == null || body.getOtherRelevantPublications() == null ) {
            throw new BadRequestException( "A request body with an 'otherRelevantPublications' list is required (use an empty list, and a null 'primaryPublication', to clear all publications)." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );

        PublicationAssertion primary = resolveAssertion( body.getPrimaryPublication(), "primaryPublication" );

        List<PublicationAssertion> other = new ArrayList<>();
        for ( PublicationEntry entry : body.getOtherRelevantPublications() ) {
            PublicationAssertion a = resolveAssertion( entry, "otherRelevantPublications" );
            if ( a == null ) {
                throw new BadRequestException( "Each entry in 'otherRelevantPublications' must carry a non-blank 'pubMedId' or 'doi'." );
            }
            other.add( a );
        }

        // 🛑 Absent stays null all the way down; only a present list (including an empty one) replaces
        // the standing rejections. Unlike 'otherRelevantPublications' this field is optional, and a
        // rejection is not in what the plain GET returns -- so a client that reads a dataset and PUTs
        // back what it read has never been shown them. Coercing its silence to an empty list deleted
        // every ruling on the dataset: on GSE227854 that is Rachel's rejection of GEO's own wrong
        // !Series_pubmed_id, and with it gone the next GEO refresh re-installs that paper unopposed.
        List<PublicationAssertion> rejected = null;
        if ( body.getRejectedPublications() != null ) {
            rejected = new ArrayList<>();
            for ( PublicationEntry entry : body.getRejectedPublications() ) {
                PublicationAssertion a = resolveAssertion( entry, "rejectedPublications" );
                if ( a == null ) {
                    throw new BadRequestException( "Each entry in 'rejectedPublications' must carry a non-blank 'pubMedId' or 'doi'." );
                }
                rejected.add( a );
            }
        }

        try {
            expressionExperimentService.updatePublications( ee, primary, other, rejected );
        } catch ( PublicationAssociationConflictException e ) {
            // A refusal, not a malformed request: the caller asked for something coherent and was
            // outranked. 409 so a client can tell "you got this wrong" from "someone else decided".
            throw new ClientErrorException( e.getMessage(), Response.Status.CONFLICT, e );
        }
        return respond( datasetArgService.getPublications( datasetArg ) );
    }

    /**
     * Resolve a wire {@link PublicationEntry} into a {@link PublicationAssertion}: the persistent
     * reference plus the evidence given for it.
     * <p>
     * {@code source} defaults to {@link PublicationAssociationSource#CURATOR}, which is right for the
     * endpoint's normal user — a human with edit rights on the dataset — and is why an automated
     * client has to say so explicitly. An unrecognised value is a 400 rather than a silent fallback to
     * the highest authority in the ranking.
     */
    @Nullable
    private PublicationAssertion resolveAssertion( @Nullable PublicationEntry entry, String field ) {
        BibliographicReference ref = resolvePublication( entry );
        if ( ref == null ) {
            return null;
        }
        PublicationAssociationSource source = PublicationAssociationSource.CURATOR;
        if ( StringUtils.isNotBlank( entry.getSource() ) ) {
            try {
                source = PublicationAssociationSource.fromDbValue( entry.getSource().trim() );
            } catch ( IllegalArgumentException e ) {
                throw new BadRequestException( "Unknown 'source' " + entry.getSource() + " in '" + field
                        + "'. Use one of: curator, geo_submitter_link, external_import, agent." );
            }
        }
        GOEvidenceCode evidenceCode = null;
        if ( StringUtils.isNotBlank( entry.getEvidenceCode() ) ) {
            try {
                evidenceCode = GOEvidenceCode.valueOf( entry.getEvidenceCode().trim().toUpperCase() );
            } catch ( IllegalArgumentException e ) {
                throw new BadRequestException( "Unknown 'evidenceCode' " + entry.getEvidenceCode() + " in '" + field + "'." );
            }
        }
        if ( entry.getConfidence() != null && ( entry.getConfidence() < 0.0 || entry.getConfidence() > 1.0 ) ) {
            throw new BadRequestException( "'confidence' in '" + field + "' must be between 0 and 1." );
        }
        return new PublicationAssertion( ref, source, StringUtils.stripToNull( entry.getEvidence() ),
                StringUtils.stripToNull( entry.getSupportingEvidence() ), evidenceCode, entry.getConfidence(),
                StringUtils.stripToNull( entry.getAssertedBy() ) );
    }

    /**
     * Resolve a wire {@link PublicationEntry} to a persistent {@link BibliographicReference}, fetching
     * from PubMed or CrossRef when necessary. A null / empty identifier resolves to {@code null} (i.e. "no
     * publication"). An identifier that names both a PubMed id and a DOI, or one whose id can't be resolved,
     * is surfaced as a {@code 400} rather than a {@code 500}.
     */
    @Nullable
    private BibliographicReference resolvePublication( @Nullable PublicationEntry id ) {
        if ( id == null ) {
            return null;
        }
        boolean hasPubMed = StringUtils.isNotBlank( id.getPubMedId() );
        boolean hasDoi = StringUtils.isNotBlank( id.getDoi() );
        if ( hasPubMed && hasDoi ) {
            throw new BadRequestException( "A publication identifier must carry exactly one of 'pubMedId' or 'doi', not both." );
        }
        if ( !hasPubMed && !hasDoi ) {
            return null;
        }
        try {
            return hasPubMed
                    ? bibliographicReferenceService.findOrCreateByPubMedId( id.getPubMedId().trim() )
                    : bibliographicReferenceService.findOrCreateByDoi( id.getDoi().trim() );
        } catch ( IllegalStateException | IllegalArgumentException e ) {
            throw new BadRequestException( "Could not resolve publication "
                    + ( hasPubMed ? "PubMed id '" + id.getPubMedId() + "'" : "DOI '" + id.getDoi() + "'" )
                    + ": " + e.getMessage() );
        }
    }

    /**
     * Request body for {@link #updateDatasetPublications}. Publications are addressed by
     * {@link PublicationEntry} (PubMed id or DOI, plus the evidence for the claim);
     * {@code primaryPublication} may be null (clears the primary), {@code otherRelevantPublications} may
     * be an empty list (clears the other-relevant set), and {@code rejectedPublications} records the
     * papers ruled out for this dataset. Symmetric with the {@link #getDatasetAllPublications} read,
     * which returns full {@link DatasetPublicationValueObject}s (the identifier is on each VO's
     * {@code pubAccession}, the evidence under {@code association}).
     */
    public static class PublicationsUpdateRequest {
        @Nullable
        private PublicationEntry primaryPublication;
        @Nullable
        private List<PublicationEntry> otherRelevantPublications;
        @Schema(description = "Publications considered for this dataset and ruled out. Recorded rather than merely omitted, so a later automated writer that re-finds one of them is refused instead of quietly re-attaching it. Omit or send an empty list to clear the rejections.")
        @Nullable
        private List<PublicationEntry> rejectedPublications;

        @Nullable
        public PublicationEntry getPrimaryPublication() {
            return primaryPublication;
        }

        public void setPrimaryPublication( @Nullable PublicationEntry primaryPublication ) {
            this.primaryPublication = primaryPublication;
        }

        @Nullable
        public List<PublicationEntry> getOtherRelevantPublications() {
            return otherRelevantPublications;
        }

        public void setOtherRelevantPublications( @Nullable List<PublicationEntry> otherRelevantPublications ) {
            this.otherRelevantPublications = otherRelevantPublications;
        }

        @Nullable
        public List<PublicationEntry> getRejectedPublications() {
            return rejectedPublications;
        }

        public void setRejectedPublications( @Nullable List<PublicationEntry> rejectedPublications ) {
            this.rejectedPublications = rejectedPublications;
        }
    }

    /**
     * A single publication on the {@link #updateDatasetPublications} wire: which paper, and why.
     * <p>
     * Identity is exactly one of {@code pubMedId} or {@code doi}. DOIs may be given bare
     * ({@code 10.x/…}), as a {@code doi.org} URL, or {@code doi:}-prefixed; a DOI not indexed by PubMed
     * is resolved via CrossRef (covers bioRxiv / medRxiv preprints). Manual metadata entry
     * (title/authors) is intentionally not accepted here yet.
     * <p>
     * Everything after the identity is the evidence, and it is optional in the sense that a request
     * without it still works — but {@code source} is what decides precedence between writers, so a
     * caller that leaves it out is recorded as {@code curator}, the highest authority. An agent must
     * set {@code "source": "agent"} rather than let it default, or its proposals will outrank the
     * curators they are meant to defer to.
     */
    public static class PublicationEntry {
        @Nullable
        private String pubMedId;
        @Nullable
        private String doi;
        @Schema(description = "Who is making this claim: `curator`, `agent`, `geo_submitter_link`, `external_import`. Defaults to `curator`, which outranks everything — an automated caller must set this explicitly.",
                allowableValues = { "curator", "agent", "geo_submitter_link", "external_import" })
        @Nullable
        private String source;
        @Schema(description = "The one-line quotable basis, e.g. \"the series title names this paper almost verbatim\" or \"the paper cites this accession under Data Availability\".")
        @Nullable
        private String evidence;
        @Schema(description = "Structured evidence items backing the one-line basis, as a JSON array in the curation agents' FindingEvidence shape. Stored verbatim and never parsed by Gemma.")
        @Nullable
        private String supportingEvidence;
        @Schema(description = "How the claim was arrived at, in the vocabulary annotations use: IC (curator inference), TAS (stated in a traceable source), IEA (software, unchecked), IIA (carried in from imported data).")
        @Nullable
        private String evidenceCode;
        @Schema(description = "Self-reported confidence in [0,1], for machine claims.")
        @Nullable
        private Double confidence;
        @Schema(description = "Username or agent run identifier behind the claim. Defaults to the authenticated user.")
        @Nullable
        private String assertedBy;

        @Nullable
        public String getPubMedId() {
            return pubMedId;
        }

        public void setPubMedId( @Nullable String pubMedId ) {
            this.pubMedId = pubMedId;
        }

        @Nullable
        public String getDoi() {
            return doi;
        }

        public void setDoi( @Nullable String doi ) {
            this.doi = doi;
        }

        @Nullable
        public String getSource() {
            return source;
        }

        public void setSource( @Nullable String source ) {
            this.source = source;
        }

        @Nullable
        public String getEvidence() {
            return evidence;
        }

        public void setEvidence( @Nullable String evidence ) {
            this.evidence = evidence;
        }

        @Nullable
        public String getSupportingEvidence() {
            return supportingEvidence;
        }

        public void setSupportingEvidence( @Nullable String supportingEvidence ) {
            this.supportingEvidence = supportingEvidence;
        }

        @Nullable
        public String getEvidenceCode() {
            return evidenceCode;
        }

        public void setEvidenceCode( @Nullable String evidenceCode ) {
            this.evidenceCode = evidenceCode;
        }

        @Nullable
        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence( @Nullable Double confidence ) {
            this.confidence = confidence;
        }

        @Nullable
        public String getAssertedBy() {
            return assertedBy;
        }

        public void setAssertedBy( @Nullable String assertedBy ) {
            this.assertedBy = assertedBy;
        }
    }

    @GET
    @Path("/{dataset}/tickets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the open curation tickets for a dataset",
            description = "Legacy mode (no `cursor` parameter): returns the full unpaginated open-ticket list "
                    + "in the existing shape (no count query, full result set). "
                    + "Cursor mode (recommended for noisy datasets accumulating long curation histories): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` "
                    + "field along with a `limit`. In cursor mode the result is always sorted by ascending `id` "
                    + "(cursor mode forces a single-component id sort pending the indexed-column audit in phase B); "
                    + "the path-derived `targetType = EXPRESSION_EXPERIMENT, targetId = {dataset}` constraint and "
                    + "the open-state restriction (OPEN/IN_PROGRESS) are preserved; `totalElements` is `null` by "
                    + "default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    ResponseDataObjectListTicketValueObject.class,
                                    CursorPaginatedResponseDataObjectTicketValueObject.class
                            }))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Object getDatasetTickets(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Opaque keyset-pagination cursor token.")
            @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Page size for cursor mode (ignored when no `cursor` is supplied).")
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( cursorArg != null ) {
            CursorPage<TicketValueObject> page = ticketsWebService.openTicketsForExpressionExperimentByCursor(
                    ee.getId(), cursorArg.getValue(), limitArg.getValue() );
            return paginateByCursor( page, new String[] { "id" } );
        }
        return respond( ticketsWebService.openTicketsForExpressionExperiment( ee.getId() ) );
    }

    /**
     * Groups that have ANY permission (read or admin) on the given dataset
     * (gap §3c of {@code GEMMA_UI_ENDPOINT_GAP.md}). When
     * {@code includeSummaries=true}, each entry includes the group's
     * lightweight summary (name, description, memberCount); otherwise only
     * the group names are returned.
     */
    @GET
    @Path("/{dataset}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve groups that have any permission on the given dataset",
            description = "Returns the names (or summaries) of groups with read or admin permission "
                    + "on the dataset's ACL. The set is computed from the SecurityService union of "
                    + "groupsReadableBy + groupsEditableBy. Filtered by the current caller's ACL view.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<?> getDatasetGroups(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("includeSummaries") @DefaultValue("false") boolean includeSummaries,
            // Legacy spelling, accepted so a stale caller gets the behaviour it asked for rather
            // than silently falling back to the default. Remove once no client sends it.
            @Parameter(hidden = true)
            @QueryParam("include_summaries") @DefaultValue("false") boolean includeSummariesLegacy
    ) {
        includeSummaries = includeSummaries || includeSummariesLegacy;
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        Set<String> groupNames = new LinkedHashSet<>();
        groupNames.addAll( securityService.getGroupsReadableBy( ee ) );
        groupNames.addAll( securityService.getGroupsEditableBy( ee ) );
        if ( includeSummaries ) {
            return respond( groupsWebService.summariesForGroupNames( groupNames ) );
        }
        return respond( new ArrayList<>( groupNames ) );
    }

    @GET
    @Path("/{dataset}/auditEvents")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the audit events of a dataset",
            description = "Legacy mode (no `cursor` parameter): returns the full unpaginated audit-event "
                    + "list in the existing shape (no count query, full result set, sorted by `date, id`). "
                    + "Cursor mode (recommended for datasets accumulating long curation/processing histories): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` "
                    + "field along with a `limit`. In cursor mode the result is always sorted by ascending `id` "
                    + "(cursor mode forces a single-component id sort pending the indexed-column audit in phase B; "
                    + "audit events are append-only so id-asc tracks date-asc in practice); the path-derived "
                    + "dataset (AuditTrail) scope is preserved; `totalElements` is `null` by default "
                    + "(no count query per request). "
                    + "Pass `compact=true` to collapse consecutive same-(eventType, performer) events into a "
                    + "single entry carrying `collapsedCount` (run length) and `lastOccurrence` (last event's "
                    + "date); the first event's message is kept verbatim. Compression happens within the "
                    + "response page only — runs are never merged across cursor boundaries. "
                    + "Pass `excludeEmpty=true` to drop entries that have no eventType AND blank note/detail "
                    + "(plain audit ticks that carry no story); when combined with `compact=true`, filtering "
                    + "happens FIRST so collapsing runs over the post-filter sequence. Default for both "
                    + "options is `false` (full fidelity).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    ResponseDataObjectListAuditEventValueObject.class,
                                    ResponseDataObjectListCompactAuditEventValueObject.class,
                                    CursorPaginatedResponseDataObjectAuditEventValueObject.class,
                                    CursorPaginatedResponseDataObjectCompactAuditEventValueObject.class
                            }))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Object getDatasetAuditEvents(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Opaque keyset-pagination cursor token.")
            @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Page size for cursor mode (ignored when no `cursor` is supplied).")
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @Parameter(description = "Collapse runs of consecutive same-(eventType, performer) events into one entry with `collapsedCount` + `lastOccurrence`. Default `false`.")
            @QueryParam("compact") @DefaultValue("false") boolean compact,
            @Parameter(description = "Drop entries with no eventType AND blank note/detail (boring update ticks). Default `false`. Combine with `compact=true` for a tight curator-story view.")
            @QueryParam("excludeEmpty") @DefaultValue("false") boolean excludeEmpty
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( cursorArg != null ) {
            CursorPage<AuditEventValueObject> page = auditEventService
                    .getEventsByCursor( ee, cursorArg.getValue(), limitArg.getValue() )
                    .map( AuditEventValueObject::new );
            if ( excludeEmpty ) {
                // CursorPage extends AbstractList<O>, so stream directly off it
                List<AuditEventValueObject> filtered = page.stream()
                        .filter( e -> !isEmptyUpdate( e ) )
                        .collect( Collectors.toList() );
                page = new CursorPage<>( filtered, page.getSort(), page.getLimit(),
                        page.getNextCursor(), page.getPrevCursor(), page.getTotalElements() );
            }
            if ( compact ) {
                List<CompactAuditEventValueObject> collapsed = collapseAuditEvents( page );
                CursorPage<CompactAuditEventValueObject> compactPage = new CursorPage<>(
                        collapsed,
                        page.getSort(),
                        page.getLimit(),
                        page.getNextCursor(),
                        page.getPrevCursor(),
                        page.getTotalElements() );
                return paginateByCursor( compactPage, new String[] { "id" } );
            }
            return paginateByCursor( page, new String[] { "id" } );
        }
        List<AuditEventValueObject> out = auditEventService.getEvents( ee ).stream()
                .map( AuditEventValueObject::new )
                .collect( Collectors.toList() );
        if ( excludeEmpty ) {
            out = out.stream().filter( e -> !isEmptyUpdate( e ) ).collect( Collectors.toList() );
        }
        if ( compact ) {
            return respond( collapseAuditEvents( out ) );
        }
        return respond( out );
    }

    /**
     * An audit event entry is "empty" / boring when it carries no specific {@code eventType} and
     * its {@code note} + {@code detail} are both blank — i.e. just a "something was touched" tick
     * with no story value to a curator scanning the trail.
     */
    private static boolean isEmptyUpdate( AuditEventValueObject e ) {
        if ( e.getEventType() != null ) {
            return false;
        }
        return ( e.getNote() == null || e.getNote().trim().isEmpty() )
                && ( e.getDetail() == null || e.getDetail().trim().isEmpty() );
    }

    /**
     * Fold a chronological audit-event list into runs sharing the same (eventType, performer) pair.
     * Each maximal run emits ONE {@link CompactAuditEventValueObject} carrying the first event's full
     * content, a {@code collapsedCount} = run length, and a {@code lastOccurrence} = date of the LAST
     * event in the run (= the first event's date for a solo entry).
     */
    private static List<CompactAuditEventValueObject> collapseAuditEvents( List<AuditEventValueObject> events ) {
        List<CompactAuditEventValueObject> out = new ArrayList<>();
        if ( events == null || events.isEmpty() ) {
            return out;
        }
        AuditEventValueObject runHead = null;
        int runCount = 0;
        Date runLast = null;
        for ( AuditEventValueObject ev : events ) {
            if ( runHead != null
                    && Objects.equals( ev.getEventType(), runHead.getEventType() )
                    && Objects.equals( ev.getPerformer(), runHead.getPerformer() ) ) {
                runCount++;
                if ( ev.getDate() != null ) {
                    runLast = ev.getDate();
                }
            } else {
                if ( runHead != null ) {
                    out.add( new CompactAuditEventValueObject( runHead, runCount, runLast ) );
                }
                runHead = ev;
                runCount = 1;
                runLast = ev.getDate();
            }
        }
        out.add( new CompactAuditEventValueObject( runHead, runCount, runLast ) );
        return out;
    }

    /*
     * Per-dataset annotation-set surface. The handler bodies live on
     * AnnotationSetsWebService — these wrappers exist because Jersey resolves
     * /datasets/* against the class-level @Path("/datasets") and never falls
     * through to AnnotationSetsWebService's class-level @Path("/"), so the
     * routes have to be declared on this resource class to be reachable.
     */

    @POST
    @Path("/{dataset}/annotation-sets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Attach an AnnotationSet to a dataset.",
            description = "Idempotent on `(role, runId)`: a retry returns the existing row as 200 OK "
                    + "rather than 201 Created. Body's `role` selects PROPOSAL / DRAFT / SNAPSHOT.")
    public Response submitDatasetAnnotationSet(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable AnnotationSetsWebService.AnnotationSetRequest body
    ) {
        return annotationSetsWebService.submitAnnotationSet( datasetArg, body );
    }

    @POST
    @Path("/{dataset}/annotation-sets/snapshot")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Capture the dataset's current curation as an immutable SNAPSHOT AnnotationSet.",
            description = "Takes the backup for you: the server reads the current design, tags, sample "
                    + "characteristics and curation note and stores them as the AnnotationSet's `payloadJson`. "
                    + "Deliberate and append-only — nothing snapshots on its own, so this is what you call before "
                    + "letting an agent apply a batch of changes.\n\n"
                    + "The payload is a `CurationDocument`, the same shape `PUT /datasets/{id}/curation` accepts. "
                    + "That is what makes the two companion operations free: "
                    + "`POST /datasets/{id}/annotation-sets/{setId}/restore?dryRun=true` compares the snapshot "
                    + "with the present, and the same call without `dryRun` puts it back.\n\n"
                    + "Emits no audit event, so the dataset's `lastUpdated` does not move: a backup must not "
                    + "modify what it backs up, and `lastUpdated` is the optimistic-concurrency token the "
                    + "curation commit checks. The AnnotationSet row is its own record of the capture, carrying "
                    + "`createdAt`, `createdBy` and `runId`. (PROPOSAL and DRAFT do emit an `AnnotationSetEvent`.)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "The snapshot was captured.",
                            content = @Content(schema = @Schema(implementation = AnnotationSetsWebService.AnnotationSetResponse.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response snapshotDatasetCuration(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Optional note recorded as the snapshot's producer identity, e.g. why the backup was taken.")
            @QueryParam("createdBy") @Nullable String createdBy
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        AnnotationSetsWebService.AnnotationSetRequest body = new AnnotationSetsWebService.AnnotationSetRequest();
        body.role = "snapshot";
        body.createdBy = createdBy;
        body.payloadJson = writeSnapshotPayload( buildCurationSnapshot( ee ) );
        return annotationSetsWebService.submitAnnotationSet( datasetArg, body );
    }

    @POST
    @Path("/{dataset}/annotation-sets/{setId}/restore")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Restore a dataset's curation from a SNAPSHOT, or compare it with the present.",
            description = "Replays the snapshot's `CurationDocument` through the ordinary all-or-none commit, so "
                    + "there is no second diff implementation that could disagree with the first.\n\n"
                    + "`?dryRun=true` is the compare: it reports exactly what would change to get back to the "
                    + "snapshot, and writes nothing.\n\n"
                    + "🛑 A restore returns the curation's CONTENT, not its IDENTITY. Entities whose ids no longer "
                    + "exist — because an intervening run deleted and recreated them — come back as new rows with "
                    + "new ids, and any differential-expression analysis that survived that run is cascaded again "
                    + "on the way back. Run with `dryRun=true` first and read `requiresForce`.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Restored, or (dryRun) the predicted changes."),
                    @ApiResponse(responseCode = "400", description = "The set is not a SNAPSHOT, or its payload is not a CurationDocument.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "The restore would delete analyses or strand a subset; retry with ?force=true.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<CurationCommitReport> restoreDatasetCurationFromSnapshot(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("setId") Long setId,
            @Parameter(description = "Predict the changes without writing. This is the 'compare with the snapshot' mode.")
            @QueryParam("dryRun") @DefaultValue("false") Boolean dryRun,
            @Parameter(description = "Consent to the restore's consequences (analysis cascade, stranded subsets).")
            @QueryParam("force") @DefaultValue("false") Boolean force
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        CurationDocument snapshot = readSnapshotPayload( setId, ee );
        reconcileSnapshotForRestore( snapshot, ee );
        // The baseline token belongs to the moment the snapshot was taken, not to now; a restore is deliberately
        // overwriting whatever happened since, so carrying it would 409 on exactly the case this exists for.
        snapshot.setBaseline( null );
        return respond( doCommitCuration( datasetArg, snapshot, dryRun, force ) );
    }

    @GET
    @Path("/{dataset}/annotation-sets")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "List AnnotationSets attached to a dataset, newest first.",
            description = "`?role=` filters by role (`proposal`/`draft`/`snapshot`/`commit`/`all`). "
                    + "`?source=` filters by source. `?createdBy=` filters by producer identity. "
                    + "`?shape=full|meta` selects response shape.")
    public Response listDatasetAnnotationSets(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Filter by role: `proposal`, `draft`, `snapshot`, `commit`, or `all` (default).")
            @QueryParam("role") @Nullable String role,
            @Parameter(description = "Filter by source.")
            @QueryParam("source") @Nullable String source,
            @Parameter(description = "Filter by createdBy (username or agent run identifier).")
            @QueryParam("createdBy") @Nullable String createdBy,
            @Parameter(description = "Response shape: `full` (default; carries payloadJson) "
                    + "or `meta` (thin projection, payloadSize only).")
            @QueryParam("shape") @Nullable String shape
    ) {
        return annotationSetsWebService.listAnnotationSets( datasetArg, role, source, createdBy, shape );
    }

    @GET
    @Path("/{dataset}/annotation-sets/draft")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Fetch the current curator's DRAFT for a dataset (404 if none).")
    public Response getDatasetDraftAnnotationSet(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return annotationSetsWebService.getDraftForDataset( datasetArg );
    }

    @PUT
    @Path("/{dataset}/annotation-sets/draft")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Upsert the current curator's DRAFT for a dataset.",
            description = "One DRAFT per (dataset, curator); returns 201 on create, 200 on update.")
    public Response upsertDatasetDraftAnnotationSet(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable AnnotationSetsWebService.UpsertDraftRequest body
    ) {
        return annotationSetsWebService.upsertDraftForDataset( datasetArg, body );
    }

    @GET
    @Path("/{dataset}/curationDetails")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the curation details of a dataset",
            description = "The `curationNote` and `lastNoteUpdateEvent` fields are only populated for administrators.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<CurationDetailsValueObject> getDatasetCurationDetails(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        return respond( new CurationDetailsValueObject( ee.getCurationDetails() ) );
    }

    /**
     * Request body for {@link #updateDatasetCurationDetails}. Each field is optional; only provided fields are updated.
     */
    public static class CurationDetailsUpdateRequest {
        @Nullable
        private Boolean troubled;
        @Nullable
        private Boolean needsAttention;
        @Nullable
        private String curationNote;
        @Nullable
        private String note;

        @Nullable
        public Boolean getTroubled() {
            return troubled;
        }

        public void setTroubled( @Nullable Boolean troubled ) {
            this.troubled = troubled;
        }

        @Nullable
        public Boolean getNeedsAttention() {
            return needsAttention;
        }

        public void setNeedsAttention( @Nullable Boolean needsAttention ) {
            this.needsAttention = needsAttention;
        }

        @Nullable
        public String getCurationNote() {
            return curationNote;
        }

        public void setCurationNote( @Nullable String curationNote ) {
            this.curationNote = curationNote;
        }

        @Nullable
        public String getNote() {
            return note;
        }

        public void setNote( @Nullable String note ) {
            this.note = note;
        }
    }

    /**
     * @deprecated per Decision 1 of {@code AUDIT_AS_WORKFLOW_RECCE.md} the
     * {@code troubled} / {@code needsAttention} flips are now backed by
     * {@link TicketService}: a {@code troubled=true} flip opens a
     * {@link TicketType#QUALITY_REVIEW} ticket, {@code troubled=false} resolves the
     * matching open ticket(s), and analogously for {@code needsAttention}
     * (mapped to {@link TicketType#GENERIC} on open). Clients should migrate to
     * {@code POST /tickets} + {@code PUT /tickets/{id}} on {@code TicketsWebService}.
     * The endpoint is retained for back-compat while the UI moves over.
     * The {@code curationNote} field is still routed through
     * {@link CurationNoteUpdateEvent} pending the note-to-ticket-comment migration
     * (see {@code CURATION_DETAILS_RETIREMENT.md}).
     */
    @PUT
    @Path("/{dataset}/curationDetails")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Deprecated
    @Operation(summary = "Update the curation details of a dataset (deprecated; use /tickets)",
            description = "DEPRECATED — the troubled/needsAttention flips are now backed by the Ticket layer "
                    + "(see /tickets). Setting troubled=true opens a QUALITY_REVIEW ticket targeting the dataset; "
                    + "troubled=false resolves all open QUALITY_REVIEW tickets. needsAttention=true opens a GENERIC "
                    + "ticket; needsAttention=false resolves all open GENERIC/BATCH_INFO_NEEDED tickets. "
                    + "An optional `note` is supplied as the ticket title (on open) or transition reason (on resolve). "
                    + "`curationNote` is still applied via the legacy CurationNoteUpdateEvent pending the note-to-comment "
                    + "migration. New clients should call /tickets directly.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<CurationDetailsValueObject> updateDatasetCurationDetails(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable CurationDetailsUpdateRequest body
    ) {
        if ( body == null ) {
            throw new BadRequestException( "A request body is required." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        CurationDetails cd = ee.getCurationDetails();
        if ( body.getTroubled() != null && body.getTroubled() != cd.getTroubled() ) {
            applyFlagViaTickets( ee, body.getTroubled(), TicketType.QUALITY_REVIEW,
                    Collections.singleton( TicketType.QUALITY_REVIEW ),
                    body.getNote(), "Dataset flagged as troubled" );
        }
        if ( body.getNeedsAttention() != null && body.getNeedsAttention() != cd.getNeedsAttention() ) {
            applyFlagViaTickets( ee, body.getNeedsAttention(), TicketType.GENERIC,
                    EnumSet.of( TicketType.GENERIC, TicketType.BATCH_INFO_NEEDED ),
                    body.getNote(), "Dataset needs attention" );
        }
        if ( body.getCurationNote() != null ) {
            // Curation notes do not yet map onto ticket comments — keep the legacy event path
            // until the note-to-comment migration lands (see CURATION_DETAILS_RETIREMENT.md).
            //noinspection deprecation
            auditTrailService.addUpdateEvent( ee, CurationNoteUpdateEvent.class, body.getCurationNote() );
        }
        return respond( new CurationDetailsValueObject( ee.getCurationDetails() ) );
    }

    /**
     * Request body for {@link #renameDatasetShortName}.
     */
    public static class RenameDatasetRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("shortName")
        @Nullable
        private String shortName;

        @Nullable
        public String getShortName() {
            return shortName;
        }

        public void setShortName( @Nullable String shortName ) {
            this.shortName = shortName;
        }
    }

    /**
     * Response body for {@link #renameDatasetShortName}.
     */
    public static class RenameDatasetResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("experimentId")
        private final Long experimentId;
        @com.fasterxml.jackson.annotation.JsonProperty("shortName")
        private final String shortName;

        public RenameDatasetResponse( Long experimentId, String shortName ) {
            this.experimentId = experimentId;
            this.shortName = shortName;
        }

        public Long getExperimentId() {
            return experimentId;
        }

        public String getShortName() {
            return shortName;
        }
    }

    /**
     * Whitespace + character whitelist. Permitted: letters, digits, dot, underscore, hyphen.
     * Matches the values present in the existing INVESTIGATION.SHORT_NAME column
     * (e.g. {@code GSE12345.1}, {@code E-MTAB-2025}, {@code Lopes_2026}).
     */
    private static final java.util.regex.Pattern SHORT_NAME_ALLOWED =
            java.util.regex.Pattern.compile( "[A-Za-z0-9._-]+" );

    /**
     * Cap matches the DB column (VARCHAR(255) on INVESTIGATION.SHORT_NAME).
     */
    private static final int SHORT_NAME_MAX_LENGTH = 255;

    @PUT
    @Path("/{dataset}/short-name")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Rename the shortName of a dataset",
            description = "Updates the curator-facing shortName identifier on an ExpressionExperiment. "
                    + "Returns 400 on blank/too-long/illegal-character names, 404 on unknown dataset, "
                    + "409 when the requested shortName is already in use (DB unique constraint).",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Invalid shortName (blank, too long, or contains forbidden characters).",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "The requested shortName is already in use.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<RenameDatasetResponse> renameDatasetShortName(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable RenameDatasetRequest body
    ) {
        if ( body == null || body.getShortName() == null ) {
            throw new BadRequestException( "A request body with 'shortName' is required." );
        }
        String trimmed = body.getShortName().trim();
        if ( trimmed.isEmpty() ) {
            throw new BadRequestException( "shortName must not be blank." );
        }
        if ( trimmed.length() > SHORT_NAME_MAX_LENGTH ) {
            throw new BadRequestException( "shortName exceeds " + SHORT_NAME_MAX_LENGTH + " characters." );
        }
        if ( !SHORT_NAME_ALLOWED.matcher( trimmed ).matches() ) {
            throw new BadRequestException( "shortName may only contain letters, digits, '.', '_', and '-'." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        String previous = ee.getShortName();
        if ( trimmed.equals( previous ) ) {
            // No-op: idempotent rename to the same value. Skip mutation + audit; return success.
            return respond( new RenameDatasetResponse( ee.getId(), ee.getShortName() ) );
        }
        // Uniqueness is enforced by the UNIQUE KEY on INVESTIGATION.SHORT_NAME; the explicit
        // existsByShortName check turns that into a 409 instead of a 500 on DataIntegrityViolation.
        if ( expressionExperimentService.existsByShortName( trimmed ) ) {
            throw new jakarta.ws.rs.ClientErrorException(
                    "shortName '" + trimmed + "' is already in use.",
                    jakarta.ws.rs.core.Response.Status.CONFLICT );
        }
        ee.setShortName( trimmed );
        expressionExperimentService.update( ee );
        //noinspection deprecation
        auditTrailService.addUpdateEvent( ee, DatasetShortNameChangedEvent.class,
                "Renamed shortName: '" + previous + "' -> '" + trimmed + "'" );
        return respond( new RenameDatasetResponse( ee.getId(), ee.getShortName() ) );
    }

    @PATCH
    @Path("/{dataset}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the name and/or description of a dataset",
            description = "Partial update of the curator-editable basics of an ExpressionExperiment: `name` "
                    + "(the human-readable title) and `description`. A field omitted or null is left "
                    + "unchanged; a provided `name` must be non-blank. The `shortName` (identity) has its own "
                    + "admin-only route (`PUT /{dataset}/short-name`). Requires `ACL_SECURABLE_EDIT` on the "
                    + "dataset. Closes the name/description half of the retired gemma-web `updateBasics`.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Neither name nor description supplied, or a blank name.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "403", description = "The caller lacks edit permission on the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<DatasetBasicsResponse> updateDatasetBasics(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable DatasetBasicsUpdateRequest body
    ) {
        if ( body == null || ( body.getName() == null && body.getDescription() == null ) ) {
            throw new BadRequestException( "A request body with a 'name' and/or 'description' is required." );
        }
        String name = body.getName() != null ? body.getName().trim() : null;
        if ( name != null && name.isEmpty() ) {
            throw new BadRequestException( "name must not be blank." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        boolean changed = expressionExperimentService.updateNameAndDescription( ee, name, body.getDescription() );
        if ( changed ) {
            //noinspection deprecation
            auditTrailService.addUpdateEvent( ee, "Updated dataset basics ("
                    + ( name != null ? "name" : "" )
                    + ( name != null && body.getDescription() != null ? " + " : "" )
                    + ( body.getDescription() != null ? "description" : "" ) + ")" );
        }
        return respond( new DatasetBasicsResponse( ee.getId(), ee.getName(), ee.getDescription() ) );
    }

    /**
     * Request body for {@link #updateDatasetBasics}. Both fields optional; a null field is left unchanged.
     */
    public static class DatasetBasicsUpdateRequest {
        @Nullable
        private String name;
        @Nullable
        private String description;

        @Nullable
        public String getName() {
            return name;
        }

        public void setName( @Nullable String name ) {
            this.name = name;
        }

        @Nullable
        public String getDescription() {
            return description;
        }

        public void setDescription( @Nullable String description ) {
            this.description = description;
        }
    }

    /**
     * Response body for {@link #updateDatasetBasics} — the persisted name and description after the update.
     */
    public static class DatasetBasicsResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("experimentId")
        private final Long experimentId;
        private final String name;
        private final String description;

        public DatasetBasicsResponse( Long experimentId, String name, String description ) {
            this.experimentId = experimentId;
            this.name = name;
            this.description = description;
        }

        public Long getExperimentId() {
            return experimentId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    // ─────────────────────────── Composite curation commit ───────────────────────────
    // All-or-none commit of a curator's whole draft. All six sections now apply: basics, publications,
    // design, tags, sampleCharacteristics, and curationDetails (curationNote only — troubled /
    // needsAttention stay on the ticket endpoints and 400 here).
    // Envelope source of truth: CAB's curation_commit.py.

    @PUT
    @Path("/{dataset}/curation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Commit a curation draft to a dataset (all-or-none)",
            description = "Applies a whole curation draft (CurationDocument) in one transaction — either every "
                    + "supported section applies or, on any failure, nothing does. Sections: `basics` "
                    + "(name/description/shortName), `publications`, `design` (factors → factor-values → statements, "
                    + "per-sample assignments, baseline flags, split advice), `tags` (experiment-level), "
                    + "`sampleCharacteristics` (per-sample), and `curationDetails` (curationNote only — troubled / "
                    + "needsAttention 400 here and go through the ticket endpoints). New entities carry a `clientRef` "
                    + "(echoed as `clientRef → newGemmaId` in the report `idMap`); "
                    + "deletions are declared via each section's `deletedIds`. A design change that would delete "
                    + "differential-expression analyses requires `?force=true` (admin) or returns 409. "
                    + "Optimistic concurrency: `baseline.lastModified` (the dataset `lastUpdated` the draft was "
                    + "built against) is checked; a stale baseline returns 409. Requires `ACL_SECURABLE_EDIT`; a "
                    + "shortName change additionally requires admin. Returns a CurationCommitReport.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Malformed body, or an unsupported section was supplied.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "403", description = "Missing edit (or admin, for shortName) permission.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "The dataset moved since the draft's baseline.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<CurationCommitReport> commitCuration(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Consent (admin only) to deleting differential-expression analyses that a design-section change would invalidate. Ignored unless the design section triggers such a cascade.") @QueryParam("force") @DefaultValue("false") Boolean force,
            @Nullable CurationDocument body
    ) {
        return respond( doCommitCuration( datasetArg, body, false, force ) );
    }

    @POST
    @Path("/{dataset}/curation/preflight")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Preflight (dry-run) a curation draft",
            description = "Same body and validation as the commit, but writes nothing: returns the "
                    + "CurationCommitReport with `applied=false` and the per-section change counts, so the UI "
                    + "can preview the diff before committing.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Malformed body, or an unsupported section was supplied.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "The dataset moved since the draft's baseline.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<CurationCommitReport> preflightCuration(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable CurationDocument body
    ) {
        // A dry run never writes, so the differential-expression cascade never fires — force is irrelevant here.
        return respond( doCommitCuration( datasetArg, body, true, false ) );
    }

    private CurationCommitReport doCommitCuration( DatasetArg<?> datasetArg, @Nullable CurationDocument body, boolean dryRun, boolean force ) {
        if ( body == null ) {
            throw new BadRequestException( "A CurationDocument request body is required." );
        }

        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );

        CurationCommitRequest request = new CurationCommitRequest();
        request.setExpectedLastUpdated( parseBaselineToken( body.getBaseline() != null ? body.getBaseline().getLastModified() : null ) );

        // Accumulate ontology-term grounding failures across every new/changed annotation in the document and
        // reject the whole commit at once (below) — the last checkpoint before a hallucinated term is persisted.
        List<OntologyTermValidationException.Located> termViolations = new ArrayList<>();
        // Accepted near-match / blank-fill label rewrites the validator applied to persisted annotations (tags +
        // sampleCharacteristics), echoed back in the report so the UI can silently update its chip labels.
        List<Canonicalization> canonicalizations = new ArrayList<>();

        if ( body.getBasics() != null ) {
            CurationBasics b = body.getBasics();
            String name = b.getName() != null ? b.getName().trim() : null;
            if ( name != null && name.isEmpty() ) {
                throw new BadRequestException( "basics.name must not be blank." );
            }
            request.setBasicsPresent( true );
            request.setName( name );
            request.setDescription( b.getDescription() );
            request.setShortName( b.getShortName() );
            request.setShortNameChangeAllowed( SecurityUtil.isUserAdmin() );
        }

        if ( body.getPublications() != null ) {
            CurationPublications pubs = body.getPublications();
            if ( pubs.getOtherRelevant() == null ) {
                throw new BadRequestException( "publications.otherRelevant is required (use an empty list to clear)." );
            }
            request.setPublicationsPresent( true );
            // Resolve identifiers -> references BEFORE the commit transaction (PubMed/CrossRef fetch is slow).
            request.setPrimaryPublication( resolvePublication( pubs.getPrimary() ) );
            List<BibliographicReference> other = new ArrayList<>();
            for ( PublicationEntry id : pubs.getOtherRelevant() ) {
                BibliographicReference ref = resolvePublication( id );
                if ( ref == null ) {
                    throw new BadRequestException( "Each publications.otherRelevant entry needs a non-blank 'pubMedId' or 'doi'." );
                }
                other.add( ref );
            }
            request.setOtherRelevantPublications( other );
        }

        if ( body.getDesign() != null ) {
            DesignCommit dc = body.getDesign();
            // Map CAB's declared-delete DesignCommit onto a COMPLETE ExperimentalDesignValueObject (carry-forward
            // untouched entities + delta) so the shipped replace-by-absence apply yields CAB's semantics; the plan
            // carries the clientRef ledgers + deferred new-FV assignments the service needs after apply. Resolving
            // the current design + GSM→biomaterial index here (before the tx) mirrors how publications resolve above.
            ExperimentalDesignValueObject current = datasetArgService.getExperimentalDesign( datasetArg );
            Map<String, Long> gsmToBmId = buildGsmToBioMaterialIdIndex( ee );
            DesignCommitPlan plan = new DesignCommitPlan();
            ExperimentalDesignValueObject proposed = mapDesignCommit( dc, current, gsmToBmId, plan );
            request.setDesignPresent( true );
            request.setProposedDesign( proposed );
            request.setDesignPlan( plan );
            request.setSplitOnFactorId( dc.getShouldSplitOnFactorId() );
            request.setSplitRationale( dc.getShouldSplitRationale() );

            // Ground-check the ontology terms on the asserted factors / factor-value statements (same gate as
            // tags + sampleCharacteristics). Only items present in the commit are checked; pure carry-forward
            // entities aren't. Rejection only here — the near-match canonicalization the tag path applies would
            // need to be threaded into mapStatements' VO build, which is out of scope for this gate.
            collectDesignTermViolations( dc, termViolations );

            // Gate on the same preflight the standalone PUT /design uses: blockers → 400; a change with
            // consequences the curator has to agree to → 409 unless force (admin). A dry run predicts, so it
            // never 409s.
            DesignPreflightReport report = datasetArgService.previewDesignChange( datasetArg, proposed );
            if ( !report.getBlockers().isEmpty() ) {
                throw new BadRequestException( "The proposed design has validation blockers: " + summarizeDesignBlockers( report ) );
            }
            if ( !dryRun && report.requiresForce() && !( force && SecurityUtil.isUserAdmin() ) ) {
                throw new jakarta.ws.rs.ClientErrorException( summarizeDesignConsequences( report )
                        + "; retry with ?force=true (admin only) to consent.",
                        jakarta.ws.rs.core.Response.Status.CONFLICT );
            }
        }

        if ( body.getTags() != null ) {
            Section<TagCommit> ts = body.getTags();
            request.setTagsPresent( true );
            List<CurationCommitRequest.TagAdd> adds = new ArrayList<>();
            int unchanged = 0;
            int idx = 0;
            for ( TagCommit tc : nullSafe( ts.getItems() ) ) {
                if ( isExisting( tc, "tags item" ) ) {
                    unchanged++; // gemmaId item = keep (absence never deletes; deletions are declared)
                } else {
                    Characteristic ch = tagCommitToCharacteristic( tc );
                    collectTermViolations( ch, "tags[" + refOrIndex( tc.getClientRef(), idx ) + "]", tc.getClientRef(), termViolations, canonicalizations );
                    adds.add( new CurationCommitRequest.TagAdd( tc.getClientRef(), ch ) );
                }
                idx++;
            }
            request.setTagsToAdd( adds );
            request.setTagsToDelete( new ArrayList<>( nullSafe( ts.getDeletedIds() ) ) );
            request.setTagsUnchanged( unchanged );
        }

        if ( body.getSampleCharacteristics() != null ) {
            Section<SampleCharacteristicCommit> scs = body.getSampleCharacteristics();
            request.setSampleCharsPresent( true );
            Map<String, Long> gsmToBmId = buildGsmToBioMaterialIdIndex( ee );
            List<CurationCommitRequest.SampleCharacteristicAdd> adds = new ArrayList<>();
            int unchanged = 0;
            int idx = 0;
            for ( SampleCharacteristicCommit sc : nullSafe( scs.getItems() ) ) {
                if ( isExisting( sc, "sampleCharacteristics item" ) ) {
                    unchanged++;
                } else {
                    if ( StringUtils.isBlank( sc.getBioassayShortName() ) ) {
                        throw new BadRequestException( "Each new sampleCharacteristics item needs a 'bioassayShortName'." );
                    }
                    Long bmId = gsmToBmId.get( sc.getBioassayShortName().trim() );
                    if ( bmId == null ) {
                        throw new BadRequestException( "sampleCharacteristics references unknown sample short name '"
                                + sc.getBioassayShortName() + "' for this dataset." );
                    }
                    Characteristic ch = sampleCharacteristicToCharacteristic( sc );
                    collectTermViolations( ch, "sampleCharacteristics[" + refOrIndex( sc.getClientRef(), idx ) + "]", sc.getClientRef(), termViolations, canonicalizations );
                    adds.add( new CurationCommitRequest.SampleCharacteristicAdd( sc.getClientRef(), bmId, ch ) );
                }
                idx++;
            }
            request.setSampleCharsToAdd( adds );
            request.setSampleCharsToDelete( new ArrayList<>( nullSafe( scs.getDeletedIds() ) ) );
            request.setSampleCharsUnchanged( unchanged );
        }

        if ( body.getCurationDetails() != null ) {
            CurationDetailsCommit cd = body.getCurationDetails();
            // troubled / needsAttention are ticket-backed on the read side and can't join this transaction cleanly.
            if ( cd.getTroubled() != null || cd.getNeedsAttention() != null ) {
                throw new BadRequestException( "curationDetails.troubled / needsAttention are not settable through the "
                        + "composite commit (they route through the ticket layer — use the /datasets/{id}/tickets endpoints). "
                        + "Only curationNote commits here." );
            }
            request.setCurationDetailsPresent( true );
            request.setCurationDetailsNote( cd.getCurationNote() );
        }

        // ── run provenance: name the agent run applying this, if the caller gave one ──
        // A preflight carries it too so the shape is validated on the dry run, but a dry run mints no row.
        if ( body.getRun() != null ) {
            CurationRunRef run = body.getRun();
            if ( StringUtils.isNotBlank( run.getRunId() ) ) {
                request.setRunId( run.getRunId().trim() );
            } else if ( run.getAgentName() != null || run.getModel() != null || run.getRunSha() != null
                    || run.getAgentVersion() != null || run.getRanAt() != null ) {
                // Provenance with no run to hang it off cannot be stored and must not be dropped silently — the
                // caller believes it recorded something. Say so rather than accepting a write that loses it.
                throw new BadRequestException( "run.runId is required when any other run field is supplied: "
                        + "the run reference is what a COMMIT annotation set is keyed on." );
            }
            request.setRunProvenance( new AnnotationSetService.RunProvenance(
                    run.getAgentVersion(), run.getModel(), run.getRunSha(), run.getAgentName(),
                    parseRanAt( run.getRanAt() ) ) );
            if ( run.getProposalSetId() != null ) {
                request.setRunParentProposal( requireProposalFor( run.getProposalSetId(), ee ) );
            }
        }

        // Every new/changed annotation has now been ground-checked; reject the whole commit if any term failed
        // (applies equally to preflight, so a client catches these on the dry run).
        if ( !termViolations.isEmpty() ) {
            throw new OntologyTermValidationException( termViolations );
        }

        CurationCommitResult result;
        try {
            result = expressionExperimentService.commitCuration( ee, request, dryRun );
        } catch ( org.springframework.dao.OptimisticLockingFailureException e ) {
            throw new jakarta.ws.rs.ClientErrorException( e.getMessage(), jakarta.ws.rs.core.Response.Status.CONFLICT );
        } catch ( org.springframework.security.access.AccessDeniedException e ) {
            throw new jakarta.ws.rs.ForbiddenException( e.getMessage() );
        } catch ( IllegalArgumentException e ) {
            // e.g. shortName already in use
            throw new jakarta.ws.rs.ClientErrorException( e.getMessage(), jakarta.ws.rs.core.Response.Status.CONFLICT );
        }
        return CurationCommitReport.from( result, request, !dryRun, canonicalizations );
    }

    /**
     * Parse a run's {@code ranAt} stamp. Same lenient contract as the baseline token — epoch millis or ISO-8601,
     * with an unparseable value yielding null rather than failing the commit. Provenance is recorded, not
     * enforced: refusing a whole curation because a timestamp was formatted oddly would trade a real write for a
     * cosmetic field.
     */
    @Nullable
    private static java.util.Date parseRanAt( @Nullable String token ) {
        return parseBaselineToken( token );
    }

    /**
     * Parse the baseline concurrency token (the dataset {@code lastUpdated} the draft was built against).
     * Accepts epoch milliseconds or an ISO-8601 instant; blank/unparseable yields {@code null} (the check is
     * then skipped rather than failing the commit — lenient while clients settle on the format).
     */
    @Nullable
    private static java.util.Date parseBaselineToken( @Nullable String token ) {
        if ( StringUtils.isBlank( token ) ) {
            return null;
        }
        String t = token.trim();
        try {
            return new java.util.Date( Long.parseLong( t ) );
        } catch ( NumberFormatException ignored ) {
            // fall through to ISO parsing
        }
        try {
            return java.util.Date.from( java.time.Instant.parse( t ) );
        } catch ( java.time.format.DateTimeParseException ignored ) {
            return null;
        }
    }

    /**
     * The whole desired curation state for one dataset (CAB's {@code CurationDocument}). Any section left
     * null is untouched. All six sections apply: {@code basics}, {@code publications}, {@code design},
     * {@code tags}, {@code sampleCharacteristics}, and {@code curationDetails} (note only — troubled /
     * needsAttention are 400 here and go through the ticket endpoints).
     */
    public static class CurationDocument {
        @Nullable
        private CurationBaseline baseline;
        @Nullable
        private CurationBasics basics;
        @Nullable
        private CurationPublications publications;
        @Nullable
        private DesignCommit design;
        @Nullable
        private Section<TagCommit> tags;
        @Nullable
        private Section<SampleCharacteristicCommit> sampleCharacteristics;
        @Nullable
        private CurationDetailsCommit curationDetails;
        @Nullable
        private CurationRunRef run;

        @Nullable
        public CurationBaseline getBaseline() { return baseline; }
        public void setBaseline( @Nullable CurationBaseline baseline ) { this.baseline = baseline; }
        @Nullable
        public CurationBasics getBasics() { return basics; }
        public void setBasics( @Nullable CurationBasics basics ) { this.basics = basics; }
        @Nullable
        public CurationPublications getPublications() { return publications; }
        public void setPublications( @Nullable CurationPublications publications ) { this.publications = publications; }
        @Nullable
        public DesignCommit getDesign() { return design; }
        public void setDesign( @Nullable DesignCommit design ) { this.design = design; }
        @Nullable
        public Section<TagCommit> getTags() { return tags; }
        public void setTags( @Nullable Section<TagCommit> tags ) { this.tags = tags; }
        @Nullable
        public Section<SampleCharacteristicCommit> getSampleCharacteristics() { return sampleCharacteristics; }
        public void setSampleCharacteristics( @Nullable Section<SampleCharacteristicCommit> n ) { this.sampleCharacteristics = n; }
        @Nullable
        public CurationDetailsCommit getCurationDetails() { return curationDetails; }
        public void setCurationDetails( @Nullable CurationDetailsCommit n ) { this.curationDetails = n; }
        @Nullable
        public CurationRunRef getRun() { return run; }
        public void setRun( @Nullable CurationRunRef run ) { this.run = run; }
    }

    /**
     * Which agent run is applying this commit. Optional, and absent for an ordinary curator commit.
     * <p>
     * Keyed on Gemma's own AnnotationSet id, carrying the producing side's run reference as attributes — CAB's
     * ruling, and the right one: their {@code runId} is a human-authored label in a foreign namespace, it is 1:N
     * against a commit (one run writes many experiments), and it is not stable under resume.
     * <p>
     * Supplying {@code runId} mints a {@code COMMIT} AnnotationSet in the commit's own transaction; omitting it
     * mints nothing. The other four fields are recorded verbatim and never interpreted. {@code runSha} is not
     * redundant with {@code model}: behaviour differs between shas at one model, so the model alone does not
     * identify the build that wrote an annotation.
     */
    public static class CurationRunRef {
        @Nullable
        private String runId;
        @Nullable
        private String agentName;
        @Nullable
        private String agentVersion;
        @Nullable
        private String model;
        @Nullable
        private String runSha;
        @Nullable
        private String ranAt;
        /**
         * Id of the PROPOSAL annotation set this commit is applying, if it is applying one. Becomes the COMMIT
         * row's parent, so the trail reads proposal -> decision -> effect. Must belong to this dataset and be a
         * PROPOSAL.
         */
        @Nullable
        private Long proposalSetId;

        @Nullable
        public String getRunId() { return runId; }
        public void setRunId( @Nullable String runId ) { this.runId = runId; }
        @Nullable
        public String getAgentName() { return agentName; }
        public void setAgentName( @Nullable String agentName ) { this.agentName = agentName; }
        @Nullable
        public String getAgentVersion() { return agentVersion; }
        public void setAgentVersion( @Nullable String agentVersion ) { this.agentVersion = agentVersion; }
        @Nullable
        public String getModel() { return model; }
        public void setModel( @Nullable String model ) { this.model = model; }
        @Nullable
        public String getRunSha() { return runSha; }
        public void setRunSha( @Nullable String runSha ) { this.runSha = runSha; }
        @Nullable
        public String getRanAt() { return ranAt; }
        public void setRanAt( @Nullable String ranAt ) { this.ranAt = ranAt; }
        @Nullable
        public Long getProposalSetId() { return proposalSetId; }
        public void setProposalSetId( @Nullable Long proposalSetId ) { this.proposalSetId = proposalSetId; }
    }

    public static class CurationBaseline {
        @Nullable
        private String lastModified;
        @Nullable
        public String getLastModified() { return lastModified; }
        public void setLastModified( @Nullable String lastModified ) { this.lastModified = lastModified; }
    }

    public static class CurationBasics {
        @Nullable
        private String name;
        @Nullable
        private String description;
        @Nullable
        private String shortName;
        @Nullable
        public String getName() { return name; }
        public void setName( @Nullable String name ) { this.name = name; }
        @Nullable
        public String getDescription() { return description; }
        public void setDescription( @Nullable String description ) { this.description = description; }
        @Nullable
        public String getShortName() { return shortName; }
        public void setShortName( @Nullable String shortName ) { this.shortName = shortName; }
    }

    /** Publications section — same identifier shape and set-replace semantics as {@code PUT /publications}. */
    public static class CurationPublications {
        @Nullable
        private PublicationEntry primary;
        @Nullable
        private List<PublicationEntry> otherRelevant;
        @Nullable
        public PublicationEntry getPrimary() { return primary; }
        public void setPrimary( @Nullable PublicationEntry primary ) { this.primary = primary; }
        @Nullable
        public List<PublicationEntry> getOtherRelevant() { return otherRelevant; }
        public void setOtherRelevant( @Nullable List<PublicationEntry> otherRelevant ) { this.otherRelevant = otherRelevant; }
    }

    // ── Design section DTOs (mirror CAB's curation_commit.py: DesignCommit / FactorCommit / … ) ──
    // Every committable entity carries an EntityRef: exactly one of gemmaId (existing, matched by id) or clientRef
    // (new, created server-side and echoed clientRef → newGemmaId in the report idMap). Every collection is a
    // {items, deletedIds} Section: absence never deletes — only ids in deletedIds are removed. The mapper
    // (mapDesignCommit) validates the gemmaId-XOR-clientRef rule and translates this onto an ExperimentalDesignVO.

    /** Identity half of every committable design entity: exactly one of {@code gemmaId} or {@code clientRef}. */
    @Data
    public static class EntityRef {
        @Nullable
        private Long gemmaId;
        @Nullable
        private String clientRef;
    }

    /** An ontology term reference — a human label plus an optional ontology URI. (Named to avoid clashing with the core {@code OntologyTerm}.) */
    @Data
    public static class OntologyTermRef {
        @Nullable
        private String label;
        @Nullable
        private String uri;
    }

    /** A per-factor-value numeric measurement (continuous factors). */
    @Data
    public static class Measurement {
        @Nullable
        private String value;
        @Nullable
        private String unit;
        @Nullable
        private String type;
        @Nullable
        private String representation;
    }

    /** A committable collection: authoritative {@code items} plus explicit {@code deletedIds} (the only way to remove). */
    @Data
    public static class Section<T> {
        private List<T> items = new ArrayList<>();
        private List<Long> deletedIds = new ArrayList<>();
    }

    /** The experimental-design section (CAB {@code DesignCommit}). */
    @Data
    public static class DesignCommit {
        private Section<FactorCommit> factors = new Section<>();
        /** Curator split advice (factor id, or {@code -1} for "do not split"); recorded in the curation note. */
        @Nullable
        private Long shouldSplitOnFactorId;
        @Nullable
        private String shouldSplitRationale;
    }

    /** One experimental factor. */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class FactorCommit extends EntityRef {
        @Nullable
        private String name;
        @Nullable
        private OntologyTermRef category;
        @Nullable
        private String description;
        /** {@code "categorical"} | {@code "continuous"}. */
        @Nullable
        private String type;
        private Section<FactorValueCommit> factorValues = new Section<>();
    }

    /** One factor value, with the samples it applies to (by GSM short name) and its statements. */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class FactorValueCommit extends EntityRef {
        @Nullable
        private String freeTextLabel;
        /** {@code null} = leave the baseline flag unchanged. */
        @com.fasterxml.jackson.annotation.JsonProperty("isBaseline")
        @Nullable
        private Boolean baseline;
        @Nullable
        private Measurement measurement;
        /** {@code null} (or omitted) = leave sample assignments untouched; a list ({@code []} = clear) = set-replace. */
        @Nullable
        private List<String> biomaterialShortNames;
        private Section<StatementCommit> statements = new Section<>();
    }

    /** One statement (subject / predicate / object triple with an optional category). */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class StatementCommit extends EntityRef {
        @Nullable
        private OntologyTermRef category;
        @Nullable
        private OntologyTermRef subject;
        @Nullable
        private OntologyTermRef predicate;
        @Nullable
        private OntologyTermRef object;
        /**
         * Verbatim provenance for this statement — a JSON array of {@code {quote, source, location, …}} items.
         * Stored and served opaquely; the agents repo owns the schema.
         * <p>
         * The statement is the level that matters most for composed patterns, where the operative claim lives in
         * the triple rather than in the parent factor value: two factor values whose labels are byte-identical
         * and differ only by a zygosity statement cannot be told apart by evidence hung on the value.
         * <p>
         * Null / omitted leaves any evidence already recorded untouched, so a client that does not carry
         * provenance cannot wipe provenance somebody else recorded.
         */
        @Nullable
        private com.fasterxml.jackson.databind.JsonNode supportingEvidence;
    }

    /** One experiment-level tag (CAB {@code TagCommit}); a statement-shaped tag rides its {@code statements}. */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TagCommit extends EntityRef {
        @Nullable
        private OntologyTermRef category;
        @Nullable
        private OntologyTermRef value;
        private Section<StatementCommit> statements = new Section<>();
        /**
         * Verbatim provenance for this tag. Same shape and same null = "no change" convention as
         * {@link StatementCommit#getSupportingEvidence()}. When the tag rides a statement, evidence set on the
         * statement wins; this is the fallback for a plain category/value tag.
         */
        @Nullable
        private com.fasterxml.jackson.databind.JsonNode supportingEvidence;
    }

    /** One per-sample characteristic (CAB {@code SampleCharacteristicCommit}); the sample is a GSM short name. */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SampleCharacteristicCommit extends EntityRef {
        @Nullable
        private String bioassayShortName;
        @Nullable
        private OntologyTermRef category;
        @Nullable
        private OntologyTermRef value;
        /**
         * Verbatim provenance for this sample characteristic. Same shape and same null = "no change" convention
         * as {@link StatementCommit#getSupportingEvidence()}.
         */
        @Nullable
        private com.fasterxml.jackson.databind.JsonNode supportingEvidence;
    }

    /** curationDetails section. Only {@code curationNote} commits here; the flags go through the ticket layer. */
    @Data
    public static class CurationDetailsCommit {
        @Nullable
        private Boolean troubled;
        @Nullable
        private Boolean needsAttention;
        @Nullable
        private String curationNote;
    }

    // ── Design mapping (wire DesignCommit → complete ExperimentalDesignValueObject + DesignCommitPlan) ──

    /**
     * Translate CAB's declared-delete {@link DesignCommit} into a COMPLETE {@link ExperimentalDesignValueObject}
     * (carry-forward every untouched entity + apply the delta) so the shipped replace-by-absence design apply yields
     * CAB's semantics without forking it. Records the clientRef ledgers and the deferred new-factor-value sample
     * assignments into {@code plan} for the service's post-apply correlation and second pass.
     */
    private ExperimentalDesignValueObject mapDesignCommit( DesignCommit dc, ExperimentalDesignValueObject current,
            Map<String, Long> gsmToBmId, DesignCommitPlan plan ) {
        Map<Long, ExperimentalDesignValueObject.ExperimentalFactorEntry> curFactors = new LinkedHashMap<>();
        Set<Long> preFvIds = new HashSet<>();
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry f : nullSafe( current.getExperimentalFactors() ) ) {
            if ( f.getId() != null ) {
                curFactors.put( f.getId(), f );
            }
            for ( FactorValueBasicValueObject v : nullSafe( f.getValues() ) ) {
                if ( v.getId() != null ) {
                    preFvIds.add( v.getId() );
                }
            }
        }
        plan.setPreExistingFactorIds( new HashSet<>( curFactors.keySet() ) );
        plan.setPreExistingFactorValueIds( preFvIds );

        // The complete desired bm → fv-id map: start from current (carry-forward), then per-FV set-replace below.
        Map<Long, Set<Long>> bmToFvIds = new LinkedHashMap<>();
        Map<Long, String> bmNames = new HashMap<>();
        for ( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a : nullSafe( current.getBioMaterialAssignments() ) ) {
            bmToFvIds.put( a.getBioMaterialId(), new LinkedHashSet<>( nullSafe( a.getFactorValueIds() ) ) );
            bmNames.put( a.getBioMaterialId(), a.getBioMaterialName() );
        }

        Section<FactorCommit> fs = dc.getFactors() != null ? dc.getFactors() : new Section<>();
        Set<Long> factorDeleted = new HashSet<>( nullSafe( fs.getDeletedIds() ) );
        Set<Long> mentionedFactorIds = new HashSet<>();
        List<ExperimentalDesignValueObject.ExperimentalFactorEntry> outFactors = new ArrayList<>();

        for ( FactorCommit fc : nullSafe( fs.getItems() ) ) {
            String parentKey;
            ExperimentalDesignValueObject.ExperimentalFactorEntry curFactor = null;
            ExperimentalDesignValueObject.ExperimentalFactorEntry out = new ExperimentalDesignValueObject.ExperimentalFactorEntry();
            if ( isExisting( fc, "design.factors item" ) ) {
                curFactor = curFactors.get( fc.getGemmaId() );
                if ( curFactor == null ) {
                    throw new BadRequestException( "design.factors references unknown factor id " + fc.getGemmaId() + "." );
                }
                mentionedFactorIds.add( fc.getGemmaId() );
                out.setId( fc.getGemmaId() );
                parentKey = DesignCommitPlan.existingFactorKey( fc.getGemmaId() );
            } else {
                out.setId( null );
                plan.getNewFactorClientRefs().add( fc.getClientRef() );
                parentKey = DesignCommitPlan.newFactorKey( fc.getClientRef() );
            }
            out.setName( fc.getName() );
            out.setDescription( fc.getDescription() );
            out.setType( fc.getType() );
            out.setCategory( ontologyToCharacteristic( fc.getCategory() ) );
            out.setValues( mapFactorValues( fc, curFactor, parentKey, gsmToBmId, plan, bmToFvIds ) );
            outFactors.add( out );
        }

        // Carry forward untouched current factors verbatim (id + all FVs); their assignments already live in bmToFvIds.
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry cur : nullSafe( current.getExperimentalFactors() ) ) {
            if ( cur.getId() != null && !mentionedFactorIds.contains( cur.getId() ) && !factorDeleted.contains( cur.getId() ) ) {
                outFactors.add( cur );
            }
        }

        // Scrub any assignment that points at a factor value no longer present (deleted factor or deleted FV).
        Set<Long> survivingFvIds = new HashSet<>();
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry f : outFactors ) {
            for ( FactorValueBasicValueObject v : nullSafe( f.getValues() ) ) {
                if ( v.getId() != null ) {
                    survivingFvIds.add( v.getId() );
                }
            }
        }
        for ( Set<Long> set : bmToFvIds.values() ) {
            set.retainAll( survivingFvIds );
        }

        ExperimentalDesignValueObject out = new ExperimentalDesignValueObject();
        out.setId( current.getId() );
        out.setExperimentalFactors( outFactors );
        out.setBioMaterialAssignments( buildAssignmentList( bmToFvIds, bmNames ) );
        return out;
    }

    private List<FactorValueBasicValueObject> mapFactorValues( FactorCommit fc,
            @Nullable ExperimentalDesignValueObject.ExperimentalFactorEntry curFactor, String parentKey,
            Map<String, Long> gsmToBmId, DesignCommitPlan plan, Map<Long, Set<Long>> bmToFvIds ) {
        Map<Long, FactorValueBasicValueObject> curFvs = new LinkedHashMap<>();
        if ( curFactor != null ) {
            for ( FactorValueBasicValueObject v : nullSafe( curFactor.getValues() ) ) {
                if ( v.getId() != null ) {
                    curFvs.put( v.getId(), v );
                }
            }
        }
        Section<FactorValueCommit> fvs = fc.getFactorValues() != null ? fc.getFactorValues() : new Section<>();
        Set<Long> fvDeleted = new HashSet<>( nullSafe( fvs.getDeletedIds() ) );
        Set<Long> mentionedFvIds = new HashSet<>();
        List<String> fvClientRefs = new ArrayList<>();
        List<FactorValueBasicValueObject> outValues = new ArrayList<>();

        for ( FactorValueCommit fvc : nullSafe( fvs.getItems() ) ) {
            // null biomaterialShortNames = leave this FV's sample assignments untouched; a (possibly empty) list =
            // authoritative set-replace ([] clears). Same null-means-unchanged convention as isBaseline.
            boolean assignmentsGiven = fvc.getBiomaterialShortNames() != null;
            Set<Long> bmIds = assignmentsGiven ? resolveBioMaterials( fvc.getBiomaterialShortNames(), gsmToBmId ) : Collections.emptySet();
            FactorValueBasicValueObject out = new FactorValueBasicValueObject();
            if ( isExisting( fvc, "factor value" ) ) {
                if ( !curFvs.containsKey( fvc.getGemmaId() ) ) {
                    throw new BadRequestException( "design.factors references unknown factor value id " + fvc.getGemmaId() + "." );
                }
                mentionedFvIds.add( fvc.getGemmaId() );
                out.setId( fvc.getGemmaId() );
                if ( assignmentsGiven ) {
                    // Drop this factor value everywhere, then add it to exactly the listed samples (empty = clear).
                    for ( Set<Long> set : bmToFvIds.values() ) {
                        set.remove( fvc.getGemmaId() );
                    }
                    for ( Long bmId : bmIds ) {
                        bmToFvIds.computeIfAbsent( bmId, k -> new LinkedHashSet<>() ).add( fvc.getGemmaId() );
                    }
                }
                // else: leave the current memberships carried forward in bmToFvIds untouched.
            } else {
                out.setId( null );
                fvClientRefs.add( fvc.getClientRef() );
                // The id doesn't exist yet — defer the assignment to the service's second pass.
                if ( assignmentsGiven && !bmIds.isEmpty() ) {
                    plan.getPendingAssignments().add( new DesignCommitPlan.PendingAssignment( fvc.getClientRef(), bmIds ) );
                }
            }
            //noinspection deprecation
            out.setValue( fvc.getFreeTextLabel() );
            out.setBaseline( fvc.getBaseline() );
            out.setMeasurementObject( mapMeasurement( fvc.getMeasurement() ) );
            out.setStatements( mapStatements( fvc, curFvs.get( fvc.getGemmaId() ) ) );
            outValues.add( out );
        }

        // Carry forward untouched current factor values (declared-delete: only ids in deletedIds are removed).
        for ( FactorValueBasicValueObject v : nullSafe( curFactor != null ? curFactor.getValues() : null ) ) {
            if ( v.getId() != null && !mentionedFvIds.contains( v.getId() ) && !fvDeleted.contains( v.getId() ) ) {
                outValues.add( v );
            }
        }
        if ( !fvClientRefs.isEmpty() ) {
            plan.getNewFactorValueClientRefsByParentKey().put( parentKey, fvClientRefs );
        }
        return outValues;
    }

    private List<StatementValueObject> mapStatements( FactorValueCommit fvc, @Nullable FactorValueBasicValueObject curFv ) {
        Section<StatementCommit> ss = fvc.getStatements() != null ? fvc.getStatements() : new Section<>();
        Set<Long> stmtDeleted = new HashSet<>( nullSafe( ss.getDeletedIds() ) );
        Set<Long> mentioned = new HashSet<>();
        List<StatementValueObject> out = new ArrayList<>();
        for ( StatementCommit sc : nullSafe( ss.getItems() ) ) {
            StatementValueObject svo = new StatementValueObject();
            if ( isExisting( sc, "statement" ) ) {
                svo.setId( sc.getGemmaId() );
                mentioned.add( sc.getGemmaId() );
            }
            if ( sc.getCategory() != null ) {
                svo.setCategory( sc.getCategory().getLabel() );
                svo.setCategoryUri( sc.getCategory().getUri() );
            }
            if ( sc.getSubject() != null ) {
                svo.setSubject( sc.getSubject().getLabel() );
                svo.setSubjectUri( sc.getSubject().getUri() );
            }
            if ( sc.getPredicate() != null ) {
                svo.setPredicate( sc.getPredicate().getLabel() );
                svo.setPredicateUri( sc.getPredicate().getUri() );
            }
            if ( sc.getObject() != null ) {
                svo.setObject( sc.getObject().getLabel() );
                svo.setObjectUri( sc.getObject().getUri() );
            }
            svo.setSupportingEvidence( sc.getSupportingEvidence() );
            out.add( svo );
        }
        // Carry forward untouched current statements — the design apply replaces statements wholesale on a kept FV,
        // so an un-echoed statement would otherwise be deleted; re-emitting it (by id) preserves it.
        if ( curFv != null ) {
            for ( StatementValueObject s : nullSafe( curFv.getStatements() ) ) {
                if ( s.getId() != null && !mentioned.contains( s.getId() ) && !stmtDeleted.contains( s.getId() ) ) {
                    out.add( s );
                }
            }
        }
        return out;
    }

    /** Validate the gemmaId-XOR-clientRef rule; {@code true} = existing entity (has gemmaId), {@code false} = new. */
    private static boolean isExisting( EntityRef ref, String what ) {
        boolean hasId = ref.getGemmaId() != null;
        boolean hasRef = StringUtils.isNotBlank( ref.getClientRef() );
        if ( hasId == hasRef ) {
            throw new BadRequestException( "Each " + what + " needs exactly one of gemmaId (existing) or clientRef (new)." );
        }
        return hasId;
    }

    /** Resolve a list of GSM short names to biomaterial ids for this dataset; an unknown short name is a 400. */
    private static Set<Long> resolveBioMaterials( @Nullable List<String> shortNames, Map<String, Long> gsmToBmId ) {
        Set<Long> ids = new LinkedHashSet<>();
        for ( String sn : nullSafe( shortNames ) ) {
            if ( StringUtils.isBlank( sn ) ) {
                continue;
            }
            Long bmId = gsmToBmId.get( sn.trim() );
            if ( bmId == null ) {
                throw new BadRequestException( "design references unknown sample short name '" + sn + "' for this dataset." );
            }
            ids.add( bmId );
        }
        return ids;
    }

    /** GSM accession → biomaterial id for one dataset (no findByAccession exists; index the bioassays). */
    /**
     * Mapper for snapshot payloads. Configured to omit nulls so a snapshot records what the dataset has rather
     * than a wall of empty fields, and to ignore unknown properties on read so a payload taken by an older build
     * still restores after the document grows a field.
     */
    private static final com.fasterxml.jackson.databind.ObjectMapper SNAPSHOT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .setSerializationInclusion( com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL )
                    .configure( com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

    private static String writeSnapshotPayload( CurationDocument doc ) {
        try {
            return SNAPSHOT_MAPPER.writeValueAsString( doc );
        } catch ( com.fasterxml.jackson.core.JsonProcessingException e ) {
            throw new IllegalStateException( "Could not serialize the curation snapshot.", e );
        }
    }

    /**
     * Resolve the PROPOSAL annotation set a write claims to be applying, refusing anything that is not this
     * dataset's proposal.
     * <p>
     * Proposed-versus-applied is the distinction the provenance surface rests on, so a COMMIT must not be able to
     * name a DRAFT or a SNAPSHOT as the thing it applied.
     */
    private AnnotationSet requireProposalFor( Long setId, ExpressionExperiment ee ) {
        AnnotationSet set = annotationSetService.load( setId );
        if ( set == null ) {
            throw new NotFoundException( "No annotation set with id " + setId + "." );
        }
        if ( set.getInvestigation() == null || !ee.getId().equals( set.getInvestigation().getId() ) ) {
            throw new NotFoundException( "Annotation set " + setId + " does not belong to dataset " + ee.getId() + "." );
        }
        if ( set.getRole() != AnnotationSetRole.PROPOSAL ) {
            throw new BadRequestException( "Annotation set " + setId + " is a " + set.getRole()
                    + ", not a PROPOSAL; only a proposal can be recorded as the source of an applied change." );
        }
        return set;
    }

    /**
     * Record that a proposal was applied, as a COMMIT annotation set parented to it.
     * <p>
     * The run reference is copied off the proposal rather than asked for again: the proposal already carries the
     * run that produced it, and a second copy on the wire is a second chance to disagree with the first.
     */
    private void recordAppliedFromProposal( AnnotationSet proposal ) {
        AnnotationSetService.AttachedAnnotationSet attached = annotationSetService.attach(
                proposal.getInvestigation(), AnnotationSetRole.COMMIT, AnnotationSetSource.AGENT, null,
                proposal.getRunId(), proposal.getCreatedBy(),
                new AnnotationSetService.RunProvenance( proposal.getAgentVersion(), proposal.getModel(),
                        proposal.getRunSha(), proposal.getAgentName(), proposal.getRanAt() ),
                null, proposal );
        log.info( "PUT /design: applied AnnotationSet#" + proposal.getId() + " (run " + proposal.getRunId()
                + "); recorded as COMMIT AnnotationSet#" + attached.getAnnotationSet().getId()
                + ( attached.isCreated() ? "" : " (already recorded)" ) );
    }

    /**
     * Load a SNAPSHOT annotation set and parse its payload back into a {@link CurationDocument}, refusing
     * anything that is not this dataset's snapshot. A DRAFT or PROPOSAL payload is some other tool's shape;
     * replaying it as a commit would write whatever happened to parse.
     */
    private CurationDocument readSnapshotPayload( Long setId, ExpressionExperiment ee ) {
        AnnotationSet set = annotationSetService.load( setId );
        if ( set == null ) {
            throw new NotFoundException( "No annotation set with id " + setId + "." );
        }
        if ( set.getInvestigation() == null || !ee.getId().equals( set.getInvestigation().getId() ) ) {
            throw new NotFoundException( "Annotation set " + setId + " does not belong to dataset " + ee.getId() + "." );
        }
        if ( set.getRole() != AnnotationSetRole.SNAPSHOT ) {
            throw new BadRequestException( "Annotation set " + setId + " is a " + set.getRole()
                    + ", not a SNAPSHOT; only a snapshot can be restored." );
        }
        if ( StringUtils.isBlank( set.getPayloadJson() ) ) {
            throw new BadRequestException( "Annotation set " + setId + " has no payload to restore." );
        }
        try {
            return SNAPSHOT_MAPPER.readValue( set.getPayloadJson(), CurationDocument.class );
        } catch ( com.fasterxml.jackson.core.JsonProcessingException e ) {
            throw new BadRequestException( "Annotation set " + setId
                    + " payload is not a CurationDocument: " + e.getOriginalMessage() );
        }
    }

    /**
     * Capture a dataset's current curation as a {@link CurationDocument} — the same shape
     * {@code PUT /datasets/{id}/curation} accepts.
     * <p>
     * The shape is the point. A snapshot that is itself a commit document means "restore" is the commit path
     * we already have and "compare with the snapshot" is the preflight we already have; neither needs a second
     * diff implementation that could disagree with the first.
     * <p>
     * Every captured entity carries its {@code gemmaId}, so a snapshot replayed against an unchanged structure
     * updates in place rather than duplicating. Ids that no longer exist at restore time are reconciled by
     * {@link #reconcileSnapshotForRestore}, not here — a snapshot records what was true, not what to do about it.
     */
    private CurationDocument buildCurationSnapshot( ExpressionExperiment ee ) {
        CurationDocument doc = new CurationDocument();

        CurationBasics basics = new CurationBasics();
        basics.setName( ee.getName() );
        basics.setDescription( ee.getDescription() );
        doc.setBasics( basics );

        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( ee );
        Map<Long, String> gsmByBmId = new HashMap<>();
        for ( BioAssay ba : thawed.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm == null || bm.getId() == null ) continue;
            String name = ba.getAccession() != null && ba.getAccession().getAccession() != null
                    ? ba.getAccession().getAccession() : ba.getShortName();
            if ( name != null ) {
                gsmByBmId.putIfAbsent( bm.getId(), name );
            }
        }

        ExperimentalDesignValueObject design = expressionExperimentService.getExperimentalDesignValueObject( ee );
        if ( design != null ) {
            Map<Long, List<String>> samplesByFvId = new HashMap<>();
            for ( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a : nullSafe( design.getBioMaterialAssignments() ) ) {
                String gsm = gsmByBmId.get( a.getBioMaterialId() );
                if ( gsm == null ) continue;
                for ( Long fvId : nullSafe( a.getFactorValueIds() ) ) {
                    samplesByFvId.computeIfAbsent( fvId, k -> new ArrayList<>() ).add( gsm );
                }
            }
            DesignCommit dc = new DesignCommit();
            for ( ExperimentalDesignValueObject.ExperimentalFactorEntry f : nullSafe( design.getExperimentalFactors() ) ) {
                FactorCommit fc = new FactorCommit();
                fc.setGemmaId( f.getId() );
                fc.setName( f.getName() );
                fc.setDescription( f.getDescription() );
                fc.setType( f.getType() );
                fc.setCategory( termRef( f.getCategory() ) );
                for ( FactorValueBasicValueObject v : nullSafe( f.getValues() ) ) {
                    FactorValueCommit fvc = new FactorValueCommit();
                    fvc.setGemmaId( v.getId() );
                    //noinspection deprecation
                    fvc.setFreeTextLabel( v.getValue() );
                    fvc.setBaseline( v.getBaseline() );
                    fvc.setMeasurement( snapshotMeasurement( v.getMeasurementObject() ) );
                    // an explicit (possibly empty) list, so a restore re-asserts membership rather than
                    // leaving whatever the intervening run assigned
                    fvc.setBiomaterialShortNames( samplesByFvId.getOrDefault( v.getId(), new ArrayList<>() ) );
                    for ( StatementValueObject s : nullSafe( v.getStatements() ) ) {
                        StatementCommit sc = new StatementCommit();
                        sc.setGemmaId( s.getId() );
                        sc.setCategory( termRef( s.getCategory(), s.getCategoryUri() ) );
                        sc.setSubject( termRef( s.getSubject(), s.getSubjectUri() ) );
                        sc.setPredicate( termRef( s.getPredicate(), s.getPredicateUri() ) );
                        sc.setObject( termRef( s.getObject(), s.getObjectUri() ) );
                        sc.setSupportingEvidence( s.getSupportingEvidence() );
                        fvc.getStatements().getItems().add( sc );
                    }
                    fc.getFactorValues().getItems().add( fvc );
                }
                dc.getFactors().getItems().add( fc );
            }
            doc.setDesign( dc );
        }

        Section<TagCommit> tags = new Section<>();
        for ( AnnotationValueObject a : experimentLevelTags( ee ) ) {
            TagCommit tc = new TagCommit();
            tc.setGemmaId( a.getId() );
            tc.setCategory( termRef( a.getClassName(), a.getClassUri() ) );
            tc.setValue( termRef( a.getTermName(), a.getTermUri() ) );
            tc.setSupportingEvidence( a.getSupportingEvidence() );
            tags.getItems().add( tc );
        }
        doc.setTags( tags );

        Section<SampleCharacteristicCommit> sampleChars = new Section<>();
        for ( BioAssay ba : thawed.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm == null ) continue;
            String gsm = gsmByBmId.get( bm.getId() );
            if ( gsm == null ) continue;
            for ( AnnotationValueObject a : sampleAnnotationVos( bm ) ) {
                SampleCharacteristicCommit scc = new SampleCharacteristicCommit();
                scc.setGemmaId( a.getId() );
                scc.setBioassayShortName( gsm );
                scc.setCategory( termRef( a.getClassName(), a.getClassUri() ) );
                scc.setValue( termRef( a.getTermName(), a.getTermUri() ) );
                scc.setSupportingEvidence( a.getSupportingEvidence() );
                sampleChars.getItems().add( scc );
            }
        }
        doc.setSampleCharacteristics( sampleChars );

        if ( ee.getCurationDetails() != null && ee.getCurationDetails().getCurationNote() != null ) {
            CurationDetailsCommit cd = new CurationDetailsCommit();
            cd.setCurationNote( ee.getCurationDetails().getCurationNote() );
            doc.setCurationDetails( cd );
        }
        return doc;
    }

    /**
     * Turn a captured snapshot into a document that can actually be committed against the dataset as it stands
     * now. Two reconciliations, both of which exist because a snapshot is replayed after something changed:
     * <ol>
     *     <li><b>Vanished ids become creates.</b> An entity whose {@code gemmaId} is gone (an intervening run
     *         deleted and recreated its factor, say) is re-sent under a {@code clientRef}. The content comes
     *         back; the identity does not, and cannot — the row it named no longer exists.</li>
     *     <li><b>Entities absent from the snapshot become deletions.</b> Restoring means "make it look like the
     *         snapshot", and the commit is declared-delete, so anything added since has to be named in
     *         {@code deletedIds} or it would silently survive the restore.</li>
     * </ol>
     * Consequence worth stating plainly to callers: a restore returns the curation's <em>content</em>, not its
     * <em>identity</em>. Recreated factor values get fresh ids, and any analysis that survived the intervening
     * run is cascaded again on the way back.
     */
    private void reconcileSnapshotForRestore( CurationDocument snapshot, ExpressionExperiment ee ) {
        ExperimentalDesignValueObject current = expressionExperimentService.getExperimentalDesignValueObject( ee );
        Set<Long> liveFactorIds = new HashSet<>();
        Set<Long> liveFvIds = new HashSet<>();
        Set<Long> liveStatementIds = new HashSet<>();
        if ( current != null ) {
            for ( ExperimentalDesignValueObject.ExperimentalFactorEntry f : nullSafe( current.getExperimentalFactors() ) ) {
                liveFactorIds.add( f.getId() );
                for ( FactorValueBasicValueObject v : nullSafe( f.getValues() ) ) {
                    liveFvIds.add( v.getId() );
                    for ( StatementValueObject s : nullSafe( v.getStatements() ) ) {
                        liveStatementIds.add( s.getId() );
                    }
                }
            }
        }

        int seq = 0;
        Set<Long> snapshotFactorIds = new HashSet<>();
        Set<Long> snapshotFvIds = new HashSet<>();
        Set<Long> snapshotStatementIds = new HashSet<>();
        if ( snapshot.getDesign() != null ) {
            for ( FactorCommit fc : nullSafe( snapshot.getDesign().getFactors().getItems() ) ) {
                if ( fc.getGemmaId() != null && liveFactorIds.contains( fc.getGemmaId() ) ) {
                    snapshotFactorIds.add( fc.getGemmaId() );
                } else {
                    fc.setGemmaId( null );
                    fc.setClientRef( "restore-f-" + ( seq++ ) );
                }
                for ( FactorValueCommit fvc : nullSafe( fc.getFactorValues().getItems() ) ) {
                    // a factor value cannot keep its id under a factor that is being recreated
                    if ( fc.getGemmaId() != null && fvc.getGemmaId() != null && liveFvIds.contains( fvc.getGemmaId() ) ) {
                        snapshotFvIds.add( fvc.getGemmaId() );
                    } else {
                        fvc.setGemmaId( null );
                        fvc.setClientRef( "restore-fv-" + ( seq++ ) );
                    }
                    for ( StatementCommit sc : nullSafe( fvc.getStatements().getItems() ) ) {
                        if ( fvc.getGemmaId() != null && sc.getGemmaId() != null && liveStatementIds.contains( sc.getGemmaId() ) ) {
                            snapshotStatementIds.add( sc.getGemmaId() );
                        } else {
                            sc.setGemmaId( null );
                            sc.setClientRef( "restore-s-" + ( seq++ ) );
                        }
                    }
                    // statements present now but not in the snapshot were added since: drop them
                    if ( fvc.getGemmaId() != null ) {
                        for ( Long liveId : statementIdsOf( current, fvc.getGemmaId() ) ) {
                            if ( !snapshotStatementIds.contains( liveId ) ) {
                                fvc.getStatements().getDeletedIds().add( liveId );
                            }
                        }
                    }
                }
                // factor values present now but not in the snapshot were added since
                if ( fc.getGemmaId() != null ) {
                    for ( Long liveId : factorValueIdsOf( current, fc.getGemmaId() ) ) {
                        if ( !snapshotFvIds.contains( liveId ) ) {
                            fc.getFactorValues().getDeletedIds().add( liveId );
                        }
                    }
                }
            }
            for ( Long liveId : liveFactorIds ) {
                if ( !snapshotFactorIds.contains( liveId ) ) {
                    snapshot.getDesign().getFactors().getDeletedIds().add( liveId );
                }
            }
        }

        reconcileIdSection( snapshot.getTags(), currentTagIds( ee ), "restore-t-" );
        reconcileIdSection( snapshot.getSampleCharacteristics(), currentSampleCharacteristicIds( ee ), "restore-sc-" );
    }

    /**
     * Shared id reconciliation for the two flat, id-addressed sections (tags and sample characteristics):
     * a snapshot id that no longer resolves is re-sent as a create, and a live id the snapshot never mentioned
     * is added to {@code deletedIds}.
     */
    private static <T extends EntityRef> void reconcileIdSection( @Nullable Section<T> section, Set<Long> liveIds,
            String clientRefPrefix ) {
        if ( section == null ) {
            return;
        }
        int seq = 0;
        Set<Long> kept = new HashSet<>();
        for ( T item : nullSafe( section.getItems() ) ) {
            if ( item.getGemmaId() != null && liveIds.contains( item.getGemmaId() ) ) {
                kept.add( item.getGemmaId() );
            } else {
                item.setGemmaId( null );
                item.setClientRef( clientRefPrefix + ( seq++ ) );
            }
        }
        for ( Long liveId : liveIds ) {
            if ( !kept.contains( liveId ) ) {
                section.getDeletedIds().add( liveId );
            }
        }
    }

    /**
     * The experiment's OWN tags — the rows the commit's {@code tags} section writes.
     * <p>
     * {@code getAnnotations} deliberately aggregates three sources: experiment-level tags, experimental-design
     * tags, and sample-level tags. That is right for a reader and wrong for a snapshot: replaying the aggregate
     * through the {@code tags} section would re-create every design and sample annotation a second time as an
     * experiment-level tag. {@code objectClass} is what separates them.
     */
    private List<AnnotationValueObject> experimentLevelTags( ExpressionExperiment ee ) {
        return expressionExperimentService.getAnnotations( ee, true ).stream()
                .filter( a -> "ExperimentTag".equals( a.getObjectClass() ) )
                .collect( Collectors.toList() );
    }

    private Set<Long> currentTagIds( ExpressionExperiment ee ) {
        return experimentLevelTags( ee ).stream()
                .map( AnnotationValueObject::getId )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
    }

    private Set<Long> currentSampleCharacteristicIds( ExpressionExperiment ee ) {
        Set<Long> ids = new HashSet<>();
        for ( BioAssay ba : expressionExperimentService.thawBioAssays( ee ).getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm == null ) continue;
            for ( AnnotationValueObject a : sampleAnnotationVos( bm ) ) {
                if ( a.getId() != null ) {
                    ids.add( a.getId() );
                }
            }
        }
        return ids;
    }

    private static List<Long> factorValueIdsOf( @Nullable ExperimentalDesignValueObject design, Long factorId ) {
        if ( design == null ) return Collections.emptyList();
        return nullSafe( design.getExperimentalFactors() ).stream()
                .filter( f -> factorId.equals( f.getId() ) )
                .flatMap( f -> nullSafe( f.getValues() ).stream() )
                .map( FactorValueBasicValueObject::getId )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
    }

    private static List<Long> statementIdsOf( @Nullable ExperimentalDesignValueObject design, Long factorValueId ) {
        if ( design == null ) return Collections.emptyList();
        return nullSafe( design.getExperimentalFactors() ).stream()
                .flatMap( f -> nullSafe( f.getValues() ).stream() )
                .filter( v -> factorValueId.equals( v.getId() ) )
                .flatMap( v -> nullSafe( v.getStatements() ).stream() )
                .map( StatementValueObject::getId )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
    }

    @Nullable
    private static OntologyTermRef termRef( @Nullable CharacteristicValueObject c ) {
        return c == null ? null : termRef( c.getCategory(), c.getCategoryUri() );
    }

    @Nullable
    private static OntologyTermRef termRef( @Nullable String label, @Nullable String uri ) {
        if ( label == null && uri == null ) {
            return null;
        }
        OntologyTermRef ref = new OntologyTermRef();
        ref.setLabel( label );
        ref.setUri( uri );
        return ref;
    }

    @Nullable
    private static Measurement snapshotMeasurement( @Nullable MeasurementValueObject m ) {
        if ( m == null || m.getValue() == null ) {
            return null;
        }
        Measurement out = new Measurement();
        out.setValue( m.getValue() );
        out.setUnit( m.getUnit() );
        out.setType( m.getType() );
        out.setRepresentation( m.getRepresentation() );
        return out;
    }

    private Map<String, Long> buildGsmToBioMaterialIdIndex( ExpressionExperiment ee ) {
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( ee );
        Map<String, Long> index = new HashMap<>();
        for ( BioAssay ba : thawed.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            if ( bm == null || bm.getId() == null ) {
                continue;
            }
            if ( ba.getAccession() != null && ba.getAccession().getAccession() != null ) {
                index.putIfAbsent( ba.getAccession().getAccession(), bm.getId() );
            }
            if ( ba.getShortName() != null ) {
                index.putIfAbsent( ba.getShortName(), bm.getId() );
            }
        }
        return index;
    }

    @Nullable
    private static CharacteristicValueObject ontologyToCharacteristic( @Nullable OntologyTermRef t ) {
        if ( t == null || StringUtils.isBlank( t.getLabel() ) ) {
            return null;
        }
        CharacteristicValueObject c = new CharacteristicValueObject();
        c.setCategory( t.getLabel() );
        c.setCategoryUri( t.getUri() );
        c.setValue( t.getLabel() );
        c.setValueUri( t.getUri() );
        return c;
    }

    @Nullable
    private static MeasurementValueObject mapMeasurement( @Nullable Measurement m ) {
        if ( m == null || StringUtils.isBlank( m.getValue() ) ) {
            return null;
        }
        MeasurementValueObject mo = new MeasurementValueObject();
        mo.setValue( m.getValue() );
        mo.setUnit( m.getUnit() );
        mo.setType( m.getType() );
        mo.setRepresentation( m.getRepresentation() );
        return mo;
    }

    /**
     * Build a {@link Characteristic} for a new experiment-level tag. A statement-shaped tag (one riding on
     * {@code statements}) becomes a single {@link Statement}; otherwise a plain category/value characteristic.
     */
    private static Characteristic tagCommitToCharacteristic( TagCommit tc ) {
        List<StatementCommit> statements = tc.getStatements() != null ? nullSafe( tc.getStatements().getItems() ) : Collections.emptyList();
        if ( !statements.isEmpty() ) {
            StatementCommit sc = statements.get( 0 );
            if ( sc.getSubject() == null || StringUtils.isBlank( sc.getSubject().getLabel() ) ) {
                throw new BadRequestException( "A statement tag needs a 'subject'." );
            }
            Statement s = Statement.Factory.newInstance();
            OntologyTermRef cat = sc.getCategory() != null ? sc.getCategory() : tc.getCategory();
            if ( cat != null ) {
                s.setCategory( cat.getLabel() );
                s.setCategoryUri( cat.getUri() );
            }
            s.setSubject( sc.getSubject().getLabel() );
            s.setSubjectUri( sc.getSubject().getUri() );
            if ( sc.getPredicate() != null ) {
                s.setPredicate( sc.getPredicate().getLabel() );
                s.setPredicateUri( sc.getPredicate().getUri() );
            }
            if ( sc.getObject() != null ) {
                s.setObject( sc.getObject().getLabel() );
                s.setObjectUri( sc.getObject().getUri() );
            }
            // Evidence on the statement wins; the tag-level field is the fallback for a plain tag.
            s.setSupportingEvidence( CharacteristicUtils.serializeSupportingEvidence(
                    sc.getSupportingEvidence() != null ? sc.getSupportingEvidence() : tc.getSupportingEvidence() ) );
            return s;
        }
        if ( tc.getValue() == null || StringUtils.isBlank( tc.getValue().getLabel() ) ) {
            throw new BadRequestException( "Each new tag needs a 'value' (or a 'statements' entry)." );
        }
        Characteristic c = Characteristic.Factory.newInstance();
        if ( tc.getCategory() != null ) {
            c.setCategory( tc.getCategory().getLabel() );
            c.setCategoryUri( tc.getCategory().getUri() );
        }
        c.setValue( tc.getValue().getLabel() );
        c.setValueUri( tc.getValue().getUri() );
        c.setSupportingEvidence( CharacteristicUtils.serializeSupportingEvidence( tc.getSupportingEvidence() ) );
        return c;
    }

    /**
     * Validate a newly-built characteristic's ontology terms and append any grounding failures to the sink,
     * prefixing each with the item's request-body {@code location} (e.g. {@code tags[clientRef=t7]}). An
     * unverified term (OLS unreachable) is dropped rather than blocking when fail-open is configured.
     */
    private void collectTermViolations( Characteristic c, String location, @Nullable String clientRef,
            List<OntologyTermValidationException.Located> sink, @Nullable List<Canonicalization> canonSink ) {
        List<TermCanonicalization> canons = new ArrayList<>();
        for ( TermViolation v : ontologyTermValidator.validateAndCanonicalize( c, canons ) ) {
            if ( v.getReason() == TermViolation.Reason.UNVERIFIED_OLS_UNAVAILABLE && !ontologyValidationOlsFailClosed ) {
                log.warn( "Allowing unverified term at " + location + "." + v.getSlot() + " (OLS unavailable, fail-open)." );
                continue;
            }
            sink.add( new OntologyTermValidationException.Located( location + "." + v.getSlot(), v ) );
        }
        // Echo the accepted near-match / blank-fill rewrites back so the client can update its display. Only the
        // callers whose Characteristic is carried into the commit request pass a canonSink; the design gate
        // validates a throwaway Statement (rejection-only — the rewrite is never persisted) and passes null.
        if ( canonSink != null ) {
            for ( TermCanonicalization tc : canons ) {
                canonSink.add( new Canonicalization( location + "." + tc.getSlot(), clientRef,
                        tc.getSubmittedLabel(), tc.getCanonicalLabel(), tc.getSubmittedUri(), tc.getCanonicalUri() ) );
            }
        }
    }

    /** A stable location fragment for an item: its clientRef when present, else its zero-based index. */
    private static String refOrIndex( @Nullable String clientRef, int index ) {
        return StringUtils.isNotBlank( clientRef ) ? "clientRef=" + clientRef : String.valueOf( index );
    }

    /**
     * Ground-check the ontology terms carried by a design commit: each factor's category and each asserted
     * factor-value statement's subject/predicate/object/category. Throwaway entities are built purely to reuse
     * {@link #collectTermViolations}; only items present in the commit are walked (carry-forward statements
     * re-emitted from the current design are not).
     */
    private void collectDesignTermViolations( DesignCommit dc, List<OntologyTermValidationException.Located> sink ) {
        if ( dc.getFactors() == null ) {
            return;
        }
        int fi = 0;
        for ( FactorCommit fc : nullSafe( dc.getFactors().getItems() ) ) {
            String floc = "design.factors[" + refOrIndex( fc.getClientRef(), fi ) + "]";
            if ( fc.getCategory() != null && StringUtils.isNotBlank( fc.getCategory().getUri() ) ) {
                Characteristic cat = Characteristic.Factory.newInstance();
                cat.setCategory( fc.getCategory().getLabel() );
                cat.setCategoryUri( fc.getCategory().getUri() );
                collectTermViolations( cat, floc, fc.getClientRef(), sink, null );
            }
            int vi = 0;
            for ( FactorValueCommit fvc : nullSafe( fc.getFactorValues() != null ? fc.getFactorValues().getItems() : null ) ) {
                String vloc = floc + ".factorValues[" + refOrIndex( fvc.getClientRef(), vi ) + "]";
                int si = 0;
                for ( StatementCommit sc : nullSafe( fvc.getStatements() != null ? fvc.getStatements().getItems() : null ) ) {
                    Statement s = Statement.Factory.newInstance();
                    if ( sc.getCategory() != null ) {
                        s.setCategory( sc.getCategory().getLabel() );
                        s.setCategoryUri( sc.getCategory().getUri() );
                    }
                    if ( sc.getSubject() != null ) {
                        s.setSubject( sc.getSubject().getLabel() );
                        s.setSubjectUri( sc.getSubject().getUri() );
                    }
                    if ( sc.getPredicate() != null ) {
                        s.setPredicate( sc.getPredicate().getLabel() );
                        s.setPredicateUri( sc.getPredicate().getUri() );
                    }
                    if ( sc.getObject() != null ) {
                        s.setObject( sc.getObject().getLabel() );
                        s.setObjectUri( sc.getObject().getUri() );
                    }
                    collectTermViolations( s, vloc + ".statements[" + refOrIndex( sc.getClientRef(), si ) + "]", sc.getClientRef(), sink, null );
                    si++;
                }
                vi++;
            }
            fi++;
        }
    }

    /** Build a plain category/value {@link Characteristic} for a new per-sample characteristic. */
    private static Characteristic sampleCharacteristicToCharacteristic( SampleCharacteristicCommit sc ) {
        if ( sc.getValue() == null || StringUtils.isBlank( sc.getValue().getLabel() ) ) {
            throw new BadRequestException( "Each new sampleCharacteristics item needs a 'value'." );
        }
        Characteristic c = Characteristic.Factory.newInstance();
        if ( sc.getCategory() != null ) {
            c.setCategory( sc.getCategory().getLabel() );
            c.setCategoryUri( sc.getCategory().getUri() );
        }
        c.setValue( sc.getValue().getLabel() );
        c.setValueUri( sc.getValue().getUri() );
        c.setSupportingEvidence( CharacteristicUtils.serializeSupportingEvidence( sc.getSupportingEvidence() ) );
        return c;
    }

    private static List<ExperimentalDesignValueObject.BioMaterialFactorValueAssignment> buildAssignmentList(
            Map<Long, Set<Long>> bmToFvIds, Map<Long, String> bmNames ) {
        List<ExperimentalDesignValueObject.BioMaterialFactorValueAssignment> out = new ArrayList<>();
        for ( Map.Entry<Long, Set<Long>> e : bmToFvIds.entrySet() ) {
            List<Long> fvIds = new ArrayList<>( e.getValue() );
            Collections.sort( fvIds );
            out.add( new ExperimentalDesignValueObject.BioMaterialFactorValueAssignment( e.getKey(), bmNames.get( e.getKey() ), fvIds ) );
        }
        return out;
    }

    private static String summarizeDesignBlockers( DesignPreflightReport report ) {
        return report.getBlockers().stream()
                .map( b -> b.getType() + ( b.getMessage() != null ? " (" + b.getMessage() + ")" : "" ) )
                .collect( Collectors.joining( "; " ) );
    }

    /**
     * Human-readable summary of the consequences a caller is being asked to consent to, for the 409 body. Names
     * each cause separately rather than collapsing them into a count, because "would delete 2 analyses" and
     * "would strand 1 subset" call for different curator judgment — and the stranded subset is the one that
     * survives the change still looking valid.
     */
    private static String summarizeDesignConsequences( DesignPreflightReport report ) {
        List<String> parts = new ArrayList<>();
        int analyses = report.getDifferentialExpressionAnalysesToDelete().size();
        if ( analyses > 0 ) {
            parts.add( "delete " + analyses + " differential-expression analysis/analyses" );
        }
        int subsets = report.getSubsetsWithStaleAnchor().size();
        if ( subsets > 0 ) {
            parts.add( "leave " + subsets + " subset(s) anchored on factor values that would no longer exist ("
                    + report.getSubsetsWithStaleAnchor().stream()
                    .map( s -> s.getName() != null ? s.getName() : String.valueOf( s.getId() ) )
                    .collect( Collectors.joining( ", " ) ) + ")" );
        }
        return "This design change would " + String.join( " and ", parts );
    }

    private static <X> List<X> nullSafe( @Nullable List<X> l ) {
        return l != null ? l : Collections.emptyList();
    }

    /** The server's reply — mirrors CAB's {@code CurationCommitReport}. */
    public static class CurationCommitReport {
        private final boolean applied;
        private final Map<String, Long> idMap;
        private final Map<String, CurationSectionChange> changes;
        private final List<Long> auditEventIds;
        private final List<Canonicalization> canonicalizations;
        /**
         * The {@code COMMIT} AnnotationSet minted for this commit's run, or null when no run was named.
         * Null on a preflight too: a dry run writes nothing, so there is no row to point at.
         */
        @Nullable
        private final Long commitAnnotationSetId;
        private final String error;

        private CurationCommitReport( boolean applied, Map<String, CurationSectionChange> changes,
                Map<String, Long> idMap, List<Long> auditEventIds, List<Canonicalization> canonicalizations,
                @Nullable Long commitAnnotationSetId ) {
            this.applied = applied;
            this.idMap = idMap;
            this.changes = changes;
            this.auditEventIds = auditEventIds;
            this.canonicalizations = canonicalizations;
            this.commitAnnotationSetId = commitAnnotationSetId;
            this.error = "";
        }

        static CurationCommitReport from( CurationCommitResult r, CurationCommitRequest req, boolean applied,
                List<Canonicalization> canonicalizations ) {
            Map<String, CurationSectionChange> changes = new LinkedHashMap<>();
            if ( req.isBasicsPresent() ) {
                changes.put( "basics", r.isBasicsChanged()
                        ? new CurationSectionChange( 0, 1, 0, 0 )
                        : new CurationSectionChange( 0, 0, 0, 1 ) );
            }
            if ( req.isPublicationsPresent() ) {
                changes.put( "publications", new CurationSectionChange(
                        r.getPublicationsCreated(), 0, r.getPublicationsDeleted(), r.getPublicationsUnchanged() ) );
            }
            if ( req.isDesignPresent() ) {
                changes.put( "design", new CurationSectionChange(
                        r.getDesignCreated(), r.getDesignUpdated(), r.getDesignDeleted(), r.getDesignUnchanged() ) );
            }
            if ( req.isTagsPresent() ) {
                changes.put( "tags", new CurationSectionChange(
                        r.getTagsCreated(), 0, r.getTagsDeleted(), r.getTagsUnchanged() ) );
            }
            if ( req.isSampleCharsPresent() ) {
                changes.put( "sampleCharacteristics", new CurationSectionChange(
                        r.getSampleCharsCreated(), 0, r.getSampleCharsDeleted(), r.getSampleCharsUnchanged() ) );
            }
            if ( req.isCurationDetailsPresent() ) {
                changes.put( "curationDetails", r.isCurationNoteChanged()
                        ? new CurationSectionChange( 0, 1, 0, 0 )
                        : new CurationSectionChange( 0, 0, 0, 1 ) );
            }
            // Merge every section's clientRef → newId map. auditEventIds carries the design events (advisory).
            Map<String, Long> idMap = new LinkedHashMap<>();
            if ( r.getDesignIdMap() != null ) idMap.putAll( r.getDesignIdMap() );
            if ( r.getTagsIdMap() != null ) idMap.putAll( r.getTagsIdMap() );
            if ( r.getSampleCharsIdMap() != null ) idMap.putAll( r.getSampleCharsIdMap() );
            List<Long> auditEventIds = r.getDesignAuditEventIds() != null ? r.getDesignAuditEventIds() : Collections.emptyList();
            return new CurationCommitReport( applied, changes, idMap, auditEventIds, canonicalizations,
                    r.getCommitAnnotationSetId() );
        }

        public boolean isApplied() { return applied; }
        @Nullable
        public Long getCommitAnnotationSetId() { return commitAnnotationSetId; }
        public Map<String, Long> getIdMap() { return idMap; }
        public Map<String, CurationSectionChange> getChanges() { return changes; }
        public List<Long> getAuditEventIds() { return auditEventIds; }
        public List<Canonicalization> getCanonicalizations() { return canonicalizations; }
        public String getError() { return error; }
    }

    public static class CurationSectionChange {
        private final int created;
        private final int updated;
        private final int deleted;
        private final int unchanged;

        CurationSectionChange( int created, int updated, int deleted, int unchanged ) {
            this.created = created;
            this.updated = updated;
            this.deleted = deleted;
            this.unchanged = unchanged;
        }

        public int getCreated() { return created; }
        public int getUpdated() { return updated; }
        public int getDeleted() { return deleted; }
        public int getUnchanged() { return unchanged; }
    }

    /**
     * One accepted rewrite the grounding gate applied to a persisted annotation — a case/whitespace-only
     * near-match canonicalized to the term's label, a blank label filled in from its URI, and/or a known
     * Gemma-ontology term (e.g. {@code TGEMO_*}) whose URI was normalized onto the canonical Gemma base. Not a
     * rejection — the slot passed — but the stored value differs from what was submitted, so the client can
     * silently update the chip to {@code canonicalLabel} (and knows its {@code submittedUri} was off-base — a
     * signal for tracking down which writer emitted the wrong base). A field pair being equal means that
     * dimension was unchanged. Only tags + sampleCharacteristics are reported (the design-section gate is
     * rejection-only and never persists its rewrite).
     */
    public static class Canonicalization {
        private final String location;
        @Nullable
        private final String clientRef;
        @Nullable
        private final String submittedLabel;
        private final String canonicalLabel;
        private final String submittedUri;
        private final String canonicalUri;

        Canonicalization( String location, @Nullable String clientRef, @Nullable String submittedLabel, String canonicalLabel, String submittedUri, String canonicalUri ) {
            this.location = location;
            this.clientRef = clientRef;
            this.submittedLabel = submittedLabel;
            this.canonicalLabel = canonicalLabel;
            this.submittedUri = submittedUri;
            this.canonicalUri = canonicalUri;
        }

        /** Request-body path to the rewritten slot, e.g. {@code tags[clientRef=t7].value}. */
        public String getLocation() { return location; }
        /** The item's {@code clientRef}, so the client can map the rewrite back to its chip without parsing. */
        @Nullable
        public String getClientRef() { return clientRef; }
        /** The label as submitted; {@code null} when a URI arrived with no label and one was filled in. */
        @Nullable
        public String getSubmittedLabel() { return submittedLabel; }
        /** The canonical label now stored — display this (equals {@code submittedLabel} when only the URI changed). */
        public String getCanonicalLabel() { return canonicalLabel; }
        /** The URI as submitted (off-base when it differs from {@code canonicalUri}). */
        public String getSubmittedUri() { return submittedUri; }
        /** The URI now stored (equals {@code submittedUri} when only the label changed). */
        public String getCanonicalUri() { return canonicalUri; }
    }

    /**
     * Ticket-layer back-end for the legacy {@code troubled} / {@code needsAttention} flips.
     * Opens a ticket of {@code openType} (when {@code on=true} and no matching open ticket exists),
     * or transitions every open ticket whose type is in {@code resolveTypes} to
     * {@link TicketState#RESOLVED} (when {@code on=false}).
     *
     * @param note         optional human note supplied on the legacy request; used as ticket title
     *                     on open and as transition reason on resolve.
     * @param defaultTitle fallback ticket title when {@code note} is blank.
     */
    private void applyFlagViaTickets( ExpressionExperiment ee, boolean on, TicketType openType,
            Set<TicketType> resolveTypes, @Nullable String note, String defaultTitle ) {
        User actor = userManager.getCurrentUser();
        if ( actor == null ) {
            // @PreAuthorize on the endpoint already gates anonymous callers — this is a defensive guard
            // for the edge case where SecurityContext returns a non-User principal.
            throw new BadRequestException( "No authenticated user resolved." );
        }
        if ( on ) {
            // No-op if a matching open ticket already exists (prevents duplicate opens on idempotent
            // re-flips). The shim's needsAttention/troubled read should already be false here, but the
            // explicit guard keeps behaviour sane if the ticket layer is mid-state.
            List<Ticket> existing = ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() );
            for ( Ticket t : existing ) {
                if ( t.getType() == openType ) {
                    return;
                }
            }
            String title = ( note != null && !note.trim().isEmpty() ) ? note.trim() : defaultTitle;
            ticketService.openTicket( actor, openType, title,
                    Collections.singleton( TicketTarget.Factory.newInstance(
                            TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) ) );
        } else {
            for ( Ticket t : ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) ) {
                if ( resolveTypes.contains( t.getType() ) && t.getState() != TicketState.RESOLVED
                        && t.getState() != TicketState.CANCELLED ) {
                    ticketService.transition( t, TicketState.RESOLVED, actor, note );
                }
            }
        }
    }

    /**
     * Request body for {@link #updateDatasetPermissions}. Each field is optional; only provided fields are updated.
     */
    public static class PermissionsUpdateRequest {
        @Nullable
        private Boolean isPublic;

        @Nullable
        public Boolean getIsPublic() {
            return isPublic;
        }

        public void setIsPublic( @Nullable Boolean isPublic ) {
            this.isPublic = isPublic;
        }
    }

    /**
     * Lightweight view of a dataset's sharing state, returned by the permissions endpoint.
     */
    public static class DatasetPermissionsValueObject {
        private final boolean isPublic;
        private final boolean isShared;

        public DatasetPermissionsValueObject( boolean isPublic, boolean isShared ) {
            this.isPublic = isPublic;
            this.isShared = isShared;
        }

        public boolean getIsPublic() {
            return isPublic;
        }

        public boolean getIsShared() {
            return isShared;
        }
    }

    @PUT
    @Path("/{dataset}/permissions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Update the sharing permissions of a dataset",
            description = "Toggle whether a dataset is publicly readable. The `isPublic` field is optional; if omitted, "
                    + "no change is made and the current state is returned.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<DatasetPermissionsValueObject> updateDatasetPermissions(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable PermissionsUpdateRequest body
    ) {
        if ( body == null ) {
            throw new BadRequestException( "A request body is required." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( body.getIsPublic() != null ) {
            if ( body.getIsPublic() ) {
                if ( !securityService.isPublic( ee ) ) {
                    securityService.makePublic( ee );
                    auditTrailService.addUpdateEvent( ee,
                            ubic.gemma.model.common.auditAndSecurity.eventType.MakePublicEvent.class,
                            "Made public via REST (PUT permissions)" );
                }
            } else {
                if ( securityService.isPublic( ee ) ) {
                    securityService.makePrivate( ee );
                    auditTrailService.addUpdateEvent( ee,
                            ubic.gemma.model.common.auditAndSecurity.eventType.MakePrivateEvent.class,
                            "Made private via REST (PUT permissions)" );
                }
            }
        }
        return respond( new DatasetPermissionsValueObject( securityService.isPublic( ee ), securityService.isShared( ee ) ) );
    }

    /**
     * Retrieve the current sharing state of a dataset.
     * <p>
     * Curation-UI helper: the experiment-page sharing widget needs to read the current public/shared state
     * without performing an update. Same response shape as {@link #updateDatasetPermissions}.
     */
    @GET
    @Path("/{dataset}/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retrieve the sharing permissions of a dataset",
            description = "Returns whether the dataset is publicly readable and whether it has been shared with any user groups.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<DatasetPermissionsValueObject> getDatasetPermissions(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        return respond( new DatasetPermissionsValueObject( securityService.isPublic( ee ), securityService.isShared( ee ) ) );
    }

    /**
     * Curation-UI workflow-step endpoint: raw ACL flip to make a dataset publicly readable. Distinct from
     * {@code POST /publish} (which is a curator-state-machine transition that ALSO calls this under the hood).
     * Idempotent — re-running on an already-public dataset is a no-op.
     */
    @POST
    @Path("/{dataset}/makePublic")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Make a dataset publicly readable",
            description = "Performs the raw ACL flip to grant `IS_AUTHENTICATED_ANONYMOUSLY` read on the dataset. "
                    + "Idempotent. See `POST /datasets/{id}/publish` for the curator-workflow transition that also "
                    + "captures a reviewer.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<DatasetPermissionsValueObject> makeDatasetPublic(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( !securityService.isPublic( ee ) ) {
            securityService.makePublic( ee );
            auditTrailService.addUpdateEvent( ee,
                    ubic.gemma.model.common.auditAndSecurity.eventType.MakePublicEvent.class,
                    "Made public via REST (POST makePublic)" );
        }
        return respond( new DatasetPermissionsValueObject( securityService.isPublic( ee ), securityService.isShared( ee ) ) );
    }

    /**
     * Curation-UI workflow-step endpoint: raw ACL flip to make a dataset private. Idempotent.
     */
    @POST
    @Path("/{dataset}/makePrivate")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Make a dataset private",
            description = "Removes the `IS_AUTHENTICATED_ANONYMOUSLY` read ACE from the dataset. Idempotent.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<DatasetPermissionsValueObject> makeDatasetPrivate(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( securityService.isPublic( ee ) ) {
            securityService.makePrivate( ee );
            auditTrailService.addUpdateEvent( ee,
                    ubic.gemma.model.common.auditAndSecurity.eventType.MakePrivateEvent.class,
                    "Made private via REST (POST makePrivate)" );
        }
        return respond( new DatasetPermissionsValueObject( securityService.isPublic( ee ), securityService.isShared( ee ) ) );
    }

    /**
     * Curation-UI workflow-step endpoint: curator-state-machine transition that publishes a dataset under a named
     * reviewer. Distinct from {@code POST /makePublic}: this endpoint ALSO records the reviewer as a
     * {@link ubic.gemma.model.common.auditAndSecurity.eventType.DatasetPublishedEvent} audit event (with the
     * reviewer encoded in the note), and is idempotent on already-published datasets (audit-only emission then).
     */
    @POST
    @Path("/{dataset}/publish")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Publish a dataset (curator-workflow transition; records reviewer)",
            description = "Curator-workflow endpoint distinct from `/makePublic`. Records the reviewer as an audit "
                    + "event and (if the dataset is not already public) performs the ACL flip. The `reviewer` query "
                    + "parameter is required.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The `reviewer` query parameter is missing or blank.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<DatasetPermissionsValueObject> publishDataset(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("reviewer") @Nullable String reviewer
    ) {
        if ( reviewer == null || reviewer.trim().isEmpty() ) {
            throw new BadRequestException( "The `reviewer` query parameter is required." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        boolean alreadyPublic = securityService.isPublic( ee );
        if ( !alreadyPublic ) {
            securityService.makePublic( ee );
        }
        String note = alreadyPublic
                ? "Re-published by reviewer: " + reviewer.trim() + " (dataset was already public)"
                : "Published by reviewer: " + reviewer.trim();
        auditTrailService.addUpdateEvent( ee,
                ubic.gemma.model.common.auditAndSecurity.eventType.DatasetPublishedEvent.class, note );
        return respond( new DatasetPermissionsValueObject( securityService.isPublic( ee ), securityService.isShared( ee ) ) );
    }

    /**
     * Curation-UI compatibility alias for {@link #getDatasetPermissions}. The UI's dataset-page sharing widget
     * calls {@code GET /datasets/{id}/visibility}; the canonical gemma-rest endpoint lives at
     * {@code /datasets/{id}/permissions}. Hidden from the OpenAPI spec to avoid duplicating the canonical entry.
     */
    @GET
    @Path("/{dataset}/visibility")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retrieve the sharing permissions of a dataset (alias of /permissions)", hidden = true,
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) })
    public ResponseDataObject<DatasetPermissionsValueObject> getDatasetVisibility(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return getDatasetPermissions( datasetArg );
    }

    /**
     * Curation-UI compatibility alias for {@link #getDatasetPipelineStatus}: the UI uses the flatter, hyphenated
     * path {@code /datasets/{id}/pipeline-status}; the canonical handler lives at {@code /pipelineStatus}.
     */
    @GET
    @Path("/{dataset}/pipeline-status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the per-step pipeline status of a dataset (alias of /pipelineStatus)", hidden = true)
    public ResponseDataObject<PipelineStatusValueObject> getDatasetPipelineStatusAlias(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return getDatasetPipelineStatus( datasetArg );
    }

    /**
     * Step descriptor backing {@link #getDatasetPipelineStatus}: the JSON {@code step} key plus the success-event
     * and (optional) failed-event classes whose latest occurrences determine the step's state.
     */
    private static final class PipelineStepDescriptor {
        final String stepKey;
        final Class<? extends AuditEventType> successType;
        @Nullable
        final Class<? extends AuditEventType> failedType;

        PipelineStepDescriptor( String stepKey, Class<? extends AuditEventType> successType,
                @Nullable Class<? extends AuditEventType> failedType ) {
            this.stepKey = stepKey;
            this.successType = successType;
            this.failedType = failedType;
        }
    }

    // BatchInformationEvent (the abstract parent) covers Fetching/FailedFetching/Missing in one query, so no
    // separate failed class is needed for batchInfo.
    private static final List<PipelineStepDescriptor> PIPELINE_STEPS = Arrays.asList(
            new PipelineStepDescriptor( "batchInfo", BatchInformationEvent.class, null ),
            new PipelineStepDescriptor( "preprocess", ProcessedVectorComputationEvent.class, FailedProcessedVectorComputationEvent.class ),
            new PipelineStepDescriptor( "batchCorrection", BatchCorrectionEvent.class, null ),
            new PipelineStepDescriptor( "pca", PCAAnalysisEvent.class, FailedPCAAnalysisEvent.class ),
            new PipelineStepDescriptor( "sampleCorrelation", SampleCorrelationAnalysisEvent.class, FailedSampleCorrelationAnalysisEvent.class ),
            new PipelineStepDescriptor( "meanVariance", MeanVarianceUpdateEvent.class, FailedMeanVarianceUpdateEvent.class ),
            new PipelineStepDescriptor( "dea", DifferentialExpressionAnalysisEvent.class, FailedDifferentialExpressionAnalysisEvent.class ),
            new PipelineStepDescriptor( "coexpression", LinkAnalysisEvent.class, FailedLinkAnalysisEvent.class ),
            new PipelineStepDescriptor( "missingValue", MissingValueAnalysisEvent.class, FailedMissingValueAnalysisEvent.class )
    );

    @GET
    @Path("/{dataset}/pipelineStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the per-step pipeline status of a dataset",
            description = "Returns a snapshot of each preprocessing/analysis step (`batchInfo`, `preprocess`, `pca`, "
                    + "`dea`, `coexpression`, `missingValue`) with its last-run date, audit-event class name, and "
                    + "state (`ok`, `failed`, `notRun`, or `notApplicable`). The `curationNote` field is admin-only.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<PipelineStatusValueObject> getDatasetPipelineStatus(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        CurationDetails cd = ee.getCurationDetails();

        // Skip ExpressionExperimentReportService.generateSummary(id) entirely. That call
        // is the heavyweight ~30-join SELECT on INVESTIGATION/DATABASE_ENTRY/EXPERIMENTAL_DESIGN/
        // CURATION_DETAILS/AUDIT_EVENT*3/AUDIT_EVENT_TYPE*3/GEEQ + ACL subselects + a getStats
        // fan-out. It dominated the cold cost of /pipelineStatus (~10-14s against prod gemd
        // through a tunnel). pipelineStatus only needs five fields out of the resulting VO;
        // each can be computed from cheap, targeted queries.

        // hasDifferentialExpressionAnalysis: one fast WHERE id IN (?) lookup on
        // ANALYSIS.EXPERIMENT_FK rather than a separate cached-set materialization.
        boolean hasDea = !differentialExpressionAnalysisService
                .getExperimentsWithAnalysis( Collections.singleton( ee.getId() ), true ).isEmpty();

        // hasCoexpressionAnalysis: the coexpression subsystem was removed in Phase 1c. The
        // VO field was kept for API compatibility but is always false (see
        // ExpressionExperimentDaoImpl.populateAnalysisInformation javadoc).
        boolean hasCoex = false;

        // Batch-fetch every audit-event type the pipeline-step loop + GEEQ block need in a
        // single round-trip. Replaces ~13 individual getLastEvent(ee, type) calls — each of
        // which is its own DB round-trip — with one getLastEvents call returning a nested map.
        Set<Class<? extends AuditEventType>> auditTypes = new LinkedHashSet<>();
        for ( PipelineStepDescriptor desc : PIPELINE_STEPS ) {
            auditTypes.add( desc.successType );
            if ( desc.failedType != null ) {
                auditTypes.add( desc.failedType );
            }
        }
        auditTypes.add( GeeqEvent.class );
        Map<Class<? extends AuditEventType>, Map<ExpressionExperiment, AuditEvent>> auditEventsByType =
                auditEventService.getLastEvents( Collections.singleton( ee ), auditTypes );

        boolean missingValueApplicable = hasTwoColorOrDualModePlatform( ee );
        List<PipelineStatusValueObject.PipelineStepValueObject> steps = new ArrayList<>( PIPELINE_STEPS.size() );
        for ( PipelineStepDescriptor desc : PIPELINE_STEPS ) {
            boolean applicable = !"missingValue".equals( desc.stepKey ) || missingValueApplicable;
            steps.add( buildPipelineStep( ee, desc, applicable, auditEventsByType ) );
        }

        PipelineStatusValueObject result = new PipelineStatusValueObject();
        result.setExperimentId( ee.getId() );
        result.setSteps( steps );
        result.setHasBatchInformation( expressionExperimentBatchInformationService.checkHasBatchInfo( ee ) );
        result.setHasDifferentialExpressionAnalysis( hasDea );
        result.setHasCoexpressionAnalysis( hasCoex );
        result.setTroubled( cd.getTroubled() );
        // troubleDetails was sourced from ExpressionExperimentDetailsValueObject.getTroubleDetails(),
        // which concatenated the EE's own trouble note with each troubled ArrayDesign's
        // trouble details. Reconstructing the full string would re-introduce the array-design
        // fetch cost. For pipelineStatus (a status-strip endpoint), empty string when not
        // troubled is the common case; when troubled, surface the EE's CurationDetails note
        // and leave per-AD enrichment to dedicated endpoints that already load the array
        // designs.
        result.setTroubleDetails( cd.getTroubled() && cd.getCurationNote() != null ? cd.getCurationNote() : "" );
        result.setNeedsAttention( cd.getNeedsAttention() );
        if ( SecurityUtil.isUserAdmin() ) {
            result.setCurationNote( cd.getCurationNote() );
        }
        result.setIsPublic( securityService.isPublic( ee ) );

        // Hydrate GEEQ via geeqService rather than touching ee.getGeeq() directly: the
        // GEEQ field on ExpressionExperiment is lazy, and the @Transactional that loaded
        // ee has already ended by the time this handler runs, so accessing it here would
        // throw LazyInitializationException ("no session"). The proxy still exposes its
        // ID without initialization, which we use to fetch the VO inside geeqService's
        // own transaction.
        Geeq geeqProxy = ee.getGeeq();
        GeeqValueObject geeq = geeqProxy != null && geeqProxy.getId() != null
                ? geeqService.loadValueObjectById( geeqProxy.getId() )
                : null;
        if ( geeq != null ) {
            AuditEvent geeqEvent = lookupAuditEvent( auditEventsByType, GeeqEvent.class, ee );
            if ( geeqEvent != null ) {
                geeq.setLastComputed( geeqEvent.getDate() );
            }
        }
        result.setGeeq( geeq );

        return respond( result );
    }

    private PipelineStatusValueObject.PipelineStepValueObject buildPipelineStep( ExpressionExperiment ee,
            PipelineStepDescriptor desc, boolean applicable,
            Map<Class<? extends AuditEventType>, Map<ExpressionExperiment, AuditEvent>> auditEventsByType ) {
        AuditEvent successEvent = lookupAuditEvent( auditEventsByType, desc.successType, ee );
        AuditEvent failedEvent = desc.failedType != null
                ? lookupAuditEvent( auditEventsByType, desc.failedType, ee ) : null;
        AuditEvent winner = pickLatestEvent( successEvent, failedEvent );
        if ( winner == null ) {
            return new PipelineStatusValueObject.PipelineStepValueObject( desc.stepKey,
                    applicable ? "notRun" : "notApplicable", null, null, null );
        }
        String eventTypeName = winner.getEventType() != null
                ? winner.getEventType().getClass().getSimpleName() : null;
        String state = eventTypeName != null && eventTypeName.startsWith( "Failed" ) ? "failed" : "ok";
        return new PipelineStatusValueObject.PipelineStepValueObject( desc.stepKey, state,
                winner.getDate(), eventTypeName, winner.getNote() );
    }

    @Nullable
    private static AuditEvent lookupAuditEvent(
            Map<Class<? extends AuditEventType>, Map<ExpressionExperiment, AuditEvent>> eventsByType,
            Class<? extends AuditEventType> type, ExpressionExperiment ee ) {
        Map<ExpressionExperiment, AuditEvent> forType = eventsByType.get( type );
        return forType != null ? forType.get( ee ) : null;
    }

    @Nullable
    private static AuditEvent pickLatestEvent( @Nullable AuditEvent a, @Nullable AuditEvent b ) {
        if ( a == null ) {
            return b;
        }
        if ( b == null || b.getDate() == null ) {
            return a;
        }
        if ( a.getDate() == null ) {
            return b;
        }
        return a.getDate().compareTo( b.getDate() ) >= 0 ? a : b;
    }

    private boolean hasTwoColorOrDualModePlatform( ExpressionExperiment ee ) {
        for ( ArrayDesign ad : expressionExperimentService.getArrayDesignsUsed( ee ) ) {
            TechnologyType t = ad.getTechnologyType();
            if ( t == TechnologyType.TWOCOLOR || t == TechnologyType.DUALMODE ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Request body for {@link #getDatasetPipelineStatusBulk}. Field is named on the wire as
     * {@code datasetIds} (snake_case) to match the curation-UI's workflow list view client
     * (see {@code apps/curation/src/api/workflow.ts::usePipelineStatusBulk}).
     */
    public static class PipelineStatusBulkRequest {
        @Nullable
        private List<Long> datasetIds;

        @Nullable
        @com.fasterxml.jackson.annotation.JsonProperty("datasetIds")
        public List<Long> getDatasetIds() {
            return datasetIds;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("datasetIds")
        public void setDatasetIds( @Nullable List<Long> datasetIds ) {
            this.datasetIds = datasetIds;
        }
    }

    /**
     * Maximum number of dataset IDs accepted in a single bulk pipeline-status request. The
     * workflow list view paginates at 50 rows; 500 leaves comfortable headroom while bounding
     * the worst-case per-EE fan-out (batch-info / array-designs / geeq).
     */
    private static final int MAX_PIPELINE_STATUS_BULK = 500;

    @POST
    @Path("/pipeline-status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Bulk per-step pipeline status for many datasets in one round-trip",
            description = "Returns a map of dataset ID → {@link PipelineStatusValueObject}, one entry per requested ID that the caller can read. "
                    + "Mirrors the single-EE `GET /{dataset}/pipelineStatus` handler in response shape, but batches the underlying audit-event lookup and "
                    + "DEA-existence query so that loading a workflow-list page of 20–50 experiments takes one DB round-trip per concern rather than 20–50. "
                    + "ACL behaviour: IDs the caller cannot read are silently dropped from the result map (no 403 for the batch). "
                    + "Hard cap: " + MAX_PIPELINE_STATUS_BULK + " IDs per request.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The request body is missing, empty, or exceeds the per-request ID cap.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<Map<Long, PipelineStatusValueObject>> getDatasetPipelineStatusBulk(
            @Nullable PipelineStatusBulkRequest body
    ) {
        if ( body == null || body.getDatasetIds() == null || body.getDatasetIds().isEmpty() ) {
            throw new BadRequestException( "A request body with non-empty 'datasetIds' is required." );
        }
        // Deduplicate but preserve caller-supplied order on the way out (for predictable client iteration).
        List<Long> requestedIds = new ArrayList<>( new LinkedHashSet<>( body.getDatasetIds() ) );
        if ( requestedIds.size() > MAX_PIPELINE_STATUS_BULK ) {
            throw new BadRequestException( "At most " + MAX_PIPELINE_STATUS_BULK + " dataset IDs may be requested per call (got " + requestedIds.size() + ")." );
        }

        // ACL-pre-filtered load: load(Filters, Sort) on EE service uses in-query ACL filtering
        // (see SecurableFilteringVoEnabledService), so the returned list contains only EEs the
        // caller can read. Missing IDs (truly absent, blacklisted, or invisible) are dropped
        // silently per the @Operation description.
        Filters filters = Filters.by( expressionExperimentService.getFilter( "id", Long.class, Filter.Operator.in, requestedIds ) );
        List<ExpressionExperiment> visibleEEs = expressionExperimentService.load( filters, null );
        if ( visibleEEs.isEmpty() ) {
            return respond( Collections.emptyMap() );
        }
        Set<Long> visibleIds = new LinkedHashSet<>( visibleEEs.size() );
        for ( ExpressionExperiment ee : visibleEEs ) {
            visibleIds.add( ee.getId() );
        }

        // ONE batched audit-event call covering every step + GEEQ across every visible EE.
        // Replaces O(steps × EEs) individual getLastEvent calls with one DB round-trip.
        Set<Class<? extends AuditEventType>> auditTypes = new LinkedHashSet<>();
        for ( PipelineStepDescriptor desc : PIPELINE_STEPS ) {
            auditTypes.add( desc.successType );
            if ( desc.failedType != null ) {
                auditTypes.add( desc.failedType );
            }
        }
        auditTypes.add( GeeqEvent.class );
        Map<Class<? extends AuditEventType>, Map<ExpressionExperiment, AuditEvent>> auditEventsByType =
                auditEventService.getLastEvents( visibleEEs, auditTypes );

        // ONE batched DEA-existence call. Returns the subset of visibleIds that have a DEA.
        Set<Long> hasDeaIds = new HashSet<>( differentialExpressionAnalysisService
                .getExperimentsWithAnalysis( visibleIds, true ) );

        // ONE per-EE platform fetch: replaces N expressionExperimentService.getArrayDesignsUsed(ee)
        // round-trips with one HQL. Used only to decide whether the "missing value" pipeline step
        // is applicable (it's two-colour/dual-mode only).
        Map<ExpressionExperiment, Collection<ArrayDesign>> adsByEe =
                expressionExperimentService.getArrayDesignsUsedByExperiment( visibleEEs );

        // ONE batched batch-info probe: replaces N expressionExperimentBatchInformationService
        // .checkHasBatchInfo(ee) round-trips (each of which thawLiters the EE for its factors).
        Map<ExpressionExperiment, Boolean> hasBatchInfoByEe =
                expressionExperimentBatchInformationService.checkHasBatchInfo( visibleEEs );

        // ONE batched GEEQ VO load: collect every visible EE's geeq id from the proxy (no
        // initialization required), then one geeqService.loadValueObjectsByIds() call.
        Map<Long, Long> geeqIdByEeId = new HashMap<>( visibleEEs.size() );
        for ( ExpressionExperiment ee : visibleEEs ) {
            Geeq geeqProxy = ee.getGeeq();
            if ( geeqProxy != null && geeqProxy.getId() != null ) {
                geeqIdByEeId.put( ee.getId(), geeqProxy.getId() );
            }
        }
        Map<Long, GeeqValueObject> geeqVoById = new HashMap<>();
        if ( !geeqIdByEeId.isEmpty() ) {
            for ( GeeqValueObject vo : geeqService.loadValueObjectsByIds( geeqIdByEeId.values() ) ) {
                if ( vo != null && vo.getId() != null ) {
                    geeqVoById.put( vo.getId(), vo );
                }
            }
        }

        boolean isAdmin = SecurityUtil.isUserAdmin();

        Map<Long, PipelineStatusValueObject> result = new LinkedHashMap<>( visibleEEs.size() );
        // Walk requestedIds so insertion order matches the caller's request (visibleEEs is sorted by id).
        Map<Long, ExpressionExperiment> eeById = new HashMap<>( visibleEEs.size() );
        for ( ExpressionExperiment ee : visibleEEs ) {
            eeById.put( ee.getId(), ee );
        }
        for ( Long requestedId : requestedIds ) {
            ExpressionExperiment ee = eeById.get( requestedId );
            if ( ee == null ) {
                continue; // ACL-dropped or missing
            }
            result.put( ee.getId(), buildPipelineStatus( ee, auditEventsByType, hasDeaIds.contains( ee.getId() ),
                    isAdmin, adsByEe.getOrDefault( ee, Collections.emptySet() ),
                    Boolean.TRUE.equals( hasBatchInfoByEe.get( ee ) ),
                    resolveGeeqVo( ee, geeqIdByEeId, geeqVoById ) ) );
        }
        return respond( result );
    }

    @Nullable
    private static GeeqValueObject resolveGeeqVo( ExpressionExperiment ee,
            Map<Long, Long> geeqIdByEeId, Map<Long, GeeqValueObject> geeqVoById ) {
        Long geeqId = geeqIdByEeId.get( ee.getId() );
        return geeqId != null ? geeqVoById.get( geeqId ) : null;
    }

    /**
     * Build a {@link PipelineStatusValueObject} for one EE, reusing pre-batched audit-event,
     * DEA-existence, array-design, batch-info, and GEEQ inputs. Extracted from the single-EE
     * {@link #getDatasetPipelineStatus} handler so both paths share the same per-step,
     * curation, and GEEQ assembly logic; bulk callers pre-batch the inputs to avoid per-EE
     * round-trips through the tunnel.
     */
    private PipelineStatusValueObject buildPipelineStatus( ExpressionExperiment ee,
            Map<Class<? extends AuditEventType>, Map<ExpressionExperiment, AuditEvent>> auditEventsByType,
            boolean hasDea, boolean isAdmin, Collection<ArrayDesign> arrayDesignsUsed,
            boolean hasBatchInfo, @Nullable GeeqValueObject geeq ) {
        CurationDetails cd = ee.getCurationDetails();
        boolean missingValueApplicable = hasTwoColorOrDualModePlatform( arrayDesignsUsed );
        List<PipelineStatusValueObject.PipelineStepValueObject> steps = new ArrayList<>( PIPELINE_STEPS.size() );
        for ( PipelineStepDescriptor desc : PIPELINE_STEPS ) {
            boolean applicable = !"missingValue".equals( desc.stepKey ) || missingValueApplicable;
            steps.add( buildPipelineStep( ee, desc, applicable, auditEventsByType ) );
        }
        PipelineStatusValueObject result = new PipelineStatusValueObject();
        result.setExperimentId( ee.getId() );
        result.setSteps( steps );
        result.setHasBatchInformation( hasBatchInfo );
        result.setHasDifferentialExpressionAnalysis( hasDea );
        // Coexpression was removed in Phase 1c; field retained on the VO for back-compat, always false.
        result.setHasCoexpressionAnalysis( false );
        result.setTroubled( cd.getTroubled() );
        result.setTroubleDetails( cd.getTroubled() && cd.getCurationNote() != null ? cd.getCurationNote() : "" );
        result.setNeedsAttention( cd.getNeedsAttention() );
        if ( isAdmin ) {
            result.setCurationNote( cd.getCurationNote() );
        }
        result.setIsPublic( securityService.isPublic( ee ) );
        if ( geeq != null ) {
            AuditEvent geeqEvent = lookupAuditEvent( auditEventsByType, GeeqEvent.class, ee );
            if ( geeqEvent != null ) {
                geeq.setLastComputed( geeqEvent.getDate() );
            }
        }
        result.setGeeq( geeq );
        return result;
    }

    /**
     * Variant of {@link #hasTwoColorOrDualModePlatform(ExpressionExperiment)} that operates on
     * a pre-fetched platform collection — avoids the per-EE getArrayDesignsUsed round-trip
     * when bulk callers have already batched the lookup.
     */
    private boolean hasTwoColorOrDualModePlatform( Collection<ArrayDesign> ads ) {
        for ( ArrayDesign ad : ads ) {
            TechnologyType t = ad.getTechnologyType();
            if ( t == TechnologyType.TWOCOLOR || t == TechnologyType.DUALMODE ) {
                return true;
            }
        }
        return false;
    }

    @GET
    @Path("/{dataset}/geeq")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retrieve the GEEQ scores of a dataset",
            description = "Returns the administrative GEEQ view exposing the underlying suitability and quality "
                    + "score factors, plus a `lastComputed` timestamp from the most recent `GeeqEvent`. Returns "
                    + "404 when GEEQ has never been computed for this dataset (use `PUT /geeq` to compute it).",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist or GEEQ has not been computed for it.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<GeeqValueObject> getDatasetGeeq(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        ee = expressionExperimentService.thawLiter( ee );
        Geeq geeq = ee.getGeeq();
        if ( geeq == null ) {
            throw new NotFoundException( "GEEQ has not been computed for dataset " + ee.getShortName()
                    + "; use PUT /geeq to compute it." );
        }
        GeeqValueObject vo = new GeeqAdminValueObject( geeq );
        AuditEvent geeqEvent = auditEventService.getLastEvent( ee, GeeqEvent.class );
        if ( geeqEvent != null ) {
            vo.setLastComputed( geeqEvent.getDate() );
        }
        return respond( vo );
    }

    /**
     * Public sibling of {@link #getDatasetGeeq(DatasetArg)}: returns the per-factor GEEQ
     * breakdown without exposing the admin-only detected/manual override scores or the
     * free-text {@code otherIssues} curator field, which live on {@link GeeqAdminValueObject}.
     * Drives the GEEQ-badge popover in the browser UI for anonymous and non-admin users.
     * <p>
     * Served by {@link GeeqValueObject} directly. This used to return a parallel
     * {@code PublicGeeqValueObject}, written on the belief that the per-factor getters on
     * {@link GeeqValueObject} were JSON-suppressed; they were not — each backing field carries an
     * explicit {@code @JsonProperty} that Jackson keeps over the ignore on the parallel getter. The
     * two VOs serialized identical 25-key payloads, so the duplicate was retired.
     */
    @GET
    @Path("/{dataset}/geeq/public")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the public per-factor GEEQ breakdown of a dataset",
            description = "Returns the per-factor suitability and quality scores plus the aggregate "
                    + "`publicQualityScore` / `publicSuitabilityScore` already exposed inline on "
                    + "`GET /datasets/{dataset}`. Admin-only fields (detected/manual override scores, "
                    + "`otherIssues`) are omitted. Returns 404 when GEEQ has never been computed for "
                    + "the dataset.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist or GEEQ has not been computed for it.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<GeeqValueObject> getDatasetGeeqPublic(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        ee = expressionExperimentService.thawLiter( ee );
        Geeq geeq = ee.getGeeq();
        if ( geeq == null ) {
            throw new NotFoundException( "GEEQ has not been computed for dataset " + ee.getShortName()
                    + "; use PUT /geeq to compute it." );
        }
        GeeqValueObject vo = new GeeqValueObject( geeq );
        AuditEvent geeqEvent = auditEventService.getLastEvent( ee, GeeqEvent.class );
        if ( geeqEvent != null ) {
            vo.setLastComputed( geeqEvent.getDate() );
        }
        return respond( vo );
    }

    @PUT
    @Path("/{dataset}/geeq")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Recompute GEEQ scores for a dataset",
            description = "Synchronously recomputes the GEEQ quality and suitability scores for the dataset and "
                    + "writes a `GeeqEvent` to the audit log. The optional `mode` query parameter selects which "
                    + "subset of scores to recompute (`all`, `batch`, `reps`, `pub`); defaults to `all`. The "
                    + "returned object includes the updated scores and the `lastComputed` timestamp. Because the "
                    + "endpoint is admin-only, the response is the administrative GEEQ view exposing the "
                    + "underlying score variables.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<GeeqValueObject> recomputeDatasetGeeq(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("mode") @DefaultValue("all") GeeqService.ScoreMode mode
    ) {
        return doRecomputeDatasetGeeq( datasetArg, mode );
    }

    /**
     * Request body for {@link #recomputeDatasetGeeqViaPost}. All fields optional; an empty / missing body
     * triggers a full ({@code mode=all}) recompute.
     */
    public static class GeeqRecomputeRequest {
        @Nullable
        private GeeqService.ScoreMode mode;

        @Nullable
        public GeeqService.ScoreMode getMode() {
            return mode;
        }

        public void setMode( @Nullable GeeqService.ScoreMode mode ) {
            this.mode = mode;
        }
    }

    /**
     * Alias for {@link #recomputeDatasetGeeq(DatasetArg, GeeqService.ScoreMode)} that exposes the GEEQ
     * recompute under {@code POST /datasets/{id}/geeq/recompute} with a JSON body.
     * <p>
     * Curation-UI compatibility shim: the curation-UI workflow-step "recompute GEEQ" button posts to this
     * path. Behaviour is identical to {@code PUT /datasets/{id}/geeq} — both delegate to the same handler.
     * The {@code mode} defaults to {@code all} when the body is omitted. See {@code
     * CURATION_UI_HANDOFF_INVENTORY.md}.
     */
    @POST
    @Path("/{dataset}/geeq/recompute")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Recompute GEEQ scores for a dataset (alias of PUT /geeq)",
            description = "Curation-UI compatibility alias for `PUT /datasets/{id}/geeq`. Body: optional "
                    + "`{\"mode\": \"all\"|\"batch\"|\"reps\"|\"pub\"}` (defaults to `all`). Behaviour is identical "
                    + "to the canonical endpoint.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<GeeqValueObject> recomputeDatasetGeeqViaPost(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable GeeqRecomputeRequest body
    ) {
        GeeqService.ScoreMode mode = ( body != null && body.getMode() != null )
                ? body.getMode() : GeeqService.ScoreMode.all;
        return doRecomputeDatasetGeeq( datasetArg, mode );
    }

    /**
     * Curation-UI compatibility alias for {@link #recomputeDatasetGeeqViaPost}: UI calls
     * {@code POST /datasets/{id}/geeq/recalculate} with the same body shape.
     */
    @POST
    @Path("/{dataset}/geeq/recalculate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Recompute GEEQ scores for a dataset (alias of /geeq/recompute)", hidden = true,
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) })
    public ResponseDataObject<GeeqValueObject> recomputeDatasetGeeqViaPostAlias(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable GeeqRecomputeRequest body
    ) {
        return recomputeDatasetGeeqViaPost( datasetArg, body );
    }

    private ResponseDataObject<GeeqValueObject> doRecomputeDatasetGeeq( DatasetArg<?> datasetArg, GeeqService.ScoreMode mode ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        Geeq updated = geeqService.calculateScore( ee, mode );
        GeeqValueObject vo = new GeeqAdminValueObject( updated );
        AuditEvent geeqEvent = auditEventService.getLastEvent( ee, GeeqEvent.class );
        if ( geeqEvent != null ) {
            vo.setLastComputed( geeqEvent.getDate() );
        }
        return respond( vo );
    }

    /**
     * Optional request body for {@link #importDataset}. Only {@code accession} is required.
     */
    public static class DatasetImportRequest {
        @Nullable
        private String accession;
        @Nullable
        private String arrayDesignName;
        @Nullable
        private Boolean loadPlatformOnly;
        @Nullable
        private Boolean suppressMatching;
        @Nullable
        private Boolean splitByPlatform;
        @Nullable
        private Boolean aggressiveQtRemoval;
        @Nullable
        private Boolean allowSuperSeriesLoad;
        @Nullable
        private Boolean allowArrayExpressDesign;
        @Nullable
        private Boolean isArrayExpress;
        @Nullable
        private Boolean suppressPostProcessing;

        @Nullable
        public String getAccession() {
            return accession;
        }

        public void setAccession( @Nullable String accession ) {
            this.accession = accession;
        }

        @Nullable
        public String getArrayDesignName() {
            return arrayDesignName;
        }

        public void setArrayDesignName( @Nullable String arrayDesignName ) {
            this.arrayDesignName = arrayDesignName;
        }

        @Nullable
        public Boolean getLoadPlatformOnly() {
            return loadPlatformOnly;
        }

        public void setLoadPlatformOnly( @Nullable Boolean loadPlatformOnly ) {
            this.loadPlatformOnly = loadPlatformOnly;
        }

        @Nullable
        public Boolean getSuppressMatching() {
            return suppressMatching;
        }

        public void setSuppressMatching( @Nullable Boolean suppressMatching ) {
            this.suppressMatching = suppressMatching;
        }

        @Nullable
        public Boolean getSplitByPlatform() {
            return splitByPlatform;
        }

        public void setSplitByPlatform( @Nullable Boolean splitByPlatform ) {
            this.splitByPlatform = splitByPlatform;
        }

        @Nullable
        public Boolean getAggressiveQtRemoval() {
            return aggressiveQtRemoval;
        }

        public void setAggressiveQtRemoval( @Nullable Boolean aggressiveQtRemoval ) {
            this.aggressiveQtRemoval = aggressiveQtRemoval;
        }

        @Nullable
        public Boolean getAllowSuperSeriesLoad() {
            return allowSuperSeriesLoad;
        }

        public void setAllowSuperSeriesLoad( @Nullable Boolean allowSuperSeriesLoad ) {
            this.allowSuperSeriesLoad = allowSuperSeriesLoad;
        }

        @Nullable
        public Boolean getAllowArrayExpressDesign() {
            return allowArrayExpressDesign;
        }

        public void setAllowArrayExpressDesign( @Nullable Boolean allowArrayExpressDesign ) {
            this.allowArrayExpressDesign = allowArrayExpressDesign;
        }

        @Nullable
        public Boolean getIsArrayExpress() {
            return isArrayExpress;
        }

        public void setIsArrayExpress( @Nullable Boolean isArrayExpress ) {
            this.isArrayExpress = isArrayExpress;
        }

        @Nullable
        public Boolean getSuppressPostProcessing() {
            return suppressPostProcessing;
        }

        public void setSuppressPostProcessing( @Nullable Boolean suppressPostProcessing ) {
            this.suppressPostProcessing = suppressPostProcessing;
        }
    }

    /**
     * Curation-UI workflow-step endpoint: kick off an async GEO (or ArrayExpress) accession load. The actual loader
     * runs inside {@link ExpressionExperimentLoadTaskCommand}; this handler submits the command to the
     * {@link TaskRunningService} and returns a 202 with a {@code Location} header pointing at the polling endpoint.
     */
    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Import a dataset from GEO (or ArrayExpress) by accession",
            description = "Submits an async load task and returns 202 with a `Location` header pointing at "
                    + "`/tasks/{taskId}`. Body must include `accession`. Optional flags map to the corresponding "
                    + "fields on `ExpressionExperimentLoadTaskCommand`, including `suppressPostProcessing` "
                    + "(skip processed-vector creation and diagnostics, mirroring the CLI `-nopost` flag; the "
                    + "usual case for RNA-seq loads reanalyzed from raw sequence later). The load runs in the background, so its "
                    + "outcome (including any failure) is reported by polling `/tasks/{taskId}`: a failed load "
                    + "carries a structured `error` (`code` + `message`, e.g. `NETWORK_ERROR`, `ALREADY_EXISTS`, "
                    + "`INVALID_ACCESSION`, `BLACKLISTED`, `SUPERSERIES_NOT_ALLOWED`).",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "400", description = "The request body is missing or `accession` is blank.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response importDataset( @Nullable DatasetImportRequest body ) {
        if ( body == null || body.getAccession() == null || body.getAccession().trim().isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-blank `accession`." );
        }
        ExpressionExperimentLoadTaskCommand cmd = new ExpressionExperimentLoadTaskCommand();
        cmd.setAccession( body.getAccession().trim() );
        if ( body.getArrayDesignName() != null ) {
            cmd.setArrayDesignName( body.getArrayDesignName() );
        }
        if ( body.getLoadPlatformOnly() != null ) {
            cmd.setLoadPlatformOnly( body.getLoadPlatformOnly() );
        }
        if ( body.getSuppressMatching() != null ) {
            cmd.setSuppressMatching( body.getSuppressMatching() );
        }
        if ( body.getSplitByPlatform() != null ) {
            cmd.setSplitByPlatform( body.getSplitByPlatform() );
        }
        if ( body.getAggressiveQtRemoval() != null ) {
            cmd.setAggressiveQtRemoval( body.getAggressiveQtRemoval() );
        }
        if ( body.getAllowSuperSeriesLoad() != null ) {
            cmd.setAllowSuperSeriesLoad( body.getAllowSuperSeriesLoad() );
        }
        if ( body.getAllowArrayExpressDesign() != null ) {
            cmd.setAllowArrayExpressDesign( body.getAllowArrayExpressDesign() );
        }
        if ( body.getIsArrayExpress() != null ) {
            cmd.setArrayExpress( body.getIsArrayExpress() );
        }
        if ( body.getSuppressPostProcessing() != null ) {
            cmd.setSuppressPostProcessing( body.getSuppressPostProcessing() );
        }
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    @POST
    @Path("/{dataset}/tasks/preprocess")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Run preprocessing run for a dataset",
            description = "Recomputes processed data vectors and refreshes downstream diagnostics. Returns 202 with "
                    + "a `Location` header pointing at the polling endpoint `/tasks/{taskId}`. Tasks are kept "
                    + "in memory only and evicted ~10 minutes after completion.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response runDatasetPreprocess(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        PreprocessTaskCommand cmd = new PreprocessTaskCommand( ee );
        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    @POST
    @Path("/{dataset}/tasks/diagnostics")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Submit a diagnostics-only preprocessing run for a dataset",
            description = "Refreshes mean-variance, PCA and sample-correlation diagnostics without recomputing "
                    + "processed vectors. Returns 202 with a `Location` header pointing at `/tasks/{taskId}`.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response runDatasetDiagnostics(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        PreprocessTaskCommand cmd = new PreprocessTaskCommand( ee );
        cmd.setDiagnosticsOnly( true );
        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    @POST
    @Path("/{dataset}/tasks/svd")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Recompute the singular value decomposition for a dataset",
            description = "Submits an async task that recomputes the SVD of the dataset's expression matrix and "
                    + "persists the result. The companion `GET /{dataset}/svd` reads the stored result. Returns 202 "
                    + "with a `Location` header pointing at `/tasks/{taskId}`.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response runDatasetSvd(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        SvdTaskCommand cmd = new SvdTaskCommand( ee );
        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    @POST
    @Path("/{dataset}/tasks/batchInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Run a batch-information fetch for a dataset",
            description = "Re-fetches batch information from the source data. Returns 202 with a `Location` "
                    + "header pointing at `/tasks/{taskId}`.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response runDatasetBatchInformationFetch(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        BatchInfoFetchTaskCommand cmd = new BatchInfoFetchTaskCommand( ee );
        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    @POST
    @Path("/{dataset}/tasks/geeq")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Recompute GEEQ quality scores for a dataset (async)",
            description = "Submits an async task that recomputes the GEEQ quality and suitability scores for the "
                    + "dataset and writes a `GeeqEvent` to the audit log. The optional `mode` query parameter "
                    + "selects which subset of scores to recompute (`all`, `batch`, `reps`, `pub`); defaults to "
                    + "`all`. Returns 202 with a `Location` header pointing at `/tasks/{taskId}`. The companion "
                    + "synchronous endpoint `PUT /{dataset}/geeq` blocks until the recompute finishes; this task "
                    + "variant returns immediately.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response runDatasetGeeq(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("mode") @DefaultValue("all") GeeqService.ScoreMode mode
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        GeeqTaskCommand cmd = new GeeqTaskCommand( ee, mode );
        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    /**
     * Optional request body for {@link #runDatasetSwitchPlatform}.
     */
    public static class PlatformSwitchRequest {
        @Nullable
        private String targetArrayDesignName;

        @Nullable
        public String getTargetArrayDesignName() {
            return targetArrayDesignName;
        }

        public void setTargetArrayDesignName( @Nullable String targetArrayDesignName ) {
            this.targetArrayDesignName = targetArrayDesignName;
        }
    }

    @POST
    @Path("/{dataset}/tasks/switch-platform")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Switch a dataset to use a different (typically merged) array design (async)",
            description = "Submits an async task that switches every BioAssay on the experiment to use the supplied "
                    + "target ArrayDesign (looked up by short name, e.g. `GPL570`), remaps composite-sequence-keyed "
                    + "vectors, and writes an `ExpressionExperimentPlatformSwitchEvent` to the audit log. If "
                    + "`targetArrayDesignName` is omitted (or the request body is omitted entirely) the service "
                    + "auto-detects a merged platform that the experiment's current ArrayDesigns are merged into; "
                    + "fails if none exists or the merge is ambiguous. This task is potentially long-running because "
                    + "it remaps every raw vector and regenerates processed vectors. Returns 202 with a `Location` "
                    + "header pointing at `/tasks/{taskId}`. Mirrors the `switchExperimentPlatform` CLI.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "400", description = "The supplied target ArrayDesign short name does not match any platform.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response runDatasetSwitchPlatform(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable PlatformSwitchRequest body
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        ArrayDesign target = null;
        if ( body != null && body.getTargetArrayDesignName() != null && !body.getTargetArrayDesignName().isBlank() ) {
            target = arrayDesignService.findByShortName( body.getTargetArrayDesignName() );
            if ( target == null ) {
                throw new BadRequestException(
                        "No ArrayDesign with shortName '" + body.getTargetArrayDesignName() + "' exists." );
            }
        }
        ExpressionExperimentPlatformSwitchTaskCommand cmd = new ExpressionExperimentPlatformSwitchTaskCommand( ee, target );
        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    /**
     * Curation-UI compatibility alias for {@link #runDatasetPreprocess}: UI calls {@code POST /datasets/{id}/preprocess}.
     */
    @POST
    @Path("/{dataset}/preprocess")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Run preprocessing run for a dataset (alias of /tasks/preprocess)", hidden = true,
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) })
    public Response runDatasetPreprocessAlias(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return runDatasetPreprocess( datasetArg );
    }

    /**
     * Curation-UI compatibility alias for {@link #runDatasetDiagnostics}: UI calls
     * {@code POST /datasets/{id}/preprocess/diagnostics}.
     */
    @POST
    @Path("/{dataset}/preprocess/diagnostics")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Submit a diagnostics-only preprocessing run for a dataset (alias of /tasks/diagnostics)", hidden = true,
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) })
    public Response runDatasetDiagnosticsAlias(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return runDatasetDiagnostics( datasetArg );
    }

    /**
     * Curation-UI compatibility alias for {@link #runDatasetBatchInformationFetch}: UI calls
     * {@code POST /datasets/{id}/batchInformation/fetch}.
     */
    @POST
    @Path("/{dataset}/batchInformation/fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Run a batch-information fetch for a dataset (alias of /tasks/batchInfo)", hidden = true,
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) })
    public Response runDatasetBatchInformationFetchAlias(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return runDatasetBatchInformationFetch( datasetArg );
    }

    /**
     * Optional request body for {@link #runDatasetDifferentialAnalysis}. When omitted, all non-batch experimental
     * factors are used and interactions are included.
     */
    public static class DifferentialAnalysisRunRequest {
        @Nullable
        private List<Long> factorIds;
        @Nullable
        private Boolean includeInteractions;
        @Nullable
        private Long subsetFactorId;

        @Nullable
        public List<Long> getFactorIds() {
            return factorIds;
        }

        public void setFactorIds( @Nullable List<Long> factorIds ) {
            this.factorIds = factorIds;
        }

        @Nullable
        public Boolean getIncludeInteractions() {
            return includeInteractions;
        }

        public void setIncludeInteractions( @Nullable Boolean includeInteractions ) {
            this.includeInteractions = includeInteractions;
        }

        @Nullable
        public Long getSubsetFactorId() {
            return subsetFactorId;
        }

        public void setSubsetFactorId( @Nullable Long subsetFactorId ) {
            this.subsetFactorId = subsetFactorId;
        }
    }

    @POST
    @Path("/{dataset}/tasks/differential")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Run differential expression analysis for a dataset",
            description = "If the request body is omitted (or all fields are null), every non-batch experimental "
                    + "factor is included with `includeInteractions=true`. Returns 202 with a `Location` header "
                    + "pointing at `/tasks/{taskId}`. Note: when any selected factor is a batch factor the "
                    + "interaction term is silently dropped, mirroring the legacy controller's behaviour.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "400", description = "The request body references factor ids that don't belong to the dataset, or names a subset factor that's also in `factorIds`.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response runDatasetDifferentialAnalysis(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable DifferentialAnalysisRunRequest body
    ) {
        return doRunDatasetDifferentialAnalysis( datasetArg, body );
    }

    /**
     * Alias for {@link #runDatasetDifferentialAnalysis(DatasetArg, DifferentialAnalysisRunRequest)} that exposes the
     * DEA dispatch under {@code /datasets/{id}/analyses/differential}.
     * <p>
     * Curation-UI compatibility shim: the curation-UI dispatch hook (apps/curation/.../workflow.ts:165) calls
     * {@code POST /datasets/{id}/analyses/differential}; the canonical gemma-rest endpoint lives at
     * {@code /tasks/differential}. Both paths delegate to the same handler so the UI's "dispatch DEA" button works
     * without modification. See {@code CURATION_UI_HANDOFF_INVENTORY.md}.
     */
    @POST
    @Path("/{dataset}/analyses/differential")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Run differential expression analysis for a dataset (alias of /tasks/differential)",
            description = "Curation-UI compatibility alias for `POST /datasets/{id}/tasks/differential`. Behaviour "
                    + "is identical to the canonical endpoint.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "400", description = "The request body references factor ids that don't belong to the dataset, or names a subset factor that's also in `factorIds`.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response runDatasetDifferentialAnalysisAlias(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable DifferentialAnalysisRunRequest body
    ) {
        return doRunDatasetDifferentialAnalysis( datasetArg, body );
    }

    private Response doRunDatasetDifferentialAnalysis(
            DatasetArg<?> datasetArg,
            @Nullable DifferentialAnalysisRunRequest body
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        // experimental design / factors are lazy-loaded; thaw them before iterating outside a transaction.
        ee = expressionExperimentService.thawLite( ee );
        if ( ee.getExperimentalDesign() == null ) {
            throw new BadRequestException( ee.getShortName() + " does not have an experimental design." );
        }
        Collection<ExperimentalFactor> allFactors = ee.getExperimentalDesign().getExperimentalFactors();

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );
        cmd.setUseWeights( expressionExperimentService.isRNASeq( ee ) );

        Collection<ExperimentalFactor> factors;
        boolean includeInteractions;
        ExperimentalFactor subsetFactor = null;

        boolean hasCustomFactors = body != null && body.getFactorIds() != null && !body.getFactorIds().isEmpty();
        if ( hasCustomFactors ) {
            factors = new HashSet<>();
            for ( ExperimentalFactor ef : allFactors ) {
                if ( body.getFactorIds().contains( ef.getId() ) ) {
                    factors.add( ef );
                }
            }
            if ( factors.size() != body.getFactorIds().size() ) {
                throw new BadRequestException( "One or more factor ids do not belong to dataset " + ee.getShortName() + "." );
            }
            includeInteractions = body.getIncludeInteractions() != null ? body.getIncludeInteractions() : false;
            if ( body.getSubsetFactorId() != null ) {
                for ( ExperimentalFactor ef : allFactors ) {
                    if ( body.getSubsetFactorId().equals( ef.getId() ) ) {
                        subsetFactor = ef;
                        break;
                    }
                }
                if ( subsetFactor == null ) {
                    throw new BadRequestException( "Subset factor id " + body.getSubsetFactorId() + " does not belong to dataset " + ee.getShortName() + "." );
                }
                if ( factors.contains( subsetFactor ) ) {
                    throw new BadRequestException( "Subset factor must not appear in factorIds." );
                }
            }
        } else {
            factors = allFactors.stream()
                    .filter( f -> !ExperimentFactorUtils.isBatchFactor( f ) )
                    .collect( Collectors.toSet() );
            includeInteractions = true;
        }

        // Mirror legacy behaviour: if any selected factor is a batch factor, drop the interaction term.
        for ( ExperimentalFactor ef : factors ) {
            if ( ExperimentFactorUtils.isBatchFactor( ef ) ) {
                log.warn( "Removing interaction term because it includes a batch factor for "
                        + ee.getShortName() );
                includeInteractions = false;
                break;
            }
        }

        cmd.setFactors( factors );
        cmd.setSubsetFactor( subsetFactor );
        cmd.setIncludeInteractions( includeInteractions );

        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    @POST
    @Path("/{dataset}/tasks/redo/{analysisId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Redo an existing differential expression analysis",
            description = "Re-runs the named differential analysis using its original configuration. Returns 202 "
                    + "with a `Location` header pointing at `/tasks/{taskId}`.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "404", description = "The dataset or analysis does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response redoDatasetDifferentialAnalysis(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("analysisId") Long analysisId
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        DifferentialExpressionAnalysis toRedo = differentialExpressionAnalysisService
                .findByExperimentAndAnalysisId( ee, true, analysisId );
        if ( toRedo == null ) {
            throw new NotFoundException( "No differential expression analysis with id " + analysisId
                    + " was found on dataset " + ee.getShortName() + "." );
        }
        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee, toRedo );
        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    @DELETE
    @Path("/{dataset}/tasks/differential/{analysisId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Remove of a differential expression analysis",
            description = "Asynchronously deletes the named differential analysis from the dataset. Returns 202 "
                    + "with a `Location` header pointing at `/tasks/{taskId}`; the actual delete completes "
                    + "in the background.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "202", content = @Content(schema = @Schema(ref = "ResponseDataObjectTaskStatusValueObject"))),
                    @ApiResponse(responseCode = "404", description = "The dataset or analysis does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response removeDatasetDifferentialAnalysis(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("analysisId") Long analysisId
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        DifferentialExpressionAnalysis toRemove = differentialExpressionAnalysisService
                .findByExperimentAndAnalysisId( ee, true, analysisId );
        if ( toRemove == null ) {
            throw new NotFoundException( "No differential expression analysis with id " + analysisId
                    + " was found on dataset " + ee.getShortName() + "." );
        }
        DifferentialExpressionAnalysisRemoveTaskCommand cmd =
                new DifferentialExpressionAnalysisRemoveTaskCommand( ee, toRemove );
        expressionExperimentReportService.evictFromCache( ee.getId() );
        return acceptedTaskResponse( taskRunningService.submitTaskCommand( cmd ) );
    }

    /**
     * Curation-UI compatibility alias for {@link #redoDatasetDifferentialAnalysis}: UI calls
     * {@code POST /datasets/{id}/analyses/differential/{aid}/redo}; the canonical handler lives at
     * {@code /tasks/redo/{analysisId}}.
     */
    @POST
    @Path("/{dataset}/analyses/differential/{analysisId}/redo")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Redo an existing differential expression analysis (alias of /tasks/redo/{analysisId})", hidden = true,
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) })
    public Response redoDatasetDifferentialAnalysisAlias(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("analysisId") Long analysisId
    ) {
        return redoDatasetDifferentialAnalysis( datasetArg, analysisId );
    }

    /**
     * Curation-UI compatibility alias for {@link #removeDatasetDifferentialAnalysis}: UI calls
     * {@code DELETE /datasets/{id}/analyses/differential/{aid}}.
     */
    @DELETE
    @Path("/{dataset}/analyses/differential/{analysisId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Remove a differential expression analysis (alias of /tasks/differential/{analysisId})", hidden = true,
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) })
    public Response removeDatasetDifferentialAnalysisAlias(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("analysisId") Long analysisId
    ) {
        return removeDatasetDifferentialAnalysis( datasetArg, analysisId );
    }

    /**
     * Delete the raw expression data vectors for a dataset (port of {@code deleteRawData} CLI).
     * <p>
     * Synchronous DB delete. Requires the destructive-intent guard {@code confirm=true}; without it the
     * request is rejected as a {@code 400} so an accidental call (mistyped URL, stale browser tab) cannot
     * wipe vectors. The {@code quantitationType} query param selects which raw QT to delete; when omitted
     * the dataset's preferred raw QT is used.
     */
    @DELETE
    @Path("/{dataset}/data/raw")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Delete raw expression data vectors for a dataset",
            description = "Synchronous deletion of the raw expression data vectors for the dataset. "
                    + "The `confirm=true` query parameter MUST be supplied; without it the call returns `400` "
                    + "to guard against accidental destruction. The optional `quantitationType` query param "
                    + "selects which raw QT to delete; if omitted, the preferred raw QT is used. Port of the "
                    + "`deleteRawData` CLI.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Raw data vectors deleted."),
                    @ApiResponse(responseCode = "400", description = "The `confirm=true` guard was not supplied.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response deleteDatasetRawData(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Optional quantitation-type selector; defaults to the dataset's preferred raw QT.")
            @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg,
            @Parameter(description = "Must be `true` to authorize the destructive delete.")
            @QueryParam("confirm") @DefaultValue("false") boolean confirm
    ) {
        if ( !confirm ) {
            throw new BadRequestException( "Refusing to delete raw data without `confirm=true`." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( quantitationTypeArg != null ) {
            qt = quantitationTypeArgService.getEntity( quantitationTypeArg, ee );
        } else {
            qt = expressionExperimentService.getPreferredQuantitationType( ee )
                    .orElseThrow( () -> new NotFoundException( ee.getShortName()
                            + " has no preferred raw quantitation type; supply `quantitationType` explicitly." ) );
        }
        expressionDataDeleterService.deleteRawData( ee, qt );
        return Response.noContent().build();
    }

    /**
     * Delete the processed expression data vectors for a dataset (port of {@code deleteProcessedData} CLI).
     * <p>
     * Synchronous DB delete. Requires the destructive-intent guard {@code confirm=true}; without it the
     * request is rejected as a {@code 400} so an accidental call cannot wipe vectors.
     */
    @DELETE
    @Path("/{dataset}/data/processed")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Delete processed expression data vectors for a dataset",
            description = "Synchronous deletion of the processed expression data vectors for the dataset. "
                    + "The `confirm=true` query parameter MUST be supplied; without it the call returns `400` "
                    + "to guard against accidental destruction. Port of the `deleteProcessedData` CLI.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Processed data vectors deleted."),
                    @ApiResponse(responseCode = "400", description = "The `confirm=true` guard was not supplied.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response deleteDatasetProcessedData(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Must be `true` to authorize the destructive delete.")
            @QueryParam("confirm") @DefaultValue("false") boolean confirm
    ) {
        if ( !confirm ) {
            throw new BadRequestException( "Refusing to delete processed data without `confirm=true`." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        expressionDataDeleterService.deleteProcessedData( ee );
        return Response.noContent().build();
    }

    private Response acceptedTaskResponse( String taskId ) {
        SubmittedTask task = taskRunningService.getSubmittedTask( taskId );
        TaskStatusValueObject vo;
        if ( task != null ) {
            vo = new TaskStatusValueObject( task );
        } else {
            // Submitted task should always be queryable immediately after submission, but guard anyway.
            vo = new TaskStatusValueObject();
            vo.setTaskId( taskId );
            vo.setStatus( "queued" );
            vo.setMessage( "" );
        }
        return Response.status( Response.Status.ACCEPTED )
                .location( URI.create( "/tasks/" + taskId ) )
                .entity( new ResponseDataObject<>( vo ) )
                .build();
    }

    /**
     * Retrieves the differential analysis results for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/analyses/differential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve annotations and surface level stats for a dataset's differential analyses",
            description = "By default, the per-analysis `bioAssaysAnalyzed` collection is omitted from the response: "
                    + "populating it requires thawing every BioAssay on every analyzed experiment, which dominates "
                    + "the cost of this endpoint on large datasets and is rarely needed by callers (the UI fetches "
                    + "sample metadata separately via `/datasets/{id}/samples`). Set `includeAssays=true` to opt back "
                    + "into the pre-2.0 behaviour.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<DifferentialExpressionAnalysisValueObject>> getDatasetDifferentialExpressionAnalyses( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @Parameter(deprecated = true, description = "This parameter is ignored and will be removed in the 2.10 release.") @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg, // Optional, default 0
            @Parameter(deprecated = true, description = "This parameter is ignored and will be removed in the 2.10 release.") @QueryParam("limit") @DefaultValue("20") LimitArg limitArg, // Optional, default 20
            @Parameter(description = "When true, populate the `bioAssaysAnalyzed` collection on each analysis. Defaults to false because thawing every BioAssay is expensive and the field is rarely consumed.") @QueryParam("includeAssays") @DefaultValue("false") boolean includeAssays // Optional, default false
    ) {
        List<DifferentialExpressionAnalysisValueObject> result;
        Long eeId = datasetArgService.getEntity( datasetArg ).getId();
        Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> map = differentialExpressionAnalysisService.findByExperimentIds( Collections.singleton( eeId ), true, includeAssays );
        if ( map == null || map.isEmpty() ) {
            result = Collections.emptyList();
        } else {
            result = map.get( map.keySet().iterator().next() ).stream()
                    .sorted( Comparator.comparing( IdentifiableUtils::getRequiredId ) )
                    .collect( Collectors.toList() );
        }
        return respond( result );
    }

    /**
     * Retrieves the result sets of all the differential expression analyses of a dataset.
     * <p>
     * This is actually performing a 302 Found redirection to point the HTTP client to the corresponding result sets
     * endpoint.
     *
     * @see AnalysisResultSetsWebService#getResultSets(DatasetArrayArg, DatabaseEntryArrayArg, FilterArg, OffsetArg, LimitArg, SortArg)
     */
    @GET
    @Path("/{dataset}/analyses/differential/resultSets")
    @Operation(summary = "Retrieve the result sets of all differential analyses of a dataset", responses = {
            @ApiResponse(responseCode = "302", description = "If the dataset is found, a redirection to the corresponding getResultSets operation."),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetDifferentialExpressionAnalysisResultSets(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Context UriInfo uriInfo ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        URI resultSetUri = uriInfo.getBaseUriBuilder()
                .scheme( null ).host( null ).port( -1 )
                .path( "/resultSets" )
                .queryParam( "datasets", "{datasetId}" )
                .build( ee.getId() );
        return Response.status( Response.Status.FOUND )
                .location( resultSetUri )
                .build();
    }

    private static final String GET_DATASETS_DIFFERENTIAL_ANALYSIS_EXPRESSION_RESULTS_DESCRIPTION = "Pagination with `offset` and `limit` is done on the datasets, thus `data` will hold a variable number of results.\n\nIf a result set has more than one probe for a given gene, the result corresponding to the lowest corrected P-value is retained. This statistic reflects the goodness of the fit of the linear model for the probe, and not the significance of the contrasts.\n\nResults for non-specific probes (i.e. probes that map to more than one genes) are excluded.";
    private static final String PVALUE_THRESHOLD_DESCRIPTION = "Maximum threshold on the corrected P-value to retain a result. The threshold is inclusive (i.e. 0.05 will match results with corrected P-values lower or equal to 0.05).";
    private static final int GET_DATASETS_DIFFERENTIAL_ANALYSIS_EXPRESSION_RESULTS_DEFAULT_LIMIT = 20;

    /**
     * Obtain differential expression analysis results for a given gene.
     */
    @GET
    @GZIP
    @Path("/analyses/differential/results/genes/{gene}")
    @Produces({ MediaType.APPLICATION_JSON, TEXT_TAB_SEPARATED_VALUES_UTF8 + "; q=0.9" })
    @Operation(
            summary = "Retrieve the differential expression results for a given gene among datasets matching the provided query and filter",
            description = GET_DATASETS_DIFFERENTIAL_ANALYSIS_EXPRESSION_RESULTS_DESCRIPTION,
            responses = {
                    @ApiResponse(responseCode = "200", content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = QueriedAndFilteredAndInferredAndPaginatedResponseDataObjectDifferentialExpressionAnalysisResultByGeneValueObject.class)),
                            @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8 + "; q=0.9", schema = @Schema(type = "string"))
                    })
            })
    public Object getDatasetsDifferentialExpressionAnalysisResultsForGene(
            @PathParam("gene") GeneArg<?> geneArg,
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("offset") OffsetArg offsetArg,
            @QueryParam("limit") LimitArg limitArg,
            @Parameter(description = PVALUE_THRESHOLD_DESCRIPTION, schema = @Schema(minimum = "0.0", maximum = "1.0")) @QueryParam("threshold") @DefaultValue("1.0") Double threshold,
            @Context HttpHeaders headers
    ) {
        Gene gene = geneArgService.getEntity( geneArg );
        MediaType accepted = negotiate( headers, MediaType.APPLICATION_JSON_TYPE, withQuality( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE, 0.9 ) );
        if ( accepted.equals( MediaType.APPLICATION_JSON_TYPE ) ) {
            return getDatasetsDifferentialExpressionAnalysisResultsForGeneInternal( gene, query, filter, offsetArg, limitArg, threshold );
        } else {
            if ( offsetArg != null || limitArg != null ) {
                throw new BadRequestException( "The offset/limit parameters cannot be used with the TSV representation." );
            }
            return getDatasetsDifferentialExpressionAnalysisResultsForGeneInternalAsTsv( gene, query, filter, threshold );
        }
    }

    /**
     * Obtain differential expression analysis results for a given gene in a given taxon.
     */
    @GET
    @GZIP
    @Path("/analyses/differential/results/taxa/{taxon}/genes/{gene}")
    @Produces({ MediaType.APPLICATION_JSON, TEXT_TAB_SEPARATED_VALUES_UTF8 })
    @Operation(
            summary = "Retrieve the differential expression results for a given gene and taxa among datasets matching the provided query and filter",
            description = GET_DATASETS_DIFFERENTIAL_ANALYSIS_EXPRESSION_RESULTS_DESCRIPTION,
            responses = {
                    @ApiResponse(responseCode = "200", content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = QueriedAndFilteredAndInferredAndPaginatedResponseDataObjectDifferentialExpressionAnalysisResultByGeneValueObject.class)),
                            @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, schema = @Schema(type = "string"))
                    })
            })
    public Object getDatasetsDifferentialExpressionAnalysisResultsForGeneInTaxon(
            @PathParam("taxon") TaxonArg<?> taxonArg,
            @PathParam("gene") GeneArg<?> geneArg,
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("offset") OffsetArg offsetArg,
            @QueryParam("limit") LimitArg limitArg,
            @Parameter(description = PVALUE_THRESHOLD_DESCRIPTION, schema = @Schema(minimum = "0.0", maximum = "1.0")) @QueryParam("threshold") @DefaultValue("1.0") Double threshold,
            @Context HttpHeaders headers
    ) {
        Taxon taxon = taxonArgService.getEntity( taxonArg );
        Gene gene = geneArgService.getEntityWithTaxon( geneArg, taxon );
        MediaType accepted = negotiate( headers, MediaType.APPLICATION_JSON_TYPE, TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE );
        if ( accepted.equals( MediaType.APPLICATION_JSON_TYPE ) ) {
            return getDatasetsDifferentialExpressionAnalysisResultsForGeneInternal( gene, query, filter, offsetArg, limitArg, threshold );
        } else {
            if ( offsetArg != null || limitArg != null ) {
                throw new BadRequestException( "The offset/limit parameters cannot be used with the TSV representation." );
            }
            return getDatasetsDifferentialExpressionAnalysisResultsForGeneInternalAsTsv( gene, query, filter, threshold );
        }
    }

    private QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultByGeneValueObject> getDatasetsDifferentialExpressionAnalysisResultsForGeneInternal( Gene gene, QueryArg query, FilterArg<ExpressionExperiment> filter, OffsetArg offsetArg, LimitArg limitArg, double threshold ) {
        int offset = offsetArg != null ? offsetArg.getValue() : 0;
        int limit = limitArg != null ? limitArg.getValue() : GET_DATASETS_DIFFERENTIAL_ANALYSIS_EXPRESSION_RESULTS_DEFAULT_LIMIT;
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, null, inferredTerms );
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        if ( threshold < 0 || threshold > 1 ) {
            throw new BadRequestException( "The threshold must be in the [0, 1] interval." );
        }
        List<Long> ids = new ArrayList<>( expressionExperimentService.loadIdsWithCache( filters, expressionExperimentService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ) ) );
        if ( query != null ) {
            ids.retainAll( datasetArgService.getIdsForSearchQuery( query, warnings ) );
        }
        // slice IDs
        Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap = new HashMap<>();
        Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap = new HashMap<>();
        Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap = new HashMap<>();
        List<DifferentialExpressionAnalysisResultByGeneValueObject> payload = differentialExpressionResultService
                .findByGeneAndExperimentAnalyzedIds( gene, true, false, sliceIds( ids, offset, limit ), true, sourceExperimentIdMap, experimentAnalyzedIdMap, baselineMap, threshold, true ).stream()
                .map( r -> new DifferentialExpressionAnalysisResultByGeneValueObject( r, sourceExperimentIdMap.get( r ), experimentAnalyzedIdMap.get( r ), baselineMap.get( r ) ) )
                .sorted( Comparator.comparing( DifferentialExpressionAnalysisResultByGeneValueObject::getSourceExperimentId )
                        .thenComparing( DifferentialExpressionAnalysisResultByGeneValueObject::getExperimentAnalyzedId )
                        .thenComparing( DifferentialExpressionAnalysisResultByGeneValueObject::getResultSetId ) )
                .collect( Collectors.toList() );

        // obtain result set IDs of results that lack baselines (i.e. for interactions)
        Set<Long> missingBaselines = payload.stream()
                .filter( vo -> vo.getBaseline() == null )
                .map( DifferentialExpressionAnalysisResultByGeneValueObject::getResultSetId ).collect( Collectors.toSet() );
        Map<Long, Baseline> b = expressionAnalysisResultSetService.getBaselinesForInteractionsByIds( missingBaselines, true );
        for ( DifferentialExpressionAnalysisResultByGeneValueObject r : payload ) {
            Baseline b2 = b.get( r.getResultSetId() );
            if ( b2 == null ) {
                continue;
            }
            r.setBaseline( new FactorValueBasicValueObject( b2.getFactorValue() ) );
            if ( b2.getSecondFactorValue() != null ) {
                r.setSecondBaseline( new FactorValueBasicValueObject( b2.getSecondFactorValue() ) );
            }
        }

        return paginate( new Slice<>( payload, Sort.by( null, "sourceExperimentId", Sort.Direction.ASC, Sort.NullMode.LAST, "sourceExperimentId" ), offset, limit, ( long ) ids.size() ),
                query != null ? query.getValue() : null, filters, new String[] { "sourceExperimentId", "experimentAnalyzedId", "resultSetId" }, inferredTerms )
                .addWarnings( warnings, "query", LocationType.QUERY );
    }

    public static class QueriedAndFilteredAndInferredAndPaginatedResponseDataObjectDifferentialExpressionAnalysisResultByGeneValueObject extends QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultByGeneValueObject> {

        public QueriedAndFilteredAndInferredAndPaginatedResponseDataObjectDifferentialExpressionAnalysisResultByGeneValueObject( Slice<DifferentialExpressionAnalysisResultByGeneValueObject> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, query, filters, groupBy, inferredTerms );
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DifferentialExpressionAnalysisResultByGeneValueObject extends DifferentialExpressionAnalysisResultValueObject {

        /**
         * The ID of the source experiment, which differs only if this result is from a subset. This is always referring
         * to an {@link ExpressionExperiment}.
         */
        private Long sourceExperimentId;
        /**
         * The ID of the experiment analyzed which is either an {@link ExpressionExperiment} or an {@link ExpressionExperimentSubSet}.
         */
        private Long experimentAnalyzedId;
        /**
         * The result set ID to which this result belong.
         */
        private Long resultSetId;

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private FactorValueBasicValueObject baseline;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private FactorValueBasicValueObject secondBaseline;

        public DifferentialExpressionAnalysisResultByGeneValueObject( DifferentialExpressionAnalysisResult result, Long sourceExperimentId, Long experimentAnalyzedId, @Nullable Baseline baseline ) {
            super( result, true );
            this.sourceExperimentId = sourceExperimentId;
            this.experimentAnalyzedId = experimentAnalyzedId;
            this.resultSetId = result.getResultSet().getId();
            if ( baseline != null ) {
                this.baseline = new FactorValueBasicValueObject( baseline.getFactorValue() );
                if ( baseline.getSecondFactorValue() != null ) {
                    this.secondBaseline = new FactorValueBasicValueObject( baseline.getSecondFactorValue() );
                }
            }
        }
    }

    private StreamingOutput getDatasetsDifferentialExpressionAnalysisResultsForGeneInternalAsTsv( Gene gene, QueryArg query, FilterArg<ExpressionExperiment> filter, double threshold ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, null, inferredTerms );
        if ( threshold < 0 || threshold > 1 ) {
            throw new BadRequestException( "The threshold must be in the [0, 1] interval." );
        }
        Set<Long> ids = new HashSet<>( expressionExperimentService.loadIdsWithCache( filters, expressionExperimentService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ) ) );
        if ( query != null ) {
            ids.retainAll( datasetArgService.getIdsForSearchQuery( query, null ) );
        }
        Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap = new HashMap<>();
        Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap = new HashMap<>();
        Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap = new HashMap<>();
        //noinspection Convert2MethodRef
        List<DifferentialExpressionAnalysisResult> payload = differentialExpressionResultService.findByGeneAndExperimentAnalyzedIds( gene, true, false, ids, true, sourceExperimentIdMap, experimentAnalyzedIdMap, baselineMap, threshold, false ).stream()
                .sorted( Comparator.comparing( ( DifferentialExpressionAnalysisResult r ) -> sourceExperimentIdMap.get( r ) )
                        .thenComparing( ( DifferentialExpressionAnalysisResult r ) -> experimentAnalyzedIdMap.get( r ) )
                        .thenComparing( ( DifferentialExpressionAnalysisResult r ) -> r.getResultSet().getId() ) )
                .collect( Collectors.toList() );
        // obtain result set IDs of results that lack baselines (i.e. for interactions)
        Set<@MayBeUninitialized ExpressionAnalysisResultSet> missingBaselines = payload.stream()
                .filter( vo -> baselineMap.get( vo ) == null )
                .map( DifferentialExpressionAnalysisResult::getResultSet )
                .collect( toIdentifiableSet() );
        Map<@MayBeUninitialized ExpressionAnalysisResultSet, Baseline> b = expressionAnalysisResultSetService.getBaselinesForInteractions( missingBaselines, false );
        for ( DifferentialExpressionAnalysisResult r : payload ) {
            Baseline b2 = b.get( r.getResultSet() );
            if ( b2 == null ) {
                continue;
            }
            baselineMap.put( r, b2 );
        }
        return output -> {
            try ( Writer writer = new OutputStreamWriter( output, StandardCharsets.UTF_8 ) ) {
                differentialExpressionAnalysisResultListFileService.writeTsv( payload, gene, sourceExperimentIdMap, experimentAnalyzedIdMap, baselineMap, writer );
            }
        };
    }

    /**
     * Retrieves the annotations for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @CacheControl(maxAge = 1200)
    @Path("/{dataset}/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the annotations of a dataset", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<Set<AnnotationValueObject>> getDatasetAnnotations( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @Parameter(description = "Also return tags that carry no ontology mapping. Off by "
                    + "default, which suits display: an unmapped string cannot be searched or "
                    + "reasoned over. Turn it on for curation read-back — a free-text tag is "
                    + "persisted like any other, so a caller that has just written one otherwise "
                    + "reads back nothing and cannot tell a rejected write from a filtered read.")
            @QueryParam("includeFreeText") @DefaultValue("false") Boolean includeFreeText
    ) {
        return respond( datasetArgService.getAnnotations( datasetArg, Boolean.TRUE.equals( includeFreeText ) ) );
    }

    /**
     * Request body for {@link #updateDatasetAnnotations}. The {@code annotations} field is the desired
     * direct-EE characteristic set; the call is idempotent set-replace semantics (see the service
     * javadoc on {@code ExpressionExperimentService#updateAnnotations}).
     */
    public static class AnnotationsUpdateRequest {
        @Nullable
        private List<AnnotationTagInput> annotations;

        @Nullable
        public List<AnnotationTagInput> getAnnotations() {
            return annotations;
        }

        public void setAnnotations( @Nullable List<AnnotationTagInput> annotations ) {
            this.annotations = annotations;
        }
    }

    /**
     * Write-shape for an annotation tag. {@code category} and {@code value} are required;
     * {@code categoryUri} and {@code valueUri} are optional ontology pointers. This is the JSON shape
     * the curation-agents client sends; mapped to a {@code Characteristic} server-side.
     * <p>
     * When any of the eight optional {@code predicate*} / {@code object*} / {@code secondPredicate*} /
     * {@code secondObject*} fields is non-null the row is materialised as a {@link Statement} instead
     * of a plain {@link Characteristic}, with {@code value} / {@code valueUri} interpreted as the
     * statement's subject (Statement aliases subject ↔ value internally). The wire field set mirrors
     * what the read-side {@code AnnotationValueObject} exposes for Statement-backed annotations, so
     * a round-trip GET → PUT preserves the statement shape.
     */
    public static class AnnotationTagInput {
        private String category;
        @Nullable
        private String categoryUri;
        private String value;
        @Nullable
        private String valueUri;
        @Nullable
        private String predicate;
        @Nullable
        private String predicateUri;
        @Nullable
        private String object;
        @Nullable
        private String objectUri;
        @Nullable
        private String secondPredicate;
        @Nullable
        private String secondPredicateUri;
        @Nullable
        private String secondObject;
        @Nullable
        private String secondObjectUri;
        /**
         * Verbatim provenance backing the tag — a JSON array of {@code {quote, source, location, ...}}
         * items (the agents-side {@code FindingEvidence} shape). Stored opaquely and round-tripped on the
         * read VO's {@code supportingEvidence}. Null/omitted leaves any existing evidence untouched on a
         * set-replace update; non-null refreshes it on the matched tag.
         */
        @Nullable
        private com.fasterxml.jackson.databind.JsonNode supportingEvidence;

        public String getCategory() {
            return category;
        }

        public void setCategory( String category ) {
            this.category = category;
        }

        @Nullable
        public String getCategoryUri() {
            return categoryUri;
        }

        public void setCategoryUri( @Nullable String categoryUri ) {
            this.categoryUri = categoryUri;
        }

        public String getValue() {
            return value;
        }

        public void setValue( String value ) {
            this.value = value;
        }

        @Nullable
        public String getValueUri() {
            return valueUri;
        }

        public void setValueUri( @Nullable String valueUri ) {
            this.valueUri = valueUri;
        }

        @Nullable
        public String getPredicate() {
            return predicate;
        }

        public void setPredicate( @Nullable String predicate ) {
            this.predicate = predicate;
        }

        @Nullable
        public String getPredicateUri() {
            return predicateUri;
        }

        public void setPredicateUri( @Nullable String predicateUri ) {
            this.predicateUri = predicateUri;
        }

        @Nullable
        public String getObject() {
            return object;
        }

        public void setObject( @Nullable String object ) {
            this.object = object;
        }

        @Nullable
        public String getObjectUri() {
            return objectUri;
        }

        public void setObjectUri( @Nullable String objectUri ) {
            this.objectUri = objectUri;
        }

        @Nullable
        public String getSecondPredicate() {
            return secondPredicate;
        }

        public void setSecondPredicate( @Nullable String secondPredicate ) {
            this.secondPredicate = secondPredicate;
        }

        @Nullable
        public String getSecondPredicateUri() {
            return secondPredicateUri;
        }

        public void setSecondPredicateUri( @Nullable String secondPredicateUri ) {
            this.secondPredicateUri = secondPredicateUri;
        }

        @Nullable
        public String getSecondObject() {
            return secondObject;
        }

        public void setSecondObject( @Nullable String secondObject ) {
            this.secondObject = secondObject;
        }

        @Nullable
        public String getSecondObjectUri() {
            return secondObjectUri;
        }

        public void setSecondObjectUri( @Nullable String secondObjectUri ) {
            this.secondObjectUri = secondObjectUri;
        }

        @Nullable
        public com.fasterxml.jackson.databind.JsonNode getSupportingEvidence() {
            return supportingEvidence;
        }

        public void setSupportingEvidence( @Nullable com.fasterxml.jackson.databind.JsonNode supportingEvidence ) {
            this.supportingEvidence = supportingEvidence;
        }

        boolean hasStatementFields() {
            return predicate != null || predicateUri != null
                    || object != null || objectUri != null
                    || secondPredicate != null || secondPredicateUri != null
                    || secondObject != null || secondObjectUri != null;
        }
    }

    @PUT
    @Path("/{dataset}/annotations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Replace the direct annotations of a dataset",
            description = "Idempotent set-replace for the experiment-level tags (organism part, disease, "
                    + "treatment, etc.) held directly by the dataset. Tags on subsets, factor values, "
                    + "and biomaterials are NOT touched. The diff is computed by (category, categoryUri, "
                    + "value, valueUri); unchanged tags keep their identity, drops are removed, new ones "
                    + "are added with an `IC` evidence code. A single `ManualAnnotationEvent` is recorded "
                    + "when the call actually changes the set. Requires `ACL_SECURABLE_EDIT` on the dataset.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The request body is missing or malformed.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "403", description = "The caller lacks edit permission on the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<Set<AnnotationValueObject>> updateDatasetAnnotations(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable AnnotationsUpdateRequest body
    ) {
        if ( body == null || body.getAnnotations() == null ) {
            throw new BadRequestException( "A request body with an 'annotations' field is required (use an empty list to clear)." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        List<Characteristic> desired = new ArrayList<>( body.getAnnotations().size() );
        for ( AnnotationTagInput tag : body.getAnnotations() ) {
            if ( tag == null ) {
                throw new BadRequestException( "Annotation entries must not be null." );
            }
            if ( StringUtils.isBlank( tag.getCategory() ) ) {
                throw new BadRequestException( "Each annotation must have a non-blank 'category'." );
            }
            if ( StringUtils.isBlank( tag.getValue() ) ) {
                throw new BadRequestException( "Each annotation must have a non-blank 'value'." );
            }
            desired.add( tagToCharacteristic( tag ) );
        }
        expressionExperimentService.updateAnnotations( ee, desired );
        // Echo unmapped tags too. This endpoint accepts a tag with nothing but a category and a
        // value — no URIs required — so filtering them out of its own response meant it could
        // confirm a write by returning a list that did not contain what was just written, which
        // reads as a silent rejection. What the caller gets back is now what it sent.
        return respond( expressionExperimentService.getAnnotations( ee, true ) );
    }

    /**
     * Convert a wire {@link AnnotationTagInput} to a {@link Characteristic}, building a {@link Statement}
     * (with the "Statement" discriminator and the predicate / object pair) when any statement field is set,
     * else a plain {@link Characteristic}. Shared by the experiment- and sample-level annotation writes.
     * <p>
     * The wire's {@code value} / {@code valueUri} become the statement's subject — {@link Statement} aliases
     * subject &harr; value internally.
     */
    private static Characteristic tagToCharacteristic( AnnotationTagInput tag ) {
        if ( tag.hasStatementFields() ) {
            Statement s = Statement.Factory.newInstance();
            s.setCategory( tag.getCategory() );
            s.setCategoryUri( tag.getCategoryUri() );
            s.setSubject( tag.getValue() );
            if ( tag.getValueUri() != null ) {
                s.setSubjectUri( tag.getValueUri() );
            }
            s.setPredicate( tag.getPredicate() );
            s.setPredicateUri( tag.getPredicateUri() );
            s.setObject( tag.getObject() );
            s.setObjectUri( tag.getObjectUri() );
            s.setSecondPredicate( tag.getSecondPredicate() );
            s.setSecondPredicateUri( tag.getSecondPredicateUri() );
            s.setSecondObject( tag.getSecondObject() );
            s.setSecondObjectUri( tag.getSecondObjectUri() );
            s.setSupportingEvidence( serializeEvidence( tag.getSupportingEvidence() ) );
            return s;
        } else {
            Characteristic c = Characteristic.Factory.newInstance();
            c.setCategory( tag.getCategory() );
            c.setCategoryUri( tag.getCategoryUri() );
            c.setValue( tag.getValue() );
            c.setValueUri( tag.getValueUri() );
            c.setSupportingEvidence( serializeEvidence( tag.getSupportingEvidence() ) );
            return c;
        }
    }

    /**
     * Serialize the wire's supporting-evidence tree to the opaque JSON string Gemma stores. Null/empty
     * (including a JSON {@code null}) maps to a stored {@code null}, so a tag arriving without evidence
     * doesn't clobber any evidence already on a matched tag (see {@code updateAnnotations}).
     * <p>
     * Package-private rather than private because {@code AnnotationsWebService.annotationDtoToCharacteristic}
     * maps the same field off {@code AnnotationDto}. One definition, so the empty-array-collapses-to-null
     * rule cannot drift between the two write paths.
     */
    @Nullable
    static String serializeEvidence( @Nullable com.fasterxml.jackson.databind.JsonNode evidence ) {
        return CharacteristicUtils.serializeSupportingEvidence( evidence );
    }

    /**
     * Resolve a {@code {bioAssayId}} path param to its sample (the {@link BioMaterial}), validating that the
     * assay belongs to the path-derived dataset. Mirrors the addressing of the outlier endpoints — a "sample"
     * is addressed by its BioAssay id; characteristics live on the underlying BioMaterial. Throws
     * {@link NotFoundException} when the assay does not belong to the dataset so the caller cannot reach a
     * foreign sample through a dataset it can edit.
     */
    private BioMaterial resolveSampleBioMaterial( ExpressionExperiment ee, Long bioAssayId ) {
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( ee );
        for ( BioAssay ba : thawed.getBioAssays() ) {
            if ( bioAssayId.equals( ba.getId() ) ) {
                BioMaterial bm = ba.getSampleUsed();
                return bioMaterialService.thaw( bm );
            }
        }
        throw new NotFoundException( "BioAssay " + bioAssayId + " does not belong to dataset " + thawed.getShortName() + "." );
    }

    private static Set<AnnotationValueObject> sampleAnnotationVos( BioMaterial bm ) {
        Set<AnnotationValueObject> vos = new HashSet<>();
        for ( Characteristic c : bm.getCharacteristics() ) {
            vos.add( new AnnotationValueObject( c, BioMaterial.class ) );
        }
        return vos;
    }

    @GET
    @Path("/{dataset}/samples/{bioAssayId}/characteristics")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the characteristics (tags) of a sample",
            description = "Returns the direct characteristics held by the sample's biomaterial, including the full "
                    + "Statement shape (predicate / object / second pair) for statement-backed tags. The sample is "
                    + "addressed by its BioAssay id (as in the outlier endpoints); characteristics live on the "
                    + "underlying biomaterial.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset or sample does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<Set<AnnotationValueObject>> getSampleCharacteristics(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("bioAssayId") Long bioAssayId
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        BioMaterial bm = resolveSampleBioMaterial( ee, bioAssayId );
        return respond( sampleAnnotationVos( bm ) );
    }

    @PUT
    @Path("/{dataset}/samples/{bioAssayId}/characteristics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Replace the characteristics (tags) of a sample",
            description = "Idempotent set-replace for the direct characteristics of the sample's biomaterial. The "
                    + "diff is computed by (category, categoryUri, value, valueUri) plus statement awareness; unchanged "
                    + "tags keep their identity, drops are removed, new ones are added with an `IC` evidence code. The "
                    + "sample is addressed by its BioAssay id. A single `ManualAnnotationEvent` is recorded on the "
                    + "owning experiment (sample tag edits surface on the experiment's history). Requires "
                    + "`ACL_SECURABLE_EDIT` on the dataset.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The request body is missing or malformed.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "403", description = "The caller lacks edit permission on the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset or sample does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<Set<AnnotationValueObject>> updateSampleCharacteristics(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("bioAssayId") Long bioAssayId,
            @Nullable AnnotationsUpdateRequest body
    ) {
        if ( body == null || body.getAnnotations() == null ) {
            throw new BadRequestException( "A request body with an 'annotations' field is required (use an empty list to clear)." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        BioMaterial bm = resolveSampleBioMaterial( ee, bioAssayId );
        List<Characteristic> desired = new ArrayList<>( body.getAnnotations().size() );
        for ( AnnotationTagInput tag : body.getAnnotations() ) {
            if ( tag == null ) {
                throw new BadRequestException( "Annotation entries must not be null." );
            }
            if ( StringUtils.isBlank( tag.getCategory() ) ) {
                throw new BadRequestException( "Each annotation must have a non-blank 'category'." );
            }
            if ( StringUtils.isBlank( tag.getValue() ) ) {
                throw new BadRequestException( "Each annotation must have a non-blank 'value'." );
            }
            desired.add( tagToCharacteristic( tag ) );
        }
        bioMaterialService.updateAnnotations( ee, bm, desired );
        return respond( sampleAnnotationVos( resolveSampleBioMaterial( ee, bioAssayId ) ) );
    }

    @POST
    @Path("/{dataset}/samples/{bioAssayId}/characteristics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add a characteristic (tag) to a sample",
            description = "Adds a single characteristic to the sample's biomaterial, accepting the full Statement "
                    + "shape (predicate / object / second pair). A `TagAddedEvent` is recorded on the owning "
                    + "experiment. Returns 409 if a tag with the same (category, value) already exists. Requires "
                    + "`ACL_SECURABLE_EDIT` on the dataset.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The request body is missing or malformed.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "403", description = "The caller lacks edit permission on the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset or sample does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "A tag with the same (category, value) already exists on the sample.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<AnnotationValueObject> addSampleCharacteristic(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("bioAssayId") Long bioAssayId,
            @Nullable AnnotationTagInput body
    ) {
        if ( body == null ) {
            throw new BadRequestException( "A request body describing the tag is required." );
        }
        if ( StringUtils.isBlank( body.getCategory() ) ) {
            throw new BadRequestException( "The annotation must have a non-blank 'category'." );
        }
        if ( StringUtils.isBlank( body.getValue() ) ) {
            throw new BadRequestException( "The annotation must have a non-blank 'value'." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        BioMaterial bm = resolveSampleBioMaterial( ee, bioAssayId );
        Characteristic created;
        try {
            created = bioMaterialService.addAnnotation( ee, bm, tagToCharacteristic( body ) );
        } catch ( IllegalArgumentException e ) {
            // 409 Conflict for duplicate (category, value) — service throws IAE on dup.
            throw new ClientErrorException( e.getMessage(), Response.Status.CONFLICT, e );
        }
        return respond( new AnnotationValueObject( created, BioMaterial.class ) );
    }

    @DELETE
    @Path("/{dataset}/samples/{bioAssayId}/characteristics/{characteristicId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove a characteristic (tag) from a sample",
            description = "Removes the characteristic with the given id from the sample's biomaterial. A "
                    + "`TagRemovedEvent` is recorded on the owning experiment. Returns 404 if the characteristic is "
                    + "not present on the sample. Requires `ACL_SECURABLE_EDIT` on the dataset.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "403", description = "The caller lacks edit permission on the dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset, sample, or characteristic does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<AnnotationValueObject> removeSampleCharacteristic(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("bioAssayId") Long bioAssayId,
            @PathParam("characteristicId") Long characteristicId
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        BioMaterial bm = resolveSampleBioMaterial( ee, bioAssayId );
        Characteristic removed = bioMaterialService.removeAnnotation( ee, bm, characteristicId );
        if ( removed == null ) {
            throw new NotFoundException( "Characteristic " + characteristicId + " is not present on sample (bioAssay " + bioAssayId + ")." );
        }
        return respond( new AnnotationValueObject( removed, BioMaterial.class ) );
    }

    /**
     * Retrieve all available quantitation types for a dataset.
     */
    @GET
    @Path("/{dataset}/quantitationTypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve quantitation types of a dataset")
    public ResponseDataObject<Set<QuantitationTypeValueObject>> getDatasetQuantitationTypes( @PathParam("dataset") DatasetArg<?> datasetArg ) {
        return respond( datasetArgService.getQuantitationTypes( datasetArg ) );
    }

    /**
     * Request body for {@link #setDatasetQuantitationTypePreferred}. {@code preferred} defaults to {@code true}
     * when the body or field is omitted, matching the curation-UI "mark as preferred" button semantics.
     */
    public static class QuantitationTypePreferredRequest {
        @Nullable
        private Boolean preferred;

        @Nullable
        public Boolean getPreferred() {
            return preferred;
        }

        public void setPreferred( @Nullable Boolean preferred ) {
            this.preferred = preferred;
        }
    }

    /**
     * Mark a QuantitationType as the preferred one (within its vector-type bucket) for the given dataset.
     * <p>
     * Curation-UI workflow-step endpoint: the experiment-page "set preferred QT" button calls this. The
     * {@link ExpressionExperimentService#updateQuantitationType} handler takes care of the
     * "unmark every other QT of the same vector type" book-keeping and emits the appropriate
     * {@code PreferredDataChangedEvent}. Body may be omitted (defaults to {@code preferred=true}) or
     * supplied as {@code {"preferred": false}} to clear the flag.
     */
    @PATCH
    @Path("/{dataset}/quantitationTypes/{qtId}/preferred")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Set (or clear) a quantitation type's preferred flag",
            description = "Body: optional `{\"preferred\": true|false}` (defaults to `true`). Marks the named "
                    + "QT as preferred for its vector-type bucket on the given dataset; any other QT that was "
                    + "previously preferred in the same bucket is automatically unmarked. Returns the updated "
                    + "`QuantitationTypeValueObject`.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The QT has no data vectors / no resolvable vector type.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset or quantitation type does not exist (or the QT does not belong to the dataset).",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<QuantitationTypeValueObject> setDatasetQuantitationTypePreferred(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("qtId") Long qtId,
            @Nullable QuantitationTypePreferredRequest body
    ) {
        return doSetDatasetQuantitationTypePreferred( datasetArg, qtId, body );
    }

    /**
     * Request body for {@link #patchDatasetQuantitationType}. Currently understands the
     * {@code isPreferred} (or {@code isPreferred}) field; future patchable fields can be added here.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuantitationTypePatchRequest {
        @Nullable
        @com.fasterxml.jackson.annotation.JsonAlias({ "is_preferred", "isPreferred" })
        private Boolean preferred;

        @Nullable
        public Boolean getPreferred() {
            return preferred;
        }

        public void setPreferred( @Nullable Boolean preferred ) {
            this.preferred = preferred;
        }
    }

    /**
     * Body-driven PATCH dispatcher for a quantitation type. Curation-UI calls
     * {@code PATCH /datasets/{id}/quantitationTypes/{qtId}} with {@code {"isPreferred": true}} instead of
     * routing through the {@code /preferred} suffix; this handler dispatches based on which fields are present.
     */
    @PATCH
    @Path("/{dataset}/quantitationTypes/{qtId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Patch a quantitation type (currently dispatches on `isPreferred`)",
            description = "Curation-UI compatibility shim for body-driven patches. Body: `{\"isPreferred\": true|false}` "
                    + "delegates to the canonical `/preferred` handler. Other patchable fields can be added later.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The patch body is empty or invalid.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset or quantitation type does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<QuantitationTypeValueObject> patchDatasetQuantitationType(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("qtId") Long qtId,
            @Nullable QuantitationTypePatchRequest body
    ) {
        if ( body == null || body.getPreferred() == null ) {
            throw new BadRequestException( "PATCH body must include at least one supported field (currently: `isPreferred`)." );
        }
        QuantitationTypePreferredRequest preferredBody = new QuantitationTypePreferredRequest();
        preferredBody.setPreferred( body.getPreferred() );
        return doSetDatasetQuantitationTypePreferred( datasetArg, qtId, preferredBody );
    }

    private ResponseDataObject<QuantitationTypeValueObject> doSetDatasetQuantitationTypePreferred(
            DatasetArg<?> datasetArg, Long qtId, @Nullable QuantitationTypePreferredRequest body
    ) {
        boolean preferred = body == null || body.getPreferred() == null || body.getPreferred();
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt = quantitationTypeService.loadById( qtId, ee );
        if ( qt == null ) {
            throw new NotFoundException( "Quantitation type " + qtId + " does not belong to dataset " + ee.getShortName() + "." );
        }
        Class<? extends DataVector> vectorType = quantitationTypeService.getDataVectorType( qt );
        if ( vectorType == null ) {
            throw new BadRequestException( "Quantitation type " + qtId + " has no resolvable vector type "
                    + "(likely no data vectors); cannot toggle its preferred flag." );
        }
        // Capture the previous preferred QT in this vector-type bucket so the write service can emit the
        // appropriate audit event (PreferredRawDataChangedEvent / PreferredSingleCellDataChangedEvent).
        QuantitationType previousPreferred = null;
        for ( QuantitationType other : ee.getQuantitationTypes() ) {
            if ( !other.equals( qt ) ) {
                Class<? extends DataVector> otherVt = quantitationTypeService.getDataVectorType( other );
                if ( otherVt != null && otherVt.equals( vectorType ) && other.isPreferred( vectorType ) ) {
                    previousPreferred = other;
                    break;
                }
            }
        }
        qt.setIsPreferred( preferred, vectorType );
        expressionExperimentService.updateQuantitationType( ee, qt, previousPreferred );
        return respond( new QuantitationTypeValueObject( qt, ee, vectorType ) );
    }

    /**
     * Retrieve the single-cell dimension for a given quantitation type.
     */
    @GZIP
    @GET
    @Produces({ MediaType.APPLICATION_JSON, TEXT_TAB_SEPARATED_VALUES_UTF8 })
    @Path("/{dataset}/singleCellDimension")
    @Operation(summary = "Retrieve a single-cell dimension of a single-cell dataset", responses = {
            @ApiResponse(responseCode = "200", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDataObjectSingleCellDimensionValueObject.class)),
                    @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, examples = { @ExampleObject("classpath:/restapidocs/examples/dataset-single-cell-dimension.tsv") })
            })
    })
    public Object getDatasetSingleCellDimension(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("quantitationType") QuantitationTypeArg<?> qtArg,
            @Parameter(description = "Exclude cell IDs from the output") @QueryParam("exclude") ExcludeArg<SingleCellDimensionValueObject> excludeArg,
            @Parameter(description = "Use numerical BioAssay identifier", hidden = true) @QueryParam("useBioAssayId") @DefaultValue("false") Boolean useBioAssayIds,
            @Context HttpHeaders headers
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( qtArg == null ) {
            qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                    .orElseThrow( () -> new NotFoundException( ee.getShortName() + " does not have a preferred single-cell quantitation type." ) );
        } else {
            qt = quantitationTypeArgService.getEntity( qtArg, ee, SingleCellExpressionDataVector.class );
        }
        MediaType negotiate = negotiate( headers, MediaType.APPLICATION_JSON_TYPE, TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE );
        if ( negotiate.equals( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE ) ) {
            if ( excludeArg != null ) {
                throw new BadRequestException( "The 'exclude' query parameter cannot be used with the TSV output." );
            }
            SingleCellDimension dimension = singleCellExpressionExperimentService.getSingleCellDimensionWithCellLevelCharacteristics( ee, qt );
            if ( dimension == null ) {
                throw new NotFoundException( "No single-cell dimension found for " + ee.getShortName() + " and " + qt.getName() + "." );
            }
            return ( StreamingOutput ) output -> {
                CellLevelCharacteristicsWriter writer = new CellLevelCharacteristicsWriter();
                writer.setUseBioAssayIds( useBioAssayIds );
                try ( Writer w = new OutputStreamWriter( output, StandardCharsets.UTF_8 ) ) {
                    writer.write( dimension, w );
                }
            };
        } else {
            SingleCellDimension dimension;
            Set<String> excludedFields;
            if ( excludeArg == null ) {
                excludedFields = Collections.emptySet();
            } else {
                excludedFields = excludeArg.getValue( SCD_ALLOWED_EXCLUDE_FIELDS );
            }
            if ( excludedFields.contains( "cellIds" ) ) {
                boolean includeBioAssays = !excludedFields.contains( "bioAssayIds" );
                // we can go extra-fast if both are excluded
                boolean includeIndices = !( excludedFields.contains( "cellTypeAssignments.cellTypeIds" ) && excludedFields.contains( "cellLevelCharacteristics.characteristicIds" ) );
                SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig initializationConfig = SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig.builder()
                        .includeBioAssays( includeBioAssays )
                        .includeCtas( true )
                        .includeClcs( true )
                        .includeProtocol( true )
                        .includeCharacteristics( true )
                        .includeIndices( includeIndices )
                        .build();
                dimension = singleCellExpressionExperimentService.getSingleCellDimensionWithoutCellIds( ee, qt, initializationConfig );
            } else {
                dimension = singleCellExpressionExperimentService.getSingleCellDimensionWithCellLevelCharacteristics( ee, qt );
            }
            if ( dimension == null ) {
                throw new NotFoundException( "No single-cell dimension found for " + ee.getShortName() + " and " + qt.getName() + "." );
            }
            return respond( new SingleCellDimensionValueObject( dimension, excludedFields.contains( "bioAssayIds" ), excludedFields.contains( "cellTypeAssignments.cellTypeIds" ), excludedFields.contains( "cellLevelCharacteristics.characteristicIds" ) ) );
        }
    }

    @GZIP
    @GET
    @Produces({ MediaType.APPLICATION_JSON, TEXT_TAB_SEPARATED_VALUES_UTF8 })
    @Path("/{dataset}/cellTypeAssignment")
    @Operation(summary = "Retrieve a cell-type assignment of a single-cell dataset", responses = {
            @ApiResponse(responseCode = "200", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDataObjectCellTypeAssignmentValueObject.class)),
                    @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, examples = { @ExampleObject("classpath:/restapidocs/examples/dataset-cell-type-assignment.tsv") })
            }),
            @ApiResponse(responseCode = "404",
                    description = "If the dataset, quantitation type or cell type assignment does not exist, or if a preferred cell type assignment is requested but none is available.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public Object getDatasetCellTypeAssignment(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("quantitationType") QuantitationTypeArg<?> qtArg,
            // TODO: implement CellTypeAssignmentArg
            @Parameter(description = "The name of the cell type assignment to retrieve. If left unset, this the preferred one is returned.") @QueryParam("cellTypeAssignment") String ctaName,
            @Parameter(description = "The protocol of the cell type assignment to retrieve. This cannot be used in combination with `cellTypeAssignment`.") @QueryParam("protocol") String protocolName,
            @Parameter(description = "Use numerical BioAssay identifier", hidden = true) @QueryParam("useBioAssayId") @DefaultValue("false") Boolean useBioAssayId,
            @Context HttpHeaders headers
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( qtArg == null ) {
            qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                    .orElseThrow( () -> new NotFoundException( ee.getShortName() + " does not have a preferred single-cell quantitation type." ) );
        } else {
            qt = quantitationTypeArgService.getEntity( qtArg, ee, SingleCellExpressionDataVector.class );
        }
        SingleCellDimension dimension = singleCellExpressionExperimentService.getSingleCellDimension( ee, qt );
        if ( dimension == null ) {
            throw new NotFoundException( "No single-cell dimension found for " + ee.getShortName() + " and " + qt.getName() + "." );
        }
        CellTypeAssignment cta;
        if ( protocolName != null ) {
            Collection<CellTypeAssignment> found = singleCellExpressionExperimentService.getCellTypeAssignmentByProtocol( ee, qt, protocolName );
            if ( found.isEmpty() ) {
                throw new NotFoundException( "No cell type assignment with protocol " + protocolName + " found for " + ee.getShortName() + " and " + qt.getName() + "." );
            } else if ( found.size() > 1 ) {
                throw new IllegalArgumentException( "There is more than one cell type assignment with protocol " + protocolName + " for " + ee.getShortName() + " and " + qt.getName() + ". Please specify the name of the cell type assignment to retrieve." );
            } else {
                cta = found.iterator().next();
            }
        } else if ( ctaName != null ) {
            cta = singleCellExpressionExperimentService.getCellTypeAssignment( ee, qt, ctaName );
            if ( cta == null ) {
                throw new NotFoundException( "No cell type assignment with name " + ctaName + " found for " + ee.getShortName() + " and " + qt.getName() + "." );
            }
        } else {
            cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt )
                    .orElseThrow( () -> new NotFoundException( "No preferred cell type assignment found for " + ee.getShortName() + " and " + qt.getName() + "." ) );
        }
        MediaType negotiate = negotiate( headers, MediaType.APPLICATION_JSON_TYPE, TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE );
        if ( negotiate.equals( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE ) ) {
            return ( StreamingOutput ) output -> {
                try ( Writer w = new OutputStreamWriter( output, StandardCharsets.UTF_8 ) ) {
                    CellLevelCharacteristicsWriter writer = new CellLevelCharacteristicsWriter();
                    writer.setUseBioAssayIds( useBioAssayId );
                    writer.write( cta, dimension, w );
                }
            };
        } else {
            return respond( new CellTypeAssignmentValueObject( cta, false ) );
        }
    }

    @GZIP
    @GET
    @Produces({ MediaType.APPLICATION_JSON, TEXT_TAB_SEPARATED_VALUES_UTF8 })
    @Path("/{dataset}/cellLevelCharacteristics")
    @Operation(summary = "Retrieve all other cell-level characteristics of a single-cell dataset", responses = {
            @ApiResponse(responseCode = "200", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDataObjectListCellLevelCharacteristicsValueObject.class)),
                    @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, examples = { @ExampleObject("classpath:/restapidocs/examples/dataset-cell-level-characteristics.tsv") })
            })
    })
    public Object getDatasetCellLevelCharacteristics(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("quantitationType") QuantitationTypeArg<?> qtArg,
            @Context HttpHeaders headers
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( qtArg == null ) {
            qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                    .orElseThrow( () -> new NotFoundException( ee.getShortName() + " does not have a preferred single-cell quantitation type." ) );
        } else {
            qt = quantitationTypeArgService.getEntity( qtArg, ee, SingleCellExpressionDataVector.class );
        }
        SingleCellDimension dimension = singleCellExpressionExperimentService.getSingleCellDimensionWithCellLevelCharacteristics( ee, qt );
        if ( dimension == null ) {
            throw new NotFoundException( "No single-cell dimension found for " + ee.getShortName() + " and " + qt.getName() + "." );
        }
        MediaType negotiate = negotiate( headers, MediaType.APPLICATION_JSON_TYPE, TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE );
        if ( negotiate.equals( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE ) ) {
            return ( StreamingOutput ) output -> {
                try ( Writer w = new OutputStreamWriter( output, StandardCharsets.UTF_8 ) ) {
                    CellLevelCharacteristicsWriter writer = new CellLevelCharacteristicsWriter();
                    writer.write( dimension.getCellLevelCharacteristics(), dimension, w );
                }
            };
        } else {
            return respond( dimension.getCellLevelCharacteristics().stream()
                    .map( clc -> new CellLevelCharacteristicsValueObject( clc, false ) )
                    .collect( Collectors.toList() ) );
        }
    }

    private static final String DATA_TSV_OUTPUT_DESCRIPTION = "The following columns are available: Probe, Sequence, GeneSymbol, GeneName, GemmaId, NCBIid followed by one column per sample. GeneSymbol, GeneName, GemmaId and NCBIid are optional.";

    /**
     * Retrieves the data for the given dataset.
     * <p>
     * The returned TSV format contains the following columns:
     *
     * <ul>
     *     <li>Probe</li>
     *     <li>Sequence</li>
     *     <li>GeneSymbol (optional)</li>
     *     <li>GeneName (optional)</li>
     *     <li>GemmaId (optional)</li>
     *     <li>NCBIid (optional)</li>
     * </ul>
     *
     * followed by one column per sample.
     * <p>
     * <b>Note:</b> Additional gene information is only available if the corresponding platform's annotations has been dumped
     * on-disk.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     * @param filterData return filtered the expression data.
     */
    @GZIP(mediaTypes = TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed = true)
    @GET
    @Path("/{dataset}/data")
    @Produces(TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve processed expression data of a dataset",
            description = "This endpoint is deprecated and getDatasetProcessedExpression() should be used instead. " + DATA_TSV_OUTPUT_DESCRIPTION,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8,
                            schema = @Schema(type = "string"),
                            examples = { @ExampleObject("classpath:/restapidocs/examples/dataset-data.tsv") })),
                    @ApiResponse(responseCode = "204", description = "The dataset expression matrix is empty."),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) },
            deprecated = true)
    public Response getDatasetExpression( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("filter") @DefaultValue("false") Boolean filterData, // Optional, default false
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download,
            @Parameter(hidden = true) @QueryParam("force") @DefaultValue("false") Boolean force
    ) {
        return getDatasetProcessedExpression( datasetArg, filterData, download, force );
    }

    /**
     * Retrieve processed expression data.
     * <p>
     * The payload is transparently compressed via a <code>Content-Encoding</code> header and streamed to avoid dumping
     * the whole payload in memory.
     */
    @GZIP(mediaTypes = TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed = true)
    @GET
    @Path("/{dataset}/data/processed")
    @Produces(TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve processed expression data of a dataset",
            description = DATA_TSV_OUTPUT_DESCRIPTION,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8,
                            schema = @Schema(type = "string"),
                            examples = { @ExampleObject("classpath:/restapidocs/examples/dataset-processed-data.tsv") })),
                    @ApiResponse(responseCode = "204", description = "The dataset expression matrix is empty. Only applicable if filter is set to true."),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetProcessedExpression(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("filter") @DefaultValue("false") Boolean filtered,
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download,
            @Parameter(hidden = true) @QueryParam("force") @DefaultValue("false") Boolean force
    ) {
        if ( force ) {
            checkIsAdmin();
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( !expressionExperimentService.hasProcessedExpressionData( ee ) ) {
            throw new NotFoundException( ee.getShortName() + " does not have any processed vectors." );
        }
        // Async-build pattern (mirrors the single-cell /data/singleCell endpoint): short-timeout cache probe via
        // getDataFile + sendfile if hot; otherwise kick the matrix build onto expressionDataFileTaskExecutor and
        // stream the data in-band so the caller doesn't block on the 30-120s cold matrix-build TTFB. Force-rewrite
        // skips the cache probe so the admin path stays deterministic.
        if ( !force ) {
            try ( LockedPath p = expressionDataFileService.getDataFile( getDataOutputFilename( ee, filtered, TABULAR_BULK_DATA_FILE_SUFFIX ), false, 5, TimeUnit.SECONDS ) ) {
                if ( Files.exists( p.getPath() ) ) {
                    String filename = download ? p.getPath().getFileName().toString() : FilenameUtils.removeExtension( p.getPath().getFileName().toString() );
                    return sendfile( p.getPath() )
                            .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                            .header( "Content-Disposition", "attachment; filename=\"" + filename + "\"" )
                            .build();
                }
            } catch ( TimeoutException e ) {
                // file is locked by another writer — fall through to stream
                log.warn( "Processed data for " + ee.getShortName() + " is locked, will stream instead." );
            } catch ( IOException e ) {
                log.error( "Failed to probe processed data cache for " + ee + ", will stream instead.", e );
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new InternalServerErrorException( e );
            }
        }
        if ( !expressionExperimentService.hasProcessedExpressionData( ee ) ) {
            // re-check defensively after the cache probe (cheap)
            throw new NotFoundException( ee.getShortName() + " does not have any processed vectors." );
        }
        // One build, two consumers: stream in-band so the caller doesn't block on the full matrix build,
        // and populate the cache file for the next caller from the SAME pass. This replaces the
        // fire-and-forget executor build that raced the in-band stream and did the whole matrix —
        // vector fetch, platform thaw, annotation read — twice per cold request (2026-08-19 baseline:
        // both builds visible in the DAO thaw warnings, two seconds apart). A caller that disconnects
        // mid-stream does not abort the cache build, so the cold path still heals behind an impatient
        // client; a concurrent builder degrades this to a plain stream, as before.
        String filename = download ? getDataOutputFilename( ee, filtered, TABULAR_BULK_DATA_FILE_SUFFIX ) : FilenameUtils.removeExtension( getDataOutputFilename( ee, filtered, TABULAR_BULK_DATA_FILE_SUFFIX ) );
        return Response.ok( ( StreamingOutput ) output -> {
                    try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( output ), StandardCharsets.UTF_8 ) ) {
                        expressionDataFileService.streamAndWriteProcessedExpressionData( ee, filtered, force,
                                writer, true );
                    } catch ( NoDesignElementsException ex ) {
                        // streaming has already started; we cannot downgrade to 204, just truncate the body
                        log.warn( "Processed data for " + ee + " is empty after filtering; truncating stream.", ex );
                    } catch ( FilteringException ex ) {
                        // this is a bit unfortunate, because it's too late for producing an error response
                        throw new RuntimeException( ex );
                    }
                } )
                .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .header( "Content-Disposition", "attachment; filename=\"" + filename + "\"" )
                .build();
    }

    /**
     * Retrieve raw expression data.
     * <p>
     * The payload is transparently compressed via a <code>Content-Encoding</code> header and streamed to avoid dumping
     * the whole payload in memory.
     */
    @GZIP(mediaTypes = TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed = true)
    @GET
    @Path("/{dataset}/data/raw")
    @Produces(TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve raw expression data of a dataset",
            description = DATA_TSV_OUTPUT_DESCRIPTION,
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8,
                            schema = @Schema(type = "string"),
                            examples = { @ExampleObject("classpath:/restapidocs/examples/dataset-raw-data.tsv") })),
                    @ApiResponse(responseCode = "404", description = "Either the dataset or the quantitation type do not exist.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetRawExpression(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg,
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download,
            @Parameter(hidden = true) @QueryParam("force") @DefaultValue("false") Boolean force
    ) {
        if ( force ) {
            checkIsAdmin();
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( quantitationTypeArg != null ) {
            qt = quantitationTypeArgService.getEntity( quantitationTypeArg, ee, RawExpressionDataVector.class );
        } else {
            qt = expressionExperimentService.getPreferredQuantitationType( ee )
                    .orElseThrow( () -> new NotFoundException( String.format( "No preferred quantitation type could be found for raw expression data data of %s.", ee ) ) );
        }
        // Async-build pattern (mirrors /data/singleCell): short-timeout cache probe + sendfile if hot; otherwise
        // fire-and-forget the disk write onto expressionDataFileTaskExecutor and stream the data in-band so the
        // caller doesn't block on the matrix build.
        if ( !force ) {
            try ( LockedPath p = expressionDataFileService.getDataFile( getDataOutputFilename( ee, qt, TABULAR_BULK_DATA_FILE_SUFFIX ), false, 5, TimeUnit.SECONDS ) ) {
                if ( Files.exists( p.getPath() ) ) {
                    String filename = download ? p.getPath().getFileName().toString() : FilenameUtils.removeExtension( p.getPath().getFileName().toString() );
                    return sendfile( p.getPath() )
                            .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                            .header( "Content-Disposition", "attachment; filename=\"" + filename + "\"" )
                            .build();
                }
            } catch ( TimeoutException e ) {
                log.warn( "Raw data for " + qt + " is locked, will stream instead." );
            } catch ( IOException e ) {
                log.error( "Failed to probe raw data cache for " + qt + ", will stream instead.", e );
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new InternalServerErrorException( e );
            }
        }
        // One build, two consumers — same tee as the processed endpoint above: the in-band stream and
        // the cache file are fed from a single pass instead of racing two full builds per cold request.
        String filename = getDataOutputFilename( ee, qt, TABULAR_BULK_DATA_FILE_SUFFIX );
        return Response.ok( ( StreamingOutput ) output -> {
                    try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( output ), StandardCharsets.UTF_8 ) ) {
                        expressionDataFileService.streamAndWriteRawExpressionData( ee, qt, force, writer, true );
                    }
                } )
                .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .header( "Content-Disposition", "attachment; filename=\"" + ( download ? filename : FilenameUtils.removeExtension( filename ) ) + "\"" )
                .build();
    }

    /**
     * Retrieve the differential-expression analysis archive for a dataset.
     * <p>
     * Builds (or, on cache hit, locates) the ZIP archive containing the analysis result + per-result-set contrast
     * files for a single differential-expression analysis on this dataset, and sendfile-s the cached file directly.
     * <p>
     * The archive is generated lazily on first access by {@link ExpressionDataFileService#writeOrLocateDiffExAnalysisArchiveFile};
     * subsequent accesses skip the rebuild.
     * <p>
     * If the dataset has more than one differential-expression analysis, the caller must disambiguate by passing
     * {@code analysisId}; otherwise the response is 409 Conflict.
     */
    @GET
    @Path("/{dataset}/data/dea")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Retrieve the differential expression analysis archive of a dataset",
            description = "Returns a ZIP archive (one per analysis) containing the analysis result and per-result-set "
                    + "contrast files. The archive is cached on disk under <dataDir> and rebuilt on first access; "
                    + "for datasets with multiple differential-expression analyses, pass `analysisId` to select one.",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM,
                            schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not have any differential-expression analyses, or the supplied analysis ID does not belong to this dataset.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "The dataset has more than one differential-expression analysis; pass `analysisId` to select one.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetDiffExAnalysisArchive(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Identifier of the differential-expression analysis to retrieve. Required when the dataset has more than one analysis.") @QueryParam("analysisId") Long analysisId,
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download,
            @Parameter(hidden = true) @QueryParam("force") @DefaultValue("false") Boolean force
    ) {
        if ( force ) {
            checkIsAdmin();
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        DifferentialExpressionAnalysis analysis;
        if ( analysisId != null ) {
            analysis = differentialExpressionAnalysisService.findByExperimentAndAnalysisId( ee, true, analysisId );
            if ( analysis == null ) {
                throw new NotFoundException( "No differential-expression analysis with ID " + analysisId + " was found for " + ee.getShortName() + "." );
            }
        } else {
            Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService.findByExperiment( ee, true );
            if ( analyses.isEmpty() ) {
                throw new NotFoundException( ee.getShortName() + " does not have any differential-expression analyses." );
            }
            if ( analyses.size() > 1 ) {
                throw new ClientErrorException( ee.getShortName() + " has " + analyses.size() + " differential-expression analyses; pass ?analysisId= to select one.", Response.Status.CONFLICT );
            }
            analysis = analyses.iterator().next();
        }
        try ( LockedPath p = expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFile( analysis, force ) ) {
            String filename = p.getPath().getFileName().toString();
            return sendfile( p.getPath() )
                    .type( MediaType.APPLICATION_OCTET_STREAM_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + filename + "\"" )
                    .build();
        } catch ( IOException e ) {
            log.error( "Failed to locate or create the DEA archive for " + analysis + ".", e );
            throw new InternalServerErrorException( e );
        }
    }

    @GZIP(mediaTypes = TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed = true)
    @GET
    @Path("/{dataset}/data/singleCell")
    @Produces({ APPLICATION_10X_MEX, TEXT_TAB_SEPARATED_VALUES_UTF8 + ";q=0.9" })
    @Operation(summary = "Retrieve single-cell expression data of a dataset",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = {
                                    @Content(mediaType = APPLICATION_10X_MEX, schema = @Schema(description = "Sample files are bundled in a TAR archive according to the 10x MEX format.", type = "string", format = "binary", externalDocs = @ExternalDocumentation(url = "https://www.10xgenomics.com/support/software/cell-ranger/latest/analysis/outputs/cr-outputs-mex-matrices")),
                                            examples = { @ExampleObject("classpath:/restapidocs/examples/dataset-single-cell-data.mex") }),
                                    @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8 + "; q=0.9", schema = @Schema(type = "string"),
                                            examples = { @ExampleObject("classpath:/restapidocs/examples/dataset-single-cell-data.tsv") })
                            }),
                    @ApiResponse(responseCode = "404", description = "Either the dataset or the quantitation type do not exist.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetSingleCellExpression(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg,
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download,
            @Parameter(hidden = true) @QueryParam("force") @DefaultValue("false") Boolean force,
            @Context HttpHeaders headers
    ) {
        if ( force ) {
            checkIsAdmin();
        }
        MediaType mediaType = negotiate( headers, APPLICATION_10X_MEX_TYPE, withQuality( TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE, 0.9 ) );
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( quantitationTypeArg != null ) {
            qt = quantitationTypeArgService.getEntity( quantitationTypeArg, ee, SingleCellExpressionDataVector.class );
        } else {
            qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                    .orElseThrow( () -> new NotFoundException( "No preferred single-cell quantitation type could be found for " + ee + "." ) );
        }
        if ( mediaType.equals( APPLICATION_10X_MEX_TYPE ) ) {
            try ( LockedPath p = expressionDataFileService.getDataFile( ee, qt, ExpressionExperimentDataFileType.MEX, false, 5, TimeUnit.SECONDS ) ) {
                if ( Files.exists( p.getPath() ) ) {
                    return Response.ok( p.getPath() )
                            .type( APPLICATION_10X_MEX_TYPE )
                            .header( "Content-Disposition", "attachment; filename=\"" + p.getPath().getFileName() + ".tar\"" )
                            .build();
                } else {
                    // no cursor fetching because this requires a lot of memory on the database server
                    expressionDataFileService.writeOrLocateMexSingleCellExpressionDataAsync( ee, qt, 30, false, false );
                    throw new ServiceUnavailableException( "MEX single-cell data for " + qt + " is still being generated.", 30L );
                }
            } catch ( TimeoutException e ) {
                throw new ServiceUnavailableException( "MEX single-cell data for " + qt + " is still being generated.", 30L, e );
            } catch ( RejectedExecutionException e ) {
                throw new ServiceUnavailableException( "Too many file generation tasks are being processed at this time.", 30L, e );
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new InternalServerErrorException( e );
            } catch ( IOException e ) {
                throw new InternalServerErrorException( e );
            }
        } else {
            try ( LockedPath p = expressionDataFileService.getDataFile( ee, qt, ExpressionExperimentDataFileType.TABULAR, false, 5, TimeUnit.SECONDS ) ) {
                if ( !force && Files.exists( p.getPath() ) ) {
                    return Response.ok( p.getPath() )
                            .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                            .header( "Content-Disposition", "attachment; filename=\"" + ( download ? p.getPath().getFileName().toString() : FilenameUtils.removeExtension( p.getPath().getFileName().toString() ) ) + "\"" )
                            .build();
                } else {
                    // generate the file in the background and stream it
                    // TODO: limit the number of threads writing SC data to disk to not overwhelm the short-lived task pool
                    log.info( "Single-cell data for " + qt + " is not available, will generate it in the background and stream it in the meantime." );
                    // we do not want to use cursor fetch because it requires a lot of memory on the database server
                    expressionDataFileService.writeOrLocateTabularSingleCellExpressionDataAsync( ee, qt, 30, false, force );
                    return streamTabularDatasetSingleCellExpression( ee, qt, download );
                }
            } catch ( TimeoutException e ) {
                // file is being written, recommend to the user to wait a little bit, stacktrace is superfluous
                log.warn( "Single-cell data for " + qt + " is still being generated, it will be streamed in the meantime." );
                return streamTabularDatasetSingleCellExpression( ee, qt, download );
            } catch ( RejectedExecutionException e ) {
                log.warn( "Too many file generation tasks are being executed, will stream the single-cell data instead.", e );
                return streamTabularDatasetSingleCellExpression( ee, qt, download );
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new InternalServerErrorException( e );
            } catch ( IOException e ) {
                throw new InternalServerErrorException( e );
            }
        }
    }

    private Response streamTabularDatasetSingleCellExpression( ExpressionExperiment ee, QuantitationType qt, Boolean download ) {
        String filename = getDataOutputFilename( ee, qt, TABULAR_SC_DATA_SUFFIX );
        return Response.ok( ( StreamingOutput ) stream -> {
                    try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( stream ), StandardCharsets.UTF_8 ) ) {
                        expressionDataFileService.writeTabularSingleCellExpressionData( ee, qt, null, false, false, 30, false, writer, true, null );
                    }
                } )
                .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .header( "Content-Disposition", "attachment; filename=\"" + ( download ? filename : FilenameUtils.removeExtension( filename ) ) + "\"" )
                .build();
    }

    /**
     * Retrieves the structured experimental design for the given dataset as JSON.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234).
     */
    @GET
    @Path("/{dataset}/design")
    @Produces(MediaType.APPLICATION_JSON)
    // The @Operation annotation is intentionally identical to the one on getDatasetDesign() below. The two
    // JAX-RS methods collapse to a single OpenAPI operation under (GET, /{dataset}/design); duplicating the
    // annotation makes the merge result deterministic regardless of reflection order, and keeps the JSON
    // return type visible so swagger auto-registers the ResponseDataObjectExperimentalDesignValueObject schema.
    @Operation(summary = "Retrieve the design of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = {
                    @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, schema = @Schema(type = "string"),
                            examples = @ExampleObject("classpath:/restapidocs/examples/dataset-design.tsv")),
                    @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(ref = "ResponseDataObjectExperimentalDesignValueObject"))
            }),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<ExperimentalDesignValueObject> getDatasetDesignJson(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return respond( datasetArgService.getExperimentalDesign( datasetArg ) );
    }

    /**
     * Dry-run preflight for a proposed design replacement.
     * <p>
     * Accepts the same JSON shape that {@code GET /datasets/{id}/design} returns ({@link ExperimentalDesignValueObject}),
     * with {@code id} fields treated as identity claims (existing entity) or {@code null} (new entity), and returns
     * a {@link DesignPreflightReport} describing validation errors and the impact a real PUT would have.
     * <p>
     * The preflight never mutates state. POST is used (not GET) because the response depends on a non-trivial
     * request body.
     */
    @POST
    @Path("/{dataset}/designPreflight")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Dry-run preflight for a proposed experimental design replacement", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectDesignPreflightReport"))),
            @ApiResponse(responseCode = "400", description = "Request body is missing or malformed.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<DesignPreflightReport> previewDatasetDesignChange(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            ExperimentalDesignValueObject proposed
    ) {
        return respond( datasetArgService.previewDesignChange( datasetArg, proposed ) );
    }

    /**
     * Apply a proposed {@link ExperimentalDesignValueObject} as the experiment's new design.
     * <p>
     * The same validation pass performed by {@code POST /datasets/{id}/designPreflight} is re-run server-side. If
     * blockers are present, returns 400 with a {@link DesignPreflightReport} payload — fix the body and retry.
     * If the change carries consequences needing consent — it would delete differential-expression analyses, or
     * leave a subset anchored on factor values that no longer exist — and {@code force=false}, returns 409 with
     * the report; admins may re-issue with {@code ?force=true}. The 409 body is the report itself, so the client
     * can show the curator exactly which analyses and which subsets they are agreeing to.
     * On success, returns 200 with the freshly-rebuilt design.
     */
    @PUT
    @Path("/{dataset}/design")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Replace the experimental design of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectExperimentalDesignValueObject"))),
            @ApiResponse(responseCode = "400", description = "The proposed design has validation blockers; see the report in the response body.",
                    content = @Content(schema = @Schema(ref = "ResponseDataObjectDesignPreflightReport"))),
            @ApiResponse(responseCode = "409", description = "The proposed change would delete differential-expression analyses, or strand a subset on deleted factor values; retry with ?force=true to consent.",
                    content = @Content(schema = @Schema(ref = "ResponseDataObjectDesignPreflightReport"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response replaceDatasetDesign(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Set to true to consent to the change's consequences: deleting differential-expression analyses that depend on affected factors or factor values, and leaving subsets anchored on factor values that would no longer exist.") @QueryParam("force") @DefaultValue("false") Boolean force,
            @Parameter(description = "Optional id of the PROPOSAL annotation set driving this apply. On success the apply is recorded as a COMMIT annotation set carrying that proposal's run reference and parented to it, so the trail reads proposal -> decision -> effect. The set must belong to this dataset and must be a PROPOSAL.") @QueryParam("agentProposalId") @Nullable Long agentProposalId,
            ExperimentalDesignValueObject proposed
    ) {
        // The AgentProposal entity this parameter was written for never landed under that name — AnnotationSet is
        // it. So the proposal is validated up front, before anything is applied: naming a set that does not exist,
        // belongs to another dataset, or is not a PROPOSAL is a client bug, and finding out after the design has
        // been rewritten helps nobody.
        AnnotationSet proposal = agentProposalId != null
                ? requireProposalFor( agentProposalId, datasetArgService.getEntity( datasetArg ) )
                : null;

        ubic.gemma.rest.util.args.DatasetArgService.DesignChangeResult result =
                datasetArgService.applyDesignChange( datasetArg, proposed, force );
        if ( result.blockingReport != null ) {
            Response.Status status = result.forceRequired ? Response.Status.CONFLICT : Response.Status.BAD_REQUEST;
            return Response.status( status ).entity( respond( result.blockingReport ) ).build();
        }
        // Record WHICH run applied this, now that it has. Unlike the composite commit — which mints its COMMIT row
        // inside its own transaction — the design apply's transaction closed in applyDesignChange, so the row is
        // written after. The asymmetry only costs the benign direction: a failure here loses the provenance record
        // of an apply that really happened, and can never leave a row claiming an apply that rolled back.
        if ( proposal != null ) {
            recordAppliedFromProposal( proposal );
        }
        return Response.ok( respond( result.updated ) ).build();
    }

    /**
     * Retrieves the design for the given dataset.
     * <p>
     * Two response media types are supported on this path, selected via the {@code Accept} header:
     * <ul>
     *     <li>{@code application/json} (default) — a structured {@link ExperimentalDesignValueObject} with
     *         factors, factor values (statements with stable IDs), and biomaterial-to-factor-value
     *         assignments. The JSON variant ignores the {@code quantitationType}/
     *         {@code useProcessedQuantitationType} parameters.</li>
     *     <li>{@code text/tab-separated-values; charset=UTF-8} — the design matrix as TSV, served only when
     *         requested explicitly via {@code Accept}.</li>
     * </ul>
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GZIP(mediaTypes = TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed = true)
    @GET
    @Path("/{dataset}/design")
    // lowering qs sets json to default
    @Produces(TEXT_TAB_SEPARATED_VALUES_UTF8 + ";qs=0.9")
    @Operation(summary = "Retrieve the design of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = {
                    @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, schema = @Schema(type = "string"),
                            examples = @ExampleObject("classpath:/restapidocs/examples/dataset-design.tsv")),
                    @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(ref = "ResponseDataObjectExperimentalDesignValueObject"))
            }),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetDesign( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @Parameter(description = "Quantitation type to produce the experimental design for. This only works for raw data vectors. The default is to produce the design for the experiment.") @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg,
            @Parameter(description = "Produce an experimental design compatible with the preferred data vectors. The default is to produce the design for the experiment.") @QueryParam("useProcessedQuantitationType") @DefaultValue("false") Boolean useProcessedQuantitationType, // Optional, default false
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download,
            @Parameter(hidden = true) @QueryParam("force") @DefaultValue("false") Boolean force
    ) {
        if ( quantitationTypeArg != null && useProcessedQuantitationType ) {
            throw new BadRequestException( "Cannot use both 'quantitationType' and 'useProcessedQuantitationType' parameters together." );
        }
        if ( force ) {
            checkIsAdmin();
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( quantitationTypeArg != null ) {
            QuantitationType qt = quantitationTypeArgService.getEntity( quantitationTypeArg, ee, RawExpressionDataVector.class );
            // Empty-EE guard: writeDesignMatrix throws IllegalStateException (→ 500) when the EE has
            // no ExperimentalDesign or no factors. The plain-TSV branch below produces 404 in that
            // shape via writeOrLocateDesignFile.Optional.empty(); mirror that here so a half-imported
            // / direct-upload EE returns 404 rather than 500.
            if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
                throw new NotFoundException( ee.getShortName() + " does not have an experimental design." );
            }
            String filename = getDesignFileName( ee, qt );
            return Response.ok( ( StreamingOutput ) stream -> {
                        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( stream ), StandardCharsets.UTF_8 ) ) {
                            expressionDataFileService.writeDesignMatrix( ee, qt, RawExpressionDataVector.class, writer, false );
                        }
                    } )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + ( download ? filename : FilenameUtils.removeExtension( filename ) ) + "\"" )
                    .build();
        }
        try ( LockedPath file = expressionDataFileService.writeOrLocateDesignFile( ee, useProcessedQuantitationType, force, 5, TimeUnit.SECONDS )
                .orElseThrow( () -> new NotFoundException( ee.getShortName() + " does not have an experimental design." ) ) ) {
            String filename = file.getPath().getFileName().toString();
            return sendfile( file.getPath() )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + ( download ? filename : FilenameUtils.removeExtension( filename ) ) + "\"" )
                    .build();
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( "Experimental design for " + ee.getShortName() + " is still being generated.", 30L, e );
        } catch ( IOException e ) {
            log.error( "Failed to write design for " + ee + " to disk, will resort to stream it.", e );
            String filename = getDesignFileName( ee, useProcessedQuantitationType );
            return Response.ok( ( StreamingOutput ) stream -> {
                        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( stream ), StandardCharsets.UTF_8 ) ) {
                            expressionDataFileService.writeDesignMatrix( ee, useProcessedQuantitationType, writer, false );
                        }
                    } )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + ( download ? filename : FilenameUtils.removeExtension( filename ) ) + "\"" )
                    .build();
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * List the preprocessing-metadata files available for a dataset.
     * <p>
     * Entries reflect {@link ExpressionExperimentMetaFileType} instances whose underlying file
     * exists on disk for the given experiment. The {@code MULTIQC_REPORT} alias is suppressed
     * (it duplicates {@code RNASEQ_PIPELINE_REPORT} with the same id).
     */
    @GET
    @Path("/{dataset}/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List the preprocessing-metadata files available for a dataset",
            description = "Returns one entry per metadata file present on disk. Each entry exposes "
                    + "the type identifier (used to fetch the file via /datasets/{id}/metadata/{type}), "
                    + "a human-readable display name, the download filename, the MIME content-type, "
                    + "and a flag indicating whether the metadata is organized as a directory.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<DatasetMetadataFileValueObject>> getDatasetMetadataFiles( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        List<DatasetMetadataFileValueObject> entries = new ArrayList<>();
        for ( ExpressionExperimentMetaFileType type : ExpressionExperimentMetaFileType.values() ) {
            if ( type == ExpressionExperimentMetaFileType.MULTIQC_REPORT ) {
                // deprecated alias for RNASEQ_PIPELINE_REPORT; same id, would double-list.
                continue;
            }
            try ( LockedPath probe = expressionDataFileService.getMetadataFile( ee, type, false ).orElse( null ) ) {
                if ( probe == null ) {
                    continue;
                }
                if ( !Files.isReadable( probe.getPath() ) ) {
                    continue;
                }
                entries.add( new DatasetMetadataFileValueObject( type, ee ) );
            } catch ( IOException e ) {
                log.warn( "Failed to probe metadata file " + type + " for " + ee.getShortName(), e );
            }
        }
        return respond( entries );
    }

    /**
     * Stream a single preprocessing-metadata file by type for a dataset.
     * <p>
     * Returns 404 if the file is absent on disk or the type is a directory with no contents.
     * Successful responses use the file's native MIME type from {@link ExpressionExperimentMetaFileType#getContentType()}.
     */
    @GET
    @Path("/{dataset}/metadata/{type}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Retrieve a single preprocessing-metadata file by type",
            description = "Streams the metadata file. The {type} path parameter is the "
                    + "ExpressionExperimentMetaFileType enum name (e.g. BASE_METADATA, ALIGNMENT_METADATA, "
                    + "RNASEQ_PIPELINE_REPORT). Use GET /datasets/{id}/metadata to discover available types. "
                    + "The response Content-Type reflects the type's native MIME (e.g. text/plain, "
                    + "application/json, text/html); pass ?download=true to force application/octet-stream.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "400", description = "The metadata type is not a recognised enum value, or it refers to a directory-organised metadata.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist or has no metadata of the requested type.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetMetadataFile( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @PathParam("type") String typeArg, // Required
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download
    ) {
        ExpressionExperimentMetaFileType type;
        try {
            type = ExpressionExperimentMetaFileType.valueOf( typeArg );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown metadata file type '" + typeArg + "'." );
        }
        if ( type == ExpressionExperimentMetaFileType.MULTIQC_REPORT ) {
            // Deprecated alias; redirect callers to the canonical name to avoid drift.
            throw new BadRequestException( "MULTIQC_REPORT is a deprecated alias; use RNASEQ_PIPELINE_REPORT instead." );
        }
        if ( type.isDirectory() ) {
            // The legacy DWR controller streamed individual files inside the directory by separate
            // type ids (e.g. RNASEQ_PIPELINE_REPORT_DATA). Mirror that here: no bare directory download.
            throw new BadRequestException( "Metadata type " + type + " is a directory; request a specific contained file type instead." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        try ( LockedPath file = expressionDataFileService.getMetadataFile( ee, type, false )
                .orElseThrow( () -> new NotFoundException( ee.getShortName() + " does not have metadata of type " + type + "." ) ) ) {
            if ( !Files.isReadable( file.getPath() ) ) {
                throw new NotFoundException( ee.getShortName() + " does not have metadata of type " + type + "." );
            }
            String downloadName = type.getDownloadName( ee );
            return sendfile( file.getPath() )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : MediaType.valueOf( type.getContentType() ) )
                    .header( "Content-Disposition", ( type.isMultiQC() ? "inline" : "attachment" ) + "; filename=\"" + downloadName + "\"" )
                    .build();
        } catch ( IOException e ) {
            log.error( "Failed to stream metadata file " + type + " for " + ee, e );
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * Wire shape for {@link #getDatasetMetadataFiles}: one entry per available metadata file.
     */
    @Value
    public static class DatasetMetadataFileValueObject {

        /**
         * Enum name of the metadata file type — use this value as the {@code {type}} path
         * parameter on {@code GET /datasets/{id}/metadata/{type}} to fetch the file.
         */
        String type;

        /**
         * Human-readable label suitable for UI display.
         */
        String displayName;

        /**
         * Filename the server will use in the {@code Content-Disposition} header on download.
         */
        String downloadName;

        /**
         * MIME content type the server will serve the file with.
         */
        String contentType;

        /**
         * True when the underlying metadata is organised as a directory; in that case the file
         * cannot be downloaded directly via {@code /metadata/{type}} — request a specific
         * sub-type (e.g. {@code RNASEQ_PIPELINE_REPORT_DATA}) instead.
         */
        boolean directory;

        public DatasetMetadataFileValueObject( ExpressionExperimentMetaFileType type, ExpressionExperiment ee ) {
            this.type = type.name();
            this.displayName = type.getDisplayName();
            this.downloadName = type.getDownloadName( ee );
            this.contentType = type.getContentType();
            this.directory = type.isDirectory();
        }
    }

    /**
     * Indicate if the experiment has batch information.
     * <p>
     * This does not imply that the batch information is usable. This will be true even if there is only one batch. It
     * does not reflect the presence or absence of a batch effect.
     */
    @GET
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Path("/{dataset}/hasbatch")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Indicate of a dataset has batch information", hidden = true)
    public ResponseDataObject<Boolean> getDatasetHasBatchInformation( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        return respond( expressionExperimentBatchInformationService.checkHasBatchInfo( ee ) );
    }

    @GET
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dataset}/batchInformation")
    @Operation(summary = "Retrieve the batch information of a dataset", hidden = true)
    public ResponseDataObject<BatchInformationValueObject> getDatasetBatchInformation(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        BatchEffectDetails details = expressionExperimentBatchInformationService.getBatchEffectDetails( ee );
        BatchEffectType be = getBatchEffectType( details );
        List<BatchConfound> confounds;
        Map<ExpressionExperimentSubSet, List<BatchConfound>> subsetConfounds;
        if ( expressionExperimentBatchInformationService.checkHasUsableBatchInfo( ee ) ) {
            confounds = expressionExperimentBatchInformationService.getSignificantBatchConfounds( ee );
            subsetConfounds = expressionExperimentBatchInformationService.getSignificantBatchConfoundsForSubsets( ee );
        } else {
            confounds = null;
            subsetConfounds = null;
        }
        return respond( new BatchInformationValueObject( be, details, confounds, subsetConfounds ) );
    }

    @Value
    public static class BatchInformationValueObject {

        @Schema(implementation = BatchEffectType.class)
        String batchEffect;

        @Nullable
        BatchEffectStatisticsValueObject batchEffectStatistics;

        boolean hasBatchInformation;
        boolean hasProblematicBatchInformation;
        boolean hasUninformativeBatchInformation;
        boolean hasSingletonBatch;
        boolean isSingleBatch;
        boolean dataWasBatchCorrected;

        @Nullable
        List<BatchConfoundValueObject> batchConfounds;
        @Nullable
        Map<Long, List<BatchConfoundValueObject>> subsetBatchConfounds;

        public BatchInformationValueObject( BatchEffectType batchEffectType, BatchEffectDetails batchEffectDetails, List<BatchConfound> batchConfound, Map<ExpressionExperimentSubSet, List<BatchConfound>> subsetBatchConfounds ) {
            this.batchEffect = batchEffectType.name();
            this.batchEffectStatistics = batchEffectDetails.getBatchEffectStatistics() != null ? new BatchEffectStatisticsValueObject( batchEffectDetails.getBatchEffectStatistics() ) : null;
            this.hasBatchInformation = batchEffectDetails.hasBatchInformation();
            this.hasProblematicBatchInformation = batchEffectDetails.hasProblematicBatchInformation();
            this.hasUninformativeBatchInformation = batchEffectDetails.hasUninformativeBatchInformation();
            this.hasSingletonBatch = batchEffectDetails.hasSingletonBatches();
            this.isSingleBatch = batchEffectDetails.isSingleBatch();
            this.dataWasBatchCorrected = batchEffectDetails.dataWasBatchCorrected();
            this.batchConfounds = batchConfound != null ? batchConfound.stream()
                    .map( BatchConfoundValueObject::new )
                    .collect( Collectors.toList() ) : null;
            this.subsetBatchConfounds = subsetBatchConfounds != null ? subsetBatchConfounds.entrySet().stream()
                    .collect( Collectors.toMap(
                            e -> e.getKey().getId(),
                            e -> e.getValue().stream().map( BatchConfoundValueObject::new ).collect( Collectors.toList() ) ) ) : null;

        }
    }

    @Value
    public static class BatchEffectStatisticsValueObject {

        double pvalue;
        int component;
        double componentVarianceProportion;

        public BatchEffectStatisticsValueObject( BatchEffectDetails.BatchEffectStatistics stats ) {
            this.pvalue = stats.getPvalue();
            this.component = stats.getComponent();
            this.componentVarianceProportion = stats.getComponentVarianceProportion();
        }
    }

    @Value
    public static class BatchConfoundValueObject {
        ExperimentalFactorValueObject factor;
        double chiSquared;
        int df;
        double pvalue;
        int numberOfBatches;

        public BatchConfoundValueObject( BatchConfound batchConfound ) {
            this.factor = new ExperimentalFactorValueObject( batchConfound.getFactor(), false );
            this.chiSquared = batchConfound.getChiSquare();
            this.df = batchConfound.getDf();
            this.pvalue = batchConfound.getPValue();
            this.numberOfBatches = batchConfound.getNumBatches();
        }
    }

    /**
     * Retrieves the per-probe mean / variance pre-computed by {@link ubic.gemma.core.analysis.preprocess.MeanVarianceService}.
     * <p>
     * 404 when {@link ExpressionExperiment#getMeanVarianceRelation()} is null (i.e. the
     * mean-variance step hasn't been run for the dataset). Backs the curation-UI Diagnostics
     * tab's mean-variance scatter.
     */
    @GET
    @Path("/{dataset}/mean-variance")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the per-probe mean / variance for a dataset",
            description = "Returns parallel mean[] and variance[] arrays computed by the mean-variance step. "
                    + "404 if the dataset has no MeanVarianceRelation. Note: design-element ids and names are "
                    + "currently omitted (Gemma's MeanVarianceRelation stores only the numeric arrays); the UI "
                    + "indexes by position.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist or has no mean-variance relation.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<MeanVarianceValueObject> getDatasetMeanVariance( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        // Re-load via loadWithMeanVarianceRelation: the entity from getEntity() carries a lazy
        // MVR proxy, so accessing getMeans()/getVariances() outside the open session throws
        // LazyInitializationException. The eager-loading variant fetches the arrays in-session.
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        ExpressionExperiment eeWithMvr = expressionExperimentService.loadWithMeanVarianceRelation( ee.getId() );
        MeanVarianceRelation mvr = eeWithMvr != null ? eeWithMvr.getMeanVarianceRelation() : null;
        if ( mvr == null || mvr.getMeans() == null || mvr.getVariances() == null ) {
            throw new NotFoundException( ee.getShortName() + " does not have a mean-variance relation." );
        }
        return respond( new MeanVarianceValueObject( mvr ) );
    }

    /**
     * Retrieves the sample-sample correlation matrix plus both outlier classifications
     * (curator-flagged + algorithmic), unmasked.
     * <p>
     * The UI applies its own masking interactively so the curator can see the effect of
     * including / excluding outliers on the correlation distribution. Two outlier sets
     * accompany the matrix:
     * <ul>
     *   <li>{@code actualOutlierBioAssayIds} — bioAssays the curator has flagged via
     *       {@code PUT /samples/{id}/outlier} ({@link BioAssay#getIsOutlier()}).
     *   <li>{@code predictedOutlierBioAssayIds} — bioAssays the median-correlation algorithm
     *       picks as outliers ({@link OutlierDetectionService#getOutlierDetails}); cached.
     * </ul>
     */
    @GET
    @Path("/{dataset}/sample-correlation")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the sample-sample correlation matrix + outlier classifications",
            description = "Returns the regressed (best) sample correlation matrix UNMASKED, plus two parallel outlier-id lists: `actualOutlierBioAssayIds` (curator-flagged) and `predictedOutlierBioAssayIds` (algorithmic). The UI applies any visualization masking it wants. 404 if no correlation analysis has been computed for the dataset.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist or has no sample correlation matrix.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<SampleCorrelationMatrixValueObject> getDatasetSampleCorrelation(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        DoubleMatrix<BioAssay, BioAssay> matrix = sampleCoexpressionAnalysisService.loadBestMatrix( ee );
        if ( matrix == null ) {
            throw new NotFoundException( ee.getShortName() + " does not have a sample correlation matrix." );
        }
        // Thaw bioassays so isOutlier reads from the persisted set.
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( ee );
        Set<Long> actualOutliers = new HashSet<>();
        for ( BioAssay ba : thawed.getBioAssays() ) {
            if ( ba.getIsOutlier() ) {
                actualOutliers.add( ba.getId() );
            }
        }
        Set<Long> predictedOutliers = new HashSet<>();
        try {
            outlierDetectionService.getOutlierDetails( thawed ).ifPresent( details -> {
                for ( OutlierDetails d : details ) {
                    predictedOutliers.add( d.getBioAssayId() );
                }
            } );
        } catch ( RuntimeException e ) {
            // Detection is best-effort; if it throws (empty matrix, etc.) just leave the set empty.
            log.warn( "predicted-outlier detection failed for " + thawed.getShortName() + ": " + e.getMessage() );
        }
        return respond( new SampleCorrelationMatrixValueObject( matrix, actualOutliers, predictedOutliers ) );
    }

    /**
     * Retrieves the design for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/svd")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the singular value decomposition (SVD) of a dataset expression data", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<SimpleSVDValueObject> getDatasetSvd( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        SVDResult svd = svdService.getSvd( ee );
        if ( svd == null ) {
            throw new NotFoundException( ee.getShortName() + " does not have an SVD." );
        }
        return respond( new SimpleSVDValueObject( svd ) );
    }

    /**
     * Sort direction for {@link #getDatasetSvdLoadings}: {@code both} sorts by |loading| desc,
     * {@code positive} filters to loading &gt; 0 desc, {@code negative} filters to loading &lt; 0 asc.
     */
    public enum PcLoadingDirection {
        both, positive, negative
    }

    private static final int SVD_LOADINGS_DEFAULT_TOP = 50;
    private static final int SVD_LOADINGS_MAX_TOP = 500;

    /**
     * Retrieve the top-N probe loadings on a chosen principal component, plus the bioAssay
     * scores on that PC. Backs the curation-UI Diagnostics tab's "click-PC → loaded-genes
     * popup" flow.
     * <p>
     * Uses {@link SVDService#getTopLoadedVectors(ExpressionExperiment, int, int)} to fetch the
     * stored {@link ProbeLoading} rows for the component (one DB hit; no expression-matrix
     * recompute). bioAssay scores come from the SVDResult's vMatrix column for the PC. Returns
     * 404 if the dataset has no SVD analysis, 400 if {@code pc} or {@code top} are out of range.
     */
    @GET
    @Path("/{dataset}/svd/loadings")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve top-loaded probes on a principal component for a dataset",
            description = "Returns the top-N probe loadings on the chosen PC (sorted by |loading| desc for "
                    + "`direction=both`, signed for `positive` / `negative`) plus the bioAssay scores on that PC. "
                    + "404 if SVD has not been computed.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Invalid pc, top, or direction.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist or has no SVD analysis.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<PcLoadingsValueObject> getDatasetSvdLoadings( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @Parameter(description = "1-indexed principal component number.", required = true) @QueryParam("pc") Integer pc,
            @Parameter(description = "Number of top loadings to return (max " + SVD_LOADINGS_MAX_TOP + ").",
                    schema = @Schema(type = "integer", defaultValue = "" + SVD_LOADINGS_DEFAULT_TOP, minimum = "1", maximum = "" + SVD_LOADINGS_MAX_TOP))
            @QueryParam("top") @DefaultValue("" + SVD_LOADINGS_DEFAULT_TOP) Integer top,
            @Parameter(description = "`both` (sort by |loading| desc, default), `positive` (loading > 0 desc), `negative` (loading < 0 asc).")
            @QueryParam("direction") @DefaultValue("both") PcLoadingDirection direction
    ) {
        if ( pc == null || pc < 1 ) {
            throw new BadRequestException( "pc must be a positive 1-indexed integer." );
        }
        if ( top == null || top < 1 || top > SVD_LOADINGS_MAX_TOP ) {
            throw new BadRequestException( "top must be in [1, " + SVD_LOADINGS_MAX_TOP + "]." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( !svdService.hasSvd( ee ) ) {
            throw new NotFoundException( ee.getShortName() + " does not have an SVD analysis." );
        }
        // SVDService.getTopLoadedVectors is 1-indexed on the component arg (component=1 is PC1).
        // We over-fetch (top+spare) so the in-memory filter for direction=positive/negative still
        // has top entries when the natural-rank top is mixed-sign; cap the over-fetch.
        int fetchCount = direction == PcLoadingDirection.both ? top : Math.min( SVD_LOADINGS_MAX_TOP, top * 4 );
        Map<ProbeLoading, DoubleVectorValueObject> loaded = svdService.getTopLoadedVectors( ee, pc, fetchCount );
        SVDResult svd = svdService.getSvd( ee );
        if ( svd == null ) {
            // hasSvd() can be true (probe-loading rows exist) while getSvd() returns null
            // (no full SVDResult entity stored). Avoid the NPE that surfaces as a 500 on the
            // Diagnostics tab; treat as "no usable SVD" so the UI can render the gracefully-empty
            // state next to the working sample-correlation / mean-variance panes.
            throw new NotFoundException( ee.getShortName() + " has SVD loadings but no full SVDResult; rerun the SVD task to populate." );
        }
        return respond( PcLoadingsValueObject.from( pc, top, direction, loaded, svd ) );
    }

    /**
     * Retrieve the expression levels of a given gene across all datasets.
     */
    @GET
    @Path("/expressions/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression levels of a gene among datasets matching the provided query and filter",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination when many datasets carry the gene of interest): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive -- passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the dataset list is always sorted by ascending `datasetId` (the underlying dataset-id list is +id sorted, matching the existing groupBy=datasetId contract); "
                    + "the user-supplied `?filter=` and optional `?query=` constraints are preserved (both modes intersect the search-resolved dataset ids identically); "
                    + "`totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    QueriedAndFilteredAndInferredAndPaginatedResponseDataObjectExperimentExpressionLevelsValueObject.class,
                                    QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObjectExperimentExpressionLevelsValueObject.class
                            }))),
            })
    public Object getDatasetsExpressionLevelsForGene(
            @PathParam("gene") GeneArg<?> geneArg,
            @QueryParam("query") QueryArg queryArg,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate, // Optional, default everything is returned
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg
    ) {
        return getDatasetsExpressionLevelsForGeneInTaxonInternal( geneArgService.getEntity( geneArg ), queryArg, filterArg, offsetArg, limitArg, keepNonSpecific, consolidate, cursorArg );
    }

    /**
     * Retrieve the expression levels of a given gene and taxon across all datasets.
     */
    @GET
    @Path("/expressions/taxa/{taxon}/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression levels of a gene and taxa among datasets matching the provided query and filter",
            description = "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); response includes `offset` and `totalElements`. "
                    + "Cursor mode (recommended for deep pagination): pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive -- passing a non-null `cursor` selects cursor mode. "
                    + "In cursor mode the dataset list is always sorted by ascending `datasetId`; the path-derived `{taxon}` scope is preserved at gene-resolution time identically to the offset variant; "
                    + "`totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    QueriedAndFilteredAndInferredAndPaginatedResponseDataObjectExperimentExpressionLevelsValueObject.class,
                                    QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObjectExperimentExpressionLevelsValueObject.class
                            }))),
            })
    public Object getDatasetsExpressionLevelsForGeneInTaxon(
            @PathParam("taxon") TaxonArg<?> taxonArg,
            @PathParam("gene") GeneArg<?> geneArg,
            @QueryParam("query") QueryArg queryArg,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate, // Optional, default everything is returned
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.") @QueryParam("cursor") CursorArg cursorArg
    ) {
        return getDatasetsExpressionLevelsForGeneInTaxonInternal( geneArgService.getEntityWithTaxon( geneArg, taxonArgService.getEntity( taxonArg ) ), queryArg, filterArg, offsetArg, limitArg, keepNonSpecific, consolidate, cursorArg );
    }

    private Object getDatasetsExpressionLevelsForGeneInTaxonInternal( Gene gene, @Nullable QueryArg queryArg, FilterArg<ExpressionExperiment> filterArg, OffsetArg offsetArg, LimitArg limitArg, boolean keepNonSpecific, @Nullable ExpLevelConsolidationArg consolidate, @Nullable CursorArg cursorArg ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filter = datasetArgService.getFilters( filterArg, null, inferredTerms );
        Sort sort = datasetArgService.getSort( SortArg.valueOf( "+id" ) );
        List<Long> datasetIds = expressionExperimentService.loadIdsWithCache( filter, sort );
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        if ( queryArg != null ) {
            datasetIds.retainAll( datasetArgService.getIdsForSearchQuery( queryArg, warnings ) );
        }
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels step 1u /datasets/{dataset}/subSets/{subSet}/samples
            // and earlier steps). In cursor mode the sortSpec is fixed at "+datasetId" -- the
            // underlying datasetIds list is loaded +id sorted (loadIdsWithCache(filter, +id)) and
            // the cursor pages over that in-memory list. The filter and (optional) search-query
            // intersection are applied identically to the offset variant; only the slicing
            // strategy changes. totalElements is omitted (cursor mode does not count per request).
            CursorPage<ExperimentExpressionLevelsValueObject> page = sliceExpressionLevelsByCursor(
                    datasetIds, gene, keepNonSpecific, consolidate, cursorArg.getValue(), limitArg.getValue() );
            return new QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<>(
                    page, queryArg != null ? queryArg.getValue() : null, filter, new String[] { "datasetId" }, inferredTerms )
                    .addWarnings( warnings, "query", LocationType.QUERY );
        }
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        Slice<ExperimentExpressionLevelsValueObject> slice = new Slice<>( processedExpressionDataVectorService
                .getExpressionLevelsByIds( sliceIds( datasetIds, offset, limit ),
                        Collections.singleton( gene ),
                        keepNonSpecific,
                        consolidate == null ? null : consolidate.getValue() ), sort, offset, limit, ( long ) datasetIds.size() );
        return paginate( slice, queryArg != null ? queryArg.getValue() : null, filter, new String[] { "datasetId" }, inferredTerms )
                .addWarnings( warnings, "query", LocationType.QUERY );
    }

    /**
     * In-memory keyset-paginate the +id-sorted {@code datasetIds} list, then materialize the
     * expression-level value objects for that window. Mirrors the offset variant's
     * {@link #sliceIds(List, int, int)} + {@link ProcessedExpressionDataVectorService#getExpressionLevelsByIds(Collection, Collection, boolean, String)}
     * pipeline but routes through cursor semantics (lastSeenId + direction) instead of (offset, limit).
     * <p>
     * The cursor sortSpec is "+datasetId" -- that's the field name exposed in the response
     * payload's groupBy contract. The DAO-side ee.id-asc ordering produced by
     * {@code loadIdsWithCache(filter, +id sort)} is the same canonical ordering this cursor
     * pages over, so cursor and offset modes scan the same sequence.
     */
    private CursorPage<ExperimentExpressionLevelsValueObject> sliceExpressionLevelsByCursor(
            List<Long> datasetIds, Gene gene, boolean keepNonSpecific,
            @Nullable ExpLevelConsolidationArg consolidate, @Nullable Cursor cursor, int limit ) {
        if ( limit <= 0 ) {
            throw new MalformedArgException( "Cursor page limit must be > 0.", null );
        }
        final String expectedSortSpec = "+datasetId";
        boolean backward = false;
        Long lastSeenId = null;
        if ( cursor != null ) {
            if ( !expectedSortSpec.equals( cursor.getSortSpec() ) ) {
                throw new MalformedArgException( "Cursor sort spec '" + cursor.getSortSpec()
                        + "' does not match the requested sort '" + expectedSortSpec + "'.", null );
            }
            Object[] key = cursor.getKeyTuple();
            if ( key.length != 1 ) {
                throw new MalformedArgException( "Cursor key tuple must have exactly 1 component for sort '"
                        + expectedSortSpec + "'; got " + key.length + ".", null );
            }
            try {
                lastSeenId = ( ( Number ) key[0] ).longValue();
            } catch ( ClassCastException e ) {
                throw new MalformedArgException( "Cursor key component must be numeric for sort '"
                        + expectedSortSpec + "'.", e );
            }
            backward = cursor.getDirection() == Cursor.Direction.BACKWARD;
        }

        // Walk the +id sorted in-memory list, applying the cursor's window predicate. Pull
        // limit+1 ids to detect hasMore; reverse the slice when the request was BACKWARD so
        // the returned page is always ascending datasetId in client-visible order.
        List<Long> windowIds = new ArrayList<>( limit + 1 );
        if ( backward ) {
            // BACKWARD: take ids strictly less than lastSeenId, scanning from the tail.
            // datasetIds is +id sorted; walk it in reverse to fill up to limit+1 ids whose
            // value is less than the cursor key.
            for ( int i = datasetIds.size() - 1; i >= 0 && windowIds.size() < limit + 1; i-- ) {
                Long id = datasetIds.get( i );
                if ( lastSeenId == null || id < lastSeenId ) {
                    windowIds.add( id );
                }
            }
            // Collected in descending id order; reverse for ascending client-visible output.
            Collections.reverse( windowIds );
        } else {
            for ( int i = 0; i < datasetIds.size() && windowIds.size() < limit + 1; i++ ) {
                Long id = datasetIds.get( i );
                if ( lastSeenId == null || id > lastSeenId ) {
                    windowIds.add( id );
                }
            }
        }
        boolean hasMore = windowIds.size() > limit;
        if ( hasMore ) {
            windowIds = backward
                    // backward over-read sits at the FRONT (smaller ids); drop the head item.
                    ? new ArrayList<>( windowIds.subList( 1, windowIds.size() ) )
                    // forward over-read sits at the TAIL; drop the tail item.
                    : new ArrayList<>( windowIds.subList( 0, limit ) );
        }

        List<ExperimentExpressionLevelsValueObject> data = windowIds.isEmpty()
                ? Collections.emptyList()
                : processedExpressionDataVectorService.getExpressionLevelsByIds( windowIds,
                Collections.singleton( gene ), keepNonSpecific,
                consolidate == null ? null : consolidate.getValue() );

        String nextCursor = null;
        String prevCursor = null;
        if ( !windowIds.isEmpty() ) {
            Long lastId = windowIds.get( windowIds.size() - 1 );
            Long firstId = windowIds.get( 0 );
            // forward: emit nextCursor only when there's another page in the forward direction
            // backward: we know at least one page exists ahead (we just came from there), so
            // always emit nextCursor when navigating backward.
            if ( backward || hasMore ) {
                nextCursor = new Cursor( expectedSortSpec, new Object[] { lastId }, Cursor.Direction.FORWARD ).encode();
            }
            // emit prevCursor whenever we have a cursor (at least one page is behind us) OR
            // when the forward over-read indicated we already advanced past the first page.
            if ( cursor != null ) {
                prevCursor = new Cursor( expectedSortSpec, new Object[] { firstId }, Cursor.Direction.BACKWARD ).encode();
            }
        }

        Sort idSort = Sort.by( null, "datasetId", Sort.Direction.ASC, Sort.NullMode.LAST, "datasetId" );
        return new CursorPage<>( data, idSort, limit, nextCursor, prevCursor, null );
    }

    /**
     * Retrieves the expression levels of given genes on given datasets.
     *
     * @param datasets        a list of dataset identifiers separated by commas (','). The identifiers can either be the
     *                        ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                        is more efficient. Only datasets that user has access to will be available.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param taxonArg        a taxon to retrieve gene identifiers from
     * @param genes           a list of gene identifiers, separated by commas (','). Identifiers can be one of
     *                        NCBI ID, Ensembl ID or official symbol. NCBI ID is the most efficient (and
     *                        guaranteed to be unique) identifier. Official symbol will return a random homologue. Use
     *                        one
     *                        of the IDs to specify the correct taxon - if the gene taxon does not match the taxon of
     *                        the
     *                        given datasets, expression levels for that gene will be missing from the response.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param keepNonSpecific whether to keep elements that are mapped to multiple genes.
     * @param consolidate     whether genes with multiple elements should consolidate the information. The options are:
     *                        <ul>
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its
     *                        bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its
     *                        bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all
     *                        vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets}/expressions/taxa/{taxon}/genes/{genes}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression data matrix of a set of datasets and genes")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetsExpressionLevelsForGenesInTaxon( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @PathParam("genes") GeneArrayArg genes, // Required
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
    ) {
        return respond( processedExpressionDataVectorService
                .getExpressionLevels( datasetArgService.getEntities( datasets ),
                        geneArgService.getEntitiesWithTaxon( genes, taxonArgService.getEntity( taxonArg ) ),
                        keepNonSpecific,
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    @GET
    @Path("/{datasets}/expressions/genes/{genes}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression data matrix of a set of datasets and genes")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetsExpressionLevelsForGenes( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @PathParam("genes") GeneArrayArg genes, // Required
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean
                    keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg
                    consolidate // Optional, default everything is returned
    ) {
        return respond( processedExpressionDataVectorService
                .getExpressionLevels( datasetArgService.getEntities( datasets ),
                        geneArgService.getEntities( genes ), keepNonSpecific,
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    /**
     * Retrieves the expression levels of genes highly expressed in the given component on given datasets.
     *
     * @param datasets        a list of dataset identifiers separated by commas (','). The identifiers can either be the
     *                        ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                        is more efficient. Only datasets that user has access to will be available.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param limit           maximum amount of returned gene-probe expression level pairs.
     * @param component       the pca component to limit the results to.
     * @param keepNonSpecific whether to keep elements that are mapped to multiple genes.
     * @param consolidate     whether genes with multiple elements should consolidate the information. The options are:
     *                        <ul>
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its
     *                        bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its
     *                        bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all
     *                        vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets}/expressions/pca")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the principal components (PCA) of a set of datasets")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetsExpressionPca( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @QueryParam("component") @DefaultValue("1") Integer component, // Required, default 1
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean
                    keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg
                    consolidate // Optional, default everything is returned
    ) {
        return respond( processedExpressionDataVectorService
                .getExpressionLevelsPca( datasetArgService.getEntities( datasets ), limit.getValueNoMaximum(),
                        component, keepNonSpecific,
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    /**
     * Retrieves the expression levels of genes highly expressed in the given component on given datasets.
     *
     * @param datasets        a list of dataset identifiers separated by commas (','). The identifiers can either be the
     *                        ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                        is more efficient. Only datasets that user has access to will be available.
     *                        <p>
     *                        You can combine various identifiers in one query, but an invalid identifier will cause the
     *                        call to yield an error.
     *                        </p>
     * @param diffExSet       the ID of the differential expression set to retrieve the data from.
     * @param threshold       the FDR threshold that the differential expression has to meet to be included in the response.
     * @param limit           maximum amount of returned gene-probe expression level pairs.
     * @param keepNonSpecific whether to keep elements that are mapped to multiple genes.
     * @param consolidate     whether genes with multiple elements should consolidate the information. The options are:
     *                        <ul>
     *                        <li>pickmax: only return the vector that has the highest expression (mean over all its
     *                        bioAssays)</li>
     *                        <li>pickvar: only return the vector with highest variance of expression across its
     *                        bioAssays</li>
     *                        <li>average: create a new vector that will average the bioAssay values from all
     *                        vectors</li>
     *                        </ul>
     */
    @GET
    @Path("/{datasets}/expressions/differential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression levels of a set of datasets subject to a threshold on their differential expressions",
            description = "Each entry under data[].geneExpressionLevels[] also carries gene-level "
                    + "metadata (officialName, ensemblId) and contrast statistics for the chosen "
                    + "result set: correctedPvalue and pvalue come from the per-row "
                    + "DifferentialExpressionAnalysisResult that the endpoint ranks by (ordered by "
                    + "correctedPvalue ascending, nulls last), and log2FoldChange is taken from the "
                    + "single contrast on that row — or, for multi-contrast result sets, from the "
                    + "contrast with the smallest uncorrected p-value on that row. When a gene maps "
                    + "to several probes, the most-significant probe row is used. All five fields are "
                    + "nullable and are additive — existing fields are unchanged.")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetsDifferentialExpression( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @QueryParam("diffExSet") Long diffExSet, // Required
            @Parameter(description = PVALUE_THRESHOLD_DESCRIPTION) @QueryParam("threshold") @DefaultValue("1.0") Double threshold, // Optional, default 1.0
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @Parameter(description = "Keep results from non-specific probes.") @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @Parameter(description = "Strategy for consolidating expression of multiple probes for a given gene.") @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
    ) {
        if ( diffExSet == null ) {
            throw new BadRequestException( "The 'diffExSet' query parameter must be supplied." );
        }
        return respond( processedExpressionDataVectorService
                .getExpressionLevelsDiffEx( datasetArgService.getEntities( datasets ),
                        diffExSet, threshold, limit.getValueNoMaximum(), keepNonSpecific,
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    /**
     * Retrieve a "refreshed" dataset.
     * <p>
     * This has the main side effect of refreshing the second-level cache with the contents of the database.
     */
    @GET
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Path("/{dataset}/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve a refreshed dataset",
            security = {
                    @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" })
            },
            responses = {
                    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = ResponseDataObjectExpressionExperimentValueObject.class)))
            })
    public Response refreshDataset(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Refresh processed data vectors.") @QueryParam("refreshVectors") @DefaultValue("false") Boolean refreshVectors,
            @Parameter(description = "Refresh experiment reports which include differential expression analyses and batch effects.") @QueryParam("refreshReports") @DefaultValue("false") Boolean refreshReports
    ) {
        Long id = datasetArgService.getEntityId( datasetArg );
        if ( id == null ) {
            throw new NotFoundException( "No dataset matches " + datasetArg );
        }
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteWithRefreshCacheMode( id );
        if ( ee == null ) {
            throw new NotFoundException( "No dataset with ID " + id );
        }
        if ( refreshVectors ) {
            processedExpressionDataVectorService.evictFromCache( ee );
        }
        if ( refreshReports ) {
            expressionExperimentReportService.evictFromCache( id );
        }
        return Response.created( URI.create( "/datasets/" + ee.getId() ) )
                .entity( new ResponseDataObjectExpressionExperimentValueObject( expressionExperimentService.loadValueObject( ee ) ) )
                .build();
    }

    /**
     * Retrieve all the "groups" of subsets of a dataset.
     * <p>
     * Each group of subsets is logically organized by a {@link BioAssayDimension} that holds its assays. We don't
     * expose that aspect however, and simply use the ID of the BAD as ID of the group.
     */
    @GET
    @Path("/{dataset}/subSetGroups")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain all the subset groups of a dataset")
    public ResponseDataObject<List<ExpressionExperimentSubSetGroupValueObject>> getDatasetSubSetGroups(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        return respond( expressionExperimentService.getSubSetsByDimension( ee )
                .entrySet()
                .stream()
                .map( e -> {
                    Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> ssvs = expressionExperimentService.getSubSetsByFactorValue( ee, e.getKey() );
                    List<QuantitationTypeValueObject> qts = expressionExperimentService.getQuantitationTypes( ee, e.getKey() ).stream()
                            .sorted( Comparator.comparing( QuantitationType::getName ) )
                            .map( qt -> new QuantitationTypeValueObject( qt, ee, quantitationTypeService.getDataVectorType( qt ) ) )
                            .collect( Collectors.toList() );
                    return createSubSetGroup( e.getKey(), e.getValue(), ssvs, qts, false );
                } )
                .collect( Collectors.toList() ) );
    }

    @GET
    @Path("/{dataset}/subSetGroups/{subSetGroup}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain a specific subset group of a dataset")
    public ResponseDataObject<ExpressionExperimentSubSetGroupValueObject> getDatasetSubSetGroup(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("subSetGroup") Long bioAssayDimensionId
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        // this is preferred, because it does not require any data to be present
        BioAssayDimension bad = expressionExperimentService.getBioAssayDimensionById( ee, bioAssayDimensionId );
        if ( bad == null ) {
            throw new NotFoundException( "No subset group with ID " + bioAssayDimensionId );
        }
        Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> ssvs = expressionExperimentService.getSubSetsByFactorValue( ee, bad );
        List<QuantitationTypeValueObject> qts = expressionExperimentService.getQuantitationTypes( ee, bad ).stream()
                .sorted( Comparator.comparing( QuantitationType::getName ) )
                .map( qt -> new QuantitationTypeValueObject( qt, ee, quantitationTypeService.getDataVectorType( qt ) ) )
                .collect( Collectors.toList() );
        return respond( createSubSetGroup( bad, expressionExperimentService.getSubSetsWithBioAssays( ee, bad ), ssvs, qts, true ) );
    }

    private ExpressionExperimentSubSetGroupValueObject createSubSetGroup( BioAssayDimension bad,
            Collection<ExpressionExperimentSubSet> subsets,
            Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> ssvs,
            List<QuantitationTypeValueObject> qts,
            boolean includeAssays ) {
        Map<ExpressionExperimentSubSet, Set<FactorValue>> fvs = new HashMap<>();
        ssvs.forEach( ( ef, s2fv ) -> {
            s2fv.forEach( ( fv, s ) -> {
                fvs.computeIfAbsent( s, k -> new HashSet<>() ).add( fv );
            } );
        } );
        List<ExperimentalFactorValueObject> factors = ssvs.keySet().stream()
                .sorted( Comparator.comparing( ExperimentalFactor::getName ) )
                // don't include values, those are already included in the subsets
                .map( ef -> new ExperimentalFactorValueObject( ef, false ) )
                .collect( Collectors.toList() );
        List<ExpressionExperimentSubsetWithFactorValuesObject> ssvos = subsets.stream()
                // TODO order the subsets by how they appear in the BioAssayDimension
                .sorted( Comparator.comparing( ExpressionExperimentSubSet::getName ) )
                .map( subset -> {
                    Map<ArrayDesign, ArrayDesignValueObject> id2advo;
                    Map<BioAssay, BioAssay> assay2sourceAssayMap;
                    if ( includeAssays ) {
                        id2advo = new HashMap<>();
                        for ( BioAssay ba : subset.getBioAssays() ) {
                            id2advo.computeIfAbsent( ba.getArrayDesignUsed(), ArrayDesignValueObject::new );
                            if ( ba.getOriginalPlatform() != null ) {
                                id2advo.computeIfAbsent( ba.getOriginalPlatform(), ArrayDesignValueObject::new );
                            }
                        }
                        assay2sourceAssayMap = BioAssayUtils.createBioAssayToSourceBioAssayMap( subset.getSourceExperiment(), subset.getBioAssays() );
                    } else {
                        id2advo = null;
                        assay2sourceAssayMap = null;
                    }
                    ExpressionExperimentSubsetWithFactorValuesObject vo = new ExpressionExperimentSubsetWithFactorValuesObject( subset, fvs.get( subset ), id2advo, includeAssays, assay2sourceAssayMap );
                    if ( includeAssays ) {
                        datasetArgService.populateOutliers( subset.getSourceExperiment(), vo.getBioAssays() );
                    }
                    return vo;
                } )
                .collect( Collectors.toList() );
        return new ExpressionExperimentSubSetGroupValueObject( bad, ssvos, factors, qts );
    }

    @GET
    @Path("/{dataset}/subSets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain all subsets of a dataset")
    public ResponseDataObject<List<ExpressionExperimentSubSetWithGroupsValueObject>> getDatasetSubSets(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        Map<ExpressionExperimentSubSet, List<Long>> subSetGroups = datasetArgService.getSubSetsGroupIds( datasetArg );
        return respond( datasetArgService.getSubSets( datasetArg ).stream()
                .map( subset -> new ExpressionExperimentSubSetWithGroupsValueObject( subset, subSetGroups.getOrDefault( subset, Collections.emptyList() ) ) )
                .collect( Collectors.toList() ) );
    }

    @GET
    @Path("/{dataset}/subSets/{subSet}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain a specific subset of a dataset")
    public ResponseDataObject<ExpressionExperimentSubSetWithGroupsValueObject> getDatasetSubSetById(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("subSet") Long subSetId
    ) {
        ExpressionExperimentSubSet subset = datasetArgService.getSubSet( datasetArg, subSetId );
        List<Long> subSetGroups = datasetArgService.getSubSetGroupIds( datasetArg, subset );
        return respond( new ExpressionExperimentSubSetWithGroupsValueObject( subset, subSetGroups ) );
    }

    /**
     * Retrieves the samples of a specific subset of a dataset.
     * <p>
     * Step 1u of {@code CURSOR_PAGINATION_STEP1_PLAN.md} adds an opt-in cursor-mode branch
     * parallel to step 1k ({@code /datasets/{dataset}/samples}). The legacy mode (no
     * {@code cursor}) is preserved byte-for-byte: an unpaginated
     * {@link ResponseDataObject}{@code <List<BioAssayValueObject>>} with the full subset
     * sample list. Cursor mode is available for consistency with the other listings; a
     * subset's assay list itself stays small (single-cell size is in cells, not assays).
     * Cursor mode always sorts by ascending {@code id}; the
     * path-derived {@code subSet.id = ?} constraint is preserved across modes;
     * {@code totalElements} is {@code null} by default (no count query per request).
     */
    @GET
    @Path("/{dataset}/subSets/{subSet}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain the samples of a specific subset of a dataset",
            description = "Legacy mode (no `cursor` parameter): returns the full unpaginated assay list in the existing shape. "
                    + "Cursor mode (available for consistency; a subset's assay list stays small — single-cell size is in cells, not assays): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` field along with a `limit`. "
                    + "In cursor mode the result is always sorted by ascending `id` (cursor mode forces a single-component id sort pending the indexed-column audit in phase B); "
                    + "the path-derived `subSet.id = ?` constraint is preserved; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    ResponseDataObjectListBioAssayValueObject.class,
                                    CursorPaginatedResponseDataObjectBioAssayValueObject.class
                            }))),
                    @ApiResponse(responseCode = "404", description = "The dataset or subset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Object getDatasetSubSetSamples(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("subSet") Long subSetId,
            @Parameter(description = "Opaque keyset-pagination cursor token.")
            @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Page size for cursor mode (ignored when no `cursor` is supplied).")
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        if ( cursorArg != null ) {
            CursorPage<BioAssayValueObject> page = datasetArgService.getSubSetSamplesByCursor( datasetArg, subSetId, cursorArg.getValue(), limitArg.getValue() );
            return paginateByCursor( page, new String[] { "id" } );
        }
        return respond( datasetArgService.getSubSetSamples( datasetArg, subSetId ) );
    }

    /**
     * A group of subsets, logically organized by a {@link BioAssayDimension}.
     *
     * @author poirigui
     */
    @Getter
    public static class ExpressionExperimentSubSetGroupValueObject {

        private final Long id;

        private final String name;

        /**
         * List of factors that are associated with the subsets in this group.
         */
        private final List<ExperimentalFactorValueObject> factors;

        private final List<QuantitationTypeValueObject> quantitationTypes;

        private final List<ExpressionExperimentSubsetWithFactorValuesObject> subSets;

        public ExpressionExperimentSubSetGroupValueObject( BioAssayDimension bioAssayDimension, List<ExpressionExperimentSubsetWithFactorValuesObject> subSets, List<ExperimentalFactorValueObject> factors, List<QuantitationTypeValueObject> quantitationTypes ) {
            this.id = bioAssayDimension.getId();
            // FIXME: make the name generation more robust, it's only tailored to how we name single-cell subsets
            this.name = StringUtils.removeEnd( StringUtils.getCommonPrefix( subSets.stream().map( ExpressionExperimentSubsetValueObject::getName ).toArray( String[]::new ) ), " - " );
            this.subSets = subSets;
            this.factors = factors;
            this.quantitationTypes = quantitationTypes;
        }
    }

    @Getter
    public static class ExpressionExperimentSubsetWithFactorValuesObject extends ExpressionExperimentSubsetValueObject {

        private final List<FactorValueBasicValueObject> factorValues;

        public ExpressionExperimentSubsetWithFactorValuesObject( ExpressionExperimentSubSet subset,
                Set<FactorValue> factorValues,
                @Nullable Map<ArrayDesign, ArrayDesignValueObject> id2advo,
                boolean includeAssays, @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap ) {
            super( subset, id2advo, assay2sourceAssayMap, includeAssays, true, true );
            this.factorValues = factorValues.stream()
                    .map( FactorValueBasicValueObject::new )
                    .collect( Collectors.toList() );
        }
    }

    @Getter
    public static class ExpressionExperimentSubSetWithGroupsValueObject extends ExpressionExperimentSubsetValueObject {

        private final List<Long> subSetGroupIds;

        public ExpressionExperimentSubSetWithGroupsValueObject( ExpressionExperimentSubSet subset, List<Long> subSetGroupIds ) {
            super( subset, null, null, false, true, false );
            this.subSetGroupIds = subSetGroupIds;
        }
    }

    public static class ResponseDataObjectExpressionExperimentValueObject extends ResponseDataObject<ExpressionExperimentValueObject> {

        public ResponseDataObjectExpressionExperimentValueObject( ExpressionExperimentValueObject payload ) {
            super( payload );
        }
    }

    @Value
    public static class SimpleSVDValueObject {
        /**
         * BioAssay IDs
         * Order same as rows of the v matrix.
         */
        List<Long> bioAssayIds;
        /**
         * Order same as the rows of the v matrix.
         */
        List<Long> bioMaterialIds;

        /**
         * An array of values representing the fraction of the variance each component accounts for
         */
        double[] variances;
        double[][] vMatrix;

        public SimpleSVDValueObject( SVDResult svd ) {
            bioAssayIds = svd.getBioAssays().stream().map( BioAssay::getId ).collect( Collectors.toList() );
            bioMaterialIds = svd.getBioMaterials().stream().map( BioMaterial::getId ).collect( Collectors.toList() );
            variances = svd.getVariances();
            vMatrix = svd.getVMatrix().getRawMatrix();
        }
    }

    /**
     * Wire shape for {@link #getDatasetSampleCorrelation}: a symmetric N×N Pearson correlation
     * matrix with bioAssay ids + short names parallel to the rows/columns.
     */
    @Value
    public static class SampleCorrelationMatrixValueObject {

        /**
         * BioAssay ids in the order the rows / columns of {@link #values} appear.
         */
        Long[] bioAssayIds;

        /**
         * BioAssay short names parallel to {@link #bioAssayIds}, for axis labels. Entries may be
         * null for assays whose name has not been set.
         */
        String[] bioAssayShortNames;

        /**
         * Symmetric, row-major, N×N Pearson correlation matrix. {@code values[i][j]} is the
         * correlation in [-1, 1] between the i'th and j'th bioAssay.
         * Always sent UNMASKED — the UI applies any outlier-driven masking interactively.
         */
        double[][] values;

        /**
         * BioAssay ids the curator has explicitly flagged as outliers ({@link BioAssay#getIsOutlier()}).
         */
        Long[] actualOutlierBioAssayIds;

        /**
         * BioAssay ids the median-correlation algorithm flags as outliers
         * ({@link OutlierDetectionService#getOutlierDetails}). May overlap with
         * {@link #actualOutlierBioAssayIds} or stand alone.
         */
        Long[] predictedOutlierBioAssayIds;

        /**
         * Currently always {@code null}; placeholder for a probe-filter caption once
         * {@link SampleCoexpressionAnalysisService} surfaces it.
         */
        @Nullable
        String filterDescription;

        /**
         * Currently always {@code "pearson"} — Gemma's only supported correlation method here.
         */
        @Nullable
        String method;

        public SampleCorrelationMatrixValueObject( DoubleMatrix<BioAssay, BioAssay> matrix,
                Set<Long> actualOutlierIds, Set<Long> predictedOutlierIds ) {
            List<BioAssay> rowAssays = matrix.getRowNames();
            this.bioAssayIds = rowAssays.stream().map( BioAssay::getId ).toArray( Long[]::new );
            this.bioAssayShortNames = rowAssays.stream().map( BioAssay::getName ).toArray( String[]::new );
            this.values = matrix.getRawMatrix();
            this.actualOutlierBioAssayIds = actualOutlierIds.stream().sorted().toArray( Long[]::new );
            this.predictedOutlierBioAssayIds = predictedOutlierIds.stream().sorted().toArray( Long[]::new );
            this.filterDescription = null;
            this.method = "pearson";
        }
    }

    /**
     * Wire shape for {@link #getDatasetMeanVariance}: parallel mean / variance arrays per probe.
     * Design-element ids / names and the optional limma/edgeR fit curve are placeholders for now:
     * Gemma's {@link MeanVarianceRelation} stores only the numeric arrays.
     */
    @Value
    public static class MeanVarianceValueObject {

        /**
         * Reserved — Gemma's {@link MeanVarianceRelation} does not currently carry design-element
         * ids; the UI indexes the parallel arrays positionally.
         */
        @Nullable
        Long[] designElementIds;

        /**
         * Reserved — see {@link #designElementIds}.
         */
        @Nullable
        String[] designElementNames;

        /**
         * Per-probe means (typically log-CPM or normalized intensity).
         */
        double[] means;

        /**
         * Per-probe variances (squared SD or robust variance), parallel to {@link #means}.
         */
        double[] variances;

        /**
         * Reserved — Gemma's {@link MeanVarianceRelation} does not currently expose a fit curve.
         */
        @Nullable
        Fit fit;

        /**
         * Reserved — placeholder for the producing method (e.g. {@code "limma_voom"},
         * {@code "edger_glmqlf"}, {@code "naive"}). Currently always {@code null}.
         */
        @Nullable
        String source;

        public MeanVarianceValueObject( MeanVarianceRelation mvr ) {
            this.designElementIds = null;
            this.designElementNames = null;
            this.means = mvr.getMeans();
            this.variances = mvr.getVariances();
            this.fit = null;
            this.source = null;
        }

        @Value
        public static class Fit {
            double[] sortedMeans;
            double[] fittedVariances;
        }
    }

    /**
     * Wire shape for {@link #getDatasetSvdLoadings}: the top-N probe loadings on a principal
     * component plus the bioAssay scores on the same PC.
     */
    @Value
    public static class PcLoadingsValueObject {

        /**
         * 1-indexed principal component number this payload is for.
         */
        int pc;

        /**
         * Top-N probe loadings, sorted by |loading| desc ({@code direction=both}), descending
         * loading ({@code positive}), or ascending loading ({@code negative}).
         */
        List<Row> rows;

        /**
         * Map of bioAssay id to the assay's score on this PC. Pulled from the SVDResult's
         * v-matrix column for the requested PC.
         */
        Map<Long, Double> bioAssayScores;

        public static PcLoadingsValueObject from( int pc, int top, PcLoadingDirection direction,
                Map<ProbeLoading, DoubleVectorValueObject> loaded, SVDResult svd ) {
            // Filter + sort the loadings.
            List<Row> rows = loaded.keySet().stream()
                    .filter( pl -> pl.getLoading() != null )
                    .filter( pl -> {
                        double v = pl.getLoading();
                        switch ( direction ) {
                            case positive:
                                return v > 0;
                            case negative:
                                return v < 0;
                            case both:
                            default:
                                return true;
                        }
                    } )
                    .sorted( ( a, b ) -> {
                        double av = a.getLoading();
                        double bv = b.getLoading();
                        switch ( direction ) {
                            case positive:
                                return Double.compare( bv, av );
                            case negative:
                                return Double.compare( av, bv );
                            case both:
                            default:
                                return Double.compare( Math.abs( bv ), Math.abs( av ) );
                        }
                    } )
                    .limit( top )
                    .map( pl -> {
                        CompositeSequence probe = pl.getProbe();
                        Long deId = probe != null ? probe.getId() : null;
                        String deName = probe != null ? probe.getName() : null;
                        return new Row( deId, deName, null, pl.getLoading() );
                    } )
                    .collect( Collectors.toList() );

            // bioAssayScores: pull column `pc-1` of the v-matrix (1-indexed PC).
            Map<Long, Double> scores = new LinkedHashMap<>();
            int colIdx = pc - 1;
            DoubleMatrix<BioMaterial, Integer> v = svd.getVMatrix();
            if ( v != null && colIdx >= 0 && colIdx < v.columns() ) {
                List<BioAssay> assays = svd.getBioAssays();
                for ( int i = 0; i < assays.size() && i < v.rows(); i++ ) {
                    BioAssay ba = assays.get( i );
                    if ( ba.getId() != null ) {
                        scores.put( ba.getId(), v.get( i, colIdx ) );
                    }
                }
            }

            return new PcLoadingsValueObject( pc, rows, scores );
        }

        @Value
        public static class Row {
            @Nullable Long designElementId;
            @Nullable String designElementName;
            /**
             * Reserved — gene-symbol enrichment via the CompositeSequence → Gene mapping path
             * is deferred. Currently always null.
             */
            @Nullable String geneSymbol;
            double loading;
        }
    }

    private <T> QueriedAndFilteredAndInferredResponseDataObject<T> all( List<T> results, String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort by, Collection<OntologyTerm> inferredTerms ) {
        return new QueriedAndFilteredAndInferredResponseDataObject<>( results, query, filters, groupBy, by, inferredTerms );
    }

    private <T> QueriedAndFilteredAndInferredAndLimitedResponseDataObject<T> top( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, @Nullable Integer limit, Collection<OntologyTerm> inferredTerms ) {
        return new QueriedAndFilteredAndInferredAndLimitedResponseDataObject<>( payload, query, filters, groupBy, sort, limit, inferredTerms );
    }

    private <T> FilteredAndInferredAndPaginatedResponseDataObject<T> paginate( Slice<T> payload, @Nullable Filters filters, String[] groupBy, Collection<OntologyTerm> inferredTerms ) throws NotFoundException {
        return new FilteredAndInferredAndPaginatedResponseDataObject<>( payload, filters, groupBy, inferredTerms );
    }

    private <T> QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<T> paginate( Slice<T> payload, String query, Filters filters, String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
        return new QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<>( payload, query, filters, groupBy, inferredTerms );
    }

    private <T> FilteredAndInferredAndPaginatedResponseDataObject<T> paginate( Responders.FilterMethod<T> filterMethod, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, int offset, int limit, Collection<OntologyTerm> inferredTerms ) throws NotFoundException {
        return paginate( filterMethod.load( filters, sort, offset, limit ), filters, groupBy, inferredTerms );
    }

    @Getter
    public static class QueriedAndFilteredAndInferredResponseDataObject<T> extends QueriedAndFilteredResponseDataObject<T> {

        private final List<CharacteristicValueObject> inferredTerms;

        public QueriedAndFilteredAndInferredResponseDataObject( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, Collection<OntologyTerm> inferredTerms ) {
            super( payload, query, filters, groupBy, sort );
            this.inferredTerms = inferredTerms.stream()
                    .map( t -> new CharacteristicValueObject( t.getLabel(), t.getUri() ) )
                    .collect( Collectors.toList() );
        }
    }

    @Getter
    public static class QueriedAndFilteredAndInferredAndLimitedResponseDataObject<T> extends QueriedAndFilteredAndLimitedResponseDataObject<T> {

        private final List<CharacteristicValueObject> inferredTerms;

        public QueriedAndFilteredAndInferredAndLimitedResponseDataObject( List<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, @Nullable Sort sort, @Nullable Integer limit, Collection<OntologyTerm> inferredTerms ) {
            super( payload, query, filters, groupBy, sort, limit );
            this.inferredTerms = inferredTerms.stream()
                    .map( t -> new CharacteristicValueObject( t.getLabel(), t.getUri() ) )
                    .collect( Collectors.toList() );
        }
    }

    @Getter
    public static class FilteredAndInferredAndPaginatedResponseDataObject<T> extends FilteredAndPaginatedResponseDataObject<T> {

        private final List<CharacteristicValueObject> inferredTerms;

        public FilteredAndInferredAndPaginatedResponseDataObject( Slice<T> payload, @Nullable Filters filters, @Nullable String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, filters, groupBy );
            this.inferredTerms = inferredTerms.stream()
                    .map( t -> new CharacteristicValueObject( t.getLabel(), t.getUri() ) )
                    .collect( Collectors.toList() );
        }
    }

    /**
     * Cursor-mode counterpart to {@link FilteredAndInferredAndPaginatedResponseDataObject}.
     * Drops {@code offset}; adds {@code nextCursor} / {@code prevCursor}; keeps the echoed
     * {@code filter} and {@code inferredTerms} fields. See {@code CURSOR_PAGINATION_STEP1_PLAN.md}
     * step 1t (the EE-targeted twin of step 1h's {@link FilteredAndCursorPaginatedResponseDataObject}
     * shape).
     */
    @Getter
    public static class FilteredAndInferredAndCursorPaginatedResponseDataObject<T> extends FilteredAndCursorPaginatedResponseDataObject<T> {

        private final List<CharacteristicValueObject> inferredTerms;

        public FilteredAndInferredAndCursorPaginatedResponseDataObject( CursorPage<T> payload, @Nullable Filters filters, @Nullable String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, filters, groupBy );
            this.inferredTerms = inferredTerms.stream()
                    .map( t -> new CharacteristicValueObject( t.getLabel(), t.getUri() ) )
                    .collect( Collectors.toList() );
        }
    }

    @Getter
    public static class QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<T> extends QueriedAndFilteredAndPaginatedResponseDataObject<T> {

        private final List<CharacteristicValueObject> inferredTerms;

        public QueriedAndFilteredAndInferredAndPaginatedResponseDataObject( Slice<T> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, query, filters, groupBy );
            this.inferredTerms = inferredTerms.stream()
                    .map( t -> new CharacteristicValueObject( t.getLabel(), t.getUri() ) )
                    .collect( Collectors.toList() );
        }
    }

    /**
     * Cursor-mode counterpart to {@link QueriedAndFilteredAndInferredAndPaginatedResponseDataObject}.
     * Drops {@code offset}; adds {@code nextCursor} / {@code prevCursor}; keeps the echoed
     * {@code query}, {@code filter} and {@code inferredTerms} fields. See {@code CURSOR_PAGINATION_STEP1_PLAN.md}
     * step 1v (the /datasets/expressions/genes/{gene} pair). Mirrors the existing
     * {@link FilteredAndInferredAndCursorPaginatedResponseDataObject} pattern from step 1t,
     * adding the {@code query} field on top.
     */
    @Getter
    public static class QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<T> extends QueriedAndFilteredAndCursorPaginatedResponseDataObject<T> {

        private final List<CharacteristicValueObject> inferredTerms;

        public QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject( CursorPage<T> payload, @Nullable String query, @Nullable Filters filters, @Nullable String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, query, filters, groupBy );
            this.inferredTerms = inferredTerms.stream()
                    .map( t -> new CharacteristicValueObject( t.getLabel(), t.getUri() ) )
                    .collect( Collectors.toList() );
        }
    }

    /**
     * TODO (Phase 3 cleanup leftover): cannot fold into a method-level
     * {@code @PreAuthorize("hasAuthority('GROUP_ADMIN')")} because the four call-sites
     * ({@link #getDatasetProcessedExpression}, {@link #getDatasetRawExpression},
     * {@link #getDatasetSingleCellExpression}, {@link #getDatasetDesign}) gate this admin
     * requirement on {@code force == true} — the endpoints must remain callable by non-admins
     * when {@code force} is false. Refactoring would require splitting each endpoint into
     * admin-only and public variants, which inflates the public REST surface. Leaving the
     * conditional manual check in place is the least-bad option until the {@code force}
     * parameter is itself reconsidered.
     */
    private void checkIsAdmin() {
        accessDecisionManager.decide( SecurityContextHolder.getContext().getAuthentication(), null, Collections.singletonList( new SecurityConfig( "GROUP_ADMIN" ) ) );
    }

    private List<Long> sliceIds( List<Long> ids, int offset, int limit ) {
        if ( offset < ids.size() ) {
            return ids.subList( offset, Math.min( offset + limit, ids.size() ) );
        } else {
            return Collections.emptyList();
        }
    }

    public static class ResponseDataObjectCellTypeAssignmentValueObject extends ResponseDataObject<CellTypeAssignmentValueObject> {

        public ResponseDataObjectCellTypeAssignmentValueObject( CellTypeAssignmentValueObject payload ) {
            super( payload );
        }
    }

    public static class ResponseDataObjectListCellLevelCharacteristicsValueObject extends ResponseDataObject<List<CellLevelCharacteristicsValueObject>> {

        public ResponseDataObjectListCellLevelCharacteristicsValueObject( List<CellLevelCharacteristicsValueObject> payload ) {
            super( payload );
        }
    }

    public static class ResponseDataObjectSingleCellDimensionValueObject extends ResponseDataObject<SingleCellDimensionValueObject> {

        public ResponseDataObjectSingleCellDimensionValueObject( SingleCellDimensionValueObject payload ) {
            super( payload );
        }
    }

    /**
     * Legacy-mode response shape for {@link #getDatasetSamples}. Exists solely to bind the {@code data}
     * type argument so the OpenAPI generator emits the full {@link BioAssayValueObject} structure; a raw
     * {@code ResponseDataObject.class} reference in the {@code oneOf} erases it to an untyped object.
     */
    public static class ResponseDataObjectListBioAssayValueObject extends ResponseDataObject<List<BioAssayValueObject>> {

        public ResponseDataObjectListBioAssayValueObject( List<BioAssayValueObject> payload ) {
            super( payload );
        }
    }

    /**
     * Cursor-mode response shape for {@link #getDatasetSamples}. Binds the element type so the generated
     * schema documents both the {@link BioAssayValueObject} {@code data} array and the cursor envelope.
     */
    public static class CursorPaginatedResponseDataObjectBioAssayValueObject extends CursorPaginatedResponseDataObject<BioAssayValueObject> {

        public CursorPaginatedResponseDataObjectBioAssayValueObject( CursorPage<BioAssayValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    // --- OpenAPI doc-only wrappers: bind the data type argument for the oneOf response schemas of the
    // --- cursor-paginated dataset endpoints. Raw generic references (e.g. ResponseDataObject.class) erase
    // --- the data element type in the generated spec; these concrete subclasses restore it.

    /** Legacy shape for {@link #getDatasetsByIds} / {@link #getBlacklistedDatasets}. */
    public static class FilteredAndInferredAndPaginatedResponseDataObjectExpressionExperimentValueObject extends FilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> {

        public FilteredAndInferredAndPaginatedResponseDataObjectExpressionExperimentValueObject( Slice<ExpressionExperimentValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, filters, groupBy, inferredTerms );
        }
    }

    /** Cursor shape for {@link #getDatasetsByIds} / {@link #getBlacklistedDatasets}. */
    public static class FilteredAndInferredAndCursorPaginatedResponseDataObjectExpressionExperimentValueObject extends FilteredAndInferredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> {

        public FilteredAndInferredAndCursorPaginatedResponseDataObjectExpressionExperimentValueObject( CursorPage<ExpressionExperimentValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, filters, groupBy, inferredTerms );
        }
    }

    /** Legacy shape for {@link #getDatasetTickets}. */
    public static class ResponseDataObjectListTicketValueObject extends ResponseDataObject<List<TicketValueObject>> {

        public ResponseDataObjectListTicketValueObject( List<TicketValueObject> payload ) {
            super( payload );
        }
    }

    /** Cursor shape for {@link #getDatasetTickets}. */
    public static class CursorPaginatedResponseDataObjectTicketValueObject extends CursorPaginatedResponseDataObject<TicketValueObject> {

        public CursorPaginatedResponseDataObjectTicketValueObject( CursorPage<TicketValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Legacy full-fidelity shape for {@link #getDatasetAuditEvents} (compact=false). */
    public static class ResponseDataObjectListAuditEventValueObject extends ResponseDataObject<List<AuditEventValueObject>> {

        public ResponseDataObjectListAuditEventValueObject( List<AuditEventValueObject> payload ) {
            super( payload );
        }
    }

    /** Cursor full-fidelity shape for {@link #getDatasetAuditEvents} (compact=false). */
    public static class CursorPaginatedResponseDataObjectAuditEventValueObject extends CursorPaginatedResponseDataObject<AuditEventValueObject> {

        public CursorPaginatedResponseDataObjectAuditEventValueObject( CursorPage<AuditEventValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Legacy collapsed shape for {@link #getDatasetAuditEvents} (compact=true). */
    public static class ResponseDataObjectListCompactAuditEventValueObject extends ResponseDataObject<List<CompactAuditEventValueObject>> {

        public ResponseDataObjectListCompactAuditEventValueObject( List<CompactAuditEventValueObject> payload ) {
            super( payload );
        }
    }

    /** Cursor collapsed shape for {@link #getDatasetAuditEvents} (compact=true). */
    public static class CursorPaginatedResponseDataObjectCompactAuditEventValueObject extends CursorPaginatedResponseDataObject<CompactAuditEventValueObject> {

        public CursorPaginatedResponseDataObjectCompactAuditEventValueObject( CursorPage<CompactAuditEventValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Legacy shape for {@link #getDatasetsExpressionLevelsForGene} / {@link #getDatasetsExpressionLevelsForGeneInTaxon}. */
    public static class QueriedAndFilteredAndInferredAndPaginatedResponseDataObjectExperimentExpressionLevelsValueObject extends QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> {

        public QueriedAndFilteredAndInferredAndPaginatedResponseDataObjectExperimentExpressionLevelsValueObject( Slice<ExperimentExpressionLevelsValueObject> payload, @Nullable String query, @Nullable Filters filters, String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, query, filters, groupBy, inferredTerms );
        }
    }

    /** Cursor shape for {@link #getDatasetsExpressionLevelsForGene} / {@link #getDatasetsExpressionLevelsForGeneInTaxon}. */
    public static class QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObjectExperimentExpressionLevelsValueObject extends QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> {

        public QueriedAndFilteredAndInferredAndCursorPaginatedResponseDataObjectExperimentExpressionLevelsValueObject( CursorPage<ExperimentExpressionLevelsValueObject> payload, @Nullable String query, @Nullable Filters filters, @Nullable String[] groupBy, Collection<OntologyTerm> inferredTerms ) {
            super( payload, query, filters, groupBy, inferredTerms );
        }
    }
}
