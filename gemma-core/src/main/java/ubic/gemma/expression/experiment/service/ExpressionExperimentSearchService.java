package ubic.gemma.expression.experiment.service;

import java.util.Collection;
import java.util.List;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.search.SearchResultDisplayObject;

public interface ExpressionExperimentSearchService {

    /**
     * 
     * @param query
     * @return Collection of expression experiment entity objects
     */
    public abstract Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query );

    /**
     * does not include session bound sets
     * 
     * @param query
     * @param taxonId if the search should not be limited by taxon, pass in null
     * @return Collection of SearchResultDisplayObjects
     */
    public abstract List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId );

}