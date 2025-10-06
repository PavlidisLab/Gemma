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
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ubic.gemma.core.mail.MailService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.web.controller.util.JsonUtil;
import ubic.gemma.web.controller.util.MessageUtil;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Controller to edit profile of users.
 *
 * @author pavlidis
 * @author keshav
 */
@Controller
@CommonsLog
public class UserFormMultiActionController {

    public static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * RFC 5322 email pattern.
     */
    public static final Pattern RFC_5322_EMAIL_PATTERN = Pattern.compile( "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$" );

    @Autowired
    private UserManager userManager;
    @Autowired
    private MailService mailService;
    @Autowired
    private MessageUtil messageUtil;

    /**
     * Entry point for updates.
     */
    @RequestMapping(value = "/editUser.html", method = RequestMethod.POST)
    public void editUser(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) @Nullable String password,
            @RequestParam(value = "passwordConfirm", required = false) @Nullable String passwordConfirm,
            @RequestParam(value = "oldPassword", required = false) @Nullable String oldPassword,
            HttpServletResponse response ) throws IOException {

        /*
         * I had this idea we could let users change their user names, but this turns out to be a PITA.
         */

        try {
            if ( email != null ) {
                /*
                 * Pulling username out of security context to ensure users are logged in and can only update themselves.
                 */
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                UserDetailsImpl user = ( UserDetailsImpl ) userManager.loadUserByUsername( username );
                if ( !user.getEmail().equalsIgnoreCase( email ) ) {
                    if ( !RFC_5322_EMAIL_PATTERN.matcher( email ).matches() ) {
                        throw new IllegalArgumentException( "The email address does not look valid." );
                    }
                    user.setEmail( email );
                    userManager.updateUser( user );
                    messageUtil.saveMessage( "Email address was updated." );
                }
            }

            if ( password != null ) {
                if ( oldPassword == null ) {
                    throw new IllegalArgumentException( "Old password is required to change the password." );
                }
                if ( passwordConfirm == null ) {
                    throw new IllegalArgumentException( "Password confirmation is required to change the password." );
                }
                if ( password.length() < MIN_PASSWORD_LENGTH ) {
                    throw new IllegalArgumentException( "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long." );
                }
                if ( !password.equals( passwordConfirm ) ) {
                    throw new IllegalArgumentException( "Password does not match the confirmation." );
                }
                userManager.changePassword( oldPassword, password );
                messageUtil.saveMessage( "Password was changed." );
            }

            JsonUtil.writeSuccessToResponse( response );
        } catch ( Exception e ) {
            log.error( "Failed to update user profile.", e );
            JsonUtil.writeErrorToResponse( e, response );
        }
    }

    /**
     * AJAX entry point. Loads a user.
     */
    @RequestMapping(value = "/loadUser.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void loadUser( HttpServletResponse response ) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication.isAuthenticated();

        if ( !isAuthenticated ) {
            JsonUtil.writeErrorToResponse( HttpServletResponse.SC_FORBIDDEN, "User not authenticated.  Cannot populate user data.", response );
            return;
        }

        try {
            Object o = authentication.getPrincipal();
            String username;
            if ( o instanceof UserDetails ) {
                username = ( ( UserDetails ) o ).getUsername();
            } else {
                username = o.toString();
            }
            User user = userManager.findByUserName( username );
            JSONObject json = new JSONObject().put( "success", true )
                    .put( "data", new JSONObject()
                            .put( "username", user.getUserName() )
                            .put( "email", user.getEmail() ) );
            JsonUtil.writeToResponse( json, response );
        } catch ( Exception e ) {
            log.error( "Error while retrieving user by username.", e );
            JsonUtil.writeErrorToResponse( e, response );
        }
    }

    /**
     * Resets the password to a random alphanumeric (of length MIN_PASSWORD_LENGTH).
     */
    @RequestMapping(value = "/resetPassword.html", method = RequestMethod.POST)
    public void resetPassword( @RequestParam("email") String email, @RequestParam("username") String username, Locale locale, HttpServletResponse response ) throws IOException {
        /* look up the user's information and reset password. */
        try {
            /* make sure the email and username has been sent */
            if ( StringUtils.isEmpty( email ) || StringUtils.isEmpty( username ) ) {
                String txt = "Email or username not specified.  These are required fields.";
                throw new IllegalArgumentException( txt );
            }

            /* Change the password. */
            String pwd = RandomStringUtils.secureStrong()
                    .nextAlphanumeric( UserFormMultiActionController.MIN_PASSWORD_LENGTH )
                    .toLowerCase();

            String token = userManager.changePasswordForUser( email, username, pwd );

            try {
                mailService.sendResetConfirmationEmail( email, username, pwd, token, locale );
            } catch ( MailException e ) {
                log.error( "Couldn't send password reset email to " + email + ".", e );
            }
            messageUtil.saveMessage( "login.passwordReset", new Object[] { username, email }, "??login.passwordReset??" );

            JsonUtil.writeSuccessToResponse( response );
        } catch ( AuthenticationException e ) {
            log.info( "Password could not be reset due to an authentication-related error.", e );
            JsonUtil.writeErrorToResponse( e, response );
        } catch ( Exception e ) {
            log.error( "Unexpected exception when attempting to change password.", e );
            JsonUtil.writeErrorToResponse( e, response );
        }
    }
}