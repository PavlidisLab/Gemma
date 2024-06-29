package ubic.gemma.core.search;

import org.junit.Test;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.test.Maps.map;

/**
 * Tests for {@link SearchResult}.
 *
 * @author poirigui
 */
public class SearchResultTest {

    private static class FooBar implements Identifiable {

        @Nullable
        private final Long id;

        private FooBar( @Nullable Long id ) {
            this.id = id;
        }

        @Override
        @Nullable
        public Long getId() {
            return id;
        }
    }

    @Test
    public void testResultObject() {
        SearchResult<Identifiable> sr = SearchResult.from( FooBar.class, new FooBar( 1L ), 1.0, Collections.singletonMap( "a", "b" ), "test object" );
        assertThat( sr.getScore() ).isEqualTo( 1.0 );
        assertThat( sr.getHighlights() ).isEqualTo( map( "a", "b" ) );
        assertThat( sr ).hasToString( String.format( "FooBar Id=1 Score=%.2f Highlights=a Source=test object [Not Filled]", 1.0 ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResultObjectWithNullId() {
        SearchResult.from( FooBar.class, new FooBar( null ), 1.0, null, "test object" );
    }

    @Test
    public void testSetResultObject() {
        SearchResult<Identifiable> sr = SearchResult.from( FooBar.class, 1L, 1.0, null, "test object" );
        sr.setResultObject( new FooBar( 1L ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetResultObjectWithNullId() {
        SearchResult<Identifiable> sr = SearchResult.from( FooBar.class, 1L, 1.0, null, "test object" );
        sr.setResultObject( new FooBar( null ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetResultObjectWithDifferentId() {
        SearchResult<Identifiable> sr = SearchResult.from( FooBar.class, 1L, 1.0, null, "test object" );
        sr.setResultObject( new FooBar( 2L ) );
    }

}