package ubic.gemma.cli.batch;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import ubic.gemma.cli.util.AnsiEscapeCodes;

import javax.annotation.Nullable;
import java.io.Console;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Report progress on batch tasks.
 * @author poirigui
 */
@CommonsLog
public class BatchTaskProgressReporter implements AutoCloseable {

    private final BatchTaskSummaryWriter summaryWriter;
    private final Console console;

    private final AtomicInteger numberOfSuccessOrErrorObjects = new AtomicInteger( 0 );
    private volatile boolean hasErrorObjects = false;

    private final ThreadLocal<Boolean> wasSuccessObjectAdded = ThreadLocal.withInitial( () -> false );
    private final ThreadLocal<Boolean> wasErrorObjectAdded = ThreadLocal.withInitial( () -> false );

    private final StopWatch timer = StopWatch.createStarted();
    private int lastCompletedTasks = 0;
    private int estimatedMaxTasks = -1;
    private int reportFrequencyMillis = 30000;

    /**
     *
     * @param summaryWriter      a writer to report batch task results, results are emitted as they are added and
     *                           {@link BatchTaskSummaryWriter#close()} is invoked when the reporter is closed to
     *                           provide an opportunity to write a summary
     * @param console a console capable of receiving ANSI escape codes
     */
    public BatchTaskProgressReporter( BatchTaskSummaryWriter summaryWriter, @Nullable Console console ) {
        this.summaryWriter = summaryWriter;
        this.console = console;
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
    public void setReportFrequencyMillis( int reportFrequencyMillis ) {
        Assert.isTrue( reportFrequencyMillis >= 0, "Report frequency must be non-negative." );
        this.reportFrequencyMillis = reportFrequencyMillis;
    }

    /**
     * Indicate if error objects have been reported.
     */
    public boolean hasErrorObjects() {
        return hasErrorObjects;
    }

    /**
     * Obtain the number of completed tasks (either with a success or failure result).
     */
    public int getCompletedTasks() {
        return numberOfSuccessOrErrorObjects.get();
    }

    /**
     * Set the maximum number of tasks that are expected to be run.
     */
    public void setEstimatedMaxTasks( int estimatedMaxTasks ) {
        this.estimatedMaxTasks = estimatedMaxTasks;
    }

    /**
     * Add a success object to indicate success in a batch processing.
     *
     * @param successObject object that was processed
     * @param message       success message
     */
    public void addSuccessObject( Serializable successObject, String message ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.SUCCESS, successObject, message, null ) );
    }

    /**
     * @see #addSuccessObject(Serializable, String)
     */
    public void addSuccessObject( Serializable successObject ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.SUCCESS, successObject, null, null ) );
    }

    public void addWarningObject( @Nullable Serializable warningObject, String message ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.WARNING, warningObject, message, null ) );
    }

    public void addWarningObject( @Nullable Serializable warningObject, String message, Throwable throwable ) {
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
    public void addErrorObject( @Nullable Serializable errorObject, String message, Throwable throwable ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.ERROR, errorObject, message, throwable ) );
    }

    /**
     * Add an error object without a cause stacktrace.
     *
     * @see #addErrorObject(Serializable, String)
     */
    public void addErrorObject( @Nullable Serializable errorObject, String message ) {
        addBatchProcessingResult( new BatchTaskProcessingResult( BatchTaskProcessingResult.ResultType.ERROR, errorObject, message, null ) );
    }

    /**
     * Add an error object based on an exception.
     *
     * @see #addErrorObject(Serializable, String, Throwable)
     */
    public void addErrorObject( @Nullable Serializable errorObject, Exception exception ) {
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

    public void reportProgress() {
        reportProgress( numberOfSuccessOrErrorObjects.get() );
    }

    private void reportProgress( int completedTasks ) {
        Assert.state( completedTasks >= lastCompletedTasks );
        if ( completedTasks > lastCompletedTasks ) {
            if ( timer.getTime() > reportFrequencyMillis ) {
                reportProgressToLogger( completedTasks );
                timer.reset();
                timer.start();
                lastCompletedTasks = completedTasks;
            }
            reportProgressToConsole( completedTasks );
        }
    }

    private void reportProgressToLogger( int completedTasks ) {
        if ( estimatedMaxTasks > 0 ) {
            log.info( String.format( "Completed %d/%d batch tasks.", completedTasks, estimatedMaxTasks ) );
        } else {
            log.info( String.format( "Completed %d batch tasks.", completedTasks ) );
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
        clearProgressIndicatorInConsole();
    }

    /**
     * Report progress to console using ANSI escape sequences.
     */
    private void reportProgressToConsole( int completedTasks ) {
        if ( console == null ) {
            return;
        }
        if ( estimatedMaxTasks > 0 && completedTasks <= estimatedMaxTasks ) {
            console.printf( AnsiEscapeCodes.progress( Math.round( 100.0f * completedTasks / estimatedMaxTasks ) ) );
        } else {
            // indeterminate if there is no max task estimate or if we are over the max
            console.printf( AnsiEscapeCodes.indeterminateProgress() );
        }
    }

    private void clearProgressIndicatorInConsole() {
        if ( console == null ) {
            return;
        }
        console.printf( AnsiEscapeCodes.clearProgress() );
    }
}