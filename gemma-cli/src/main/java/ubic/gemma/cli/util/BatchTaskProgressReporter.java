package ubic.gemma.cli.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Report progress on batch tasks.
 * @author poirigui
 */
@CommonsLog
class BatchTaskProgressReporter implements AutoCloseable {

    private final BatchTaskSummaryWriter summaryWriter;

    private final AtomicInteger numberOfSuccessOrErrorObjects = new AtomicInteger( 0 );
    private volatile boolean hasErrorObjects = false;

    private final ThreadLocal<Boolean> wasSuccessObjectAdded = ThreadLocal.withInitial( () -> false );
    private final ThreadLocal<Boolean> wasErrorObjectAdded = ThreadLocal.withInitial( () -> false );

    private final StopWatch timer = StopWatch.createStarted();
    private int lastCompletedTasks = 0;
    private int estimatedMaxTasks = -1;
    private int reportFrequencyMillis = 30000;

    BatchTaskProgressReporter( BatchTaskSummaryWriter summaryWriter ) {
        this.summaryWriter = summaryWriter;
    }

    /**
     * Indicate if a success object was added for the current thread.
     * <p>
     * This status is reset by {@link #clearThreadLocals()}.
     */
    boolean wasSuccessObjectAdded() {
        return wasSuccessObjectAdded.get();
    }

    /**
     * Indicate if an error object was added for the current thread.
     * <p>
     * This status is reset by {@link #clearThreadLocals()}.
     */
    boolean wasErrorObjectAdded() {
        return wasErrorObjectAdded.get();
    }

    /**
     * Clear thread local variables used to track success and error objects.
     */
    void clearThreadLocals() {
        wasErrorObjectAdded.remove();
        wasSuccessObjectAdded.remove();
    }

    /**
     * Set the frequency at which to report progress in milliseconds.
     * <p>
     * Repeated calls to {@link #reportProgress()} will be ignored until at least this amount of time has passed.
     * <p>
     * The default is to report task progress every 30 seconds.
     */
    void setReportFrequencyMillis( int reportFrequencyMillis ) {
        Assert.isTrue( reportFrequencyMillis >= 0, "Report frequency must be non-negative." );
        this.reportFrequencyMillis = reportFrequencyMillis;
    }

    /**
     * Indicate if error objects have been reported.
     */
    boolean hasErrorObjects() {
        return hasErrorObjects;
    }

    /**
     * Obtain the number of completed tasks (either with a success or failure result).
     */
    int getCompletedTasks() {
        return numberOfSuccessOrErrorObjects.get();
    }

    /**
     * Set the maximum number of tasks that are expected to be run.
     */
    void setEstimatedMaxTasks( int estimatedMaxTasks ) {
        this.estimatedMaxTasks = estimatedMaxTasks;
    }

    /**
     * Add a success object to indicate success in a batch processing.
     *
     * @param successObject object that was processed
     * @param message       success message
     */
    void addSuccessObject( Object successObject, String message ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.SUCCESS, successObject, message, null ) );
    }

    /**
     * @see #addSuccessObject(Object, String)
     */
    void addSuccessObject( Object successObject ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.SUCCESS, successObject, null, null ) );
    }

    void addWarningObject( @Nullable Object warningObject, String message ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.WARNING, warningObject, message, null ) );
    }

    void addWarningObject( @Nullable Object warningObject, String message, Throwable throwable ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.WARNING, warningObject, message, throwable ) );
    }

    /**
     * Add an error object with a stacktrace to indicate failure in a batch processing.
     * <p>
     * This is intended to be used when an {@link Exception} is caught.
     *
     * @param errorObject object that was processed
     * @param message     error message
     * @param throwable   throwable to produce a stacktrace
     */
    void addErrorObject( @Nullable Object errorObject, String message, Throwable throwable ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.ERROR, errorObject, message, throwable ) );
    }

    /**
     * Add an error object without a cause stacktrace.
     *
     * @see #addErrorObject(Object, String)
     */
    void addErrorObject( @Nullable Object errorObject, String message ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.ERROR, errorObject, message, null ) );
    }

    /**
     * Add an error object based on an exception.
     *
     * @see #addErrorObject(Object, String, Throwable)
     */
    void addErrorObject( @Nullable Object errorObject, Exception exception ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.ERROR, errorObject, exception.getMessage(), exception ) );
    }

    private void addBatchProcessingResult( BatchTaskProcessingResult result ) {
        int completed;
        if ( result.getResultType() == BatchTaskProcessingResult.ResultType.ERROR ) {
            wasErrorObjectAdded.set( true );
            hasErrorObjects = true;
            completed = numberOfSuccessOrErrorObjects.incrementAndGet();
        } else if ( result.getResultType() == BatchTaskProcessingResult.ResultType.SUCCESS ) {
            wasSuccessObjectAdded.set( true );
            completed = numberOfSuccessOrErrorObjects.incrementAndGet();
        } else {
            completed = numberOfSuccessOrErrorObjects.get();
        }
        try {
            synchronized ( summaryWriter ) {
                summaryWriter.write( result );
            }
        } catch ( IOException e ) {
            log.error( "Failed to write batch task result to the summary writer.", e );
        }
        reportProgress( completed );
    }

    void reportProgress() {
        reportProgress( numberOfSuccessOrErrorObjects.get() );
    }

    private void reportProgress( int completedTasks ) {
        if ( timer.getTime() > reportFrequencyMillis && completedTasks > lastCompletedTasks ) {
            if ( estimatedMaxTasks > 0 ) {
                log.info( String.format( "Completed %d/%d batch tasks.", completedTasks, estimatedMaxTasks ) );
            } else {
                log.info( String.format( "Completed %d batch tasks.", completedTasks ) );
            }
            timer.reset();
            timer.start();
            lastCompletedTasks = completedTasks;
        }
    }

    /**
     * Print out a summary of what the program did. Useful when analyzing lists of experiments etc. Use the
     * 'successObjects' and 'errorObjects'
     */
    @Override
    public void close() throws IOException {
        try {
            summaryWriter.close();
        } catch ( IOException e ) {
            log.error( "Failed to complete batch task summary.", e );
        }
    }
}
