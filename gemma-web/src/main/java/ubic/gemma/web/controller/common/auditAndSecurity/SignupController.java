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

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserExistsException;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.util.UserConstants;

/**
 * Controller to signup new users. Based on code from Appfuse.
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="signupController"
 * @spring.property name="formView" value="signup"
 * @spring.property name="validator" ref="userValidator"
 * @spring.property name="successView" value="redirect:mainMenu.html"
 * @spring.property name="commandName" value="user"
 * @spring.property name="commandClass" value="ubic.gemma.web.controller.common.auditAndSecurity.UserUpdateCommand"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="mailMessage" ref="mailMessage"
 * @spring.property name="templateName" value="accountCreated.vm"
 */
public class SignupController extends UserAuthenticatingController {

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        UserUpdateCommand user = ( UserUpdateCommand ) command;
        Locale locale = request.getLocale();

        assert user.getNewPassword().equals( user.getConfirmNewPassword() ); // should be checked by validation!

        String unencryptedPassword = user.getNewPassword();
        encryptPassword( user, request );
        user.setEnabled( true );
        addRole( user );

        try {
            log.info( "Signing up " + user + " " + user.getUserName() );

            User savedUser = this.userService.create( user.asUser() );

            assert savedUser != null;
        } catch ( UserExistsException e ) {
            log.warn( e.getMessage() );

            errors.rejectValue( "userName", "errors.existing.user",
                    new Object[] { user.getUserName(), user.getEmail() }, "duplicate user" );

            return showForm( request, response, errors );
        }

        // log user in automatically
        signInUser( request, user.asUser(), unencryptedPassword );

        sendConfirmationEmail( request, user.asUser(), locale, unencryptedPassword );

        saveMessage( request, "user.registered", user.getUserName(), "Registered" );
        return new ModelAndView( getSuccessView() );
    }

    /**
     * Adds a role to the user. If setAsAdmin is true, sets the user as an administrator.
     * 
     * @param user
     */
    private void addRole( UserUpdateCommand user ) {
        // Set the default user role on this new user
        UserRole role = null;

        String message = "added by " + this.getClass().getName();

        boolean setAsAdmin = user.getAdminUser();
        if ( setAsAdmin ) {
            role = UserRole.Factory.newInstance( user.getUserName(), UserConstants.ADMIN_ROLE, message );
        } else {
            role = UserRole.Factory.newInstance( user.getUserName(), UserConstants.USER_ROLE, message );
        }

        user.getRoles().add( role );
    }

    @Override
    @SuppressWarnings("unused")
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return new UserUpdateCommand();
    }

}