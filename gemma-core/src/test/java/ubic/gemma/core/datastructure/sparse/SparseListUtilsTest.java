package ubic.gemma.core.datastructure.sparse;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SparseListUtilsTest {

    @Test
    public void testSparseRangeArray() {
        SparseListUtils.validateSparseRangeArray( Arrays.asList( "a", "b" ), new int[] { 0, 50 }, 100 );
        assertThatThrownBy( () -> SparseListUtils.validateSparseRangeArray( Arrays.asList( "a", "a" ), new int[] { 0, 50 }, 100 ) )
                .isInstanceOf( IllegalArgumentException.class );
        assertThat( SparseListUtils.getSparseRangeArrayElement( Arrays.asList( "a", "b" ), new int[] { 0, 50 }, 100, 0 ) )
                .isEqualTo( "a" );
    }
}