package edu.columbia.gemma.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;

import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserService;
import edu.columbia.gemma.util.RequestUtil;
import edu.columbia.gemma.util.StringUtil;

/**
 * <p>
 * Intercepts Login requests for "Remember Me" functionality. The intercepted request is checked to see if there is a
 * valid cookie for the user. If so, they are authenticated without being shown the login page.
 * <p>
 * Based on code from Appfuse.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 * @web.filter display-name="Login Filter" name="loginFilter"
 * @web.filter-init-param name="enabled" value="true"
 */
public class LoginFilter implements Filter {
    private transient final Log log = LogFactory.getLog( LoginFilter.class );
    private FilterConfig config = null;
    private boolean enabled = true;

    public void doFilter( ServletRequest req, ServletResponse resp, FilterChain chain ) throws IOException,
            ServletException {

        HttpServletRequest request = ( HttpServletRequest ) req;
        HttpServletResponse response = ( HttpServletResponse ) resp;

        // See if the user has a remember me cookie
        Cookie c = RequestUtil.getCookie( request, "sessionId" );

        WebApplicationContext context = ( WebApplicationContext ) config.getServletContext().getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE );

        UserService userService = ( UserService ) context.getBean( "userService" );

        // Check to see if the user is logging out, if so, remove all
        // login cookies
        if ( request.getRequestURL().indexOf( "logout" ) != -1 && request.getRemoteUser() != null ) {
            // make sure user's session hasn't timed out
            if ( request.getRemoteUser() != null ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( "logging out '" + request.getRemoteUser() + "'" );
                }
                request.getRemoteUser();
                userService.removeLoginCookies( request.getRemoteUser() );
                RequestUtil.deleteCookie( response, c, request.getContextPath() );
                request.getSession().invalidate();
            }
        } else if ( c != null && enabled ) {
            String loginCookie = userService.checkLoginCookie( c.getValue() );

            if ( loginCookie != null ) {
                RequestUtil.setCookie( response, "sessionId", loginCookie, request.getContextPath() );
                loginCookie = StringUtil.decodeString( loginCookie );

                String[] value = StringUtils.split( loginCookie, '|' );

                User user = userService.getUser( value[0] );

                // authenticate user without displaying login page
                String route = "/authorize?j_username=" + user.getUserName() + "&j_password=" + user.getPassword();

                request.setAttribute( "encrypt", "false" );
                request.getSession( true ).setAttribute( "cookieLogin", "true" );

                log.debug( "I remember you '" + user.getUserName() + "', attempting to authenticate..." );

                RequestDispatcher dispatcher = request.getRequestDispatcher( route );
                dispatcher.forward( request, response );

                return;
            }
        }

        chain.doFilter( req, resp );
    }

    /**
     * Initialize controller values of filter.
     */
    public void init( FilterConfig config ) {
        this.config = config;

        String param = config.getInitParameter( "enabled" );
        enabled = Boolean.valueOf( param ).booleanValue();

        log.debug( "Remember Me enabled: " + enabled );

        config.getServletContext().setAttribute( "rememberMeEnabled", config.getInitParameter( "enabled" ) );
    }

    /**
     * destroy any instance values other than config *
     */
    public void destroy() {
    }
}
