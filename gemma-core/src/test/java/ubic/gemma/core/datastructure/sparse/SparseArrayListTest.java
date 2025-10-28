package ubic.gemma.core.datastructure.sparse;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SparseArrayListTest {

    @Test
    public void test() {
        SparseArrayList<?> array = new SparseArrayList<>( Arrays.asList( "a", "b", "b" ), new int[] { 3, 7, 15 }, 17 );
        assertThat( array ).hasSize( 17 );
        assertThat( array.storageSize() ).isEqualTo( 3 );
        assertThat( array.indexOf( "b" ) )
                .isEqualTo( 7 );
        assertThat( array.lastIndexOf( "b" ) )
                .isEqualTo( 15 );
        assertThatThrownBy( () -> new SparseArrayList<>( Arrays.asList( "a", "b" ), new int[] { 3, 3 }, 10 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Indices must be strictly increasing." );
        assertThatThrownBy( () -> new SparseArrayList<>( Arrays.asList( "a", "b" ), new int[] { 7, 3 }, 10 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Indices must be strictly increasing." );
        assertThatThrownBy( () -> new SparseArrayList<>( Arrays.asList( "a", null ), new int[] { 3, 7 }, 10 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Array may not contain the default value." );
        assertThatThrownBy( () -> new SparseArrayList<>( Arrays.asList( "a", "b" ), new int[] { 3, 15 }, 10 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Indices must be in the [0, 10[ range." );
    }

    @Test
    public void testEmptyArray() {
        SparseArrayList<Object> arr = new SparseArrayList<>();
        assertThat( arr ).isEmpty();
        assertThat( arr.storageSize() ).isEqualTo( 0 );
        assertThatThrownBy( () -> arr.get( 0 ) )
                .isInstanceOf( IndexOutOfBoundsException.class );
    }

    @Test
    public void testFromCollection() {
        SparseArrayList<String> arr = new SparseArrayList<>( Arrays.asList( "A", "B", null, null, null, "C", null, "A" ) );
        assertThat( arr )
                .hasSize( 8 )
                .containsExactly( "A", "B", null, null, null, "C", null, "A" );
        assertThat( arr.storageSize() ).isEqualTo( 4 );
    }
}