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
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchConfound;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.filter.NoRowsLeftAfterFilteringException;
import ubic.gemma.core.analysis.preprocess.svd.SVDResult;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.service.DifferentialExpressionAnalysisResultListFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionExperimentDataFileType;
import ubic.gemma.core.loader.expression.singleCell.metadata.CellLevelCharacteristicsWriter;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.DefaultHighlighter;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.lucene.SimpleMarkdownFormatter;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.analysis.CellTypeAssignmentValueObject;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
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
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.annotations.CacheControl;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.util.*;
import ubic.gemma.rest.util.args.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import static ubic.gemma.rest.util.Responders.respond;
import static ubic.gemma.rest.util.Responders.sendfile;

/**
 * RESTful interface for datasets.
 *
 * @author tesarst
 */
@Service
@Path("/datasets")
@CommonsLog
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

    @Context
    private UriInfo uriInfo;

    @ParametersAreNonnullByDefault
    private class Highlighter extends DefaultHighlighter {

        private final Set<Long> documentIdsToHighlight;

        private Highlighter( Set<Long> documentIdsToHighlight ) {
            super( new SimpleMarkdownFormatter() );
            this.documentIdsToHighlight = documentIdsToHighlight;
        }

        @Override
        public Map<String, String> highlightTerm( @Nullable String termUri, String termLabel, String field ) {
            URI reconstructedUri = uriInfo.getBaseUriBuilder()
                    .scheme( null ).host( null ).port( -1 )
                    // replace the query with the term URI and only retain the filter
                    .replaceQueryParam( "query", termUri != null ? termUri : termLabel )
                    .replaceQueryParam( "offset" )
                    .replaceQueryParam( "limit" )
                    .replaceQueryParam( "sort" )
                    .build();
            return Collections.singletonMap( field, String.format( "**[%s](%s)**", termLabel, reconstructedUri ) );
        }

        @Override
        public Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer ) {
            long id = Long.parseLong( document.get( "id" ) );
            // TODO: maybe use a filter in the Lucene query?
            if ( !documentIdsToHighlight.contains( id ) ) {
                return Collections.emptyMap();
            }
            return super.highlightDocument( document, highlighter, analyzer );
        }
    }

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

            // now highlight the results in the slice
            List<SearchResult<ExpressionExperiment>> results = datasetArgService.getResultsForSearchQuery( query, new Highlighter( new HashSet<>( idsSlice ) ), warnings );
            Map<Long, SearchResult<ExpressionExperiment>> resultById = results.stream().collect( Collectors.toMap( SearchResult::getResultId, e -> e ) );

            List<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjectsByIdsWithRelationsAndCache( idsSlice );
            payload = new Slice<>( vos, sort, offset, limit, ( long ) ids.size() )
                    .map( vo -> new ExpressionExperimentWithSearchResultValueObject( vo, resultById.get( vo.getId() ) ) );
        } else {
            Sort sort = sortArg != null ? datasetArgService.getSort( sortArg ) : datasetArgService.getSort( SortArg.valueOf( "+id" ) );
            payload = expressionExperimentService.loadValueObjectsWithCache( filters, sort, offset, limit )
                    .map( vo -> new ExpressionExperimentWithSearchResultValueObject( vo, null ) );
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

        public ExpressionExperimentWithSearchResultValueObject( ExpressionExperimentValueObject vo, @Nullable SearchResult<ExpressionExperiment> result ) {
            super( vo );
            if ( result != null ) {
                this.searchResult = new SearchWebService.SearchResultValueObject<>( result.withResultObject( null ) );
            } else {
                this.searchResult = null;
            }
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
    @Secured("GROUP_ADMIN")
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
            @Parameter(description = "Retain terms mentioned in the `filter` parameter even if they don't meet the `minFrequency` threshold or are excluded via `excludedCategories` or `excludedTerms`.", hidden = true) @QueryParam("retainMentionedTerms") @DefaultValue("false") Boolean retainMentionedTerms
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
    @Path("/annotations/refresh")
    @Secured("GROUP_ADMIN")
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
            @Parameter(description = "Retain terms mentioned in the `filter` parameter even if they don't meet the `minFrequency` threshold or are excluded via `excludedCategories` or `excludedTerms`.", hidden = true) @QueryParam("retainMentionedTerms") @DefaultValue("false") Boolean retainMentionedTerms
    ) {
        tableMaintenanceUtil.evictEe2CQueryCache();
        return Response.created( URI.create( "/datasets/annotations" ) )
                .entity( getDatasetsAnnotationsUsageStatistics( query, filter, exclude, limitArg, minFrequency, category, excludedCategoryUris, excludeFreeTextCategories, excludedTermUris, excludeFreeTextTerms, excludeUncategorizedTerms, retainMentionedTerms ) )
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
    @Operation(summary = "Retrieve datasets by their identifiers")
    public FilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> getDatasetsByIds( // Params:
            @PathParam("dataset") DatasetArrayArg datasetsArg, // Optional
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, null, inferredTerms ).and( datasetArgService.getFilters( datasetsArg ) );
        return paginate( expressionExperimentService::loadValueObjectsWithCache, filters, new String[] { "id" },
                datasetArgService.getSort( sort ), offset.getValue(), limit.getValue(), inferredTerms );
    }

    /**
     * Browse blacklisted datasets.
     */
    @GET
    @Path("/blacklisted")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured("GROUP_ADMIN")
    @Operation(summary = "Retrieve all blacklisted datasets", hidden = true)
    public FilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> getBlacklistedDatasets(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        return paginate( expressionExperimentService::loadBlacklistedValueObjects,
                datasetArgService.getFilters( filterArg, null, inferredTerms ), new String[] { "id" }, datasetArgService.getSort( sortArg ),
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
    @Operation(summary = "Retrieve the samples of a dataset", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<BioAssayValueObject>> getDatasetSamples( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg,
            @QueryParam("useProcessedQuantitationType") boolean useProcessedQuantitationType
    ) {
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
     * Retrieves the differential analysis results for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/analyses/differential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve annotations and surface level stats for a dataset's differential analyses", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<DifferentialExpressionAnalysisValueObject>> getDatasetDifferentialExpressionAnalyses( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg // Optional, default 20
    ) {
        List<DifferentialExpressionAnalysisValueObject> result;
        Long eeId = datasetArgService.getEntity( datasetArg ).getId();
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> map = differentialExpressionAnalysisService.getAnalysesByExperiment( Collections.singleton( eeId ), offset, limit );
        if ( map == null || map.isEmpty() ) {
            result = Collections.emptyList();
        } else {
            result = map.get( map.keySet().iterator().next() );
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
                            @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8 + "; q=0.9", schema = @Schema(type = "string", format = "binary"))
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
                            @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, schema = @Schema(type = "string", format = "binary"))
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
                .findByGeneAndExperimentAnalyzed( gene, sliceIds( ids, offset, limit ), sourceExperimentIdMap, experimentAnalyzedIdMap, baselineMap, threshold, false, true ).stream()
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
        List<DifferentialExpressionAnalysisResult> payload = differentialExpressionResultService.findByGeneAndExperimentAnalyzed( gene, ids, sourceExperimentIdMap, experimentAnalyzedIdMap, baselineMap, threshold, false, false ).stream()
                .sorted( Comparator.comparing( ( DifferentialExpressionAnalysisResult r ) -> sourceExperimentIdMap.get( r ) )
                        .thenComparing( ( DifferentialExpressionAnalysisResult r ) -> experimentAnalyzedIdMap.get( r ) )
                        .thenComparing( ( DifferentialExpressionAnalysisResult r ) -> r.getResultSet().getId() ) )
                .collect( Collectors.toList() );
        // obtain result set IDs of results that lack baselines (i.e. for interactions)
        Set<ExpressionAnalysisResultSet> missingBaselines = payload.stream()
                .filter( vo -> baselineMap.get( vo ) == null )
                .map( DifferentialExpressionAnalysisResult::getResultSet )
                .collect( toIdentifiableSet() );
        Map<ExpressionAnalysisResultSet, Baseline> b = expressionAnalysisResultSetService.getBaselinesForInteractions( missingBaselines, false );
        for ( DifferentialExpressionAnalysisResult r : payload ) {
            Baseline b2 = b.get( r.getResultSet() );
            if ( b2 == null ) {
                continue;
            }
            baselineMap.put( r, b2 );
        }
        return output -> differentialExpressionAnalysisResultListFileService.writeTsv( payload, gene, sourceExperimentIdMap, experimentAnalyzedIdMap, baselineMap, new OutputStreamWriter( output ) );
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
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        return respond( datasetArgService.getAnnotations( datasetArg ) );
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
                writer.setUseBioAssayId( useBioAssayId );
                writer.write( dimension, new OutputStreamWriter( output, StandardCharsets.UTF_8 ) );
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
                dimension = singleCellExpressionExperimentService.getSingleCellDimensionWithoutCellIds( ee, qt, includeBioAssays, true, true, true, includeIndices );
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
            @Parameter(description = "The name of of the cell type assignment to retrieve. If left unset, this the preferred one is returned.") @QueryParam("cellTypeAssignment") String ctaName,
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
        if ( ctaName != null ) {
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
                CellLevelCharacteristicsWriter writer = new CellLevelCharacteristicsWriter();
                writer.setUseBioAssayId( useBioAssayId );
                writer.write( cta, dimension, new OutputStreamWriter( output, StandardCharsets.UTF_8 ) );
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
                CellLevelCharacteristicsWriter writer = new CellLevelCharacteristicsWriter();
                writer.write( dimension.getCellLevelCharacteristics(), dimension, new OutputStreamWriter( output, StandardCharsets.UTF_8 ) );
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
                            schema = @Schema(type = "string", format = "binary"),
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
                            schema = @Schema(type = "string", format = "binary"),
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
        try ( LockedPath p = expressionDataFileService.writeOrLocateProcessedDataFile( ee, filtered, force, 5, TimeUnit.SECONDS )
                .orElseThrow( () -> new NotFoundException( ee.getShortName() + " does not have any processed vectors." ) ) ) {
            String filename = download ? p.getPath().getFileName().toString() : FilenameUtils.removeExtension( p.getPath().getFileName().toString() );
            return sendfile( p.getPath() )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + filename + "\"" )
                    .build();
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( "Processed data for " + ee.getShortName() + " is still being generated.", 30L, e );
        } catch ( IOException e ) {
            log.error( "Failed to create processed expression data for " + ee + ", will have to stream it as a fallback.", e );
            String filename = download ? getDataOutputFilename( ee, filtered, TABULAR_BULK_DATA_FILE_SUFFIX ) : FilenameUtils.removeExtension( getDataOutputFilename( ee, filtered, TABULAR_BULK_DATA_FILE_SUFFIX ) );
            return Response.ok( ( StreamingOutput ) output -> {
                        try {
                            expressionDataFileService.writeProcessedExpressionData( ee, filtered, null, new OutputStreamWriter( new GZIPOutputStream( output ), StandardCharsets.UTF_8 ), true );
                        } catch ( FilteringException ex ) {
                            // this is a bit unfortunate, because it's too late for producing a 204 error
                            throw new RuntimeException( ex );
                        }
                    } )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + filename + "\"" )
                    .build();
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException( e );
        } catch ( NoRowsLeftAfterFilteringException e ) {
            return Response.noContent().build();
        } catch ( FilteringException e ) {
            throw new InternalServerErrorException( String.format( "Filtering of dataset %s failed.", ee.getShortName() ), e );
        }
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
                            schema = @Schema(type = "string", format = "binary"),
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
        try ( LockedPath p = expressionDataFileService.writeOrLocateRawExpressionDataFile( ee, qt, force, 5, TimeUnit.SECONDS ) ) {
            String filename = download ? p.getPath().getFileName().toString() : FilenameUtils.removeExtension( p.getPath().getFileName().toString() );
            return sendfile( p.getPath() )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + filename + "\"" )
                    .build();
        } catch ( TimeoutException e ) {
            // file is being written, recommend to the user to wait a lils.copy( is, entityStittle bit
            throw new ServiceUnavailableException( "Raw data for " + qt + " is still being generated.", 30L, e );
        } catch ( IOException e ) {
            log.error( "Failed to write raw expression data for " + qt + " to disk, will resort to stream it.", e );
            String filename = getDataOutputFilename( ee, qt, TABULAR_BULK_DATA_FILE_SUFFIX );
            return Response.ok( ( StreamingOutput ) output -> expressionDataFileService.writeRawExpressionData( ee, qt, null, new OutputStreamWriter( new GZIPOutputStream( output ), StandardCharsets.UTF_8 ), true ) )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + ( download ? filename : FilenameUtils.removeExtension( filename ) ) + "\"" )
                    .build();
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
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
                                    @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8 + "; q=0.9", schema = @Schema(type = "string", format = "binary"),
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
                    expressionDataFileService.writeOrLocateTabularSingleCellExpressionDataAsync( ee, qt, 30, false, force, false );
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
        return Response.ok( ( StreamingOutput ) stream -> expressionDataFileService.writeTabularSingleCellExpressionData( ee, qt, null, 30, false, new OutputStreamWriter( new GZIPOutputStream( stream ), StandardCharsets.UTF_8 ), true ) )
                .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                .header( "Content-Disposition", "attachment; filename=\"" + ( download ? filename : FilenameUtils.removeExtension( filename ) ) + "\"" )
                .build();
    }

    /**
     * Retrieves the design for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GZIP(mediaTypes = TEXT_TAB_SEPARATED_VALUES_UTF8, alreadyCompressed = true)
    @GET
    @Path("/{dataset}/design")
    @Produces(TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve the design of a dataset", responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = TEXT_TAB_SEPARATED_VALUES_UTF8, schema = @Schema(type = "string", format = "binary"),
                    examples = @ExampleObject("classpath:/restapidocs/examples/dataset-design.tsv"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetDesign( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @Parameter(hidden = true) @QueryParam("download") @DefaultValue("false") Boolean download,
            @Parameter(hidden = true) @QueryParam("force") @DefaultValue("false") Boolean force
    ) {
        if ( force ) {
            checkIsAdmin();
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        try ( LockedPath file = expressionDataFileService.writeOrLocateDesignFile( ee, force, 5, TimeUnit.SECONDS )
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
            String filename = getDesignFileName( ee );
            return Response.ok( ( StreamingOutput ) stream -> expressionDataFileService.writeDesignMatrix( ee, new OutputStreamWriter( new GZIPOutputStream( stream ), StandardCharsets.UTF_8 ) ) )
                    .type( download ? MediaType.APPLICATION_OCTET_STREAM_TYPE : TEXT_TAB_SEPARATED_VALUES_UTF8_TYPE )
                    .header( "Content-Disposition", "attachment; filename=\"" + ( download ? filename : FilenameUtils.removeExtension( filename ) ) + "\"" )
                    .build();
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * Indicate if the experiment has batch information.
     * <p>
     * This does not imply that the batch information is usable. This will be true even if there is only one batch. It
     * does not reflect the presence or absence of a batch effect.
     */
    @GET
    @Secured("GROUP_ADMIN")
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
    @Secured("GROUP_ADMIN")
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
            this.factor = new ExperimentalFactorValueObject( batchConfound.getEf(), false );
            this.chiSquared = batchConfound.getChiSquare();
            this.df = batchConfound.getDf();
            this.pvalue = batchConfound.getP();
            this.numberOfBatches = batchConfound.getNumBatches();
        }
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
     * Retrieve the expression levels of a given gene across all datasets.
     */
    @GET
    @Path("/expressions/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression levels of a gene among datasets matching the provided query and filter")
    public PaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> getDatasetsExpressionLevelsForGene(
            @PathParam("gene") GeneArg<?> geneArg,
            @QueryParam("query") QueryArg queryArg,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned

    ) {
        return getDatasetsExpressionLevelsForGeneInTaxonInternal( geneArgService.getEntity( geneArg ), queryArg, filterArg, offsetArg, limitArg, keepNonSpecific, consolidate );
    }

    /**
     * Retrieve the expression levels of a given gene and taxon across all datasets.
     */
    @GET
    @Path("/expressions/taxa/{taxon}/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression levels of a gene and taxa among datasets matching the provided query and filter")
    public PaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> getDatasetsExpressionLevelsForGeneInTaxon(
            @PathParam("taxon") TaxonArg<?> taxonArg,
            @PathParam("gene") GeneArg<?> geneArg,
            @QueryParam("query") QueryArg queryArg,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned

    ) {
        return getDatasetsExpressionLevelsForGeneInTaxonInternal( geneArgService.getEntityWithTaxon( geneArg, taxonArgService.getEntity( taxonArg ) ), queryArg, filterArg, offsetArg, limitArg, keepNonSpecific, consolidate );
    }

    private QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<ExperimentExpressionLevelsValueObject> getDatasetsExpressionLevelsForGeneInTaxonInternal( Gene gene, @Nullable QueryArg queryArg, FilterArg<ExpressionExperiment> filterArg, OffsetArg offsetArg, LimitArg limitArg, boolean keepNonSpecific, @Nullable ExpLevelConsolidationArg consolidate ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filter = datasetArgService.getFilters( filterArg, null, inferredTerms );
        Sort sort = datasetArgService.getSort( SortArg.valueOf( "+id" ) );
        List<Long> datasetIds = expressionExperimentService.loadIdsWithCache( filter, sort );
        LinkedHashSet<Throwable> warnings = new LinkedHashSet<>();
        if ( queryArg != null ) {
            datasetIds.retainAll( datasetArgService.getIdsForSearchQuery( queryArg, warnings ) );
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
    @Operation(summary = "Retrieve the expression levels of a set of datasets subject to a threshold on their differential expressions")
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
    @Secured("GROUP_ADMIN")
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

    @GET
    @Path("/{dataset}/subSets/{subSet}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Obtain the samples of a specific subset of a dataset")
    public ResponseDataObject<List<BioAssayValueObject>> getDatasetSubSetSamples(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("subSet") Long subSetId
    ) {
        return respond( datasetArgService.getSubSetSamples( datasetArg, subSetId ) );
    }

    /**
     * A group of subsets, logically organized by a {@link BioAssayDimension}.
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
}
