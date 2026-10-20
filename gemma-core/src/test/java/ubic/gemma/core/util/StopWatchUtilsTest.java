package ubic.gemma.core.util;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.profiling.StopWatchUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StopWatchUtilsTest {

    @Test
    public void testMeasuredRegion() {
        StopWatch sw = StopWatch.create();
        try ( StopWatchUtils.StopWatchRegion region = StopWatchUtils.measuredRegion( sw ) ) {
            // nom nom
            assertThat( region.getStopWatch() ).isSameAs( sw );
        }
        assertThat( sw.isSuspended() ).isTrue();
    }

    @Test
    public void testRepeatedMeasuredRegion() {
        StopWatch sw = StopWatch.create();
        for ( int i = 0; i < 10; i++ ) {
            try ( StopWatchUtils.StopWatchRegion region = StopWatchUtils.measuredRegion( sw ) ) {
                // nom nom
                assertThat( region.getStopWatch() ).isSameAs( sw );
            }
            assertThat( sw.isSuspended() ).isTrue();
        }
    }

    @Test
    public void testMeasuredRegionWithInnerRegion() {
        StopWatch sw = StopWatch.create();
        assertThrows( IllegalStateException.class, () -> {
            try ( StopWatchUtils.StopWatchRegion region = StopWatchUtils.measuredRegion( sw ) ) {
                // nom nom
                assertThat( region.getStopWatch() ).isSameAs( sw );
                try ( StopWatchUtils.StopWatchRegion innerRegion = StopWatchUtils.measuredRegion( sw ) ) {
                    // nom nom
                }
            }
        } );
    }

    @Test
    public void testMeasuredRegionWhhenStopWatchIsAlreadyStarted() {
        StopWatch sw = StopWatch.createStarted();
        assertThrows( IllegalStateException.class, () -> StopWatchUtils.measuredRegion( sw ) );
    }

    @Test
    public void testMeasuredRegionWhhenStopWatchIsStopped() {
        StopWatch sw = StopWatch.createStarted();
        sw.stop();
        assertThrows( IllegalStateException.class, () -> StopWatchUtils.measuredRegion( sw ) );
    }
}
