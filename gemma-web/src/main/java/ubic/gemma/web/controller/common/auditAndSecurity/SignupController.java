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

import gemma.gsec.authentication.LoginDetailsValueObject;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.util.SecurityUtil;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ubic.gemma.core.mail.MailService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptcha;
import ubic.gemma.web.controller.util.JsonUtil;
import ubic.gemma.web.controller.util.MessageUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * Controller to signup new users. See also the UserListController.
 *
 * @author pavlidis
 * @author keshav
 */
@Controller
@CommonsLog
public class SignupController implements InitializingBean {

    @Autowired
    private UserManager userManager;
    @Autowired
    private MailService mailService;
    @Autowired
    protected MessageSource messageSource;
    @Autowired
    protected MessageUtil messageUtil;

    @Value("${gemma.recaptcha.privateKey}")
    private String recaptchaPrivateKey;

    @Setter
    private ReCaptcha reCaptcha;

    @Override
    public void afterPropertiesSet() {
        reCaptcha = new ReCaptcha( recaptchaPrivateKey );
        if ( reCaptcha.isPrivateKeySet() ) {
            log.warn( "No recaptcha private key is configured, skipping validation" );
        }
    }

    @RequestMapping(value = "/ajaxLoginCheck.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void ajaxLoginCheck( HttpServletResponse response ) throws IOException {
        JSONObject json;
        try {
            if ( userManager.loggedIn() ) {
                json = new JSONObject().put( "success", true )
                        .put( "user", userManager.getCurrentUsername() )
                        .put( "isAdmin", SecurityUtil.isUserAdmin() );
            } else {
                json = new JSONObject().put( "success", false );
            }
            JsonUtil.writeToResponse( json, response );
        } catch ( Exception e ) {
            log.error( "Error while checking if the current user is logged in.", e );
            JsonUtil.writeErrorToResponse( e, response );
        }
    }

    /*
     * This is hit when a user clicks on the confirmation link they received by email.
     */
    @RequestMapping(value = "/confirmRegistration.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void confirmRegistration( @RequestParam("username") String username, @RequestParam("key") String key,
            HttpServletRequest request, HttpServletResponse response ) throws Exception {
        if ( StringUtils.isBlank( username ) || StringUtils.isBlank( key ) ) {
            throw new IllegalArgumentException(
                    "The confirmation url was not valid; it must contain the key and username" );
        }

        boolean ok = userManager.validateSignupToken( username, key );

        if ( ok ) {
            messageUtil.saveMessage( "Your account is now enabled. Log in to continue" );
            response.sendRedirect( response.encodeRedirectURL( request.getContextPath() + "/home.html" ) );
        } else {
            messageUtil.saveMessage( "Sorry, your registration could not be validated. Please register again." );
            response.sendRedirect( response.encodeRedirectURL( request.getContextPath() + "/signup.html" ) );
        }

    }

    /**
     * AJAX DWR
     *
     * @return loginDetails
     */
    public LoginDetailsValueObject loginCheck() {

        LoginDetailsValueObject LDVo = new LoginDetailsValueObject();

        if ( userManager.loggedIn() ) {
            assert userManager.getCurrentUsername() != null;
            LDVo.setUserName( userManager.getCurrentUsername() );
            LDVo.setLoggedIn( true );
        } else {
            LDVo.setLoggedIn( false );
        }

        return LDVo;

    }

    /*
     * Used when a user signs themselves up.
     */
    @RequestMapping(value = "/signup.html", method = RequestMethod.POST)
    public void signup(
            @RequestParam("password") String password,
            @RequestParam("passwordConfirm") String cPass,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("emailConfirm") String cEmail,
            @RequestParam(value = "ajaxRegisterTrue", defaultValue = "false") boolean ajaxRegisterTrue,
            HttpServletRequest request, HttpServletResponse response, Locale locale ) throws Exception {
        try {
            if ( reCaptcha.isPrivateKeySet() && !reCaptcha.validateRequest( request ).isValid() ) {
                throw new IllegalArgumentException( "Captcha was not entered correctly." );
            }

            if ( password.length() < UserFormMultiActionController.MIN_PASSWORD_LENGTH ) {
                throw new IllegalArgumentException( "Password must be at least " + UserFormMultiActionController.MIN_PASSWORD_LENGTH + " characters long." );
            }

            if ( !password.equals( cPass ) ) {
                throw new IllegalArgumentException( "Password does not match the confirmation." );
            }

            if ( !UserFormMultiActionController.RFC_5322_EMAIL_PATTERN.matcher( email ).matches() ) {
                throw new IllegalArgumentException( "Invalid email address." );
            }

            if ( !email.equals( cEmail ) ) {
                throw new IllegalArgumentException( "Email does not match the confirmation." );
            }

            UserDetailsImpl u = userManager.createUser( username, email, password );
            try {
                mailService.sendSignupConfirmationEmail( u.getEmail(), u.getUsername(), u.getSignupToken(), locale );
            } catch ( MailException e ) {
                log.error( "Couldn't send email with confirmation link to " + email + ".", e );
            }

            // See if this comes from AjaxRegister.js, if it does don't save confirmation message
            if ( !ajaxRegisterTrue ) {
                messageUtil.saveMessage( "signup.email.sent", email,
                        "A confirmation email was sent. Please check your mail and click the link it contains" );
            }
            JsonUtil.writeSuccessToResponse( response );
        } catch ( Exception e ) {
            /*
             * Most common cause: user exists already.
             */
            log.error( "User registration failed.", e );
            JsonUtil.writeErrorToResponse( e, response );
        }
    }

    @RequestMapping(value = "/signup.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public String signupForm() {
        return "register";
    }
}
