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
package ubic.gemma.cli.util;

import org.apache.commons.cli.*;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ocpsoft.prettytime.shade.edu.emory.mathcs.backport.java.util.Collections;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import ubic.gemma.cli.batch.*;
import ubic.gemma.core.util.SimpleThreadFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
public abstract class AbstractCLI implements CLI, ApplicationContextAware {

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
    private static final String BATCH_REPORT_FREQUENCY_OPTION = "batchReportFrequency";
    private static final String BATCH_LOG_CATEGORY = "ubic.gemma.core.util.BatchLogger";

    /**
     * Application context
     */
    private ApplicationContext applicationContext;

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
    private BatchFormat batchFormat;
    /**
     * Destination for batch processing summary.
     */
    @Nullable
    private Path batchOutputFile;
    /**
     * Frequency at which to report batch processing progress.
     */
    @Nullable
    private Integer batchReportFrequencySeconds;

    /**
     * Indicate if we are "inside" {@link #doWork()}.
     */
    private boolean insideDoWork = false;

    @Nullable
    private BatchTaskExecutorService executorService;
    @Nullable
    private BatchTaskProgressReporter progressReporter;

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
            return progressReporter != null && progressReporter.hasErrorObjects() ? FAILURE_FROM_ERROR_OBJECTS : SUCCESS;
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
                executorService = null;
            }
            if ( progressReporter != null ) {
                // always summarize processing, even if an error is thrown
                try {
                    progressReporter.close();
                } catch ( IOException e ) {
                    log.error( "Failed to close batch task executor.", e );
                }
                progressReporter = null;
            }
        }
    }

    /**
     * Obtain the application context.
     * <p>
     * Beans in a CLI context are usually lazily-initialized as using {@link ubic.gemma.core.context.LazyInitByDefaultPostProcessor},
     * so using the context to create beans ensures that only the necessary beans are created.
     */
    public final ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public final void setApplicationContext( ApplicationContext applicationContext ) {
        this.applicationContext = applicationContext;
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
        String commandName = this.getCliContext().getCommandNameOrAliasUsed();
        if ( commandName == null ) {
            commandName = getClass().getName();
        }
        return "gemma-cli [options] " + commandName + " [commandOptions]" + ( allowPositionalArguments ? " [files]" : "" );
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
        OptionsUtils.addEnumOption( options, BATCH_FORMAT_OPTION, "batch-format",
                "Format to use to the batch summary (default to TEXT or TSV if a file is specified via -" + BATCH_OUTPUT_FILE_OPTION + ")",
                BatchFormat.class );
        options.addOption( Option.builder( BATCH_OUTPUT_FILE_OPTION )
                .longOpt( "batch-output-file" ).hasArg().type( Path.class )
                .desc( "Output file to use for the batch summary (default is standard output)" ).build() );
        options.addOption( Option.builder( BATCH_REPORT_FREQUENCY_OPTION ).longOpt( "batch-report-frequency" )
                .hasArg().type( Integer.class )
                .desc( "Frequency at which to report batch task progress in seconds (default is every 30 seconds)" )
                .build() );
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
            this.batchFormat = OptionsUtils.getEnumOptionValue( commandLine, BATCH_FORMAT_OPTION );
        } else {
            this.batchFormat = commandLine.hasOption( BATCH_OUTPUT_FILE_OPTION ) ? BatchFormat.TSV : BatchFormat.TEXT;
        }
        this.batchOutputFile = commandLine.getParsedOptionValue( BATCH_OUTPUT_FILE_OPTION );
        this.batchReportFrequencySeconds = commandLine.getParsedOptionValue( BATCH_REPORT_FREQUENCY_OPTION );
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

    enum BatchFormat {
        TEXT,
        TSV,
        LOG,
        SUPPRESS
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
        getBatchTaskProgressReporter().addSuccessObject( successObject, message );
    }

    /**
     * @see #addSuccessObject(Object, String)
     */
    protected final void addSuccessObject( Object successObject ) {
        getBatchTaskProgressReporter().addSuccessObject( successObject );
    }

    /**
     * @see #addWarningObject(Object, String, Throwable)
     */
    protected final void addWarningObject( @Nullable Object warningObject, String message ) {
        getBatchTaskProgressReporter().addWarningObject( warningObject, message );
    }

    /**
     * Add a warning object with a stacktrace to indicate a recoverable failure in a batch processing.
     *
     * @param warningObject that was processed
     * @param message       error message
     * @param throwable     throwable to produce a stacktrace
     */
    protected final void addWarningObject( @Nullable Object warningObject, String message, Throwable throwable ) {
        getBatchTaskProgressReporter().addWarningObject( warningObject, message, throwable );
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
        getBatchTaskProgressReporter().addErrorObject( errorObject, message, throwable );
    }

    /**
     * Add an error object without a cause stacktrace.
     *
     * @see #addErrorObject(Object, String)
     */
    protected final void addErrorObject( @Nullable Object errorObject, String message ) {
        getBatchTaskProgressReporter().addErrorObject( errorObject, message );
    }

    /**
     * Add an error object based on an exception.
     *
     * @see #addErrorObject(Object, String, Throwable)
     */
    protected final void addErrorObject( @Nullable Object errorObject, Exception exception ) {
        getBatchTaskProgressReporter().addErrorObject( errorObject, exception );
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
    private synchronized BatchTaskExecutorService getBatchTaskExecutorInternal() {
        Assert.state( insideDoWork, "Batch tasks can only be submitted in doWork()." );
        if ( executorService == null ) {
            executorService = new BatchTaskExecutorService( createBatchTaskExecutorService(), getBatchTaskProgressReporter() );
        }
        return executorService;
    }

    /**
     * This needs to be synchronized because multiple threads might try to report progress at the same time.
     */
    private synchronized BatchTaskProgressReporter getBatchTaskProgressReporter() {
        if ( progressReporter == null ) {
            BatchTaskSummaryWriter summaryWriter;
            switch ( batchFormat ) {
                case TEXT:
                    try {
                        summaryWriter = new CompositeBatchTaskSummaryWriter( Arrays.asList(
                                new TextBatchTaskSummaryWriter( openBatchTaskSummaryDestination() ),
                                new LoggingBatchTaskSummaryWriter( BATCH_LOG_CATEGORY, true ) ) );
                    } catch ( IOException e ) {
                        throw new RuntimeException( "Failed to open destination for writing batch task results.", e );
                    }
                    break;
                case TSV:
                    try {
                        summaryWriter = new CompositeBatchTaskSummaryWriter( Arrays.asList(
                                new TsvBatchTaskSummaryWriter( openBatchTaskSummaryDestination() ),
                                new LoggingBatchTaskSummaryWriter( BATCH_LOG_CATEGORY, true ) ) );
                    } catch ( IOException e ) {
                        throw new RuntimeException( "Failed to open destination for writing batch task results.", e );
                    }
                    break;
                case LOG:
                    summaryWriter = new LoggingBatchTaskSummaryWriter( BATCH_LOG_CATEGORY );
                    break;
                case SUPPRESS:
                    summaryWriter = new SuppressBatchTaskSummaryWriter();
                    break;
                default:
                    throw new IllegalStateException( "Unsupported batch format " + batchFormat );
            }
            progressReporter = new BatchTaskProgressReporter( summaryWriter );
            if ( batchReportFrequencySeconds != null ) {
                progressReporter.setReportFrequencyMillis( batchReportFrequencySeconds * 1000 );
            }
        }
        return progressReporter;
    }

    private Writer openBatchTaskSummaryDestination() throws IOException {
        if ( batchOutputFile != null ) {
            log.info( String.format( "Batch processing summary will be written to %s", batchOutputFile ) );
            // always produce UTF-8 files
            return Files.newBufferedWriter( batchOutputFile, StandardCharsets.UTF_8 );
        } else {
            // prevent closing the standard output stream
            // use the standard charset since we're printing to the console
            return new OutputStreamWriter( CloseShieldOutputStream.wrap( getCliContext().getOutputStream() ) );
        }
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
     * Set the frequency at which the batch task executor should report progress.
     * <p>
     * The default is to report progress every 30 seconds. It may be overwritten by the {@code -batchReportFrequency}
     * option.
     */
    protected void setReportFrequencyMillis( int reportFrequencyMillis ) {
        getBatchTaskProgressReporter().setReportFrequencyMillis( reportFrequencyMillis );
    }

    /**
     * Set the estimated maximum number of batch tasks to be processed.
     */
    protected void setEstimatedMaxTasks( int estimatedMaxTasks ) {
        getBatchTaskProgressReporter().setEstimatedMaxTasks( estimatedMaxTasks );
    }

    /**
     * Await the completion of all batch tasks.
     * <p>
     * The batch task executor will be shutdown, preventing any new tasks from being submitted.
     */
    protected final void awaitBatchExecutorService() throws InterruptedException {
        if ( executorService == null ) {
            return;
        }
        executorService.shutdown();
        if ( executorService.isTerminated() ) {
            return;
        }
        int remainingTasks = executorService.getRemainingTasks();
        if ( remainingTasks > 0 ) {
            log.info( String.format( "Awaiting for %d batch tasks to finish...", remainingTasks ) );
        }
        getBatchTaskProgressReporter().setEstimatedMaxTasks( getBatchTaskProgressReporter().getCompletedTasks() + remainingTasks );
        while ( !executorService.awaitTermination( 100, TimeUnit.MILLISECONDS ) ) {
            getBatchTaskProgressReporter().reportProgress();
        }
    }
}
