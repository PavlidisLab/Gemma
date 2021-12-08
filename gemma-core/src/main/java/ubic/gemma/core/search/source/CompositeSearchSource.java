package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchResultSet;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A search source constituted of multiple other sources.
 *
 * @author poirigui
 */
@CommonsLog
public class CompositeSearchSource implements SearchSource {

    private final List<SearchSource> sources;

    public CompositeSearchSource( List<SearchSource> sources ) {
        this.sources = sources;
    }

    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws SearchException {
        return searchWith( ( s ) -> s.searchArrayDesign( settings ) );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) throws SearchException {
        return searchWith( ( s ) -> s.searchBibliographicReference( settings ) );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws SearchException {
        return searchWith( ( s ) -> s.searchExperimentSet( settings ) );
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws SearchException {
        return searchWith( ( s ) -> s.searchBioSequence( settings ) );
    }

    @Override
    public Collection<SearchResult<?>> searchBioSequenceAndGene( SearchSettings settings, @Nullable Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException {
        // FIXME: use searchWith
        Set<SearchResult<?>> results = new HashSet<>();
        for ( SearchSource source : sources ) {
            results.addAll( source.searchBioSequenceAndGene( settings, previousGeneSearchResults ) );
        }
        return results;
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) throws SearchException {
        return searchWith( ( s ) -> s.searchCompositeSequence( settings ) );
    }

    @Override
    public Collection<SearchResult<?>> searchCompositeSequenceAndGene( SearchSettings settings ) throws SearchException {
        // FIXME: use searchWith
        Set<SearchResult<?>> results = new HashSet<>();
        for ( SearchSource source : sources ) {
            results.addAll( source.searchCompositeSequenceAndGene( settings ) );
        }
        return results;
    }

    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws SearchException {
        return searchWith( ( s ) -> s.searchExpressionExperiment( settings ) );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws SearchException {
        return searchWith( ( s ) -> s.searchGene( settings ) );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException {
        return searchWith( ( s ) -> s.searchGeneSet( settings ) );
    }

    @FunctionalInterface
    public interface SearchFunction<T extends Identifiable> {
        Collection<SearchResult<T>> apply( SearchSource searchSource ) throws SearchException;
    }

    private <T extends Identifiable> Collection<SearchResult<T>> searchWith( SearchFunction<T> func ) throws SearchException {
        StopWatch timer = StopWatch.createStarted();
        Set<SearchResult<T>> results = new SearchResultSet<>();
        long[] timeSpentBySource = new long[sources.size()];
        int[] foundItemsBySource = new int[sources.size()];
        int[] newItemsBySource = new int[sources.size()];
        for ( int i = 0; i < sources.size(); i++ ) {
            long timeBefore = timer.getTime( TimeUnit.MILLISECONDS );
            int sizeBefore = results.size();
            SearchSource source = sources.get( i );
            Collection<SearchResult<T>> r = func.apply( source );
            results.addAll( r );
            foundItemsBySource[i] = r.size();
            newItemsBySource[i] = results.size() - sizeBefore;
            timeSpentBySource[i] = timer.getTime( TimeUnit.MILLISECONDS ) - timeBefore;
        }
        timer.stop();
        boolean shouldWarn = timer.getTime( TimeUnit.MILLISECONDS ) > 200;
        if ( shouldWarn || log.isDebugEnabled() ) {
            String breakdownBySource = IntStream.range( 0, sources.size() ).mapToObj( i -> String.format( "source: %s, found items: %d, found items (novel): %d, time spent: %d ms", sources.get( i ).getClass().getSimpleName(), foundItemsBySource[i], newItemsBySource[i], timeSpentBySource[i] ) ).collect( Collectors.joining( "; " ) );
            String message = String.format( "Found %d results in %d ms (%s)", results.size(), timer.getTime( TimeUnit.MILLISECONDS ), breakdownBySource );
            if ( shouldWarn ) {
                log.warn( message );
            } else {
                log.debug( message );
            }
        }
        return results;
    }
}
