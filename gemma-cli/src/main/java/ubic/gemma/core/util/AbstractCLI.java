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
import org.ocpsoft.prettytime.shade.edu.emory.mathcs.backport.java.util.Collections;
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
    protected static final int FAILURE_FROM_ERROR_OBJECTS = 2;
    /**
     * Exit code used when a {@link #doWork()} is aborted.
     */
    protected static final int ABORTED = 3;

    private static final String THREADS_OPTION = "threads";
    private static final String HELP_OPTION = "h";

    private static final String BATCH_FORMAT_OPTION = "batchFormat";
    private static final String BATCH_OUTPUT_FILE_OPTION = "batchOutputFile";

    /**
     * CLI context.
     */
    @Nullable
    private CliContext cliContext;
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
    public String getCommandName() {
        return null;
    }

    @Override
    public List<String> getCommandAliases() {
        //noinspection unchecked
        return Collections.emptyList();
    }

    @Override
    public String getShortDesc() {
        return null;
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
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
    public final int executeCommand( CliContext cliContext ) {
        Assert.state( this.cliContext == null, "There is already a CLI context." );
        try {
            this.cliContext = cliContext;
            return executeCommandWithCliContext();
        } finally {
            this.cliContext = null;
        }
    }

    /**
     * Execute a command with a {@link CliContext} set.
     */
    private int executeCommandWithCliContext() {
        Options options = getOptions();
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse( options, getCliContext().getArguments() );
            // check if -h/--help is provided before pursuing option processing
            if ( commandLine.hasOption( HELP_OPTION ) ) {
                printHelp( options, new PrintWriter( getCliContext().getOutputStream(), true ) );
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
                if ( ArrayUtils.contains( getCliContext().getArguments(), "-h" ) || ArrayUtils.contains( getCliContext().getArguments(), "--help" ) ) {
                    printHelp( options, new PrintWriter( getCliContext().getOutputStream(), true ) );
                    return SUCCESS;
                }
                getCliContext().getErrorStream().println( "Required option(s) were not supplied: " + e.getMessage() );
            } else if ( e instanceof AlreadySelectedException ) {
                getCliContext().getErrorStream().println( "The option(s) " + e.getMessage() + " were already selected" );
            } else if ( e instanceof MissingArgumentException ) {
                getCliContext().getErrorStream().println( "Missing argument: " + e.getMessage() );
            } else if ( e instanceof UnrecognizedOptionException ) {
                getCliContext().getErrorStream().println( "Unrecognized option: " + e.getMessage() );
            } else {
                getCliContext().getErrorStream().println( e.getMessage() );
            }
            printHelp( options, new PrintWriter( getCliContext().getErrorStream(), true ) );
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
     * Obtain the CLI context.
     * <p>
     * This is only valid for the duration of the {@link #executeCommand(CliContext)} method.
     */
    protected final CliContext getCliContext() {
        Assert.state( cliContext != null, "The CLI context can only be obtained during the execution of the command." );
        return cliContext;
    }

    private void printHelp( Options options, PrintWriter writer ) {
        HelpUtils.printHelp( writer, getUsage(), options, getShortDesc(), getHelpFooter() );
    }

    /**
     * Describe the intended usage for the command.
     * <p>
     * This will be included in the 'Usage: ...' error message when the CLI is misused.
     */
    protected String getUsage() {
        return "gemma-cli [options] " + this.getCommandName() + " [commandOptions]" + ( allowPositionalArguments ? " [files]" : "" );
    }

    /**
     * Include a detailed help footer in the --help menu.
     * @see HelpUtils#printHelp(PrintWriter, String, Options, String, String)
     */
    @Nullable
    protected String getHelpFooter() {
        return null;
    }

    /**
     * Add the {@code -threads} option.
     * <p>
     * This is used to configure the internal batch processing thread pool which can be used with {@link #getBatchTaskExecutor()}.
     * <p>
     * You may also use {@link #getNumThreads()} to retrieve the number of threads to use.
     */
    protected final void addThreadsOption( Options options ) {
        Assert.state( !options.hasOption( THREADS_OPTION ), "The -" + THREADS_OPTION + " option was already added." );
        options.addOption( Option.builder( THREADS_OPTION )
                .longOpt( "threads" )
                .argName( "numThreads" ).hasArg()
                .desc( "Number of threads to use for batch processing." )
                .type( Number.class ) // FIXME: this should be an Integer.class
                .build() );
    }

    /**
     * Add the -batchFormat and -batchOutputFile options.
     * <p>
     * These options allow the user to control how and where batch processing results are summarized.
     */
    protected final void addBatchOption( Options options ) {
        Assert.state( !options.hasOption( BATCH_FORMAT_OPTION ), "The -" + BATCH_FORMAT_OPTION + " option was already added." );
        options.addOption( BATCH_FORMAT_OPTION, true, "Format to use to the batch summary" );
        options.addOption( Option.builder( BATCH_OUTPUT_FILE_OPTION ).hasArg().type( Path.class ).desc( "Output file to use for the batch summary (default is standard output)" ).build() );
    }

    /**
     * Allow positional arguments to be specified. The default is false and an error will be produced if positional
     * arguments are supplied by the user.
     * <p>
     * Those arguments can be retrieved in {@link #processOptions(CommandLine)} by using {@link CommandLine#getArgList()}.
     */
    protected final void setAllowPositionalArguments() {
        this.allowPositionalArguments = true;
    }

    protected final int getNumThreads() {
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
    protected void buildOptions( Options options ) {

    }

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
    protected void processOptions( CommandLine commandLine ) throws ParseException {
    }

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
    protected final void promptConfirmationOrAbort( String confirmationMessage ) throws Exception {
        if ( getCliContext().getConsole() == null ) {
            throw new IllegalStateException( "A console must be available for prompting confirmation." );
        }
        String line = getCliContext().getConsole().readLine( "WARNING: %s\nWARNING: Enter YES to continue: ",
                confirmationMessage.replaceAll( "\n", "\nWARNING: " ) );
        if ( "YES".equals( line.trim() ) ) {
            return;
        }
        abort( "Confirmation failed, the command cannot proceed." );
    }

    /**
     * Abort the execution of the CLI with the given message.
     * <p>
     * The CLI will exit with the {@link #ABORTED} exit code as a result.
     */
    protected final void abort( String message ) throws Exception {
        throw new WorkAbortedException( message );
    }

    /**
     * Exception raised when a {@link AbstractCLI#doWork()} aborted by the user.
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
    protected final void addSuccessObject( Object successObject, String message ) {
        getBatchTaskExecutorInternal().addSuccessObject( successObject, message );
    }

    /**
     * @see #addSuccessObject(Object, String)
     */
    protected final void addSuccessObject( Object successObject ) {
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
    protected final void addErrorObject( @Nullable Object errorObject, String message, Throwable throwable ) {
        getBatchTaskExecutorInternal().addErrorObject( errorObject, message, throwable );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ) + ":\n\t" + message, throwable );
    }

    /**
     * Add an error object without a cause stacktrace.
     *
     * @see #addErrorObject(Object, String)
     */
    protected final void addErrorObject( @Nullable Object errorObject, String message ) {
        getBatchTaskExecutorInternal().addErrorObject( errorObject, message );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ) + ":\n\t" + message );
    }

    /**
     * Add an error object based on an exception.
     *
     * @see #addErrorObject(Object, String, Throwable)
     */
    protected final void addErrorObject( @Nullable Object errorObject, Exception exception ) {
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
        Assert.state( insideDoWork, "Batch tasks can only be submitted in doWork()." );
        if ( executorService == null ) {
            executorService = new BatchTaskExecutorService( createBatchTaskExecutorService(), batchFormat, batchOutputFile, getCliContext() );
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
    protected final void awaitBatchExecutorService() throws InterruptedException {
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
