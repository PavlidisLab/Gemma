package ubic.gemma.core.search;

import org.junit.Test;
import ubic.gemma.model.common.Identifiable;

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
        new SearchResult<Identifiable>( new FooBar( 1L ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResultObjectWithNullId() {
        new SearchResult<Identifiable>( new FooBar( null ) );
    }

    @Test
    public void testSetResultObject() {
        SearchResult<Identifiable> sr = new SearchResult<>( FooBar.class, 1L, 0, null );
        sr.setResultObject( new FooBar( 1L ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetResultObjectWithNullId() {
        SearchResult<Identifiable> sr = new SearchResult<>( FooBar.class, 1L, 0, null );
        sr.setResultObject( new FooBar( null ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetResultObjectWithDifferentId() {
        SearchResult<Identifiable> sr = new SearchResult<>( FooBar.class, 1L, 0, null );
        sr.setResultObject( new FooBar( 2L ) );
    }

}