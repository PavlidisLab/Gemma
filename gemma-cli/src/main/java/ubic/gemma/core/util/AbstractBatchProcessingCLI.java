package ubic.gemma.core.util;

import lombok.Value;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import ubic.basecode.util.DateUtil;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;

/**
 * Base implementation for CLIs with batch processing capabili
 */
public abstract class AbstractBatchProcessingCLI extends AbstractCLI {

    private static final String
            DATE_OPTION = "mdate",
            AUTO_OPTION = "auto",
            THREADS_OPTION = "threads";

    /**
     * Date used to identify which entities to run the tool on (e.g., those which were run less recently than mDate). To
     * enable call addDateOption.
     */
    private String mDate = null;

    /**
     * Automatically identify which entities to run the tool on. To enable call addAutoOption.
     */
    private boolean autoSeek = false;
    /**
     * The event type to look for the lack of, when using auto-seek or null if undefined or implied.
     */
    @Nullable
    private Class<? extends AuditEventType> autoSeekEventType = null;

    private int numThreads = 1;

    @Nullable
    private ExecutorService executorService;

    // hold the results of the command execution
    // needs to be concurrently modifiable and kept in-order
    private final List<BatchProcessingResult> errorObjects = Collections.synchronizedList( new ArrayList<>() );
    private final List<BatchProcessingResult> successObjects = Collections.synchronizedList( new ArrayList<>() );

    @Override
    protected final void buildOptions( Options options ) {
        options.addOption( Option.builder( THREADS_OPTION ).argName( "numThreads" ).hasArg()
                .desc( "Number of threads to use for batch processing." )
                .build() );
        buildBatchOptions( options );
    }

    @Override
    protected final void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( DATE_OPTION ) ) {
            this.mDate = commandLine.getOptionValue( DATE_OPTION );
            if ( commandLine.hasOption( AUTO_OPTION ) ) {
                throw new IllegalArgumentException( "Please only select one of 'mdate' OR 'auto'" );
            }
        }
        if ( commandLine.hasOption( AUTO_OPTION ) ) {
            this.autoSeek = true;
            if ( commandLine.hasOption( DATE_OPTION ) ) {
                throw new IllegalArgumentException( "Please only select one of 'mdate' OR 'auto'" );
            }
        }
        if ( commandLine.hasOption( THREADS_OPTION ) ) {
            this.numThreads = Integer.parseInt( commandLine.getOptionValue( THREADS_OPTION ) );
        }
        if ( this.numThreads < 1 ) {
            throw new IllegalArgumentException( "Number of threads must be greater than 1." );
        }
        processBatchOptions( commandLine );
    }

    @Override
    protected final void doWork() throws Exception {
        ExecutorService executorService = this.executorService = new DelegatingSecurityContextExecutorService( Executors.newFixedThreadPool( this.numThreads ) );
        try {
            doBatchWork();
        } catch ( Exception e ) {
            if ( errorObjects.isEmpty() ) {
                throw e;
            } else {
                throw new BatchProcessingFailureException(
                        String.format( "The doBatchWork() implementation of %s failed. In addition, there were %d failures in the batch processing.",
                                getClass().getName(), errorObjects.size() ), e );
            }
        } finally {
            executorService.shutdown();
            try {
                if ( !executorService.isTerminated() ) {
                    do {
                        log.info( "Waiting for background tasks to complete..." );
                    } while ( !executorService.awaitTermination( 5, TimeUnit.MINUTES ) );
                }
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                log.error( "Thread was interrupted while waiting for background tasks to complete.", e );
            }
            if ( !executorService.isTerminated() ) {
                log.warn( "The background jobs pool is not fully terminated, the batch processing summary might be incomplete." );
            }
            // always summarize processing, even if an error is thrown
            summarizeProcessing();
        }

        // if doBatchWork raised no exception, there might
        if ( !errorObjects.isEmpty() ) {
            throw new BatchProcessingFailureException( String.format( "There were %d failures in the batch processing.", errorObjects.size() ) );
        }
    }

    /**
     * Build batch-specific options.
     */
    protected abstract void buildBatchOptions( Options options );

    /**
     * Process batch-specific options.
     */
    protected abstract void processBatchOptions( CommandLine commandLine ) throws ParseException;

    /**
     * Perform batch-specific work.
     */
    protected abstract void doBatchWork() throws Exception;

    /**
     * Obtain an executor service for running tasks in parallel.
     */
    protected final ExecutorService getExecutorService() {
        if ( executorService == null ) {
            throw new IllegalStateException( "The executor service has not yet been created, are you trying to access it outside of doBatchWork()?" );
        }
        return executorService;
    }

    protected final Date getLimitingDate() {
        Date skipIfLastRunLaterThan = null;
        if ( StringUtils.isNotBlank( mDate ) ) {
            skipIfLastRunLaterThan = DateUtil.getRelativeDate( new Date(), mDate );
            AbstractCLI.log.info( "Analyses will be run only if last was older than " + skipIfLastRunLaterThan );
        }
        return skipIfLastRunLaterThan;
    }

    protected final boolean isAutoSeek() {
        return autoSeek;
    }

    @Nullable
    protected final Class<? extends AuditEventType> getAutoSeekEventType() {
        return autoSeekEventType;
    }

    /**
     * You must implement the handling for this option.
     */
    protected final void addAutoOption( Options options, @Nullable Class<? extends
            AuditEventType> autoSeekEventType ) {
        Option autoSeekOption = Option.builder( AUTO_OPTION )
                .desc( "Attempt to process entities that need processing based on workflow criteria." )
                .build();
        options.addOption( autoSeekOption );
        this.autoSeekEventType = autoSeekEventType;
    }

    protected final void addDateOption( Options options ) {
        Option dateOption = Option.builder( DATE_OPTION ).hasArg().desc(
                        "Constrain to run only on entities with analyses older than the given date. "
                                + "For example, to run only on entities that have not been analyzed in the last 10 days, use '-10d'. "
                                + "If there is no record of when the analysis was last run, it will be run." )
                .build();

        options.addOption( dateOption );
    }


    /**
     * Add a success object to indicate success in a batch processing.
     * <p>
     * This is further used in {@link #summarizeProcessing()} to summarize the execution of the command.
     *
     * @param successObject object that was processed
     * @param message       success message
     */
    protected final void addSuccessObject( Object successObject, String message ) {
        successObjects.add( new BatchProcessingResult( successObject, message ) );
        log.info( successObject + ": " + message );
    }

    /**
     * Add an error object with a stacktrace to indicate failure in a batch processing.
     * <p>
     * This is further used in {@link #summarizeProcessing()} to summarize the execution of the command.
     * <p>
     * This is intended to be used when an {@link Exception} is caught.
     *
     * @param errorObject object that was processed
     * @param message     error message
     * @param throwable   throwable to produce a stacktrace
     */
    protected final void addErrorObject( Object errorObject, String message, Throwable throwable ) {
        errorObjects.add( new BatchProcessingResult( errorObject, message ) );
        log.error( errorObject + ": " + message, throwable );
    }

    /**
     * Add an error object to indicate failure in a batch processing.
     * <p>
     * This is further used in {@link #summarizeProcessing()} to summarize the execution of the command.
     */
    protected final void addErrorObject( Object errorObject, String message ) {
        errorObjects.add( new BatchProcessingResult( errorObject, message ) );
        log.error( errorObject + ": " + message );
    }

    /**
     * Print out a summary of what the program did. Useful when analyzing lists of experiments etc. Use the
     * 'successObjects' and 'errorObjects'
     */
    private void summarizeProcessing() {
        if ( successObjects.size() > 0 ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\nSuccessfully processed " ).append( successObjects.size() )
                    .append( " objects:\n" );
            for ( BatchProcessingResult result : successObjects ) {
                buf.append( "Success\t" )
                        .append( result.source ).append( ": " )
                        .append( result.message ).append( "\n" );
            }
            buf.append( "---------------------\n" );

            AbstractCLI.log.info( buf );
        }

        if ( errorObjects.size() > 0 ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\nErrors occurred during the processing of " )
                    .append( errorObjects.size() ).append( " objects:\n" );
            for ( BatchProcessingResult result : errorObjects ) {
                buf.append( "Error\t" )
                        .append( result.source ).append( ": " )
                        .append( result.message ).append( "\n" );
            }
            buf.append( "---------------------\n" );
            AbstractCLI.log.error( buf );
        }
    }

    /**
     * Represents an individual result in a batch processing.
     */
    @Value
    private static class BatchProcessingResult {
        Object source;
        String message;
        Throwable throwable;

        public BatchProcessingResult( Object source, String message ) {
            this.source = source;
            this.message = message;
            this.throwable = null;
        }
    }

    static final class BatchProcessingFailureException extends Exception {

        public BatchProcessingFailureException( String message ) {
            super( message );
        }

        public BatchProcessingFailureException( String message, Throwable cause ) {
            super( message, cause );
        }
    }
}
