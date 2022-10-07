package ubic.gemma.core.search;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.util.Collection;

/**
 * Search source that provides {@link SearchResult} from a search engine.
 * @author poirigui
 */
public interface SearchSource {

    Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws SearchException;

    /**
     * Search for biosequence and, unfortunately genes.
     *
     * I wanted to remove this, but there's some logic with indirect gene hit penalty that we might want to keep around.
     *
     * @deprecated use {@link #searchBioSequence(SearchSettings)} (SearchSettings)} instead
     *
     * @return a mixture of {@link BioSequence} and {@link Gene} matching the search settings.
     */
    @Deprecated
    @SuppressWarnings("unused")
    Collection<SearchResult> searchBioSequenceAndGene( SearchSettings settings,
            Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException;

    Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) throws SearchException;

    /**
     * Search for composite sequences and, unfortunately, genes.
     * <p>
     * FIXME: this should solely return {@link CompositeSequence}
     *
     * @deprecated use {@link #searchCompositeSequence(SearchSettings)} instead
     *
     * @return a mixture of {@link Gene} and {@link CompositeSequence} matching the search settings
     */
    @Deprecated
    Collection<SearchResult> searchCompositeSequenceAndGene( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException;

    Collection<SearchResult<CharacteristicValueObject>> searchPhenotype( SearchSettings settings ) throws SearchException;
}
