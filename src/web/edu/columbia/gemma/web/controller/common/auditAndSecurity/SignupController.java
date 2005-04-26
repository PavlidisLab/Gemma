package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserExistsException;
import edu.columbia.gemma.common.auditAndSecurity.UserRoleService;
import edu.columbia.gemma.util.RequestUtil;
import edu.columbia.gemma.util.StringUtil;
import edu.columbia.gemma.web.Constants;
import edu.columbia.gemma.web.controller.BaseFormController;

/**
 * Controller to signup new users. Based on code from Appfuse.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="signupController" name="/signup.html"
 * @spring.property name="formView" value="signup"
 * @spring.property name="validator" ref="userValidator"
 * @spring.property name="successView" value="redirect:mainMenu.html"
 * @spring.property name="commandName" value="user"
 * @spring.property name="commandClass" value="edu.columbia.gemma.common.auditAndSecurity.User"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="userRoleService" ref="userRoleService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="message" ref="mailMessage"
 * @spring.property name="templateName" value="accountCreated.vm"
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 */
public class SignupController extends BaseFormController {
    private UserRoleService userRoleService;

    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering 'onSubmit' method..." );

        User user = ( User ) command;
        Locale locale = request.getLocale();

        String algorithm = ( String ) getConfiguration().get( Constants.ENC_ALGORITHM );

        if ( algorithm == null ) { // should only happen for test case
            log.debug( "assuming testcase, setting algorithm to 'SHA'" );
            algorithm = "SHA";
        }

        user.setPassword( StringUtil.encodePassword( user.getPassword(), algorithm ) );

        // Set the default user role on this new user
        userService.addRole( user, userRoleService.getRole( Constants.USER_ROLE ) );

        try {
            userService.saveUser( user );
        } catch ( UserExistsException e ) {
            log.warn( e.getMessage() );

            errors.rejectValue( "username", "errors.existing.user",
                    new Object[] { user.getUserName(), user.getEmail() }, "duplicate user" );

            // redisplay the unencrypted passwords
            user.setPassword( user.getConfirmPassword() );
            return showForm( request, response, errors );
        }

        // Set cookies for auto-magical login ;-)
        String loginCookie = userService.createLoginCookie( user.getUserName() );
        RequestUtil.setCookie( response, Constants.LOGIN_COOKIE, loginCookie, request.getContextPath() );

        saveMessage( request, getText( "user.registered", user.getUserName(), locale ) );

        request.getSession().setAttribute( Constants.REGISTERED, Boolean.TRUE );

        // Send user an e-mail
        log.debug( "Sending user '" + user.getUserName() + "' an account information e-mail" );

        // Send an account information e-mail
        message.setSubject( getText( "signup.email.subject", locale ) );
        sendUserMessage( user, getText( "signup.email.message", locale ), RequestUtil.getAppURL( request ) );

        return new ModelAndView( getSuccessView() );
    }
    
    /**
     * @param roleManager The roleManager to set.
     */
    public void setUserRoleService( UserRoleService userRoleService ) {
        this.userRoleService = userRoleService;
    }

    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return User.Factory.newInstance();
    }
}