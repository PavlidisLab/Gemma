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
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private String username;
    private String password;

    @Autowired
    protected AuditTrailService auditTrailService;
    @Autowired
    protected AuditEventService auditEventService;
    @Autowired
    protected BeanFactory ctx;
    @Autowired
    private ExpressionExperimentService ees;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    @Autowired
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
                .required( requireLogin() )
                .build() );
        options.addOption( Option.builder( PASSWORD_OPTION ).argName( "passwd" ).longOpt( "password" ).hasArg()
                .desc( "Password for accessing the system (optional for some tools)" )
                .required( requireLogin() )
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
        if ( commandLine.hasOption( USERNAME_OPTION ) ) {
            this.username = commandLine.getOptionValue( USERNAME_OPTION );
        }
        if ( commandLine.hasOption( PASSWORD_OPTION ) ) {
            this.password = commandLine.getOptionValue( PASSWORD_OPTION );
        }
        this.authenticate( commandLine );
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
     * @param commandLine
     */
    private void authenticate( CommandLine commandLine ) {

        /*
         * Allow security settings (authorization etc) in a given context to be passed into spawned threads
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_GLOBAL );

        ManualAuthenticationService manAuthentication = ctx.getBean( ManualAuthenticationService.class );
        if ( commandLine.hasOption( 'u' ) && commandLine.hasOption( 'p' ) ) {
            username = commandLine.getOptionValue( 'u' );
            password = commandLine.getOptionValue( 'p' );

            if ( StringUtils.isBlank( username ) ) {
                AbstractCLI.log.debug( "Username=" + username );
                throw new RuntimeException( "Not authenticated. Username was blank" );
            }

            if ( StringUtils.isBlank( password ) ) {
                throw new RuntimeException( "Not authenticated. You didn't enter a password" );
            }

            boolean success = manAuthentication.validateRequest( username, password );
            if ( !success ) {
                throw new RuntimeException( "Not authenticated. Make sure you entered a valid username (got '" + username
                        + "') and/or password" );
            } else {
                AbstractCLI.log.info( "Logged in as " + username );
            }
        } else {
            AbstractCLI.log.info( "Logging in as anonymous guest with limited privileges" );
            manAuthentication.authenticateAnonymously();
        }

    }

}
