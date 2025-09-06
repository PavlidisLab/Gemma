package ubic.gemma.core.util;

import org.junit.Test;
import ubic.gemma.core.datastructure.sparse.SparseRangeArrayList;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SparseRangeArrayListTest {

    @Test
    public void test() {
        SparseRangeArrayList<String> arr = new SparseRangeArrayList<>( Arrays.asList( "a", "b" ), new int[] { 0, 25 }, 50 );
        assertThat( arr ).hasSize( 50 );
        assertThat( arr.storageSize() ).isEqualTo( 2 );
    }

    @Test
    public void testEmptyArray() {
        SparseRangeArrayList<Object> arr = new SparseRangeArrayList<>();
        assertThat( arr ).isEmpty();
        assertThat( arr.storageSize() ).isEqualTo( 0 );
        assertThatThrownBy( () -> arr.get( 0 ) )
                .isInstanceOf( IndexOutOfBoundsException.class );
    }

    @Test
    public void testFromCollection() {
        SparseRangeArrayList<String> arr = new SparseRangeArrayList<>( Arrays.asList( "A", "B", "B", "C", "C", "C", "A", "A" ) );
        assertThat( arr )
                .hasSize( 8 )
                .containsExactly( "A", "B", "B", "C", "C", "C", "A", "A" );
        assertThat( arr.storageSize() ).isEqualTo( 4 );
    }
}
