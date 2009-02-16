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
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.security.RunAsManager;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.RequestUtil;
import ubic.gemma.web.util.JSONUtil;

/**
 * Controller to edit profile of users.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="userFormMultiActionController"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="mailMessage" ref="mailMessage"
 * @spring.property name="messageSource" ref="messageSource"
 * @spring.property name="methodNameResolver" ref="editUserActions"
 */
public class UserFormMultiActionController extends UserAuthenticatingMultiActionController {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private MessageSource messageSource = null;

    /**
     * AJAX entry point. Loads a user.
     * 
     * @param request
     * @param response
     */
    public void loadUser( HttpServletRequest request, HttpServletResponse response ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication.isAuthenticated();

        if ( !isAuthenticated ) {
            log.error( "User not authenticated.  Cannot populate user data." );
            return;
        }

        String username = authentication.getPrincipal().toString();
        User user = userService.findByUserName( username );
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {
            jsonText = "{success:true, data:{username:" + "\"" + username + "\"" + ",email:" + "\"" + user.getEmail()
                    + "\"" + "}}";

        } catch ( Exception e ) {
            e.printStackTrace();
            jsonText = "{success:false}";
        } finally {
            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }

    }

    /**
     * AJAX entry point.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void onSubmit( HttpServletRequest request, HttpServletResponse response ) {

        String email = request.getParameter( "email" );
        String firstname = request.getParameter( "firstname" );
        String lastname = request.getParameter( "lastname" );
        String password = request.getParameter( "password" );
        String passwordConfirm = request.getParameter( "passwordConfirm" );

        /*
         * Pulling username out of security context to ensure users are logged in and can only update themselves.
         */
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userService.findByUserName( username );
        if ( !StringUtils.equals( password, passwordConfirm ) ) {
            throw new RuntimeException( "Passwords do not match." );
        }
        String encryptedPassword = super.encryptPassword( password, request );
        user.setPassword( encryptedPassword );

        if ( StringUtils.isNotBlank( firstname ) ) {
            user.setName( firstname );
        }

        if ( StringUtils.isNotBlank( lastname ) ) {
            user.setName( lastname );
        }

        user.setEmail( email );

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;
        try {
            userService.update( user );
            jsonText = "{success:true}";
        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage() );
            jsonText = jsonUtil.getJSONErrorMessage( e );
            log.info( jsonText );
        } finally {
            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Resets the password to a random alphanumeric (of length 8).
     * 
     * @param request
     * @param response
     */
    public void resetPassword( HttpServletRequest request, HttpServletResponse response ) {
        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'resetPassword' method..." );
        }

        MessageSourceAccessor text = new MessageSourceAccessor( messageSource, request.getLocale() );

        String email = request.getParameter( "email" );
        String username = request.getParameter( "username" );

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String txt = null;
        String jsonText = null;

        /* look up the user's information and reset password. */
        try {

            /* make sure the email and username has been sent */
            if ( StringUtils.isEmpty( email ) || StringUtils.isEmpty( username ) ) {
                txt = "Email or username not specified.  These are required fields.";
                log.warn( txt );
                throw new RuntimeException( txt );
            }

            User user = userService.findByEmail( email );

            /* make sure user exists with email */
            if ( user == null ) {
                txt = "User with email " + email + " not found.";
                log.warn( txt );
                throw new RuntimeException( txt );
            }

            /* check the username matches that of the user */
            if ( !StringUtils.equals( user.getUserName(), username ) ) {
                txt = "User with username " + username + " not found.";
                log.warn( txt );
                throw new RuntimeException( txt );
            }

            /*
             * Must run as someone else to update user. Run as the user we are resetting the password for. First, save
             * the current authentication.
             */
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            /* Create a new authentication for the user we are resetting password for. */
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken( user.getUserName(),
                    user.getPassword(), new GrantedAuthority[] { new GrantedAuthorityImpl(
                            SecurityService.USER_AUTHORITY ) } );

            RunAsManager r = new RunAsManager();
            String recipient = user.getUserName();
            Authentication runAs = r.buildRunAs( user, token, recipient );

            SecurityContextHolder.getContext().setAuthentication( runAs );

            /* Change the password. */
            String pwd = RandomStringUtils.randomAlphanumeric( UserFormMultiActionController.MIN_PASSWORD_LENGTH );
            String encryptedPwd = super.encryptPassword( pwd, request );
            user.setPassword( encryptedPwd );
            userService.update( user );

            /* Set the context back */
            SecurityContextHolder.getContext().setAuthentication( auth );

            StringBuffer body = new StringBuffer();
            body.append( "Your password is: " + pwd );
            body.append( "\n\nLogin at: " + RequestUtil.getAppURL( request ) + "/login.jsp" );

            mailMessage.setTo( user.getUserName() + "<" + user.getEmail() + ">" );
            String subject = text.getMessage( "webapp.prefix" ) + text.getMessage( "user.passwordReset" );
            mailMessage.setSubject( subject );
            mailMessage.setText( body.toString() );
            mailEngine.send( mailMessage );

            saveMessage( request, text.getMessage( "login.passwordReset", new Object[] { user.getUserName(),
                    user.getEmail() } ) );

            jsonText = "{success:true}";

        } catch ( Exception e ) {
            log.error( e, e );
            jsonText = jsonUtil.getJSONErrorMessage( e );
        } finally {
            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    public void setMessageSource( MessageSource messageSource ) {
        this.messageSource = messageSource;
    }
}