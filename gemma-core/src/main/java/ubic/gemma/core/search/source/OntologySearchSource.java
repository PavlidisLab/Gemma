package ubic.gemma.core.search.source;

import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.Map;

public interface OntologySearchSource extends SearchSource {

    /**
     * Flavour of {@link #searchExpressionExperiment(SearchSettings)} that searches directly with URIs.
     * @param settings  search settings
     * @param uris      collection of URIs to use for searching
     * @param uri2value labels for mapping URIs to text
     */
    Collection<SearchResult<ExpressionExperiment>> searchExpressionExperimentByUris( SearchSettings settings, Collection<String> uris, Map<String, String> uri2value, Map<String, Double> uri2score );
}
