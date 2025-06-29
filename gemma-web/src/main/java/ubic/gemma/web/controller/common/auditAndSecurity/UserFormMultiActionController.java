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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.MailEngine;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.web.controller.util.JsonUtil;
import ubic.gemma.web.controller.util.MessageUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Controller to edit profile of users.
 *
 * @author pavlidis
 * @author keshav
 */
@Controller
public class UserFormMultiActionController {

    public static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * RFC 5322 email pattern.
     */
    private static final Pattern RFC_5322_EMAIL_PATTERN = Pattern.compile( "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$" );
    protected final Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    private UserManager userManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MailEngine mailEngine;
    @Autowired
    private ServletContext servletContext;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private MessageUtil messageUtil;

    @Value("${gemma.hosturl}")
    private String hostUrl;

    /**
     * Entry point for updates.
     */
    @RequestMapping(value = "/editUser.html", method = RequestMethod.POST)
    public void editUser( @RequestParam("email") String email, @RequestParam("password") String password,
            @RequestParam("passwordConfirm") String passwordConfirm, @RequestParam("oldPassword") String oldPassword,
            @RequestParam("username") String originalUserName,
            HttpServletResponse response ) throws Exception {

        /*
         * I had this idea we could let users change their user names, but this turns out to be a PITA.
         */

        try {
            /*
             * Pulling username out of security context to ensure users are logged in and can only update themselves.
             */
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if ( !username.equals( originalUserName ) ) {
                throw new AccessDeniedException( "You must be logged in to edit your profile." );
            }

            UserDetailsImpl user = ( UserDetailsImpl ) userManager.loadUserByUsername( username );

            boolean changed = false;

            if ( StringUtils.isNotBlank( email ) && !user.getEmail().equals( email ) ) {
                if ( !RFC_5322_EMAIL_PATTERN.matcher( email ).matches() ) {
                    JSONObject json = new JSONObject()
                            .put( "success", false )
                            .put( "message", "The email address does not look valid" );
                    JsonUtil.writeToResponse( json, response );
                    return;
                }
                user.setEmail( email );
                changed = true;
            }

            if ( !password.isEmpty() ) {
                if ( !StringUtils.equals( password, passwordConfirm ) ) {
                    throw new IllegalArgumentException( "Passwords do not match." );
                }
                String encryptedPassword = passwordEncoder.encodePassword( password, user.getUsername() );
                userManager.changePassword( oldPassword, encryptedPassword );
            }

            if ( changed ) {
                userManager.updateUser( user );
            }

            messageUtil.saveMessage( "Changes saved." );
            JsonUtil.writeToResponse( new JSONObject().put( "success", true ), response );

        } catch ( Exception e ) {
            log.error( "Failed to update user profile", e );
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

        User user;
        try {
            user = userManager.findByUserName( username );
        } catch ( Exception e ) {
            log.error( "Error while retrieving user by username.", e );
            JsonUtil.writeErrorToResponse( e, response );
            return;
        }

        JSONObject json = new JSONObject().put( "success", true )
                .put( "data", new JSONObject()
                        .put( "username", user.getUserName() )
                        .put( "email", user.getEmail() ) );
        JsonUtil.writeToResponse( json, response );
    }

    /**
     * Resets the password to a random alphanumeric (of length MIN_PASSWORD_LENGTH).
     */
    @RequestMapping(value = "/resetPassword.html", method = RequestMethod.POST)
    public void resetPassword( @RequestParam("email") String email, @RequestParam("username") String username, HttpServletRequest request, HttpServletResponse response, Locale locale ) throws IOException {
        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'resetPassword' method..." );
        }

        /* make sure the email and username has been sent */
        if ( StringUtils.isEmpty( email ) || StringUtils.isEmpty( username ) ) {
            String txt = "Email or username not specified.  These are required fields.";
            log.warn( txt );
            throw new IllegalArgumentException( txt );
        }

        /* look up the user's information and reset password. */
        try {
            /* Change the password. */
            String pwd = RandomStringUtils.randomAlphanumeric( UserFormMultiActionController.MIN_PASSWORD_LENGTH )
                    .toLowerCase();

            String token = userManager.changePasswordForUser( email, username, passwordEncoder.encodePassword( pwd, username ) );

            sendResetConfirmationEmail( request, token, username, pwd, email, locale );

            JsonUtil.writeToResponse( new JSONObject().put( "success", true ), response );
        } catch ( AuthenticationException e ) {
            log.info( "Password could not be reset due to an authentication-related error.", e );
            JsonUtil.writeErrorToResponse( e, response );
        } catch ( Exception e ) {
            log.error( "Unexpected exception when attempting to change password.", e );
            JsonUtil.writeErrorToResponse( e, response );
        }
    }

    /**
     * Send an email to request signup confirmation.
     */
    private void sendResetConfirmationEmail( HttpServletRequest request, String token, String username, String password,
            String email, Locale locale ) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put( "password", password );
            model.put( "message", messageSource.getMessage( "login.passwordReset.emailMessage", null, request.getLocale() ) );
            model.put( "username", username );
            model.put( "confirmLink",
                    hostUrl + servletContext.getContextPath() + "/confirmRegistration.html?key=" + token + "&username=" + username );
            try {
                mailEngine.sendMessage( username + "<" + email + ">", this.messageSource.getMessage( "signup.email.subject", null, locale ), "passwordReset.vm", model );
            } catch ( Exception e ) {
                log.error( "Couldn't send email to " + email, e );
            }
            messageUtil.saveMessage( "login.passwordReset", new Object[] { username, email }, "??login.passwordReset??" );
        } catch ( Exception e ) {
            throw new RuntimeException( "Couldn't send password change confirmation email to " + email, e );
        }
    }
}