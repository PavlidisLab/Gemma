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

import gemma.gsec.authentication.ManualAuthenticationService;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.persistence.util.Settings;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Subclass this to create command line interface (CLI) tools that need a Spring context. A standard set of CLI options
 * are provided to manage authentication.
 *
 * @author pavlidis
 */
public abstract class AbstractCLI implements CLI {

    protected static final Logger log = LogManager.getLogger( AbstractCLI.class );


    /**
     * Environment variable used to store the username (if not passed directly to the CLI).
     */
    private static final String USERNAME_ENV = "GEMMA_USERNAME";

    /**
     * Environment variable used to store the user password.
     */
    private static final String PASSWORD_ENV = "GEMMA_PASSWORD";

    /**
     * Environment variable used to store the command that produces the password.
     */
    private static final String PASSWORD_CMD_ENV = "GEMMA_PASSWORD_CMD";
    private static final String HELP_OPTION = "h";
    private static final String TESTING_OPTION = "testing";

    @Autowired
    private BeanFactory ctx;
    @Autowired
    private ManualAuthenticationService manAuthentication;

    /**
     * If this CLI failed, this holds the corresponding exception.
     */
    private Exception lastException = null;

    /**
     * Indicate if the command requires authentication.
     * <p>
     * Override this to return true to make authentication required.
     *
     * @return true if login is required, otherwise false
     */
    protected boolean requireLogin() {
        return false;
    }

    /**
     * @deprecated Use {@link Autowired} to specify your dependencies, this is just a wrapper around the current
     * {@link BeanFactory}.
     */
    @Deprecated
    protected <T> T getBean( Class<T> clz ) {
        return ctx.getBean( clz );
    }

    /**
     * check username and password.
     */
    private void authenticate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( authentication != null && authentication.isAuthenticated() ) {
            AbstractCLI.log.info( String.format( "Logged in as %s", authentication.getPrincipal() ) );
        } else if ( requireLogin() || System.getenv().containsKey( USERNAME_ENV ) ) {
            String username = getUsername();
            String password = getPassword();

            if ( StringUtils.isBlank( username ) ) {
                throw new IllegalArgumentException( "Not authenticated. Username was blank" );
            }

            if ( StringUtils.isBlank( password ) ) {
                throw new IllegalArgumentException( "Not authenticated. You didn't enter a password" );
            }

            boolean success = manAuthentication.validateRequest( username, password );
            if ( !success ) {
                throw new IllegalStateException( "Not authenticated. Make sure you entered a valid username (got '" + username
                        + "') and/or password" );
            } else {
                AbstractCLI.log.info( "Logged in as " + username );
            }
        } else {
            AbstractCLI.log.info( "Logging in as anonymous guest with limited privileges" );
            manAuthentication.authenticateAnonymously();
        }
    }

    private String getUsername() {
        if ( System.getenv().containsKey( USERNAME_ENV ) ) {
            return System.getenv().get( USERNAME_ENV );
        } else if ( System.console() != null ) {
            return System.console().readLine( "Username: " );
        } else {
            throw new RuntimeException( String.format( "Could not read the username from the console. Please run Gemma CLI from an interactive shell or provide the %s environment variable.", USERNAME_ENV ) );
        }
    }

    private String getPassword() {
        if ( System.getenv().containsKey( PASSWORD_ENV ) ) {
            return System.getenv().get( PASSWORD_ENV );
        }

        if ( System.getenv().containsKey( PASSWORD_CMD_ENV ) ) {
            String passwordCommand = System.getenv().get( PASSWORD_CMD_ENV );
            try {
                Process proc = Runtime.getRuntime().exec( passwordCommand );
                if ( proc.waitFor() == 0 ) {
                    try ( BufferedReader reader = new BufferedReader( new InputStreamReader( proc.getInputStream() ) ) ) {
                        return reader.readLine();
                    }
                } else {
                    log.error( "Could not read the password from '" + passwordCommand + "':\n"
                            + String.join( "\n", IOUtils.readLines( proc.getErrorStream(), StandardCharsets.UTF_8 ) ) );
                    throw new IllegalArgumentException( "Could not read the password from '" + passwordCommand + "'." );
                }
            } catch ( IOException | InterruptedException e ) {
                log.error( "Could not read the password from '" + passwordCommand + "'.", e );
                throw new IllegalArgumentException( "Could not read the password from '" + passwordCommand + "'.", e );
            }
        }

        // prompt the user for his password
        if ( System.console() != null ) {
            return String.valueOf( System.console().readPassword( "Password: " ) );
        } else {
            throw new IllegalArgumentException( String.format( "Could not read the password from the console. Please run Gemma CLI from an interactive shell or provide either the %s or %s environment variables.",
                    PASSWORD_ENV, PASSWORD_CMD_ENV ) );
        }
    }

    /**
     * Run the command.
     * <p>
     * Parse and process CLI arguments, invoke the command doWork implementation, and print basic statistics about time
     * usage.
     *
     * @param args Arguments to pass to {@link #processCommandLine(Options, String[])}
     * @return Exit code intended to be used with {@link System#exit(int)} to indicate a success or failure to the
     * end-user. Any exception raised by doWork results in a value of {@link #FAILURE}.
     */
    @Override
    public int executeCommand( String[] args ) {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Options options = new Options();
            AbstractCLI.log.debug( "Creating standard options" );
            options.addOption( new Option( HELP_OPTION, "help", false, "Print this message" ) );
            options.addOption( new Option( TESTING_OPTION, "testing", false, "Use the test environment. This option must be passed before the command." ) );
            buildOptions( options );
            CommandLine commandLine = processCommandLine( options, args );
            // check if -h/--help is provided before pursuing option processing
            if ( commandLine.hasOption( 'h' ) ) {
                printHelp( options );
                return CLI.SUCCESS;
            }
            if ( commandLine.hasOption( TESTING_OPTION ) ) {
                System.err.printf( "The -testing/--testing option must be passed before the %s command.%n", getCommandName() );
                return CLI.FAILURE;
            }
            this.authenticate();
            processOptions( commandLine );
            doWork();
            return CLI.SUCCESS;
        } catch ( Exception e ) {
            lastException = e;
            log.error( getCommandName() + " failed.", e );
            return CLI.FAILURE;
        } finally {
            AbstractCLI.log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds." );
        }
    }

    @Override
    @Nullable
    public Exception getLastException() {
        return lastException;
    }

    /**
     * Build option implementation.
     * <p>
     * Implement this method to add options to your command line, using the OptionBuilder.
     */
    protected abstract void buildOptions( Options options );

    /**
     * Process command line options.
     * <p>
     * Implement this to provide processing of options. It is called after {@link #buildOptions(Options)} and right
     * before {@link #doWork()}.
     *
     * @throws ParseException if the parsing of the command line exception fails
     */
    protected abstract void processOptions( CommandLine commandLine ) throws ParseException;

    /**
     * Command line implementation.
     * <p>
     * This is called after {@link #buildOptions(Options)} and {@link #processOptions(CommandLine)}, so the implementation can assume that
     * all its arguments have already been initialized.
     *
     * @throws Exception in case of unrecoverable failure, an exception is thrown and will result in a {@link #FAILURE}
     *                   exit code
     */
    protected abstract void doWork() throws Exception;

    private void printHelp( Options options ) {
        new HelpFormatter().printHelp( new PrintWriter( System.err, true ), 150,
                this.getCommandName() + " [options]",
                ( this.getShortDesc() != null ? this.getShortDesc() + "\n" : "" ) + "Options:",
                options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, COPYRIGHT_NOTICE );
    }

    /**
     * This must be called in your main method. It triggers parsing of the command line and processing of the options.
     * Check the error code to decide whether execution of your program should proceed.
     *
     * @param args args
     * @return Exception; null if nothing went wrong.
     */
    private CommandLine processCommandLine( Options options, String[] args ) throws Exception {
        /* COMMAND LINE PARSER STAGE */
        DefaultParser parser = new DefaultParser();
        String appVersion = Settings.getAppVersion();
        if ( appVersion == null )
            appVersion = "?";
        System.err.println( "Gemma version " + appVersion );

        if ( args == null ) {
            this.printHelp( options );
            throw new Exception( "No arguments" );
        }

        try {
            return parser.parse( options, args );
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

            this.printHelp( options );

            if ( AbstractCLI.log.isDebugEnabled() ) {
                AbstractCLI.log.debug( e );
            }

            throw e;
        }
    }
}
