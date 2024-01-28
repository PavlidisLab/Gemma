/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.util;

import lombok.Value;
import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.DateUtil;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Basic implementation of the {@link CLI} interface.
 * <p>
 * To use this, in your concrete subclass, implement {@link #buildOptions} and {@link #processOptions} to handle any
 * application-specific options (they can be no-ops) and {@link #doWork()} to perform the actual work of the CLI.
 * <p>
 * Use {@link AbstractAuthenticatedCLI} if you need to authenticate the user.
 * @author pavlidis
 */
public abstract class AbstractCLI implements CLI {

    protected static final Log log = LogFactory.getLog( AbstractCLI.class );

    /**
     * Exit code used for a successful {@link #doWork} execution.
     */
    protected static final int SUCCESS = 0;
    /**
     * Exit code used for a failed {@link #doWork} execution.
     */
    protected static final int FAILURE = 1;
    /**
     * Exit code used for a successful doWork execution that resulted in failed error objects.
     */
    protected static final int FAILURE_FROM_ERROR_OBJECTS = 1;
    /**
     * Exit code used when a {@link #doWork()} is aborted.
     */
    protected static final int ABORTED = 2;

    public static final String HEADER = "Options:";
    public static final String FOOTER = "The Gemma project, Copyright (c) 2007-2021 University of British Columbia.";

    private static final String AUTO_OPTION_NAME = "auto";
    private static final String THREADS_OPTION = "threads";
    private static final String HELP_OPTION = "h";
    private static final String DATE_OPTION = "mdate";
    private static final String BATCH_FORMAT_OPTION = "batchFormat";
    private static final String BATCH_OUTPUT_FILE_OPTION = "batchOutputFile";

    @Autowired
    private BeanFactory ctx;

    /**
     * Indicate if this CLI allows positional arguments.
     */
    private boolean allowPositionalArguments = false;

    /* support for convenience options */
    /**
     * Automatically identify which entities to run the tool on. To enable call addAutoOption.
     */
    private boolean autoSeek;
    /**
     * The event type to look for the lack of, when using auto-seek.
     */
    private Class<? extends AuditEventType> autoSeekEventType;
    /**
     * Date used to identify which entities to run the tool on (e.g., those which were run less recently than mDate). To
     * enable call addDateOption.
     */
    private String mDate;
    /**
     * Number of threads to use for batch processing.
     */
    private int numThreads;
    /**
     * Format to use to summarize batch processing.
     */
    private BatchFormat batchFormat;
    /**
     * Destination for batch processing summary.
     */
    @Nullable
    private File batchOutputFile;
    private ExecutorService executorService;

    /**
     * Hold the results of the command execution
     * needs to be concurrently modifiable and kept in-order
     */
    private final List<BatchProcessingResult> batchProcessingResults = Collections.synchronizedList( new ArrayList<>() );

    /**
     * Convenience method to obtain instance of any bean by name.
     *
     * @deprecated Use {@link Autowired} to specify your dependencies, this is just a wrapper around the current
     * {@link BeanFactory}.
     *
     * @param <T>  the bean class type
     * @param clz  class
     * @param name name
     * @return bean
     */
    @SuppressWarnings("SameParameterValue") // Better for general use
    @Deprecated
    protected <T> T getBean( String name, Class<T> clz ) {
        assert ctx != null : "Spring context was not initialized";
        return ctx.getBean( name, clz );
    }

    @Deprecated
    protected <T> T getBean( Class<T> clz ) {
        assert ctx != null : "Spring context was not initialized";
        return ctx.getBean( clz );
    }

    @Override
    public Options getOptions() {
        Options options = new Options();
        buildStandardOptions( options );
        buildOptions( options );
        return options;
    }

    @Override
    public boolean allowPositionalArguments() {
        return allowPositionalArguments;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Parse and process CLI arguments, invoke the command {@link #doWork()} implementation, and print basic statistics
     * about time usage.
     * <p>
     * Any exception raised by doWork results in a value of {@link #FAILURE}, and any error set in the internal error
     * objects will result in a value of {@link #FAILURE_FROM_ERROR_OBJECTS}.
     */
    @Override
    public int executeCommand( String... args ) {
        Options options = getOptions();
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse( options, args );
            // check if -h/--help is provided before pursuing option processing
            if ( commandLine.hasOption( HELP_OPTION ) ) {
                printHelp( options, new PrintWriter( System.out, true ) );
                return SUCCESS;
            }
            if ( !allowPositionalArguments && !commandLine.getArgList().isEmpty() ) {
                throw new UnrecognizedOptionException( "The command line does not allow positional arguments." );
            }
            processStandardOptions( commandLine );
            processOptions( commandLine );
        } catch ( ParseException e ) {
            if ( e instanceof MissingOptionException ) {
                // check if -h/--help was passed alongside a required argument
                if ( ArrayUtils.contains( args, "-h" ) || ArrayUtils.contains( args, "--help" ) ) {
                    printHelp( options, new PrintWriter( System.out, true ) );
                    return SUCCESS;
                }
                System.err.println( "Required option(s) were not supplied: " + e.getMessage() );
            } else if ( e instanceof AlreadySelectedException ) {
                System.err.println( "The option(s) " + e.getMessage() + " were already selected" );
            } else if ( e instanceof MissingArgumentException ) {
                System.err.println( "Missing argument: " + e.getMessage() );
            } else if ( e instanceof UnrecognizedOptionException ) {
                System.err.println( "Unrecognized option: " + e.getMessage() );
            } else {
                System.err.println( e.getMessage() );
            }
            printHelp( options, new PrintWriter( System.err, true ) );
            return FAILURE;
        }
        StopWatch watch = StopWatch.createStarted();
        try {
            doWork();
            return batchProcessingResults.stream().noneMatch( BatchProcessingResult::isError ) ? SUCCESS : FAILURE_FROM_ERROR_OBJECTS;
        } catch ( WorkAbortedException e ) {
            log.warn( "Operation was aborted by the current user." );
            return ABORTED;
        } catch ( Exception e ) {
            log.error( String.format( "%s failed: %s", getCommandName(), ExceptionUtils.getRootCauseMessage( e ) ), e );
            return FAILURE;
        } finally {
            // always summarize processing, even if an error is thrown
            summarizeBatchProcessing();
            log.info( String.format( "Elapsed time: %d seconds.", watch.getTime( TimeUnit.SECONDS ) ) );
        }
    }

    private void printHelp( Options options, PrintWriter writer ) {
        new HelpFormatter().printHelp( writer, 150,
                this.getCommandName() + " [options]",
                this.getShortDesc() + "\n" + AbstractCLI.HEADER,
                options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, AbstractCLI.FOOTER );
    }

    /**
     * Add the {@code -auto} option.
     * <p>
     * The auto option value can be retrieved with {@link #isAutoSeek()}.
     */
    protected void addAutoOption( Options options ) {
        options.addOption( Option.builder( AUTO_OPTION_NAME )
                .desc( "Attempt to process entities that need processing based on workflow criteria." )
                .build() );
    }

    /**
     * Add the {@code -auto} option for a specific {@link AuditEventType}.
     * <p>
     * The event type can be retrieved with {@link #getAutoSeekEventType()}.
     */
    protected void addAutoOption( Options options, Class<? extends AuditEventType> autoSeekEventType ) {
        addAutoOption( options );
        this.autoSeekEventType = autoSeekEventType;
    }

    /**
     * Add the {@code -mdate} option.
     * <p>
     * The limiting date can be retrieved with {@link #getLimitingDate()}.
     */
    protected void addDateOption( Options options ) {
        options.addOption( Option.builder( DATE_OPTION ).hasArg()
                .desc( "Constrain to run only on entities with analyses older than the given date. "
                        + "For example, to run only on entities that have not been analyzed in the last 10 days, use '-10d'. "
                        + "If there is no record of when the analysis was last run, it will be run." )
                .build() );
    }

    /**
     * Add the {@code -threads} option.
     * <p>
     * This is used to configure the internal batch processing thread pool which can be used with
     * {@link #executeBatchTasks(Collection)}. You may also use {@link #getNumThreads()} to retrieve the number of
     * threads to use.
     */
    protected void addThreadsOption( Options options ) {
        options.addOption( Option.builder( THREADS_OPTION ).argName( "numThreads" ).hasArg()
                .desc( "Number of threads to use for batch processing." )
                .type( Integer.class )
                .build() );
    }

    /**
     * Allow positional arguments to be specified. The default is false and an error will be produced if positional
     * arguments are supplied by the user.
     * <p>
     * Those arguments can be retrieved in {@link #processOptions(CommandLine)} by using {@link CommandLine#getArgList()}.
     */
    @SuppressWarnings("unused")
    protected void setAllowPositionalArguments( boolean allowPositionalArguments ) {
        this.allowPositionalArguments = allowPositionalArguments;
    }

    protected boolean isAutoSeek() {
        return autoSeek;
    }

    protected Class<? extends AuditEventType> getAutoSeekEventType() {
        return autoSeekEventType;
    }

    protected int getNumThreads() {
        return numThreads;
    }

    protected Date getLimitingDate() {
        Date skipIfLastRunLaterThan = null;
        if ( StringUtils.isNotBlank( mDate ) ) {
            skipIfLastRunLaterThan = DateUtil.getRelativeDate( new Date(), mDate );
            AbstractCLI.log.info( "Analyses will be run only if last was older than " + skipIfLastRunLaterThan );
        }
        return skipIfLastRunLaterThan;
    }

    protected void buildStandardOptions( Options options ) {
        AbstractCLI.log.debug( "Creating standard options" );
        options.addOption( HELP_OPTION, "help", false, "Print this message" );
        options.addOption( BATCH_FORMAT_OPTION, true, "Format to use to the batch summary" );
        options.addOption( Option.builder( BATCH_OUTPUT_FILE_OPTION ).hasArg().type( File.class ).desc( "Output file to use for the batch summary (default is standard output)" ).build() );
    }

    /**
     * Build option implementation.
     * <p>
     * Implement this method to add options to your command line, using the OptionBuilder.
     * <p>
     * This is called right after {@link #buildStandardOptions(Options)} so the options will be added after standard options.
     */
    protected abstract void buildOptions( Options options );

    /**
     * Somewhat annoying: This causes subclasses to be unable to safely use 'h', 'p', 'u' and 'P' etc. for their own
     * purposes.
     */
    protected void processStandardOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( DATE_OPTION ) ^ commandLine.hasOption( AbstractCLI.AUTO_OPTION_NAME ) ) {
            throw new IllegalArgumentException( String.format( "Please only select one of -%s or -%s", DATE_OPTION, AUTO_OPTION_NAME ) );
        }

        if ( commandLine.hasOption( DATE_OPTION ) ) {
            this.mDate = commandLine.getOptionValue( DATE_OPTION );
        }

        this.autoSeek = commandLine.hasOption( AbstractCLI.AUTO_OPTION_NAME );

        if ( commandLine.hasOption( THREADS_OPTION ) ) {
            this.numThreads = ( Integer ) commandLine.getParsedOptionValue( THREADS_OPTION );
            if ( this.numThreads < 1 ) {
                throw new IllegalArgumentException( "Number of threads must be greater than 1." );
            }
        } else {
            this.numThreads = 1;
        }

        if ( this.numThreads > 1 ) {
            this.executorService = Executors.newFixedThreadPool( this.numThreads );
        } else {
            this.executorService = Executors.newSingleThreadExecutor();
        }

        if ( commandLine.hasOption( BATCH_FORMAT_OPTION ) ) {
            try {
                this.batchFormat = BatchFormat.valueOf( commandLine.getOptionValue( BATCH_FORMAT_OPTION ).toUpperCase() );
            } catch ( IllegalArgumentException e ) {
                throw new ParseException( String.format( "Unsupported batch format: %s.", commandLine.getOptionValue( BATCH_FORMAT_OPTION ) ) );
            }
        } else {
            this.batchFormat = commandLine.hasOption( BATCH_OUTPUT_FILE_OPTION ) ? BatchFormat.TSV : BatchFormat.TEXT;
        }
        this.batchOutputFile = ( File ) commandLine.getParsedOptionValue( BATCH_OUTPUT_FILE_OPTION );
    }

    /**
     * Process command line options.
     * <p>
     * Implement this to provide processing of options. It is called after {@link #buildOptions(Options)} and right before
     * {@link #doWork()}.
     * @throws ParseException in case of unrecoverable failure (i.e. missing option or invalid value), an exception can
     *                        be raised and will result in an exit code of {@link #FAILURE}.
     */
    protected abstract void processOptions( CommandLine commandLine ) throws ParseException;

    /**
     * Command line implementation.
     * <p>
     * This is called after {@link #buildOptions(Options)} and {@link #processOptions(CommandLine)}, so the
     * implementation can assume that all its arguments have already been initialized.
     *
     * @throws Exception            in case of unrecoverable failure, an exception is thrown and will result in a
     *                              {@link #FAILURE} exit code, otherwise use {@link #addErrorObject} to indicate an
     *                              error and resume processing
     */
    protected abstract void doWork() throws Exception;

    /**
     * Prompt the user for a confirmation or raise an exception to abort the {@link #doWork()} method.
     */
    protected void promptConfirmationOrAbort( String message ) throws Exception {
        if ( System.console() == null ) {
            throw new IllegalStateException( "A console must be available for prompting confirmation." );
        }
        String line = System.console().readLine( "WARNING: %s\nWARNING: Enter YES to continue: ",
                message.replaceAll( "\n", "\nWARNING: " ) );
        if ( "YES".equals( line.trim() ) ) {
            return;
        }
        throw new WorkAbortedException( "Confirmation failed, the command cannot proceed." );
    }

    /**
     * Add a success object to indicate success in a batch processing.
     * <p>
     * This is further used in {@link #summarizeBatchProcessing()} to summarize the execution of the command.
     *
     * @param successObject object that was processed
     * @param message       success message
     */
    protected void addSuccessObject( Object successObject, String message ) {
        batchProcessingResults.add( new BatchProcessingResult( false, successObject, message, null ) );
    }

    /**
     * @see #addSuccessObject(Object, String)
     */
    protected void addSuccessObject( Object successObject ) {
        batchProcessingResults.add( new BatchProcessingResult( false, successObject, null, null ) );
    }

    /**
     * Add an error object with a stacktrace to indicate failure in a batch processing.
     * <p>
     * This is further used in {@link #summarizeBatchProcessing()} to summarize the execution of the command.
     * <p>
     * This is intended to be used when an {@link Exception} is caught.
     *
     * @param errorObject object that was processed
     * @param message     error message
     * @param throwable   throwable to produce a stacktrace
     */
    protected void addErrorObject( @Nullable Object errorObject, String message, Throwable throwable ) {
        batchProcessingResults.add( new BatchProcessingResult( true, errorObject, message, throwable ) );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ) + ":\n\t" + message, throwable );
    }

    /**
     * Add an error object without a cause stacktrace.
     * @see #addErrorObject(Object, String)
     */
    protected void addErrorObject( @Nullable Object errorObject, String message ) {
        batchProcessingResults.add( new BatchProcessingResult( true, errorObject, message, null ) );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ) + ":\n\t" + message );
    }

    /**
     * Add an error object based on an exception.
     * @see #addErrorObject(Object, String, Throwable)
     */
    protected void addErrorObject( @Nullable Object errorObject, Exception exception ) {
        batchProcessingResults.add( new BatchProcessingResult( true, errorObject, exception.getMessage(), exception ) );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ), exception );
    }

    /**
     * Print out a summary of what the program did. Useful when analyzing lists of experiments etc. Use the
     * 'successObjects' and 'errorObjects'
     */
    private void summarizeBatchProcessing() {
        if ( batchProcessingResults.isEmpty() ) {
            return;
        }
        if ( batchFormat != BatchFormat.SUPPRESS && batchOutputFile != null ) {
            log.info( String.format( "Batch processing summary will be written to %s", batchOutputFile.getAbsolutePath() ) );
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
        } catch ( IOException e ) {
            log.error( "Failed to summarize batch processing.", e );
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

    /**
     * Execute batch tasks using a preconfigured {@link ExecutorService} and return all the resulting tasks results.
     *
     */
    protected <T> List<T> executeBatchTasks( Collection<? extends Callable<T>> tasks ) throws InterruptedException {
        List<Future<T>> futures = executorService.invokeAll( tasks );
        List<T> futureResults = new ArrayList<>( futures.size() );
        int i = 1;
        for ( Future<T> future : futures ) {
            try {
                futureResults.add( future.get() );
            } catch ( ExecutionException e ) {
                addErrorObject( null, String.format( "Batch task #%d failed", i ), e.getCause() );
            } finally {
                i++;
            }
        }
        return futureResults;
    }

    private enum BatchFormat {
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

    /**
     * Exception raised when a {@link #doWork()} aborted by the user.
     * @author poirigui
     */
    private static class WorkAbortedException extends Exception {

        private WorkAbortedException( String message ) {
            super( message );
        }
    }
}
