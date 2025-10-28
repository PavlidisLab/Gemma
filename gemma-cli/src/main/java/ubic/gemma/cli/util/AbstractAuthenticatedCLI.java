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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Subclass this to create command line interface (CLI) tools that need authentication.
 * <p>
 * Credentials may be supplied via the environment using the {@code $GEMMA_USERNAME} and {@code $GEMMA_PASSWORD}
 * variables. A more secure {@code $GEMMA_PASSWORD_CMD} variable can be used to specify a command that produces the
 * password. If no environment variables are supplied, they will be prompted if the standard input is attached to a
 * console (i.e tty).
 * <p>
 * If the {@code test} or {@code testdb} profile is active, environment variables with the {@code $GEMMA_TESTDB_} prefix
 * will be looked up instead.
 *
 * @author pavlidis
 */
public abstract class AbstractAuthenticatedCLI extends AbstractCLI implements InitializingBean, EnvironmentAware {

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

    private String
            usernameEnv = USERNAME_ENV,
            passwordEnv = PASSWORD_ENV,
            passwordCmdEnv = PASSWORD_CMD_ENV;

    private boolean requireLogin = false;

    private boolean authenticateAnonymously = false;

    private Environment environment;

    @Override
    public void afterPropertiesSet() throws Exception {
        if ( environment.acceptsProfiles( "test", "testdb" ) ) {
            log.info( "The test or testdb profile is active, using test credentials from environment variables starting with 'GEMMA_TESTDB_'." );
            usernameEnv = "GEMMA_TESTDB_USERNAME";
            passwordEnv = "GEMMA_TESTDB_PASSWORD";
            passwordCmdEnv = "GEMMA_TESTDB_PASSWORD_CMD";
        }
    }

    @Override
    public void setEnvironment( Environment environment ) {
        this.environment = environment;
    }

    /**
     * Indicate that the command requires authentication.
     */
    protected void setRequireLogin() {
        Assert.state( !authenticateAnonymously, "Cannot set both requireLogin and authenticateAnonymously to true." );
        this.requireLogin = true;
    }

    /**
     * Indicate that the command should authenticate anonymously, i.e. without requiring a username and password.
     * <p>
     * This can be used for commands that are intended to access public data.
     */
    protected void setAuthenticateAnonymously() {
        Assert.state( !requireLogin, "Cannot set both requireLogin and authenticateAnonymously to true." );
        this.authenticateAnonymously = true;
    }

    @Override
    protected final void doWork() throws Exception {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication( authenticate() );
            SecurityContextHolder.setContext( context );
            doAuthenticatedWork();
        } finally {
            SecurityContextHolder.clearContext();
            SecurityContextHolder.setContext( previousContext );
        }
    }

    protected abstract void doAuthenticatedWork() throws Exception;

    /**
     * Perform authentication from the CLI.
     */
    private Authentication authenticate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( authentication != null && authentication.isAuthenticated() ) {
            // already authenticated
            log.info( String.format( "Logged in as %s", authentication.getPrincipal() ) );
            return authentication;
        } else if ( authenticateAnonymously ) {
            Authentication anonymousAuthentication = getApplicationContext()
                    .getBean( CLIAuthenticationManager.class )
                    .authenticateAnonymously();
            log.info( "Logged in as anonymous guest with limited privileges" );
            return anonymousAuthentication;
        } else if ( requireLogin || getCliContext().getEnvironment().containsKey( usernameEnv ) ) {
            String username = getUsername();
            String password = getPassword();

            if ( StringUtils.isBlank( username ) ) {
                throw new IllegalArgumentException( "Not authenticated. Username was blank" );
            }

            if ( StringUtils.isBlank( password ) ) {
                throw new IllegalArgumentException( "Not authenticated. You didn't enter a password" );
            }

            Authentication auth = getApplicationContext()
                    .getBean( CLIAuthenticationManager.class )
                    .authenticate( username, password );
            log.info( "Logged in as " + username );
            return auth;
        } else {
            Authentication anonymousAuthentication = getApplicationContext()
                    .getBean( CLIAuthenticationManager.class )
                    .authenticateAnonymously();
            log.info( "Logged in as anonymous guest with limited privileges" );
            return anonymousAuthentication;
        }
    }

    private String getUsername() {
        if ( getCliContext().getEnvironment().containsKey( usernameEnv ) ) {
            return getCliContext().getEnvironment().get( usernameEnv );
        } else if ( getCliContext().getConsole() != null ) {
            log.warn( "The " + usernameEnv + " environment variable is not set, consider setting it to skip this prompt." );
            return getCliContext().getConsole().readLine( "Username: " );
        } else {
            throw new RuntimeException( String.format( "Could not read the username from the console. Please run Gemma CLI from an interactive shell or provide the %s environment variable.", usernameEnv ) );
        }
    }

    private String getPassword() {
        if ( getCliContext().getEnvironment().containsKey( passwordEnv ) ) {
            return getCliContext().getEnvironment().get( passwordEnv );
        }

        if ( getCliContext().getEnvironment().containsKey( passwordCmdEnv ) ) {
            String passwordCommand = getCliContext().getEnvironment().get( passwordCmdEnv );
            try {
                Process proc = Runtime.getRuntime().exec( passwordCommand );
                if ( proc.waitFor() == 0 ) {
                    try ( BufferedReader reader = new BufferedReader( new InputStreamReader( proc.getInputStream() ) ) ) {
                        return reader.readLine();
                    }
                } else {
                    throw new IllegalArgumentException( "Could not read the password from '" + passwordCommand + "':\n"
                            + String.join( "\n", IOUtils.readLines( proc.getErrorStream(), StandardCharsets.UTF_8 ) ) );
                }
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new RuntimeException( e );
            } catch ( IOException e ) {
                throw new IllegalArgumentException( "Could not read the password from '" + passwordCommand + "'.", e );
            }
        }

        // prompt the user for his password
        if ( getCliContext().getConsole() != null ) {
            log.warn( "Neither " + passwordEnv + " nor " + passwordCmdEnv + " environment variables are set, consider setting one of those to skip this prompt." );
            return String.valueOf( getCliContext().getConsole().readPassword( "Password: " ) );
        } else {
            throw new IllegalArgumentException( String.format( "Could not read the password from the console. Please run Gemma CLI from an interactive shell or provide either the %s or %s environment variables.",
                    passwordEnv, passwordCmdEnv ) );
        }
    }
}
