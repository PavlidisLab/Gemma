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

import com.google.common.base.Charsets;
import gemma.gsec.authentication.ManualAuthenticationService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
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
import ubic.gemma.persistence.util.SpringContextUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

/**
 * Subclass this to create command line interface (CLI) tools that need a Spring context. A standard set of CLI options
 * are provided to manage authentication.
 *
 * @author pavlidis
 */
public abstract class AbstractSpringAwareCLI extends AbstractCLI {

    private static final String USERNAME_OPTION = "u";
    private static final String PASSWORD_OPTION = "p";

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

    protected AuditTrailService auditTrailService;
    protected AuditEventService auditEventService;
    protected BeanFactory ctx;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public AbstractSpringAwareCLI() {
        super();
    }

    @Override
    public String getShortDesc() {
        return "No description provided";
    }

    @Override
    protected void buildStandardOptions( Options options ) {
        super.buildStandardOptions( options );
        options.addOption( Option.builder( USERNAME_OPTION ).argName( "user" ).longOpt( "user" ).hasArg()
                .desc( "User name for accessing the system (optional for some tools)" )
                .build() );
        options.addOption( Option.builder( PASSWORD_OPTION ).argName( "passwd" ).longOpt( "password" ).hasArg()
                .desc( "Password for accessing the system (optional for some tools)" )
                .build() );
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
     * @param commandLine
     */
    @Override
    protected void processStandardOptions( CommandLine commandLine ) {
        super.processStandardOptions( commandLine );
        this.createSpringContext( commandLine );
        this.authenticate( commandLine );
        this.auditTrailService = this.getBean( AuditTrailService.class );
        this.auditEventService = this.getBean( AuditEventService.class );
    }

    @SuppressWarnings("unused") // Possible external use
    public void setCtx( BeanFactory ctx ) {
        this.ctx = ctx;
    }

    /**
     * Convenience method to obtain instance of any bean by name.
     *
     * @param <T>  the bean class type
     * @param clz  class
     * @param name name
     * @return bean
     */
    @SuppressWarnings("SameParameterValue") // Better for general use
    protected <T> T getBean( String name, Class<T> clz ) {
        assert ctx != null : "Spring context was not initialized";
        return ctx.getBean( name, clz );
    }

    protected <T> T getBean( Class<T> clz ) {
        assert ctx != null : "Spring context was not initialized";
        return ctx.getBean( clz );
    }

    protected Persister getPersisterHelper() {
        assert ctx != null : "Spring context was not initialized";
        return ( Persister ) ctx.getBean( "persisterHelper" );
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
                ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );
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
     * check if using test or production contexts
     * @param commandLine
     */
    protected void createSpringContext( CommandLine commandLine ) {

        ctx = SpringContextUtil.getApplicationContext( commandLine.hasOption( "testing" ), false, null );

        /*
         * Guarantee that the security settings are uniform throughout the application (all threads).
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_GLOBAL );
    }

    /**
     * check username and password.
     * @param commandLine
     */
    private void authenticate( CommandLine commandLine ) {

        /*
         * Allow security settings (authorization etc) in a given context to be passed into spawned threads
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_GLOBAL );

        ManualAuthenticationService manAuthentication = ctx.getBean( ManualAuthenticationService.class );
        if ( requireLogin() || commandLine.hasOption( USERNAME_OPTION ) || System.getenv().containsKey( USERNAME_ENV ) ) {
            String username = getUsername( commandLine );
            String password = getPassword( commandLine );

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

    private String getUsername( CommandLine commandLine ) {
        if ( commandLine.hasOption( USERNAME_OPTION ) ) {
            return commandLine.getOptionValue( USERNAME_OPTION );
        } else if ( System.getenv().containsKey( USERNAME_ENV ) ) {
            return System.getenv().get( USERNAME_ENV );
        } else {
            return System.console().readLine( "Username: " );
        }
    }

    private String getPassword( CommandLine commandLine ) {
        if ( commandLine.hasOption( PASSWORD_OPTION ) ) {
            log.warn( "Using the " + PASSWORD_OPTION + " CLI option is highly discouraged. Consider instead passing  a " + PASSWORD_ENV + " or " + PASSWORD_CMD_ENV + " environment variable." );
            return commandLine.getOptionValue( PASSWORD_OPTION );
        }

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
                            + String.join( "\n", IOUtils.readLines( proc.getErrorStream(), Charsets.UTF_8 ) ) );
                    throw new IllegalArgumentException( "Could not read the password from '" + passwordCommand + "'." );
                }
            } catch ( IOException | InterruptedException e ) {
                log.error( "Could not read the password from '" + passwordCommand + "'.", e );
                throw new IllegalArgumentException( "Could not read the password from '" + passwordCommand + "'.", e );
            }
        }

        // prompt the user for his password
        return String.valueOf( System.console().readPassword( "Password: " ) );
    }
}