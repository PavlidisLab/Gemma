package ubic.gemma.core.search;

import org.junit.Test;
import ubic.gemma.model.common.Identifiable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SearchResult}.
 *
 * @author poirigui
 */
public class SearchResultTest {

    private static class FooBar implements Identifiable {

        private final Long id;

        private FooBar( Long id ) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    @Test
    public void testResultObject() {
        SearchResult<Identifiable> sr = new SearchResult<>( new FooBar( 1L ), "test object" );
        assertThat( sr.getScore() ).isEqualTo( 1.0 );
        assertThat( sr.getHighlightedText() ).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResultObjectWithNullId() {
        new SearchResult<Identifiable>( new FooBar( null ), "test object" );
    }

    @Test
    public void testSetResultObject() {
        SearchResult<Identifiable> sr = new SearchResult<>( FooBar.class, 1L, "test object" );
        sr.setResultObject( new FooBar( 1L ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetResultObjectWithNullId() {
        SearchResult<Identifiable> sr = new SearchResult<>( FooBar.class, 1L, "test object" );
        sr.setResultObject( new FooBar( null ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetResultObjectWithDifferentId() {
        SearchResult<Identifiable> sr = new SearchResult<>( FooBar.class, 1L, "test object" );
        sr.setResultObject( new FooBar( 2L ) );
    }

}