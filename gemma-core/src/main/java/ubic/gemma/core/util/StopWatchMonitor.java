package ubic.gemma.core.util;

import org.apache.commons.lang3.time.StopWatch;

/**
 * Start or resume a {@link StopWatch} with a try-with-resource statement.
 *
 * This is really handy to measure code regions and report time usage.
 *
 * @author poirigui
 */
public class StopWatchMonitor implements AutoCloseable {

    private final StopWatch stopWatch;

    public StopWatchMonitor( StopWatch stopWatch ) {
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
