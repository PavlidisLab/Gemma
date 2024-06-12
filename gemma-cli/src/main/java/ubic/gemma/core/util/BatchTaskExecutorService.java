package ubic.gemma.core.util;

import lombok.Value;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A task executor that automatically reports errors in batch tasks.
 */
@ParametersAreNonnullByDefault
class BatchTaskExecutorService extends AbstractDelegatingExecutorService {

    private final AtomicInteger batchTaskCounter = new AtomicInteger( 0 );
    private final AtomicInteger completedBatchTasks = new AtomicInteger( 0 );

    /**
     * Hold the results of the command execution
     * needs to be concurrently modifiable and kept in-order
     */
    private final List<BatchProcessingResult> batchProcessingResults = Collections.synchronizedList( new ArrayList<>() );
    private boolean hasErrorObjects = false;

    public BatchTaskExecutorService( ExecutorService delegate ) {
        super( delegate );
    }

    private final ThreadLocal<Boolean> wasSuccessObjectAdded = ThreadLocal.withInitial( () -> false );
    private final ThreadLocal<Boolean> wasErrorObjectAdded = ThreadLocal.withInitial( () -> false );

    @Override
    protected Runnable wrap( Runnable runnable ) {
        Object batchObject = String.format( "Batch task #%d", batchTaskCounter.incrementAndGet() );
        return () -> {
            try {
                runnable.run();
                if ( !wasSuccessObjectAdded.get() && !wasErrorObjectAdded.get() ) {
                    addSuccessObject( batchObject );
                }
            } catch ( Exception e ) {
                if ( !wasErrorObjectAdded.get() ) {
                    addErrorObject( batchObject, e );
                }
                throw e;
            } finally {
                completedBatchTasks.incrementAndGet();
                wasSuccessObjectAdded.remove();
                wasErrorObjectAdded.remove();
            }
        };
    }

    @Override
    protected <T> Callable<T> wrap( Callable<T> callable ) {
        Object batchObject = String.format( "Batch task #%d", batchTaskCounter.incrementAndGet() );
        return () -> {
            try {
                T result = callable.call();
                if ( !wasSuccessObjectAdded.get() && !wasErrorObjectAdded.get() ) {
                    addSuccessObject( batchObject );
                }
                return result;
            } catch ( Exception e ) {
                if ( !wasErrorObjectAdded.get() ) {
                    addErrorObject( batchObject, e );
                }
                throw e;
            } finally {
                completedBatchTasks.incrementAndGet();
                wasSuccessObjectAdded.remove();
                wasErrorObjectAdded.remove();
            }
        };
    }

    /**
     * Obtain the number of completed batch tasks.
     */
    int getCompletedTasks() {
        return completedBatchTasks.get();
    }

    /**
     * Obtain the number of submitted batch tasks.
     */
    int getSubmittedTasks() {
        return batchTaskCounter.get();
    }

    /**
     * Add a success object to indicate success in a batch processing.
     *
     * @param successObject object that was processed
     * @param message       success message
     */
    void addSuccessObject( Object successObject, String message ) {
        addBatchProcessingResult( new BatchProcessingResult( false, successObject, message, null ) );
    }

    /**
     * @see #addSuccessObject(Object, String)
     */
    void addSuccessObject( Object successObject ) {
        addBatchProcessingResult( new BatchProcessingResult( false, successObject, null, null ) );
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
        addBatchProcessingResult( new BatchProcessingResult( true, errorObject, message, throwable ) );
    }

    /**
     * Add an error object without a cause stacktrace.
     *
     * @see #addErrorObject(Object, String)
     */
    void addErrorObject( @Nullable Object errorObject, String message ) {
        addBatchProcessingResult( new BatchProcessingResult( true, errorObject, message, null ) );
    }

    /**
     * Add an error object based on an exception.
     *
     * @see #addErrorObject(Object, String, Throwable)
     */
    void addErrorObject( @Nullable Object errorObject, Exception exception ) {
        addBatchProcessingResult( new BatchProcessingResult( true, errorObject, exception.getMessage(), exception ) );
    }

    /**
     * Indicate if error objects have been reported.
     */
    boolean hasErrorObjects() {
        return hasErrorObjects;
    }

    List<BatchProcessingResult> getBatchProcessingResults() {
        return batchProcessingResults;
    }

    private void addBatchProcessingResult( BatchProcessingResult result ) {
        if ( result.isError() ) {
            wasErrorObjectAdded.set( true );
            hasErrorObjects = true;
        } else {
            wasSuccessObjectAdded.set( true );
        }
    }

    /**
     * Represents an individual result in a batch processing.
     */
    @Value
    static class BatchProcessingResult {
        boolean isError;
        @Nullable
        Object source;
        @Nullable
        String message;
        @Nullable
        Throwable throwable;

        public BatchProcessingResult( boolean isError, @Nullable Object source, @Nullable String message, @Nullable Throwable throwable ) {
            this.isError = isError;
            this.source = source;
            this.message = message;
            this.throwable = throwable;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append( source != null ? source : "Unknown object" );
            if ( message != null ) {
                buf.append( "\t" )
                        .append( message.replace( "\n", "\n\t" ) ); // FIXME We don't want newlines here at all, but I'm not sure what condition this is meant to fix exactly.
            }
            if ( throwable != null ) {
                buf.append( "\t" )
                        .append( "Reason: " )
                        .append( ExceptionUtils.getRootCauseMessage( throwable ) );
            }
            return buf.toString();
        }
    }
}
