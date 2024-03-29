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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Subclass this to create command line interface (CLI) tools that need authentication.
 * <p>
 * Credentials may be supplied via the environment using the {@code $GEMMMA_USERNAME} and {@code $GEMMA_PASSWORD}
 * variables. A more secure {@code $GEMMA_PASSWORD_CMD} variable can be used to specify a command that produces the
 * password. If no environment variables are supplied, they will be prompted if the standard input is attached to a
 * console (i.e tty).
 * @author pavlidis
 */
public abstract class AbstractAuthenticatedCLI extends AbstractCLI {

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

    @Autowired
    private ManualAuthenticationService manAuthentication;

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
     * You must override this method to process any options you added.
     */
    @Override
    protected void processStandardOptions( CommandLine commandLine ) throws ParseException {
        super.processStandardOptions( commandLine );
        this.authenticate();
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
                    throw new IllegalArgumentException( "Could not read the password from '" + passwordCommand + "':\n"
                            + String.join( "\n", IOUtils.readLines( proc.getErrorStream(), StandardCharsets.UTF_8 ) ) );
                }
            } catch ( IOException | InterruptedException e ) {
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
     * {@inheritDoc}
     * <p>
     * Tasks are wrapped with {@link DelegatingSecurityContextCallable} to ensure that they execute with the security
     * context set up by {@link #authenticate()}.
     */
    @Override
    protected <T> List<T> executeBatchTasks( Collection<? extends Callable<T>> tasks ) throws InterruptedException {
        return super.executeBatchTasks( tasks.stream()
                .map( DelegatingSecurityContextCallable::new )
                .collect( Collectors.toList() ) );
    }
}
