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
package edu.columbia.gemma.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserService;
import edu.columbia.gemma.util.RequestUtil;
import edu.columbia.gemma.util.SslUtil;
import edu.columbia.gemma.web.Constants;

/**
 * This class is used to filter all requests to the <code>Action</code> servlet and detect if a user is authenticated.
 * If a user is authenticated, but no user object exists, this class populates the <code>UserForm</code> from the user
 * store.
 * <hr>
 * 
 * @author Matt Raible
 * @author pavlidis
 * @version $Id$
 * @web.filter display-name="Action Filter" name="actionFilter"
 *             <p>
 *             Change this value to true if you want to secure your entire application. This can also be done in
 *             web-security.xml by setting <transport-guarantee> to CONFIDENTIAL.
 *             </p>
 * @web.filter-init-param name="isSecure" value="false"
 */
public class ActionFilter implements Filter {
    private static Boolean secure = Boolean.FALSE;
    private final transient Log log = LogFactory.getLog( ActionFilter.class );
    private FilterConfig config = null;

    public void init( FilterConfig c ) {
        this.config = c;

        /* This determines if the application uconn SSL or not */
        secure = Boolean.valueOf( config.getInitParameter( "isSecure" ) );
    }

    /**
     * Destroys the filter.
     */
    public void destroy() {
        config = null;
    }

    public void doFilter( ServletRequest req, ServletResponse resp, FilterChain chain ) throws IOException,
            ServletException {
        // cast to the types I want to use
        HttpServletRequest request = ( HttpServletRequest ) req;
        HttpServletResponse response = ( HttpServletResponse ) resp;
        HttpSession session = request.getSession( true );

        // do pre filter work here
        // If using https, switch to http
        String redirectString = SslUtil.getRedirectString( request, config.getServletContext(), secure.booleanValue() );

        if ( redirectString != null ) {
            if ( log.isDebugEnabled() ) {
                log.debug( "protocol switch needed, redirecting to '" + redirectString + "'" );
            }

            // Redirect the page to the desired URL
            response.sendRedirect( response.encodeRedirectURL( redirectString ) );

            // ensure we don't chain to requested resource
            return;
        }

        User user = ( User ) session.getAttribute( Constants.USER_KEY );
        ServletContext context = config.getServletContext();
        String username = request.getRemoteUser();

        // user authenticated, empty user object
        if ( ( username != null ) && ( user == null ) ) {
            ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( context );

            UserService userService = ( UserService ) ctx.getBean( "userService" );
            user = userService.getUser( username );
            session.setAttribute( Constants.USER_KEY, user );

            // if user wants to be remembered, create a remember me cookie
            if ( session.getAttribute( Constants.LOGIN_COOKIE ) != null ) {
                session.removeAttribute( Constants.LOGIN_COOKIE );

                String loginCookie = userService.createLoginCookie( username );
                RequestUtil.setCookie( response, Constants.LOGIN_COOKIE, loginCookie, request.getContextPath() );
            }
        }

        chain.doFilter( request, response );
    }
}
