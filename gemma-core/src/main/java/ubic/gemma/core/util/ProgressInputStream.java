package ubic.gemma.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

@ParametersAreNonnullByDefault
public class ProgressInputStream extends FilterInputStream {

    private final Object what;
    private final Log logger;
    private final long maxSizeInBytes;

    double progressIncrementToReportInPercent = 0.05;
    double progressIncrementToReportInBytes = 1e6; // every MB

    private long startTimeNanos = -1;
    private long progressInBytes = 0;
    private double lastReportedProgress = 0.0;

    /**
     * @param what           an object describing what is being processed, it will be used for logging purposes
     * @param logCategory    a log category to report progress to
     * @param maxSizeInBytes the maximum size in bytes
     */
    public ProgressInputStream( InputStream in, Object what, String logCategory, long maxSizeInBytes ) {
        super( in );
        this.what = what;
        this.logger = LogFactory.getLog( logCategory );
        this.maxSizeInBytes = maxSizeInBytes;
    }

    public ProgressInputStream( InputStream in, Object what, String logCategory ) {
        this( in, what, logCategory, -1 );
    }

    /**
     * Set the minimum progress increment size to report in percentage.
     * <p>
     * This is used if maxSizeInBytes is set.
     */
    public void setProgressIncrementToReportInPercent( double progressIncrementToReportInPercent ) {
        this.progressIncrementToReportInPercent = progressIncrementToReportInPercent;
    }

    /**
     * Set the minimum progress increment size to report in bytes.
     * <p>
     * This is used if maxSizeInBytes is unset.
     */
    public void setProgressIncrementToReportInBytes( double progressIncrementToReportInBytes ) {
        this.progressIncrementToReportInBytes = progressIncrementToReportInBytes;
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException {
        if ( startTimeNanos == -1 ) {
            startTimeNanos = System.nanoTime();
        }
        return recordProgress( super.read( b, off, len ), false );
    }

    @Override
    public int read() throws IOException {
        if ( startTimeNanos == -1 ) {
            startTimeNanos = System.nanoTime();
        }
        return recordProgress( super.read(), true );
    }

    @Override
    public long skip( long n ) throws IOException {
        if ( startTimeNanos == -1 ) {
            startTimeNanos = System.nanoTime();
        }
        return recordProgress( super.skip( n ), false );
    }

    private int recordProgress( int read, boolean oneByte ) {
        return ( int ) recordProgress( ( long ) read, oneByte );
    }

    private long recordProgress( long read, boolean oneByte ) {
        if ( read < 0 ) {
            return read;
        }
        progressInBytes += oneByte ? 1 : read;
        if ( maxSizeInBytes > 0 ) {
            double progress = ( double ) progressInBytes / ( double ) maxSizeInBytes;
            if ( progress - lastReportedProgress > progressIncrementToReportInPercent ) {
                logger.info( String.format( "%s %.2f%% [%d/%d] @ %s/s",
                        what,
                        100 * progress,
                        progressInBytes, maxSizeInBytes,
                        byteCountToDisplaySize( 1e9 * progressInBytes / ( System.nanoTime() - startTimeNanos ) ) ) );
                lastReportedProgress = progress;
            }
        } else {
            double progress = ( double ) progressInBytes;
            if ( progress - lastReportedProgress > progressIncrementToReportInBytes ) {
                logger.info( String.format( "%s [%d/?] @ %s/s",
                        what,
                        progressInBytes,
                        byteCountToDisplaySize( 1e9 * progressInBytes / ( System.nanoTime() - startTimeNanos ) ) ) );
                lastReportedProgress = progress;
            }
        }
        return read;
    }
}
