package ubic.gemma.core.profiling;

import org.apache.commons.lang3.time.StopWatch;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Utilities for working with {@link StopWatch}.
 * @author poirigui
 */
public class StopWatchUtils {

    /**
     * Create a measured region by a {@link StopWatch}, which can be used with a try-with-resource statement to
     * {@link StopWatch#start()} and {@link StopWatch#suspend()} when entering and leaving the region.
     *
     * If the stop watch is suspended, let's say from a previous call to this function, it will be resumed, allowing for
     * a cumulative measurement of time usage.
     *
     * This is really handy to measure code regions and report time usage.
     *
     * @author poirigui
     */
    public static StopWatchRegion measuredRegion( StopWatch stopWatch ) {
        return new StopWatchRegion( stopWatch );
    }

    public static String getMinutesElapsed( StopWatch overallWatch ) {
        Long overallElapsed = overallWatch.getTime();
        NumberFormat nf = new DecimalFormat();
        nf.setMaximumFractionDigits( 2 );
        return nf.format( overallElapsed / ( 60.0 * 1000.0 ) );
    }

    public static class StopWatchRegion implements AutoCloseable {
        private final StopWatch stopWatch;

        private StopWatchRegion( StopWatch stopWatch ) {
            this.stopWatch = stopWatch;
            if ( this.stopWatch.isSuspended() ) {
                stopWatch.resume();
            } else {
                stopWatch.start();
            }
        }

        @Override
        public void close() {
            stopWatch.suspend();
        }

        public StopWatch getStopWatch() {
            return stopWatch;
        }
    }
}
