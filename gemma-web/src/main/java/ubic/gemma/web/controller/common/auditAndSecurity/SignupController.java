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
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.MailEngine;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptcha;
import ubic.gemma.web.controller.util.JsonUtil;
import ubic.gemma.web.controller.util.MessageUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserManager userManager;
    @Autowired
    private MailEngine mailEngine;
    @Autowired
    protected MessageSource messageSource;
    @Autowired
    protected MessageUtil messageUtil;
    @Autowired
    private ServletContext servletContext;

    @Value("${gemma.hosturl}")
    private String hostUrl;
    @Value("${gemma.recaptcha.privateKey}")
    private String recaptchaPrivateKey;

    private ReCaptcha reCaptcha;

    @Override
    public void afterPropertiesSet() {
        reCaptcha = new ReCaptcha( recaptchaPrivateKey );
        if ( reCaptcha.isPrivateKeySet() ) {
            log.warn( "No recaptcha private key is configured, skipping validation" );
        }
    }

    @RequestMapping(value = "/ajaxLoginCheck.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void ajaxLoginCheck( HttpServletResponse response ) throws Exception {
        JSONObject json;
        try {
            if ( userManager.loggedIn() ) {
                json = new JSONObject().put( "success", true )
                        .put( "user", userManager.getCurrentUsername() )
                        .put( "isAdmin", SecurityUtil.isUserAdmin() );
            } else {
                json = new JSONObject().put( "success", false );
            }
        } catch ( Exception e ) {
            log.error( "Error while checking if the current user is logged in.", e );
            JsonUtil.writeErrorToResponse( e, response );
            return;
        }
        JsonUtil.writeToResponse( json, response );
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
            HttpServletRequest request, HttpServletResponse response ) throws Exception {

        if ( reCaptcha.isPrivateKeySet() ) {

            if ( !reCaptcha.validateRequest( request ).isValid() ) {
                JsonUtil.writeErrorToResponse( HttpServletResponse.SC_BAD_REQUEST, "Captcha was not entered correctly.", response );
                return;
            }

        }

        if ( password.length() < UserFormMultiActionController.MIN_PASSWORD_LENGTH || !password.equals( cPass ) ) {
            JsonUtil.writeErrorToResponse( HttpServletResponse.SC_BAD_REQUEST, "Password was not valid or didn't match", response );
            return;
        }

        String encodedPassword = passwordEncoder.encodePassword( password, username );

        /*
         * Validate that it is a valid email....this regex adapted from extjs; a word possibly containing '-', '+' or
         * '.', following by '@', followed by up to 5 chunks separated by '.', finally a 2-4 letter alphabetic suffix.
         */
        if ( !email.matches( "^(\\w+)([-+.][\\w]+)*@(\\w[-\\w]*\\.){1,5}([A-Za-z]){2,4}$" ) || !email
                .equals( cEmail ) ) {
            JsonUtil.writeErrorToResponse( HttpServletResponse.SC_BAD_REQUEST, "Email was not valid or didn't match", response );
            return;
        }

        String key = userManager.generateSignupToken( username );

        Date now = new Date();

        UserDetailsImpl u = new UserDetailsImpl( encodedPassword, username, false, null, email, key, now );

        try {
            userManager.createUser( u );
            sendSignupConfirmationEmail( request, u );
        } catch ( Exception e ) {
            /*
             * Most common cause: user exists already.
             */
            log.error( String.format( "User registration failed: %s", ExceptionUtils.getRootCauseMessage( e ) ), e );
            JsonUtil.writeErrorToResponse( e, response );
            return;
        }

        JsonUtil.writeToResponse( new JSONObject().put( "success", true ), response );
    }

    @RequestMapping(value = "/signup.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public String signupForm() {
        return "register";
    }

    /*
     * Send an email to request signup confirmation.
     */
    private void sendSignupConfirmationEmail( HttpServletRequest request, UserDetailsImpl u ) {
        String email = u.getEmail();

        Map<String, Object> model = new HashMap<>();
        model.put( "siteurl", hostUrl + servletContext.getContextPath() + "/" );

        this.sendConfirmationEmail( u.getSignupToken(), u.getUsername(), email, model, "accountCreated.vm" );

        // See if this comes from AjaxRegister.js, if it does don't save confirmation message
        String ajaxRegisterTrue = request.getParameter( "ajaxRegisterTrue" );

        if ( ajaxRegisterTrue == null || !ajaxRegisterTrue.equals( "true" ) ) {
            messageUtil.saveMessage( "signup.email.sent", email,
                    "A confirmation email was sent. Please check your mail and click the link it contains" );
        }
    }

    private void sendConfirmationEmail( String token, String username, String email, Map<String, Object> model, String templateName ) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            model.put( "username", username );
            model.put( "confirmLink",
                    hostUrl + servletContext.getContextPath() + "/confirmRegistration.html?key=" + token + "&username=" + username );

            mailEngine.sendMessage( username + "<" + email + ">", this.messageSource.getMessage( "signup.email.subject", null, locale ), templateName, model );

        } catch ( Exception e ) {
            log.error( "Couldn't send email to " + email, e );
        }
    }

    public void setRecaptchaTester( ReCaptcha reCaptcha ) {
        this.reCaptcha = reCaptcha;
    }
}
