package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.util.SslUtil;
import edu.columbia.gemma.util.StringUtil;
import edu.columbia.gemma.web.Constants;

/**
 * Implementation of <strong>HttpServlet</strong> that is used to get a username and password and encrypt the password
 * before sending to container-managed authentication.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 * @web.servlet name="login" display-name="Login Servlet" load-on-startup="1"
 * @web.servlet-init-param name="authURL" value="j_security_check"
 *                         <p>
 *                         Change the following value to false if you don't require SSL for login
 *                         </p>
 * @web.servlet-init-param name="isSecure" value="false" If you're not using Tomcat, change encrypt-password to true
 * @web.servlet-init-param name="encrypt-password" value="true"
 * @web.servlet-init-param name="algorithm" value="SHA"
 * @web.servlet-mapping url-pattern="/authorize/*"
 */
@SuppressWarnings("unchecked")
public final class LoginServlet extends HttpServlet {
    private static String authURL = "j_security_check";
    private static String httpsPort = null;
    private static String httpPort = null;
    private static Boolean secure = Boolean.FALSE;
    private static String algorithm = "SHA";
    private static Boolean encrypt = Boolean.FALSE;
    private transient final Log log = LogFactory.getLog( LoginServlet.class );

    /**
     * Initializes the port numbers based on the port init parameters as defined in web.xml
     */
    private static void initializeSchemePorts( ServletContext servletContext ) {
        if ( httpPort == null ) {
            String portNumber = servletContext.getInitParameter( SslUtil.HTTP_PORT_PARAM );
            httpPort = ( ( portNumber == null ) ? SslUtil.STD_HTTP_PORT : portNumber );
        }

        if ( httpsPort == null ) {
            String portNumber = servletContext.getInitParameter( SslUtil.HTTPS_PORT_PARAM );
            httpsPort = ( ( portNumber == null ) ? SslUtil.STD_HTTPS_PORT : portNumber );
        }
    }

    /**
     * Validates the Init and Context parameters, configures authentication URL
     * 
     * @throws ServletException if the init parameters are invalid or any other problems occur during initialisation
     */
    public void init() {
        // Get the container authentication URL for FORM-based Authentication
        // J2EE spec says should be j_security_check
        authURL = getInitParameter( Constants.AUTH_URL );

        // Get the encryption algorithm to use for encrypting passwords before
        // storing in database
        algorithm = getInitParameter( Constants.ENC_ALGORITHM );

        /* This determines if the login uses SSL or not */
        secure = Boolean.valueOf( getInitParameter( "isSecure" ) );

        /* This determines if the password should be encrypted programmatically */
        encrypt = Boolean.valueOf( getInitParameter( "encrypt-password" ) );

        log.debug( "Authentication URL: " + authURL );
        log.debug( "Use SSL for login? " + secure );
        log.debug( "Programmatic encryption of password? " + encrypt );
        log.debug( "Encryption algorithm: " + algorithm );

        ServletContext ctx = getServletContext();
        initializeSchemePorts( ctx );

        log.debug( "HTTP Port: " + httpPort );
        log.debug( "HTTPS Port: " + httpsPort );

        // Orion starts Servlets before Listeners, so check if the config
        // object already exists
        Map<String, Object> config = ( HashMap<String, Object> ) ctx.getAttribute( Constants.CONFIG );

        if ( config == null ) {
            config = new HashMap<String, Object>();
        }

        // update the config object with the init-params from this servlet
        config.put( Constants.HTTP_PORT, httpPort );
        config.put( Constants.HTTPS_PORT, httpsPort );
        config.put( Constants.SECURE_LOGIN, secure );
        config.put( Constants.ENC_ALGORITHM, algorithm );
        config.put( Constants.ENCRYPT_PASSWORD, encrypt );
        ctx.setAttribute( Constants.CONFIG, config );
    }

    /**
     * Route the user to the execute method
     * 
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @exception IOException if an input/output error occurs
     */
    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        execute( request, response );
    }

    /**
     * Route the user to the execute method
     * 
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @exception IOException if an input/output error occurs
     */
    public void doPost( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        execute( request, response );
    }

    /**
     * Process the specified HTTP request, and create the corresponding HTTP response (or forward to another web
     * component that will create it).
     * 
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    public void execute( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        // If user is already authenticated, it means they probably bookmarked
        // or typed in the URL to login.jsp directly, route them to the main
        // menu is this is the case

        // *** Removed For Acegi ***
        // if ( request.getRemoteUser() != null ) {
        // if ( log.isDebugEnabled() ) {
        // log.debug( "User '" + request.getRemoteUser() + "' already logged in, routing to mainMenu" );
        // }
        // response.sendRedirect( request.getContextPath() + "/mainMenu.html" );
        // return;
        // }

        String redirectString = SslUtil.getRedirectString( request, getServletContext(), secure.booleanValue() );

        if ( redirectString != null ) {
            // Redirect the page to the desired URL
            response.sendRedirect( response.encodeRedirectURL( redirectString ) );
            log.debug( "Switching protocols, redirecting user to " + response.encodeRedirectURL( redirectString ) );
        }

        // Extract attributes we will need
        String username = request.getParameter( "j_username" );
        String password = request.getParameter( "j_password" );

        if ( request.getParameter( "rememberMe" ) != null ) {
            request.getSession().setAttribute( Constants.LOGIN_COOKIE, "true" );
        }

        String encryptedPassword = "";

        if ( encrypt.booleanValue() && ( request.getAttribute( "encrypt" ) == null ) ) {
            log.debug( "Encrypting password for user '" + username + "'" );
            encryptedPassword = StringUtil.encodePassword( password, algorithm );
            log.debug( "Encrypted" );
        } else {
            encryptedPassword = password;
        }

        if ( redirectString == null ) {
            // signifies already in correct protocol
            log.debug( "Authenticating..." );
            String uri = request.getParameter( "j_uri" );
            String req = request.getContextPath() + "/" + authURL + "?j_username=" + username + "&j_password="
                    + encryptedPassword + "&j_uri=" + uri;
            log.debug( "Authenticating user '" + username + "', redirecting to " + response.encodeRedirectURL( req ) );
            response.sendRedirect( response.encodeRedirectURL( req ) );
        }
    }
}
