package ubic.gemma.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import ubic.gemma.core.search.*;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUrlBuilder;
import ubic.gemma.persistence.util.UnsupportedEntityUrlException;
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;
import ubic.gemma.rest.util.MalformedArgException;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.*;

import org.springframework.lang.Nullable;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Provides search capabilities to the RESTful API.
 *
 * @author poirigui
 */
@Service
@Path("/search")
@Slf4j
public class SearchWebService {

    /**
     * Name used in the OpenAPI schema to identify result types as per {@link #search}'s
     * fourth argument.
     */
    public static final String RESULT_TYPES_SCHEMA_NAME = "SearchResultType";

    /**
     * Maximum number of search results.
     */
    public static final int MAX_SEARCH_RESULTS = 2000;

    @Autowired
    private SearchService searchService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private DatasetArgService datasetArgService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private TaxonArgService taxonArgService;
    @Autowired
    private PlatformArgService platformArgService;

    @Context
    private UriInfo uriInfo;
    @Autowired
    private EntityUrlBuilder entityUrlBuilder;

    /**
     * Search everything subject to taxon and platform constraints.
     * <p>
     * Naming the schema in for the result types is necessary so that it can be resolved in {@link CustomModelResolver}.
     */
    @GET
    @GZIP
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search everything in Gemma", responses = {
            @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid search query, taxon, platform result type or exclusion specification.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
            @ApiResponse(responseCode = "503", description = "The search timed out.", content = @Content(schema = @Schema(implementation = ResponseErrorObject.class)))
    })
    public SearchResultsResponseDataObject search(
            @QueryParam("query") QueryArg query,
            @QueryParam("dataset") DatasetArg<?> datasetArg,
            @QueryParam("taxon") TaxonArg<?> taxonArg,
            @QueryParam("platform") PlatformArg<?> platformArg,
            @Parameter(array = @ArraySchema(schema = @Schema(name = RESULT_TYPES_SCHEMA_NAME, hidden = true))) @QueryParam("resultTypes") List<String> resultTypes,
            @Parameter(description = "Maximum number of search results to return; capped at " + MAX_SEARCH_RESULTS + " unless `resultObject` is excluded.", schema = @Schema(type = "integer", minimum = "1", maximum = "" + MAX_SEARCH_RESULTS)) @QueryParam("limit") LimitArg limit,
            @Parameter(description = "List of fields to exclude from the payload. Only `resultObject` is supported.") @QueryParam("exclude") ExcludeArg<SearchResult<?>> excludeArg,
            @Parameter(description = "Expand a gene search through Gene Ontology terms (GO term → annotated genes). "
                    + "Off by default: a gene search hits only the gene services (symbol/name/alias). GO→gene is a "
                    + "function search, not a gene search, and scanning GO on every query is slow — opt in explicitly.")
            @QueryParam("useGeneOntology") @DefaultValue("false") boolean useGeneOntology,
            @Parameter(description = "Widen the search into terms Gemma knows are related to the ones matched, "
                    + "so that a search for a disease also returns datasets annotated only with a genotype that "
                    + "stands for it. Off by default: turning it on changes every existing result set and every "
                    + "count derived from one, and no specificity threshold has been tuned against curator "
                    + "judgement yet. Inferred hits score below direct ones and say so in their provenance.")
            @QueryParam("inferRelations") @DefaultValue("false") boolean inferRelations
    ) {
        if ( query == null ) {
            throw new BadRequestException( "A query must be supplied." );
        }
        Map<String, Class<? extends Identifiable>> supportedResultTypesByName = searchService.getSupportedResultTypes().stream()
                .collect( Collectors.toMap( Class::getName, identity() ) );
        Collection<Class<? extends Identifiable>> resultTypesCls;
        if ( resultTypes == null || resultTypes.isEmpty() ) {
            // include everything
            resultTypesCls = supportedResultTypesByName.values();
        } else if ( supportedResultTypesByName.keySet().containsAll( resultTypes ) ) {
            // only include what the user asks for
            resultTypesCls = resultTypes.stream().map( supportedResultTypesByName::get ).collect( Collectors.toSet() );
        } else {
            throw new BadRequestException( String.format( "Unsupported result type(s). Ensure that your results are among: %s.",
                    String.join( ", ", supportedResultTypesByName.keySet() ) ) );
        }

        boolean fillResults;
        int maxResults;
        if ( getExcludedFields( excludeArg ).contains( "resultObject" ) ) {
            fillResults = false;
            maxResults = limit != null ? limit.getValueNoMaximum() : -1;
        } else {
            fillResults = true;
            maxResults = limit != null ? limit.getValue( MAX_SEARCH_RESULTS ) : 100;
        }

        ubic.gemma.model.genome.Taxon taxon = taxonArg != null ? taxonArgService.getEntity( taxonArg ) : null;

        // The taxon backstop below discards results, so the search has to be asked for more than the
        // caller wants or the discards come straight off the response.
        boolean taxonBackstop = taxon != null;
        // The backstop reads each result's taxon off its value object. When the caller excluded
        // resultObject we load them anyway and strip them before responding: a search that accepts a
        // ?taxon and then silently ignores it is worse than a slower one, and this is the path where
        // ignoring it is invisible (the caller gets bare ids and cannot tell the species is wrong).
        // Only pays the cost when a taxon was actually supplied.
        boolean loadObjectsForBackstop = taxonBackstop && !fillResults;
        int searchMaxResults = maxResults;
        if ( taxonBackstop && maxResults > 0 ) {
            searchMaxResults = Math.min( maxResults * TAXON_BACKSTOP_OVERFETCH, MAX_SEARCH_RESULTS );
        }

        SearchSettings searchSettings = SearchSettings.builder()
                .query( query.getValue() )
                .datasetConstraint( datasetArg != null ? datasetArgService.getEntity( datasetArg ) : null )
                .taxonConstraint( taxon )
                .platformConstraint( platformArg != null ? platformArgService.getEntity( platformArg ) : null )
                .resultTypes( resultTypesCls )
                .maxResults( searchMaxResults )
                .fillResults( fillResults )
                // Gene search hits only the gene services by default; GO→gene expansion is opt-in
                // (it is a function search, and scanning GO on every gene query was a ~25s hot spot
                // that pinned a DB connection and starved the pool).
                .useGeneOntology( useGeneOntology )
                .useInferredRelations( inferRelations )
                .build();

        List<SearchResult<?>> searchResults;
        try {
            searchResults = searchService.search( searchSettings, new SearchContext( null, null ) ).toList();
        } catch ( ParseSearchException e ) {
            throw new BadRequestException( "Invalid search query: " + e.getQuery(), e );
        } catch ( SearchTimeoutException e ) {
            throw new ServiceUnavailableException( e.getMessage(), DateUtils.addSeconds( new Date(), 30 ), e.getCause() );
        } catch ( SearchException e ) {
            throw new InternalServerErrorException( e );
        }

        List<SearchResult<? extends IdentifiableValueObject<?>>> searchResultVos;

        // Some result VOs are null for unknown reasons, see https://github.com/PavlidisLab/Gemma/issues/417
        if ( fillResults || loadObjectsForBackstop ) {
            searchResultVos = searchService.loadValueObjects( searchResults );
        } else {
            searchResultVos = searchResults.stream()
                    .map( sr -> sr.withResultObject( ( IdentifiableValueObject<?> ) null ) )
                    .collect( Collectors.toList() );
        }

        // convert the response to search results of VOs
        List<Exception> exceptions = new ArrayList<>();
        try {
            return new SearchResultsResponseDataObject( searchResultVos.stream()
                    .sorted() // SearchResults are sorted by descending score order
                    // Taxon backstop. SearchSettings.taxonConstraint is advisory: it is passed to every
                    // source but the gene sources (HibernateSearchSource, the GO source) do not apply it,
                    // so ?query=Myc&taxon=mouse used to answer with the rat and human orthologs at every
                    // limit — silently, which is worse than an error. Drop what provably does not match,
                    // AFTER scoring and BEFORE the caller's limit. Mirrors GeneWebService.searchGenes.
                    .filter( sr -> !taxonBackstop || matchesTaxon( sr.getResultObject(), taxon.getId() ) )
                    .limit( maxResults > 0 ? maxResults : Long.MAX_VALUE ) // results are limited by class, so there might be more results than expected when unraveling everything
                    // honour ?exclude=resultObject now that the backstop has had what it needed
                    .map( sr -> loadObjectsForBackstop ? sr.withResultObject( ( IdentifiableValueObject<?> ) null ) : sr )
                    .map( sr -> {
                        String resultUrl;
                        boolean resultUrlExternal;
                        try {
                            EntityUrlBuilder.EntityUrl<? extends Identifiable> builder = entityUrlBuilder.fromHostUrl()
                                    .entity( sr.getResultType(), sr.getResultId() )
                                    .rest();
                            resultUrl = builder.toUriString();
                            resultUrlExternal = builder.isExternal();
                        } catch ( UnsupportedEntityUrlException e ) {
                            exceptions.add( e );
                            resultUrl = null;
                            resultUrlExternal = false;
                        }
                        return new SearchResultValueObject<>( sr, resultUrl, resultUrlExternal );
                    } )
                    .collect( Collectors.toList() ), new SearchSettingsValueObject( searchSettings ) );
        } finally {
            if ( !exceptions.isEmpty() ) {
                Iterator<Exception> it = exceptions.iterator();
                Exception e = it.next();
                it.forEachRemaining( e::addSuppressed );
                log.warn( "Failed to generate URLs for " + exceptions.size() + " search results.", e );
            }
        }
    }

    /**
     * How much wider than the caller's {@code limit} to search when the taxon backstop is active.
     * A symbol like {@code Myc} matches an ortholog in every taxon Gemma carries, so the wanted one
     * can sit several rows down; this buys room for those to be discarded without eating into the
     * response.
     */
    private static final int TAXON_BACKSTOP_OVERFETCH = 10;

    /**
     * Whether a search-result VO is compatible with a requested taxon.
     * <p>
     * Only four of the nine supported result types carry a taxon at all. The rest
     * ({@link BibliographicReferenceValueObject}, {@link BioSequenceValueObject},
     * {@link CompositeSequenceValueObject}, {@link ExpressionExperimentSetValueObject},
     * blacklisted entities) have no taxon concept, and a taxon-constrained search must NOT
     * silently drop them — {@code true} is the right answer there, not "unknown, discard".
     * <p>
     * A type that does carry a taxon but has none set is discarded: the caller asked for one
     * taxon, and a result we cannot show to belong to it does not answer that question. Same
     * rule as {@code GeneWebService.searchGenes}.
     * <p>
     * Reads the taxon OBJECT rather than the flattened id accessors: those are {@code @Deprecated}
     * on two of the four, and their names disagree ({@code getTaxonID} on platforms,
     * {@code getTaxonId} elsewhere). Mind that {@code getTaxon()} is not the object on every type —
     * on experiments and platforms it returns the common NAME as a String.
     */
    private static boolean matchesTaxon( @Nullable Object vo, Long wantTaxonId ) {
        TaxonValueObject voTaxon;
        if ( vo instanceof GeneValueObject ) {
            voTaxon = ( ( GeneValueObject ) vo ).getTaxon();
        } else if ( vo instanceof ExpressionExperimentValueObject ) {
            voTaxon = ( ( ExpressionExperimentValueObject ) vo ).getTaxonObject();
        } else if ( vo instanceof ArrayDesignValueObject ) {
            voTaxon = ( ( ArrayDesignValueObject ) vo ).getTaxonObject();
        } else if ( vo instanceof GeneSetValueObject ) {
            voTaxon = ( ( GeneSetValueObject ) vo ).getTaxon();
        } else {
            // no taxon concept for this result type — the constraint does not apply to it
            return true;
        }
        return voTaxon != null && wantTaxonId.equals( voTaxon.getId() );
    }

    private static final Set<String> ALLOWED_FIELDS = Collections.singleton( "resultObject" );

    private static Set<String> getExcludedFields( @Nullable ExcludeArg<SearchResult<?>> arg ) {
        if ( arg == null ) {
            return Collections.emptySet();
        }
        if ( !ALLOWED_FIELDS.containsAll( arg.getValue() ) ) {
            throw new MalformedArgException( String.format( "Only the following fields can be excluded: %s.",
                    String.join( ", ", ALLOWED_FIELDS ) ) );
        }
        return new HashSet<>( arg.getValue() );
    }

    /**
     * Represents search settings for the RESTful API.
     * <p>
     * Note that we will only expose back what the {@link SearchWebService} accepts to take as parameters for searching.
     */
    @Value
    public class SearchSettingsValueObject {

        String query;
        @ArraySchema(schema = @Schema(ref = "SearchResultType"))
        Set<String> resultTypes;

        /* constraints */
        @Nullable
        TaxonValueObject taxon;
        @Nullable
        ArrayDesignValueObject platform;

        /**
         * The maximum number of results, of null if unlimited.
         */
        @Nullable
        Integer maxResults;

        public SearchSettingsValueObject( SearchSettings searchSettings ) {
            this.query = searchSettings.getQuery();
            this.resultTypes = searchSettings.getResultTypes().stream().map( Class::getName ).collect( Collectors.toSet() );
            if ( searchSettings.getTaxonConstraint() != null ) {
                this.taxon = taxonService.loadValueObject( searchSettings.getTaxonConstraint() );
            } else {
                this.taxon = null;
            }
            if ( searchSettings.getPlatformConstraint() != null ) {
                this.platform = arrayDesignService.loadValueObject( searchSettings.getPlatformConstraint() );
            } else {
                this.platform = null;
            }
            this.maxResults = searchSettings.getMaxResults() > 0 ? searchSettings.getMaxResults() : null;
        }
    }

    /**
     * Representation of {@link SearchResult} for the RESTful API.
     */
    @Value
    public static class SearchResultValueObject<T extends IdentifiableValueObject<?>> {

        Long resultId;

        @Schema(ref = "SearchResultType")
        String resultType;

        double score;

        Map<String, String> highlights;

        @Schema(hidden = true)
        String source;

        /**
         * How the result matched the query (e.g. {@code exact_symbol}, {@code alias},
         * {@code prefix}), or {@code null} when the producing source did not classify it.
         * Lets a client distinguish a safe alias hit from a low-trust prefix look-alike even
         * when their scores coincide. See {@code SearchMatchType}.
         */
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "How the result matched the query (exact_symbol, alias, prefix, official_name, official_name_prefix, exact_identifier); omitted if unclassified.")
        String matchType;

        @Schema(oneOf = {
                ArrayDesignValueObject.class,
                BibliographicReferenceValueObject.class,
                BioSequenceValueObject.class,
                CompositeSequenceValueObject.class,
                ExpressionExperimentValueObject.class,
                ExpressionExperimentSetValueObject.class,
                GeneValueObject.class,
                GeneSetValueObject.class,
                CharacteristicValueObject.class // for PhenotypeAssociation
        })
        @JsonInclude(JsonInclude.Include.NON_NULL)
        T resultObject;

        @Nullable
        String resultObjectUrl;

        /**
         * Indicate that the result object URL is external and not under Gemma's control.
         */
        boolean resultObjectUrlExternal;

        public SearchResultValueObject( SearchResult<T> searchResult, @Nullable String resultObjectUrl, boolean resultObjectUrlExternal ) {
            this.resultId = searchResult.getResultId();
            this.resultType = searchResult.getResultType().getName();
            this.resultObject = searchResult.getResultObject();
            this.resultObjectUrl = resultObjectUrl;
            this.resultObjectUrlExternal = resultObjectUrlExternal;
            this.score = searchResult.getScore();
            this.highlights = searchResult.getHighlights();
            this.source = searchResult.getSource().toString();
            this.matchType = searchResult.getMatchKind() != null ? searchResult.getMatchKind().getWireName() : null;
        }
    }

    public static class SearchResultsResponseDataObject extends ResponseDataObject<List<SearchResultValueObject<?>>> {

        private final SearchSettingsValueObject searchSettings;

        /**
         * @param payload the data to be serialised and returned as the response payload.
         */
        public SearchResultsResponseDataObject( List<SearchResultValueObject<?>> payload, SearchSettingsValueObject searchSettings ) {
            super( payload );
            this.searchSettings = searchSettings;
        }

        public SearchSettingsValueObject getSearchSettings() {
            return searchSettings;
        }
    }
}
