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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.util.DateUtil;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Base Command Line Interface. Provides some default functionality.
 * <p>
 * To use this, in your concrete subclass, implement a main method. You must implement buildOptions and processOptions
 * to handle any application-specific options (they can be no-ops).
 * <p>
 * To facilitate testing of your subclass, your main method must call a non-static 'doWork' method, that will be exposed
 * for testing. In that method call processCommandline. You should return any non-null return value from
 * processCommandLine.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class AbstractCLI implements CLI {

    /**
     * Exit code used for a successful doWork execution.
     */
    public static final int SUCCESS = 0;
    /**
     * Exit code used for a failed doWork execution.
     */
    public static final int FAILURE = 1;
    /**
     * Exit code used for a successful doWork execution that resulted in failed error objects.
     */
    public static final int FAILURE_FROM_ERROR_OBJECTS = 1;

    public static final String FOOTER = "The Gemma project, Copyright (c) 2007-2023 University of British Columbia.";
    protected static final String AUTO_OPTION_NAME = "auto";
    protected static final String THREADS_OPTION = "threads";
    protected static final Log log = LogFactory.getLog( AbstractCLI.class );
    private static final int DEFAULT_PORT = 3306;
    public static final String HEADER = "Options:";
    private static final String HOST_OPTION = "H";
    private static final String PORT_OPTION = "P";
    private static final String HELP_OPTION = "h";
    private static final String TESTING_OPTION = "testing";
    private static final String DATE_OPTION = "mdate";

    /* support for convenience options */
    private final String DEFAULT_HOST = "localhost";
    /**
     * Automatically identify which entities to run the tool on. To enable call addAutoOption.
     */
    protected boolean autoSeek = false;
    /**
     * The event type to look for the lack of, when using auto-seek.
     */
    protected Class<? extends AuditEventType> autoSeekEventType = null;
    /**
     * Date used to identify which entities to run the tool on (e.g., those which were run less recently than mDate). To
     * enable call addDateOption.
     */
    protected String mDate = null;
    protected int numThreads = 1;
    protected String host = DEFAULT_HOST;
    protected int port = AbstractCLI.DEFAULT_PORT;
    private ExecutorService executorService;

    // hold the results of the command execution
    // needs to be concurrently modifiable and kept in-order
    private final List<BatchProcessingResult> errorObjects = Collections.synchronizedList( new ArrayList<>() );
    private final List<BatchProcessingResult> successObjects = Collections.synchronizedList( new ArrayList<>() );

    /**
     * Run the command.
     * <p>
     * Parse and process CLI arguments, invoke the command doWork implementation, and print basic statistics about time
     * usage.
     *
     * @param args Arguments to pass to {@link #processCommandLine(Options, String[])}
     * @return Exit code intended to be used with {@link System#exit(int)} to indicate a success or failure to the
     * end-user. Any exception raised by doWork results in a value of {@link #FAILURE}, and any error set in the
     * internal error objects will result in a value of {@link #FAILURE_FROM_ERROR_OBJECTS}.
     */
    @Override
    public int executeCommand( String[] args ) {
        StopWatch watch = new StopWatch();
        watch.start();
        if ( args == null ) {
            System.err.println( "No arguments" );
            return FAILURE;
        }
        try {
            Options options = new Options();
            buildStandardOptions( options );
            buildOptions( options );
            DefaultParser parser = new DefaultParser();
            CommandLine commandLine;
            try {
                commandLine = parser.parse( options, args );
            } catch ( ParseException e ) {
                if ( e instanceof MissingOptionException ) {
                    // check if -h/--help was passed alongside a required argument
                    if ( ArrayUtils.contains( args, "-h" ) || ArrayUtils.contains( args, "--help" ) ) {
                        printHelp( options );
                        return SUCCESS;
                    }
                    System.err.println( "Required option(s) were not supplied: " + e.getMessage() );
                } else if ( e instanceof AlreadySelectedException ) {
                    System.err.println( "The option(s) " + e.getMessage() + " were already selected" );
                } else if ( e instanceof MissingArgumentException ) {
                    System.err.println( "Missing argument: " + e.getMessage() );
                } else if ( e instanceof UnrecognizedOptionException ) {
                    System.err.println( "Unrecognized option: " + e.getMessage() );
                }
                printHelp( options );
                return FAILURE;
            }
            // check if -h/--help is provided before pursuing option processing
            if ( commandLine.hasOption( HELP_OPTION ) ) {
                printHelp( options );
                return SUCCESS;
            }
            if ( commandLine.hasOption( TESTING_OPTION ) ) {
                AbstractCLI.log.error( String.format( "The -testing/--testing option must be passed before the %s command.", getCommandName() ) );
                return FAILURE;
            }
            processStandardOptions( commandLine );
            processOptions( commandLine );
            doWork();
            return errorObjects.isEmpty() ? SUCCESS : FAILURE_FROM_ERROR_OBJECTS;
        } catch ( Exception e ) {
            log.error( String.format( "%s failed: %s", getCommandName(), ExceptionUtils.getRootCauseMessage( e ) ), e );
            return FAILURE;
        } finally {
            // always summarize processing, even if an error is thrown
            summarizeProcessing();
            log.info( String.format( "Elapsed time: %d seconds.", watch.getTime( TimeUnit.SECONDS ) ) );
        }
    }

    /**
     * You must implement the handling for this option.
     */
    protected void addAutoOption( Options options ) {
        Option autoSeekOption = Option.builder( AUTO_OPTION_NAME )
                .desc( "Attempt to process entities that need processing based on workflow criteria." )
                .build();

        options.addOption( autoSeekOption );
    }

    protected void addDateOption( Options options ) {
        Option dateOption = Option.builder( DATE_OPTION ).hasArg().desc(
                        "Constrain to run only on entities with analyses older than the given date. "
                                + "For example, to run only on entities that have not been analyzed in the last 10 days, use '-10d'. "
                                + "If there is no record of when the analysis was last run, it will be run." )
                .build();

        options.addOption( dateOption );
    }

    /**
     * Convenience method to add a standard pair of options to intake a host name and port number. *
     *
     * @param hostRequired Whether the host name is required
     * @param portRequired Whether the port is required
     */
    protected void addHostAndPortOptions( Options options, boolean hostRequired, boolean portRequired ) {
        Option hostOpt = Option.builder( HOST_OPTION ).argName( "host name" ).longOpt( "host" ).hasArg()
                .desc( "Hostname to use (Default = " + DEFAULT_HOST + ")" )
                .build();

        hostOpt.setRequired( hostRequired );

        Option portOpt = Option.builder( PORT_OPTION ).argName( "port" ).longOpt( "port" ).hasArg()
                .desc( "Port to use on host (Default = " + AbstractCLI.DEFAULT_PORT + ")" )
                .build();

        portOpt.setRequired( portRequired );

        options.addOption( hostOpt );
        options.addOption( portOpt );
    }

    /**
     * Convenience method to add an option for parallel processing option.
     */
    protected void addThreadsOption( Options options ) {
        Option threadsOpt = Option.builder( THREADS_OPTION ).argName( "numThreads" ).hasArg()
                .desc( "Number of threads to use for batch processing." )
                .build();
        options.addOption( threadsOpt );
    }

    /**
     * Build option implementation.
     * <p>
     * Implement this method to add options to your command line, using the OptionBuilder.
     * <p>
     * This is called right after {@link #buildStandardOptions(Options)} so the options will be added after standard options.
     */
    protected abstract void buildOptions( Options options );

    protected void buildStandardOptions( Options options ) {
        AbstractCLI.log.debug( "Creating standard options" );
        options.addOption( HELP_OPTION, "help", false, "Print this message" );
        options.addOption( TESTING_OPTION, "testing", false, "Use the test environment. This option must be passed before the command." );
    }

    /**
     * Command line implementation.
     * <p>
     * This is called after {@link #buildOptions(Options)} and {@link #processOptions(CommandLine)}, so the implementation can assume that
     * all its arguments have already been initialized.
     *
     * @throws Exception in case of unrecoverable failure, an exception is thrown and will result in a {@link #FAILURE}
     *                   exit code, otherwise use {@link #addErrorObject}
     */
    protected abstract void doWork() throws Exception;

    protected final double getDoubleOptionValue( CommandLine commandLine, char option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( this.invalidOptionString( commandLine, String.valueOf( option ) ) + ", not a valid double", e );
        }
    }

    protected final double getDoubleOptionValue( CommandLine commandLine, String option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( this.invalidOptionString( commandLine, option ) + ", not a valid double", e );
        }
    }

    protected final String getFileNameOptionValue( CommandLine commandLine, char c ) {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            throw new RuntimeException( this.invalidOptionString( commandLine, String.valueOf( c ) ) + ", cannot read from file" );
        }
        return fileName;
    }

    protected final String getFileNameOptionValue( CommandLine commandLine, String c ) {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            throw new RuntimeException( this.invalidOptionString( commandLine, c ) + ", cannot read from file" );
        }
        return fileName;
    }

    protected final int getIntegerOptionValue( CommandLine commandLine, char option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( this.invalidOptionString( commandLine, String.valueOf( option ) ) + ", not a valid integer", e );
        }
    }

    protected final int getIntegerOptionValue( CommandLine commandLine, String option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( this.invalidOptionString( commandLine, option ) + ", not a valid integer", e );
        }
    }

    protected Date getLimitingDate() {
        Date skipIfLastRunLaterThan = null;
        if ( StringUtils.isNotBlank( mDate ) ) {
            skipIfLastRunLaterThan = DateUtil.getRelativeDate( new Date(), mDate );
            AbstractCLI.log.info( "Analyses will be run only if last was older than " + skipIfLastRunLaterThan );
        }
        return skipIfLastRunLaterThan;
    }

    protected void printHelp( Options options ) {
        new HelpFormatter().printHelp( new PrintWriter( System.err, true ), 150,
                this.getCommandName() + " [options]",
                this.getShortDesc() + "\n" + AbstractCLI.HEADER,
                options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, AbstractCLI.FOOTER );
    }

    /**
     * Process command line options.
     * <p>
     * Implement this to provide processing of options. It is called after {@link #buildOptions(Options)} and right before
     * {@link #doWork()}.
     *
     * @throws Exception in case of unrecoverable failure (i.e. missing option or invalid value), an exception can be
     *                   raised and will result in an exit code of {@link #FAILURE}.
     */
    protected abstract void processOptions( CommandLine commandLine ) throws Exception;

    /**
     * Add a success object to indicate success in a batch processing.
     * <p>
     * This is further used in {@link #summarizeProcessing()} to summarize the execution of the command.
     *
     * @param successObject object that was processed
     * @param message       success message
     */
    protected void addSuccessObject( Object successObject, String message ) {
        successObjects.add( new BatchProcessingResult( successObject, message, null ) );
    }

    /**
     * @see #addSuccessObject(Object, String)
     */
    protected void addSuccessObject( Object successObject ) {
        successObjects.add( new BatchProcessingResult( successObject, null, null ) );
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
    protected void addErrorObject( @Nullable Object errorObject, String message, Throwable throwable ) {
        errorObjects.add( new BatchProcessingResult( errorObject, message, throwable ) );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ) + ":\n\t" + message, throwable );
    }

    /**
     * Add an error object without a cause stacktrace.
     * @see #addErrorObject(Object, String)
     */
    protected void addErrorObject( @Nullable Object errorObject, String message ) {
        errorObjects.add( new BatchProcessingResult( errorObject, message, null ) );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ) + ":\n\t" + message );
    }

    /**
     * Add an error object based on an exception.
     * @see #addErrorObject(Object, String, Throwable)
     */
    protected void addErrorObject( @Nullable Object errorObject, Exception exception ) {
        errorObjects.add( new BatchProcessingResult( errorObject, exception.getMessage(), exception ) );
        log.error( "Error while processing " + ( errorObject != null ? errorObject : "unknown object" ), exception );
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
                buf.append( result ).append( "\n" );
            }
            buf.append( "---------------------\n" );

            AbstractCLI.log.info( buf );
        }

        if ( errorObjects.size() > 0 ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\nErrors occurred during the processing of " )
                    .append( errorObjects.size() ).append( " objects:\n" );
            for ( BatchProcessingResult result : errorObjects ) {
                buf.append( result ).append( "\n" );
            }
            buf.append( "---------------------\n" );
            AbstractCLI.log.error( buf );
        }
    }

    /**
     * Execute batch tasks using a preconfigured {@link ExecutorService} and return all the resulting tasks results.
     *
     */
    protected <T> List<T> executeBatchTasks( Collection<? extends Callable<T>> tasks ) throws InterruptedException {
        List<Future<T>> futures = executorService.invokeAll( tasks );
        List<T> futureResults = new ArrayList<>( futures.size() );
        int i = 0;
        for ( Future<T> future : futures ) {
            try {
                futureResults.add( future.get() );
            } catch ( ExecutionException e ) {
                addErrorObject( null, String.format( "Batch task #%d failed", ++i ), e.getCause() );
            }
        }
        return futureResults;
    }

    private String invalidOptionString( CommandLine commandLine, String option ) {
        return "Invalid value '" + commandLine.getOptionValue( option ) + " for option " + option;
    }

    /**
     * Somewhat annoying: This causes subclasses to be unable to safely use 'h', 'p', 'u' and 'P' etc. for their own
     * purposes.
     */
    protected void processStandardOptions( CommandLine commandLine ) {

        if ( commandLine.hasOption( AbstractCLI.HOST_OPTION ) ) {
            this.host = commandLine.getOptionValue( AbstractCLI.HOST_OPTION );
        } else {
            this.host = DEFAULT_HOST;
        }

        if ( commandLine.hasOption( AbstractCLI.PORT_OPTION ) ) {
            this.port = this.getIntegerOptionValue( commandLine, AbstractCLI.PORT_OPTION );
        } else {
            this.port = AbstractCLI.DEFAULT_PORT;
        }

        if ( commandLine.hasOption( DATE_OPTION ) ) {
            this.mDate = commandLine.getOptionValue( DATE_OPTION );
        }

        if ( this.numThreads < 1 ) {
            throw new IllegalArgumentException( "Number of threads must be greater than 1." );
        }
        this.executorService = new ForkJoinPool( this.numThreads );
    }

    /**
     * Represents an individual result in a batch processing.
     */
    @Value
    private static class BatchProcessingResult {
        @Nullable
        Object source;
        @Nullable
        String message;
        @Nullable
        Throwable throwable;

        public BatchProcessingResult( @Nullable Object source, @Nullable String message, @Nullable Throwable throwable ) {
            this.source = source;
            this.message = message;
            this.throwable = throwable;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append( source != null ? source : "Unknown object" );
            if ( message != null ) {
                buf.append( ":\n\t" )
                        .append( message.replace( "\n", "\n\t" ) );
            }
            return buf.toString();
        }
    }
}
