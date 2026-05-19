package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchResultSet;
import ubic.gemma.model.common.search.SearchSettings;
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
 * Sources are used in the order they are passed to the {@link #CompositeSearchSource(List)} constructor. This source
 * checks each child source via {@link SearchSource#accepts(SearchSettings)} and only delegates to the ones that accept
 * the settings.
 *
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

    public void setFastWarningThresholdMillis( int fastWarningThresholdMillis ) {
        Assert.isTrue( fastWarningThresholdMillis >= 0, "fastWarningThresholdMillis must be non-negative" );
        this.fastWarningThresholdMillis = fastWarningThresholdMillis;
    }

    public void setWarningThresholdMills( int warningThresholdMills ) {
        Assert.isTrue( warningThresholdMills >= 0, "warningThresholdMills must be non-negative" );
        this.warningThresholdMills = warningThresholdMills;
    }

    @Override
    public boolean accepts( SearchSettings settings ) {
        return sources.stream().anyMatch( s -> s.accepts( settings ) );
    }

    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchArrayDesign( st, context ), ArrayDesign.class );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchBibliographicReference( st, context ), BibliographicReference.class );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchExperimentSet( st, context ), ExpressionExperimentSet.class );
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchBioSequence( st, context ), BioSequence.class );
    }

    @Override
    @Deprecated
    public Collection<SearchResult<?>> searchBioSequenceAndGene( SearchSettings settings, SearchContext context, @Nullable Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException {
        Set<SearchResult<?>> results = new HashSet<>();
        for ( SearchSource source : sources ) {
            results.addAll( source.searchBioSequenceAndGene( settings, context, previousGeneSearchResults ) );
        }
        return results;
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchCompositeSequence( st, context ), CompositeSequence.class );
    }

    @Override
    @Deprecated
    public Collection<SearchResult<?>> searchCompositeSequenceAndGene( SearchSettings settings, SearchContext context ) throws SearchException {
        Set<SearchResult<?>> results = new HashSet<>();
        for ( SearchSource source : sources ) {
            results.addAll( source.searchCompositeSequenceAndGene( settings, context ) );
        }
        return results;
    }

    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchExpressionExperiment( st, context ), ExpressionExperiment.class );
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchGene( st, context ), Gene.class );
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchGeneSet( st, context ), GeneSet.class );
    }

    @Override
    public Collection<SearchResult<BlacklistedEntity>> searchBlacklistedEntities( SearchSettings settings, SearchContext context ) throws SearchException {
        return searchWith( settings, ( s, st ) -> s.searchBlacklistedEntities( st, context ), BlacklistedEntity.class );
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
