package ubic.gemma.core.search.source;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the identifier-match short-circuit in {@link CompositeSearchSource}: once any source
 * returns a result tagged {@link SearchResult#isExactIdentifierMatch()}, subsequent sources are
 * not consulted. This prevents the Lucene full-text leg from running (and contributing fuzzy
 * matches) for queries that the DB has already pinned by canonical identifier — e.g. a dataset
 * short name like {@code west-breast}, a GSE accession, an NCBI gene id.
 */
public class CompositeSearchSourceTest {

    private static final SearchSettings EE_SETTINGS = SearchSettings.expressionExperimentSearch( "west-breast" );

    /**
     * Test double that records whether it was invoked and returns a fixed result list.
     */
    private static final class RecordingSource implements SearchSource {
        boolean searchCalled = false;
        final Collection<SearchResult<ExpressionExperiment>> eeResults;

        RecordingSource( Collection<SearchResult<ExpressionExperiment>> eeResults ) {
            this.eeResults = eeResults;
        }

        @Override
        public boolean accepts( SearchSettings settings ) {
            return true;
        }

        @Override
        public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings, SearchContext context ) {
            searchCalled = true;
            return eeResults;
        }
    }

    @Test
    public void exactIdentifierHitShortCircuitsLaterSources() throws SearchException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 42L );
        SearchResult<ExpressionExperiment> identifierHit =
                SearchResult.fromExactIdentifier( ExpressionExperiment.class, ee, 1.0, null, "DatabaseSearchSource" );

        RecordingSource first = new RecordingSource( Collections.singleton( identifierHit ) );
        RecordingSource second = new RecordingSource( Collections.emptySet() );

        CompositeSearchSource composite = new CompositeSearchSource( Arrays.asList( first, second ) );
        Collection<SearchResult<ExpressionExperiment>> out =
                composite.searchExpressionExperiment( EE_SETTINGS, new SearchContext( null, null ) );

        assertThat( first.searchCalled ).isTrue();
        assertThat( second.searchCalled )
                .as( "second source must NOT run after first returned an exact-identifier hit" )
                .isFalse();
        assertThat( out ).hasSize( 1 );
        assertThat( out.iterator().next().getResultId() ).isEqualTo( 42L );
    }

    @Test
    public void nonIdentifierHitDoesNotShortCircuit() throws SearchException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 7L );
        // Plain `from(...)` — name match, fuzzy etc. — does NOT carry the identifier flag.
        SearchResult<ExpressionExperiment> fuzzyHit =
                SearchResult.from( ExpressionExperiment.class, ee, 0.95, null, "DatabaseSearchSource" );

        RecordingSource first = new RecordingSource( Collections.singleton( fuzzyHit ) );
        RecordingSource second = new RecordingSource( Collections.emptySet() );

        CompositeSearchSource composite = new CompositeSearchSource( Arrays.asList( first, second ) );
        composite.searchExpressionExperiment( EE_SETTINGS, new SearchContext( null, null ) );

        assertThat( second.searchCalled )
                .as( "without an identifier-grade hit, all sources must run so full-text broadens coverage" )
                .isTrue();
    }

    @Test
    public void emptyFirstSourceFallsThroughToSecond() throws SearchException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 99L );
        SearchResult<ExpressionExperiment> hit =
                SearchResult.from( ExpressionExperiment.class, ee, 0.9, null, "HibernateSearchSource" );

        RecordingSource first = new RecordingSource( Collections.emptySet() );
        RecordingSource second = new RecordingSource( Collections.singleton( hit ) );

        CompositeSearchSource composite = new CompositeSearchSource( Arrays.asList( first, second ) );
        Collection<SearchResult<ExpressionExperiment>> out =
                composite.searchExpressionExperiment( EE_SETTINGS, new SearchContext( null, null ) );

        assertThat( second.searchCalled ).isTrue();
        assertThat( out ).hasSize( 1 );
        assertThat( out.iterator().next().getResultId() ).isEqualTo( 99L );
    }
}
