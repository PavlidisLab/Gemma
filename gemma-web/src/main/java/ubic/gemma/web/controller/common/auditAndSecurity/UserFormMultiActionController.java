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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.util.JSONUtil;

/**
 * Controller to edit profile of users.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
@Controller
public class UserFormMultiActionController extends BaseController {

    public static final int MIN_PASSWORD_LENGTH = 6;

    @Autowired
    private UserService userService;

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Entry point for updates.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/editUser.html")
    public void editUser( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        String email = request.getParameter( "email" );
        // String firstname = request.getParameter( "firstname" );
        // String lastname = request.getParameter( "lastname" );
        String password = request.getParameter( "password" );
        String passwordConfirm = request.getParameter( "passwordConfirm" );
        String oldPassword = request.getParameter( "oldpassword" );

        /*
         * I had this idea we could let users change their user names, but this turns out to be a PITA.
         */
        String originalUserName = request.getParameter( "username" );

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        try {
            /*
             * Pulling username out of security context to ensure users are logged in and can only update themselves.
             */
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if ( !username.equals( originalUserName ) ) {
                throw new RuntimeException( "You must be logged in to edit your profile." );
            }

            userManager.reauthenticate( originalUserName, oldPassword );

            UserDetailsImpl user = ( UserDetailsImpl ) userManager.loadUserByUsername( username );

            boolean changed = false;

            // if ( StringUtils.isNotBlank( firstname ) ) {
            // changed = true;
            // }
            //
            // if ( StringUtils.isNotBlank( lastname ) ) {
            // changed = true;
            // }

            if ( StringUtils.isNotBlank( email ) && !user.getEmail().equals( email ) ) {
                if ( !email.matches( "/^(\\w+)([-+.][\\w]+)*@(\\w[-\\w]*\\.){1,5}([A-Za-z]){2,4}$/;" ) ) {
                    jsonText = "{success:false,message:'The email address does not look valid'}";
                    jsonUtil.writeToResponse( jsonText );
                    return;
                }
                user.setEmail( email );
                changed = true;
            }

            if ( password.length() > 0 ) {
                if ( !StringUtils.equals( password, passwordConfirm ) ) {
                    throw new RuntimeException( "Passwords do not match." );
                }
                String encryptedPassword = passwordEncoder.encodePassword( password, username );
                userManager.changePassword( oldPassword, encryptedPassword );
            }

            if ( changed ) {
                saveMessage( request, "Changes saved." );
                userManager.updateUser( user );
            } else {
                saveMessage( request, "No changes recorded" );
            }

            jsonText = "{success:true}";

        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage() );
            jsonText = jsonUtil.getJSONErrorMessage( e );
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }
    }

    /**
     * AJAX entry point. Loads a user.
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/loadUser.html")
    public void loadUser( HttpServletRequest request, HttpServletResponse response ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication.isAuthenticated();

        if ( !isAuthenticated ) {
            log.error( "User not authenticated.  Cannot populate user data." );
            return;
        }

        Object o = authentication.getPrincipal();
        String username = null;

        if ( o instanceof UserDetails ) {
            username = ( ( UserDetails ) o ).getUsername();
        } else {
            username = o.toString();
        }

        User user = userService.findByUserName( username );

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {

            if ( user == null ) {
                // this shouldn't happen.
                jsonText = "{success:false,message:'No user with name " + username + "}";
            } else {
                jsonText = "{success:true, data:{username:" + "\"" + username + "\"" + ",email:" + "\""
                        + user.getEmail() + "\"" + "}}";
            }

        } catch ( Exception e ) {
            jsonText = "{success:false,message:" + e.getLocalizedMessage() + "}";
        } finally {
            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Resets the password to a random alphanumeric (of length MIN_PASSWORD_LENGTH).
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/resetPassword.html")
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

            /* Change the password. */
            String pwd = RandomStringUtils.randomAlphanumeric( UserFormMultiActionController.MIN_PASSWORD_LENGTH )
                    .toLowerCase();

            user.setPassword( passwordEncoder.encodePassword( pwd, user.getUserName() ) );

            userService.update( user );

            StringBuffer body = new StringBuffer();
            body.append( "Your password is: " + pwd );

            body.append( "\n\nLogin at: http://www.chibi.ubc.ca/Gemma/login.jsp" );

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom( ConfigUtils.getAdminEmailAddress() );

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

}