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

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.Constants;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserExistsException;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.model.common.auditAndSecurity.UserRoleService;

/**
 * Controller to signup new users. Based on code from Appfuse.
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
public class SignupController extends UserAuthenticatingController {
    private UserRoleService userRoleService;

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        User user = ( User ) command;
        Locale locale = request.getLocale();

        assert user.getPassword().equals( user.getConfirmPassword() ); // should be checked by validation!

        String unencryptedPassword = user.getPassword();
        encryptPassword( user, request );
        user.setEnabled( true );
        addUserRole( user );

        try {
            log.info( "Signing up " + user + " " + user.getUserName() );

            User savedUser = this.userService.saveUser( user );

            assert savedUser != null;
        } catch ( UserExistsException e ) {
            log.warn( e.getMessage() );

            errors.rejectValue( "userName", "errors.existing.user",
                    new Object[] { user.getUserName(), user.getEmail() }, "duplicate user" );

            // redisplay the unencrypted passwords
            user.setPassword( unencryptedPassword );
            user.setConfirmPassword( unencryptedPassword );
            return showForm( request, response, errors );
        }

        // log user in automatically
        signInUser( request, user, unencryptedPassword );

        sendConfirmationEmail( request, user, locale );

        request.getSession().setAttribute( Constants.REGISTERED, Boolean.TRUE );
        saveMessage( request, getText( "user.registered", user.getUserName(), locale ) );
        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param user
     */
    private void addUserRole( User user ) {
        // Set the default user role on this new user
        UserRole role = this.userRoleService.getRole( Constants.USER_ROLE );
        assert role != null : "Role was null";
        role.setUserName( user.getUserName() ); // FIXME = UserRoleService should set this.
        user.getRoles().add( role );
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