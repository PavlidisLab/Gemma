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
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.highlight.QueryScorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.filter.NoRowsLeftAfterFilteringException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.analysis.preprocess.svd.SVDValueObject;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.search.DefaultHighlighter;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.lucene.SimpleMarkdownFormatter;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

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
    private AuditEventService auditEventService;
    @Autowired
    private DatasetArgService datasetArgService;
    @Autowired
    private GeneArgService geneArgService;

    @Autowired
    private HttpServletRequest request;

    @ParametersAreNonnullByDefault
    private class Highlighter extends DefaultHighlighter {

        private final Set<Long> documentIdsToHighlight;

        private Highlighter( Set<Long> documentIdsToHighlight ) {
            this.documentIdsToHighlight = documentIdsToHighlight;
        }

        @Override
        public Map<String, String> highlightTerm( @Nullable String termUri, String termLabel, String field ) {
            String reconstructedUri = ServletUriComponentsBuilder.fromRequest( request )
                    .scheme( null ).host( null ).port( -1 )
                    // replace the query with the term URI and only retain the filter
                    .replaceQueryParam( "query", termUri != null ? termUri : termLabel )
                    .replaceQueryParam( "offset" )
                    .replaceQueryParam( "limit" )
                    .replaceQueryParam( "sort" )
                    .build()
                    .toUriString();
            return Collections.singletonMap( field, String.format( "**[%s](%s)**", termLabel, reconstructedUri ) );
        }

        @Override
        public org.apache.lucene.search.highlight.Highlighter createLuceneHighlighter( QueryScorer queryScorer ) {
            return new org.apache.lucene.search.highlight.Highlighter( new SimpleMarkdownFormatter(), queryScorer );
        }

        @Override
        public Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer, Set<String> fields ) {
            long id = Long.parseLong( document.get( "id" ) );
            // TODO: maybe use a filter in the Lucene query?
            if ( !documentIdsToHighlight.contains( id ) ) {
                return Collections.emptyMap();
            }
            return super.highlightDocument( document, highlighter, analyzer, fields );
        }
    }

    @GZIP
    @GET
    @CacheControl(maxAge = 1200)
    @CacheControl(isPrivate = true, authorities = { "GROUP_USER" })
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all datasets")
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentWithSearchResultValueObject> getDatasets( // Params:
            @QueryParam("query") String query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg // Optional, default +id
    ) {
        Filters filters = datasetArgService.getFilters( filterArg );
        Sort sort = datasetArgService.getSort( sortArg );
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        if ( query != null ) {
            Map<Long, Double> scoreById = new HashMap<>();
            Filters filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilterForSearchQuery( query, scoreById ) );
            List<Long> ids = new ArrayList<>( expressionExperimentService.loadIdsWithCache( filtersWithQuery, sort ) );
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
            return Responder.queryAndPaginate(
                    new Slice<>( vos, Sort.by( null, "searchResult.score", Sort.Direction.DESC ), offset, limit, ( long ) ids.size() )
                            .map( vo -> new ExpressionExperimentWithSearchResultValueObject( vo, resultById.get( vo.getId() ) ) ),
                    query, filters, new String[] { "id" } );
        } else {
            return Responder.queryAndPaginate(
                    expressionExperimentService.loadValueObjectsWithCache( filters, sort, offset, limit ).map( vo -> new ExpressionExperimentWithSearchResultValueObject( vo, null ) ),
                    null, filters, new String[] { "id" } );
        }
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
                this.searchResult = new SearchWebService.SearchResultValueObject<>( SearchResult.from( result, null ) );
            } else {
                this.searchResult = null;
            }
        }
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Count datasets matching the provided query and  filter")
    public ResponseDataObject<Long> getNumberOfDatasets(
            @QueryParam("query") String query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter ) {
        Filters filters = datasetArgService.getFilters( filter );
        if ( query != null ) {
            filters.and( datasetArgService.getFilterForSearchQuery( query, null ) );
        }
        return Responder.respond( expressionExperimentService.countWithCache( filters ) );
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
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.")
    public LimitedResponseDataObject<ArrayDesignWithUsageStatisticsValueObject> getDatasetsPlatformsUsageStatistics(
            @QueryParam("query") String query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @QueryParam("limit") @DefaultValue("50") LimitArg limit ) {
        Filters filters = datasetArgService.getFilters( filter );
        Filters filtersWithQuery;
        if ( query != null ) {
            filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilterForSearchQuery( query, null ) );
        } else {
            filtersWithQuery = filters;
        }
        Integer l = limit.getValueNoMaximum();
        Map<TechnologyType, Long> tts = expressionExperimentService.getTechnologyTypeUsageFrequency( filtersWithQuery );
        Map<ArrayDesign, Long> ads = expressionExperimentService.getArrayDesignUsedOrOriginalPlatformUsageFrequency( filtersWithQuery, l );
        List<ArrayDesignValueObject> adsVos = arrayDesignService.loadValueObjects( ads.keySet() );
        Map<Long, Long> countsById = ads.entrySet().stream().collect( Collectors.toMap( e -> e.getKey().getId(), Map.Entry::getValue ) );
        List<ArrayDesignWithUsageStatisticsValueObject> results =
                adsVos.stream()
                        .map( e -> new ArrayDesignWithUsageStatisticsValueObject( e, countsById.get( e.getId() ), tts.getOrDefault( TechnologyType.valueOf( e.getTechnologyType() ), 0L ) ) )
                        .sorted( Comparator.comparing( UsageStatistics::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) )
                        .collect( Collectors.toList() );
        return Responder.limit( results, query, filters, new String[] { "id" }, Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ), l );
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
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.")
    public QueriedAndFilteredResponseDataObject<CategoryWithUsageStatisticsValueObject> getDatasetsCategoriesUsageStatistics(
            @QueryParam("query") String query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter,
            @Parameter(description = "Excluded category URIs.", hidden = true) @QueryParam("excludedCategories") StringArrayArg excludedCategoryUris,
            @Parameter(description = "Exclude free-text categories (i.e. those with null URIs).", hidden = true) @QueryParam("excludeFreeTextCategories") @DefaultValue("false") Boolean excludeFreeTextCategories,
            @Parameter(description = "Excluded term URIs; this list is expanded with subClassOf inference.", hidden = true) @QueryParam("excludedTerms") StringArrayArg excludedTermUris,
            @Parameter(description = "Exclude free-text terms (i.e. those with null URIs).", hidden = true) @QueryParam("excludeFreeTextTerms") @DefaultValue("false") Boolean excludeFreeTextTerms,
            @Parameter(description = "Exclude uncategorized terms.", hidden = true) @QueryParam("excludeUncategorizedTerms") @DefaultValue("false") Boolean excludeUncategorizedTerms,
            @Parameter(description = "Retain the categories applicable to terms mentioned in the `filter` parameter even if they are excluded by `excludedCategories` or `excludedTerms`.", hidden = true) @QueryParam("retainMentionedTerms") @DefaultValue("false") Boolean retainMentionedTerms
    ) {
        // ensure that implied terms are retained in the usage frequency
        Collection<OntologyTerm> mentionedTerms = retainMentionedTerms ? new HashSet<>() : null;
        Filters filters = datasetArgService.getFilters( filter, mentionedTerms );
        Filters filtersWithQuery;
        if ( query != null ) {
            filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilterForSearchQuery( query, null ) );
        } else {
            filtersWithQuery = filters;
        }
        List<CategoryWithUsageStatisticsValueObject> results = expressionExperimentService.getCategoriesUsageFrequency(
                        filtersWithQuery,
                        datasetArgService.getExcludedUris( excludedCategoryUris, excludeFreeTextCategories, excludeUncategorizedTerms ),
                        datasetArgService.getExcludedUris( excludedTermUris, excludeFreeTextTerms, excludeUncategorizedTerms ),
                        mentionedTerms != null ? mentionedTerms.stream().map( OntologyTerm::getUri ).collect( Collectors.toSet() ) : null )
                .entrySet()
                .stream()
                .map( e -> new CategoryWithUsageStatisticsValueObject( e.getKey().getCategoryUri(), e.getKey().getCategory(), e.getValue() ) )
                .sorted( Comparator.comparing( UsageStatistics::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) )
                .collect( Collectors.toList() );
        return Responder.queryAndFilter( results, query, filters, new String[] { "classUri", "className" }, Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ) );
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
            description = "Usage statistics are aggregated across experiment tags, samples and factor values mentioned in the experimental design.")
    public LimitedResponseDataObject<AnnotationWithUsageStatisticsValueObject> getDatasetsAnnotationsUsageStatistics(
            @QueryParam("query") String query,
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
        Filters filters = datasetArgService.getFilters( filter, mentionedTerms );
        Filters filtersWithQuery;
        if ( query != null ) {
            filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilterForSearchQuery( query, null ) );
        } else {
            filtersWithQuery = filters;
        }
        if ( category != null && category.isEmpty() ) {
            category = ExpressionExperimentService.UNCATEGORIZED;
        }
        // cache for visited parents (if two term share the same parent, we can save significant time generating the ancestors)
        Map<OntologyTerm, Set<OntologyTermValueObject>> visited = new HashMap<>();
        List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> initialResults = expressionExperimentService.getAnnotationsUsageFrequency(
                filtersWithQuery,
                limit,
                minFrequency != null ? minFrequency : 0,
                category,
                datasetArgService.getExcludedUris( excludedCategoryUris, excludeFreeTextCategories, excludeUncategorizedTerms ),
                datasetArgService.getExcludedUris( excludedTermUris, excludeFreeTextTerms, excludeUncategorizedTerms ),
                mentionedTerms != null ? mentionedTerms.stream().map( OntologyTerm::getUri ).collect( Collectors.toSet() ) : null );
        List<AnnotationWithUsageStatisticsValueObject> results = initialResults
                .stream()
                .map( e -> new AnnotationWithUsageStatisticsValueObject( e.getCharacteristic(), e.getNumberOfExpressionExperiments(), !excludeParentTerms && e.getTerm() != null ? getParentTerms( e.getTerm(), visited ) : null ) )
                .sorted( Comparator.comparing( UsageStatistics::getNumberOfExpressionExperiments, Comparator.reverseOrder() ) )
                .collect( Collectors.toList() );
        return Responder.limit( results, query, filters, new String[] { "classUri", "className", "termUri", "termName" },
                Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ),
                limit );
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


    private static Set<OntologyTermValueObject> getParentTerms( OntologyTerm c, Map<OntologyTerm, Set<OntologyTermValueObject>> visited ) {
        return c.getParents( true, false ).stream()
                .map( t -> toTermVo( t, visited ) )
                .collect( Collectors.toSet() );
    }

    private static OntologyTermValueObject toTermVo( OntologyTerm ontologyTerm, Map<OntologyTerm, Set<OntologyTermValueObject>> visited ) {
        Set<OntologyTermValueObject> parentVos;
        if ( visited.containsKey( ontologyTerm ) ) {
            parentVos = visited.get( ontologyTerm );
        } else {
            visited.put( ontologyTerm, Collections.emptySet() );
            parentVos = ontologyTerm.getParents( true, false ).stream()
                    .map( t -> toTermVo( t, visited ) )
                    .collect( Collectors.toSet() );
            visited.put( ontologyTerm, parentVos );
        }
        return new OntologyTermValueObject( ontologyTerm, parentVos );
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
         * URIs of parent terms.
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
    @Operation(summary = "Retrieve taxa usage statistics for datasets matching the provided query and filter")
    public QueriedAndFilteredResponseDataObject<TaxonWithUsageStatisticsValueObject> getDatasetsTaxaUsageStatistics(
            @QueryParam("query") String query, @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg ) {
        Filters filters = datasetArgService.getFilters( filterArg );
        Filters filtersWithQuery;
        if ( query != null ) {
            filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilterForSearchQuery( query, null ) );
        } else {
            filtersWithQuery = filters;
        }
        return Responder.queryAndFilter( expressionExperimentService.getTaxaUsageFrequency( filtersWithQuery )
                .entrySet().stream()
                .map( e -> new TaxonWithUsageStatisticsValueObject( e.getKey(), e.getValue() ) )
                .collect( Collectors.toList() ), query, filters, new String[] { "id" }, Sort.by( null, "numberOfExpressionExperiments", Sort.Direction.DESC, "numberOfExpressionExperiments" ) );
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
    public FilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> getDatasetsByIds( // Params:
            @PathParam("dataset") DatasetArrayArg datasetsArg, // Optional
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        Filters filters = datasetArgService.getFilters( filter ).and( datasetArgService.getFilters( datasetsArg ) );
        return Responder.paginate( expressionExperimentService::loadValueObjectsWithCache, filters, new String[] { "id" },
                datasetArgService.getSort( sort ), offset.getValue(), limit.getValue() );
    }

    /**
     * Browse blacklisted datasets.
     */
    @GET
    @Path("/blacklisted")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured("GROUP_ADMIN")
    @Operation(summary = "Retrieve all blacklisted datasets", hidden = true)
    public FilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> getBlacklistedDatasets(
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg,
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset,
            @QueryParam("limit") @DefaultValue("20") LimitArg limit ) {
        return Responder.paginate( expressionExperimentService::loadBlacklistedValueObjects,
                datasetArgService.getFilters( filterArg ), new String[] { "id" }, datasetArgService.getSort( sortArg ),
                offset.getValue(), limit.getValue() );
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
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectListArrayDesignValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<ArrayDesignValueObject>> getDatasetPlatforms( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        return Responder.respond( datasetArgService.getPlatforms( datasetArg ) );
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
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectListBioAssayValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<BioAssayValueObject>> getDatasetSamples( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        return Responder.respond( datasetArgService.getSamples( datasetArg ) );
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
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectListDifferentialExpressionAnalysisValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<DifferentialExpressionAnalysisValueObject>> getDatasetDifferentialExpressionAnalyses( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg, // Required
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit // Optional, default 20
    ) {
        return Responder.respond(
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
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetDifferentialExpressionAnalysesResultSets(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Context HttpServletRequest request ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        UriComponentsBuilder uriComponents;
        // this is only for testing because Jersey in-memory container lacks a servlet context
        if ( request != null ) {
            uriComponents = ServletUriComponentsBuilder.fromContextPath( request )
                    .scheme( null ).host( null ).port( -1 );
        } else {
            uriComponents = UriComponentsBuilder.newInstance();
        }
        URI resultSetUri = uriComponents
                .path( "/resultSets" )
                .queryParam( "datasets", "{datasetId}" )
                .buildAndExpand( ee.getId() ).toUri();
        return Response.status( Response.Status.FOUND )
                .location( resultSetUri )
                .build();
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
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectSetAnnotationValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<Set<AnnotationValueObject>> getDatasetAnnotations( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        return Responder.respond( datasetArgService.getAnnotations( datasetArg ) );
    }

    /**
     * Retrieve all available quantitation types for a dataset.
     */
    @GET
    @Path("/{dataset}/quantitationTypes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve quantitation types of a dataset")
    public ResponseDataObject<Set<QuantitationTypeValueObject>> getDatasetQuantitationTypes( @PathParam("dataset") DatasetArg<?> datasetArg ) {
        return Responder.respond( datasetArgService.getQuantitationTypes( datasetArg ) );
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
                    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
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
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Either the dataset or the quantitation type do not exist.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response getDatasetProcessedExpression( @PathParam("dataset") DatasetArg<?> datasetArg ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        QuantitationType qt = expressionExperimentService.getMaskedPreferredQuantitationType( ee );
        if ( qt == null ) {
            throw new NotFoundException( String.format( "No preferred quantitation type could be for found processed expression data data of %s.", ee ) );
        }
        StreamingOutput stream = ( output ) -> expressionDataFileService.writeProcessedExpressionData( ee, qt, new OutputStreamWriter( output ) );
        return Response.ok( stream )
                .header( "Content-Disposition", String.format( "attachment; filename=%d_%s_expmat.unfilt.data.txt", ee.getId(), ee.getShortName() ) )
                .build();
    }

    @Autowired
    private QuantitationTypeArgService quantitationTypeArgService;

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
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
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
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaTypeUtils.TEXT_TAB_SEPARATED_VALUES_UTF8,
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
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * Returns true if the experiment has had batch information successfully filled in. This will be true even if there
     * is only one batch. It does not reflect the presence or absence of a batch effect.
     *
     * @param datasetArg can either be the ExpressionExperiment ID or its short name (e.g. GSE1234). Retrieval by ID
     *                   is more efficient. Only datasets that user has access to will be available.
     */
    @GET
    @Path("/{dataset}/hasbatch")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Indicate of a dataset has batch information", hidden = true)
    public ResponseDataObject<Boolean> getDatasetHasBatchInformation( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        return Responder.respond( this.auditEventService.hasEvent( ee, BatchInformationFetchingEvent.class ) );
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
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(ref = "ResponseDataObjectSimpleSVDValueObject"))),
            @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                    content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<SimpleSVDValueObject> getDatasetSvd( // Params:
            @PathParam("dataset") DatasetArg<?> datasetArg // Required
    ) {
        SVDValueObject svd = svdService.getSvd( datasetArgService.getEntity( datasetArg ).getId() );
        return Responder.respond( svd == null ? null : new SimpleSVDValueObject( Arrays.asList( svd.getBioMaterialIds() ), svd.getVariances(), svd.getvMatrix().getRawMatrix() )
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
    @Path("/{datasets}/expressions/genes/{genes: [^/]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the expression data matrix of a set of datasets and genes")
    public ResponseDataObject<List<ExperimentExpressionLevelsValueObject>> getDatasetExpressionForGenes( // Params:
            @PathParam("datasets") DatasetArrayArg datasets, // Required
            @PathParam("genes") GeneArrayArg genes, // Required
            @QueryParam("keepNonSpecific") @DefaultValue("false") Boolean keepNonSpecific, // Optional, default false
            @QueryParam("consolidate") ExpLevelConsolidationArg consolidate // Optional, default everything is returned
    ) {
        return Responder.respond( processedExpressionDataVectorService
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
        return Responder.respond( processedExpressionDataVectorService
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
        return Responder.respond( processedExpressionDataVectorService
                .getExpressionLevelsDiffEx( datasetArgService.getEntities( datasets ),
                        diffExSet, threshold, limit.getValueNoMaximum(), keepNonSpecific,
                        consolidate == null ? null : consolidate.getValue() )
        );
    }

    private Response outputDataFile( ExpressionExperiment ee, boolean filter ) throws FilteringException, IOException {
        ee = expressionExperimentService.thawLite( ee );
        File file = expressionDataFileService.writeOrLocateDataFile( ee, false, filter ).orElse( null );
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
}
