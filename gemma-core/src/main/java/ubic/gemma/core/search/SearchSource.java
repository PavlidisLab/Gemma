package ubic.gemma.core.search;

import ubic.gemma.model.common.search.SearchSettings;

import java.util.Collection;

/**
 * Search source that provides {@link SearchResult} from a search engine.
 * @author poirigui
 */
public interface SearchSource {

    Collection<SearchResult<?>> searchArrayDesign( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<?>> searchBibliographicReference( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<?>> searchExperimentSet( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<?>> searchBioSequence( SearchSettings settings,
            Collection<SearchResult<?>> previousGeneSearchResults ) throws SearchException;

    Collection<SearchResult<?>> searchCompositeSequence( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<?>> searchExpressionExperiment( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<?>> searchGene( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<?>> searchGeneSet( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<?>> searchPhenotype( SearchSettings settings );
}
