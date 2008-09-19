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
package ubic.gemma.security.authentication;

import java.io.IOException;

import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.event.authentication.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ubic.gemma.security.principal.AuthenticationUtils;

/**
 * Process authentication requests that come from outside a web context. This is used for command line interfaces, for
 * example.
 * 
 * @author keshav
 * @version $Id$
 */
public class ManualAuthenticationProcessing implements ApplicationContextAware {
    private static Log log = LogFactory.getLog( ManualAuthenticationProcessing.class.getName() );

    private AuthenticationManager authenticationManager;
    private ApplicationContext context;

    /**
     * @param username
     * @param password
     * @return
     * @throws AuthenticationException
     */
    public Authentication attemptAuthentication( String username, String password ) throws AuthenticationException {

        if ( username == null ) {
            username = "";
        }

        if ( password == null ) {
            password = "";
        }

        if ( SecurityContextHolder.getContext().getAuthentication() == null ) {
            // need this so we can check user credentials in the database.
            AuthenticationUtils.anonymousAuthenticate( username, this.getAuthenticationManager() );
        }

        // now ready to log the user in
        Authentication authRequest = new UsernamePasswordAuthenticationToken( username, password );
        return this.getAuthenticationManager().authenticate( authRequest );
    }

    /**
     * Provide "anonymous" authentication.
     */
    public void anonymousAuthentication() {
        AuthenticationUtils.anonymousAuthenticate( "anonymous", this.getAuthenticationManager() );
    }

    /**
     * @return Returns the authenticationManager.
     */
    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    /**
     * @param authenticationManager The authenticationManager to set.
     */
    public void setAuthenticationManager( AuthenticationManager authenticationManager ) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Entry point for non-http request.
     * 
     * @param username
     * @param password
     */
    public boolean validateRequest( String username, String password ) {

        Authentication authResult = null;

        try {
            authResult = attemptAuthentication( username, password );
            SecurityContextHolder.getContext().setAuthentication( authResult );
        } catch ( AuthenticationException failed ) {
            // Authentication failed
            log.info( "**  Authentication failed for user " + username + ": " + failed.getMessage() + "  **" );
            log.debug( failed );
            unsuccessfulAuthentication( failed );
            return false;
        }

        log.debug( "Updated SecurityContextHolder to contain the following Authentication: '" + authResult + "'" );
        successfulAuthentication( authResult );
        return true;
    }

    /**
     * @param authResult
     * @throws IOException
     */
    protected void successfulAuthentication( Authentication authResult ) {
        if ( log.isDebugEnabled() ) {
            log.debug( "Authentication success: " + authResult.toString() );
        }

        // Fire event
        assert context != null;

        if ( this.context != null ) {
            context.publishEvent( new InteractiveAuthenticationSuccessEvent( authResult, this.getClass() ) );
        } else {
            log.fatal( "No context in which to place the authentication object" );
        }
    }

    /**
     * @param failed
     * @throws IOException
     */
    protected void unsuccessfulAuthentication( AuthenticationException failed ) {
        log.debug( "Updated SecurityContextHolder to contain null Authentication" );
        log.debug( "Authentication request failed: " + failed.toString() );

    }

    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.context = applicationContext;
    }

}
