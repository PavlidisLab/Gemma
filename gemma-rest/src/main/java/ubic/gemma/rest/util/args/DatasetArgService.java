package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.Highlighter;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import java.util.Collection;
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
    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg ) throws BadRequestException {
        return getFilters( filterArg, null );
    }

    public Filters getFilters( FilterArg<ExpressionExperiment> filterArg, @Nullable Collection<String> impliedTermUris ) {
        return service.getFiltersWithInferredAnnotations( super.getFilters( filterArg ), impliedTermUris );
    }

    /**
     * Obtain a {@link Filter} for the result of a {@link ExpressionExperiment} search.
     * <p>
     * The filter is a restriction over the EE IDs.
     *
     * @param query    query
     * @param minScore minimum score to retain a result
     * @param _results destination for the search results
     */
    public Filter getFilterForSearchQuery( String query, double minScore, @Nullable Highlighter highlighter, @Nullable List<SearchResult<ExpressionExperiment>> _results ) {
        try {
            SearchSettings settings = SearchSettings.builder()
                    .query( query )
                    .resultType( ExpressionExperiment.class )
                    .highlighter( highlighter )
                    .fillResults( false )
                    .build();
            List<SearchResult<ExpressionExperiment>> results = searchService.search( settings )
                    .getByResultObjectType( ExpressionExperiment.class )
                    .stream()
                    .filter( r -> r.getScore() >= minScore )
                    .collect( Collectors.toList() );
            if ( _results != null ) {
                _results.addAll( results );
            }
            Set<Long> ids = results.stream()
                    .map( SearchResult::getResultId )
                    .collect( Collectors.toSet() );
            if ( ids.isEmpty() ) {
                return service.getFilter( "id", Long.class, Filter.Operator.eq, -1L );
            } else {
                return service.getFilter( "id", Long.class, Filter.Operator.in, ids );
            }
        } catch ( SearchException e ) {
            throw new BadRequestException( e );
        }
    }

    /**
     * @see #getFilterForSearchQuery(String, double, Highlighter, List)
     */
    public Filter getFilterForSearchQuery( String query ) {
        return getFilterForSearchQuery( query, 0.0, null, null );
    }
}
