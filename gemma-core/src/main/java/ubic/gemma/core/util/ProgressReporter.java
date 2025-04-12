package ubic.gemma.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * Report progress.
 * @author poirigui
 */
public class ProgressReporter {

    private final Object what;
    private final Log logger;

    private double progressIncrementToReportInPercent = 0.05;
    private double progressIncrementToReportInBytes = 1e6; // every MB

    private final long startTimeNanos;

    private double lastReportedProgressInPercent = 0.0;
    private long lastReportedProgressInBytes = 0;

    public ProgressReporter( Object what, String logCategory ) {
        this.what = what;
        this.logger = LogFactory.getLog( logCategory );
        this.startTimeNanos = System.nanoTime();
    }

    /**
     * Set the minimum progress increment size to report in percentage.
     * <p>
     * This is used if maxSizeInBytes is set.
     */
    public void setProgressIncrementToReportInPercent( double progressIncrementToReportInPercent ) {
        Assert.isTrue( progressIncrementToReportInBytes >= 0, "Progress increment must be zero or greater." );
        this.progressIncrementToReportInPercent = progressIncrementToReportInPercent;
    }

    /**
     * Set the minimum progress increment size to report in bytes.
     * <p>
     * This is used if maxSizeInBytes is unknown.
     */
    public void setProgressIncrementToReportInBytes( double progressIncrementToReportInBytes ) {
        Assert.isTrue( progressIncrementToReportInBytes >= 0, "Progress increment must be zero or greater." );
        this.progressIncrementToReportInBytes = progressIncrementToReportInBytes;
    }

    /**
     * Report progress.
     * <p>
     * This method assumes that if progressInBytes and maxSizeInBytes are equal, this is the last report.
     */
    public void reportProgress( long progressInBytes, long maxSizeInBytes ) {
        reportProgress( progressInBytes, maxSizeInBytes, progressInBytes == maxSizeInBytes );
    }

    /**
     * Report progress.
     * @param progressInBytes progress in bytes
     * @param maxSizeInBytes  maximum size in bytes if known, else -1
     * @param atEnd           indicate that this is the last progress report
     */
    public void reportProgress( long progressInBytes, long maxSizeInBytes, boolean atEnd ) {
        Assert.isTrue( progressInBytes >= 0, "Progress increment must be zero or greater." );
        if ( maxSizeInBytes > 0 ) {
            double progressInPercent = ( double ) progressInBytes / ( double ) maxSizeInBytes;
            if ( atEnd || progressInPercent - lastReportedProgressInPercent > progressIncrementToReportInPercent ) {
                logger.info( String.format( "%s %.2f%% [%d/%d] @ %s/s",
                        what,
                        100 * progressInPercent,
                        progressInBytes, maxSizeInBytes,
                        byteCountToDisplaySize( 1e9 * progressInBytes / ( System.nanoTime() - startTimeNanos ) ) ) );
                lastReportedProgressInPercent = progressInPercent;
                lastReportedProgressInBytes = progressInBytes;
            }
        } else {
            if ( atEnd || progressInBytes - lastReportedProgressInBytes > progressIncrementToReportInBytes ) {
                logger.info( String.format( "%s [%d/?] @ %s/s",
                        what,
                        progressInBytes,
                        byteCountToDisplaySize( 1e9 * progressInBytes / ( System.nanoTime() - startTimeNanos ) ) ) );
                lastReportedProgressInPercent = 0.0;
                lastReportedProgressInBytes = progressInBytes;
            }
        }
    }
}
