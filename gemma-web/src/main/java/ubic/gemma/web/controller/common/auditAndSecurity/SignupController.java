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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.security.authentication.LoginDetailsValueObject;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.util.JSONUtil;

/**
 * Controller to signup new users. See also the {@see UserListController}.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
@Controller
public class SignupController extends BaseController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserManager userManager;

    /**
     * This is hit when a user clicks on the confirmation link they received by email.
     * 
     * @param request
     * @param response
     * @throws Exception
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
            response.sendRedirect( response.encodeRedirectURL( "/Gemma/home.html" ) );
        } else {
            super.saveMessage( request, "Sorry, your registration could not be validated. Please register again." );
            response.sendRedirect( response.encodeRedirectURL( "/Gemma/signup.html" ) );
        }

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

    /**
     * Used when a user signs themselves up.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/signup.html", method = RequestMethod.POST)
    public void signup( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;

        String password = request.getParameter( "password" );

        String cPass = request.getParameter( "passwordConfirm" );

        String recatpchaPvtKey = ConfigUtils.getString( "gemma.recaptcha.privateKey" );

        if ( StringUtils.isNotBlank( recatpchaPvtKey ) ) {

            boolean valid = validateCaptcha( request, recatpchaPvtKey );

            if ( !valid ) {
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
         * Validate that it is a valid email....this regex adapted from extjs; a word possibly containingi '-', '+' or
         * '.', following by '@', followed by up to 5 chunks separated by '.', finally a 2-4 letter alphabetic suffix.
         */
        if ( !email.matches( "^(\\w+)([-+.][\\w]+)*@(\\w[-\\w]*\\.){1,5}([A-Za-z]){2,4}$" ) || !email.equals( cEmail ) ) {
            jsonText = "{success:false,message:'Email was not valid or didn't match'}";
            jsonUtil.writeToResponse( jsonText );
            return;
        }

        String key = userManager.generateSignupToken( username );

        Date now = new Date();

        boolean enabled = false;
        UserDetailsImpl u = new UserDetailsImpl( encodedPassword, username, enabled, null, email, key, now );

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

    /**
     * Send an email to request signup confirmation.
     * 
     * @param request
     * @param u
     */
    private void sendSignupConfirmationEmail( HttpServletRequest request, UserDetailsImpl u ) {

        // Send an account information e-mail
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom( ConfigUtils.getAdminEmailAddress() );
        mailMessage.setSubject( getText( "signup.email.subject", request.getLocale() ) );
        try {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put( "username", u.getUsername() );

            model.put( "password", "" );

            model.put( "confirmLink", ConfigUtils.getBaseUrl() + "confirmRegistration.html?key=" + u.getSignupToken()
                    + "&username=" + u.getUsername() );
            model.put( "message", getText( "signup.email.message", request.getLocale() ) );
            model.put( "overviewURL", ConfigUtils.getBaseUrl() + "static/about.html" );
            model.put( "faqURL", ConfigUtils.getBaseUrl() + "resources/faq.html" );
            model.put( "wikiURL", ConfigUtils.getBaseUrl() + "faculty/pavlidis/wiki/display/gemma" );

            /*
             * FIXME: make the template name configurable.
             */
            String templateName = "accountCreated.vm";
            sendEmail( u.getUsername(), u.getEmail(), "Successful registration for Gemma", templateName, model );

            //See if this comes from AjaxRegister.js, if it does don't save confirmation message
            String ajaxRegisterTrue = request.getParameter( "ajaxRegisterTrue" );

            if ( ajaxRegisterTrue == null || !ajaxRegisterTrue.equals( "true" ) ) {

                this.saveMessage( request, "signup.email.sent", u.getEmail(),
                        "A confirmation email was sent. Please check your mail and click the link it contains" );

            }

        } catch ( Exception e ) {
            log.error( "Couldn't send email to " + u.getEmail(), e );
        }

    }

    /**
     * @param request
     * @param recatpchaPvtKey
     * @return
     */
    private boolean validateCaptcha( HttpServletRequest request, String recatpchaPvtKey ) {
        String rcChallenge = request.getParameter( "recaptcha_challenge_field" );
        String rcResponse = request.getParameter( "recaptcha_response_field" );

        String remoteAddr = request.getRemoteAddr();
        ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
        reCaptcha.setPrivateKey( recatpchaPvtKey );
        ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer( remoteAddr, rcChallenge, rcResponse );
        return reCaptchaResponse.isValid();
    }

    @RequestMapping(value = "/ajaxLoginCheck.html")
    public void ajaxLoginCheck( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = "{success:false}";
        String userName = null;
        
        try {

            if ( userManager.loggedIn() ) {
                userName = userManager.getCurrentUser().getUserName();
                jsonText = "{success:true,user:\'" + userName + "\',isAdmin:"+SecurityServiceImpl.isUserAdmin()+"}";
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
    
    /**
     * AJAX DWR
     * 
     * @return loginDetails
     */    
    public LoginDetailsValueObject loginCheck(){

        LoginDetailsValueObject ldvo= new LoginDetailsValueObject();
        
            if ( userManager.loggedIn() ) {
                ldvo.setUserName( userManager.getCurrentUser().getUserName() );
                ldvo.setLoggedIn( true );
            } else {
                ldvo.setLoggedIn( false );
            }
            
            return ldvo;        
        
    }
}