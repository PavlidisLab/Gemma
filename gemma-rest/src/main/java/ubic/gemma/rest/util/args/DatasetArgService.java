package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DatasetArgService extends AbstractEntityArgService<ExpressionExperiment, ExpressionExperimentService> {

    @Autowired
    private SearchService searchService;

    @Autowired
    public DatasetArgService( ExpressionExperimentService service ) {
        super( service );
    }

    @Override
    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg ) {
        return service.getFiltersWithInferredAnnotations( super.getFilters( filterArg ) );
    }

    /**
     * Obtain a {@link Filter} for the result of a {@link ExpressionExperiment} search.
     * <p>
     * The filter is a restriction over the EE IDs.
     *
     * @param query    query
     * @param _results destination for the search results
     */
    public Filter getFilterForSearchQuery( String query, List<SearchResult<ExpressionExperiment>> _results ) {
        try {
            List<SearchResult<ExpressionExperiment>> results = searchService.search( SearchSettings.expressionExperimentSearch( query )
                            .withCacheResults( true )
                            .withFillResults( false ) )
                    .getByResultObjectType( ExpressionExperiment.class );
            if ( _results != null ) {
                _results.addAll( results );
            }
            Set<String> ids = results.stream()
                    .map( SearchResult::getResultId )
                    .map( String::valueOf )
                    .collect( Collectors.toSet() );
            if ( ids.isEmpty() ) {
                return service.getFilter( "id", Filter.Operator.eq, "-1" );
            } else {
                return service.getFilter( "id", Filter.Operator.in, ids );
            }
        } catch ( SearchException e ) {
            throw new BadRequestException( e );
        }
    }

    /**
     * @see #getFilterForSearchQuery(String, List)
     */
    public Filter getFilterForSearchQuery( String query ) {
        return getFilterForSearchQuery( query, null );
    }
}
