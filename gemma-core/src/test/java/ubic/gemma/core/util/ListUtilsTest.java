package ubic.gemma.core.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
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

    @Test
    public void testIndexOfCaseInsensitiveStringElements() {
        Map<String, Integer> str2position = ListUtils.indexOfCaseInsensitiveStringElements( Arrays.asList( "a", "A", "baba", "BABA", "C", "c" ) );
        assertThat( str2position.get( "a" ) ).isEqualTo( 0 );
        assertThat( str2position.get( "A" ) ).isEqualTo( 0 );
        assertThat( str2position.get( "baBa" ) ).isEqualTo( 2 );
    }
    @Test
    public void testPadToNextPowerOfTwo() {
        assertThat( ListUtils.padToNextPowerOfTwo( Collections.emptyList(), null ) ).hasSize( 0 );
        assertThat( ListUtils.padToNextPowerOfTwo( Arrays.asList( 1L, 2L, 3L ), null ) ).hasSize( 4 );
        assertThat( ListUtils.padToNextPowerOfTwo( Arrays.asList( 1L, 2L, 3L, 4L ), null ) ).hasSize( 4 );
        assertThat( ListUtils.padToNextPowerOfTwo( Arrays.asList( 1L, 2L, 3L, 4L, 5L ), null ) ).hasSize( 8 );
    }
}