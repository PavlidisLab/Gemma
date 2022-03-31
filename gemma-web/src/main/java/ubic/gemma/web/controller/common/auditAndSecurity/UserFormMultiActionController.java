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

import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.util.JSONUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.web.controller.BaseController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller to edit profile of users.
 *
 * @author pavlidis
 * @author keshav
 */
@Controller
public class UserFormMultiActionController extends BaseController {

    public static final int MIN_PASSWORD_LENGTH = 6;

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Entry point for updates.
     */
    @RequestMapping("/editUser.html")
    public void editUser( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        String email = request.getParameter( "email" );
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

            UserDetailsImpl user = ( UserDetailsImpl ) userManager.loadUserByUsername( username );

            boolean changed = false;

            if ( StringUtils.isNotBlank( email ) && !user.getEmail().equals( email ) ) {
                if ( !EmailValidator.getInstance().isValid( email ) ) {
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
                String encryptedPassword = passwordEncoder.encodePassword( password, user.getUsername() );
                userManager.changePassword( oldPassword, encryptedPassword );
            }

            if ( changed ) {
                userManager.updateUser( user );
            }

            saveMessage( request, "Changes saved." );
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
        String username;

        if ( o instanceof UserDetails ) {
            username = ( ( UserDetails ) o ).getUsername();
        } else {
            username = o.toString();
        }

        User user = userManager.findByUserName( username );

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {

            if ( user == null ) {
                // this shouldn't happen.
                jsonText = "{success:false,message:'No user with name " + username + "}";
            } else {
                jsonText =
                        "{success:true, data:{username:" + "\"" + username + "\"" + ",email:" + "\"" + user.getEmail()
                                + "\"" + "}}";
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
     */
    @RequestMapping("/resetPassword.html")
    public void resetPassword( HttpServletRequest request, HttpServletResponse response ) {
        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'resetPassword' method..." );
        }

        String email = request.getParameter( "email" );
        String username = request.getParameter( "username" );

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String txt;
        String jsonText = null;

        /* look up the user's information and reset password. */
        try {

            /* make sure the email and username has been sent */
            if ( StringUtils.isEmpty( email ) || StringUtils.isEmpty( username ) ) {
                txt = "Email or username not specified.  These are required fields.";
                log.warn( txt );
                throw new RuntimeException( txt );
            }

            /* Change the password. */
            String pwd = RandomStringUtils.randomAlphanumeric( UserFormMultiActionController.MIN_PASSWORD_LENGTH )
                    .toLowerCase();

            String token = userManager.changePasswordForUser( email, username, passwordEncoder.encodePassword( pwd, username ) );

            sendResetConfirmationEmail( request, token, username, pwd, email );

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

    /**
     * Send an email to request signup confirmation.
     */
    private void sendResetConfirmationEmail( HttpServletRequest request, String token, String username, String password,
            String email ) {

        try {
            Map<String, Object> model = new HashMap<>();
            model.put( "password", password );
            model.put( "message", getText( "login.passwordReset.emailMessage", request.getLocale() ) );

            this.sendConfirmationEmail( request, token, username, email, model, "passwordReset.vm" );

            saveMessage( request,
                    getText( "login.passwordReset", new Object[] { username, email }, request.getLocale() ) );
        } catch ( Exception e ) {
            throw new RuntimeException( "Couldn't send password change confirmation email to " + email, e );
        }

    }

}