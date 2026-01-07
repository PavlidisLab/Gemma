package ubic.gemma.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static ubic.gemma.core.util.NetUtils.bytePerSecondToDisplaySize;

/**
 * Report progress to a specific log category.
 *
 * @author poirigui
 */
public class LoggingProgressReporter extends AbstractProgressReporter implements ProgressReporter {

    private final Object what;
    private final Log logger;

    private final long startTimeNanos;

    public LoggingProgressReporter( Object what, String logCategory ) {
        this.what = what;
        this.logger = LogFactory.getLog( logCategory );
        this.startTimeNanos = System.nanoTime();
    }

    @Override
    protected void doReportProgress( double progressInPercent, long progressInBytes, long maxSizeInBytes, boolean atEnd ) {
        logger.info( String.format( "%s %.2f%% [%d/%d] @ %s",
                what,
                100 * progressInPercent,
                progressInBytes, maxSizeInBytes,
                bytePerSecondToDisplaySize( 1e9 * progressInBytes / ( System.nanoTime() - startTimeNanos ) ) ) );
    }

    @Override
    protected void doReportUnknownProgress( long progressInBytes, boolean atEnd ) {
        logger.info( String.format( "%s [%d/?] @ %s",
                what,
                progressInBytes,
                bytePerSecondToDisplaySize( 1e9 * progressInBytes / ( System.nanoTime() - startTimeNanos ) ) ) );
    }

    @Override
    public void close() {
        // nothing to do
    }
}
