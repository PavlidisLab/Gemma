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

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.quartz.impl.StdScheduler;
import org.springframework.beans.factory.BeanFactory;

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

    protected BeanFactory ctx = null;
    PersisterHelper ph = null;

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
     * @return
     */
    protected PersisterHelper getPersisterHelper() {
        if ( ph != null ) {
            return ph;
        }

        assert ctx != null : "Spring context was not initialized";
        return ( PersisterHelper ) ctx.getBean( "persisterHelper" );

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

    /** check if using test or production context */
    void createSpringContext() {
        ctx = SpringContextUtil.getApplicationContext( hasOption( "testing" ), false );
        CompassUtils.deleteCompassLocks();
        QuartzUtils.disableQuartzScheduler( ( StdScheduler ) this.getBean( "schedulerFactoryBean" ) );
    }

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
    }

    /**
     * @param errorObjects
     * @param successObjects
     */
    protected void summarizeProcessing( Collection<String> errorObjects, Collection<String> successObjects ) {
        if ( successObjects.size() > 0 ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\n   Processed:\n" );
            for ( String object : successObjects ) {
                buf.append( "    " + object + "\n" );
            }
            buf.append( "---------------------\n" );

            log.info( buf );
        } else {
            log.error( "No objects processed successfully!" );
        }

        if ( errorObjects.size() > 0 ) {
            StringBuilder buf = new StringBuilder();
            buf.append( "\n---------------------\n   Errors occurred during the processing of:\n" );
            for ( String object : errorObjects ) {
                buf.append( "    " + object + "\n" );
            }
            buf.append( "---------------------\n" );
            log.error( buf );
        }
    }

}
