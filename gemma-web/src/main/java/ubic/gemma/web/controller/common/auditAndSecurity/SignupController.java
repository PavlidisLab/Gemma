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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptcha;
import ubic.gemma.web.util.JsonUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller to signup new users. See also the UserListController.
 *
 * @author pavlidis
 * @author keshav
 */
@Controller
public class SignupController extends BaseController implements InitializingBean {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserManager userManager;

    @Autowired
    private ServletContext servletContext;

    private ReCaptcha reCaptcha = new ReCaptcha( Settings.getString( "gemma.recaptcha.privateKey" ) );

    @Override
    public void afterPropertiesSet() {
        if ( reCaptcha.isPrivateKeySet() ) {
            log.warn( "No recaptcha private key is configured, skipping validation" );
        }
    }

    @RequestMapping(value = "/ajaxLoginCheck.html")
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
    @RequestMapping("/confirmRegistration.html")
    public void confirmRegistration( @RequestParam("username") String username, @RequestParam("key") String key,
            HttpServletRequest request, HttpServletResponse response ) throws Exception {
        if ( StringUtils.isBlank( username ) || StringUtils.isBlank( key ) ) {
            throw new IllegalArgumentException(
                    "The confirmation url was not valid; it must contain the key and username" );
        }

        boolean ok = userManager.validateSignupToken( username, key );

        if ( ok ) {
            super.saveMessage( request, "Your account is now enabled. Log in to continue" );
            response.sendRedirect( response.encodeRedirectURL( request.getContextPath() + "/home.html" ) );
        } else {
            super.saveMessage( request, "Sorry, your registration could not be validated. Please register again." );
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
            LDVo.setUserName( userManager.getCurrentUsername() );
            LDVo.setLoggedIn( true );
        } else {
            LDVo.setLoggedIn( false );
        }

        return LDVo;

    }

    /**
     * @param passwordEncoder the passwordEncoder to set
     */
    public void setPasswordEncoder( PasswordEncoder passwordEncoder ) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @param userManager the userManager to set
     */
    public void setUserManager( UserManager userManager ) {
        this.userManager = userManager;
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

    @RequestMapping(value = "/signup.html", method = RequestMethod.GET)
    public String signupForm() {
        return "register";
    }

    /*
     * Send an email to request signup confirmation.
     */
    private void sendSignupConfirmationEmail( HttpServletRequest request, UserDetailsImpl u ) {
        String email = u.getEmail();

        Map<String, Object> model = new HashMap<>();
        model.put( "siteurl", Settings.getHostUrl() + servletContext.getContextPath() + "/" );

        this.sendConfirmationEmail( request, u.getSignupToken(), u.getUsername(), email, model, "accountCreated.vm" );

        // See if this comes from AjaxRegister.js, if it does don't save confirmation message
        String ajaxRegisterTrue = request.getParameter( "ajaxRegisterTrue" );

        if ( ajaxRegisterTrue == null || !ajaxRegisterTrue.equals( "true" ) ) {
            this.saveMessage( request, "signup.email.sent", email,
                    "A confirmation email was sent. Please check your mail and click the link it contains" );
        }
    }

    public void setRecaptchaTester( ReCaptcha reCaptcha ) {
        this.reCaptcha = reCaptcha;
    }
}
