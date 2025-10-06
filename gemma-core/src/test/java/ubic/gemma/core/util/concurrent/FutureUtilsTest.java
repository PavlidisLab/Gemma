package ubic.gemma.core.util.concurrent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FutureUtilsTest {

    @Test
    public void testParallelMap() {
        assertThatThrownBy( () -> FutureUtils.parallelMapRange( i -> {
            if ( i == 5 ) {
                throw new RuntimeException( "Failed at 5." );
            } else {
                return i;
            }
        }, 0, 10, Executors.newSingleThreadExecutor(), true ) )
                .isInstanceOf( RuntimeException.class )
                .hasMessage( "Failed at 5." );
    }

    @Test
    public void testParallelMapRange() {
        assertThat( FutureUtils.parallelMapRange( i -> i, 0, 10, Executors.newSingleThreadExecutor(), true ) )
                .hasSize( 10 );
    }
}