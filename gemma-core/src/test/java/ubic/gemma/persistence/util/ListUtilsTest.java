package ubic.gemma.persistence.util;

import org.junit.Test;
import ubic.gemma.core.util.ListUtils;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for extra utilities for {@link java.util.List}.
 * @author poirigui
 */
public class ListUtilsTest {

    @Test
    public void testIndexOfElements() {
        Map<Long, Integer> id2position = ListUtils.indexOfElements( Arrays.asList( 1L, 2L, 3L, 2L, 4L, 7L, 5L ) );
        assertThat( id2position.get( 1L ) ).isEqualTo( 0 );
        assertThat( id2position.get( 2L ) ).isEqualTo( 1 );
        assertThat( id2position.get( 3L ) ).isEqualTo( 2 );
        assertThat( id2position.get( 4L ) ).isEqualTo( 4 );
        assertThat( id2position.get( 5L ) ).isEqualTo( 6 );
        assertThat( id2position.get( 7L ) ).isEqualTo( 5 );
        assertThat( id2position ).doesNotContainKey( 6L );
    }

}