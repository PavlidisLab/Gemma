package ubic.gemma.core.search;

import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Search source that provides {@link SearchResult} from a search engine.
 * @author poirigui
 */
public interface SearchSource {

    /**
     * Indicate if this source accepts the given search settings.
     */
    boolean accepts( SearchSettings settings );

    default Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }

    default Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }

    default Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }

    default Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }

    /**
     * Search for biosequence and, unfortunately genes.
     * <p>
     * I wanted to remove this, but there's some logic with indirect gene hit penalty that we might want to keep around.
     *
     * @return a mixture of {@link BioSequence} and {@link Gene} matching the search settings.
     * @deprecated use {@link #searchBioSequence(SearchSettings)} (SearchSettings)} instead
     */
    @Deprecated
    default Collection<SearchResult<?>> searchBioSequenceAndGene( SearchSettings settings,
            @Nullable Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException {
        Collection<SearchResult<?>> results = new HashSet<>();
        results.addAll( this.searchBioSequence( settings ) );
        results.addAll( this.searchGene( settings ) );
        return results;
    }

    default Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }

    /**
     * Search for composite sequences and, unfortunately, genes.
     * <p>
     * FIXME: this should solely return {@link CompositeSequence}
     *
     * @return a mixture of {@link Gene} and {@link CompositeSequence} matching the search settings
     * @deprecated use {@link #searchCompositeSequence(SearchSettings)} instead
     */
    @Deprecated
    default Collection<SearchResult<?>> searchCompositeSequenceAndGene( SearchSettings settings ) throws SearchException {
        Collection<SearchResult<?>> results = new HashSet<>();
        results.addAll( this.searchCompositeSequence( settings ) );
        results.addAll( this.searchGene( settings ) );
        return results;
    }

    default Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }

    default Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }

    default Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }

    default Collection<SearchResult<BlacklistedEntity>> searchBlacklistedEntities( SearchSettings settings ) throws SearchException {
        return Collections.emptyList();
    }
}
