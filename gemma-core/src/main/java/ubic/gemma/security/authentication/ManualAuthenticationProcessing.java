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

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.event.authentication.InteractiveAuthenticationSuccessEvent;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

/**
 * Process authentication requests that come from outside a web context. This is used for command line interfaces, for
 * example.
 * 
 * @author keshav
 * @version $Id$
 */
public class ManualAuthenticationProcessing {
    private static Log logger = LogFactory.getLog( ManualAuthenticationProcessing.class.getName() );

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

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken( username, password );

        return this.getAuthenticationManager().authenticate( authRequest );
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

        Authentication authResult;

        try {
            authResult = attemptAuthentication( username, password );
        } catch ( AuthenticationException failed ) {
            // Authentication failed
            logger.error( "**  Authentication failed for user " + username + ": " + failed.getMessage() + "  **" );
            logger.info( failed );
            unsuccessfulAuthentication( failed );
            return false;
        }

        successfulAuthentication( authResult );
        return true;
    }

    /**
     * @param request
     * @param response
     * @param authResult
     * @throws IOException
     */
    protected void successfulAuthentication( Authentication authResult ) {
        if ( logger.isDebugEnabled() ) {
            logger.debug( "Authentication success: " + authResult.toString() );
        }

        SecurityContextHolder.getContext().setAuthentication( authResult );

        logger.debug( "Updated SecurityContextHolder to contain the following Authentication: '" + authResult + "'" );

        // Fire event
        if ( this.context != null ) {
            context.publishEvent( new InteractiveAuthenticationSuccessEvent( authResult, this.getClass() ) );
        }
    }

    /**
     * @param request
     * @param response
     * @param failed
     * @throws IOException
     */
    protected void unsuccessfulAuthentication( AuthenticationException failed ) {
        SecurityContextHolder.getContext().setAuthentication( null );
        logger.debug( "Updated SecurityContextHolder to contain null Authentication" );
        logger.debug( "Authentication request failed: " + failed.toString() );
    }

}
