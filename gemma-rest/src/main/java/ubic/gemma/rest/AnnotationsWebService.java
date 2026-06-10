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
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyTerm;
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
import ubic.gemma.model.genome.Gene;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
     * Spring-registered cache for successful {@code /annotations/search} responses. Absorbs repeat
     * queries from curation pipelines that ask for the same terms across many experiments. Keyed by
     * the normalized (trimmed + lowercased) query payload + the flags that affect the response
     * shape; only success results are cached, exceptions propagate uncached.
     * <p>
     * Configured in {@code EhcacheConfig#APP_CACHES} so it appears in {@code GET /admin/caches}
     * with hit/miss stats and can be flushed via the unified {@code DELETE /admin/caches/{name}}.
     */
    private static final String SEARCH_CACHE_NAME = "AnnotationsSearchResponseCache";

    private OntologyService ontologyService;
    private SearchService searchService;
    private CharacteristicService characteristicService;
    private ExpressionExperimentService expressionExperimentService;
    private DatasetArgService datasetArgService;
    private TaxonArgService taxonArgService;
    private ubic.gemma.persistence.service.genome.gene.GeneService geneService;
    @Autowired(required = false)
    private ubic.gemma.persistence.service.association.Gene2GOAssociationService gene2GOAssociationService;
    @Autowired(required = false)
    private ubic.gemma.core.ontology.providers.GeneOntologyService geneOntologyService;
    @Autowired(required = false)
    private org.springframework.cache.CacheManager cacheManager;
    private volatile org.springframework.cache.Cache searchResponseCache;

    private org.springframework.cache.Cache searchResponseCache() {
        org.springframework.cache.Cache c = searchResponseCache;
        if ( c == null && cacheManager != null ) {
            c = cacheManager.getCache( SEARCH_CACHE_NAME );
            searchResponseCache = c;
        }
        return c;
    }
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
     * Optional upstream gemma-rest URL to delegate the ontology Lucene-index lookup to. When set
     * and the request carries {@code ?upstream=true}, this service issues a server-to-server GET
     * to {@code <url>/rest/v2/annotations/search?rank=lucene&limit=<UPSTREAM_LIMIT>} with the same
     * query, parses the hits, and runs the local post-processing pipeline (prefix filter,
     * canonical sort, ranking, enrichment) against the shared gemd. Lets 2.0 ship new filtering /
     * ranking on top of staging's richer ontology index without ingesting OWLs locally.
     * <p>
     * Empty (the default) disables the path — requests with {@code ?upstream=true} return 400.
     */
    @org.springframework.beans.factory.annotation.Value("${gemma.upstream.annotationSearch.url:}")
    private String upstreamUrl;

    @org.springframework.beans.factory.annotation.Value("${gemma.upstream.annotationSearch.username:}")
    private String upstreamUsername;

    @org.springframework.beans.factory.annotation.Value("${gemma.upstream.annotationSearch.password:}")
    private String upstreamPassword;

    @org.springframework.beans.factory.annotation.Value("${gemma.upstream.annotationSearch.timeoutMs:25000}")
    private long upstreamTimeoutMs;

    /**
     * Raw {@code annotation.category.prefixes} property — semicolon-separated category:prefix
     * pairs (e.g. {@code cellType:CL_,CLO_,EFO_;cellLine:CLO_,CL_,EFO_}). Parsed once into
     * {@link #categoryPrefixesByKey} on first use. See default.properties for the shipped
     * defaults and the format.
     */
    @org.springframework.beans.factory.annotation.Value("${annotation.category.prefixes:}")
    private String categoryPrefixesRaw;

    /**
     * Parsed map from camelCase category key to ordered prefix list. Lazy-init since
     * {@code @Value} fields aren't populated when the constructor runs. Empty list under any
     * key means "no preference" (client picks). Absent key means "no entry configured".
     */
    @Nullable
    private volatile Map<String, List<String>> categoryPrefixesByKey;

    /** Page size requested from upstream — we want the wide candidate set so local filtering has room. */
    private static final int UPSTREAM_LIMIT = 1000;

    private final ObjectMapper upstreamMapper = new ObjectMapper();

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
            ubic.gemma.persistence.service.genome.gene.GeneService geneService,
            @Nullable Map<String, AnnotationSearchRankingStrategy> rankingStrategies ) {
        this.ontologyService = ontologyService;
        this.searchService = searchService;
        this.characteristicService = characteristicService;
        this.expressionExperimentService = expressionExperimentService;
        this.datasetArgService = datasetArgService;
        this.taxonArgService = taxonArgService;
        this.geneService = geneService;
        if ( rankingStrategies == null || rankingStrategies.isEmpty() ) {
            // Test contexts may omit ranker beans; degrade to no-op so default-rank queries still work.
            LuceneOrderRankingStrategy fallback = new LuceneOrderRankingStrategy();
            this.rankingStrategies = Collections.singletonMap( fallback.getName(), fallback );
        } else {
            // Spring injects with bean-name keys (e.g. luceneOrderRankingStrategy); the REST surface
            // uses the strategy's short getName() (e.g. lucene). Re-key so resolveRankingStrategy()
            // finds the strategy by its public-facing name.
            this.rankingStrategies = rankingStrategies.values().stream()
                    .collect( Collectors.toMap( AnnotationSearchRankingStrategy::getName, s -> s ) );
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
                datasetArgService, taxonArgService, null, null );
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
            // Direct + includeAdditionalProperties=true == nearest is_a OR part_of parents.
            // Null when the term has no URI (matches the count-skip semantics above).
            List<OntologyTermSimpleValueObject> parentVos;
            if ( term.getUri() != null ) {
                Collection<OntologyTerm> parents = ontologyService.getParents( Collections.singleton( term ),
                        true, true, Math.max( 30000 - timer.getTime(), 0 ), TimeUnit.MILLISECONDS );
                parentVos = parents.stream()
                        .map( p -> new OntologyTermSimpleValueObject( p.getUri(), p.getLabel() ) )
                        .collect( Collectors.toList() );
            } else {
                parentVos = null;
            }
            return respond( new OntologyTermValueObject( term.getUri(), term.getLabel(), definition, term.isObsolete(), usageCount, parentVos ) );
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
    public ResponseDataObject<List<AnnotationCategoryValueObject>> getAnnotationCategories() {
        Map<String, List<String>> prefixesByKey = resolveCategoryPrefixes();
        List<AnnotationCategoryValueObject> vos = ontologyService.getCategoryTerms().stream()
                .map( t -> {
                    String label = t.getLabel();
                    List<String> prefs = label != null
                            ? prefixesByKey.getOrDefault( categoryKey( label ), Collections.emptyList() )
                            : Collections.emptyList();
                    return new AnnotationCategoryValueObject( t.getUri(), label, prefs );
                } )
                .collect( Collectors.toList() );
        return respond( vos );
    }

    /**
     * Parse {@code annotation.category.prefixes} on first use, cache thereafter. Each entry is
     * a {@code key:prefix,prefix,...} pair; entries are semicolon-separated. Whitespace and
     * blank entries are tolerated.
     */
    Map<String, List<String>> resolveCategoryPrefixes() {
        Map<String, List<String>> cached = categoryPrefixesByKey;
        if ( cached != null ) return cached;
        Map<String, List<String>> out = new LinkedHashMap<>();
        if ( categoryPrefixesRaw != null && !categoryPrefixesRaw.trim().isEmpty() ) {
            for ( String entry : categoryPrefixesRaw.split( ";" ) ) {
                String e = entry.trim();
                if ( e.isEmpty() ) continue;
                int colon = e.indexOf( ':' );
                if ( colon <= 0 ) continue;
                String key = e.substring( 0, colon ).trim();
                String prefixList = e.substring( colon + 1 );
                List<String> prefixes = new ArrayList<>();
                for ( String p : prefixList.split( "," ) ) {
                    String t = p.trim();
                    if ( !t.isEmpty() ) prefixes.add( t );
                }
                out.put( key, prefixes );
            }
        }
        categoryPrefixesByKey = out;
        return out;
    }

    /**
     * Map an ontology category label (e.g. {@code "cell type"}) to its property-key form
     * ({@code "cellType"}) — lowercase, split on non-alphanumerics, camelCase. Categories
     * outside the configured set fall through to an empty preference list.
     */
    static String categoryKey( String label ) {
        if ( label == null || label.isEmpty() ) return "";
        String[] parts = label.toLowerCase( Locale.ROOT ).split( "[^a-z0-9]+" );
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < parts.length; i++ ) {
            String p = parts[i];
            if ( p.isEmpty() ) continue;
            if ( sb.length() == 0 ) {
                sb.append( p );
            } else {
                sb.append( Character.toUpperCase( p.charAt( 0 ) ) );
                if ( p.length() > 1 ) sb.append( p.substring( 1 ) );
            }
        }
        return sb.toString();
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
                            t.getUri() != null ? countsByUri.getOrDefault( t.getUri(), 0 ) : null,
                            null, null, null, null, null ) )
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
                    "the hit's label. `composite` combines coverage, usage, and rank into a single " +
                    "weighted score.")
            @QueryParam("rank") @DefaultValue(LuceneOrderRankingStrategy.NAME) String rank,
            @Parameter(description = "Maximum number of hits to return. Defaults to 20 (typeahead UX). " +
                    "Hard upper bound is 100; values outside [1, 100] yield HTTP 400. The truncation is " +
                    "applied AFTER ranking, so reducing the limit also reduces top-N enrichment cost.")
            @QueryParam("limit") @DefaultValue(SEARCH_DEFAULT_LIMIT_STR) int limit,
            @Parameter(description = "Allow-list of URI namespace prefixes; the candidate set is " +
                    "filtered to URIs containing one of these tokens BEFORE ranking + truncation. " +
                    "Comma-separated (e.g. `CL_,EFO_`). When omitted, no filter is applied. " +
                    "Pushing the filter server-side avoids the truncate-then-filter footgun where " +
                    "allow-list survivors depend on which items the per-strategy `limit` happened " +
                    "to keep.")
            @QueryParam("prefixes") @DefaultValue("") String prefixesParam,
            @Parameter(description = "When `true`, delegate the ontology Lucene-index lookup to the " +
                    "upstream gemma-rest configured by `gemma.upstream.annotationSearch.url` (e.g. " +
                    "staging). Lets a local 2.0 instance benefit from staging's richer ontology " +
                    "index while still applying 2.0's local filtering / ranking on top. Returns " +
                    "400 if the upstream URL is unset. Default `false` (local path).")
            @QueryParam("upstream") @DefaultValue("false") boolean upstream,
            @Parameter(description = "When `true`, keep only candidates whose label is case-" +
                    "insensitively equal to the (trimmed) query string. Cuts wire traffic for " +
                    "resolver-style callers that already know the exact label and only need the " +
                    "canonical row(s). Filter is applied BEFORE ranking + truncation, so `top_k`/" +
                    "`limit` still applies when an exact match has multiple alternate-URI rows. " +
                    "Empty result is `200` with `data: []`. Default `false` (current substring " +
                    "behaviour). See handoffs/HANDOFF_2026-05-25_EXACT_LABEL_PARAM.md.")
            @QueryParam("exact_label") @DefaultValue("false") boolean exactLabel,
            @Parameter(description = "Hint from the calling widget about what kind of annotation " +
                    "is being edited. Accepts a canonical category label (e.g. `genotype`, " +
                    "`organism part`) or the matching EFO URI. The response shape does not vary " +
                    "by category today: gene-symbol matches (value=symbol, valueUri=NCBI Gene " +
                    "URI, category=`gene`) are merged in unconditionally so STAT5B finds the gene " +
                    "whether the picker is on Genotype, Treatment, or a generic characteristic. " +
                    "Future ranking strategies will use the category to boost relevant ontology " +
                    "URIs (e.g. UBERON when category=organism part). The parameter keys the " +
                    "response cache so any future per-category divergence stays correct.")
            @QueryParam("category") @DefaultValue("") String category,
            @Parameter(description = "When true, populate `geneCount` on each hit with the distinct " +
                    "number of genes annotated to that GO term (including descendants walked under " +
                    "the `geneCountMaxTerms` cap). Adds a parallel fan-out at the response shape " +
                    "boundary; sub-100ms on a warm GO index. Default false.")
            @QueryParam("includeGeneCount") @DefaultValue("false") boolean includeGeneCount,
            @Parameter(description = "When `includeGeneCount=true`, cap the per-hit BFS descendant " +
                    "walk at this many GO terms. Default 50 (bounds broad parents like `metabolic " +
                    "process` to ~50ms per hit). 0 = unbounded; not recommended for typeahead.")
            @QueryParam("geneCountMaxTerms") @DefaultValue("50") int geneCountMaxTerms
    ) {
        if ( query == null || query.getValue().isEmpty() ) {
            throw new BadRequestException( "Search query cannot be empty." );
        }
        if ( limit < 1 || limit > SEARCH_MAX_LIMIT ) {
            throw new BadRequestException( "The 'limit' parameter must be between 1 and " + SEARCH_MAX_LIMIT + " (got " + limit + ")." );
        }
        if ( geneCountMaxTerms < 0 ) {
            throw new BadRequestException( "geneCountMaxTerms must be >= 0." );
        }
        AnnotationSearchRankingStrategy strategy = resolveRankingStrategy( rank );
        List<String> prefixes = parsePrefixes( prefixesParam );
        if ( upstream && ( upstreamUrl == null || upstreamUrl.trim().isEmpty() ) ) {
            throw new BadRequestException( "Upstream delegation requested but "
                    + "`gemma.upstream.annotationSearch.url` is unset on this server." );
        }
        // Cache key includes includeGeneCount + geneCountMaxTerms so a "with counts" call doesn't
        // hit a cached "without counts" payload.
        String cacheKey = buildSearchCacheKey( query.getValue(), strategy.getName(), limit, prefixes, upstream, exactLabel, category )
                + ( includeGeneCount ? "|gc=" + geneCountMaxTerms : "" );
        org.springframework.cache.Cache searchCache = searchResponseCache();
        if ( searchCache != null ) {
            org.springframework.cache.Cache.ValueWrapper hit = searchCache.get( cacheKey );
            if ( hit != null && hit.get() instanceof List ) {
                //noinspection unchecked
                List<AnnotationSearchResultValueObject> cached =
                        (List<AnnotationSearchResultValueObject>) hit.get();
                log.debug( "annotation-search cache HIT key={}", cacheKey );
                return respond( new ArrayList<>( cached ) );
            }
        }
        try {
            List<AnnotationSearchResultValueObject> result = new ArrayList<>( this.getTerms( query, strategy, limit, prefixes, upstream, exactLabel, category, FIND_CHARACTERISTICS_TIMEOUT_MS ) );
            if ( includeGeneCount && !result.isEmpty() ) {
                result = attachGeneCounts( result, geneCountMaxTerms );
            }
            // Cache only non-empty results. An empty hit list is almost always either (a) genuinely
            // no match, where re-running is cheap, or (b) a transient gap (ontologies still warming
            // after a restart, basecode Lucene index temporarily empty, etc.) — caching the empty
            // would pin the typeahead at "no results" until either an explicit cache flush or a
            // restart. Both classes lose nothing by recomputing.
            if ( !result.isEmpty() && searchCache != null ) {
                searchCache.put( cacheKey, Collections.unmodifiableList( new ArrayList<>( result ) ) );
                log.debug( "annotation-search cache MISS key={} stored {} hits", cacheKey, result.size() );
            } else if ( result.isEmpty() ) {
                log.debug( "annotation-search cache MISS key={} empty result — not cached", cacheKey );
            }
            return respond( result );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
    }

    /** Default number of hits returned by {@code /annotations/search}; sized for typeahead UX. */
    static final int SEARCH_DEFAULT_LIMIT = 20;
    private static final String SEARCH_DEFAULT_LIMIT_STR = "20";
    /** Upper bound for {@code ?limit=}; requests above this are 400. */
    static final int SEARCH_MAX_LIMIT = 100;

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

    // Bespoke /search/cache/evict was removed once SEARCH_CACHE moved into the Spring
    // CacheManager (region name AnnotationsSearchResponseCache). Use
    // DELETE /admin/caches/AnnotationsSearchResponseCache (or DELETE /admin/caches
    // for everything) — same admin endpoint that lists hit/miss stats.

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
        return searchAnnotations( query, LuceneOrderRankingStrategy.NAME, SEARCH_DEFAULT_LIMIT, "", false, false, "", false, 50 );
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
            AnnotationSearchRankingStrategy strategy, int limit, List<String> prefixes, boolean upstream,
            boolean exactLabel, String category, long timeoutMs ) throws SearchException {
        StopWatch timer = StopWatch.createStarted();
        long phaseStart = timer.getTime();
        List<CharacteristicValueObject> rawHits = new ArrayList<>();
        for ( String query : arg.getValue() ) {
            query = query.trim();
            URI uri = parseTermUriQuery( query );
            if ( uri != null ) {
                rawHits.addAll( characteristicService.loadValueObjects( characteristicService
                        .findByUri( StringUtils.strip( query ), null, null, true, -1 ) ) );
            } else if ( upstream ) {
                // Delegate the ontology Lucene-index lookup; downstream pipeline runs locally
                // against shared gemd. Failure here propagates as SearchException so the caller
                // sees a 5xx (or 503 via the existing timeout mapping), rather than silently
                // falling back to local — the curator explicitly asked for staging's index.
                rawHits.addAll( fetchUpstreamHits( query, Math.max( timeoutMs - timer.getTime(), 0 ) ) );
            } else {
                rawHits.addAll( ontologyService.findExperimentsCharacteristicTags( query, 1000, false, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ) );
            }
        }
        long tFindCharacteristics = timer.getTime() - phaseStart;
        int rawCount = rawHits.size();
        // Order hits by relevance tier (exact label ≺ starts-with ≺ word-boundary-contains ≺
        // substring ≺ other), with the prefixes parameter's order honoured as the next tier
        // and URI ASC as the deterministic tiebreaker inside each tier. Without the tier sort,
        // a typeahead query for "synaptic" returns "acetylcholine catabolic process in synaptic
        // cleft" before "chemical synaptic transmission" because the URI sort ranks by GO-ID
        // ascending. Honouring the prefixes order also lets a caller request
        // `prefixes=GO_,EFO_` and get GO_ matches above EFO_ within the tier.
        String relevanceQuery = String.join( " ", arg.getValue() ).trim().toLowerCase( Locale.ROOT );
        java.util.function.ToIntFunction<CharacteristicValueObject> tierFn = h -> {
            String label = h.getValue();
            if ( label == null ) return 5;
            String l = label.toLowerCase( Locale.ROOT );
            if ( l.equals( relevanceQuery ) ) return 0;        // exact label
            if ( l.startsWith( relevanceQuery ) ) return 1;    // label starts with query
            // word-boundary contains: query appears at the start of any token in the label
            int idx = l.indexOf( relevanceQuery );
            if ( idx > 0 && !Character.isLetterOrDigit( l.charAt( idx - 1 ) ) ) return 2;
            if ( idx >= 0 ) return 3;                          // raw substring
            return 4;                                          // URI / synonym match only
        };
        java.util.function.ToIntFunction<CharacteristicValueObject> prefixRankFn = h -> {
            if ( prefixes.isEmpty() ) return 0;
            String uri = h.getValueUri();
            if ( uri == null ) return prefixes.size();
            for ( int i = 0; i < prefixes.size(); i++ ) {
                if ( uri.contains( prefixes.get( i ) ) ) return i;
            }
            return prefixes.size();
        };
        rawHits.sort( Comparator
                .<CharacteristicValueObject>comparingInt( tierFn::applyAsInt )
                .thenComparingInt( prefixRankFn::applyAsInt )
                .thenComparing( CharacteristicValueObject::getValueUri, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( CharacteristicValueObject::getValue, Comparator.nullsLast( Comparator.naturalOrder() ) ) );
        // Exact-label pushdown for resolver-style callers (cuts 5-10x candidate payload).
        // Case-insensitive equality against the trimmed query — mirrors the trim+lowercase
        // that callers do client-side today. Applies AFTER the canonical sort so the kept
        // subset is also deterministic. Empty result is a valid outcome.
        if ( exactLabel ) {
            String wantedLower = arg.getValue().stream()
                    .map( s -> s != null ? s.trim().toLowerCase( Locale.ROOT ) : "" )
                    .filter( s -> !s.isEmpty() )
                    .findFirst()
                    .orElse( "" );
            if ( !wantedLower.isEmpty() ) {
                List<CharacteristicValueObject> exact = new ArrayList<>( rawHits.size() );
                for ( CharacteristicValueObject h : rawHits ) {
                    String label = h.getValue();
                    if ( label != null && label.trim().toLowerCase( Locale.ROOT ).equals( wantedLower ) ) {
                        exact.add( h );
                    }
                }
                rawHits = exact;
            }
        }
        // Allow-list pushdown: when callers know which ontology namespaces are relevant (e.g.
        // proposer agents passing `prefixes=CL_,EFO_`), filter BEFORE ranking + truncation so
        // the kept top-N is determined by ranking among the allowed set, not by which items the
        // strategy's truncate happened to keep. URI substring match (`.contains`) since the
        // canonical namespace prefix lives after `/obo/` in OBO-style URIs.
        if ( !prefixes.isEmpty() ) {
            List<CharacteristicValueObject> kept = new ArrayList<>( rawHits.size() );
            for ( CharacteristicValueObject h : rawHits ) {
                String hitUri = h.getValueUri();
                if ( hitUri == null ) continue;
                for ( String p : prefixes ) {
                    if ( hitUri.contains( p ) ) {
                        kept.add( h );
                        break;
                    }
                }
            }
            rawHits = kept;
        }
        long tFilters = timer.getTime() - phaseStart - tFindCharacteristics;
        phaseStart = timer.getTime();
        // Only fetch usage counts up-front when the ranking strategy actually consumes them.
        // For the default lucene/relevance-tier path, the heavy IN-clause across 400-1000
        // candidate URIs (~2.8s on the prod-tunneled DB) is wasted work — the relevance tiers
        // already drive the order. Counts for the visible top-N are still loaded after
        // truncation so the response payload's usageCount field stays populated.
        Map<String, Integer> countsByUri;
        if ( strategy.requiresUsageCounts() ) {
            Set<String> uris = rawHits.stream()
                    .map( CharacteristicValueObject::getValueUri )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() );
            countsByUri = getDistinctEeCountsByUri( uris );
        } else {
            countsByUri = Collections.emptyMap();
        }
        long tCounts = timer.getTime() - phaseStart;
        phaseStart = timer.getTime();
        // Apply the requested ranking strategy. The joined query text drives token-coverage; for
        // multi-term StringArrayArg inputs (typically comma-joined keywords), pass them space-joined
        // so the tokeniser sees the union.
        String joinedQuery = String.join( " ", arg.getValue() );
        List<CharacteristicValueObject> ranked = strategy.rank( joinedQuery, rawHits, countsByUri );

        // Truncate to the requested limit BEFORE enrichment, so per-URI definition + parents
        // lookups only fire for hits the client will actually see.
        if ( ranked.size() > limit ) {
            ranked = new ArrayList<>( ranked.subList( 0, limit ) );
        }
        long tRank = timer.getTime() - phaseStart;
        phaseStart = timer.getTime();
        // Top-N usage counts for the response payload (display). When the ranking strategy
        // didn't need counts, this is the only count query that fires — a much narrower
        // IN-clause (≤ limit URIs) than the original full-candidate scan.
        if ( !strategy.requiresUsageCounts() && !ranked.isEmpty() ) {
            Set<String> topUrisForCount = ranked.stream()
                    .map( CharacteristicValueObject::getValueUri )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() );
            if ( !topUrisForCount.isEmpty() ) {
                countsByUri = getDistinctEeCountsByUri( topUrisForCount );
            }
        }
        long tTopCounts = timer.getTime() - phaseStart;
        phaseStart = timer.getTime();

        // Enrich the top-N ranked hits with definition + nearest is_a/part_of parents + match
        // attribution (matchedVia/matchedText). The rest carry null sentinels so the UI can
        // lazy-load via /annotations/term?uri=X. Lookups share the remaining ontology-search
        // timeout budget; per-URI failures (timeout, missing term) degrade silently to null on
        // that single field.
        Set<String> topUris = collectTopUris( ranked, ENRICH_TOP_N );
        Map<String, String> defByUri = new HashMap<>();
        Map<String, List<OntologyTermSimpleValueObject>> parentsByUri = new HashMap<>();
        Map<String, MatchAttribution> matchByUri = new HashMap<>();
        if ( !topUris.isEmpty() ) {
            try {
                enrichTopHits( topUris, defByUri, parentsByUri, matchByUri, joinedQuery,
                        Math.max( timeoutMs - timer.getTime(), 0 ) );
            } catch ( TimeoutException e ) {
                // Budget exhausted mid-enrichment; surface whatever did resolve and leave the rest null.
                log.debug( "annotation-search enrichment hit shared-budget timeout: {} of {} URIs enriched (definitions); {} URIs enriched (parents)",
                        defByUri.size(), topUris.size(), parentsByUri.size() );
            }
        }
        long tEnrich = timer.getTime() - phaseStart;
        if ( timer.getTime() > 1000 ) {
            log.info( String.format(
                    "annotation-search: query='%s' raw=%d top=%d total=%dms (find=%dms filter=%dms counts=%dms rank=%dms topCounts=%dms enrich=%dms)",
                    arg.getValue(), rawCount, topUris.size(), timer.getTime(),
                    tFindCharacteristics, tFilters, tCounts, tRank, tTopCounts, tEnrich ) );
        }

        LinkedHashSet<AnnotationSearchResultValueObject> vos = new LinkedHashSet<>();
        for ( CharacteristicValueObject vo : ranked ) {
            Integer count = vo.getValueUri() != null ? countsByUri.getOrDefault( vo.getValueUri(), 0 ) : null;
            String uri = vo.getValueUri();
            boolean isTop = uri != null && topUris.contains( uri );
            String definition = isTop ? defByUri.get( uri ) : null;
            List<OntologyTermSimpleValueObject> parents = isTop ? parentsByUri.get( uri ) : null;
            MatchAttribution match = isTop ? matchByUri.get( uri ) : null;
            String matchedVia = match != null ? match.via.token : null;
            String matchedText = match != null ? match.text : null;
            vos.add( new AnnotationSearchResultValueObject( vo.getValue(), vo.getValueUri(), vo.getCategory(),
                    vo.getCategoryUri(), count, definition, parents, matchedVia, matchedText, null ) );
        }
        // Always merge gene hits in, regardless of category — the typeahead surface should
        // surface STAT5B whether the curator is in a Genotype factor, a Treatment factor, or a
        // generic characteristic picker. Category-aware ranking (e.g. boost UBERON URIs when
        // category=organism part, boost gene rows when category=genotype) is a future ranking-
        // strategy concern; for now the merge is unconditional and ranking is unchanged.
        // The category param is still accepted (and keyed into the cache) so future per-category
        // boosting can land without breaking on-wire callers.
        //
        // Cost: one extra Hibernate-Search gene query per call. Cache hits cover repeat calls
        // within the 5-min window, so a 20-keystroke typeahead session pays it once per query.
        LinkedHashSet<AnnotationSearchResultValueObject> geneRows = new LinkedHashSet<>();
        for ( String q : arg.getValue() ) {
            if ( q == null ) continue;
            String trimmed = q.trim();
            if ( trimmed.isEmpty() ) continue;
            geneRows.addAll( resolveGeneHits( trimmed, limit ) );
        }
        if ( !geneRows.isEmpty() ) {
            // Prepend so an exact-symbol match (STAT5B etc.) lands above ontology hits. Generic
            // queries with no real gene match yield an empty geneRows and we return vos as-is.
            LinkedHashSet<AnnotationSearchResultValueObject> merged = new LinkedHashSet<>( geneRows );
            merged.addAll( vos );
            if ( merged.size() > limit ) {
                LinkedHashSet<AnnotationSearchResultValueObject> trimmedSet = new LinkedHashSet<>();
                int n = 0;
                for ( AnnotationSearchResultValueObject e : merged ) {
                    if ( n++ >= limit ) break;
                    trimmedSet.add( e );
                }
                return trimmedSet;
            }
            return merged;
        }
        return vos;
    }

    /**
     * Resolve gene matches for the query via {@link SearchService}, render each as a synthetic
     * {@link AnnotationSearchResultValueObject} with category="gene" and valueUri=NCBI Gene URI.
     * Returns empty on any search failure; gene resolution is best-effort and must never break
     * the wider annotation-search response.
     */
    private LinkedHashSet<AnnotationSearchResultValueObject> resolveGeneHits( String query, int limit ) {
        LinkedHashSet<AnnotationSearchResultValueObject> out = new LinkedHashSet<>();
        // Previously this went through SearchService.search(Gene.class), which fans out across
        // every SearchSource — including GeneOntologySearchSource, which for queries like
        // "metabolism" or "neuron" walks the entire GO subtree (~30s wall on prod-tunneled DB).
        // We only keep exact-symbol matches (score >= 1.0) downstream, so the GO walk is wasted
        // work. Call GeneService.findByOfficialSymbol directly — same matches, no GO traversal,
        // a couple of ms instead of tens of seconds.
        if ( geneService == null ) {
            return out;
        }
        try {
            Collection<Gene> genes = geneService.findByOfficialSymbol( query );
            if ( genes == null || genes.isEmpty() ) {
                return out;
            }
            for ( Gene g : genes ) {
                if ( out.size() >= limit ) break;
                if ( g == null ) continue;
                String label = g.getOfficialSymbol();
                if ( label == null || label.isEmpty() ) continue;
                String uri = g.getNcbiGeneId() != null
                        ? "http://purl.org/commons/record/ncbi_gene/" + g.getNcbiGeneId()
                        : null;
                out.add( new AnnotationSearchResultValueObject( label, uri, "gene", null,
                        0, null, null, "search:gene", label, null ) );
            }
        } catch ( Exception e ) {
            log.debug( "Gene resolution skipped for query '{}': {}", query, e.toString() );
        }
        return out;
    }

    /** Top-N hits to enrich inline with definition + parents on /annotations/search. */
    private static final int ENRICH_TOP_N = 25;

    /** Canonical GO URI prefix; used to detect GO-shaped {@code valueUri}s in the response shape. */
    private static final String GO_URI_PREFIX = "http://purl.obolibrary.org/obo/GO_";

    /**
     * Augment each hit whose valueUri is a GO term with a {@code geneCount} field —
     * distinct genes annotated to that term, including the descendant terms walked under
     * the supplied {@code maxTerms} BFS cap. Per-hit work runs on a bounded fixed-size
     * pool so a 15-result typeahead pays ~50-100ms wall time rather than 15*50ms serial.
     * Wraps the existing result list — non-GO hits pass through unchanged.
     */
    private List<AnnotationSearchResultValueObject> attachGeneCounts(
            List<AnnotationSearchResultValueObject> results, int maxTerms ) {
        if ( gene2GOAssociationService == null || results.isEmpty() ) {
            return results;
        }
        // Identify the GO-URI hits we can count for.
        List<Integer> goIdxs = new ArrayList<>();
        for ( int i = 0; i < results.size(); i++ ) {
            String u = results.get( i ).getValueUri();
            if ( u != null && u.startsWith( GO_URI_PREFIX ) ) {
                goIdxs.add( i );
            }
        }
        if ( goIdxs.isEmpty() ) {
            return results;
        }
        Map<String, Long> countByUri = new java.util.concurrent.ConcurrentHashMap<>();
        int parallelism = Math.min( goIdxs.size(), 8 );
        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool( parallelism );
        try {
            List<java.util.concurrent.Future<?>> tasks = new ArrayList<>( goIdxs.size() );
            for ( Integer idx : goIdxs ) {
                String uri = results.get( idx ).getValueUri();
                tasks.add( pool.submit( () -> {
                    Set<String> uris = expandGoSubtree( uri, maxTerms );
                    long c = gene2GOAssociationService.countByGOTermUris( uris, null );
                    countByUri.put( uri, c );
                } ) );
            }
            // Bound to the annotation-search outer timeout so a stuck GO subtree
            // walk can't pin the response forever.
            long deadline = System.currentTimeMillis() + FIND_CHARACTERISTICS_TIMEOUT_MS;
            for ( java.util.concurrent.Future<?> f : tasks ) {
                long left = deadline - System.currentTimeMillis();
                if ( left <= 0 ) { f.cancel( true ); continue; }
                try {
                    f.get( left, java.util.concurrent.TimeUnit.MILLISECONDS );
                } catch ( java.util.concurrent.TimeoutException e ) {
                    f.cancel( true );
                } catch ( InterruptedException ie ) {
                    Thread.currentThread().interrupt();
                    return results;
                } catch ( java.util.concurrent.ExecutionException ee ) {
                    log.debug( "gene-count task failed", ee.getCause() );
                }
            }
        } finally {
            pool.shutdownNow();
        }
        // Rebuild the list with the count attached (VO is @Value-immutable).
        List<AnnotationSearchResultValueObject> out = new ArrayList<>( results.size() );
        for ( AnnotationSearchResultValueObject r : results ) {
            Long c = r.getValueUri() != null ? countByUri.get( r.getValueUri() ) : null;
            if ( c == null ) {
                out.add( r );
            } else {
                out.add( new AnnotationSearchResultValueObject(
                        r.getValue(), r.getValueUri(), r.getCategory(), r.getCategoryUri(),
                        r.getUsageCount(), r.getDefinition(), r.getParents(),
                        r.getMatchedVia(), r.getMatchedText(), c ) );
            }
        }
        return out;
    }

    /**
     * BFS-bounded subtree expansion for a single GO URI. Matches the design of
     * {@code GoTermsWebService.expandUris} — direct-children frontier walk, stop at
     * {@code maxTerms} URIs. {@code maxTerms <= 0} means "exact term only" here
     * (different default from GoTermsWebService where 0 means unbounded; this side
     * keeps it bounded to protect the typeahead p95).
     */
    private Set<String> expandGoSubtree( String goUri, int maxTerms ) {
        Set<String> uris = new LinkedHashSet<>();
        uris.add( goUri );
        if ( geneOntologyService == null || !geneOntologyService.isOntologyLoaded() ) {
            return uris;
        }
        if ( maxTerms <= 1 ) {
            return uris;
        }
        ubic.gemma.core.ontology.model.OntologyTerm term = geneOntologyService.getTerm( goUri );
        if ( term == null ) {
            return uris;
        }
        List<ubic.gemma.core.ontology.model.OntologyTerm> frontier = new ArrayList<>();
        frontier.add( term );
        while ( !frontier.isEmpty() && uris.size() < maxTerms ) {
            List<ubic.gemma.core.ontology.model.OntologyTerm> next = new ArrayList<>();
            for ( ubic.gemma.core.ontology.model.OntologyTerm f : frontier ) {
                for ( ubic.gemma.core.ontology.model.OntologyTerm c : f.getChildren( true, false ) ) {
                    if ( c.getUri() == null ) continue;
                    if ( uris.add( c.getUri() ) ) {
                        next.add( c );
                        if ( uris.size() >= maxTerms ) break;
                    }
                }
                if ( uris.size() >= maxTerms ) break;
            }
            frontier = next;
        }
        return uris;
    }

    private static Set<String> collectTopUris( List<CharacteristicValueObject> ranked, int topN ) {
        Set<String> out = new LinkedHashSet<>();
        int seen = 0;
        for ( CharacteristicValueObject vo : ranked ) {
            if ( seen >= topN ) {
                break;
            }
            seen++;
            String uri = vo.getValueUri();
            if ( uri != null ) {
                out.add( uri );
            }
        }
        return out;
    }

    /**
     * Per-URI definition + nearest-parents lookup with a shared timeout budget. OntologyService
     * has no batch get-definition / get-parents-by-URI API, so this loops; each per-URI call
     * sees what is left of the budget after prior calls subtract from it. Per-URI TimeoutException
     * is logged and the offending URI is left unset (downstream renders null); a TimeoutException
     * is rethrown only if the budget is exhausted upfront.
     */
    private void enrichTopHits( Set<String> topUris,
            Map<String, String> defByUri,
            Map<String, List<OntologyTermSimpleValueObject>> parentsByUri,
            Map<String, MatchAttribution> matchByUri,
            String originalQuery,
            long budgetMs ) throws TimeoutException {
        StopWatch local = StopWatch.createStarted();
        // Each URI's enrichment (definition + term + parents) is independent and read-only
        // against the ontology model. Running them in parallel collapses 3*N serial Jena
        // queries into ~3*N/parallelism wall time. For 15-URI typeahead this changed cold
        // queries from 7-10s to <2s. Bound concurrency to keep from saturating Jena's read
        // locks under burst load.
        int parallelism = Math.min( topUris.size(), 8 );
        if ( parallelism <= 1 ) {
            enrichOne( topUris.iterator().next(), defByUri, parentsByUri, matchByUri,
                    originalQuery, Math.max( budgetMs - local.getTime(), 0 ) );
            return;
        }
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool( parallelism );
        try {
            List<java.util.concurrent.Future<?>> tasks = new ArrayList<>( topUris.size() );
            for ( String uri : topUris ) {
                tasks.add( pool.submit( () -> {
                    long remaining = Math.max( budgetMs - local.getTime(), 0 );
                    if ( remaining <= 0 ) return;
                    enrichOne( uri, defByUri, parentsByUri, matchByUri, originalQuery, remaining );
                } ) );
            }
            long deadline = System.currentTimeMillis() + budgetMs;
            for ( java.util.concurrent.Future<?> f : tasks ) {
                long left = deadline - System.currentTimeMillis();
                if ( left <= 0 ) {
                    f.cancel( true );
                    continue;
                }
                try {
                    f.get( left, TimeUnit.MILLISECONDS );
                } catch ( java.util.concurrent.TimeoutException e ) {
                    f.cancel( true );
                    // Whatever did complete is already in the maps; report budget exhaustion.
                    throw new TimeoutException( "annotation-search enrichment exhausted timeout budget" );
                } catch ( InterruptedException ie ) {
                    Thread.currentThread().interrupt();
                    return;
                } catch ( java.util.concurrent.ExecutionException ee ) {
                    log.debug( "enrichment task failed", ee.getCause() );
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Enrich a single URI's definition + parents + match attribution. Each ontology call is
     * wrapped in a try so a per-URI failure (typeahead-friendly: silent) leaves the other URIs'
     * data intact. Concurrent invocations of this method by {@link #enrichTopHits} write into
     * the shared {@code defByUri}/{@code parentsByUri}/{@code matchByUri} maps — pass {@link
     * java.util.concurrent.ConcurrentHashMap} or wrap in {@link java.util.Collections#synchronizedMap}
     * if the caller iterates concurrently while this runs.
     */
    private void enrichOne( String uri,
            Map<String, String> defByUri,
            Map<String, List<OntologyTermSimpleValueObject>> parentsByUri,
            Map<String, MatchAttribution> matchByUri,
            String originalQuery,
            long remaining ) {
        if ( remaining <= 0 ) return;
        try {
            String def = ontologyService.getDefinition( uri, remaining, TimeUnit.MILLISECONDS );
            if ( def != null ) {
                synchronized ( defByUri ) { defByUri.put( uri, def ); }
            }
        } catch ( TimeoutException e ) {
            log.debug( "definition lookup timed out for {}", uri );
        }
        try {
            OntologyTerm term = ontologyService.getTerm( uri, remaining, TimeUnit.MILLISECONDS );
            if ( term == null ) return;
            MatchAttribution attribution = computeMatchAttribution( term, originalQuery );
            if ( attribution != null ) {
                synchronized ( matchByUri ) { matchByUri.put( uri, attribution ); }
            }
            Collection<OntologyTerm> parents = ontologyService.getParents( Collections.singleton( term ),
                    true, true, remaining, TimeUnit.MILLISECONDS );
            List<OntologyTermSimpleValueObject> parentVos = parents.stream()
                    .map( p -> new OntologyTermSimpleValueObject( p.getUri(), p.getLabel() ) )
                    .collect( Collectors.toList() );
            synchronized ( parentsByUri ) { parentsByUri.put( uri, parentVos ); }
        } catch ( TimeoutException e ) {
            log.debug( "term/parents lookup timed out for {}", uri );
        }
    }

    /**
     * OBO + IAO synonym-property URIs probed when back-computing per-hit match attribution. The
     * ordering matches the JSON-serialised {@link MatchedVia} ordering: preferred_label is checked
     * first by the caller; we then probe exact > narrow > related > broad > generic > alt_label.
     */
    private static final String OBO_EXACT_SYNONYM = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";
    private static final String OBO_NARROW_SYNONYM = "http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym";
    private static final String OBO_RELATED_SYNONYM = "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym";
    private static final String OBO_BROAD_SYNONYM = "http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym";
    private static final String OBO_GENERIC_SYNONYM = "http://www.geneontology.org/formats/oboInOwl#hasSynonym";
    private static final String IAO_ALT_LABEL = "http://purl.obolibrary.org/obo/IAO_0000118";

    /**
     * Back-compute which Lucene field produced the hit by checking the term's preferred label
     * and indexed synonyms for a normalised-equality match against the query (preferred_label >
     * exact > narrow > related > broad > generic > alt_label).
     * <p>
     * Strict equality is intentional: a token-overlap rule tagged every Lucene hit whose label
     * shared a single word with the query (e.g. {@code "disease"}) as {@code preferred_label},
     * which made the attribution useless as a relevance signal for clients. {@code matchedVia}
     * now means "this row's label/synonym IS the query", nothing fuzzier.
     * <p>
     * Returns {@code null} when nothing matches by equality — the hit came in via a Lucene
     * field we don't probe (definition, obo_id) or via fuzzy ranking. Clients render that as
     * "ranked-but-unattributed" rather than as a falsely strong match.
     */
    @Nullable
    private static MatchAttribution computeMatchAttribution( OntologyTerm term, String originalQuery ) {
        String normalisedQuery = normaliseForEquality( originalQuery );
        if ( normalisedQuery.isEmpty() ) {
            return null;
        }
        String label = term.getLabel();
        if ( label != null && normaliseForEquality( label ).equals( normalisedQuery ) ) {
            return new MatchAttribution( MatchedVia.PREFERRED_LABEL, label );
        }
        // Walk synonym fields in strength order; the first synonym whose normalised value equals
        // the normalised query wins. Most ontology terms expose only a handful of synonyms so
        // this is cheap.
        String[][] probes = {
                { OBO_EXACT_SYNONYM, MatchedVia.EXACT_SYNONYM.token },
                { OBO_NARROW_SYNONYM, MatchedVia.NARROW_SYNONYM.token },
                { OBO_RELATED_SYNONYM, MatchedVia.RELATED_SYNONYM.token },
                { OBO_BROAD_SYNONYM, MatchedVia.BROAD_SYNONYM.token },
                { OBO_GENERIC_SYNONYM, MatchedVia.EXACT_SYNONYM.token },  // collapsed to exact
                { IAO_ALT_LABEL, MatchedVia.ALT_LABEL.token },
        };
        for ( String[] probe : probes ) {
            Collection<AnnotationProperty> annots = term.getAnnotations( probe[0] );
            if ( annots == null || annots.isEmpty() ) {
                continue;
            }
            for ( AnnotationProperty ap : annots ) {
                String text = ap.getContents();
                if ( text != null && normaliseForEquality( text ).equals( normalisedQuery ) ) {
                    return new MatchAttribution( MatchedVia.fromToken( probe[1] ), text );
                }
            }
        }
        return null;
    }

    /**
     * Lowercase + collapse runs of non-alphanumeric characters to a single space + trim. Used
     * for {@code matchedVia} equality so that {@code "Down-Syndrome"} matches {@code
     * "down syndrome"} but {@code "type b pancreatic cell"} does NOT match {@code "type 2"}.
     */
    private static String normaliseForEquality( @Nullable String s ) {
        if ( s == null ) return "";
        return s.toLowerCase( Locale.ROOT ).replaceAll( "[^a-z0-9]+", " " ).trim();
    }

    /**
     * JSON-friendly enumeration of which Lucene field produced a hit. Serialised as the
     * lowercase-snake string in {@code token} for the {@code matchedVia} response field.
     */
    public enum MatchedVia {
        PREFERRED_LABEL( "preferred_label" ),
        EXACT_SYNONYM( "exact_synonym" ),
        NARROW_SYNONYM( "narrow_synonym" ),
        RELATED_SYNONYM( "related_synonym" ),
        BROAD_SYNONYM( "broad_synonym" ),
        ALT_LABEL( "alt_label" );

        final String token;

        MatchedVia( String token ) {
            this.token = token;
        }

        static MatchedVia fromToken( String token ) {
            for ( MatchedVia v : values() ) {
                if ( v.token.equals( token ) ) {
                    return v;
                }
            }
            return PREFERRED_LABEL;
        }
    }

    /** Pair of (matched field, matched text) attached to an annotation-search hit. */
    static final class MatchAttribution {
        final MatchedVia via;
        @Nullable
        final String text;

        MatchAttribution( MatchedVia via, @Nullable String text ) {
            this.via = via;
            this.text = text;
        }
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
        /**
         * Definition of the term, if enriched. Null indicates "not enriched" (the typeahead caller can
         * lazy-load via {@code /annotations/term?uri=X}); empty string indicates "enriched, no definition
         * known". Populated for the top-25 search hits only — see {@code getTerms}.
         */
        @Nullable String definition;
        /**
         * Nearest is_a OR part_of parents of this term, if enriched. Null indicates "not enriched"
         * (sentinel for lazy-load); empty list indicates "enriched, no parents" (e.g. top-level term).
         * Populated for the top-25 search hits only — see {@code getTerms}.
         */
        @Nullable List<OntologyTermSimpleValueObject> parents;
        /**
         * Which Lucene field most likely produced this hit. One of {@code preferred_label} (default),
         * {@code exact_synonym}, {@code narrow_synonym}, {@code related_synonym}, {@code broad_synonym},
         * {@code alt_label}. Back-computed by replaying the query tokens against the term's label
         * and indexed synonyms — see {@code computeMatchAttribution}. Null indicates "not enriched"
         * (lazy-load sentinel, same semantics as {@link #definition}). Populated for the top-25
         * search hits only.
         */
        @Nullable String matchedVia;
        /**
         * The actual label or synonym text that scored the match — surfaced so the UI can render
         * "↪ matches synonym 'Ammon's horn'" beneath the preferred label. Equals the term's label
         * when {@link #matchedVia} is {@code preferred_label}. Null when {@link #matchedVia} is null.
         */
        @Nullable String matchedText;
        /**
         * Distinct genes annotated with this term (including the descendant terms walked under
         * the request's {@code geneCountMaxTerms} cap). Populated only when the caller passed
         * {@code includeGeneCount=true}; null otherwise. Counts include propagation through
         * GO subClassOf descendants by default — set via {@code Gene2GOAssociationService.countByGOTermUris}.
         */
        @Nullable Long geneCount;
    }

    @Value
    public static class OntologyTermValueObject {
        String uri;
        String label;
        String definition;
        boolean obsolete;
        Integer usageCount;
        /**
         * Nearest is_a OR part_of parents of this term. Empty list when the term is a top-level node;
         * null only if the lookup was skipped (e.g. term has no URI).
         */
        @Nullable List<OntologyTermSimpleValueObject> parents;
    }

    @Value
    public static class OntologyTermSimpleValueObject {
        String uri;
        String label;
    }

    /**
     * Wire shape for {@code GET /annotations/categories} — carries the ontology term info
     * plus a preferred-prefix list per category (config-driven via
     * {@code annotation.category.prefixes}). Curation-ui passes the listed prefixes as
     * {@code ?prefixes=} on a downstream {@code /annotations/search} when the curator
     * scopes the search to this category. Empty list = no preference; client decides.
     */
    @Value
    public static class AnnotationCategoryValueObject {
        String uri;
        String label;
        List<String> preferredPrefixes;
    }

    /**
     * Fetch the raw ontology hits from the configured upstream gemma-rest. Server-to-server GET
     * against {@code <upstreamUrl>/rest/v2/annotations/search?query=<q>&rank=lucene&limit=UPSTREAM_LIMIT}.
     * Basic auth using the configured credentials when both username + password are non-blank;
     * otherwise issues unauthenticated (lets a local devbox point at a public upstream).
     * <p>
     * Parses the {@code data} array from the standard {@code ResponseDataObject} envelope and
     * lifts each hit into a {@link CharacteristicValueObject} carrying {@code value} / {@code valueUri}
     * / {@code category} / {@code categoryUri}. The local downstream pipeline (sort, prefix
     * filter, ranking, enrichment, truncation, usage counts) runs on these as if they came
     * from the local ontology service.
     */
    List<CharacteristicValueObject> fetchUpstreamHits( String query, long timeoutMs ) throws SearchException {
        long budget = Math.max( timeoutMs, 1 );
        try {
            String encoded = URLEncoder.encode( query, StandardCharsets.UTF_8 );
            URI target = URI.create( upstreamUrl.replaceAll( "/+$", "" )
                    + "/rest/v2/annotations/search?query=" + encoded
                    + "&rank=" + LuceneOrderRankingStrategy.NAME + "&limit=" + UPSTREAM_LIMIT );
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout( Duration.ofMillis( Math.min( budget, upstreamTimeoutMs ) ) )
                    .build();
            HttpRequest.Builder reqB = HttpRequest.newBuilder( target )
                    .timeout( Duration.ofMillis( Math.min( budget, upstreamTimeoutMs ) ) )
                    .header( "Accept", "application/json" )
                    .GET();
            if ( StringUtils.isNotBlank( upstreamUsername ) && StringUtils.isNotBlank( upstreamPassword ) ) {
                String credToken = Base64.getEncoder().encodeToString(
                        ( upstreamUsername + ":" + upstreamPassword ).getBytes( StandardCharsets.UTF_8 ) );
                reqB.header( "Authorization", "Basic " + credToken );
            }
            HttpResponse<String> resp = client.send( reqB.build(), HttpResponse.BodyHandlers.ofString() );
            if ( resp.statusCode() < 200 || resp.statusCode() >= 300 ) {
                throw new SearchException( "upstream annotation-search returned HTTP " + resp.statusCode()
                        + " for query '" + query + "'", null );
            }
            JsonNode root = upstreamMapper.readTree( resp.body() );
            JsonNode dataArr = root.path( "data" );
            List<CharacteristicValueObject> out = new ArrayList<>( Math.max( dataArr.size(), 16 ) );
            if ( dataArr.isArray() ) {
                for ( JsonNode hit : dataArr ) {
                    CharacteristicValueObject vo = new CharacteristicValueObject(
                            jsonString( hit, "value" ),
                            jsonString( hit, "valueUri" ),
                            jsonString( hit, "category" ),
                            jsonString( hit, "categoryUri" ) );
                    out.add( vo );
                }
            }
            return out;
        } catch ( SearchException e ) {
            throw e;
        } catch ( java.net.http.HttpTimeoutException e ) {
            // Wrap HttpTimeoutException (extends IOException) into the j.u.c.TimeoutException
            // the SearchTimeoutException constructor expects.
            java.util.concurrent.TimeoutException wrapped = new java.util.concurrent.TimeoutException(
                    "upstream annotation-search timed out after " + upstreamTimeoutMs + "ms for query '" + query + "'" );
            wrapped.initCause( e );
            throw new SearchTimeoutException( wrapped.getMessage(), wrapped );
        } catch ( Exception e ) {
            throw new SearchException( "upstream annotation-search failed for query '" + query
                    + "': " + e.getClass().getSimpleName() + ": " + e.getMessage(), e );
        }
    }

    @Nullable
    private static String jsonString( JsonNode hit, String field ) {
        JsonNode n = hit.path( field );
        return n.isMissingNode() || n.isNull() ? null : n.asText();
    }

    /**
     * Parse the {@code ?prefixes=} comma-separated list into a canonical lookup list. Trims each
     * entry, drops blanks, preserves caller order so the cache key is stable. Returns an empty
     * list when the param is null / blank.
     */
    private static List<String> parsePrefixes( @Nullable String raw ) {
        if ( raw == null || raw.trim().isEmpty() ) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for ( String token : raw.split( "," ) ) {
            String t = token.trim();
            if ( !t.isEmpty() ) {
                out.add( t );
            }
        }
        return out;
    }

    /**
     * Build a stable cache key from the raw query payload. Trims each term and lowercases for plain-text
     * matches (the underlying LIKE search and ontology lookup are case-insensitive); URI-shaped terms keep
     * their case because URIs are case-sensitive on the path/query portion.
     */
    private static String buildSearchCacheKey( List<String> values, String rankName, int limit, List<String> prefixes, boolean upstream, boolean exactLabel, String category ) {
        StringBuilder sb = new StringBuilder( values.size() * 16 );
        // Prefix the strategy name + limit + namespaces so swapping any of them invalidates the
        // cache without colliding with a different-query default-rank entry. STX separates the
        // header section from the query payload; SOH separates header fields.
        sb.append( rankName != null ? rankName : LuceneOrderRankingStrategy.NAME );
        sb.append( '' );
        sb.append( limit );
        sb.append( '' );
        sb.append( String.join( ",", prefixes ) );
        sb.append( '' );
        sb.append( upstream ? "u" : "l" );
        sb.append( '' );
        sb.append( exactLabel ? "e" : "s" );
        sb.append( '' );
        // Category affects whether synthetic gene rows are merged in, so it must key the cache
        // (lowercased + trimmed to canonicalize EFO label casing).
        sb.append( category != null ? category.trim().toLowerCase( Locale.ROOT ) : "" );
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
                    @ApiResponse(responseCode = "204", description = "Annotation removed."),
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
