package ubic.gemma.core.search;

import org.junit.Test;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SearchResult}.
 *
 * @author poirigui
 */
public class SearchResultTest {

    private static class FooBar implements Identifiable {

        private final Long id;

        private FooBar( @Nullable Long id ) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    @Test
    public void testResultObject() {
        SearchResult<Identifiable> sr = SearchResult.from( FooBar.class, new FooBar( 1L ), 1.0, "test object" );
        assertThat( sr.getScore() ).isEqualTo( 1.0 );
        assertThat( sr.getHighlightedText() ).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResultObjectWithNullId() {
        SearchResult.from( FooBar.class, new FooBar( null ), 1.0, "test object" );
    }

    @Test
    public void testSetResultObject() {
        SearchResult<Identifiable> sr = SearchResult.from( FooBar.class, 1L, 1.0, "test object" );
        sr.setResultObject( new FooBar( 1L ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetResultObjectWithNullId() {
        SearchResult<Identifiable> sr = SearchResult.from( FooBar.class, 1L, 1.0, "test object" );
        sr.setResultObject( new FooBar( null ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetResultObjectWithDifferentId() {
        SearchResult<Identifiable> sr = SearchResult.from( FooBar.class, 1L, 1.0, "test object" );
        sr.setResultObject( new FooBar( 2L ) );
    }

}