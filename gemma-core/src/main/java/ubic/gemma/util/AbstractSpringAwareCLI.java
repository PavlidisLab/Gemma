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
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.Persister;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.security.authentication.ManualAuthenticationService;

/**
 * Subclass this to create command line interface (CLI) tools that need a Spring context. A standard set of CLI options
 * are provided to manage authentication.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractSpringAwareCLI extends AbstractCLI {

    protected AuditTrailService auditTrailService;

    protected AuditEventService auditEventService;

    protected BeanFactory ctx = null;
    protected Collection<Exception> exceptionCache = new ArrayList<Exception>();
    private boolean forceGigaSpacesOn = false;
    private Persister persisterHelper = null;

    public AbstractSpringAwareCLI() {
        super();

        CompassUtils.deleteCompassLocks();

    }

    @Override
    public String getShortDesc() {
        return "";
    }

    public boolean isForceGigaSpacesOn() {
        return forceGigaSpacesOn;
    }

    /**
     * @param ctx
     */
    public void setCtx( BeanFactory ctx ) {
        this.ctx = ctx;
    }

    @Override
    protected void buildStandardOptions() {
        super.buildStandardOptions();
        addUserNameAndPasswordOptions();
    }

    /**
     * @param e Adds an exception to a cache. this is usefull in the scenairo where we don't want the CLI to bomb on the
     *        exception but continue with its processing. Granted if the exception is fatal then the CLI should
     *        terminate regardless.
     */
    protected void cacheException( Exception e ) {
        exceptionCache.add( e );
    }

    /**
     * Override this method in your subclass to provide additional Spring configuration files that will be merged with
     * the Gemma spring context. See SpringContextUtil; an example path is
     * "classpath*:/myproject/applicationContext-mine.xml".
     * 
     * @return
     */
    protected String[] getAdditionalSpringConfigLocations() {
        return null;
    }

    /**
     * Convenience method to obtain instance of any bean by name.
     * 
     * @param name
     * @return
     * @deprecated Use the getBean(Class) method instead
     */
    @Deprecated
    protected Object getBean( String name ) {
        assert ctx != null : "Spring context was not initialized";
        return ctx.getBean( name );
    }

    protected <T> T getBean( Class<T> clz ) {
        assert ctx != null : "Spring context was not initialized";
        return ctx.getBean( clz );
    }

    /**
     * @return
     */
    protected Persister getPersisterHelper() {
        if ( persisterHelper != null ) {
            return persisterHelper;
        }
        assert ctx != null : "Spring context was not initialized";
        return ( PersisterHelper ) ctx.getBean( "persisterHelper" );
    }

    /**
     * @param auditable
     * @param eventClass can be null
     * @return
     */
    protected boolean needToRun( Auditable auditable, Class<? extends AuditEventType> eventClass ) {
        boolean needToRun = true;
        Date skipIfLastRunLaterThan = getLimitingDate();
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
                        log.info( auditable + ": " + " run more recently than " + skipIfLastRunLaterThan );
                        errorObjects.add( auditable + ": " + " run more recently than " + skipIfLastRunLaterThan );
                        needToRun = false;
                    }
                } else {
                    needToRun = false; // it has been run already at some point
                }
            }
        }

        /*
         * Always skip if we have trouble.
         */
        AuditEvent lastTrouble = this.auditTrailService.getLastTroubleEvent( auditable );

        // special case for expression experiments - check associated ADs.
        if ( lastTrouble == null && auditable instanceof ExpressionExperiment ) {
            ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );
            for ( Object o : ees.getArrayDesignsUsed( ( ExpressionExperiment ) auditable ) ) {
                lastTrouble = auditTrailService.getLastTroubleEvent( ( ArrayDesign ) o );
            }
        }

        okToRun = lastTrouble == null;
        // }

        if ( !okToRun ) {
            log.info( auditable + ": has an active 'trouble' flag" );
            errorObjects.add( auditable + ": has an active 'trouble' flag" );
        }

        return needToRun && okToRun;
    }

    protected void printExceptions() {
        log.info( "Displaying cached error messages: " );

        for ( Exception e : exceptionCache ) {
            log.info( e );
        }
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
     * You must override this method to process any options you added.
     */
    @Override
    protected void processOptions() {
        createSpringContext();
        authenticate();
        this.auditTrailService = this.getBean( AuditTrailService.class );
        this.auditEventService = this.getBean( AuditEventService.class );
    }

    /** check username and password. */
    void authenticate() {

        /*
         * Allow security settings (authorization etc) in a given context to be passed into spawned threads
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_GLOBAL );

        ManualAuthenticationService manAuthentication = ctx.getBean( ManualAuthenticationService.class );
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

            boolean success = manAuthentication.validateRequest( username, password );
            if ( !success ) {
                System.err.println( "Not authenticated. Make sure you entered a valid username (got '" + username
                        + "') and/or password" );
                bail( ErrorCode.AUTHENTICATION_ERROR );
            } else {
                log.info( "Logged in as " + username );
            }
        } else {
            log.info( "Logging in as anonymous guest with limited privileges" );
            manAuthentication.authenticateAnonymously();
        }

    }

    /**
     * check if using test or production contexts
     */
    void createSpringContext() {

        ctx = SpringContextUtil.getApplicationContext( hasOption( "testing" ), true, false,
                getAdditionalSpringConfigLocations() );

        QuartzUtils.disableQuartzScheduler( this.getBean( StdScheduler.class ) );

        /*
         * Guarantee that the security settings are uniform throughout the application (all threads).
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_GLOBAL );

    }

}
