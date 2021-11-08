package ubic.gemma.web.services.rest;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;

import javax.ws.rs.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private SearchService searchService;

    /**
     * Search everything.
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search everything in Gemma.")
    public ResponseDataObject<List<SearchResultValueObject>> search( @QueryParam("query") String query, @QueryParam("resultTypes") List<String> resultTypes ) {
        Map<String, Class<?>> supportedResultTypesByName = searchService.getSupportedResultTypes().stream()
                .collect( Collectors.toMap( Class::getName, identity() ) );
        Collection<Class<?>> resultTypesCls;
        if ( resultTypes == null || resultTypes.isEmpty() ) {
            // include everything
            resultTypesCls = supportedResultTypesByName.values();
        } else if ( supportedResultTypesByName.keySet().containsAll( resultTypes ) ) {
            // only include what the user asks for
            resultTypesCls = resultTypes.stream().map( supportedResultTypesByName::get ).collect( Collectors.toSet() );
        } else {
            throw new BadRequestException( String.format( "Unsupported result type(s). Ensure that your results are among: %s.",
                    supportedResultTypesByName.keySet().stream().collect( Collectors.joining( ", " ) ) ) );
        }
        SearchSettings searchSettings = SearchSettings.builder()
                .query( query )
                .resultTypes( resultTypesCls )
                .build();
        return Responder.respond( searchAndInitialize( searchSettings ) );
    }

    @Transactional(readOnly = true)
    List<SearchResultValueObject> searchAndInitialize( SearchSettings searchSettings ) {
        return searchService.search( searchSettings ).values()
                .stream()
                .flatMap( List::stream )
                .sorted() // SearchResults are sorted by descending score order
                .map( SearchResultValueObject::new )
                .filter( vo -> vo.resultObject != null ) // exclude null results, there will be a warning in the logs
                .collect( Collectors.toList() );
    }

    /**
     * Representation of {@link SearchResult} for the RESTful API.
     */
    @Data
    public class SearchResultValueObject {

        private final Long resultId;

        private final String resultType;

        private final Double score;

        private final Object resultObject;

        public SearchResultValueObject( SearchResult searchResult ) {
            this.resultId = searchResult.getResultId();
            this.resultType = searchResult.getResultClass().getName();
            this.score = searchResult.getScore();
            this.resultObject = toValueObject( searchResult.getResultObject(), searchResult.getResultClass() );
        }

        private <T> Object toValueObject( T resultObject, Class<T> resultClass ) {
            if ( resultObject instanceof Gene ) {
                return new GeneValueObject( ( Gene ) resultObject );
            } else if ( resultObject instanceof ExpressionExperiment ) {
                return new ExpressionExperimentValueObject( ( ExpressionExperiment ) resultObject );
            } else if ( resultObject instanceof CompositeSequence ) {
                return new CompositeSequenceValueObject( ( CompositeSequence ) resultObject );
            } else if ( resultObject instanceof BioSequence ) {
                return null;
                // return BioSequenceValueObject.fromEntity( ( BioSequence ) resultObject );
            } else if ( searchService.getSupportedResultTypes().contains( resultClass ) ) {
                // ideally we would raise a specific HTTP status code,
                log.warn( "Result type " + resultClass + " is not handled by this endpoint." );
                return null;
            } else {
                // this should never happen though since we minimally expect the search service to only produce result types it supports.
                throw new IllegalArgumentException( "Unsupported result type " + resultClass + " for searching." );
            }
        }
    }
}
