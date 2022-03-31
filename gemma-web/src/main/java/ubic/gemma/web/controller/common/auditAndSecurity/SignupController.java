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
import gemma.gsec.util.JSONUtil;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.controller.common.auditAndSecurity.recaptcha.ReCaptcha;

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
public class SignupController extends BaseController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserManager userManager;

    private ReCaptcha reCaptcha = new ReCaptcha( Settings.getString( "gemma.recaptcha.privateKey" ) );

    @RequestMapping(value = "/ajaxLoginCheck.html")
    public void ajaxLoginCheck( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = "{success:false}";
        String userName;

        try {

            if ( userManager.loggedIn() ) {
                userName = userManager.getCurrentUsername();
                jsonText = "{success:true,user:\'" + userName + "\',isAdmin:" + SecurityUtil.isUserAdmin() + "}";
            } else {
                jsonText = "{success:false}";
            }
        } catch ( Exception e ) {

            log.error( e, e );
            jsonText = jsonUtil.getJSONErrorMessage( e );
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }

    }

    /*
     * This is hit when a user clicks on the confirmation link they received by email.
     */
    @RequestMapping("/confirmRegistration.html")
    public void confirmRegistration( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        String username = request.getParameter( "username" );
        String key = request.getParameter( "key" );

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
    public void signup( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;

        String password = request.getParameter( "password" );

        String cPass = request.getParameter( "passwordConfirm" );


        if ( reCaptcha.isPrivateKeySet() ) {

            if ( !reCaptcha.validateRequest( request ).isValid() ) {
                jsonText = "{success:false,message:'Captcha was not entered correctly.'}";
                jsonUtil.writeToResponse( jsonText );
                return;
            }

        } else {
            log.warn( "No recaptcha private key is configured, skipping validation" );
        }

        if ( password.length() < UserFormMultiActionController.MIN_PASSWORD_LENGTH || !password.equals( cPass ) ) {
            jsonText = "{success:false,message:'Password was not valid or didn't match'}";
            jsonUtil.writeToResponse( jsonText );
            return;
        }

        String username = request.getParameter( "username" );

        String encodedPassword = passwordEncoder.encodePassword( password, username );

        String email = request.getParameter( "email" );

        String cEmail = request.getParameter( "emailConfirm" );

        /*
         * Validate that it is a valid email....this regex adapted from extjs; a word possibly containing '-', '+' or
         * '.', following by '@', followed by up to 5 chunks separated by '.', finally a 2-4 letter alphabetic suffix.
         */
        if ( !email.matches( "^(\\w+)([-+.][\\w]+)*@(\\w[-\\w]*\\.){1,5}([A-Za-z]){2,4}$" ) || !email
                .equals( cEmail ) ) {
            jsonText = "{success:false,message:'Email was not valid or didn't match'}";
            jsonUtil.writeToResponse( jsonText );
            return;
        }

        String key = userManager.generateSignupToken( username );

        Date now = new Date();

        UserDetailsImpl u = new UserDetailsImpl( encodedPassword, username, false, null, email, key, now );

        try {
            userManager.createUser( u );
            sendSignupConfirmationEmail( request, u );

            jsonText = "{success:true}";
        } catch ( Exception e ) {
            /*
             * Most common cause: user exists already.
             */
            log.error( e, e );
            jsonText = jsonUtil.getJSONErrorMessage( e );
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }
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
        model.put( "siteurl", Settings.getBaseUrl() );

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
