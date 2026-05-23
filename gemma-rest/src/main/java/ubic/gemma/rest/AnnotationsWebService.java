/*
 * The gemma-web project
 *
 * Copyright (c) 2015 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.*;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSearchService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.ranking.AnnotationSearchRankingStrategy;
import ubic.gemma.rest.ranking.LuceneOrderRankingStrategy;
import ubic.gemma.rest.util.QueriedAndFilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.*;

import org.springframework.lang.Nullable;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for annotations.
 *
 * @author tesarst
 */
@Service
@Slf4j
@Path("/annotations")
public class AnnotationsWebService {


    private static final String SEARCH_QUERY_DESCRIPTION = "A comma-delimited list of keywords to find annotations.";

    private static final String FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION = "The search for annotations has timed out. It can generally be resolved by reattempting the search 30 seconds later. Lookup the `Retry-After` header for the recommended delay.";

    /**
     * Amout of time allowed to spend on finding characteristics.
     */
    private static final long FIND_CHARACTERISTICS_TIMEOUT_MS = 30000;

    /**
     * Bounded LRU cache for successful {@code /annotations/search} responses. Absorbs repeat queries from
     * curation pipelines that ask for the same terms across many experiments. Keyed by the normalized
     * (trimmed + lowercased) query payload; only success results are cached, exceptions propagate uncached.
     * <p>
     * Process-local, access-order eviction; size bound is the eviction signal. Plain {@link LinkedHashMap}
     * wrapped in {@link Collections#synchronizedMap} — no Guava on the classpath here.
     */
    private static final int SEARCH_CACHE_MAX_ENTRIES = 500;

    private static final Map<String, List<AnnotationSearchResultValueObject>> SEARCH_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, List<AnnotationSearchResultValueObject>>( 64, 0.75f, true ) {
                @Override
                protected boolean removeEldestEntry( Map.Entry<String, List<AnnotationSearchResultValueObject>> eldest ) {
                    return size() > SEARCH_CACHE_MAX_ENTRIES;
                }
            } );

    private OntologyService ontologyService;
    private SearchService searchService;
    private CharacteristicService characteristicService;
    private ExpressionExperimentService expressionExperimentService;
    private DatasetArgService datasetArgService;
    private TaxonArgService taxonArgService;
    /**
     * Strategy registry keyed by short name ({@code lucene}, {@code usage}, {@code coverage}).
     * Spring populates this from every {@link AnnotationSearchRankingStrategy} bean — the bean name
     * (set via {@code @Component("name")}) is the map key. Adding a new strategy is one bean.
     * Optional so the existing {@code @TestComponent} contexts that don't define any ranker bean
     * still wire — the constructor falls back to a single-entry map with the no-op default.
     */
    @Nullable
    private Map<String, AnnotationSearchRankingStrategy> rankingStrategies;

    /**
     * Required by spring
     */
    public AnnotationsWebService() {
    }

    /**
     * Constructor for service autowiring
     */
    @Autowired
    public AnnotationsWebService( OntologyService ontologyService, SearchService searchService,
            CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
            DatasetArgService datasetArgService, TaxonArgService taxonArgService,
            @Nullable Map<String, AnnotationSearchRankingStrategy> rankingStrategies ) {
        this.ontologyService = ontologyService;
        this.searchService = searchService;
        this.characteristicService = characteristicService;
        this.expressionExperimentService = expressionExperimentService;
        this.datasetArgService = datasetArgService;
        this.taxonArgService = taxonArgService;
        if ( rankingStrategies == null || rankingStrategies.isEmpty() ) {
            // Test contexts may omit ranker beans; degrade to no-op so default-rank queries still work.
            LuceneOrderRankingStrategy fallback = new LuceneOrderRankingStrategy();
            this.rankingStrategies = Collections.singletonMap( fallback.getName(), fallback );
        } else {
            this.rankingStrategies = rankingStrategies;
        }
    }

    /**
     * Back-compat constructor for tests that wire only the core collaborators. Equivalent to
     * passing {@code null} for {@code rankingStrategies}.
     */
    public AnnotationsWebService( OntologyService ontologyService, SearchService searchService,
            CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
            DatasetArgService datasetArgService, TaxonArgService taxonArgService ) {
        this( ontologyService, searchService, characteristicService, expressionExperimentService,
                datasetArgService, taxonArgService, null );
    }

    /*https://www.w3.org/TR/owl-ref/#subClassOf-def*
     * Obtain the parent of a given annotation.
     * <p>
     * This is plural as we might add support for querying multiple annotations at once in the future.
     */
    @GET
    @Path("/parents")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the parents of the given annotations",
            description = "Terms that are returned satisfies the [rdfs:subClassOf](https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Subclass_Axioms) or [part_of](http://purl.obolibrary.org/obo/BFO_0000050) relations. When `direct` is set to false, this rule is applied recursively.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No term matched the given URI.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = "Ontology inference timed out.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<List<AnnotationSearchResultValueObject>> getAnnotationsParents(
            @Parameter(description = "Term URI") @QueryParam("uri") String termUri,
            @Parameter(description = "Only include direct children.") @QueryParam("direct") @DefaultValue("false") boolean direct ) {
        return respond( getAnnotationsParentsOrChildren( termUri, direct, true ) );
    }

    /**
     * Obtain the children of a given annotation.
     * <p>
     * This is plural as we might add support for querying multiple annotations at once in the future.
     */
    @GET
    @Path("/children")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the children of the given annotations",
            description = "Terms that are returned satisfies the [inverse of rdfs:subClassOf](https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Subclass_Axioms) or [has_part](http://purl.obolibrary.org/obo/BFO_0000051) relations. When `direct` is set to false, this rule is applied recursively.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No term matched the given URI.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = "Ontology inference timed out.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<List<AnnotationSearchResultValueObject>> getAnnotationsChildren(
            @Parameter(description = "Term URI") @QueryParam("uri") String termUri,
            @Parameter(description = "Only include direct parents.") @QueryParam("direct") @DefaultValue("false") boolean direct ) {
        return respond(getAnnotationsParentsOrChildren( termUri, direct, false ) );
    }

    /**
     * Look up an ontology term by its URI.
     */
    @GET
    @Path("/term")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve an ontology term by its URI", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "404", description = "No term matched the given URI.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
            @ApiResponse(responseCode = "503", description = "Ontology lookup timed out.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public ResponseDataObject<OntologyTermValueObject> getAnnotationTerm(
            @Parameter(description = "Term URI") @QueryParam("uri") String termUri ) {
        if ( StringUtils.isBlank( termUri ) ) {
            throw new BadRequestException( "The 'uri' parameter must not be blank." );
        }
        try {
            StopWatch timer = StopWatch.createStarted();
            // get term returns the first match
            OntologyTerm term = ontologyService.getTerm( termUri, Math.max( 30000 - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
            if ( term == null ) {
                throw new NotFoundException( "No ontology term with URI " + termUri );
            }
            String definition = ontologyService.getDefinition( termUri, Math.max( 30000 - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
            Integer usageCount = term.getUri() != null
                    ? getDistinctEeCountsByUri( Collections.singleton( term.getUri() ) ).getOrDefault( term.getUri(), 0 )
                    : null;
            return respond( new OntologyTermValueObject( term.getUri(), term.getLabel(), definition, term.isObsolete(), usageCount ) );
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( DateUtils.addSeconds( new Date(), 30 ), e );
        }
    }

    /**
     * List the ontology categories allowed for use in characteristics.
     */
    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all ontology categories used in Gemma", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content())
    })
    public ResponseDataObject<List<OntologyTermSimpleValueObject>> getAnnotationCategories() {
        List<OntologyTermSimpleValueObject> vos = ontologyService.getCategoryTerms().stream()
                .map( t -> new OntologyTermSimpleValueObject( t.getUri(), t.getLabel() ) )
                .collect( Collectors.toList() );
        return respond( vos );
    }

    /**
     * List the ontology predicates allowed for use in statements.
     */
    @GET
    @Path("/predicates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve all ontology predicates used in Gemma", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content())
    })
    public ResponseDataObject<List<OntologyTermSimpleValueObject>> getAnnotationPredicates() {
        List<OntologyTermSimpleValueObject> vos = ontologyService.getRelationTerms().stream()
                .map( p -> new OntologyTermSimpleValueObject( p.getUri(), p.getLabel() ) )
                .collect( Collectors.toList() );
        return respond( vos );
    }

    private List<AnnotationSearchResultValueObject> getAnnotationsParentsOrChildren( String termUri, boolean direct, boolean parents ) {
        if ( StringUtils.isBlank( termUri ) ) {
            throw new BadRequestException( "The 'uri' parameter must not be blank." );
        }
        try {
            StopWatch timer = StopWatch.createStarted();
            OntologyTerm term = ontologyService.getTerm( termUri, Math.max( 30000 - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
            if ( term == null ) {
                throw new NotFoundException( "No ontology term with URI " + termUri );
            }
            Collection<OntologyTerm> terms = parents ?
                    ontologyService.getParents( Collections.singleton( term ), direct, true, Math.max( 30000 - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ) :
                    ontologyService.getChildren( Collections.singleton( term ), direct, true, Math.max( 30000 - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
            Set<String> uris = terms.stream()
                    .map( OntologyTerm::getUri )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() );
            Map<String, Integer> countsByUri = getDistinctEeCountsByUri( uris );
            return terms.stream()
                    .map( t -> new AnnotationSearchResultValueObject( t.getLabel(), t.getUri(), null, null,
                            t.getUri() != null ? countsByUri.getOrDefault( t.getUri(), 0 ) : null ) )
                    .collect( Collectors.toList() );
        } catch ( TimeoutException e ) {
            throw new ServiceUnavailableException( DateUtils.addSeconds( new Date(), 30 ), e );
        }
    }

    /**
     * Does a search for annotation tags based on the given string.
     *
     * @param query the search query. Either plain text, or an ontology term URI
     * @return response data object with a collection of found terms, each wrapped in a CharacteristicValueObject.
     * @see OntologyService#findTermsInexact(String, int, Taxon, long, TimeUnit) for better description of the search process.
     * @see CharacteristicValueObject for the output object structure.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search for annotation tags", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
            @ApiResponse(responseCode = "503", description = FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public ResponseDataObject<List<AnnotationSearchResultValueObject>> searchAnnotations(
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION) @QueryParam("query") @DefaultValue("") StringArrayArg query,
            @Parameter(description = "Ranking strategy to apply on top of the raw Lucene order. " +
                    "`lucene` (default) preserves today's behaviour. `usage` blends rank with per-URI " +
                    "experiment usage count. `coverage` sorts by fraction of query tokens present in " +
                    "the hit's label.")
            @QueryParam("rank") @DefaultValue(LuceneOrderRankingStrategy.NAME) String rank
    ) {
        if ( query == null || query.getValue().isEmpty() ) {
            throw new BadRequestException( "Search query cannot be empty." );
        }
        AnnotationSearchRankingStrategy strategy = resolveRankingStrategy( rank );
        String cacheKey = buildSearchCacheKey( query.getValue(), strategy.getName() );
        List<AnnotationSearchResultValueObject> cached = SEARCH_CACHE.get( cacheKey );
        if ( cached != null ) {
            log.debug( "annotation-search cache HIT key={} (size={})", cacheKey, SEARCH_CACHE.size() );
            return respond( new ArrayList<>( cached ) );
        }
        try {
            List<AnnotationSearchResultValueObject> result = new ArrayList<>( this.getTerms( query, strategy, FIND_CHARACTERISTICS_TIMEOUT_MS ) );
            // store an unmodifiable defensive copy so callers can't mutate the cached value
            SEARCH_CACHE.put( cacheKey, Collections.unmodifiableList( new ArrayList<>( result ) ) );
            log.debug( "annotation-search cache MISS key={} stored {} hits (size={})", cacheKey, result.size(), SEARCH_CACHE.size() );
            return respond( result );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
    }

    private AnnotationSearchRankingStrategy resolveRankingStrategy( String name ) {
        String key = name != null ? name.trim().toLowerCase( Locale.ROOT ) : LuceneOrderRankingStrategy.NAME;
        if ( key.isEmpty() ) {
            key = LuceneOrderRankingStrategy.NAME;
        }
        AnnotationSearchRankingStrategy s = rankingStrategies != null ? rankingStrategies.get( key ) : null;
        if ( s == null ) {
            throw new BadRequestException( "Unknown ranking strategy '" + name + "'. Supported values: "
                    + ( rankingStrategies != null ? rankingStrategies.keySet() : Collections.emptySet() ) + "." );
        }
        return s;
    }

    /**
     * @see #searchAnnotations(StringArrayArg)
     */
    @GET
    @Path("/search/{query}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search for annotation tags",
            description = "This is deprecated in favour of passing `query` as a query parameter.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<List<AnnotationSearchResultValueObject>> searchAnnotationsByPathQuery( // Params:
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION) @PathParam("query") @DefaultValue("") StringArrayArg query // Required
    ) {
        return searchAnnotations( query, LuceneOrderRankingStrategy.NAME );
    }

    /**
     * Does a search for datasets containing characteristics matching the given string.
     * If filterArg, offset, limit or sortArg parameters are provided.
     *
     * @param query the search query. Either plain text, or an ontology term URI
     * @return response data object with a collection of dataset that match the search query.
     * @see ExpressionExperimentSearchService#searchExpressionExperiments(String) for better description of the search process.
     */
    @GET
    @Path("/search/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets associated to an annotation tags search",
            description = "This is deprecated in favour of the [/datasets](#/default/getDatasets) endpoint. Use the `AND` operator to intersect the results of multiple queries.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> searchDatasets( // Params:
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION + " Matching datasets for each query are intersected.") @QueryParam("query") @DefaultValue("") StringArrayArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg // Optional, default +id
    ) {
        if ( query == null || query.getValue().isEmpty() ) {
            throw new BadRequestException( "Search query cannot be empty." );
        }
        Collection<Long> foundIds;
        try {
            foundIds = this.searchEEs( query.getValue(), null );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }

        Filters filters = datasetArgService.getFilters( filterArg );
        Sort sort = datasetArgService.getSort( sortArg );

        Slice<ExpressionExperimentValueObject> slice;
        if ( foundIds.isEmpty() ) {
            slice = new Slice<>( Collections.emptyList(), sort, offset.getValue(), limit.getValue(), 0L );
        } else if ( filters.isEmpty()
                && offset.getValue() == 0
                && foundIds.size() <= limit.getValue()
                && sort.getPropertyName().

                equals( "id" )
                && sort.getDirection() == Sort.Direction.ASC ) {
            slice = new Slice<>( expressionExperimentService.loadValueObjectsByIds( foundIds ), sort, 0, limit.getValue(), ( long ) foundIds.size() );

        } else {
            // Otherwise there is no need to go the pre-filter path since we already know exactly what IDs we want.
            // If there are filters other than the search query, intersect the results.
            Filters filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilters( DatasetArrayArg.valueOf( StringUtils.join( foundIds, ',' ) ) ) );
            slice = expressionExperimentService.loadValueObjects( filtersWithQuery, sort, offset.getValue(), limit.getValue() );
        }

        return paginate( slice, String.join( " AND ", query.getValue() ), filters, new String[] { "id" } );
    }

    @GET
    @Path("/search/{query}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets associated to an annotation tags search",
            description = "This is deprecated in favour of passing `query` as a query parameter.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> searchDatasetsByQueryInPath( // Params:
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION + " Matching datasets for each query are intersected.")
            @PathParam("query") @DefaultValue("") StringArrayArg query, // Required
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filterArg, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sortArg // Optional, default +id
    ) {
        return searchDatasets( query, filterArg, offset, limit, sortArg );
    }

    /**
     * Same as {@link #searchDatasets(StringArrayArg, FilterArg, OffsetArg, LimitArg, SortArg)} but also filters by
     * taxon.
     */
    @GET
    @Path("/{taxon}/search/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets within a given taxa associated to an annotation tags search",
            description = "This is deprecated in favour of the [/datasets](#/default/getDatasets) endpoint with a `query` parameter and a `filter` parameter with `taxon.id = {taxon} or taxon.commonName = {taxon} or taxon.scientificName = {taxon}` to restrict the taxon instead.  Use the `AND` operator to intersect the results of multiple queries.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> searchTaxonDatasets( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION + " Matching datasets for each query are intersected.")
            @QueryParam("query") @DefaultValue("") StringArrayArg query,
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        if ( query == null || query.getValue().isEmpty() ) {
            throw new BadRequestException( "Search query cannot be empty." );
        }

        // will raise a NotFoundException early if not found
        Taxon taxon = taxonArgService.getEntity( taxonArg );

        Collection<Long> foundIds;
        try {
            foundIds = this.searchEEs( query.getValue(), taxon );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }

        // We always have to do filtering, because we always have at least the taxon argument (otherwise this#datasets method is used)
        Filters filters = datasetArgService.getFilters( filter ).and( taxonArgService.getFilters( taxonArg ) );

        Slice<ExpressionExperimentValueObject> slice;
        if ( foundIds.isEmpty() ) {
            slice = new Slice<>( Collections.emptyList(), datasetArgService.getSort( sort ), offset.getValue(), limit.getValue(), 0L );
        } else {
            // We always have to do filtering, because we always have at least the taxon argument (otherwise this#datasets method is used)
            Filters filtersWithQuery = Filters.by( filters ).and( datasetArgService.getFilters( DatasetArrayArg.valueOf( StringUtils.join( foundIds, ',' ) ) ) );
            slice = expressionExperimentService.loadValueObjects( filtersWithQuery, datasetArgService.getSort( sort ), offset.getValue(), limit.getValue() );
        }

        return paginate( slice, String.join( " AND ", query.getValue() ), filters, new String[] { "id" } );
    }

    /**
     * @see #searchDatasets(StringArrayArg, FilterArg, OffsetArg, LimitArg, SortArg)
     */
    @GET
    @Path("/{taxon}/search/{query}/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve datasets within a given taxa associated to an annotation tags search",
            description = "This is deprecated in favour of passing `query` as a query parameter.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The search query is empty or invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> searchTaxonDatasetsByQueryInPath( // Params:
            @PathParam("taxon") TaxonArg<?> taxonArg, // Required
            @Parameter(schema = @Schema(implementation = StringArrayArg.class), explode = Explode.FALSE, description = SEARCH_QUERY_DESCRIPTION + " Matching datasets for each query are intersected.")
            @PathParam("query") @DefaultValue("") StringArrayArg query, // Required
            @QueryParam("filter") @DefaultValue("") FilterArg<ExpressionExperiment> filter, // Optional, default null
            @QueryParam("offset") @DefaultValue("0") OffsetArg offset, // Optional, default 0
            @QueryParam("limit") @DefaultValue("20") LimitArg limit, // Optional, default 20
            @QueryParam("sort") @DefaultValue("+id") SortArg<ExpressionExperiment> sort // Optional, default +id
    ) {
        return searchTaxonDatasets( taxonArg, query, filter, offset, limit, sort );
    }

    /**
     * Performs a dataset search for each given value, then intersects the results to create a final set of dataset IDs.
     *
     * @param values the values that the datasets should match.
     * @return set of IDs that satisfy all given search values.
     */
    private Collection<Long> searchEEs( List<String> values, @Nullable Taxon taxon ) throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .resultType( ExpressionExperiment.class )
                .fillResults( false )
                .taxonConstraint( taxon )
                .build();
        Set<Long> ids = new HashSet<>();
        for ( String value : values ) {
            List<SearchResult<ExpressionExperiment>> eeResults = searchService.search( settings.withQuery( value ) )
                    .getByResultObjectType( ExpressionExperiment.class );
            // Working only with IDs
            Set<Long> valueIds = new HashSet<>();
            for ( SearchResult<ExpressionExperiment> result : eeResults ) {
                valueIds.add( result.getResultId() );
            }
            // Intersecting with previous results
            if ( ids.isEmpty() ) {
                // In the first run we keep the whole list od IDs
                ids.addAll( valueIds );
            } else {
                // Intersecting with the IDs found in the current run
                ids.retainAll( valueIds );
            }
            // if one query is empty, then the intersection will be empty as well
            if ( ids.isEmpty() ) {
                break;
            }
        }
        return ids;
    }

    /**
     * Finds characteristics by either a plain text or URI.
     *
     * @param arg the array arg containing all the strings to search for.
     * @return a collection of characteristics matching the input query.
     */
    private LinkedHashSet<AnnotationSearchResultValueObject> getTerms( StringArrayArg arg,
            AnnotationSearchRankingStrategy strategy, long timeoutMs ) throws SearchException {
        StopWatch timer = StopWatch.createStarted();
        List<CharacteristicValueObject> rawHits = new ArrayList<>();
        for ( String query : arg.getValue() ) {
            query = query.trim();
            URI uri = parseTermUriQuery( query );
            if ( uri != null ) {
                rawHits.addAll( characteristicService.loadValueObjects( characteristicService
                        .findByUri( StringUtils.strip( query ), null, null, true, -1 ) ) );
            } else {
                rawHits.addAll( ontologyService.findExperimentsCharacteristicTags( query, 1000, false, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ) );
            }
        }
        Set<String> uris = rawHits.stream()
                .map( CharacteristicValueObject::getValueUri )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        Map<String, Integer> countsByUri = getDistinctEeCountsByUri( uris );
        // Apply the requested ranking strategy. The joined query text drives token-coverage; for
        // multi-term StringArrayArg inputs (typically comma-joined keywords), pass them space-joined
        // so the tokeniser sees the union.
        String joinedQuery = String.join( " ", arg.getValue() );
        List<CharacteristicValueObject> ranked = strategy.rank( joinedQuery, rawHits, countsByUri );
        LinkedHashSet<AnnotationSearchResultValueObject> vos = new LinkedHashSet<>();
        for ( CharacteristicValueObject vo : ranked ) {
            Integer count = vo.getValueUri() != null ? countsByUri.getOrDefault( vo.getValueUri(), 0 ) : null;
            vos.add( new AnnotationSearchResultValueObject( vo.getValue(), vo.getValueUri(), vo.getCategory(),
                    vo.getCategoryUri(), count ) );
        }
        return vos;
    }

    /**
     * Count the number of distinct expression experiments that reference each of the given annotation URIs.
     */
    private Map<String, Integer> getDistinctEeCountsByUri( Set<String> uris ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits =
                characteristicService.findExperimentsByUris( uris, true, true, true, null, -1, false, false );
        Map<String, Set<Long>> distinctIdsByUri = new HashMap<>();
        for ( Map<String, Set<ExpressionExperiment>> perClass : hits.values() ) {
            for ( Map.Entry<String, Set<ExpressionExperiment>> entry : perClass.entrySet() ) {
                Set<Long> bucket = distinctIdsByUri.computeIfAbsent( entry.getKey(), k -> new HashSet<>() );
                for ( ExpressionExperiment ee : entry.getValue() ) {
                    bucket.add( ee.getId() );
                }
            }
        }
        Map<String, Integer> counts = new HashMap<>( distinctIdsByUri.size() );
        distinctIdsByUri.forEach( ( k, v ) -> counts.put( k, v.size() ) );
        return counts;
    }

    @Value
    public static class AnnotationSearchResultValueObject {
        String value;
        String valueUri;
        String category;
        String categoryUri;
        Integer usageCount;
    }

    @Value
    public static class OntologyTermValueObject {
        String uri;
        String label;
        String definition;
        boolean obsolete;
        Integer usageCount;
    }

    @Value
    public static class OntologyTermSimpleValueObject {
        String uri;
        String label;
    }

    /**
     * Build a stable cache key from the raw query payload. Trims each term and lowercases for plain-text
     * matches (the underlying LIKE search and ontology lookup are case-insensitive); URI-shaped terms keep
     * their case because URIs are case-sensitive on the path/query portion.
     */
    private static String buildSearchCacheKey( List<String> values, String rankName ) {
        StringBuilder sb = new StringBuilder( values.size() * 16 );
        // Prefix the strategy name so swapping rank= invalidates the cache without colliding with
        // a different-query default-rank entry. STX separates the prefix from the query payload.
        sb.append( rankName != null ? rankName : LuceneOrderRankingStrategy.NAME );
        sb.append( '' );
        for ( int i = 0; i < values.size(); i++ ) {
            String v = values.get( i );
            if ( v == null ) continue;
            String stripped = v.trim();
            if ( parseTermUriQuery( stripped ) != null ) {
                sb.append( stripped );
            } else {
                sb.append( stripped.toLowerCase( Locale.ROOT ) );
            }
            if ( i < values.size() - 1 ) {
                sb.append( '\u0001' ); // SOH separator - not a legal character in URIs or plain queries
            }
        }
        return sb.toString();
    }

    @Nullable
    private static URI parseTermUriQuery( String query ) {
        if ( query == null ) return null;
        String stripped = StringUtils.strip( query );
        if ( stripped.startsWith( "http://" ) || stripped.startsWith( "https://" ) ) {
            try {
                return URI.create( stripped );
            } catch ( IllegalArgumentException e ) {
                return null;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Dataset annotation write endpoints (HANDOFF_DATASETS_ANNOTATIONS_WRITE.md)
    //
    // POST   /annotations/datasets/{dataset}/annotations
    // DELETE /annotations/datasets/{dataset}/annotations/{annotationId}
    // PUT    /annotations/datasets/{dataset}/annotations
    //
    // Auth model: GROUP_CURATOR or GROUP_ADMIN, matching the design-write
    // decision in STATUS_PUT_DATASETS_DESIGN.md. Fine-grained
    // `curation:annotation:write` authority is a follow-up.
    //
    // The PUT here is distinct from DatasetsWebService#updateDatasetAnnotations:
    // both target the same EE characteristic set, but this PUT emits per-row
    // TagAddedEvent / TagRemovedEvent audit rows (one per change), where the
    // DatasetsWebService PUT emits a single ManualAnnotationEvent. Bulk callers
    // that want the per-row trail should use this endpoint.
    // ---------------------------------------------------------------------

    /**
     * Request body for {@link #addDatasetAnnotation}. Fields mirror
     * {@link HANDOFF_DATASETS_ANNOTATIONS_WRITE.md} §"Required endpoints":
     * a single tag carried as a (category, value) pair with optional ontology URIs
     * and evidence code.
     */
    public static class AnnotationDto {
        @Nullable
        private String category;
        @Nullable
        private String categoryUri;
        @Nullable
        private String value;
        @Nullable
        private String valueUri;
        @Nullable
        private String evidenceCode;
        @Nullable
        private String predicateUri;

        @Nullable
        public String getCategory() {
            return category;
        }

        public void setCategory( @Nullable String category ) {
            this.category = category;
        }

        @Nullable
        public String getCategoryUri() {
            return categoryUri;
        }

        public void setCategoryUri( @Nullable String categoryUri ) {
            this.categoryUri = categoryUri;
        }

        @Nullable
        public String getValue() {
            return value;
        }

        public void setValue( @Nullable String value ) {
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
        public String getEvidenceCode() {
            return evidenceCode;
        }

        public void setEvidenceCode( @Nullable String evidenceCode ) {
            this.evidenceCode = evidenceCode;
        }

        @Nullable
        public String getPredicateUri() {
            return predicateUri;
        }

        public void setPredicateUri( @Nullable String predicateUri ) {
            this.predicateUri = predicateUri;
        }
    }

    /**
     * Request body for {@link #replaceDatasetAnnotations}: the full desired tag set plus an optional
     * {@code agentProposalId} to attach to emitted audit events (linkage is parked until the
     * {@code AgentProposal} entity ships — see {@code STATUS_PUT_DATASETS_DESIGN.md}).
     */
    public static class AnnotationsReplaceRequest {
        @Nullable
        private List<AnnotationDto> annotations;
        @Nullable
        private Long agentProposalId;

        @Nullable
        public List<AnnotationDto> getAnnotations() {
            return annotations;
        }

        public void setAnnotations( @Nullable List<AnnotationDto> annotations ) {
            this.annotations = annotations;
        }

        @Nullable
        public Long getAgentProposalId() {
            return agentProposalId;
        }

        public void setAgentProposalId( @Nullable Long agentProposalId ) {
            this.agentProposalId = agentProposalId;
        }
    }

    /**
     * Diff-and-apply summary returned by the bulk PUT.
     */
    @Value
    public static class AnnotationReplaceReport {
        Long eeId;
        int before;
        int after;
        List<AnnotationValueObject> added;
        List<AnnotationValueObject> removed;
        int unchanged;
        /**
         * Per-row audit-event ids; empty by default — populating these requires capturing the
         * id of the {@code AuditEvent} written by the {@code @Audited} aspect, which the aspect
         * does not surface back through the call. Follow-up work; see {@code STATUS_DATASETS_ANNOTATIONS_WRITE.md}.
         */
        List<Long> auditEventIds;
        /**
         * URIs in the desired set that didn't resolve against an ontology. Currently always empty
         * — the server trusts the client's URIs (per the recommendation in
         * {@code HANDOFF_DATASETS_ANNOTATIONS_WRITE.md} §"Failure modes — Unknown URIs"). Wired
         * into the response shape now so the contract is stable; populated when boundary
         * resolution lands.
         */
        List<String> unresolvedUris;
    }

    @POST
    @Path("/datasets/{dataset}/annotations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Add a single annotation tag to a dataset",
            description = "Adds one experiment-level annotation (tag) to the dataset. Emits a "
                    + "TagAddedEvent on the dataset's audit trail. Duplicate tags (same category "
                    + "URI + value URI) are rejected with 409 Conflict. Requires GROUP_CURATOR or "
                    + "GROUP_ADMIN.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Annotation created.", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The request body is missing or malformed.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "403", description = "The caller lacks curator privileges.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "An annotation with the same (category, value) already exists.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response addDatasetAnnotation(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable AnnotationDto body,
            @Parameter(description = "Optional id of the AgentProposal this tag is being applied from; "
                    + "linkage is parked until the AgentProposal entity ships.")
            @QueryParam("agentProposalId") @Nullable Long agentProposalId
    ) {
        if ( body == null ) {
            throw new BadRequestException( "A request body is required." );
        }
        Characteristic vc = annotationDtoToCharacteristic( body );
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        Characteristic persisted;
        try {
            persisted = expressionExperimentService.addAnnotation( ee, vc );
        } catch ( IllegalArgumentException e ) {
            // 409 Conflict for duplicate (category, value) — service throws IAE on dup.
            throw new ClientErrorException( e.getMessage(), Response.Status.CONFLICT, e );
        }
        // agentProposalId is accepted-and-dropped; see STATUS_PUT_DATASETS_DESIGN.md.
        if ( agentProposalId != null ) {
            log.debug( "addDatasetAnnotation: received agentProposalId={} for ee={} (linkage parked)",
                    agentProposalId, ee.getId() );
        }
        return Response.status( Response.Status.CREATED )
                .entity( respond( new AnnotationValueObject( persisted, ExpressionExperiment.class ) ) )
                .build();
    }

    @DELETE
    @Path("/datasets/{dataset}/annotations/{annotationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Remove a single annotation tag from a dataset",
            description = "Removes the annotation with the given id from the dataset. Emits a "
                    + "TagRemovedEvent. Returns 404 if no such annotation exists on this dataset. "
                    + "Requires GROUP_CURATOR or GROUP_ADMIN.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "204", description = "Annotation removed.", content = @Content()),
                    @ApiResponse(responseCode = "403", description = "The caller lacks curator privileges.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset or annotation does not exist on this dataset.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response removeDatasetAnnotation(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @PathParam("annotationId") Long annotationId
    ) {
        if ( annotationId == null ) {
            throw new BadRequestException( "An annotation id is required." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        Characteristic removed = expressionExperimentService.removeAnnotation( ee, annotationId );
        if ( removed == null ) {
            throw new NotFoundException( "No annotation with id " + annotationId + " on dataset " + ee.getShortName() + "." );
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("/datasets/{dataset}/annotations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Bulk-replace a dataset's annotations (diff-then-apply)",
            description = "Computes the diff between the desired tag set and the dataset's current "
                    + "tag set, then applies adds and removes per-row in a single transaction. "
                    + "Emits one TagAddedEvent per add and one TagRemovedEvent per remove (NOT a "
                    + "single summary event). Idempotent: re-PUTing the same set yields an empty "
                    + "diff and no events. Distinct from PUT /datasets/{id}/annotations on "
                    + "DatasetsWebService, which emits a single aggregate ManualAnnotationEvent. "
                    + "Requires GROUP_CURATOR or GROUP_ADMIN.",
            security = { @SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "cookieAuth") },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The request body is missing or malformed.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "403", description = "The caller lacks curator privileges.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<AnnotationReplaceReport> replaceDatasetAnnotations(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable AnnotationsReplaceRequest body
    ) {
        if ( body == null || body.getAnnotations() == null ) {
            throw new BadRequestException( "A request body with an 'annotations' field is required (use an empty list to clear)." );
        }
        List<Characteristic> desired = new ArrayList<>( body.getAnnotations().size() );
        for ( AnnotationDto dto : body.getAnnotations() ) {
            if ( dto == null ) {
                throw new BadRequestException( "Annotation entries must not be null." );
            }
            desired.add( annotationDtoToCharacteristic( dto ) );
        }
        // agentProposalId accepted but currently dropped; see STATUS_PUT_DATASETS_DESIGN.md.
        if ( body.getAgentProposalId() != null ) {
            log.debug( "replaceDatasetAnnotations: received agentProposalId={} (linkage parked)",
                    body.getAgentProposalId() );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );

        // The mutations are applied per-row through expressionExperimentService so each call fires
        // its own @Audited aspect (one TagAddedEvent / TagRemovedEvent per row).
        // Re-read current characteristics directly off the EE for identity-based removal.
        Collection<Characteristic> currentChars = new ArrayList<>( ee.getCharacteristics() );
        List<Characteristic> toAdd = new ArrayList<>();
        List<Characteristic> toRemove = new ArrayList<>();
        for ( Characteristic c : currentChars ) {
            boolean keep = false;
            for ( Characteristic d : desired ) {
                if ( sameTag( c, d ) ) {
                    keep = true;
                    break;
                }
            }
            if ( !keep ) {
                toRemove.add( c );
            }
        }
        for ( Characteristic d : desired ) {
            boolean already = false;
            for ( Characteristic c : currentChars ) {
                if ( sameTag( c, d ) ) {
                    already = true;
                    break;
                }
            }
            if ( !already ) {
                toAdd.add( d );
            }
        }
        int before = currentChars.size();
        int unchanged = before - toRemove.size();

        // No-op fast path: idempotent re-PUT — no events, no DB writes, empty diff report.
        if ( toAdd.isEmpty() && toRemove.isEmpty() ) {
            return respond( new AnnotationReplaceReport(
                    ee.getId(),
                    before,
                    before,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    unchanged,
                    Collections.emptyList(),
                    Collections.emptyList()
            ) );
        }

        // Apply removes first, then adds, per-row through the service so each call is its own
        // @Audited aspect emission. The service method is @Transactional so the inner-loop
        // emissions land within the surrounding transaction; a per-row failure rolls back the
        // whole bulk (matches HANDOFF §"Partial failure inside bulk PUT").
        List<AnnotationValueObject> removedVOs = new ArrayList<>( toRemove.size() );
        for ( Characteristic c : toRemove ) {
            Characteristic gone = expressionExperimentService.removeAnnotation( ee, c.getId() );
            if ( gone != null ) {
                removedVOs.add( new AnnotationValueObject( gone, ExpressionExperiment.class ) );
            }
        }
        List<AnnotationValueObject> addedVOs = new ArrayList<>( toAdd.size() );
        for ( Characteristic c : toAdd ) {
            Characteristic added = expressionExperimentService.addAnnotation( ee, c );
            addedVOs.add( new AnnotationValueObject( added, ExpressionExperiment.class ) );
        }

        int after = before - toRemove.size() + toAdd.size();
        return respond( new AnnotationReplaceReport(
                ee.getId(),
                before,
                after,
                addedVOs,
                removedVOs,
                unchanged,
                Collections.emptyList(),
                Collections.emptyList()
        ) );
    }

    /**
     * Map an inbound {@link AnnotationDto} into a transient {@link Characteristic}, validating
     * required fields and parsing the evidence code. Throws {@link BadRequestException} on bad
     * input (mapped to HTTP 400 by the Jersey exception mapper).
     */
    private static Characteristic annotationDtoToCharacteristic( AnnotationDto dto ) {
        if ( dto == null ) {
            throw new BadRequestException( "Annotation entry must not be null." );
        }
        if ( StringUtils.isBlank( dto.getCategory() ) ) {
            throw new BadRequestException( "Each annotation must have a non-blank 'category'." );
        }
        if ( StringUtils.isBlank( dto.getValue() ) ) {
            throw new BadRequestException( "Each annotation must have a non-blank 'value'." );
        }
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( dto.getCategory() );
        c.setCategoryUri( dto.getCategoryUri() );
        c.setValue( dto.getValue() );
        c.setValueUri( dto.getValueUri() );
        if ( StringUtils.isNotBlank( dto.getEvidenceCode() ) ) {
            try {
                c.setEvidenceCode( GOEvidenceCode.valueOf( dto.getEvidenceCode().trim().toUpperCase( Locale.ROOT ) ) );
            } catch ( IllegalArgumentException e ) {
                throw new BadRequestException( "Unknown evidence_code: '" + dto.getEvidenceCode() + "'. "
                        + "Expected one of the GOEvidenceCode enum values (IEA, IDA, IC, ...).", e );
            }
        }
        return c;
    }

    private static boolean sameTag( Characteristic a, Characteristic b ) {
        return CharacteristicUtils.equals( a.getCategory(), a.getCategoryUri(), b.getCategory(), b.getCategoryUri() )
                && CharacteristicUtils.equals( a.getValue(), a.getValueUri(), b.getValue(), b.getValueUri() );
    }
}
