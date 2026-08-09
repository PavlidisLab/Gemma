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
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.core.search.*;
import ubic.gemma.core.security.concurrent.DelegatingSecurityContextExecutorService;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicDao;
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

import com.fasterxml.jackson.annotation.JsonInclude;
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
     * Raw {@code annotation.category.excludedPrefixes} — same {@code key:prefix,...;key:...} shape
     * as the preference table, but a DENY list rather than an ordering hint.
     *
     * <p>Deliberately a separate property, not the inverse of the preference. The two make
     * different claims: a preference says a namespace is usually right, which is cheap to be wrong
     * about and so only reorders; an exclusion says a namespace is CATEGORICALLY impossible for the
     * position -- a chemical cannot be a genotype -- which is what justifies removing a row.
     *
     * <p>Measured evidence for keeping them separate (agents' 500-experiment run): across
     * categories that have a preference, 100 of 634 committed answers (16%) were out-of-namespace
     * and overwhelmingly CORRECT -- a treatment legitimately is an organism (`Mycobacterium
     * tuberculosis`), a role (`reference substance role`), a procedure (PBS) or an injury
     * (`traumatic brain injury`). Inverting the preference into a filter would have destroyed
     * those 100 to catch about 2. So `treatment` earns almost no exclusions despite having the
     * strongest preference, while `genotype` earns real ones: 10 measured cases answered a gene
     * symbol with the disease it causes (`RB1` -> retinoblastoma).
     */
    @org.springframework.beans.factory.annotation.Value("${annotation.category.excludedPrefixes:}")
    private String categoryExcludedPrefixesRaw;

    @Nullable
    private volatile Map<String, List<String>> categoryExcludedPrefixesByKey;

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
            List<OntologyTermSynonymValueObject> synonyms = collectSynonyms( term );
            List<String> alternativeIds = term.getAlternativeIds() != null
                    ? new ArrayList<>( term.getAlternativeIds() )
                    : Collections.emptyList();
            List<String> dbXrefs = collectDbXrefs( term );
            String ontologyVersion = term.getUri() != null
                    ? ontologyService.getVersion( term.getUri(), Math.max( 30000 - timer.getTime(), 0 ), TimeUnit.MILLISECONDS )
                    : null;
            return respond( new OntologyTermValueObject( term.getUri(), term.getLabel(), definition, term.isObsolete(), usageCount, parentVos, synonyms, alternativeIds, dbXrefs, ontologyVersion ) );
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
        cached = parseCategoryPrefixProperty( categoryPrefixesRaw );
        categoryPrefixesByKey = cached;
        return cached;
    }

    /** Parse a {@code key:prefix,prefix;key:...} property into an ordered per-key prefix list. */
    static Map<String, List<String>> parseCategoryPrefixProperty( @Nullable String raw ) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        if ( raw != null && !raw.trim().isEmpty() ) {
            for ( String entry : raw.split( ";" ) ) {
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
        return out;
    }

    /**
     * Map an ontology category label (e.g. {@code "cell type"}) to its property-key form
     * ({@code "cellType"}) — lowercase, split on non-alphanumerics, camelCase. Categories
     * outside the configured set fall through to an empty preference list.
     */
    static String categoryKey( String label ) {
        if ( label == null || label.isEmpty() ) return "";
        String lower = label.toLowerCase( Locale.ROOT );
        // An ontology that obsoletes a term renames its label rather than removing it: EFO
        // obsoleted its own `disease` in favour of MONDO's and the term now reads
        // `obsolete_disease`. Gemma still files ~15k annotations under the old URI, so the
        // category is very much alive while its label no longer matches anything configured --
        // `category=disease` worked, the URI form silently did not, and /annotations/categories
        // advertised no preference at all, which is how a client concludes none exists.
        // Stripping the marker keeps the two spellings on one key until the data migration
        // happens; see the disease-category note in project memory.
        if ( lower.startsWith( "obsolete_" ) ) {
            lower = lower.substring( "obsolete_".length() );
        }
        String[] parts = lower.split( "[^a-z0-9]+" );
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
     * Preferred ontology namespaces for a caller-supplied {@code category}, or an empty list when
     * the category is blank, unrecognised, or configured with no preference.
     *
     * <p>Accepts either the canonical category label ({@code treatment}, {@code organism part}) or
     * the category term's URI. The URI form is resolved against the in-memory category-term list;
     * a lookup failure yields no preference rather than an error, since the preference is an
     * ordering hint and a search that ignores it is still correct.</p>
     */
    private List<String> resolveCategoryPreferredPrefixes( @Nullable String category ) {
        if ( category == null || category.trim().isEmpty() ) {
            return Collections.emptyList();
        }
        return resolveCategoryPrefixes().getOrDefault(
                categoryKey( resolveCategoryLabel( category ) ), Collections.emptyList() );
    }

    /**
     * Normalize a caller-supplied category to its label. A URI is looked up in the in-memory
     * category-term list; anything else is already a label. A failed lookup returns the input, so
     * the caller simply finds no preference rather than erroring.
     */
    private String resolveCategoryLabel( String category ) {
        String c = category.trim();
        if ( c.startsWith( "http://" ) || c.startsWith( "https://" ) ) {
            try {
                for ( OntologyTerm t : ontologyService.getCategoryTerms() ) {
                    if ( c.equals( t.getUri() ) && t.getLabel() != null ) {
                        return t.getLabel();
                    }
                }
            } catch ( RuntimeException e ) {
                log.debug( "could not resolve category URI {} to a label", c, e );
            }
        }
        return c;
    }

    /**
     * Namespaces that are categorically impossible for a caller-supplied {@code category}.
     * Empty when nothing is configured, which is the default for every category.
     */
    private List<String> resolveCategoryExcludedPrefixes( @Nullable String category ) {
        if ( category == null || category.trim().isEmpty() ) {
            return Collections.emptyList();
        }
        Map<String, List<String>> cached = categoryExcludedPrefixesByKey;
        if ( cached == null ) {
            cached = parseCategoryPrefixProperty( categoryExcludedPrefixesRaw );
            categoryExcludedPrefixesByKey = cached;
        }
        return cached.getOrDefault( categoryKey( resolveCategoryLabel( category ) ), Collections.emptyList() );
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
                            null, null, null, null, null, null, null, null, null, null ) )
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
    public AnnotationSearchResponseDataObject searchAnnotations(
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
                    "`organism part`) or the matching EFO URI. Gene-symbol matches (value=symbol, " +
                    "valueUri=NCBI Gene URI, category=`gene`) are merged in unconditionally so " +
                    "STAT5B finds the gene whether the picker is on Genotype, Treatment, or a " +
                    "generic characteristic. When supplied WITHOUT an explicit `prefixes` " +
                    "allow-list, the category's preferred ontology namespaces (configured by " +
                    "`annotation.category.prefixes`, and readable per-category from " +
                    "`/annotations/categories`) promote *exactly-matching* hits in those " +
                    "namespaces to the front — so `FTC` under `treatment` leads with " +
                    "emtricitabine (CHEBI) rather than the identically-labelled MGI gene. " +
                    "Promotion never filters and never applies to near-matches; see " +
                    "`suppress_near_matches` for the filtering counterpart. The parameter keys " +
                    "the response cache.")
            @QueryParam("category") @DefaultValue("") String category,
            @Parameter(description = "When `true`, drop near-matches for *designation-shaped* " +
                    "queries — a single token carrying both letters and digits, which is the " +
                    "shape of a compound code (`MK-2206`, `GSK2879552`), a cell-line " +
                    "designation (`NCI-H358`), or a mouse strain (`C57BL/6J`). For those, a hit " +
                    "that merely shares a prefix or some tokens is never the same entity — " +
                    "`MK-8353` is a different compound from `MK-2206` — so only hits matching " +
                    "the query exactly on the preferred label or a declared synonym are kept " +
                    "(compared hyphen- and `cell`-suffix-insensitively, so `MK2206` still finds " +
                    "`MK-2206`). Descriptive queries (`diamide`, `high fat diet`) are left " +
                    "alone: there a near-match is often the right term, and returning nothing " +
                    "just pushes the caller onto a fuzzy fallback, which is the failure mode " +
                    "this flag exists to prevent. Off by default — typeahead needs prefix " +
                    "matching, and a curator mid-way through typing `NCI-H3` must still see " +
                    "candidates. Resolver-style callers that hold a complete name should pass " +
                    "`true`.\n\n" +
                    "When identity matching runs, the response carries a `negativeEvidence` " +
                    "object beside `data`. `solidMatch: false` is a CONFIDENT negative — we " +
                    "searched and nothing names this string — which an empty `data` array alone " +
                    "cannot tell you apart from \"the ontology wasn't loaded\" or \"this call " +
                    "never ran\". `ruledOut` lists the terms that came back and are NOT it, " +
                    "because knowing there is no match does not stop a downstream stage from " +
                    "proposing `mk-8353` for `MK-8722`, whereas knowing it is not `mk-8353` " +
                    "does. The ruled-out terms are deliberately kept OUT of `data`, so a client " +
                    "that ignores the new field can never pick one up by reading `data[0]`.")
            @QueryParam("suppress_near_matches") @DefaultValue("false") boolean suppressNearMatches,
            @Parameter(description = "Optional taxon hint to scope gene fan-out. Accepts the same " +
                    "TaxonArg forms as elsewhere (common name `mouse`, scientific name `Mus musculus`, " +
                    "NCBI taxonomy id `10090`, or numeric Gemma taxon id). When supplied, gene " +
                    "matches are restricted to that taxon — typing `Il10` on a mouse dataset no " +
                    "longer surfaces rat/human Il10 rows. When omitted (default), gene fan-out " +
                    "returns all taxa as before. The ontology search side is unaffected.")
            @QueryParam("taxon") @Nullable TaxonArg<?> taxonArg,
            @Parameter(description = "When true, populate `geneCount` on each hit with the distinct " +
                    "number of genes annotated to that GO term (including descendants walked under " +
                    "the `geneCountMaxTerms` cap). Adds a parallel fan-out at the response shape " +
                    "boundary; sub-100ms on a warm GO index. Default false.")
            @QueryParam("includeGeneCount") @DefaultValue("false") boolean includeGeneCount,
            @Parameter(description = "When `includeGeneCount=true`, cap the per-hit BFS descendant " +
                    "walk at this many GO terms. Default 50 (bounds broad parents like `metabolic " +
                    "process` to ~50ms per hit). 0 = unbounded; not recommended for typeahead.")
            @QueryParam("geneCountMaxTerms") @DefaultValue("50") int geneCountMaxTerms,
            @Parameter(description = "When `true` (default), merge synthetic gene rows (symbol / name / "
                    + "alias matches) into the results — the STAT5B-finds-the-gene behaviour. Each query "
                    + "token fires three sequential GeneService probes, the dominant slice of the per-call "
                    + "latency floor. Resolver-style callers working a non-gene position (organism part, "
                    + "disease, …) pass `false` to skip the fan-out entirely and cut that floor.")
            @QueryParam("includeGenes") @DefaultValue("true") boolean includeGenes,
            @Parameter(description = "When `true`, attach to each hit one representative ACL-visible example of "
                    + "how the term has actually been used in the corpus (owning factor value + factor, and the "
                    + "statement triple when present, plus the source dataset id) — for showing a rare/unfamiliar "
                    + "term in context in a picker. One batched, ACL-filtered lookup for the result page; default "
                    + "false so the common typeahead path pays nothing. Gate rendering client-side (e.g. only for "
                    + "low `usageCount`).")
            @QueryParam("includeExampleUsage") @DefaultValue("false") boolean includeExampleUsage
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
        // Resolve the optional taxon hint up-front so a bad value 400s before we hit the cache or
        // launch the ontology fan-out. Null taxon = no constraint (legacy behaviour).
        Taxon taxon = taxonArg != null ? taxonArgService.getEntity( taxonArg ) : null;
        try {
            SearchOutcome outcome = searchOne( query.getValue(), strategy, limit, prefixes, upstream,
                    exactLabel, category, suppressNearMatches, taxon, includeGenes, includeGeneCount,
                    geneCountMaxTerms, includeExampleUsage );
            return new AnnotationSearchResponseDataObject( outcome.results, outcome.negativeEvidence );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }
    }

    /**
     * Shared core behind both {@code GET /annotations/search} and {@code POST /annotations/search/batch}:
     * response-cache lookup, {@link #getTerms} find/rank/enrich, optional gene-count enrichment, and
     * cache population. Kept separate so the batch endpoint reuses the SAME per-query cache (repeat
     * labels across a batch, or a later single GET for the same label, hit the warm entry) and the same
     * timeout / caching semantics without duplicating logic.
     *
     * @param queryValues the literal query token(s) — NOT comma-split (the batch passes a single label
     *                    verbatim, so a label containing a comma like "CD4-positive, alpha-beta T cell"
     *                    stays one query rather than being unioned across two).
     */
    private SearchOutcome searchOne( List<String> queryValues,
            AnnotationSearchRankingStrategy strategy, int limit, List<String> prefixes, boolean upstream,
            boolean exactLabel, String category, boolean suppressNearMatches, @Nullable Taxon taxon,
            boolean includeGenes, boolean includeGeneCount, int geneCountMaxTerms,
            boolean includeExampleUsage ) throws SearchException {
        // Cache key includes includeGeneCount + geneCountMaxTerms so a "with counts" call doesn't
        // hit a cached "without counts" payload. Taxon affects gene fan-out, so it keys too; and
        // includeGenes=false is a different response shape, so it gets its own "|ng" suffix.
        // includeExampleUsage adds a field, so it keys too ("|eu").
        String cacheKey = buildSearchCacheKey( queryValues, strategy.getName(), limit, prefixes, upstream, exactLabel, category, taxon )
                + ( includeGeneCount ? "|gc=" + geneCountMaxTerms : "" )
                + ( includeGenes ? "" : "|ng" )
                + ( includeExampleUsage ? "|eu" : "" )
                // Near-match suppression removes rows, so a suppressed response must never be
                // served to a caller that did not ask for it (nor the reverse).
                + ( suppressNearMatches ? "|snm" : "" );
        org.springframework.cache.Cache searchCache = searchResponseCache();
        if ( searchCache != null ) {
            org.springframework.cache.Cache.ValueWrapper hit = searchCache.get( cacheKey );
            if ( hit != null && hit.get() instanceof SearchOutcome ) {
                SearchOutcome cached = ( SearchOutcome ) hit.get();
                log.debug( "annotation-search cache HIT key={}", cacheKey );
                return new SearchOutcome( new ArrayList<>( cached.results ), cached.negativeEvidence );
            }
        }
        SearchOutcome outcome = this.getTerms( queryValues, strategy, limit, prefixes, upstream, exactLabel, category, suppressNearMatches, taxon, includeGenes, FIND_CHARACTERISTICS_TIMEOUT_MS );
        List<AnnotationSearchResultValueObject> result = new ArrayList<>( outcome.results );
        if ( includeGeneCount && !result.isEmpty() ) {
            result = attachGeneCounts( result, geneCountMaxTerms );
        }
        if ( includeExampleUsage && !result.isEmpty() ) {
            attachExampleUsage( result );
        }
        SearchOutcome toReturn = new SearchOutcome( result, outcome.negativeEvidence );
        // Cache only non-empty results. An empty hit list is almost always either (a) genuinely
        // no match, where re-running is cheap, or (b) a transient gap (ontologies still warming
        // after a restart, basecode Lucene index temporarily empty, etc.) — caching the empty
        // would pin the typeahead at "no results" until either an explicit cache flush or a
        // restart. Both classes lose nothing by recomputing.
        //
        // A suppressed-to-empty designation result is deliberately NOT cached either, even though
        // its negative evidence is the most useful thing we produce. A confident "nothing names
        // this" computed while CHEBI was mid-reload would be WRONG, and caching it would pin that
        // wrong negative until an explicit flush — licensing a resolver to abstain on terms that
        // do exist. Recomputing an empty is cheap; a stale confident negative is not.
        if ( !result.isEmpty() && searchCache != null ) {
            searchCache.put( cacheKey, toReturn );
            log.debug( "annotation-search cache MISS key={} stored {} hits", cacheKey, result.size() );
        } else if ( result.isEmpty() ) {
            log.debug( "annotation-search cache MISS key={} empty result — not cached", cacheKey );
        }
        return toReturn;
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

    /** Upper bound on the number of queries a single {@code /search/batch} request may carry. */
    static final int SEARCH_BATCH_MAX_ITEMS = 200;
    /** Per-request worker cap for the batch fan-out; mirrors {@link #enrichTopHits}'s bound to keep Jena/DB read pressure sane. */
    static final int SEARCH_BATCH_PARALLELISM = 8;

    @POST
    @Path("/search/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Batch search for annotation tags",
            description = "Resolve many labels in ONE request. Each item is searched independently (NOT "
                    + "unioned into a single result set the way a comma-delimited ?query= is) and returns its "
                    + "own ordered result list, so a curation resolver can collapse the dozens of round-trips a "
                    + "proposal pass would otherwise fire into a single call. Per-item work runs in parallel "
                    + "server-side (bounded to " + SEARCH_BATCH_PARALLELISM + " workers) and reuses the SAME "
                    + "response cache as GET /annotations/search, so repeated labels — within the batch or across "
                    + "later calls — are free. The shared knobs (rank, limit, prefixes, exactLabel, "
                    + "suppressNearMatches, taxon, includeGenes, includeGeneCount, geneCountMaxTerms) apply to "
                    + "every item; only category is "
                    + "per-item. A per-item failure is reported in that item's `error` field and does NOT fail the "
                    + "batch. At most " + SEARCH_BATCH_MAX_ITEMS + " items per request.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "The batch is empty / oversized, or a shared parameter is invalid.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "503", description = FIND_CHARACTERISTICS_TIMEOUT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public ResponseDataObject<List<AnnotationSearchBatchResultValueObject>> searchAnnotationsBatch( @Nullable AnnotationSearchBatchRequest body ) {
        if ( body == null || body.getQueries() == null || body.getQueries().isEmpty() ) {
            throw new BadRequestException( "A non-empty 'queries' array is required." );
        }
        if ( body.getQueries().size() > SEARCH_BATCH_MAX_ITEMS ) {
            throw new BadRequestException( "The batch may carry at most " + SEARCH_BATCH_MAX_ITEMS
                    + " queries (got " + body.getQueries().size() + ")." );
        }
        int limit = body.getLimit() != null ? body.getLimit() : SEARCH_DEFAULT_LIMIT;
        if ( limit < 1 || limit > SEARCH_MAX_LIMIT ) {
            throw new BadRequestException( "The 'limit' parameter must be between 1 and " + SEARCH_MAX_LIMIT + " (got " + limit + ")." );
        }
        int geneCountMaxTerms = body.getGeneCountMaxTerms() != null ? body.getGeneCountMaxTerms() : 50;
        if ( geneCountMaxTerms < 0 ) {
            throw new BadRequestException( "geneCountMaxTerms must be >= 0." );
        }
        // Resolve the shared knobs once — a bad rank / taxon 400s the whole batch up-front rather than
        // failing every item identically.
        AnnotationSearchRankingStrategy strategy = resolveRankingStrategy( body.getRank() );
        List<String> prefixes = parsePrefixes( body.getPrefixes() != null ? body.getPrefixes() : "" );
        boolean exactLabel = Boolean.TRUE.equals( body.getExactLabel() );
        boolean suppressNearMatches = Boolean.TRUE.equals( body.getSuppressNearMatches() );
        boolean includeGenes = body.getIncludeGenes() == null || body.getIncludeGenes(); // default true
        boolean includeGeneCount = Boolean.TRUE.equals( body.getIncludeGeneCount() );
        Taxon taxon = body.getTaxon() != null && !body.getTaxon().trim().isEmpty()
                ? taxonArgService.getEntity( TaxonArg.valueOf( body.getTaxon().trim() ) )
                : null;

        List<AnnotationSearchBatchRequest.Item> items = body.getQueries();
        // Pre-sized, index-addressed so parallel workers write disjoint slots (no shared-collection sync).
        List<AnnotationSearchBatchResultValueObject> out = new ArrayList<>( Collections.nCopies( items.size(), null ) );
        int parallelism = Math.min( items.size(), SEARCH_BATCH_PARALLELISM );
        // DelegatingSecurityContextExecutorService propagates the caller's SecurityContext onto the
        // worker threads — without it the ACL-filtered usage-count leg would silently run anonymous and
        // a curator would get public-only counts. Adopt-first: this is the repo's own wrapper.
        java.util.concurrent.ExecutorService pool = new DelegatingSecurityContextExecutorService(
                java.util.concurrent.Executors.newFixedThreadPool( parallelism ) );
        try {
            List<java.util.concurrent.Future<?>> tasks = new ArrayList<>( items.size() );
            for ( int i = 0; i < items.size(); i++ ) {
                final int idx = i;
                final AnnotationSearchBatchRequest.Item item = items.get( i );
                tasks.add( pool.submit( () -> out.set( idx, runBatchItem( item, strategy, limit, prefixes,
                        exactLabel, suppressNearMatches, taxon, includeGenes, includeGeneCount, geneCountMaxTerms ) ) ) );
            }
            for ( java.util.concurrent.Future<?> f : tasks ) {
                try {
                    f.get();
                } catch ( InterruptedException ie ) {
                    Thread.currentThread().interrupt();
                    break;
                } catch ( java.util.concurrent.ExecutionException ee ) {
                    // runBatchItem swallows its own failures into an error entry, so reaching here means
                    // an unexpected fault; log and leave the slot null (backfilled below).
                    log.warn( "batch annotation-search task failed unexpectedly", ee.getCause() );
                }
            }
        } finally {
            pool.shutdownNow();
        }
        // Backfill any slot a worker never wrote (interrupt / unexpected fault) so the response array
        // stays 1:1 with the request array.
        for ( int i = 0; i < items.size(); i++ ) {
            if ( out.get( i ) == null ) {
                AnnotationSearchBatchRequest.Item item = items.get( i );
                out.set( i, new AnnotationSearchBatchResultValueObject(
                        item != null ? item.getQuery() : null,
                        item != null ? item.getCategory() : null,
                        Collections.emptyList(), "search did not complete" ) );
            }
        }
        return respond( out );
    }

    /**
     * Resolve a single batch item through the shared {@link #searchOne} core, converting any failure into
     * an {@code error} entry so one bad label never sinks the whole batch. Runs on a worker thread with the
     * caller's SecurityContext propagated (see {@link #searchAnnotationsBatch}).
     */
    private AnnotationSearchBatchResultValueObject runBatchItem( @Nullable AnnotationSearchBatchRequest.Item item,
            AnnotationSearchRankingStrategy strategy, int limit, List<String> prefixes, boolean exactLabel,
            boolean suppressNearMatches, @Nullable Taxon taxon, boolean includeGenes, boolean includeGeneCount,
            int geneCountMaxTerms ) {
        String q = item != null ? item.getQuery() : null;
        String category = item != null && item.getCategory() != null ? item.getCategory() : "";
        if ( q == null || q.trim().isEmpty() ) {
            return new AnnotationSearchBatchResultValueObject( q, category, Collections.emptyList(), "query is empty" );
        }
        try {
            // Single literal label — NOT wrapped in StringArrayArg, so an embedded comma stays one query.
            SearchOutcome outcome = searchOne( Collections.singletonList( q ),
                    strategy, limit, prefixes, false, exactLabel, category, suppressNearMatches, taxon,
                    includeGenes, includeGeneCount, geneCountMaxTerms, false );
            return new AnnotationSearchBatchResultValueObject( q, category, outcome.results, null,
                    outcome.negativeEvidence );
        } catch ( SearchException | RuntimeException e ) {
            log.debug( "batch annotation-search item '{}' failed: {}", q, e.toString() );
            return new AnnotationSearchBatchResultValueObject( q, category, Collections.emptyList(), e.getMessage() );
        }
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
        return searchAnnotations( query, LuceneOrderRankingStrategy.NAME, SEARCH_DEFAULT_LIMIT, "", false, false, "", false, null, false, 50, true, false );
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
    private SearchOutcome getTerms( List<String> queryValues,
            AnnotationSearchRankingStrategy strategy, int limit, List<String> prefixes, boolean upstream,
            boolean exactLabel, String category, boolean suppressNearMatches, @Nullable Taxon taxon,
            boolean includeGenes, long timeoutMs ) throws SearchException {
        StopWatch timer = StopWatch.createStarted();
        long phaseStart = timer.getTime();
        List<CharacteristicValueObject> rawHits = new ArrayList<>();
        // GO is a fallback in the ontology search (consulted only when other ontologies find nothing).
        // When the caller filters to the GO_ URI prefix they explicitly want GO terms, so force GO to
        // be consulted even if another ontology also matched — otherwise the post-filter yields nothing.
        boolean forceGeneOntology = prefixes != null && prefixes.stream()
                .anyMatch( p -> p != null && ( StringUtils.startsWithIgnoreCase( p.trim(), "GO_" ) || p.trim().equalsIgnoreCase( "GO" ) ) );
        for ( String query : queryValues ) {
            query = query.trim();
            // A full term URI (http://...) OR a recognized CURIE (e.g. EFO:0600015, GO:0008150)
            // takes the exact-URI lookup path instead of a free-text search. expandTermQueryToUri
            // returns the canonical URI for both shapes, or null for plain free text.
            String termUri = expandTermQueryToUri( query );
            if ( termUri != null ) {
                rawHits.addAll( characteristicService.loadValueObjects( characteristicService
                        .findByUri( termUri, null, null, true, -1 ) ) );
            } else if ( upstream ) {
                // Delegate the ontology Lucene-index lookup; downstream pipeline runs locally
                // against shared gemd. Failure here propagates as SearchException so the caller
                // sees a 5xx (or 503 via the existing timeout mapping), rather than silently
                // falling back to local — the curator explicitly asked for staging's index.
                rawHits.addAll( fetchUpstreamHits( query, Math.max( timeoutMs - timer.getTime(), 0 ) ) );
            } else {
                rawHits.addAll( ontologyService.findExperimentsCharacteristicTags( query, 1000, false, forceGeneOntology, Math.max( timeoutMs - timer.getTime(), 0 ), TimeUnit.MILLISECONDS ) );
            }
        }
        long tFindCharacteristics = timer.getTime() - phaseStart;
        int rawCount = rawHits.size();
        // Order hits by relevance tier (exact label ≺ starts-with ≺ all-tokens-covered ≺
        // word-boundary-contains ≺ substring ≺ other), with the prefixes parameter's order
        // honoured as the next tier and URI ASC as the deterministic tiebreaker. Without the
        // tier sort, a typeahead query for "synaptic" returns "acetylcholine catabolic process
        // in synaptic cleft" before "chemical synaptic transmission" because the URI sort
        // ranks by GO-ID ascending.
        //
        // The all-tokens-covered tier (tier 2) sits between startsWith and word-boundary so a
        // multi-token query like "uzh-2 cell" promotes "uzh-2 cell line" (covers both content
        // tokens) above "cell line sample study" (covers "cell" only). This is the demotion
        // half of the fix in handoffs/ANNOTATIONS_SEARCH_OR_OVER_TOKENS_2026_06_12.md — a
        // candidate-stage AND filter would have killed legitimate synonym-only matches like
        // "ammon's horn" → "hippocampus" (label contains neither token), so we DEMOTE noise
        // via tier rather than dropping it.
        String joinedRelevanceQuery = String.join( " ", queryValues ).trim();
        String relevanceQuery = joinedRelevanceQuery.toLowerCase( Locale.ROOT );
        // Canonical form of the query used for hyphen-and-cell-suffix-insensitive equality:
        // MEC2 ↔ MEC-2 ↔ MEC-2 cell all canonicalise to "mec2". See javadoc on
        // canonicaliseForExactMatch.
        String relevanceQueryCanon = canonicaliseForExactMatch( relevanceQuery );
        List<String> queryContentTokens = contentTokens( joinedRelevanceQuery );
        boolean multiToken = queryContentTokens.size() >= 2;
        java.util.function.ToIntFunction<CharacteristicValueObject> tierFn = h -> {
            String label = h.getValue();
            if ( label == null ) return 6;
            String l = label.toLowerCase( Locale.ROOT );
            // CLO labels cell-line terms as "<NAME> cell" (e.g. "A549 cell") rather than the
            // bare "<NAME>"; treat that trailing " cell" / " cell line" as strippable so a
            // bare-name query like "A549" reaches CLO at the exact-label tier instead of
            // losing to EFO's bare "a549" by tier alone.
            String ls = stripCellSuffix( l );
            // Canonical form: also drops ASCII hyphens, so "mec-2 cell" / "mec2" / "MEC-2" all
            // collapse to the same key. Catches identifier-shaped queries that vary in
            // punctuation (MEC2 ↔ MEC-2 ↔ MEC-2 cell, NCI-H358 ↔ NCIH358, RPMI-8226 ↔ RPMI8226).
            String lCanon = canonicaliseForExactMatch( l );
            if ( l.equals( relevanceQuery ) || ls.equals( relevanceQuery )
                    || lCanon.equals( relevanceQueryCanon ) ) return 0;                            // exact label
            if ( l.startsWith( relevanceQuery ) || ls.startsWith( relevanceQuery )
                    || lCanon.startsWith( relevanceQueryCanon ) ) return 1;                        // label starts with query
            if ( multiToken && labelCoversAllTokens( normaliseForEquality( label ), queryContentTokens ) ) {
                return 2;                                       // all query content tokens present
            }
            // word-boundary contains: query appears at the start of any token in the label
            int idx = l.indexOf( relevanceQuery );
            if ( idx > 0 && !Character.isLetterOrDigit( l.charAt( idx - 1 ) ) ) return 3;
            if ( idx >= 0 ) return 4;                          // raw substring
            return 5;                                          // URI / synonym / definition only
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
        // Within the same tier, prefer the CLO term over EFO when the CLO label EARNED its
        // tier through the " cell" suffix strip — that's the signal that CLO meant this term
        // as a cell-line label and is the authoritative source. Without this, an EFO bare
        // "a549" and a CLO "a549 cell" both land in tier 0 and the URI tiebreaker decides
        // (accidentally CLO-first on host alphabetics, but not by design). Gate keeps this
        // surgical: a non-cell-line query like "lung cancer" sees no behavioural change
        // because no label is stripped.
        java.util.function.ToIntFunction<CharacteristicValueObject> cellLinePreferenceFn = h -> {
            String label = h.getValue();
            String uri = h.getValueUri();
            if ( label == null || uri == null ) return 1;
            String l = label.toLowerCase( Locale.ROOT );
            String lCanon = canonicaliseForExactMatch( l );
            // Normalisation earned the match when EITHER side needed to be normalised AND the
            // canonical forms equal-or-prefix-match. Covers both the cell-suffix-strip case
            // (A549 ↔ "A549 cell") and the hyphen-strip case (MEC2 ↔ "MEC-2 cell"). Keeps
            // the CLO-preference surgical: a query that matches a CLO label raw doesn't
            // trigger the preference here (the URI tiebreaker decides on its own merits).
            boolean normalisationOccurred = !lCanon.equals( l ) || !relevanceQueryCanon.equals( relevanceQuery );
            boolean canonicalMatch = lCanon.equals( relevanceQueryCanon ) || lCanon.startsWith( relevanceQueryCanon );
            return ( normalisationOccurred && canonicalMatch && uri.contains( "CLO_" ) ) ? 0 : 1;
        };
        rawHits.sort( Comparator
                .<CharacteristicValueObject>comparingInt( tierFn::applyAsInt )
                .thenComparingInt( prefixRankFn::applyAsInt )
                .thenComparingInt( cellLinePreferenceFn::applyAsInt )
                .thenComparing( CharacteristicValueObject::getValueUri, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( CharacteristicValueObject::getValue, Comparator.nullsLast( Comparator.naturalOrder() ) ) );
        // Runs BEFORE the exact_label and prefixes filters on purpose. Those narrow the
        // POSITIVE list; this produces the VERDICT, and the two answer different questions.
        // With this after exact_label, a caller passing both got an empty data array and no
        // negativeEvidence at all -- the exact ambiguity the block exists to remove, and
        // silent, since asking for the signal and receiving nothing looks like no signal.
        // ---- Category preference + near-match suppression -------------------------------------
        //
        // Both need to know WHY each candidate matched, and both have to act before ranking and
        // truncation, so the attribution that /annotations/search already reports per hit is
        // resolved here for the candidate set rather than only for the surviving top-N.
        //
        // The two act differently on purpose. A hit in the wrong namespace gets DEMOTED, never
        // dropped: the caller can see the URI and judge for itself, and dropping risks emptying a
        // result set that held the right answer under an unexpected namespace. A near-match gets
        // DROPPED, because it is invisible — "MK-8353" looks exactly like a legitimate answer to
        // "MK-2206" unless you already know the compound, which is precisely what the caller was
        // asking us.
        String designationProbe = queryValues.size() == 1 ? queryValues.get( 0 ) : joinedRelevanceQuery;
        boolean suppress = suppressNearMatches && isDesignationQuery( designationProbe );
        NegativeEvidenceValueObject negativeEvidence = null;
        List<RuledOutTermValueObject> ruledOut = new ArrayList<>();
        boolean nearMissTruncated = false;
        boolean excludedAny = false;
        // An explicit prefixes allow-list is the caller overriding namespace choice outright; do
        // not layer a category preference on top of it.
        List<String> preferredPrefixes = prefixes.isEmpty()
                ? resolveCategoryPreferredPrefixes( category )
                : Collections.emptyList();
        // Exclusions are honoured even when the caller supplied an explicit prefixes allow-list:
        // that list expresses which namespaces the caller WANTS, not which are possible for the
        // position, and an impossible one stays impossible either way.
        List<String> excludedPrefixes = resolveCategoryExcludedPrefixes( category );
        if ( suppress || !preferredPrefixes.isEmpty() || !excludedPrefixes.isEmpty() ) {
            if ( rawHits.size() > CANDIDATE_ATTRIBUTION_CAP ) {
                // Never silently. A capped run can only under-promote / under-suppress (the tail
                // is left exactly as the relevance tiers ordered it), but the operator should be
                // able to see that the tail was not considered.
                log.info( "annotation-search: {} candidates for query='{}' exceeds the {} attribution cap; "
                                + "category promotion and near-match suppression consider the top {} only",
                        rawHits.size(), joinedRelevanceQuery, CANDIDATE_ATTRIBUTION_CAP, CANDIDATE_ATTRIBUTION_CAP );
            }
            // Attribution is only needed to decide what NAMES the query -- suppression and
            // promotion both turn on that. Exclusion turns on the URI alone, so a category that
            // configures only exclusions must not pay for up to 200 per-URI term lookups; the
            // ruled-out rows fall back to label-level attribution, which is free.
            Map<String, MatchAttribution> candidateMatches =
                    ( rawHits.isEmpty() || !( suppress || !preferredPrefixes.isEmpty() ) )
                            ? Collections.emptyMap()
                            : attributeCandidates( rawHits, joinedRelevanceQuery,
                                    CANDIDATE_ATTRIBUTION_CAP, Math.max( timeoutMs - timer.getTime(), 0 ) );
            // A hit is "solid" when it names the query: attribution says equality against the
            // preferred label or a declared synonym. The label fallback keeps the check working
            // when the owning ontology is not loaded and no term could be resolved — the hit's own
            // label is on the row already, so a label-exact match stays verifiable without Jena.
            java.util.function.Predicate<CharacteristicValueObject> solid = h -> {
                String uri = h.getValueUri();
                MatchAttribution m = uri != null ? candidateMatches.get( uri ) : null;
                if ( isExactAttribution( m != null ? m.via : null ) ) {
                    return true;
                }
                return !relevanceQueryCanon.isEmpty()
                        && canonicaliseForExactMatch( h.getValue() ).equals( relevanceQueryCanon );
            };
            if ( suppress ) {
                List<CharacteristicValueObject> kept = new ArrayList<>( rawHits.size() );
                List<RuledOutTermValueObject> rejected = new ArrayList<>();
                for ( CharacteristicValueObject h : rawHits ) {
                    if ( solid.test( h ) ) {
                        kept.add( h );
                    } else if ( rejected.size() < RULED_OUT_MAX ) {
                        // Keep WHAT was rejected, not just how many. The caller's next stage will
                        // meet these strings again from its own index; "no match" does not stop it
                        // proposing mk-8353 for MK-8722, but "it is not mk-8353" does.
                        String uri = h.getValueUri();
                        MatchAttribution m = uri != null ? candidateMatches.get( uri ) : null;
                        if ( m == null ) {
                            // Same label-level fallback as the positive rows: "why was this
                            // returned at all" is the useful half of a ruled-out entry, and a
                            // list of nulls explains nothing to whoever reviews the run.
                            m = computeLabelAttribution( h.getValue(), joinedRelevanceQuery );
                        }
                        rejected.add( new RuledOutTermValueObject( h.getValue(), uri,
                                m != null ? m.via.token : null,
                                RuledOutTermValueObject.REASON_NEAR_MATCH ) );
                    }
                }
                int droppedTotal = rawHits.size() - kept.size();
                if ( droppedTotal > 0 ) {
                    log.debug( "annotation-search: near-match suppression dropped {} of {} candidates for designation query '{}'",
                            droppedTotal, rawHits.size(), joinedRelevanceQuery );
                }
                nearMissTruncated = droppedTotal > rejected.size();
                ruledOut.addAll( rejected );
                rawHits = kept;
            }
            if ( !excludedPrefixes.isEmpty() && !rawHits.isEmpty() ) {
                // A namespace that is impossible for this category. Unlike a near-match these rows
                // usually DO name the query -- they are the right concept filed under the wrong
                // kind of thing (a gene symbol answered with the disease it causes), so they are
                // reported rather than deleted: if a rule ever over-fires it shows up in ruledOut
                // instead of vanishing, and an out-of-category hit is itself a signal that the
                // FACTOR may be mis-categorised.
                List<CharacteristicValueObject> keptInCategory = new ArrayList<>( rawHits.size() );
                for ( CharacteristicValueObject h : rawHits ) {
                    String uri = h.getValueUri();
                    boolean impossible = false;
                    if ( uri != null ) {
                        for ( String bad : excludedPrefixes ) {
                            if ( uri.contains( bad ) ) {
                                impossible = true;
                                break;
                            }
                        }
                    }
                    if ( !impossible ) {
                        keptInCategory.add( h );
                    } else if ( ruledOut.size() < RULED_OUT_MAX ) {
                        MatchAttribution m = candidateMatches.get( uri );
                        if ( m == null ) {
                            m = computeLabelAttribution( h.getValue(), joinedRelevanceQuery );
                        }
                        ruledOut.add( new RuledOutTermValueObject( h.getValue(), uri,
                                m != null ? m.via.token : null,
                                RuledOutTermValueObject.REASON_OUT_OF_CATEGORY ) );
                    }
                }
                if ( keptInCategory.size() < rawHits.size() ) {
                    log.debug( "annotation-search: category '{}' excluded {} of {} hits by namespace",
                            category, rawHits.size() - keptInCategory.size(), rawHits.size() );
                    excludedAny = true;
                }
                rawHits = keptInCategory;
            }
            if ( suppress || excludedAny ) {
                negativeEvidence = new NegativeEvidenceValueObject( joinedRelevanceQuery,
                        !rawHits.isEmpty(), ruledOut, nearMissTruncated );
            }
            if ( !preferredPrefixes.isEmpty() ) {
                // Promote solid hits sitting in the category's preferred namespaces, in the
                // configured namespace order. Near-matches are deliberately NOT promotable: a
                // CHEBI term that merely contains the query would otherwise leapfrog an exact hit
                // from another ontology, which trades one bad ranking for another.
                java.util.function.ToIntFunction<CharacteristicValueObject> categoryRankFn = h -> {
                    String uri = h.getValueUri();
                    if ( uri == null || !solid.test( h ) ) {
                        return preferredPrefixes.size();
                    }
                    for ( int i = 0; i < preferredPrefixes.size(); i++ ) {
                        if ( uri.contains( preferredPrefixes.get( i ) ) ) {
                            return i;
                        }
                    }
                    return preferredPrefixes.size();
                };
                // Stable: everything not promoted keeps the relevance order computed above.
                rawHits = new ArrayList<>( rawHits );
                rawHits.sort( Comparator.<CharacteristicValueObject>comparingInt( categoryRankFn::applyAsInt ) );
            }
        }
        // Exact-label pushdown for resolver-style callers (cuts 5-10x candidate payload).
        // Case-insensitive equality against the trimmed query — mirrors the trim+lowercase
        // that callers do client-side today. Applies AFTER the canonical sort so the kept
        // subset is also deterministic. Empty result is a valid outcome.
        if ( exactLabel ) {
            String wantedLower = queryValues.stream()
                    .map( s -> s != null ? s.trim().toLowerCase( Locale.ROOT ) : "" )
                    .filter( s -> !s.isEmpty() )
                    .findFirst()
                    .orElse( "" );
            if ( !wantedLower.isEmpty() ) {
                // Apply the same canonicalisation the ranker's tier function uses, so a
                // resolver passing exact_label=true together with prefixes=CLO_,CL_ for
                // "MEC2" still finds CLO_0037182 (label "MEC-2 cell"). Without this, the
                // exact_label filter degrades to literal toLowerCase equality and drops
                // every hit whose label varies from the query by " cell" / hyphens —
                // exactly the cases the ranker just promoted to tier 0.
                String wantedCanon = canonicaliseForExactMatch( wantedLower );
                List<CharacteristicValueObject> exact = new ArrayList<>( rawHits.size() );
                for ( CharacteristicValueObject h : rawHits ) {
                    String label = h.getValue();
                    if ( label == null ) continue;
                    String labelLower = label.trim().toLowerCase( Locale.ROOT );
                    if ( labelLower.equals( wantedLower )
                            || canonicaliseForExactMatch( labelLower ).equals( wantedCanon ) ) {
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
        String joinedQuery = String.join( " ", queryValues );
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

        // Prior-category breakdown for the top-N kept URIs: how often has this URI been tagged
        // under each category by prior curators? One small grouped query against EE2C —
        // cacheable + EE2C-synchronised so curator-driven tag changes invalidate cleanly. Gives
        // resolvers a corpus-history signal to break ambiguous label hits (e.g. MEC-2 used 14×
        // as cell line, 1× as protein → resolver picks cell line regardless of which label
        // matched). Same URI set as topCounts so the IN-clause is tiny.
        Map<String, Map<String, Integer>> priorCategoriesByUri = Collections.emptyMap();
        if ( !ranked.isEmpty() ) {
            Set<String> topUrisForPriorCat = ranked.stream()
                    .map( CharacteristicValueObject::getValueUri )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() );
            if ( !topUrisForPriorCat.isEmpty() ) {
                Map<String, Map<String, Long>> raw = characteristicService.findEeCountsByUriGroupedByCategory( topUrisForPriorCat );
                if ( raw != null && !raw.isEmpty() ) {
                    priorCategoriesByUri = new HashMap<>( raw.size() );
                    for ( Map.Entry<String, Map<String, Long>> e : raw.entrySet() ) {
                        Map<String, Integer> inner = new HashMap<>( e.getValue().size() );
                        e.getValue().forEach( ( k, v ) -> inner.put( k, v.intValue() ) );
                        priorCategoriesByUri.put( e.getKey(), inner );
                    }
                }
            }
        }
        long tPriorCategories = timer.getTime() - phaseStart;
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
                    "annotation-search: query='%s' raw=%d top=%d total=%dms (find=%dms filter=%dms counts=%dms rank=%dms topCounts=%dms priorCats=%dms enrich=%dms)",
                    queryValues, rawCount, topUris.size(), timer.getTime(),
                    tFindCharacteristics, tFilters, tCounts, tRank, tTopCounts, tPriorCategories, tEnrich ) );
        }

        LinkedHashSet<AnnotationSearchResultValueObject> vos = new LinkedHashSet<>();
        for ( CharacteristicValueObject vo : ranked ) {
            Integer count = vo.getValueUri() != null ? countsByUri.getOrDefault( vo.getValueUri(), 0 ) : null;
            String uri = vo.getValueUri();
            boolean isTop = uri != null && topUris.contains( uri );
            String definition = isTop ? defByUri.get( uri ) : null;
            List<OntologyTermSimpleValueObject> parents = isTop ? parentsByUri.get( uri ) : null;
            MatchAttribution match = isTop ? matchByUri.get( uri ) : null;
            if ( match == null && isTop ) {
                // Attribution was attempted and produced nothing — the URI resolved to no loaded
                // term (a flat lexical catalogue, an NCBITaxon row, an ontology still warming), or
                // the hit came via a Lucene field we don't probe. The row's own label is on hand
                // and needs no ontology, so a label-level verdict is still available and is often
                // decisive: an NCBITaxon row whose label EQUALS the query was reporting null,
                // which a client filtering on equality tiers reads as "weak" and discards.
                match = computeLabelAttribution( vo.getValue(), joinedQuery );
            }
            String matchedVia = match != null ? match.via.token : null;
            String matchedText = match != null ? match.text : null;
            Map<String, Integer> priorCategories = uri != null ? priorCategoriesByUri.get( uri ) : null;
            vos.add( new AnnotationSearchResultValueObject( vo.getValue(), vo.getValueUri(), vo.getCategory(),
                    vo.getCategoryUri(), count, definition, parents, matchedVia, matchedText, null, priorCategories,
                    null, null, null, null ) );
        }
        // Always merge gene hits in, regardless of category — the typeahead surface should
        // surface STAT5B whether the curator is in a Genotype factor, a Treatment factor, or a
        // generic characteristic picker.
        //
        // Dedup by URI: when the same NCBI Gene URI already appears in the ontology/characteristic
        // results (e.g. a curator previously tagged an experiment with the gene under category
        // "genotype" → it surfaces from the corpus with a label like "Il10 [mouse] interleukin 10"
        // and a usage count), drop the synthetic gene-fanout row for that URI — the corpus row
        // carries a richer label (with taxon tag) and the load-bearing usage signal. The synthetic
        // gene row only adds value when the gene is NOT yet known to the corpus ("new" gene,
        // useful for first-time genotype annotation).
        //
        // Cost: one extra GeneService probe per query (~ms). Cache hits cover repeat calls within
        // the 5-min window.
        // The gene fan-out fires three sequential GeneService probes (symbol / name / alias) per
        // query token on EVERY call — the dominant slice of the per-request floor. Callers that
        // know they are resolving a non-gene label (e.g. a curation resolver working an
        // organism-part or disease position) pass includeGenes=false to skip it entirely.
        if ( includeGenes ) {
        LinkedHashSet<AnnotationSearchResultValueObject> geneRows = new LinkedHashSet<>();
        for ( String q : queryValues ) {
            if ( q == null ) continue;
            String trimmed = q.trim();
            if ( trimmed.isEmpty() ) continue;
            geneRows.addAll( resolveGeneHits( trimmed, taxon, limit ) );
        }
        if ( !geneRows.isEmpty() ) {
            Set<String> vosUris = new HashSet<>();
            for ( AnnotationSearchResultValueObject v : vos ) {
                if ( v.getValueUri() != null ) {
                    vosUris.add( v.getValueUri() );
                }
            }
            // Partition gene fan-out hits by probe strength:
            //   strong = symbol or name match — curator typed the gene's primary identifier,
            //   weak   = alias-only — query collided with a synonym (e.g. "age" → Renbp via
            //            the historical alias "AGE"); should never outrank an exact ontology
            //            preferred_label hit.
            // Strong gene rows still prepend so a brand-new gene with no corpus row yet
            // leads the response. Weak (alias-only) gene rows are appended BELOW vos so
            // tier-0 ontology hits ("age" → PATO:0000011, EFO:0000246) come first.
            LinkedHashSet<AnnotationSearchResultValueObject> strong = new LinkedHashSet<>();
            LinkedHashSet<AnnotationSearchResultValueObject> weak = new LinkedHashSet<>();
            for ( AnnotationSearchResultValueObject g : geneRows ) {
                if ( g.getValueUri() != null && vosUris.contains( g.getValueUri() ) ) continue;
                if ( GENE_MATCH_ALIAS.equals( g.getMatchedVia() ) ) {
                    weak.add( g );
                } else {
                    strong.add( g );
                }
            }
            LinkedHashSet<AnnotationSearchResultValueObject> merged = new LinkedHashSet<>();
            merged.addAll( strong );
            merged.addAll( vos );
            merged.addAll( weak );
            if ( merged.size() > limit ) {
                LinkedHashSet<AnnotationSearchResultValueObject> trimmedSet = new LinkedHashSet<>();
                int n = 0;
                for ( AnnotationSearchResultValueObject e : merged ) {
                    if ( n++ >= limit ) break;
                    trimmedSet.add( e );
                }
                return new SearchOutcome( new ArrayList<>( trimmedSet ), negativeEvidence );
            }
            return new SearchOutcome( new ArrayList<>( merged ), negativeEvidence );
        }
        }
        return new SearchOutcome( new ArrayList<>( vos ), negativeEvidence );
    }

    /**
     * Resolve gene matches for the query by fanning out across three exact-match probes on
     * {@link ubic.gemma.persistence.service.genome.gene.GeneService}, rendering each match
     * as a synthetic {@link AnnotationSearchResultValueObject} with {@code category="gene"}
     * and {@code valueUri}=NCBI Gene URI.
     *
     * <p>Probes (in order, dedup-by-gene-id across):</p>
     * <ol>
     *   <li>{@link ubic.gemma.persistence.service.genome.gene.GeneService#findByOfficialSymbol(String)}
     *       (or the taxon-scoped overload when {@code taxon != null}) — strongest. Curator
     *       types {@code STAT5B} and gets the gene.</li>
     *   <li>{@link ubic.gemma.persistence.service.genome.gene.GeneService#findByOfficialName(String)}
     *       — Gemma 1.0 parity. Curator types {@code haptoglobin} and gets HP / Hp back.</li>
     *   <li>{@link ubic.gemma.persistence.service.genome.gene.GeneService#findByAlias(String)}
     *       — alias matches (e.g. {@code TRP53} → {@code Trp53}).</li>
     * </ol>
     *
     * <p>When {@code taxon} is non-null, every probe's results are filtered to that taxon
     * post-hoc (only {@code findByOfficialSymbol} has a taxon-scoped DAO variant; the other
     * two filter the returned collection by {@code g.getTaxon().equals(taxon)}). This is
     * the right knob for "curator is tagging a mouse experiment": typing {@code Il10} no
     * longer surfaces rat or human Il10.</p>
     *
     * <p>Returns empty on any search failure; gene resolution is best-effort and must
     * never break the wider annotation-search response.</p>
     */
    private LinkedHashSet<AnnotationSearchResultValueObject> resolveGeneHits( String query,
            @Nullable Taxon taxon, int limit ) {
        LinkedHashSet<AnnotationSearchResultValueObject> out = new LinkedHashSet<>();
        if ( geneService == null ) {
            return out;
        }
        // Dedup at the gene-id level so a gene matching multiple probes (e.g. symbol AND
        // alias) emits one VO. Insertion order = probe order, so symbol matches stay above
        // name matches stay above alias matches.
        LinkedHashSet<Long> seenGeneIds = new LinkedHashSet<>();
        try {
            Collection<Gene> symbolHits;
            if ( taxon != null ) {
                Gene single = geneService.findByOfficialSymbol( query, taxon );
                symbolHits = single != null ? Collections.singletonList( single ) : Collections.emptyList();
            } else {
                symbolHits = geneService.findByOfficialSymbol( query );
            }
            collectGeneHits( symbolHits, taxon, seenGeneIds, out, limit, GENE_MATCH_SYMBOL );
            if ( out.size() < limit ) {
                collectGeneHits( geneService.findByOfficialName( query ), taxon, seenGeneIds, out, limit, GENE_MATCH_NAME );
            }
            if ( out.size() < limit ) {
                collectGeneHits( geneService.findByAlias( query ), taxon, seenGeneIds, out, limit, GENE_MATCH_ALIAS );
            }
        } catch ( Exception e ) {
            log.debug( "Gene resolution skipped for query '{}': {}", query, e.toString() );
        }
        return out;
    }

    /**
     * Helper: render each {@link Gene} in {@code source} as a search-result VO and add to
     * {@code out}, deduplicating by gene id and respecting the overall {@code limit}.
     * Skips genes with missing id or symbol — those can't be resolved back to a usable URI.
     * When {@code taxonFilter} is non-null, drops genes whose taxon doesn't match.
     *
     * <p>{@code matchedViaToken} is stamped on each emitted VO so the caller can distinguish
     * how the gene was found ({@link #GENE_MATCH_SYMBOL}, {@link #GENE_MATCH_NAME},
     * {@link #GENE_MATCH_ALIAS}). The merge step uses this to keep alias-only hits BELOW
     * exact ontology preferred-label matches.</p>
     */
    private static void collectGeneHits( @Nullable Collection<Gene> source, @Nullable Taxon taxonFilter,
            Set<Long> seenGeneIds,
            LinkedHashSet<AnnotationSearchResultValueObject> out, int limit,
            String matchedViaToken ) {
        if ( source == null || source.isEmpty() ) return;
        for ( Gene g : source ) {
            if ( out.size() >= limit ) return;
            if ( g == null ) continue;
            if ( taxonFilter != null && ( g.getTaxon() == null || !taxonFilter.equals( g.getTaxon() ) ) ) {
                continue;
            }
            Long id = g.getId();
            if ( id != null && !seenGeneIds.add( id ) ) continue;
            String symbol = g.getOfficialSymbol();
            if ( symbol == null || symbol.isEmpty() ) continue;
            String name = g.getOfficialName();
            // Label includes the official name when known so a curator typing "haptoglobin"
            // and seeing "HP" back doesn't have to guess; matches Gemma 1.0's "Hp haptoglobin"
            // ergonomics without including [taxon] (would require a taxon fetch).
            String label = ( name != null && !name.isEmpty() && !name.equalsIgnoreCase( symbol ) )
                    ? symbol + " " + name
                    : symbol;
            String uri = g.getNcbiGeneId() != null
                    ? "http://purl.org/commons/record/ncbi_gene/" + g.getNcbiGeneId()
                    : null;
            // Taxon is EAGER on Gene, so no extra fetch — surface it so curators can tell the species
            // of a returned gene without resolving the NCBI URI externally.
            Taxon taxon = g.getTaxon();
            Long taxonId = taxon != null ? taxon.getId() : null;
            String taxonCommonName = taxon != null ? taxon.getCommonName() : null;
            String taxonScientificName = taxon != null ? taxon.getScientificName() : null;
            out.add( new AnnotationSearchResultValueObject( label, uri, "gene", null,
                    0, null, null, matchedViaToken, label, null, null,
                    taxonId, taxonCommonName, taxonScientificName, null ) );
        }
    }

    /** matchedVia token for genes matched via the official-symbol probe — strongest probe. */
    static final String GENE_MATCH_SYMBOL = "search:gene_symbol";
    /** matchedVia token for genes matched via the official-name probe — strong (Gemma 1.0 parity). */
    static final String GENE_MATCH_NAME = "search:gene_name";
    /**
     * matchedVia token for genes matched ONLY via the alias probe. Weaker than symbol / name —
     * the query collided with a synonym, not the gene's primary identifier (e.g.
     * {@code "age"} hitting {@code Renbp} via the historical {@code "AGE"} alias). These rows
     * are appended BELOW the tier-sorted ontology hits so an exact ontology
     * {@code preferred_label} match always outranks a gene-alias collision.
     */
    static final String GENE_MATCH_ALIAS = "search:gene_alias";

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
                        r.getMatchedVia(), r.getMatchedText(), c, r.getPriorCategories(),
                        r.getTaxonId(), r.getTaxonCommonName(), r.getTaxonScientificName(), r.getExampleUsage() ) );
            }
        }
        return out;
    }

    /**
     * Attach one representative, ACL-visible usage example to each hit whose {@code valueUri} has an
     * accessible prior usage. ONE batched EE2C lookup for the whole result page (bounded by {@code limit});
     * synthetic gene rows and terms with no accessible usage pass through with a null {@code exampleUsage}.
     * Rebuilds the affected entries because the VO is {@code @Value}-immutable. Best-effort: a lookup failure
     * leaves the hits un-enriched rather than failing the search.
     */
    private void attachExampleUsage( List<AnnotationSearchResultValueObject> results ) {
        if ( characteristicService == null ) {
            return;
        }
        Set<String> uris = new LinkedHashSet<>();
        for ( AnnotationSearchResultValueObject r : results ) {
            // Skip synthetic gene rows (category "gene") — they aren't corpus annotations.
            if ( r.getValueUri() != null && !"gene".equals( r.getCategory() ) ) {
                uris.add( r.getValueUri() );
            }
        }
        if ( uris.isEmpty() ) {
            return;
        }
        Map<String, CharacteristicDao.UsageExample> byUri;
        try {
            byUri = characteristicService.findRepresentativeUsageByValueUris( uris );
        } catch ( RuntimeException e ) {
            log.warn( "example-usage enrichment failed; returning hits without it", e );
            return;
        }
        for ( int i = 0; i < results.size(); i++ ) {
            AnnotationSearchResultValueObject r = results.get( i );
            CharacteristicDao.UsageExample ex = r.getValueUri() != null ? byUri.get( r.getValueUri() ) : null;
            if ( ex == null ) {
                continue;
            }
            results.set( i, new AnnotationSearchResultValueObject(
                    r.getValue(), r.getValueUri(), r.getCategory(), r.getCategoryUri(),
                    r.getUsageCount(), r.getDefinition(), r.getParents(),
                    r.getMatchedVia(), r.getMatchedText(), r.getGeneCount(), r.getPriorCategories(),
                    r.getTaxonId(), r.getTaxonCommonName(), r.getTaxonScientificName(),
                    toExampleUsageVo( ex ) ) );
        }
    }

    private static ExampleUsageValueObject toExampleUsageVo( CharacteristicDao.UsageExample ex ) {
        return new ExampleUsageValueObject(
                levelLabel( ex.level ),
                ex.value,        // parentName — the owning value (e.g. "wild type")
                ex.category,     // parentOfParentName — the owning category / factor (e.g. "genotype")
                ex.categoryUri,
                ex.predicate, ex.predicateUri, ex.object, ex.objectUri,
                ex.secondPredicate, ex.secondPredicateUri, ex.secondObject, ex.secondObjectUri,
                ex.sourceExperimentId > 0 ? ex.sourceExperimentId : null );
    }

    /**
     * Map the EE2C {@code LEVEL} class to the wire label the picker renders. EE2C only ever stores three
     * owning levels — {@link ExpressionExperiment} (experiment tag), {@link ExperimentalDesign}, and
     * {@link BioMaterial} — because it rolls factor-value annotations up under the experimental design (see
     * {@code getExperimentalDesignAnnotations}, which returns the factor-value annotations). So an
     * {@code ExperimentalDesign} level IS a factor-value usage; report it as {@code FactorValue} to match the
     * documented {@code ExperimentTag | FactorValue | BioMaterial} enum.
     */
    @Nullable
    private static String levelLabel( @Nullable Class<?> level ) {
        if ( level == null ) {
            return null;
        }
        if ( ExpressionExperiment.class.isAssignableFrom( level ) ) {
            return "ExperimentTag";
        }
        if ( ExperimentalDesign.class.isAssignableFrom( level ) ) {
            return "FactorValue";
        }
        return level.getSimpleName(); // BioMaterial
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
     * Cap on how many candidates get match attribution resolved at candidate stage (i.e. BEFORE
     * ranking + truncation). Attribution costs one {@code getTerm} plus a handful of in-memory
     * annotation reads per URI, so this is far cheaper than the enrichment fan-out — but it is
     * still per-URI work over a set that can reach 1000.
     *
     * <p>200 is chosen to comfortably cover the regime the feature targets. Designation and
     * abbreviation queries — the only ones that reach here — match few terms: the whole point of a
     * coined identifier is that it is rare. A query broad enough to blow this cap is not a
     * designation query and would not have been suppressed anyway.</p>
     */
    static final int CANDIDATE_ATTRIBUTION_CAP = 200;

    /**
     * Cap on the ruled-out terms reported back. Bounds the payload on a query that retrieves a
     * large near-match neighbourhood; when it bites, {@code ruledOutTruncated} says so, so the
     * caller knows it holds a sample rather than the complete exclusion set.
     */
    static final int RULED_OUT_MAX = 25;

    /**
     * Resolve match attribution for a bounded slice of the candidate set, in parallel.
     *
     * <p>The post-truncation {@link #enrichTopHits} pass computes the same attribution for the
     * top-N, but that is too late for the two jobs that need it here: deciding which candidates
     * are near-matches (they must go before they can be truncated around) and deciding which
     * candidates a category preference may promote (a synonym-exact hit can sit hundreds of rows
     * down the raw order — {@code FTC} finds emtricitabine only through a synonym, so it lands in
     * the weakest relevance tier despite being the right answer).</p>
     *
     * <p>Failures degrade to "no attribution" for that URI rather than propagating: an unresolvable
     * URI means the owning ontology is not loaded, which is an infrastructure state, and callers
     * handle the absent entry explicitly.</p>
     *
     * @param hits candidates in their current order; only the first {@code cap} are attributed
     * @return URI → attribution, omitting URIs that resolved to no term or matched no tier
     */
    private Map<String, MatchAttribution> attributeCandidates( List<CharacteristicValueObject> hits,
            String originalQuery, int cap, long budgetMs ) {
        List<String> uris = hits.stream()
                .map( CharacteristicValueObject::getValueUri )
                .filter( Objects::nonNull )
                .distinct()
                .limit( cap )
                .collect( Collectors.toList() );
        if ( uris.isEmpty() || budgetMs <= 0 ) {
            return Collections.emptyMap();
        }
        Map<String, MatchAttribution> out = new java.util.concurrent.ConcurrentHashMap<>();
        StopWatch local = StopWatch.createStarted();
        int parallelism = Math.min( uris.size(), 8 );
        if ( parallelism <= 1 ) {
            attributeOne( uris.get( 0 ), originalQuery, out, budgetMs );
            return out;
        }
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool( parallelism );
        try {
            List<java.util.concurrent.Future<?>> tasks = new ArrayList<>( uris.size() );
            for ( String uri : uris ) {
                tasks.add( pool.submit( () -> {
                    long remaining = Math.max( budgetMs - local.getTime(), 0 );
                    if ( remaining > 0 ) {
                        attributeOne( uri, originalQuery, out, remaining );
                    }
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
                } catch ( InterruptedException ie ) {
                    Thread.currentThread().interrupt();
                    break;
                } catch ( java.util.concurrent.ExecutionException ee ) {
                    log.debug( "candidate attribution task failed", ee.getCause() );
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return out;
    }

    private void attributeOne( String uri, String originalQuery, Map<String, MatchAttribution> out, long remaining ) {
        try {
            OntologyTerm term = ontologyService.getTerm( uri, remaining, TimeUnit.MILLISECONDS );
            if ( term == null ) {
                return;
            }
            MatchAttribution attribution = computeMatchAttribution( term, originalQuery );
            if ( attribution != null ) {
                out.put( uri, attribution );
            }
        } catch ( TimeoutException e ) {
            log.debug( "candidate attribution timed out for {}", uri );
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
            if ( term == null ) {
                // The Lucene/DB hit had this URI, but no loaded ontology service can resolve it
                // back to a term — most often that means the owning ontology is still mid-init
                // (just refreshed) or never loaded at all. matchedVia will land as null on the
                // wire; clients can't distinguish "we never tried" from "we tried and gave up"
                // without this log line. INFO so a sustained failure pattern is visible without
                // turning on debug logging.
                log.info( "annotation-search: getTerm({}) returned null for query='{}'; matchedVia will be null. "
                        + "Likely cause: owning ontology not loaded or still initializing.",
                        uri, originalQuery );
                return;
            }
            MatchAttribution attribution = computeMatchAttribution( term, originalQuery );
            if ( attribution != null ) {
                synchronized ( matchByUri ) { matchByUri.put( uri, attribution ); }
            } else if ( log.isDebugEnabled() ) {
                // No label / synonym normalised-equal to the query — hit came in via a Lucene
                // field we don't probe (definition, obo_id) or a fuzzy / token-overlap rank.
                // The term itself loaded fine; log label so a debug session can see why strict
                // equality didn't bite.
                log.debug( "annotation-search: no match attribution for uri={} label='{}' query='{}'",
                        uri, term.getLabel(), originalQuery );
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

    /** OBO database cross-reference predicate — pointers into MESH / OMIM / UMLS / ICD / SNOMED / etc. */
    private static final String OBO_DB_XREF = "http://www.geneontology.org/formats/oboInOwl#hasDbXref";

    /**
     * Whether {@code query} has the shape of a designation — a coined identifier for one specific
     * entity, rather than a description of one. Operationally: a single token carrying at least one
     * letter AND at least one digit.
     *
     * <p>That shape covers the three families where a near-match is never the same thing: compound
     * codes ({@code MK-2206}, {@code GSK2879552}), cell-line designations ({@code NCI-H358},
     * {@code FTC-133}) and mouse strains ({@code C57BL/6J}). The digits carry the identity —
     * {@code MK-8353} shares every letter with {@code MK-2206} and is a different molecule — so
     * lexical neighbourhood says nothing at all about referential identity.</p>
     *
     * <p>Descriptive queries ({@code diamide}, {@code high fat diet}, {@code cortex}) deliberately
     * fail this test. There a near-match frequently IS the intended term, and suppressing it would
     * return nothing and push the caller onto a fuzzy fallback — which is where fabricated
     * groundings come from in the first place. Suppression has to be narrower than "be strict",
     * or it manufactures the very failure it exists to prevent.</p>
     */
    static boolean isDesignationQuery( @Nullable String query ) {
        if ( query == null ) {
            return false;
        }
        String q = query.trim();
        if ( q.length() < 2 ) {
            return false;
        }
        boolean hasLetter = false, hasDigit = false;
        for ( int i = 0; i < q.length(); i++ ) {
            char c = q.charAt( i );
            if ( Character.isWhitespace( c ) ) {
                return false;   // multi-token ⇒ descriptive, not a designation
            }
            if ( Character.isLetter( c ) ) {
                hasLetter = true;
            } else if ( Character.isDigit( c ) ) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    /**
     * Whether an attribution represents string EQUALITY against one of the term's own names — its
     * preferred label or a declared synonym — as opposed to a prefix / token-overlap neighbourhood
     * match.
     *
     * <p>This is the line between "this row names the thing you asked for" and "this row is
     * lexically nearby". Only the former survives {@code suppress_near_matches}, and only the
     * former is eligible for category promotion. Every synonym scope counts: a narrow or related
     * synonym that equals the query still names the entity — scope is about ontological breadth,
     * not about how confident the match is.</p>
     */
    static boolean isExactAttribution( @Nullable MatchedVia via ) {
        if ( via == null ) {
            return false;
        }
        switch ( via ) {
            case PREFERRED_LABEL:
            case EXACT_SYNONYM:
            case NARROW_SYNONYM:
            case RELATED_SYNONYM:
            case BROAD_SYNONYM:
            case ALT_LABEL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Collect the class-level database cross-references ({@code oboInOwl#hasDbXref}) declared on an
     * already-resolved term. Reuses the predicate-targeted {@code getAnnotations(uri)} lookup the synonym
     * sweep uses, so it is a cheap in-memory read (no DB hit). De-duplicates and drops blanks.
     */
    private static List<String> collectDbXrefs( OntologyTerm term ) {
        Collection<AnnotationProperty> annots = term.getAnnotations( OBO_DB_XREF );
        if ( annots == null ) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for ( AnnotationProperty ap : annots ) {
            String text = ap.getContents();
            if ( StringUtils.isBlank( text ) ) {
                continue;
            }
            if ( seen.add( text ) ) {
                out.add( text );
            }
        }
        return out;
    }

    /**
     * Collect every synonym declared on an already-resolved term, tagged with its scope. Walks the same
     * OBO/IAO predicate set {@link #computeMatchAttribution} probes; each {@code getAnnotations(uri)} call
     * is a predicate-targeted lookup on the in-memory term, so the whole sweep is cheap (no DB hit). The
     * unscoped generic {@code hasSynonym} collapses to {@code related_synonym}. De-duplicates on
     * (type, text) so a string asserted under both a scoped and the generic predicate appears once.
     */
    private static List<OntologyTermSynonymValueObject> collectSynonyms( OntologyTerm term ) {
        String[][] probes = {
                { OBO_EXACT_SYNONYM, MatchedVia.EXACT_SYNONYM.token },
                { OBO_NARROW_SYNONYM, MatchedVia.NARROW_SYNONYM.token },
                { OBO_BROAD_SYNONYM, MatchedVia.BROAD_SYNONYM.token },
                { OBO_RELATED_SYNONYM, MatchedVia.RELATED_SYNONYM.token },
                { OBO_GENERIC_SYNONYM, MatchedVia.RELATED_SYNONYM.token },
                { IAO_ALT_LABEL, MatchedVia.ALT_LABEL.token },
        };
        List<OntologyTermSynonymValueObject> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for ( String[] probe : probes ) {
            Collection<AnnotationProperty> annots = term.getAnnotations( probe[0] );
            if ( annots == null ) {
                continue;
            }
            for ( AnnotationProperty ap : annots ) {
                String text = ap.getContents();
                if ( StringUtils.isBlank( text ) ) {
                    continue;
                }
                if ( seen.add( probe[1] + ' ' + text ) ) {
                    out.add( new OntologyTermSynonymValueObject( text, probe[1] ) );
                }
            }
        }
        return out;
    }

    /**
     * Back-compute which Lucene field produced the hit by walking attribution tiers from
     * strongest to weakest:
     *
     * <ol>
     *   <li>Strict equality against the preferred label →
     *       {@link MatchedVia#PREFERRED_LABEL}.</li>
     *   <li>Strict equality against a synonym (exact &gt; narrow &gt; related &gt; broad &gt;
     *       generic &gt; alt_label).</li>
     *   <li>{@link MatchedVia#LABEL_PREFIX} when the query is a prefix of the label or vice
     *       versa (typeahead-friendly).</li>
     *   <li>{@link MatchedVia#LABEL_TOKENS} when every content token in the query
     *       (length-≥-2, stop-word-stripped) appears as a substring of the label.</li>
     *   <li>{@link MatchedVia#SYNONYM_TOKENS} when label-token coverage fails but the same
     *       check passes against any synonym.</li>
     * </ol>
     *
     * <p>Reasoning: a hit deserves a <em>reason</em> for being a hit — a popular term that
     * shares one stop-word with the query isn't a reason. The
     * {@link #filterByTokenCoverage(java.util.List, String) token-coverage filter}
     * applied upstream already rejected the worst false positives, so every retained hit
     * has at least token coverage on its label or one of its synonyms; this method picks
     * which level of attribution to surface.</p>
     *
     * <p>Returns {@code null} only when no tier matches — typically because the hit came
     * via a Lucene field we don't probe (definition, obo_id), or in the rare case where
     * the upstream filter was bypassed (single-content-token queries).</p>
     */
    /**
     * Attribution derived from a hit's own label alone, with no ontology lookup.
     *
     * <p>Used when {@link #computeMatchAttribution} could not run or returned nothing — the URI
     * belongs to a source with no loaded Jena model (flat lexical catalogues, NCBITaxon rows), or
     * the owning ontology is still initializing. Those rows previously reported {@code matchedVia:
     * null}, which a client filtering on equality tiers reads as "weak" and drops, even when the
     * label was exactly the query.
     *
     * <p>Only label-level tiers are reachable here: without the term we cannot see its synonyms, so
     * this NEVER claims a synonym tier. A synonym-exact hit whose ontology is unloaded stays
     * unattributed rather than being mislabelled as a weaker label match.
     */
    @Nullable
    static MatchAttribution computeLabelAttribution( @Nullable String label, String originalQuery ) {
        if ( label == null || StringUtils.isBlank( originalQuery ) ) {
            return null;
        }
        String normalisedQuery = normaliseForEquality( originalQuery );
        String normalisedLabel = normaliseForEquality( label );
        if ( normalisedQuery.isEmpty() || normalisedLabel.isEmpty() ) {
            return null;
        }
        if ( normalisedLabel.equals( normalisedQuery )
                || canonicaliseForExactMatch( label ).equals( canonicaliseForExactMatch( originalQuery.trim() ) ) ) {
            return new MatchAttribution( MatchedVia.PREFERRED_LABEL, label );
        }
        if ( normalisedLabel.startsWith( normalisedQuery ) || normalisedQuery.startsWith( normalisedLabel ) ) {
            return new MatchAttribution( MatchedVia.LABEL_PREFIX, label );
        }
        List<String> contentTokens = contentTokens( originalQuery );
        if ( !contentTokens.isEmpty() && labelCoversAllTokens( normalisedLabel, contentTokens ) ) {
            return new MatchAttribution( MatchedVia.LABEL_TOKENS, label );
        }
        return null;
    }

    @Nullable
    static MatchAttribution computeMatchAttribution( OntologyTerm term, String originalQuery ) {
        String normalisedQuery = normaliseForEquality( originalQuery );
        if ( normalisedQuery.isEmpty() ) {
            return null;
        }
        // Canonical form additionally drops hyphens and a trailing " cell" / " cell line", so
        // MK2206 ↔ MK-2206 and A549 ↔ "A549 cell" compare equal. This is the SAME equivalence the
        // candidate-stage relevance tiers already use (see canonicaliseForExactMatch); without it
        // here, attribution disagrees with the tier sort — a hit the tier sort called an exact
        // label match reports as label_prefix, and near-match suppression then discards the one
        // row the caller wanted. Designations are exactly where punctuation varies most.
        String canonicalQuery = canonicaliseForExactMatch( originalQuery.trim() );
        String label = term.getLabel();
        String normalisedLabel = label != null ? normaliseForEquality( label ) : "";
        // Tier 1: exact equality on preferred label.
        if ( !normalisedLabel.isEmpty() && normalisedLabel.equals( normalisedQuery ) ) {
            return new MatchAttribution( MatchedVia.PREFERRED_LABEL, label );
        }
        if ( label != null && !canonicalQuery.isEmpty()
                && canonicaliseForExactMatch( label ).equals( canonicalQuery ) ) {
            return new MatchAttribution( MatchedVia.PREFERRED_LABEL, label );
        }
        // Tier 2: exact equality on a synonym. Walk in strength order.
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
                if ( text == null ) {
                    continue;
                }
                if ( normaliseForEquality( text ).equals( normalisedQuery )
                        || ( !canonicalQuery.isEmpty()
                        && canonicaliseForExactMatch( text ).equals( canonicalQuery ) ) ) {
                    return new MatchAttribution( MatchedVia.fromToken( probe[1] ), text );
                }
            }
        }
        // Tier 3: typeahead prefix match on the preferred label. Either direction counts —
        // the user typed "alz" and got "alzheimer's disease", or typed "alzheimer's diseas"
        // and the label is the prefix "alzheimer's disease". Both feel like the same kind
        // of relevance signal to the curator.
        if ( !normalisedLabel.isEmpty() &&
                ( normalisedLabel.startsWith( normalisedQuery ) || normalisedQuery.startsWith( normalisedLabel ) ) ) {
            return new MatchAttribution( MatchedVia.LABEL_PREFIX, label );
        }
        // Tier 4: every content token in the query appears as a substring of the preferred
        // label. The upstream token-coverage filter already enforced this for multi-content-
        // token queries, so we just need to re-confirm here and surface the attribution.
        List<String> contentTokens = contentTokens( originalQuery );
        if ( !contentTokens.isEmpty() && labelCoversAllTokens( normalisedLabel, contentTokens ) ) {
            return new MatchAttribution( MatchedVia.LABEL_TOKENS, label );
        }
        // Tier 5: synonym-token coverage. Check every synonym property; first one that covers
        // all content tokens wins. Surfaces the matching synonym text in matchedText so the
        // client can show "matched on: <synonym>".
        for ( String[] probe : probes ) {
            Collection<AnnotationProperty> annots = term.getAnnotations( probe[0] );
            if ( annots == null || annots.isEmpty() ) {
                continue;
            }
            for ( AnnotationProperty ap : annots ) {
                String text = ap.getContents();
                if ( text == null ) continue;
                String normalisedSyn = normaliseForEquality( text );
                if ( !contentTokens.isEmpty() && labelCoversAllTokens( normalisedSyn, contentTokens ) ) {
                    return new MatchAttribution( MatchedVia.SYNONYM_TOKENS, text );
                }
            }
        }
        return null;
    }

    /**
     * Conservative stop-word list. Tokens this short or this generic don't carry meaning
     * for ontology lookup. Lucene's StandardAnalyzer already removes most; this set covers
     * the cases where we tokenise client-side (the token-coverage filter) before the query
     * has been through Lucene's analyzer.
     */
    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if",
            "in", "into", "is", "it", "of", "on", "or", "such", "that", "the",
            "their", "then", "there", "these", "they", "this", "to", "was",
            "will", "with"
    );

    /**
     * Minimum length for a token to be considered "content". Single characters and
     * digits-only short tokens drop out — they're either part numbers ({@code "2"} in
     * {@code "uzh 2 cell"}) that survive Lucene's analyser but don't help filter
     * candidates, or stop-words.
     */
    private static final int MIN_CONTENT_TOKEN_LENGTH = 2;

    /**
     * Tokenise an arbitrary user query into "content" tokens: lowercase, split on
     * runs of non-alphanumeric characters, drop tokens shorter than
     * {@link #MIN_CONTENT_TOKEN_LENGTH}, drop stop-words.
     *
     * <p>Returned in encounter order, deduplicated; empty list when the input is null /
     * blank / all-stop-words. Callers should treat an empty list as "no token-coverage
     * constraint applies — fall back to Lucene's order".</p>
     */
    static List<String> contentTokens( @Nullable String query ) {
        if ( query == null ) return Collections.emptyList();
        String lower = query.toLowerCase( Locale.ROOT );
        String[] parts = lower.split( "[^a-z0-9]+" );
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for ( String p : parts ) {
            if ( p.length() < MIN_CONTENT_TOKEN_LENGTH ) continue;
            if ( SEARCH_STOP_WORDS.contains( p ) ) continue;
            seen.add( p );
        }
        return new ArrayList<>( seen );
    }

    /**
     * @return true iff every supplied content token appears as a substring of the
     *         normalised label. Matches the substring semantic on purpose so {@code "cell"}
     *         covers labels like {@code "cells"} and {@code "cellular"}.
     */
    private static boolean labelCoversAllTokens( String normalisedLabel, List<String> contentTokens ) {
        if ( normalisedLabel == null || normalisedLabel.isEmpty() ) return false;
        for ( String t : contentTokens ) {
            if ( !normalisedLabel.contains( t ) ) return false;
        }
        return true;
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
     * CLO labels cell-line terms with a trailing {@code " cell"} or {@code " cell line"} (e.g.
     * {@code "A549 cell"}, {@code "NCI-H358 cell"}) rather than the bare cell-line identifier.
     * Strip the suffix so a bare-name query like {@code "A549"} reaches CLO at the exact-label
     * ranking tier instead of losing to EFO's bare {@code "a549"} on tier alone.
     *
     * <p>Operates on the already-lowercased label. Returns the input unchanged when the
     * suffix isn't present.</p>
     */
    private static final java.util.regex.Pattern CELL_SUFFIX =
            java.util.regex.Pattern.compile( "\\s+cell(\\s+line)?$" );

    static String stripCellSuffix( String labelLower ) {
        if ( labelLower == null ) return "";
        return CELL_SUFFIX.matcher( labelLower ).replaceFirst( "" );
    }

    /**
     * Canonical form used for tier-0 / tier-1 equality across the search ranker AND the
     * {@code exact_label} filter: lowercase + strip the trailing {@code " cell"} /
     * {@code " cell line"} suffix + strip ASCII hyphens. Brings identifier-shaped labels
     * and queries into the same shape so the inevitable variations don't drop hits:
     *
     * <ul>
     *   <li>Query {@code MEC2} vs CLO label {@code "MEC-2 cell"} →
     *       both canonicalise to {@code "mec2"} → tier-0 match.</li>
     *   <li>Query {@code MEC-2} vs EFO label {@code "mec2"} →
     *       both canonicalise to {@code "mec2"} → tier-0 match.</li>
     *   <li>Query {@code NCI-H358} vs hypothetical label {@code "NCIH358"} →
     *       both canonicalise to {@code "ncih358"} → tier-0 match.</li>
     * </ul>
     *
     * <p>Strictly subsumes raw equality: when neither side needs normalisation the canonical
     * forms equal the raw forms, so existing matches are preserved. Multi-word labels with
     * intra-word punctuation (apostrophes, slashes, etc.) are NOT normalised here — those
     * are handled by the synonym / tokens tiers downstream.</p>
     */
    static String canonicaliseForExactMatch( @Nullable String s ) {
        if ( s == null ) return "";
        return stripCellSuffix( s.toLowerCase( Locale.ROOT ) ).replace( "-", "" );
    }

    /**
     * JSON-friendly enumeration of which Lucene field produced a hit. Serialised as the
     * lowercase-snake string in {@code token} for the {@code matchedVia} response field.
     */
    public enum MatchedVia {
        /** Query normalises to the term's preferred label exactly. Strongest match. */
        PREFERRED_LABEL( "preferred_label" ),
        /** Query normalises to one of the term's exact-OBO synonyms exactly. */
        EXACT_SYNONYM( "exact_synonym" ),
        NARROW_SYNONYM( "narrow_synonym" ),
        RELATED_SYNONYM( "related_synonym" ),
        BROAD_SYNONYM( "broad_synonym" ),
        ALT_LABEL( "alt_label" ),
        /**
         * Query is a prefix of the preferred label (or the label is a prefix of the query, when
         * the query is the longer of the two). Weaker than {@link #PREFERRED_LABEL} but stronger
         * than a bag-of-tokens overlap — survives typeahead "type while you search" usage.
         */
        LABEL_PREFIX( "label_prefix" ),
        /**
         * Every content token in the query (after lowercase + length-≥-2 + stop-word strip)
         * appears as a substring of the preferred label. Catches multi-token queries that don't
         * normalise-equal to anything (e.g. {@code "uzh-2 cell"} against the label
         * {@code "uzh-2 cell line"}) but DO carry full token coverage. Distinguishes
         * "all-tokens-present" from "stop-word matched alone" — the latter no longer survives
         * the token-coverage filter at all.
         */
        LABEL_TOKENS( "label_tokens" ),
        /**
         * Same as {@link #LABEL_TOKENS} but the coverage check passed against a synonym
         * (preferred-label coverage failed). Surfaces synonym-driven multi-token matches when
         * the preferred label happens to lack one of the query tokens.
         */
        SYNONYM_TOKENS( "synonym_tokens" );

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
         * Which Lucene field most likely produced this hit: {@code preferred_label},
         * {@code exact_synonym}, {@code narrow_synonym}, {@code related_synonym},
         * {@code broad_synonym}, {@code alt_label} (all equality tiers), or {@code label_prefix},
         * {@code label_tokens}, {@code synonym_tokens} (neighbourhood tiers). Back-computed by
         * replaying the query against the term's label and indexed synonyms — see
         * {@code computeMatchAttribution}.
         *
         * <p>Populated for the top-25 hits only. {@code null} there means UNATTRIBUTABLE, not
         * "weak": the hit came via a field we do not probe (definition, obo_id) or a pure relevance
         * rank. Label-level tiers are still filled in from the row's own label when the term itself
         * could not be resolved, so a row whose label equals the query never reports null; but a
         * SYNONYM-exact hit whose owning ontology is unloaded does, because without the term its
         * synonyms are invisible. A client cannot therefore implement "equality tiers only" from
         * this field alone — treat {@code null} as unknown and fall back to label equality.
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
        /**
         * Distinct-experiment counts grouped by the category that prior curators applied when
         * tagging this URI on an experiment — e.g.
         * {@code {"cell line": 14, "protein": 1}}. Lets resolvers break ambiguous label hits
         * by curated history: a URI tagged 14× as a cell line and 1× as a protein is almost
         * certainly a cell line, regardless of which ontology label the query happened to
         * match. Populated for the top-N kept hits only; null on synthetic gene-fanout rows
         * and on responses where the lookup was skipped (e.g. /annotations/parents). An empty
         * map means "the URI has been used in curation but never with a non-null category"
         * (rare; carries no signal).
         */
        @Nullable Map<String, Integer> priorCategories;
        /**
         * Taxon (species) of a gene hit — the gene's {@code Taxon.id}. Null on ontology-term hits
         * (taxon is a gene-only attribute). Lets a curator tell which species a returned gene belongs
         * to without resolving the NCBI Gene URI against an external database.
         */
        @Nullable Long taxonId;
        /** Common name of a gene hit's taxon (e.g. "mouse"). Null on ontology-term hits. */
        @Nullable String taxonCommonName;
        /** Scientific name of a gene hit's taxon (e.g. "Mus musculus"), to disambiguate when the common name is ambiguous. Null on ontology-term hits. */
        @Nullable String taxonScientificName;
        /**
         * One representative, ACL-visible example of how this term has actually been used in the corpus —
         * so a curator picking an unfamiliar/rare term can see it in context (e.g. "wild type" under the
         * "genotype" factor of an accessible dataset). Populated only when the caller passes
         * {@code includeExampleUsage=true}; null when the flag is off, on synthetic gene rows, or when the
         * term has no accessible usage. Gate rendering client-side (e.g. only for low {@code usageCount}).
         */
        @Nullable ExampleUsageValueObject exampleUsage;
    }

    /**
     * A single representative usage of a searched term, for showing a hit in context. Field names mirror
     * {@code ANNOTATION_PARENT_CONTEXT}: {@code parentName} is the owning value (e.g. the factor value),
     * {@code parentOfParentName} is the owning category (e.g. the factor), and the statement triple
     * (predicate/object/second*) is carried when the usage is statement-backed. {@code level} is one of
     * {@code ExperimentTag} / {@code FactorValue} / {@code BioMaterial}; {@code sourceExperimentId} is the
     * accessible dataset the example came from.
     */
    @Value
    public static class ExampleUsageValueObject {
        @Nullable String level;
        @Nullable String parentName;
        @Nullable String parentOfParentName;
        @Nullable String parentOfParentUri;
        @Nullable String predicate;
        @Nullable String predicateUri;
        @Nullable String object;
        @Nullable String objectUri;
        @Nullable String secondPredicate;
        @Nullable String secondPredicateUri;
        @Nullable String secondObject;
        @Nullable String secondObjectUri;
        @Nullable Long sourceExperimentId;
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
        /**
         * Typed synonyms drawn from the OBO/IAO synonym predicates (exact / narrow / broad / related /
         * alt_label). Empty when the term declares none. Cheap to populate — a single in-memory walk of
         * the already-resolved term, the same probe set {@code /annotations/search} uses for match
         * attribution.
         */
        List<OntologyTermSynonymValueObject> synonyms;
        /**
         * Alternative IDs for this term (OBO {@code hasAlternativeId}) — merged-in obsolete identifiers.
         * Often empty, since only terms that absorbed a retired ID carry one. Lets a client recognise a
         * term it knows by a retired identifier. Distinct from {@link #dbXrefs}.
         */
        List<String> alternativeIds;
        /**
         * Database cross-references for this term (OBO {@code hasDbXref}) — pointers into other resources
         * such as MESH, OMIM, UMLS, ICD, SNOMED, etc. (e.g. {@code "MESH:D003920"}, {@code "OMIM:222100"}).
         * Empty when the term declares none. This is the OBO "xref" most consumers mean.
         */
        List<String> dbXrefs;
        /**
         * Version (release) of the ontology this term came from — {@code owl:versionInfo} (often a release
         * date), falling back to {@code owl:versionIRI}. Null when the owning ontology declares no version.
         * Surfaced so a client can tell which ontology release a term reflects, a recurring point of
         * confusion when terms are added, merged, or obsoleted between releases.
         */
        @Nullable String ontologyVersion;
    }

    @Value
    public static class OntologyTermSimpleValueObject {
        String uri;
        String label;
    }

    /**
     * A single synonym of an ontology term plus its scope.
     */
    @Value
    public static class OntologyTermSynonymValueObject {
        /** The synonym text. */
        String value;
        /**
         * Synonym scope, mirroring the OBO/IAO predicates and the {@link MatchedVia} tokens:
         * {@code exact_synonym}, {@code narrow_synonym}, {@code broad_synonym}, {@code related_synonym},
         * {@code alt_label}. Generic {@code oboInOwl#hasSynonym} (no declared scope) is reported as
         * {@code related_synonym}.
         */
        String type;
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
    private static String buildSearchCacheKey( List<String> values, String rankName, int limit, List<String> prefixes, boolean upstream, boolean exactLabel, String category, @Nullable Taxon taxon ) {
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
        // Taxon scopes the gene fan-out (mouse Il10 vs rat Il10 vs human Il10); a cached
        // "all-taxa" entry must not satisfy a "taxon=mouse" call. Use taxon id when present;
        // empty string = no constraint.
        sb.append( '' );
        sb.append( taxon != null && taxon.getId() != null ? "t" + taxon.getId() : "" );
        sb.append( '' );
        for ( int i = 0; i < values.size(); i++ ) {
            String v = values.get( i );
            if ( v == null ) continue;
            String stripped = v.trim();
            String termUri = expandTermQueryToUri( stripped );
            if ( termUri != null ) {
                // Full URIs and CURIEs both key on their canonical URI form so a CURIE and its
                // equivalent full URI share a cache entry, and casing is preserved (URIs are
                // case-sensitive on the path portion).
                sb.append( termUri );
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

    /**
     * Resolve a query to its canonical ontology term URI, when the query is either a full term URI
     * (e.g. {@code http://www.ebi.ac.uk/efo/EFO_0600015}) or a recognized CURIE / OBO ID
     * (e.g. {@code EFO:0600015}, {@code GO:0008150}, {@code NCBITaxon:9606}).
     * <p>
     * CURIE detection is deliberately conservative so free text is never misclassified: the query
     * must match the strict {@code {IDSPACE}:{LOCALID}} shape AND its prefix must be a known
     * ontology ID space ({@link OntologyUtils#isKnownIdSpace}). A CURIE-shaped string with an
     * unrecognized prefix (e.g. {@code foo:bar}) returns null and falls through to free-text
     * search. Expansion reuses {@link OntologyUtils#termIdToUri}, which special-cases EFO's
     * {@code www.ebi.ac.uk/efo/} base and the {@code NCBITaxon}/{@code HsapDv} casing while
     * defaulting everything else to the OBO PURL base.
     *
     * @return the canonical URI string, or null if the query is neither a URI nor a known CURIE
     */
    @Nullable
    static String expandTermQueryToUri( String query ) {
        if ( query == null ) return null;
        String stripped = StringUtils.strip( query );
        URI uri = parseTermUriQuery( stripped );
        if ( uri != null ) {
            return stripped;
        }
        // Strict {IDSPACE}:{LOCALID} match with a recognized prefix only; arbitrary "word:word"
        // free text is left for the free-text search path.
        if ( OntologyUtils.isTermId( stripped, true ) ) {
            return OntologyUtils.termIdToUri( stripped );
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
    /**
     * Request body for {@link #searchAnnotationsBatch}. {@code queries} is the list of labels to resolve,
     * each independently; every other field is a shared knob applied to all items (mirroring the GET
     * {@code /annotations/search} query parameters). Nulls fall back to the same defaults as the GET.
     */
    public static class AnnotationSearchBatchRequest {
        @Nullable
        private List<Item> queries;
        @Nullable
        private String rank;
        @Nullable
        private Integer limit;
        @Nullable
        private String prefixes;
        @Nullable
        private Boolean exactLabel;
        @Nullable
        private Boolean suppressNearMatches;
        @Nullable
        private Boolean includeGenes;
        @Nullable
        private Boolean includeGeneCount;
        @Nullable
        private Integer geneCountMaxTerms;
        @Nullable
        private String taxon;

        /** One batch item: a literal label plus its optional per-item category hint. */
        public static class Item {
            @Nullable
            private String query;
            @Nullable
            private String category;

            @Nullable
            public String getQuery() {
                return query;
            }

            public void setQuery( @Nullable String query ) {
                this.query = query;
            }

            @Nullable
            public String getCategory() {
                return category;
            }

            public void setCategory( @Nullable String category ) {
                this.category = category;
            }
        }

        @Nullable
        public List<Item> getQueries() {
            return queries;
        }

        public void setQueries( @Nullable List<Item> queries ) {
            this.queries = queries;
        }

        @Nullable
        public String getRank() {
            return rank;
        }

        public void setRank( @Nullable String rank ) {
            this.rank = rank;
        }

        @Nullable
        public Integer getLimit() {
            return limit;
        }

        public void setLimit( @Nullable Integer limit ) {
            this.limit = limit;
        }

        @Nullable
        public String getPrefixes() {
            return prefixes;
        }

        public void setPrefixes( @Nullable String prefixes ) {
            this.prefixes = prefixes;
        }

        @Nullable
        public Boolean getExactLabel() {
            return exactLabel;
        }

        public void setExactLabel( @Nullable Boolean exactLabel ) {
            this.exactLabel = exactLabel;
        }

        /** @see AnnotationsWebService#searchAnnotations the {@code suppress_near_matches} query parameter */
        @Nullable
        public Boolean getSuppressNearMatches() {
            return suppressNearMatches;
        }

        public void setSuppressNearMatches( @Nullable Boolean suppressNearMatches ) {
            this.suppressNearMatches = suppressNearMatches;
        }

        @Nullable
        public Boolean getIncludeGenes() {
            return includeGenes;
        }

        public void setIncludeGenes( @Nullable Boolean includeGenes ) {
            this.includeGenes = includeGenes;
        }

        @Nullable
        public Boolean getIncludeGeneCount() {
            return includeGeneCount;
        }

        public void setIncludeGeneCount( @Nullable Boolean includeGeneCount ) {
            this.includeGeneCount = includeGeneCount;
        }

        @Nullable
        public Integer getGeneCountMaxTerms() {
            return geneCountMaxTerms;
        }

        public void setGeneCountMaxTerms( @Nullable Integer geneCountMaxTerms ) {
            this.geneCountMaxTerms = geneCountMaxTerms;
        }

        @Nullable
        public String getTaxon() {
            return taxon;
        }

        public void setTaxon( @Nullable String taxon ) {
            this.taxon = taxon;
        }
    }

    /**
     * One entry in a {@link #searchAnnotationsBatch} response — echoes the item's {@code query} and
     * {@code category} so the client can correlate without relying on positional order, carries the
     * ordered {@code results} (same shape as a single {@code /annotations/search}), and sets
     * {@code error} to a non-null message when that single item failed (the rest of the batch is
     * unaffected).
     */
    /**
     * {@code GET /annotations/search} envelope: the standard {@code data} array plus, when
     * identity matching ran, the {@link NegativeEvidenceValueObject} beside it.
     *
     * <p>The ruled-out terms deliberately do NOT go into {@code data}. A client that reads
     * {@code data[0]} without understanding a new field would otherwise pick up a term the server
     * just determined is wrong — turning a safety feature into the exact fabrication it exists to
     * prevent. {@code data} keeps its meaning: rows we stand behind.
     */
    public static class AnnotationSearchResponseDataObject extends ResponseDataObject<List<AnnotationSearchResultValueObject>> {

        @Nullable
        private final NegativeEvidenceValueObject negativeEvidence;

        public AnnotationSearchResponseDataObject( List<AnnotationSearchResultValueObject> payload,
                @Nullable NegativeEvidenceValueObject negativeEvidence ) {
            super( payload );
            this.negativeEvidence = negativeEvidence;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Nullable
        public NegativeEvidenceValueObject getNegativeEvidence() {
            return negativeEvidence;
        }
    }

    /**
     * What identity matching ruled OUT for a designation query, and whether anything survived.
     *
     * <p>Present only when identity matching actually ran — i.e. the caller asked for
     * {@code suppress_near_matches} AND the query was designation-shaped. Its presence is
     * therefore the signal a resolver needs and cannot otherwise get: an empty {@code data} array
     * on its own is indistinguishable from "nothing matched", "the ontology wasn't loaded", or
     * "this call never happened", and only the first of those licenses abstaining.
     *
     * <p>{@code solidMatch: false} says the negative is confident. {@code ruledOut} says which
     * terms the negative is about — because "there is no match" does not stop a later stage from
     * proposing {@code mk-8353} for {@code MK-8722} on its own; "it is not mk-8353, and here is
     * the rest of what it is not" does. Free text is a valid annotation; a wrong CHEBI id is a
     * fabricated fact, and the whole point of this block is to make the first choice available.
     */
    public static class NegativeEvidenceValueObject {
        private final String query;
        private final boolean solidMatch;
        private final List<RuledOutTermValueObject> ruledOut;
        private final boolean ruledOutTruncated;

        public NegativeEvidenceValueObject( String query, boolean solidMatch,
                List<RuledOutTermValueObject> ruledOut, boolean ruledOutTruncated ) {
            this.query = query;
            this.solidMatch = solidMatch;
            this.ruledOut = ruledOut;
            this.ruledOutTruncated = ruledOutTruncated;
        }

        /** The designation that was identity-matched. */
        public String getQuery() {
            return query;
        }

        /**
         * Whether any term matched the query outright (preferred label or declared synonym).
         * {@code false} is a CONFIDENT negative: we searched and nothing names this string.
         */
        public boolean isSolidMatch() {
            return solidMatch;
        }

        /** Terms that came back for the query and are NOT it. Ordered by relevance, best first. */
        public List<RuledOutTermValueObject> getRuledOut() {
            return ruledOut;
        }

        /**
         * Whether {@link #getRuledOut()} was capped. When true the list is a sample, so it may be
         * used as an exclusion list but not as an exhaustive one.
         */
        public boolean isRuledOutTruncated() {
            return ruledOutTruncated;
        }
    }

    /** One term that was retrieved for the query and rejected as not naming it. */
    public static class RuledOutTermValueObject {
        /** Lexically near the query but not naming it — a different entity. */
        public static final String REASON_NEAR_MATCH = "near_match";
        /** Names the query, but sits in a namespace that is impossible for the stated category. */
        public static final String REASON_OUT_OF_CATEGORY = "out_of_category";

        @Nullable
        private final String value;
        @Nullable
        private final String valueUri;
        @Nullable
        private final String matchedVia;
        private final String reason;

        public RuledOutTermValueObject( @Nullable String value, @Nullable String valueUri,
                @Nullable String matchedVia, String reason ) {
            this.value = value;
            this.valueUri = valueUri;
            this.matchedVia = matchedVia;
            this.reason = reason;
        }

        /**
         * Why this term is not the answer: {@code near_match} (lexically close, different entity)
         * or {@code out_of_category} (right name, impossible namespace for the category). The two
         * warrant different downstream handling — an out-of-category hit may indicate the FACTOR is
         * mis-categorised rather than the term being wrong.
         */
        public String getReason() {
            return reason;
        }

        @Nullable
        public String getValue() {
            return value;
        }

        @Nullable
        public String getValueUri() {
            return valueUri;
        }

        /**
         * Why it came back at all — {@code label_prefix}, {@code label_tokens}, … Never one of the
         * equality tiers; those are exactly the rows that were kept. {@code null} when the term
         * could not be attributed (see the {@code matchedVia} notes on the search endpoint).
         */
        @Nullable
        public String getMatchedVia() {
            return matchedVia;
        }
    }

    /**
     * Internal carrier for a single search: the rows the caller gets plus the negative evidence
     * that goes beside them. Kept together so the response cache stores BOTH — caching the rows
     * alone would silently strip the negative evidence from every cache hit, which is the failure
     * mode where a resolver abstains on a cold call and fabricates on a warm one.
     */
    private static final class SearchOutcome {
        final List<AnnotationSearchResultValueObject> results;
        @Nullable
        final NegativeEvidenceValueObject negativeEvidence;

        SearchOutcome( List<AnnotationSearchResultValueObject> results,
                @Nullable NegativeEvidenceValueObject negativeEvidence ) {
            this.results = results;
            this.negativeEvidence = negativeEvidence;
        }
    }

    public static class AnnotationSearchBatchResultValueObject {
        @Nullable
        private final String query;
        @Nullable
        private final String category;
        private final List<AnnotationSearchResultValueObject> results;
        @Nullable
        private final String error;
        @Nullable
        private final NegativeEvidenceValueObject negativeEvidence;

        public AnnotationSearchBatchResultValueObject( @Nullable String query, @Nullable String category,
                List<AnnotationSearchResultValueObject> results, @Nullable String error ) {
            this( query, category, results, error, null );
        }

        public AnnotationSearchBatchResultValueObject( @Nullable String query, @Nullable String category,
                List<AnnotationSearchResultValueObject> results, @Nullable String error,
                @Nullable NegativeEvidenceValueObject negativeEvidence ) {
            this.query = query;
            this.category = category;
            this.results = results;
            this.error = error;
            this.negativeEvidence = negativeEvidence;
        }

        /** @see NegativeEvidenceValueObject — present only when identity matching ran for this item. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Nullable
        public NegativeEvidenceValueObject getNegativeEvidence() {
            return negativeEvidence;
        }

        @Nullable
        public String getQuery() {
            return query;
        }

        @Nullable
        public String getCategory() {
            return category;
        }

        public List<AnnotationSearchResultValueObject> getResults() {
            return results;
        }

        @Nullable
        public String getError() {
            return error;
        }
    }

    public static class AnnotationDto {
        @Nullable
        private String category;
        @Nullable
        private String categoryUri;
        /**
         * Subject text. For a plain {@code Characteristic} this is "the value"; for a
         * {@code Statement} it's the statement subject (the {@code Statement} entity aliases
         * {@code setValue}/{@code getValue} to {@code setSubject}/{@code getSubject}). The
         * wire field name stays {@code value} for backwards compatibility with the
         * Characteristic-only era.
         */
        @Nullable
        private String value;
        @Nullable
        private String valueUri;
        @Nullable
        private String evidenceCode;
        /**
         * Predicate label of the statement (e.g. {@code "has dose"}). When set together with
         * any other Statement field (predicateUri, object, objectUri, secondPredicate*,
         * secondObject*), the row is persisted as a {@link Statement} rather than a plain
         * {@link Characteristic}. Null on plain tags.
         */
        @Nullable
        private String predicate;
        @Nullable
        private String predicateUri;
        /**
         * Object label of the statement (e.g. {@code "10mg"}).
         */
        @Nullable
        private String object;
        @Nullable
        private String objectUri;
        /**
         * Second predicate, for compound statements (e.g. {@code "treatment X has dose 10mg
         * for 12 weeks"} — secondPredicate={@code "for"}, secondObject={@code "12 weeks"}).
         */
        @Nullable
        private String secondPredicate;
        @Nullable
        private String secondPredicateUri;
        @Nullable
        private String secondObject;
        @Nullable
        private String secondObjectUri;

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

        /**
         * @return true when ANY Statement-shaped field is set (predicate, object,
         *         secondPredicate, secondObject, or their URIs). When true,
         *         {@code annotationDtoToCharacteristic} constructs a
         *         {@link Statement}; when false, the conversion produces a plain
         *         {@link Characteristic}.
         */
        boolean hasStatementShape() {
            return StringUtils.isNotBlank( predicate ) || StringUtils.isNotBlank( predicateUri )
                    || StringUtils.isNotBlank( object ) || StringUtils.isNotBlank( objectUri )
                    || StringUtils.isNotBlank( secondPredicate ) || StringUtils.isNotBlank( secondPredicateUri )
                    || StringUtils.isNotBlank( secondObject ) || StringUtils.isNotBlank( secondObjectUri );
        }
    }

    /**
     * Request body for {@link #replaceDatasetAnnotations}: the full desired tag set plus an optional
     * {@code annotationSetId} to attach to emitted audit events (linkage is parked until the
     * source-AnnotationSet → emitted-event audit link lands — see
     * {@code STATUS_PUT_DATASETS_DESIGN.md}).
     */
    public static class AnnotationsReplaceRequest {
        @Nullable
        private List<AnnotationDto> annotations;
        @Nullable
        private Long annotationSetId;

        @Nullable
        public List<AnnotationDto> getAnnotations() {
            return annotations;
        }

        public void setAnnotations( @Nullable List<AnnotationDto> annotations ) {
            this.annotations = annotations;
        }

        @Nullable
        public Long getAnnotationSetId() {
            return annotationSetId;
        }

        public void setAnnotationSetId( @Nullable Long annotationSetId ) {
            this.annotationSetId = annotationSetId;
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
            @Parameter(description = "Optional id of the AnnotationSet this tag is being applied from; "
                    + "linkage is parked until the source-set → emitted-event audit link lands.")
            @QueryParam("annotationSetId") @Nullable Long annotationSetId
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
        // annotationSetId is accepted-and-dropped; see STATUS_PUT_DATASETS_DESIGN.md.
        if ( annotationSetId != null ) {
            log.debug( "addDatasetAnnotation: received annotationSetId={} for ee={} (linkage parked)",
                    annotationSetId, ee.getId() );
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
        // annotationSetId accepted but currently dropped; see STATUS_PUT_DATASETS_DESIGN.md.
        if ( body.getAnnotationSetId() != null ) {
            log.debug( "replaceDatasetAnnotations: received annotationSetId={} (linkage parked)",
                    body.getAnnotationSetId() );
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
     * Map an inbound {@link AnnotationDto} into a transient {@link Characteristic} (or its
     * {@link Statement} subclass when any predicate/object field is set), validating required
     * fields and parsing the evidence code. Throws {@link BadRequestException} on bad input
     * (mapped to HTTP 400 by the Jersey exception mapper).
     *
     * <p>The {@code Statement} path keeps the Characteristic shape's {@code category} +
     * {@code value} ({@code value} is the statement subject — Statement aliases
     * {@code setValue} → {@code setSubject}) and adds {@code predicate}/{@code object} plus
     * the optional second pair. A Statement with no predicate/object fields would be
     * indistinguishable from a Characteristic on the wire and is rejected as a hint to use
     * the Characteristic shape instead.</p>
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
        Characteristic c;
        if ( dto.hasStatementShape() ) {
            // Reject the "second-* set but no first-*" shape — second-pair semantics depend
            // on the first pair being present. Predicate-only or object-only is allowed:
            // common ontology patterns express bare relationships ("has_role X") without a
            // dedicated object literal.
            boolean secondPredicateSet = StringUtils.isNotBlank( dto.getSecondPredicate() )
                    || StringUtils.isNotBlank( dto.getSecondPredicateUri() );
            boolean secondObjectSet = StringUtils.isNotBlank( dto.getSecondObject() )
                    || StringUtils.isNotBlank( dto.getSecondObjectUri() );
            boolean firstPredicateSet = StringUtils.isNotBlank( dto.getPredicate() )
                    || StringUtils.isNotBlank( dto.getPredicateUri() );
            boolean firstObjectSet = StringUtils.isNotBlank( dto.getObject() )
                    || StringUtils.isNotBlank( dto.getObjectUri() );
            if ( ( secondPredicateSet || secondObjectSet ) && !( firstPredicateSet || firstObjectSet ) ) {
                throw new BadRequestException( "secondPredicate/secondObject cannot be supplied without a first predicate/object." );
            }
            Statement s = Statement.Factory.newInstance();
            s.setCategory( dto.getCategory() );
            s.setCategoryUri( dto.getCategoryUri() );
            s.setSubject( dto.getValue() );
            s.setSubjectUri( dto.getValueUri() );
            s.setPredicate( dto.getPredicate() );
            s.setPredicateUri( dto.getPredicateUri() );
            s.setObject( dto.getObject() );
            s.setObjectUri( dto.getObjectUri() );
            s.setSecondPredicate( dto.getSecondPredicate() );
            s.setSecondPredicateUri( dto.getSecondPredicateUri() );
            s.setSecondObject( dto.getSecondObject() );
            s.setSecondObjectUri( dto.getSecondObjectUri() );
            c = s;
        } else {
            c = Characteristic.Factory.newInstance();
            c.setCategory( dto.getCategory() );
            c.setCategoryUri( dto.getCategoryUri() );
            c.setValue( dto.getValue() );
            c.setValueUri( dto.getValueUri() );
        }
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

    /**
     * Equality used for diffing/deduping annotations on an EE. Two rows are the same iff
     * they share {@code (category, value)} AND — for Statement-shaped rows — the
     * {@code (predicate, object, secondPredicate, secondObject)} tuples match.
     *
     * <p>Rationale: "fed with HFD" and "fed for 12 weeks" share (category="treatment",
     * value="HFD") but the predicates make them distinct annotations. The pre-Statement
     * rule collapsed them as duplicates; that's wrong for the Statement era. A
     * Characteristic and a Statement that share (category, value) are also distinct —
     * the Statement carries additional relational meaning the Characteristic doesn't.</p>
     */
    private static boolean sameTag( Characteristic a, Characteristic b ) {
        if ( !CharacteristicUtils.equals( a.getCategory(), a.getCategoryUri(), b.getCategory(), b.getCategoryUri() )
                || !CharacteristicUtils.equals( a.getValue(), a.getValueUri(), b.getValue(), b.getValueUri() ) ) {
            return false;
        }
        boolean aIsStatement = a instanceof Statement;
        boolean bIsStatement = b instanceof Statement;
        if ( aIsStatement != bIsStatement ) {
            return false;
        }
        if ( !aIsStatement ) {
            return true;
        }
        Statement sa = ( Statement ) a;
        Statement sb = ( Statement ) b;
        return CharacteristicUtils.equals( sa.getPredicate(), sa.getPredicateUri(), sb.getPredicate(), sb.getPredicateUri() )
                && CharacteristicUtils.equals( sa.getObject(), sa.getObjectUri(), sb.getObject(), sb.getObjectUri() )
                && CharacteristicUtils.equals( sa.getSecondPredicate(), sa.getSecondPredicateUri(), sb.getSecondPredicate(), sb.getSecondPredicateUri() )
                && CharacteristicUtils.equals( sa.getSecondObject(), sa.getSecondObjectUri(), sb.getSecondObject(), sb.getSecondObjectUri() );
    }
}
