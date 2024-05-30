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

    @Override
    protected Runnable wrap( Runnable runnable ) {
        int taskId = batchTaskCounter.getAndIncrement();
        return () -> {
            try {
                runnable.run();
            } catch ( Exception e ) {
                addErrorObject( String.format( "Batch task #%d failed", taskId + 1 ), e );
                throw e;
            } finally {
                completedBatchTasks.incrementAndGet();
            }
        };
    }

    @Override
    protected <T> Callable<T> wrap( Callable<T> callable ) {
        int taskId = batchTaskCounter.getAndIncrement();
        return () -> {
            try {
                return callable.call();
            } catch ( Exception e ) {
                addErrorObject( String.format( "Batch task #%d failed", taskId + 1 ), e );
                throw e;
            } finally {
                completedBatchTasks.incrementAndGet();
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
        batchProcessingResults.add( new BatchProcessingResult( false, successObject, message, null ) );
    }

    /**
     * @see #addSuccessObject(Object, String)
     */
    void addSuccessObject( Object successObject ) {
        batchProcessingResults.add( new BatchProcessingResult( false, successObject, null, null ) );
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
        batchProcessingResults.add( new BatchProcessingResult( true, errorObject, message, throwable ) );
        hasErrorObjects = true;
    }

    /**
     * Add an error object without a cause stacktrace.
     *
     * @see #addErrorObject(Object, String)
     */
    void addErrorObject( @Nullable Object errorObject, String message ) {
        batchProcessingResults.add( new BatchProcessingResult( true, errorObject, message, null ) );
        hasErrorObjects = true;
    }

    /**
     * Add an error object based on an exception.
     *
     * @see #addErrorObject(Object, String, Throwable)
     */
    void addErrorObject( @Nullable Object errorObject, Exception exception ) {
        batchProcessingResults.add( new BatchProcessingResult( true, errorObject, exception.getMessage(), exception ) );
        hasErrorObjects = true;
    }

    /**
     * Indicate if error objects have been reported.
     */
    boolean hasErrorObjects() {
        return hasErrorObjects;
    }

    /**
     * Print out a summary of what the program did. Useful when analyzing lists of experiments etc. Use the
     * 'successObjects' and 'errorObjects'
     */
    void summarizeBatchProcessing( BatchFormat batchFormat, @Nullable File batchOutputFile ) throws IOException {
        if ( batchProcessingResults.isEmpty() ) {
            return;
        }
        try ( Writer dest = batchOutputFile != null ? new OutputStreamWriter( Files.newOutputStream( batchOutputFile.toPath() ) ) : null ) {
            switch ( batchFormat ) {
                case TEXT:
                    summarizeBatchProcessingToText( dest != null ? dest : System.out );
                    break;
                case TSV:
                    summarizeBatchProcessingToTsv( dest != null ? dest : System.out );
                    break;
                case SUPPRESS:
                    break;
            }
        }
    }

    private void summarizeBatchProcessingToText( Appendable dest ) throws IOException {
        List<BatchProcessingResult> successObjects = batchProcessingResults.stream().filter( bp -> !bp.isError() ).collect( Collectors.toList() );
        if ( !successObjects.isEmpty() ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\nSuccessfully processed " ).append( successObjects.size() )
                    .append( " objects:\n" );
            for ( BatchProcessingResult result : successObjects ) {
                buf.append( result ).append( "\n" );
            }
            buf.append( "---------------------\n" );
            dest.append( buf );
        }

        List<BatchProcessingResult> errorObjects = batchProcessingResults.stream().filter( BatchProcessingResult::isError ).collect( Collectors.toList() );
        if ( !errorObjects.isEmpty() ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\nErrors occurred during the processing of " )
                    .append( errorObjects.size() ).append( " objects:\n" );
            for ( BatchProcessingResult result : errorObjects ) {
                buf.append( result ).append( "\n" );
            }
            buf.append( "---------------------\n" );
            dest.append( buf );
        }
    }

    private void summarizeBatchProcessingToTsv( Appendable dest ) throws IOException {
        try ( CSVPrinter printer = new CSVPrinter( dest, CSVFormat.TDF ) ) {
            for ( BatchProcessingResult result : batchProcessingResults ) {
                printer.printRecord(
                        result.getSource(),
                        result.isError() ? "ERROR" : "SUCCESS",
                        result.getMessage(),
                        result.throwable != null ? ExceptionUtils.getRootCauseMessage( result.throwable ) : null );
            }
        }
    }

    enum BatchFormat {
        TEXT,
        TSV,
        SUPPRESS
    }

    /**
     * Represents an individual result in a batch processing.
     */
    @Value
    private static class BatchProcessingResult {
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
