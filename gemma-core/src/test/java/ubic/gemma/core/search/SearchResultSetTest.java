package ubic.gemma.core.search;

import org.assertj.core.data.Index;
import org.junit.Test;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ubic.gemma.core.util.test.Maps.map;

public class SearchResultSetTest {

    @Test
    public void test() {
        SearchSettings settings = SearchSettings.expressionExperimentSearch( "test" );
        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );
        assertTrue( results.add( SearchResult.from( ExpressionExperiment.class, 1L, 0.5, null, "test" ) ) );
        assertTrue( results.add( SearchResult.from( ExpressionExperiment.class, 1L, 0.6, null, "test" ) ) );
        assertThat( results ).hasSize( 1 )
                .extracting( SearchResult::getScore ).containsExactly( 0.6 );
    }

    @Test
    public void testResultObjectIsRetainedWhenReplacingAResult() {
        SearchSettings settings = SearchSettings.expressionExperimentSearch( "test" );
        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );
        assertTrue( results.add( SearchResult.from( ExpressionExperiment.class, new ExpressionExperiment() {{
            setId( 1L );
        }}, 0.5, null, "test" ) ) );
        // replaced by a better result without a result object
        assertTrue( results.add( SearchResult.from( ExpressionExperiment.class, 1L, 0.6, null, "test" ) ) );
        assertThat( results ).satisfiesExactly( sr -> {
            assertThat( sr.getResultId() ).isEqualTo( 1L );
            assertThat( sr.getResultObject() ).isNotNull();
            assertThat( sr.getScore() ).isEqualTo( 0.6 );
        } );
    }

    @Test
    public void testAddWhenMaxResultsIsReached() {
        SearchSettings settings = SearchSettings.expressionExperimentSearch( "test" )
                .withMaxResults( 3 );
        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );
        assertTrue( results.add( SearchResult.from( ExpressionExperiment.class, 1L, 0.5, null, "test" ) ) );
        assertTrue( results.add( SearchResult.from( ExpressionExperiment.class, 2L, 0.6, null, "test" ) ) );
        assertTrue( results.add( SearchResult.from( ExpressionExperiment.class, 3L, 0.5, null, "test" ) ) );
        // ignored
        assertFalse( results.add( SearchResult.from( ExpressionExperiment.class, 4L, 0.6, null, "test" ) ) );
        assertThat( results ).hasSize( 3 );
        // this is allowed though as it replaces a previosu result
        assertTrue( results.add( SearchResult.from( ExpressionExperiment.class, 3L, 0.6, null, "test" ) ) );
        assertThat( results ).hasSize( 3 )
                .extracting( SearchResult::getResultId )
                .containsExactlyInAnyOrder( 1L, 2L, 3L );
    }

    @Test
    public void testMergingHighlightWhenReplacingAResult() {
        SearchSettings settings = SearchSettings.expressionExperimentSearch( "test" );
        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );
        results.add( SearchResult.from( ExpressionExperiment.class, 1L, 0.5, Collections.singletonMap( "a", "a" ), "test" ) );
        results.add( SearchResult.from( ExpressionExperiment.class, 1L, 0.6, Collections.singletonMap( "b", "b" ), "test" ) );
        assertThat( results ).hasSize( 1 )
                .extracting( SearchResult::getHighlights )
                .satisfies( h -> {
                    assertThat( h ).containsEntry( "a", "a" ).containsEntry( "b", "b" );
                }, Index.atIndex( 0 ) );
    }

    @Test
    public void testMergingHighlightWhenRetainingAnExistingResult() {
        SearchSettings settings = SearchSettings.expressionExperimentSearch( "test" );
        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );
        results.add( SearchResult.from( ExpressionExperiment.class, 1L, 0.6, Collections.singletonMap( "a", "a" ), "test" ) );
        results.add( SearchResult.from( ExpressionExperiment.class, 1L, 0.5, map( "a", "b", "b", "b" ), "test" ) );
        assertThat( results ).hasSize( 1 )
                .extracting( SearchResult::getHighlights )
                .containsExactly( map( "a", "a", "b", "b" ) );
    }
}