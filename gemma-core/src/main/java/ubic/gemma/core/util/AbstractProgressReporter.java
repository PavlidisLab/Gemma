package ubic.gemma.core.util;

import org.springframework.util.Assert;

public abstract class AbstractProgressReporter implements ProgressReporter {

    private double progressIncrementToReportInPercent = 0.05;
    private double progressIncrementToReportInBytes = 1e6; // every MB

    private double lastReportedProgressInPercent = 0.0;
    private long lastReportedProgressInBytes = 0;

    @Override
    public void setProgressIncrementToReportInPercent( double progressIncrementToReportInPercent ) {
        Assert.isTrue( progressIncrementToReportInBytes >= 0, "Progress increment must be zero or greater." );
        this.progressIncrementToReportInPercent = progressIncrementToReportInPercent;
    }

    @Override
    public void setProgressIncrementToReportInBytes( double progressIncrementToReportInBytes ) {
        Assert.isTrue( progressIncrementToReportInBytes >= 0, "Progress increment must be zero or greater." );
        this.progressIncrementToReportInBytes = progressIncrementToReportInBytes;
    }

    @Override
    public void reportProgress( long progressInBytes, long maxSizeInBytes ) {
        reportProgress( progressInBytes, maxSizeInBytes, progressInBytes == maxSizeInBytes );
    }

    @Override
    public void reportProgress( long progressInBytes, long maxSizeInBytes, boolean atEnd ) {
        Assert.isTrue( progressInBytes >= 0, "Progress increment must be zero or greater." );
        if ( maxSizeInBytes > 0 ) {
            double progressInPercent = ( double ) progressInBytes / ( double ) maxSizeInBytes;
            if ( atEnd || progressInPercent - lastReportedProgressInPercent > progressIncrementToReportInPercent ) {
                doReportProgress( progressInPercent, progressInBytes, maxSizeInBytes );
                lastReportedProgressInPercent = progressInPercent;
                lastReportedProgressInBytes = progressInBytes;
            }
        } else {
            if ( atEnd || progressInBytes - lastReportedProgressInBytes > progressIncrementToReportInBytes ) {
                doReportUnknownProgress( progressInBytes );
                lastReportedProgressInPercent = 0.0;
                lastReportedProgressInBytes = progressInBytes;
            }
        }
    }

    protected abstract void doReportProgress( double progressInPercent, long progressInBytes, long maxSizeInBytes );

    protected abstract void doReportUnknownProgress( long progressInBytes );
}
