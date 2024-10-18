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

import org.apache.commons.cli.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Basic implementation of the {@link CLI} interface.
 * <p>
 * To use this, in your concrete subclass, implement {@link #buildOptions} and {@link #processOptions} to handle any
 * application-specific options (they can be no-ops) and {@link #doWork()} to perform the actual work of the CLI.
 * <p>
 * Use {@link AbstractAuthenticatedCLI} if you need to authenticate the user.
 *
 * @author pavlidis
 */
public abstract class AbstractCLI implements CLI {

    protected final Log log = LogFactory.getLog( getClass() );

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

    private static final String THREADS_OPTION = "threads";
    private static final String HELP_OPTION = "h";

    private static final String BATCH_FORMAT_OPTION = "batchFormat";
    private static final String BATCH_OUTPUT_FILE_OPTION = "batchOutputFile";

    /**
     * Indicate if this CLI allows positional arguments.
     */
    private boolean allowPositionalArguments = false;

    /**
     * Number of threads to use for batch processing.
     */
    private int numThreads;
    /**
     * Format to use to summarize batch processing.
     */
    private BatchTaskExecutorService.BatchFormat batchFormat;
    /**
     * Destination for batch processing summary.
     */
    @Nullable
    private Path batchOutputFile;

    /**
     * Indicate if we are "inside" {@link #doWork()}.
     */
    private boolean insideDoWork = false;

    @Nullable
    private BatchTaskExecutorService executorService;

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
        try {
            work();
            awaitBatchExecutorService();
            return executorService != null && executorService.hasErrorObjects() ? FAILURE_FROM_ERROR_OBJECTS : SUCCESS;
        } catch ( Exception e ) {
            if ( executorService != null ) {
                List<Runnable> stillRunning = executorService.shutdownNow();
                if ( !stillRunning.isEmpty() ) {
                    log.warn( String.format( "%d batch tasks were still running, those were interrupted.", stillRunning.size() ) );
                }
            }
            if ( e instanceof WorkAbortedException ) {
                log.warn( "Operation was aborted by the current user." );
                return ABORTED;
            } else {
                log.error( String.format( "%s failed: %s", getCommandName(), ExceptionUtils.getRootCauseMessage( e ) ), e );
                return FAILURE;
            }
        } finally {
            if ( executorService != null ) {
                // always summarize processing, even if an error is thrown
                executorService.summarizeBatchProcessing();
                executorService = null;
            }
        }
    }

    /**
     * Describe the intended usage for the command.
     * <p>
     * This will be included in the 'Usage: ...' error message when the CLI is misused.
     */
    protected String getUsage() {
        return "gemma-cli [options] " + this.getCommandName() + " [commandOptions]" + ( allowPositionalArguments ? " [files]" : "" );
    }

    private void printHelp( Options options, PrintWriter writer ) {
        HelpUtils.printHelp( writer, getUsage(), options, getShortDesc(), null );
    }


    /**
     * Add the {@code -threads} option.
     * <p>
     * This is used to configure the internal batch processing thread pool which can be used with {@link #getBatchTaskExecutor()}.
     * <p>
     * You may also use {@link #getNumThreads()} to retrieve the number of threads to use.
     */
    protected void addThreadsOption( Options options ) {
        options.addOption( Option.builder( THREADS_OPTION )
                .longOpt( "threads" )
                .argName( "numThreads" ).hasArg()
                .desc( "Number of threads to use for batch processing." )
                .type( Number.class )
                .build() );
    }

    /**
     * Add the -batchFormat and -batchOutputFile options.
     * <p>
     * These options allow the user to control how and where batch processing results are summarized.
     */
    protected void addBatchOption( Options options ) {
        options.addOption( BATCH_FORMAT_OPTION, true, "Format to use to the batch summary" );
        options.addOption( Option.builder( BATCH_OUTPUT_FILE_OPTION ).hasArg().type( Path.class ).desc( "Output file to use for the batch summary (default is standard output)" ).build() );
    }

    /**
     * Allow positional arguments to be specified. The default is false and an error will be produced if positional
     * arguments are supplied by the user.
     * <p>
     * Those arguments can be retrieved in {@link #processOptions(CommandLine)} by using {@link CommandLine#getArgList()}.
     */
    protected void setAllowPositionalArguments( @SuppressWarnings("SameParameterValue") boolean allowPositionalArguments ) {
        this.allowPositionalArguments = allowPositionalArguments;
    }

    protected int getNumThreads() {
        return numThreads;
    }

    private void buildStandardOptions( Options options ) {
        options.addOption( HELP_OPTION, "help", false, "Print this message" );
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
    private void processStandardOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( THREADS_OPTION ) ) {
            this.numThreads = ( ( Number ) commandLine.getParsedOptionValue( THREADS_OPTION ) ).intValue();
            if ( this.numThreads < 1 ) {
                throw new IllegalArgumentException( "Number of threads must be greater than 1." );
            }
        } else {
            this.numThreads = 1;
        }

        if ( commandLine.hasOption( BATCH_FORMAT_OPTION ) ) {
            try {
                this.batchFormat = BatchTaskExecutorService.BatchFormat.valueOf( commandLine.getOptionValue( BATCH_FORMAT_OPTION ).toUpperCase() );
            } catch ( IllegalArgumentException e ) {
                throw new ParseException( String.format( "Unsupported batch format: %s.", commandLine.getOptionValue( BATCH_FORMAT_OPTION ) ) );
            }
        } else {
            this.batchFormat = commandLine.hasOption( BATCH_OUTPUT_FILE_OPTION ) ? BatchTaskExecutorService.BatchFormat.TSV : BatchTaskExecutorService.BatchFormat.TEXT;
        }
        this.batchOutputFile = commandLine.getParsedOptionValue( BATCH_OUTPUT_FILE_OPTION );
    }

    /**
     * Process command line options.
     * <p>
     * Implement this to provide processing of options. It is called after {@link #buildOptions(Options)} and right before
     * {@link #doWork()}.
     *
     * @throws ParseException in case of unrecoverable failure (i.e. missing option or invalid value), an exception can
     *                        be raised and will result in an exit code of {@link #FAILURE}.
     */
    protected abstract void processOptions( CommandLine commandLine ) throws ParseException;

    /**
     * Default workflow of a CLI.
     */
    private void work() throws Exception {
        try {
            insideDoWork = true;
            doWork();
        } finally {
            insideDoWork = false;
        }
    }

    /**
     * Command line implementation.
     * <p>
     * This is called after {@link #buildOptions(Options)} and {@link #processOptions(CommandLine)}.
     * so the implementation can assume that all its arguments have already been initialized and any setup behaviour
     * have been performed.
     *
     * @throws Exception in case of unrecoverable failure, an exception is thrown and will result in a
     *                   {@link #FAILURE} exit code, otherwise use {@link #addErrorObject} to indicate an
     *                   error and resume processing
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
     * Exception raised when a {@link #doWork()} aborted by the user.
     */
    private static class WorkAbortedException extends Exception {

        private WorkAbortedException( String message ) {
            super( message );
        }
    }

    /**
     * Add a success object to indicate success in a batch processing.
     * @param successObject object that was processed
     * @param message       success message
     */
    protected void addSuccessObject( Object successObject, String message ) {
        getBatchTaskExecutorInternal().addSuccessObject( successObject, message );
    }

    /**
     * @see #addSuccessObject(Object, String)
     */
    protected void addSuccessObject( Object successObject ) {
        getBatchTaskExecutorInternal().addSuccessObject( successObject );
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
    protected void addErrorObject( @Nullable Object errorObject, String message, Throwable throwable ) {
        getBatchTaskExecutorInternal().addErrorObject( errorObject, message, throwable );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ) + ":\n\t" + message, throwable );
    }

    /**
     * Add an error object without a cause stacktrace.
     *
     * @see #addErrorObject(Object, String)
     */
    protected void addErrorObject( @Nullable Object errorObject, String message ) {
        getBatchTaskExecutorInternal().addErrorObject( errorObject, message );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ) + ":\n\t" + message );
    }

    /**
     * Add an error object based on an exception.
     *
     * @see #addErrorObject(Object, String, Throwable)
     */
    protected void addErrorObject( @Nullable Object errorObject, Exception exception ) {
        getBatchTaskExecutorInternal().addErrorObject( errorObject, exception );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ), exception );
    }

    /**
     * Obtain an executor for running batch tasks.
     * <p>
     * The CLI will await any pending batch tasks before exiting. You may only submit batch tasks inside {@link #doWork()}.
     * <p>
     * Successes and errors are reported automatically if {@link #addSuccessObject(Object, String)} or {@link #addErrorObject(Object, String)}
     * haven't been invoked during the execution of the callable/runnable.
     */
    protected final ExecutorService getBatchTaskExecutor() {
        // BatchTaskExecutorService is package-private
        return getBatchTaskExecutorInternal();
    }

    /**
     * Internal batch task executor, because {@link BatchTaskExecutorService} is package-private.
     */
    private BatchTaskExecutorService getBatchTaskExecutorInternal() {
        Assert.isTrue( insideDoWork, "Batch tasks can only be submitted in doWork()." );
        if ( executorService == null ) {
            executorService = new BatchTaskExecutorService( createBatchTaskExecutorService(), batchFormat, batchOutputFile );
        }
        return executorService;
    }

    /**
     * Create an {@link ExecutorService} to be used for running batch tasks.
     */
    protected ExecutorService createBatchTaskExecutorService() {
        Assert.isNull( executorService, "There is already a batch task ExecutorService." );
        ThreadFactory threadFactory = new SimpleThreadFactory( "gemma-cli-batch-thread-" );
        if ( this.numThreads > 1 ) {
            return Executors.newFixedThreadPool( this.numThreads, threadFactory );
        } else {
            return Executors.newSingleThreadExecutor( threadFactory );
        }
    }

    /**
     * Await the completion of all batch tasks.
     */
    protected void awaitBatchExecutorService() throws InterruptedException {
        if ( executorService == null ) {
            return;
        }
        executorService.shutdown();
        if ( executorService.isTerminated() ) {
            return;
        }
        log.info( String.format( "Awaiting for %d/%d batch tasks to finish...", executorService.getSubmittedTasks() - executorService.getCompletedTasks(), executorService.getSubmittedTasks() ) );
        while ( !executorService.awaitTermination( 30, TimeUnit.SECONDS ) ) {
            log.info( String.format( "Completed %d/%d batch tasks.", executorService.getCompletedTasks(), executorService.getSubmittedTasks() ) );
        }
    }
}
