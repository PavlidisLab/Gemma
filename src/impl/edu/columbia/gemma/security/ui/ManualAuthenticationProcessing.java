/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.security.ui;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationException;
import net.sf.acegisecurity.AuthenticationManager;
import net.sf.acegisecurity.context.SecurityContextHolder;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.ui.InteractiveAuthenticationSuccessEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import edu.columbia.gemma.util.StringUtil;

/**
 * Process non-http requests
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ManualAuthenticationProcessing {
    private static Log logger = LogFactory.getLog( ManualAuthenticationProcessing.class.getName() );

    private AuthenticationManager authenticationManager;
    private ApplicationContext context;
    private static String algorithm = "SHA";

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
    public void validateRequest( String username, String password ) {

        String encryptedPassword = StringUtil.encodePassword( password, algorithm );

        Authentication authResult;

        try {
            authResult = attemptAuthentication( username, encryptedPassword );
        } catch ( AuthenticationException failed ) {
            // Authentication failed
            unsuccessfulAuthentication( failed );

            return;
        }

        successfulAuthentication( authResult );

        return;
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

        if ( logger.isDebugEnabled() ) {
            logger
                    .debug( "Updated SecurityContextHolder to contain the following Authentication: '" + authResult
                            + "'" );
        }

        // rememberMeServices.loginSuccess( request, response, authResult );

        // Fire event
        if ( this.context != null ) {
            context.publishEvent( new InteractiveAuthenticationSuccessEvent( authResult, this.getClass() ) );
        }

        // response.sendRedirect( response.encodeRedirectURL( targetUrl ) );
    }

    /**
     * @param request
     * @param response
     * @param failed
     * @throws IOException
     */
    protected void unsuccessfulAuthentication( AuthenticationException failed ) {
        SecurityContextHolder.getContext().setAuthentication( null );

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Updated SecurityContextHolder to contain null Authentication" );
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Authentication request failed: " + failed.toString() );
        }

        //
        // rememberMeServices.loginFail( request, response );
        //
        // response.sendRedirect( response.encodeRedirectURL( request.getContextPath() + failureUrl ) );
    }

}
