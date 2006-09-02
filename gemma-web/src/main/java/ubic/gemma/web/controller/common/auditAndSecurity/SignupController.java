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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.BindException;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.Constants;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserExistsException;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.model.common.auditAndSecurity.UserRoleService;
import ubic.gemma.util.RequestUtil;
import ubic.gemma.util.StringUtil;
import ubic.gemma.web.controller.BaseFormController;

/**
 * Controller to signup new users. Based on code from Appfuse.
 * <hr>
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="signupController" name="/signup.html"
 * @spring.property name="formView" value="signup"
 * @spring.property name="validator" ref="userValidator"
 * @spring.property name="successView" value="redirect:mainMenu.html"
 * @spring.property name="commandName" value="user"
 * @spring.property name="commandClass" value="ubic.gemma.model.common.auditAndSecurity.User"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="userRoleService" ref="userRoleService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="message" ref="mailMessage"
 * @spring.property name="templateName" value="accountCreated.vm"
 */
public class SignupController extends BaseFormController {
    private UserRoleService userRoleService;

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        User user = ( User ) command;
        Locale locale = request.getLocale();

        Boolean encrypt = ( Boolean ) getConfiguration().get( Constants.ENCRYPT_PASSWORD );
        
        log.debug( "Encryption is active" );

        if ( encrypt != null && encrypt.booleanValue() ) {
            String algorithm = ( String ) getConfiguration().get( Constants.ENC_ALGORITHM );

            if ( algorithm == null ) { // should only happen for test case
                log.debug( "assuming testcase, setting algorithm to 'SHA'" );
                algorithm = "SHA";
            }

            user.setPassword( StringUtil.encodePassword( user.getPassword(), algorithm ) );
        }

        user.setEnabled( true );

        // Set the default user role on this new user
        UserRole role = this.userRoleService.getRole( Constants.USER_ROLE );
        role.setUserName( user.getUserName() ); // FIXME = UserRoleService should set this.
        user.getRoles().add( role );

        try {
            log.info( "Signing up " + user + " " + user.getUserName() );
            User savedUser = this.userService.saveUser( user );
            assert savedUser != null;
        } catch ( UserExistsException e ) {
            log.warn( e.getMessage() );

            errors.rejectValue( "userName", "errors.existing.user",
                    new Object[] { user.getUserName(), user.getEmail() }, "duplicate user" );

            // redisplay the unencrypted passwords
            user.setPassword( user.getConfirmPassword() );
            return showForm( request, response, errors );
        }

        saveMessage( request, getText( "user.registered", user.getUserName(), locale ) );
       

        // log user in automatically
        assert user.getUserName() != null;
        Authentication auth = new UsernamePasswordAuthenticationToken( user.getUserName(), user.getConfirmPassword() );
        try {
            ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext( request.getSession()
                    .getServletContext() );
            if ( ctx != null ) {
                ProviderManager authenticationManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
                Authentication authentication = authenticationManager.doAuthentication( auth );
                assert authentication.isAuthenticated() : "New user " + user.getUserName()
                        + " wasn't authenticated with password " + user.getPassword() + ".";
                SecurityContextHolder.getContext().setAuthentication( authentication );
            }
        } catch ( NoSuchBeanDefinitionException n ) {
            // ignore, should only happen when testing
        }

        // Send user an e-mail
        if ( log.isDebugEnabled() ) {
            log.debug( "Sending user '" + user.getUserName() + "' an account information e-mail" );
        }

        // Send an account information e-mail
        message.setSubject( getText( "signup.email.subject", locale ) );
        sendEmail( user, getText( "signup.email.message", locale ), RequestUtil.getAppURL( request ) );

        request.getSession().setAttribute( Constants.REGISTERED, Boolean.TRUE );
        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param roleManager The roleManager to set.
     */
    public void setUserRoleService( UserRoleService userRoleService ) {
        this.userRoleService = userRoleService;
    }

    @Override
    @SuppressWarnings("unused")
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return User.Factory.newInstance();
    }

}