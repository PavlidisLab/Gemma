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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchConfound;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.filter.NoRowsLeftAfterFilteringException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.DefaultHighlighter;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.lucene.SimpleMarkdownFormatter;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for datasets.
 *
 * @author tesarst
 */
@Service
@Path("/datasets")
@CommonsLog
public class DatasetsWebService {

    private static final String ERROR_DATA_FILE_NOT_AVAILABLE = "Data file for experiment %s can not be created.";
    private static final String ERROR_DESIGN_FILE_NOT_AVAILABLE = "Design file for experiment %s can not be created.";

    private static final String SEARCH_TIMEOUT_DESCRIPTION = "The search has timed out. This can only occur if the `search` parameter is provided. It can generally be resolved by reattempting the search 30 seconds later. Lookup the `Retry-After` header for the recommended delay.";

    private static final int MAX_DATASETS_CATEGORIES = 200;
    private static final int MAX_DATASETS_ANNOTATIONS = 5000;

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
    private GeneArgService geneArgService;
    @Autowired
    private TaxonArgService taxonArgService;
    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;
    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

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
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<ExpressionExperimentWithSearchResultValueObject> getDatasets( // Params:
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg // Optional, default +id
    ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filterArg, null, inferredTerms );
        Sort sort = datasetArgService.getSort( sortArg );
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        Slice<ExpressionExperimentWithSearchResultValueObject> payload;
        if ( query != null ) {
            List<Long> ids = new ArrayList<>( expressionExperimentService.loadIdsWithCache( filters, sort ) );
            Map<Long, Double> scoreById = new HashMap<>();
            ids.retainAll( datasetArgService.getIdsForSearchQuery( query, scoreById ) );
            // sort is stable, so the order of IDs with the same score is preserved
            ids.sort( Comparator.comparingDouble( i -> -scoreById.get( i ) ) );

            // slice the ranked IDs
            List<Long> idsSlice;
            if ( offset < ids.size() ) {
                idsSlice = ids.subList( offset, Math.min( offset + limit, ids.size() ) );
            } else {
                idsSlice = Collections.emptyList();
            }

            // now highlight the results in the slice
            List<SearchResult<ExpressionExperiment>> results = datasetArgService.getResultsForSearchQuery( query, new Highlighter( new HashSet<>( idsSlice ) ) );
            Map<Long, SearchResult<ExpressionExperiment>> resultById = results.stream().collect( Collectors.toMap( SearchResult::getResultId, e -> e ) );

            List<ExpressionExperimentValueObject> vos = expressionExperimentService.loadValueObjectsByIdsWithRelationsAndCache( idsSlice );
            payload = new Slice<>( vos, Sort.by( null, "searchResult.score", Sort.Direction.DESC ), offset, limit, ( long ) ids.size() )
                    .map( vo -> new ExpressionExperimentWithSearchResultValueObject( vo, resultById.get( vo.getId() ) ) );
        } else {
            payload = expressionExperimentService.loadValueObjectsWithCache( filters, sort, offset, limit )
                    .map( vo -> new ExpressionExperimentWithSearchResultValueObject( vo, null ) );
        }
        return paginate( payload, query != null ? query.getValue() : null, filters, new String[] { "id" }, inferredTerms );
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
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public ResponseDataObject<Long> getNumberOfDatasets(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter
    ) {
        Filters filters = datasetArgService.getFilters( filter );
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query );
        } else {
            extraIds = null;
        }
        return respond( expressionExperimentService.countWithCache( filters, extraIds ) );
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
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public QueriedAndFilteredAndInferredAndLimitedResponseDataObject<ArrayDesignWithUsageStatisticsValueObject> getDatasetsPlatformsUsageStatistics(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("limit") @DefaultValue("50") LimitArg limit
    ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, null, inferredTerms );
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query );
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
        return top( results, query != null ? query.getValue() : null, filters, new String[] { "id" }, Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ), l, inferredTerms );
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
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
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
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query );
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
        return top( results, query != null ? query.getValue() : null, filters, new String[] { "classUri", "className" }, Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ), maxResults, inferredTerms );
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

    private static final Set<String> ALLOWED_FIELDS = Collections.singleton( "parentTerms" );

    @GET
    @GZIP
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Path("/annotations")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve usage statistics of annotations among datasets matching the provided query and filter",
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.",
            responses = {
                    @ApiResponse(useReturnTypeSchema = true, content = @Content()),
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
            @Parameter(description = "Retain terms mentioned in the `filter` parameter even if they don't meet the `minFrequency` threshold or are excluded via `excludedCategories` or `excludedTerms`.", hidden = true) @QueryParam("retainMentionedTerms") @DefaultValue("false") Boolean retainMentionedTerms ) {
        boolean excludeParentTerms = getExcludedFields( exclude ).contains( "parentTerms" );
        // if a minFrequency is requested, use the hard cap, otherwise use 100 as a reasonable default
        int limit = limitArg != null ? limitArg.getValue( MAX_DATASETS_ANNOTATIONS ) : minFrequency != null ? MAX_DATASETS_ANNOTATIONS : 100;
        if ( minFrequency != null && minFrequency < 0 ) {
            throw new BadRequestException( "Minimum frequency must be positive." );
        }
        // ensure that implied terms are retained in the usage frequency
        Collection<OntologyTerm> mentionedTerms = retainMentionedTerms ? new HashSet<>() : null;
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, mentionedTerms, inferredTerms );
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query );
        } else {
            extraIds = null;
        }
        if ( category != null && category.isEmpty() ) {
            category = ExpressionExperimentService.UNCATEGORIZED;
        }
        List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> initialResults = expressionExperimentService.getAnnotationsUsageFrequency(
                filters,
                extraIds,
                category,
                datasetArgService.getExcludedUris( excludedCategoryUris, excludeFreeTextCategories, excludeUncategorizedTerms ),
                datasetArgService.getExcludedUris( excludedTermUris, excludeFreeTextTerms, excludeUncategorizedTerms ),
                minFrequency != null ? minFrequency : 0,
                mentionedTerms != null ? mentionedTerms.stream().map( OntologyTerm::getUri ).collect( Collectors.toSet() ) : null,
                limit );
        List<AnnotationWithUsageStatisticsValueObject> results = new ArrayList<>();
        if ( !excludeParentTerms ) {
            int timeoutMs = 30000;
            StopWatch timer = StopWatch.createStarted();
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
        return top(
                results,
                query != null ? query.getValue() : null,
                filters,
                new String[] { "classUri", "className", "termUri", "termName" },
                Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ),
                limit,
                inferredTerms );
    }

    private Set<String> getExcludedFields( @Nullable ExcludeArg<AnnotationWithUsageStatisticsValueObject> exclude ) {
        if ( exclude == null ) {
            return Collections.emptySet();
        }
        if ( !ALLOWED_FIELDS.containsAll( exclude.getValue() ) ) {
            throw new BadRequestException( String.format( "Only the following fields can be excluded: %s.",
                    String.join( ", ", ALLOWED_FIELDS ) ) );
        }
        return new HashSet<>( exclude.getValue() );
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
    @Path("/taxa")
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve taxa usage statistics for datasets matching the provided query and filter", responses = {
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "503", description = SEARCH_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public QueriedAndFilteredAndInferredResponseDataObject<TaxonWithUsageStatisticsValueObject> getDatasetsTaxaUsageStatistics(
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg
    ) {
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filterArg, null, inferredTerms );
        Set<Long> extraIds;
        if ( query != null ) {
            extraIds = datasetArgService.getIdsForSearchQuery( query );
        } else {
            extraIds = null;
        }
        List<TaxonWithUsageStatisticsValueObject> payload = expressionExperimentService.getTaxaUsageFrequency( filters, extraIds )
                .entrySet().stream()
                .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
                .map( e -> new TaxonWithUsageStatisticsValueObject( e.getKey(), e.getValue() ) )
                .collect( Collectors.toList() );
        return all(
                payload,
                query != null ? query.getValue() : null,
                filters,
                new String[] { "id" },
                Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ),
                inferredTerms );
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
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
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
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<BioAssayValueObject>> getDatasetSamples( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
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
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<DifferentialExpressionAnalysisValueObject>> getDatasetDifferentialExpressionAnalyses( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        return respond(
                this.getDiffExVos( datasetArgService.getEntity( datasetArg ).getId(),
                        offset.getValue(), limit.getValue() )
        );
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
    public Response getDatasetDifferentialExpressionAnalysesResultSets(
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

    private static final String GET_DATASET_DIFFERENTIAL_ANALYSIS_EXPRESSION_RESULTS_DESCRIPTION = "Pagination is done on the datasets which may contain more or less results than the value of the `limit` parameter. If a result set has more than one probe for a given gene, the result corresponding to the lowest corrected P-value is retained. Results for non-specific probes (i.e. probes that map to more than one genes) are excluded.";
    private static final String PVALUE_THRESHOLD_DESCRIPTION = "Maximum threshold on the corrected P-value to retain a result. The threshold is inclusive (i.e. 0.05 will match results with corrected P-values lower or equal to 0.05).";

    /**
     * Obtain differential expression analysis results for a given gene.
     */
    @GET
    @GZIP
    @Path("/analyses/differential/results/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the differential expression results for a given gene", description = GET_DATASET_DIFFERENTIAL_ANALYSIS_EXPRESSION_RESULTS_DESCRIPTION)
    public QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultByGeneValueObject> getDatasetsDifferentialAnalysisResultsExpressionForGene(
            @PathParam("gene") GeneArg<?> geneArg,
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit,
            @Parameter(description = PVALUE_THRESHOLD_DESCRIPTION, schema = @Schema(minimum = "0.0", maximum = "1.0")) @QueryParam("threshold") @DefaultValue("1.0") Double threshold
    ) {
        return getDatasetsDifferentialExpressionAnalysisResultsForGeneInternal( null, geneArg, query, filter, offset.getValue(), limit.getValue(), threshold );
    }

    /**
     * Obtain differential expression analysis results for a given gene in a given taxon.
     */
    @GET
    @GZIP
    @Path("/analyses/differential/results/taxa/{taxon}/genes/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the differential expression results for a given gene and taxa")
    public QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultByGeneValueObject> getDatasetsDifferentialAnalysisResultsExpressionForGeneInTaxon(
            @PathParam("taxon") TaxonArg<?> taxonArg,
            @PathParam("gene") GeneArg<?> geneArg,
            @QueryParam("query") QueryArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit,
            @Parameter(description = PVALUE_THRESHOLD_DESCRIPTION, schema = @Schema(minimum = "0.0", maximum = "1.0")) @QueryParam("threshold") @DefaultValue("1.0") Double threshold
    ) {
        return getDatasetsDifferentialExpressionAnalysisResultsForGeneInternal( taxonArg, geneArg, query, filter, offset.getValue(), limit.getValue(), threshold );
    }

    private QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultByGeneValueObject> getDatasetsDifferentialExpressionAnalysisResultsForGeneInternal( @Nullable TaxonArg<?> taxonArg, GeneArg<?> geneArg, QueryArg query, FilterArg<ExpressionExperiment> filter, int offset, int limit, double threshold ) {
        Gene gene;
        if ( taxonArg != null ) {
            Taxon taxon = taxonArgService.getEntity( taxonArg );
            gene = geneArgService.getEntityWithTaxon( geneArg, taxon );
        } else {
            gene = geneArgService.getEntity( geneArg );
        }
        Collection<OntologyTerm> inferredTerms = new HashSet<>();
        Filters filters = datasetArgService.getFilters( filter, null, inferredTerms );
        if ( threshold < 0 || threshold > 1 ) {
            throw new BadRequestException( "The threshold must be in the [0, 1] interval." );
        }
        Set<Long> ids = new HashSet<>( expressionExperimentService.loadIdsWithCache( filters, expressionExperimentService.getSort( "id", Sort.Direction.ASC ) ) );
        if ( query != null ) {
            ids.retainAll( datasetArgService.getIdsForSearchQuery( query ) );
        }
        // slice IDs
        long totalElements = ids.size();
        ids = ids.stream().sorted().skip( offset ).limit( limit ).collect( Collectors.toSet() );
        Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap = new HashMap<>();
        Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap = new HashMap<>();
        List<DifferentialExpressionAnalysisResultByGeneValueObject> payload = differentialExpressionResultService.findByGeneAndExperimentAnalyzed( gene, ids, sourceExperimentIdMap, experimentAnalyzedIdMap, threshold, false ).stream()
                .map( r -> new DifferentialExpressionAnalysisResultByGeneValueObject( r, sourceExperimentIdMap.get( r ), experimentAnalyzedIdMap.get( r ) ) )
                // because of pagination, this is the most "adequate" order
                .sorted( Comparator.comparing( DifferentialExpressionAnalysisResultByGeneValueObject::getSourceExperimentId )
                        .thenComparing( DifferentialExpressionAnalysisResultByGeneValueObject::getExperimentAnalyzedId )
                        .thenComparing( DifferentialExpressionAnalysisResultByGeneValueObject::getResultSetId ) )
                .collect( Collectors.toList() );
        return paginate( new Slice<>( payload, Sort.by( null, "sourceExperimentId", Sort.Direction.ASC, "sourceExperimentId" ), offset, limit, totalElements ),
                query != null ? query.getValue() : null,
                filters,
                new String[] { "sourceExperimentId", "experimentAnalyzedId", "resultSetId" },
                inferredTerms );
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

        public DifferentialExpressionAnalysisResultByGeneValueObject( DifferentialExpressionAnalysisResult result, Long sourceExperimentId, Long experimentAnalyzedId ) {
            super( result );
            this.sourceExperimentId = sourceExperimentId;
            this.experimentAnalyzedId = experimentAnalyzedId;
            this.resultSetId = result.getResultSet().getId();
        }
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
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
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
    @GET
    @Path("/{dataset}/data")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve processed expression data of a dataset",
            description = "This endpoint is deprecated and getDatasetProcessedExpression() should be used instead.",
            responses = {
                    @ApiResponse(content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                            schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "204", description = "The dataset expression matrix is empty."),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) }, deprecated = true)
    public Response getDatasetExpression( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("filter") @DefaultValue("false") Boolean filterData // Optional, default false
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        try {
            return this.outputDataFile( ee, filterData );
        } catch ( NoRowsLeftAfterFilteringException e ) {
            return Response.noContent().build();
        } catch ( FilteringException e ) {
            throw new InternalServerErrorException( String.format( "Filtering of dataset %s failed.", ee.getShortName() ), e );
        } catch ( IOException e ) {
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * Retrieve processed expression data.
     * <p>
     * The payload is transparently compressed via a <code>Content-Encoding</code> header and streamed to avoid dumping
     * the whole payload in memory.
     */
    @GZIP
    @GET
    @Path("/{dataset}/data/processed")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve processed expression data of a dataset", responses = {
            @ApiResponse(content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Either the dataset or the quantitation type do not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetProcessedExpression( @PathParam("dataset") DatasetArg<?> datasetArg ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( !expressionExperimentService.hasProcessedExpressionData( ee ) ) {
            throw new NotFoundException( String.format( "No preferred quantitation type could be for found processed expression data data of %s.", ee ) );
        }
        StreamingOutput stream = ( output ) -> expressionDataFileService.writeProcessedExpressionData( ee, new OutputStreamWriter( output ) );
        return Response.ok( stream )
                .header( "Content-Disposition", String.format( "attachment; filename=%d_%s_expmat.unfilt.data.txt", ee.getId(), ee.getShortName() ) )
                .build();
    }

    /**
     * Retrieve raw expression data.
     * <p>
     * The payload is transparently compressed via a <code>Content-Encoding</code> header and streamed to avoid dumping
     * the whole payload in memory.
     */
    @GZIP
    @GET
    @Path("/{dataset}/data/raw")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve raw expression data of a dataset", responses = {
            @ApiResponse(content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Either the dataset or the quantitation type do not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetRawExpression( @PathParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("quantitationType") QuantitationTypeArg<?> quantitationTypeArg ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt;
        if ( quantitationTypeArg != null ) {
            qt = quantitationTypeArgService.getEntity( quantitationTypeArg, ee, RawExpressionDataVector.class );
        } else {
            qt = expressionExperimentService.getPreferredQuantitationType( ee );
            if ( qt == null ) {
                throw new NotFoundException( String.format( "No preferred quantitation type could be found for raw expression data data of %s.", ee ) );
            }
        }
        StreamingOutput stream = ( output ) -> expressionDataFileService.writeRawExpressionData( ee, qt, new OutputStreamWriter( output ) );
        return Response.ok( stream )
                .header( "Content-Disposition", String.format( "attachment; filename=%d_%s_expmat.unfilt.raw.data.txt", ee.getId(), ee.getShortName() ) )
                .build();
    }

    /**
     * Retrieves the design for the given dataset.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/design")
    @Produces(MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8)
    @Operation(summary = "Retrieve the design of a dataset", responses = {
            @ApiResponse(content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetDesign( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        try {
            return this.outputDesignFile( ee );
        } catch ( IOException e ) {
            throw new InternalServerErrorException( e.getMessage(), e );
        }
    }

    /**
     * Indicate if the experiment has batch information.
     * <p>
     * This does not imply that the batch information is usable. This will be true even if there is only one batch. It
     * does not reflect the presence or absence of a batch effect.
     */
    @GET
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{dataset}/batchInformation")
    @Operation(summary = "Retrieve the batch information of a dataset", hidden = true)
    public ResponseDataObject<BatchInformationValueObject> getDatasetBatchInformation(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        BatchEffectType be = expressionExperimentBatchInformationService.getBatchEffect( ee );
        BatchEffectDetails details = expressionExperimentBatchInformationService.getBatchEffectDetails( ee );
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

        public BatchConfoundValueObject( BatchConfound batchConfound ) {
            this.factor = new ExperimentalFactorValueObject( batchConfound.getEf(), false );
            this.chiSquared = batchConfound.getChiSquare();
            this.df = batchConfound.getDf();
            this.pvalue = batchConfound.getP();
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
            @ApiResponse(useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<SimpleSVDValueObject> getDatasetSvd( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        SVDValueObject svd = svdService.getSvd( datasetArgService.getEntity( datasetArg ).getId() );
        return respond( svd == null ? null : new SimpleSVDValueObject( Arrays.asList( svd.getBioMaterialIds() ), svd.getVariances(), svd.getvMatrix().getRawMatrix() )
        );
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
    @Path("/{datasets}/expressions/taxa/{taxa}/genes/{genes}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression data matrix of a set of datasets and genes")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetExpressionForGenesInTaxa( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @PathParam("taxa") TaxonArg<?> taxonArg, // Required
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
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetExpressionForGenes( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @PathParam("genes") GeneArrayArg genes, // Required
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
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
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetExpressionPca( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @QueryParam("component") @DefaultValue("1") Integer component, // Required, default 1
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
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
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetDifferentialExpression( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @QueryParam("diffExSet") Long diffExSet, // Required
            @QueryParam("threshold") @DefaultValue("1.0") Double threshold, // Optional, default 1.0
            @QueryParam("limit") @DefaultValue("100") LimitArg limit, // Optional, default 100
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
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
            })
    public Response getDataset(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Refresh raw and processed data vectors.") @QueryParam("refreshVectors") @DefaultValue("false") Boolean refreshVectors,
            @Parameter(description = "Refresh experiment reports which include differential expression analyses and batch effects.") @QueryParam("refreshReports") @DefaultValue("false") Boolean refreshReports
    ) {
        Long id = datasetArgService.getEntityId( datasetArg );
        if ( id == null ) {
            throw new NotFoundException( "No dataset matches " + datasetArg );
        }
        ExpressionExperiment ee;
        if ( refreshVectors ) {
            ee = expressionExperimentService.loadAndThawWithRefreshCacheMode( id );
        } else {
            ee = expressionExperimentService.loadAndThawLiteWithRefreshCacheMode( id );
        }
        if ( ee == null ) {
            throw new NotFoundException( "No dataset with ID " + id );
        }
        if ( refreshReports ) {
            expressionExperimentReportService.evictFromCache( id );
        }
        return Response.created( URI.create( "/datasets/" + ee.getId() ) )
                .entity( expressionExperimentService.loadValueObject( ee ) )
                .build();
    }

    private Response outputDataFile( ExpressionExperiment ee, boolean filter ) throws FilteringException, IOException {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeOrLocateProcessedDataFile( ee, false, filter ).orElse( null );
        return this.outputFile( file, DatasetsWebService.ERROR_DATA_FILE_NOT_AVAILABLE, ee.getShortName() );
    }

    private Response outputDesignFile( ExpressionExperiment ee ) throws IOException {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeOrLocateDesignFile( ee, false );
        return this.outputFile( file, DatasetsWebService.ERROR_DESIGN_FILE_NOT_AVAILABLE, ee.getShortName() );
    }

    private Response outputFile( @Nullable File file, String error, String shortName ) throws IOException {
        if ( file == null || !file.exists() ) {
            throw new NotFoundException( String.format( error, shortName ) );
        }
        // we remove the .gz extension because we use HTTP Content-Encoding
        return Response.ok( new GZIPInputStream( new FileInputStream( file ) ) )
                .header( "Content-Encoding", "gzip" )
                .header( "Content-Disposition", "attachment; filename=" + FilenameUtils.removeExtension( file.getName() ) )
                .build();
    }

    private List<DifferentialExpressionAnalysisValueObject> getDiffExVos( Long eeId, int offset, int limit ) {
        Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> map = differentialExpressionAnalysisService
                .getAnalysesByExperiment( Collections.singleton( eeId ), offset, limit );
        if ( map == null || map.isEmpty() ) {
            return Collections.emptyList();
        }
        return map.get( map.keySet().iterator().next() );
    }

    @Value
    private static class SimpleSVDValueObject {
        /**
         * Order same as the rows of the v matrix.
         */
        List<Long> bioMaterialIds;

        /**
         * An array of values representing the fraction of the variance each component accounts for
         */
        double[] variances;
        double[][] vMatrix;
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
}
