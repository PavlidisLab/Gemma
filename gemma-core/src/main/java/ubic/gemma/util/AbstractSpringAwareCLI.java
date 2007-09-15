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
package ubic.gemma.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.quartz.impl.StdScheduler;
import org.springframework.beans.factory.BeanFactory;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.security.authentication.ManualAuthenticationProcessing;

/**
 * Subclass this to create command line interface (CLI) tools that need a Spring context. A standard set of CLI options
 * are provided to manage authentication.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractSpringAwareCLI extends AbstractCLI {

    private static final String COMPASS_ON = "compassOn";

    private static final String GIGASPACES_ON = "gigaspacesOn";

    protected BeanFactory ctx = null;
    PersisterHelper ph = null;
    protected AuditTrailService auditTrailService;
    protected Collection<Exception> exceptionCache = new ArrayList<Exception>();

    @Override
    protected void buildStandardOptions() {
        super.buildStandardOptions();
        addUserNameAndPasswordOptions();
    }

    public AbstractSpringAwareCLI() {
        super();

        CompassUtils.deleteCompassLocks();

    }

    /**
     * @param fileName
     * @return Given a file name returns a collection of strings. Each string represents one line of the file
     */
    protected Collection<String> processFile( String fileName ) {

        Collection<String> lines = new ArrayList<String>();
        int lineNumber = 0;
        try {

            InputStream is = new FileInputStream( fileName );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

            String line = null;

            while ( ( line = br.readLine() ) != null ) {
                lineNumber++;
                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }
                lines.add( line.trim().toUpperCase() );
            }
        } catch ( IOException ioe ) {
            log.error( "At line: " + lineNumber + " an error occured processing " + fileName + ". \n Error is: " + ioe );
        }

        return lines;
    }

    /**
     * @return
     */
    protected PersisterHelper getPersisterHelper() {
        if ( ph != null ) {
            return ph;
        }

        assert ctx != null : "Spring context was not initialized";
        return ( PersisterHelper ) ctx.getBean( "persisterHelper" );

    }

    /**
     * @param auditable
     * @param eventClass
     * @return
     */
    protected boolean needToRun( Auditable auditable, Class<? extends AuditEventType> eventClass ) {
        boolean needToRun = true;
        Date skipIfLastRunLaterThan = getLimitingDate();

        List<AuditEvent> events = ( List<AuditEvent> ) auditable.getAuditTrail().getEvents();

        boolean okToRun = true; // assume okay unless indicated otherwise

        // figure out if we need to run it by date
        if ( skipIfLastRunLaterThan != null ) {
            for ( int j = events.size() - 1; j >= 0; j-- ) {
                AuditEvent event = events.get( j );
                if ( event.getEventType() != null && eventClass.isAssignableFrom( event.getEventType().getClass() ) ) {
                    if ( event.getDate().after( skipIfLastRunLaterThan ) ) {
                        errorObjects.add( auditable + ": " + " run more recently than " + skipIfLastRunLaterThan );
                        needToRun = false;
                    }
                }
            }
        }

        /*
         * If we're running in an 'auto' mode (by date, or with the --auto option, skip if we're in a 'trouble' state.
         */
        if ( autoSeek || skipIfLastRunLaterThan != null ) {
            AuditEvent lastTrouble = this.auditTrailService.getLastTroubleEvent( auditable );

            // special case for expression experiments - check associated ADs.
            if ( lastTrouble == null && auditable instanceof ExpressionExperiment ) {
                ExpressionExperimentService ees = ( ExpressionExperimentService ) this
                        .getBean( "expressionExperimentService" );
                for ( Object o : ees.getArrayDesignsUsed( ( ExpressionExperiment ) auditable ) ) {
                    lastTrouble = auditTrailService.getLastTroubleEvent( ( ArrayDesign ) o );
                }
            }

            okToRun = lastTrouble == null;
        }

        if ( !okToRun ) {
            errorObjects.add( auditable + ": has an active 'trouble' flag" );
        }

        return needToRun && okToRun;
    }

    /** check username and password. */
    void authenticate() {

        if ( hasOption( 'u' ) && hasOption( 'p' ) ) {
            username = getOptionValue( 'u' );
            password = getOptionValue( 'p' );

            if ( StringUtils.isBlank( username ) ) {
                System.err.println( "Not authenticated. Username was blank" );
                log.debug( "Username=" + username );
                bail( ErrorCode.AUTHENTICATION_ERROR );
            }

            if ( StringUtils.isBlank( password ) ) {
                System.err.println( "Not authenticated. You didn't enter a password" );
                bail( ErrorCode.AUTHENTICATION_ERROR );
            }

            ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctx
                    .getBean( "manualAuthenticationProcessing" );

            boolean success = manAuthentication.validateRequest( username, password );
            if ( !success ) {
                System.err.println( "Not authenticated. Make sure you entered a valid username (got " + username
                        + ") and/or password" );
                bail( ErrorCode.AUTHENTICATION_ERROR );
            } else {
                log.info( "Logged in as " + username );
            }
        } else {

            System.err.println( "Not authenticated. Make sure you entered a valid username (got " + username
                    + ") and/or password" );
            bail( ErrorCode.AUTHENTICATION_ERROR );
        }

    }

    /**
     * check if using test or production contexts
     */
    void createSpringContext() {
        ctx = SpringContextUtil.getApplicationContext( hasOption( "testing" ), hasOption( COMPASS_ON ),
                hasOption( GIGASPACES_ON ), false );

        CompassUtils.deleteCompassLocks();

        /* if compass is on, turn off the quartz scheduler for CLIs */
        if ( hasOption( COMPASS_ON ) ) {
            QuartzUtils.disableQuartzScheduler( ( StdScheduler ) this.getBean( "schedulerFactoryBean" ) );
        }
    }

    /**
     * @param ctx
     */
    public void setCtx( BeanFactory ctx ) {
        this.ctx = ctx;
    }

    /**
     * Convenience method to obtain instance of any bean by name. Use this only when necessary, you should wire your
     * tests by injection instead.
     * 
     * @param name
     * @return
     */
    protected Object getBean( String name ) {
        return ctx.getBean( name );
    }

    /**
     * You must override this method to process any options you added.(?)
     */
    @Override
    protected void processOptions() {
        createSpringContext();
        authenticate();
        this.auditTrailService = ( AuditTrailService ) this.getBean( "auditTrailService" );
    }

    /**
     * @param e Adds an exception to a cache. this is usefull in the scenairo where we don't want the CLI to bomb on the
     *        exception but continue with its processing. Granted if the exception is fatal then the CLI should
     *        terminate regardless.
     */
    protected void cacheException( Exception e ) {
        exceptionCache.add( e );
    }

    protected void printExceptions() {
        log.info( "Displaying cached error messages: " );

        for ( Exception e : exceptionCache ) {
            log.info( e );
        }
    }

}
