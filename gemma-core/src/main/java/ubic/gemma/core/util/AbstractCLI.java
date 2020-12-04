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

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import ubic.basecode.util.DateUtil;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.util.Settings;

/**
 * Base Command Line Interface. Provides some default functionality.
 *
 * To use this, in your concrete subclass, implement a main method. You must implement buildOptions and processOptions
 * to handle any application-specific options (they can be no-ops).
 *
 * To facilitate testing of your subclass, your main method must call a non-static 'doWork' method, that will be exposed
 * for testing. In that method call processCommandline. You should return any non-null return value from
 * processCommandLine.
 *
 * @author pavlidis
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // Possible external use
public abstract class AbstractCLI {

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

    public static final String FOOTER = "The Gemma project, Copyright (c) 2007-2018 University of British Columbia.";
    protected static final String AUTO_OPTION_NAME = "auto";
    protected static final String THREADS_OPTION = "threads";
    protected static final Log log = LogFactory.getLog( AbstractCLI.class );
    private static final String LOGGER_OPTION = "logger";
    private static final int DEFAULT_PORT = 3306;
    private static final String HEADER = "Options:";
    private static final String HOST_OPTION = "H";
    private static final String PASSWORD_CONSTANT = "p";
    private static final String PORT_OPTION = "P";
    private static final String USERNAME_OPTION = "u";
    private static final String VERBOSITY_OPTION = "v";

    protected final Options options = new Options();

    /* support for convenience options */
    private final String DEFAULT_HOST = "localhost";
    private static final Map<Logger, Level> originalLoggingLevels = new HashMap<>();
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
    private ExecutorService executorService;
    protected String password;
    protected Option passwordOpt;
    protected int port = AbstractCLI.DEFAULT_PORT;
    protected String username;
    protected Option usernameOpt;
    protected String host = DEFAULT_HOST;
    private CommandLine commandLine;

    // hold the results of the command execution
    // needs to be concurrently modifiable and kept in-order
    private final List<BatchProcessingResult> errorObjects = Collections.synchronizedList( new ArrayList<BatchProcessingResult>() );
    private final List<BatchProcessingResult> successObjects = Collections.synchronizedList( new ArrayList<BatchProcessingResult>() );

    /**
     * Run the command.
     *
     * Parse and process CLI arguments, invoke the command doWork implementation, and print basic statistics about time
     * usage.
     *
     * @param args arguments Arguments to pass to {@link #processCommandLine(String[])}
     * @return Exit code intended to be used with {@link System#exit(int)} to indicate a success or failure to the
     *         end-user. Any exception raised by doWork results in a value of {@link #FAILURE}, and any error set in the
     *         internal error objects will result in a value of {@link #FAILURE_FROM_ERROR_OBJECTS}.
     */
    public int executeCommand( String[] args ) {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            buildOptions();
            buildStandardOptions();
            processCommandLine( args );
            // check if -h/--help is provided before pursuing option processing
            if ( commandLine.hasOption( 'h' ) ) {
                printHelp();
                return SUCCESS;
            }
            processStandardOptions();
            processOptions();
            doWork();
            return errorObjects.isEmpty() ? SUCCESS : FAILURE_FROM_ERROR_OBJECTS;
        } catch ( Exception e ) {
            log.error( getCommandName() + " failed.", e );
            return FAILURE;
        } finally {
            // always summarize processing, even if an error is thrown
            summarizeProcessing();
            resetLogging();
            AbstractCLI.log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds." );
        }
    }

    /**
     * @param opt option
     * @return Options
     */
    public final Options addOption( Option opt ) {
        return this.options.addOption( opt );
    }

    /**
     * @param opt         option, required
     * @param description description, required
     * @return Options
     */
    public final Options addOption( String opt, String description ) {
        Builder b = Option.builder( opt ).desc( description );

        return this.options.addOption( b.build() );
    }

    /**
     * @param opt         option, required
     * @param description description, required
     * @param longOpt     long option (if null, ignored)
     * @param argName     name of the argument of the option (if blank or null, implies there is no argument)
     * @return Options
     */
    public final Options addOption( String opt, String longOpt, String description, String argName ) {

        Builder b = Option.builder( opt ).desc( description );
        if ( StringUtils.isNotBlank( longOpt ) ) b = b.longOpt( longOpt );
        if ( StringUtils.isNotBlank( argName ) ) b = b.argName( argName ).hasArg();
        return this.options.addOption( b.build() );

    }

    /**
     * @param group the option group
     * @return see org.apache.commons.cli.Options#addOptionGroup(org.apache.commons.cli.OptionGroup)
     */
    public final Options addOptionGroup( OptionGroup group ) {
        return this.options.addOptionGroup( group );
    }

    public List<?> getArgList() {
        return commandLine.getArgList();
    }

    public String[] getArgs() {
        return commandLine.getArgs();
    }

    /**
     * A short memorable name for the command that can be used to locate this class.
     *
     * @return name; if null, this will not be available as a shortcut command.
     */
    public abstract String getCommandName();

    /**
     * @param opt the option identifier
     * @return see org.apache.commons.cli.Options#getOption(java.lang.String)
     */
    public final Option getOption( String opt ) {
        return this.options.getOption( opt );
    }

    /**
     * @param opt option
     * @return see org.apache.commons.cli.Options#getOptionGroup(org.apache.commons.cli.Option)
     */
    public final OptionGroup getOptionGroup( Option opt ) {
        return this.options.getOptionGroup( opt );
    }

    public Object getOptionObject( char opt ) {
        return commandLine.getOptionObject( opt );
    }

    /**
     * @return see org.apache.commons.cli.Options#getOptions()
     */
    public final Collection<?> getOptions() {
        return this.options.getOptions();
    }

    public String getOptionValue( char opt ) {
        return commandLine.getOptionValue( opt );
    }

    public String getOptionValue( char opt, String defaultValue ) {
        return commandLine.getOptionValue( opt, defaultValue );
    }

    public String getOptionValue( String opt ) {
        return commandLine.getOptionValue( opt );
    }

    public String getOptionValue( String opt, String defaultValue ) {
        return commandLine.getOptionValue( opt, defaultValue );
    }

    public String[] getOptionValues( char opt ) {
        return commandLine.getOptionValues( opt );
    }

    public String[] getOptionValues( String opt ) {
        return commandLine.getOptionValues( opt );
    }

    /**
     * @return see org.apache.commons.cli.Options#getRequiredOptions()
     */
    public final List<?> getRequiredOptions() {
        return this.options.getRequiredOptions();
    }

    public abstract String getShortDesc();

    public boolean hasOption( char opt ) {
        return commandLine.hasOption( opt );
    }

    public boolean hasOption( String opt ) {
        return commandLine.hasOption( opt );
    }

    /**
     * You must implement the handling for this option.
     */
    @SuppressWarnings("static-access")
    protected void addAutoOption() {
        Option autoSeekOption = Option.builder( AUTO_OPTION_NAME )
                .desc( "Attempt to process entities that need processing based on workflow criteria." )
                .build();

        this.addOption( autoSeekOption );
    }

    @SuppressWarnings("static-access")
    protected void addDateOption() {
        Option dateOption = Option.builder( "mdate" ).hasArg().desc(
                "Constrain to run only on entities with analyses older than the given date. "
                        + "For example, to run only on entities that have not been analyzed in the last 10 days, use '-10d'. "
                        + "If there is no record of when the analysis was last run, it will be run." )
                .build();

        this.addOption( dateOption );
    }

    /**
     * Convenience method to add a standard pair of options to intake a host name and port number. *
     *
     * @param hostRequired Whether the host name is required
     * @param portRequired Whether the port is required
     */
    @SuppressWarnings("static-access")
    protected void addHostAndPortOptions( boolean hostRequired, boolean portRequired ) {
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
    @SuppressWarnings("static-access")
    protected void addThreadsOption() {
        Option threadsOpt = Option.builder( THREADS_OPTION ).argName( "numThreads" ).hasArg()
                .desc( "Number of threads to use for batch processing." )
                .build();
        options.addOption( threadsOpt );
    }

    /**
     * Add required user name and password options.
     */
    protected void addUserNameAndPasswordOptions() {
        /*
         * Changed to make it so password is not required.
         */
        this.addUserNameAndPasswordOptions( false );
    }

    /**
     * Convenience method to add a standard pair of (required) options to intake a user name and password, optionally
     * required
     *
     * @param required required
     */
    @SuppressWarnings("static-access")
    protected void addUserNameAndPasswordOptions( boolean required ) {
        this.usernameOpt = Option.builder( AbstractCLI.USERNAME_OPTION ).argName( "user" ).longOpt( "user" ).hasArg()
                .desc( "User name for accessing the system (optional for some tools)" )
                .build();

        usernameOpt.setRequired( required );

        this.passwordOpt = Option.builder( PASSWORD_CONSTANT ).argName( "passwd" ).longOpt( "password" ).hasArg()
                .desc( "Password for accessing the system (optional for some tools)" )
                .build();
        passwordOpt.setRequired( required );

        options.addOption( usernameOpt );
        options.addOption( passwordOpt );
    }

    /**
     * Build option implementation.
     *
     * Implement this method to add options to your command line, using the OptionBuilder.
     *
     * This is called right after {@link #buildStandardOptions()} so the options will be added after standard options.
     */
    protected abstract void buildOptions();

    @SuppressWarnings("static-access")
    protected void buildStandardOptions() {
        AbstractCLI.log.debug( "Creating standard options" );
        Option helpOpt = new Option( "h", "help", false, "Print this message" );
        Option testOpt = new Option( "testing", false, "Use the test environment" );
        Option logOpt = new Option( "v", "verbosity", true,
                "Set verbosity level for all loggers (0=silent, 5=very verbose; default is custom, see log4j.properties)" );
        Option otherLogOpt = Option.builder().longOpt( "logger" ).hasArg().argName( "logger" ).desc( "Configure a specific logger verbosity"
                + "For example, '--logger ubic.gemma=5' or --logger log4j.logger.org.hibernate.SQL=5" )
                .build();

        options.addOption( otherLogOpt );
        options.addOption( logOpt );
        options.addOption( helpOpt );
        options.addOption( testOpt );

    }

    /**
     * Command line implementation.
     *
     * This is called after {@link #buildOptions()} and {@link #processOptions()}, so the implementation can assume that
     * all its arguments have already been initialized.
     *
     * @throws Exception in case of unrecoverable failure, an exception is thrown and will result in a {@link #FAILURE}
     *                   exit code, otherwise use {@link #addErrorObject}
     */
    protected abstract void doWork() throws Exception;

    protected final double getDoubleOptionValue( char option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( this.invalidOptionString( "" + option ) + ", not a valid double", e );
        }
    }

    protected final double getDoubleOptionValue( String option ) {
        try {
            return Double.parseDouble( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( this.invalidOptionString( option ) + ", not a valid double", e );
        }
    }

    protected final String getFileNameOptionValue( char c ) {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            throw new RuntimeException( this.invalidOptionString( "" + c ) + ", cannot read from file" );
        }
        return fileName;
    }

    protected final String getFileNameOptionValue( String c ) {
        String fileName = commandLine.getOptionValue( c );
        File f = new File( fileName );
        if ( !f.canRead() ) {
            throw new RuntimeException( this.invalidOptionString( "" + c ) + ", cannot read from file" );
        }
        return fileName;
    }

    protected final int getIntegerOptionValue( char option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( this.invalidOptionString( "" + option ) + ", not a valid integer", e );
        }
    }

    protected final int getIntegerOptionValue( String option ) {
        try {
            return Integer.parseInt( commandLine.getOptionValue( option ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( this.invalidOptionString( option ) + ", not a valid integer", e );
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

    protected void printHelp() {
        HelpFormatter h = new HelpFormatter();
        h.setWidth( 150 );
        h.printHelp( this.getCommandName() + " [options]", this.getShortDesc() + "\n" + AbstractCLI.HEADER, options,
                AbstractCLI.FOOTER );
    }

    /**
     * This must be called in your main method. It triggers parsing of the command line and processing of the options.
     * Check the error code to decide whether execution of your program should proceed.
     *
     * @param args args
     * @return Exception; null if nothing went wrong.
     */
    private final void processCommandLine( String[] args ) throws Exception {
        /* COMMAND LINE PARSER STAGE */
        DefaultParser parser = new DefaultParser();
        String appVersion = Settings.getAppVersion();
        if ( appVersion == null )
            appVersion = "?";
        System.err.println( "Gemma version " + appVersion );

        if ( args == null ) {
            this.printHelp();
            throw new Exception( "No arguments" );
        }

        try {
            commandLine = parser.parse( options, args );
        } catch ( ParseException e ) {
            if ( e instanceof MissingOptionException ) {
                System.err.println( "Required option(s) were not supplied: " + e.getMessage() );
            } else if ( e instanceof AlreadySelectedException ) {
                System.err.println( "The option(s) " + e.getMessage() + " were already selected" );
            } else if ( e instanceof MissingArgumentException ) {
                System.err.println( "Missing argument: " + e.getMessage() );
            } else if ( e instanceof UnrecognizedOptionException ) {
                System.err.println( "Unrecognized option: " + e.getMessage() );
            } else {
                e.printStackTrace();
            }

            this.printHelp();

            if ( AbstractCLI.log.isDebugEnabled() ) {
                AbstractCLI.log.debug( e );
            }

            throw e;
        }
    }

    /**
     * Process command line options.
     *
     * Implement this to provide processing of options. It is called after {@link #buildOptions()} and right before
     * {@link #doWork()}.
     *
     * @throws Exception in case of unrecoverable failure (i.e. missing option or invalid value), an exception can be
     *                   raised and will result in an exit code of {@link #FAILURE}.
     */
    protected abstract void processOptions() throws Exception;

    /**
     * Call in 'buildOptions' to force users to provide a user name and password.
     */
    protected void requireLogin() {
        if ( this.passwordOpt != null ) {
            this.passwordOpt.setRequired( true );
        }
        if ( this.usernameOpt != null ) {
            this.usernameOpt.setRequired( true );
        }
    }

    /**
     * This is needed for CLIs that run in tests, so the logging settings get reset.
     */
    protected static void resetLogging() {
        for ( Logger log4jLogger : originalLoggingLevels.keySet() ) {
            log4jLogger.setLevel( originalLoggingLevels.get( log4jLogger ) );
        }
    }

    /**
     * Add a success object to indicate success in a batch processing.
     *
     * This is further used in {@link #summarizeProcessing()} to summarize the execution of the command.
     *
     * @param successObject object that was processed
     * @param message       success message
     */
    protected void addSuccessObject( Object successObject, String message ) {
        successObjects.add( new BatchProcessingResult( successObject, message ) );
        log.info( successObject + ": " + message );
    }

    /**
     * Add an error object with a stacktrace to indicate failure in a batch processing.
     *
     * This is further used in {@link #summarizeProcessing()} to summarize the execution of the command.
     *
     * This is intended to be used when an {@link Exception} is caught.
     *
     * @param errorObject object that was processed
     * @param message     error message
     * @param throwable   throwable to produce a stacktrace
     */
    protected void addErrorObject( Object errorObject, String message, Throwable throwable ) {
        errorObjects.add( new BatchProcessingResult( errorObject, message ) );
        log.error( errorObject + ": " + message, throwable );
    }

    /**
     * Add an error object to indicate failure in a batch processing.
     *
     * This is further used in {@link #summarizeProcessing()} to summarize the execution of the command.
     */
    protected void addErrorObject( Object errorObject, String message ) {
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
     * Execute batch tasks using a preconfigured {@link ExecutorService} and return all the resulting tasks results.
     *
     * @param tasks
     * @throws InterruptedException
     */
    protected <T> List<T> executeBatchTasks( Collection<? extends Callable<T>> tasks ) throws InterruptedException {
        List<Future<T>> futures = executorService.invokeAll( tasks );
        List<T> futureResults = new ArrayList<>( futures.size() );
        for ( Future<T> future : futures ) {
            try {
                futureResults.add( future.get() );
            } catch ( ExecutionException | InterruptedException e ) {
                addErrorObject( null, "Batch task failed.", e );
            }
        }
        return futureResults;
    }

    private void configureAllLoggers( int v ) {
        Enumeration<?> currentLoggers = LogManager.getLoggerRepository().getCurrentLoggers();
        while ( currentLoggers.hasMoreElements() ) {
            Logger logger = ( Logger ) currentLoggers.nextElement();
            this.setLoggerLevel( v, logger );
        }
        this.configureLogging( "net", v );
        this.configureLogging( "org", v );
        this.configureLogging( "com", v );
        this.configureLogging( "ubic", v );
        this.configureLogging( "gemma", v );
    }

    /**
     * Set up logging according to the user-selected (or default) verbosity level.
     */
    private void configureLogging( String loggerName, int v ) {

        Logger log4jLogger = LogManager.exists( loggerName );

        if ( log4jLogger == null ) {
            log4jLogger = LogManager.getLogger( loggerName );
        }

        // if logger name is nonsense this will not do anything.
        AbstractCLI.log.info( "Setting logging for " + loggerName + " to " + v );
        originalLoggingLevels.put( log4jLogger, log4jLogger.getLevel() );

        this.setLoggerLevel( v, log4jLogger );

        AbstractCLI.log.debug( "Logging level is at " + log4jLogger.getEffectiveLevel() );
    }

    private String invalidOptionString( String option ) {
        return "Invalid value '" + commandLine.getOptionValue( option ) + " for option " + option;
    }

    /**
     * Somewhat annoying: This causes subclasses to be unable to safely use 'h', 'p', 'u' and 'P' etc for their own
     * purposes.
     */
    private void processStandardOptions() {

        if ( commandLine.hasOption( AbstractCLI.HOST_OPTION ) ) {
            this.host = commandLine.getOptionValue( AbstractCLI.HOST_OPTION );
        } else {
            this.host = DEFAULT_HOST;
        }

        if ( commandLine.hasOption( AbstractCLI.PORT_OPTION ) ) {
            this.port = this.getIntegerOptionValue( AbstractCLI.PORT_OPTION );
        } else {
            this.port = AbstractCLI.DEFAULT_PORT;
        }

        if ( commandLine.hasOption( AbstractCLI.USERNAME_OPTION ) ) {
            this.username = commandLine.getOptionValue( AbstractCLI.USERNAME_OPTION );
        }

        if ( commandLine.hasOption( AbstractCLI.PASSWORD_CONSTANT ) ) {
            this.password = commandLine.getOptionValue( AbstractCLI.PASSWORD_CONSTANT );
        }

        if ( commandLine.hasOption( AbstractCLI.VERBOSITY_OPTION ) ) {
            int verbosity = this.getIntegerOptionValue( AbstractCLI.VERBOSITY_OPTION );
            if ( verbosity < 0 || verbosity > 5 ) {
                throw new RuntimeException( "Verbosity must be from 0 to 5" );
            }
            this.configureAllLoggers( verbosity );
        }
        PatternLayout layout = new PatternLayout( "[Gemma %d] %p [%t] %C.%M(%L) | %m%n" );
        ConsoleAppender cnslAppndr = new ConsoleAppender( layout );
        Logger f = LogManager.getRootLogger();
        assert f != null;
        f.addAppender( cnslAppndr );

        if ( commandLine.hasOption( AbstractCLI.LOGGER_OPTION ) ) {
            String value = this.getOptionValue( AbstractCLI.LOGGER_OPTION );
            String[] vals = value.split( "=" );
            if ( vals.length != 2 )
                throw new RuntimeException( "Logging value must in format [logger]=[value]" );
            try {
                this.configureLogging( vals[0], Integer.parseInt( vals[1] ) );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Logging level must be an integer between 0 and 5" );
            }
        }

        if ( this.hasOption( "mdate" ) ) {
            this.mDate = this.getOptionValue( "mdate" );
        }

        if ( this.numThreads < 1 ) {
            throw new IllegalArgumentException( "Number of threads must be greater than 1." );
        }
        this.executorService = new ForkJoinPool( this.numThreads );
    }

    private void setLoggerLevel( int level, Logger log4jLogger ) {
        switch ( level ) {
            case 0:
                log4jLogger.setLevel( Level.OFF );
                break;
            case 1:
                log4jLogger.setLevel( Level.FATAL );
                break;
            case 2:
                log4jLogger.setLevel( Level.ERROR );
                break;
            case 3:
                log4jLogger.setLevel( Level.WARN );
                break;
            case 4:
                log4jLogger.setLevel( Level.INFO );
                break;
            case 5:
                log4jLogger.setLevel( Level.DEBUG );
                break;
            default:
                throw new RuntimeException( "Verbosity must be from 0 to 5" );

        }
    }

    /**
     * Represents an individual result in a batch processing.
     */
    private static class BatchProcessingResult {
        private Object source;
        private String message;
        private Throwable throwable;

        public BatchProcessingResult( Object source, String message ) {
            this.source = source;
            this.message = message;
        }

        public BatchProcessingResult( Object source, String message, Throwable throwable ) {
            this.source = source;
            this.message = message;
            this.throwable = throwable;
        }
    }
}
