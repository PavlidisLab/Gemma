package ubic.gemma.core.search;

import ubic.gemma.model.common.search.SearchSettings;

import java.util.Collection;

/**
 * Search source that provides {@link SearchResult} from a search engine.
 * @author poirigui
 */
public interface SearchSource {

    Collection<SearchResult> searchArrayDesign( SearchSettings settings );

    Collection<SearchResult> searchBibliographicReference( SearchSettings settings );

    Collection<SearchResult> searchExperimentSet( SearchSettings settings );

    Collection<SearchResult> searchBioSequence( SearchSettings settings,
            Collection<SearchResult> previousGeneSearchResults );

    Collection<SearchResult> searchCompositeSequence( SearchSettings settings );

    Collection<SearchResult> searchExpressionExperiment( SearchSettings settings );

    Collection<SearchResult> searchGene( SearchSettings settings );

    Collection<SearchResult> searchGeneSet( SearchSettings settings );

    Collection<SearchResult> searchPhenotype( SearchSettings settings );
}
