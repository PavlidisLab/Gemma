package ubic.gemma.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ubic.gemma.core.search.DefaultHighlighter;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.search.lucene.SimpleMarkdownFormatter;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
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
import ubic.gemma.rest.annotations.GZIP;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;
import ubic.gemma.rest.util.MalformedArgException;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
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
@CommonsLog
public class SearchWebService {

    /**
     * Name used in the OpenAPI schema to identify result types as per {@link #search(QueryArg, TaxonArg, PlatformArg, List, LimitArg, ExcludeArg)}'s
     * fourth argument.
     */
    public static final String RESULT_TYPES_SCHEMA_NAME = "SearchResultType";

    /**
     * Maximum number of search results.
     */
    public static final int MAX_SEARCH_RESULTS = 2000;

    /**
     * Maximum number of highlighted documents.
     */
    private static final int MAX_HIGHLIGHTED_DOCUMENTS = 500;

    @Autowired
    private SearchService searchService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private TaxonArgService taxonArgService;
    @Autowired
    private PlatformArgService platformArgService;

    @Autowired
    private HttpServletRequest request;

    /**
     * Highlights search result.
     */
    @ParametersAreNonnullByDefault
    private class Highlighter extends DefaultHighlighter {

        private int highlightedDocuments = 0;

        public Highlighter() {
            super( new SimpleMarkdownFormatter() );
        }

        @Override
        public Map<String, String> highlightTerm( @Nullable String uri, String label, String field ) {
            String searchUrl = ServletUriComponentsBuilder.fromRequest( request )
                    .scheme( null ).host( null ).port( -1 )
                    .replaceQueryParam( "query", uri != null ? uri : label )
                    .build()
                    .toUriString();
            return Collections.singletonMap( field, String.format( "**[%s](%s)**", label, searchUrl ) );
        }

        @Override
        public Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer ) {
            if ( highlightedDocuments >= MAX_HIGHLIGHTED_DOCUMENTS ) {
                return Collections.emptyMap();
            }
            highlightedDocuments++;
            return super.highlightDocument( document, highlighter, analyzer );
        }
    }

    /**
     * Search everything subject to taxon and platform constraints.
     * <p>
     * Naming the schema in for the result types is necessary so that it can be resolved in {@link CustomModelResolver}.
     */
    @GET
    @GZIP
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search everything in Gemma")
    public SearchResultsResponseDataObject search(
            @QueryParam("query") QueryArg query,
            @QueryParam("taxon") TaxonArg<?> taxonArg,
            @QueryParam("platform") PlatformArg<?> platformArg,
            @Parameter(array = @ArraySchema(schema = @Schema(name = RESULT_TYPES_SCHEMA_NAME, hidden = true))) @QueryParam("resultTypes") List<String> resultTypes,
            @Parameter(description = "Maximum number of search results to return; capped at " + MAX_SEARCH_RESULTS + " unless `resultObject` is excluded.", schema = @Schema(type = "integer", minimum = "1", maximum = "" + MAX_SEARCH_RESULTS)) @QueryParam("limit") LimitArg limit,
            @Parameter(description = "List of fields to exclude from the payload. Only `resultObject` is supported.") @QueryParam("exclude") ExcludeArg<SearchResult<?>> excludeArg
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

        SearchSettings searchSettings = SearchSettings.builder()
                .query( query.getValue() )
                .taxon( taxonArg != null ? taxonArgService.getEntity( taxonArg ) : null )
                .platformConstraint( platformArg != null ? platformArgService.getEntity( platformArg ) : null )
                .resultTypes( resultTypesCls )
                .maxResults( maxResults )
                .fillResults( fillResults )
                .highlighter( new Highlighter() )
                .build();

        List<SearchResult<?>> searchResults;
        try {
            searchResults = searchService.search( searchSettings ).toList();
        } catch ( SearchException e ) {
            throw new BadRequestException( String.format( "Invalid search settings: %s.", ExceptionUtils.getRootCauseMessage( e ) ), e );
        }

        List<SearchResult<? extends IdentifiableValueObject<?>>> searchResultVos;

        // Some result VOs are null for unknown reasons, see https://github.com/PavlidisLab/Gemma/issues/417
        if ( fillResults ) {
            searchResultVos = searchService.loadValueObjects( searchResults );
        } else {
            searchResultVos = searchResults.stream()
                    .map( sr -> sr.withResultObject( ( IdentifiableValueObject<?> ) null ) )
                    .collect( Collectors.toList() );
        }

        // convert the response to search results of VOs
        return new SearchResultsResponseDataObject( searchResultVos.stream()
                .sorted() // SearchResults are sorted by descending score order
                .limit( maxResults > 0 ? maxResults : Long.MAX_VALUE ) // results are limited by class, so there might be more results than expected when unraveling everything
                .map( SearchResultValueObject::new )
                .collect( Collectors.toList() ), new SearchSettingsValueObject( searchSettings ) );
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
            if ( searchSettings.getTaxon() != null ) {
                this.taxon = taxonService.loadValueObject( searchSettings.getTaxon() );
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

        public SearchResultValueObject( SearchResult<T> searchResult ) {
            this.resultId = searchResult.getResultId();
            this.resultType = searchResult.getResultType().getName();
            this.resultObject = searchResult.getResultObject();
            this.score = searchResult.getScore();
            this.highlights = searchResult.getHighlights();
            this.source = searchResult.getSource().toString();
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
