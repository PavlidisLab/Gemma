package ubic.gemma.core.util;

/**
 * Report progress.
 *
 * @author poirigui
 */
public interface ProgressReporter extends AutoCloseable {

    /**
     * Set the minimum progress increment size to report in percentage.
     * <p>
     * This is used if maxSizeInBytes is set.
     */
    void setProgressIncrementToReportInPercent( double progressIncrementToReportInPercent );

    /**
     * Set the minimum progress increment size to report in bytes.
     * <p>
     * This is used if maxSizeInBytes is unknown.
     */
    void setProgressIncrementToReportInBytes( double progressIncrementToReportInBytes );

    /**
     * Report progress.
     * <p>
     * This method assumes that if progressInBytes and maxSizeInBytes are equal, this is the last report.
     */
    void reportProgress( long progressInBytes, long maxSizeInBytes );

    /**
     * Report progress.
     *
     * @param progressInBytes progress in bytes
     * @param maxSizeInBytes  maximum size in bytes if known, else -1
     * @param atEnd           indicate that this is the last progress report
     */
    void reportProgress( long progressInBytes, long maxSizeInBytes, boolean atEnd );

    /**
     * Clear any progress that was rendered.
     */
    @Override
    void close();
}
