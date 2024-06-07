package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchResultSet;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A search source constituted of multiple other sources.
 * <p>
 * Sources are used in the order they are passed to the {@link #CompositeSearchSource(List)} constructor.
 * <p>
 * This source checks if the {@link SearchSource} are accepted by each individual source with
 * {@link SearchSource#accepts(SearchSettings)} and subsequently delegate the operation.
 * <p>
 * It also supports logging of the time spent by each source and the number of results found. This is done at the DEBUG
 * level unless the value set by {@link #setWarningThresholdMills(int)} or {@link #setFastWarningThresholdMillis(int)}
 * is exceeded in which case WARNING is used.
 * @author poirigui
 */
@CommonsLog
public class CompositeSearchSource implements SearchSource {

    private final List<SearchSource> sources;

    private int fastWarningThresholdMillis = 100;
    private int warningThresholdMills = 1000;

    public CompositeSearchSource( List<SearchSource> sources ) {
        this.sources = sources;
    }

    /**
     * Threshold in milliseconds for a warning to be logged when searching with {@link ubic.gemma.model.common.search.SearchSettings.SearchMode#FAST}.
     * <p>
     * The default is 100 ms.
     */
    public void setFastWarningThresholdMillis( int fastWarningThresholdMillis ) {
        Assert.isTrue( fastWarningThresholdMillis >= 0 );
        this.fastWarningThresholdMillis = fastWarningThresholdMillis;
    }

    /**
     * Threshold in milliseconds for a warning to be logged.
     * <p>
     * The default is 1000 ms.
     */
    public void setWarningThresholdMills( int warningThresholdMills ) {
        Assert.isTrue( warningThresholdMills >= 0 );
        this.warningThresholdMills = warningThresholdMills;
    }

    @Override
    public boolean accepts( SearchSettings settings ) {
        return sources.stream().anyMatch( s -> s.accepts( settings ) );
    }

    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws SearchException {
        return searchWith( settings, SearchSource::searchArrayDesign, ArrayDesign.class );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) throws SearchException {
        return searchWith( settings, SearchSource::searchBibliographicReference, BibliographicReference.class );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws SearchException {
        return searchWith( settings, SearchSource::searchExperimentSet, ExpressionExperimentSet.class );
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws SearchException {
        return searchWith( settings, SearchSource::searchBioSequence, BioSequence.class );
    }

    @Override
    @Deprecated
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
        return searchWith( settings, SearchSource::searchCompositeSequence, CompositeSequence.class );
    }

    @Override
    @Deprecated
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
        return searchWith( settings, SearchSource::searchExpressionExperiment, ExpressionExperiment.class );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws SearchException {
        return searchWith( settings, SearchSource::searchGene, Gene.class );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException {
        return searchWith( settings, SearchSource::searchGeneSet, GeneSet.class );
    }

    @Override
    public Collection<SearchResult<BlacklistedEntity>> searchBlacklistedEntities( SearchSettings settings ) throws SearchException {
        return searchWith( settings, SearchSource::searchBlacklistedEntities, BlacklistedEntity.class );
    }

    private interface SearchFunction<T extends Identifiable> {
        Collection<SearchResult<T>> apply( SearchSource searchSource, SearchSettings settings ) throws SearchException;
    }

    private <T extends Identifiable> Collection<SearchResult<T>> searchWith( SearchSettings settings, SearchFunction<T> func, Class<T> clazz ) throws SearchException {
        StopWatch timer = StopWatch.createStarted();
        Set<SearchResult<T>> results = new SearchResultSet<>( settings );
        long[] timeSpentBySource = new long[sources.size()];
        int[] foundItemsBySource = new int[sources.size()];
        int[] newItemsBySource = new int[sources.size()];
        for ( int i = 0; i < sources.size(); i++ ) {
            long timeBefore = timer.getTime( TimeUnit.MILLISECONDS );
            SearchSource source = sources.get( i );
            if ( source.accepts( settings ) ) {
                int sizeBefore = results.size();
                Collection<SearchResult<T>> r = func.apply( source, settings );
                results.addAll( r );
                foundItemsBySource[i] = r.size();
                newItemsBySource[i] = results.size() - sizeBefore;
            } else {
                foundItemsBySource[i] = 0;
                newItemsBySource[i] = 0;
            }
            timeSpentBySource[i] = timer.getTime( TimeUnit.MILLISECONDS ) - timeBefore;
        }
        timer.stop();
        boolean shouldWarn;
        switch ( settings.getMode() ) {
            case FAST:
                shouldWarn = timer.getTime() > Math.min( fastWarningThresholdMillis, warningThresholdMills );
                break;
            case BALANCED:
                shouldWarn = timer.getTime() > warningThresholdMills;
                break;
            case ACCURATE:
            default:
                shouldWarn = false;
        }
        if ( shouldWarn || log.isDebugEnabled() ) {
            String breakdownBySource = IntStream.range( 0, sources.size() )
                    .mapToObj( i -> String.format( "source: %s, found items: %d, found items (novel): %d, time spent: %d ms",
                            sources.get( i ).getClass().getSimpleName(), foundItemsBySource[i], newItemsBySource[i], timeSpentBySource[i] ) )
                    .collect( Collectors.joining( "; " ) );
            String message = String.format( "Found %d %s results in %d ms (%s)", results.size(), clazz.getSimpleName(),
                    timer.getTime( TimeUnit.MILLISECONDS ), breakdownBySource );
            if ( shouldWarn ) {
                log.warn( message );
            } else {
                log.debug( message );
            }
        }
        return results;
    }
}
