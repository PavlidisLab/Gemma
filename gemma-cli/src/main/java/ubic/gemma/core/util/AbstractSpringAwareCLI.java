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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Subclass this to create command line interface (CLI) tools that need a Spring context. A standard set of CLI options
 * are provided to manage authentication.
 *
 * @author pavlidis
 */
public abstract class AbstractSpringAwareCLI extends AbstractCLI {

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
    private BeanFactory ctx;
    @Autowired
    private ManualAuthenticationService manAuthentication;
    @Autowired
    protected AuditTrailService auditTrailService;
    @Autowired
    protected AuditEventService auditEventService;
    @Autowired
    private ExpressionExperimentService ees;
    @Autowired
    private Persister persisterHelper;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    @Autowired
    public AbstractSpringAwareCLI() {
        super();
    }

    @Override
    public String getShortDesc() {
        return "No description provided";
    }

    /**
     * Indicate if the command requires authentication.
     *
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
    protected void processStandardOptions( CommandLine commandLine ) {
        super.processStandardOptions( commandLine );
        this.authenticate();
    }

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

    @Deprecated
    protected Persister getPersisterHelper() {
        return persisterHelper;
    }

    /**
     * @param auditable  auditable
     * @param eventClass can be null
     * @return boolean
     */
    protected boolean noNeedToRun( Auditable auditable, Class<? extends AuditEventType> eventClass ) {
        boolean needToRun = true;
        Date skipIfLastRunLaterThan = this.getLimitingDate();
        List<AuditEvent> events = this.auditEventService.getEvents( auditable );

        boolean okToRun = true; // assume okay unless indicated otherwise

        // figure out if we need to run it by date; or if there is no event of the given class; "Fail" type events don't
        // count.
        for ( int j = events.size() - 1; j >= 0; j-- ) {
            AuditEvent event = events.get( j );
            if ( event == null ) {
                continue; // legacy of ordered-list which could end up with gaps; should not be needed any more
            }
            AuditEventType eventType = event.getEventType();
            if ( eventType != null && eventClass != null && eventClass.isAssignableFrom( eventType.getClass() )
                    && !eventType.getClass().getSimpleName().startsWith( "Fail" ) ) {
                if ( skipIfLastRunLaterThan != null ) {
                    if ( event.getDate().after( skipIfLastRunLaterThan ) ) {
                        AbstractCLI.log.info( auditable + ": " + " run more recently than " + skipIfLastRunLaterThan );
                        addErrorObject( auditable, "Run more recently than " + skipIfLastRunLaterThan );
                        needToRun = false;
                    }
                } else {
                    needToRun = false; // it has been run already at some point
                }
            }
        }

        /*
         * Always skip if the object is curatable and troubled
         */
        if ( auditable instanceof Curatable ) {
            Curatable curatable = ( Curatable ) auditable;
            okToRun = !curatable.getCurationDetails().getTroubled(); //not ok if troubled

            // special case for expression experiments - check associated ADs.
            if ( okToRun && curatable instanceof ExpressionExperiment ) {
                for ( ArrayDesign ad : ees.getArrayDesignsUsed( ( ExpressionExperiment ) auditable ) ) {
                    if ( ad.getCurationDetails().getTroubled() ) {
                        okToRun = false; // not ok if even one parent AD is troubled, no need to check the remaining ones.
                        break;
                    }
                }
            }

            if ( !okToRun ) {
                addErrorObject( auditable, "Has an active 'trouble' flag" );
            }
        }

        return !needToRun || !okToRun;
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
