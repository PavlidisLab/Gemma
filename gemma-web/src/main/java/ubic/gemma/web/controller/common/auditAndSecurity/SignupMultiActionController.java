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

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.util.UserConstants;
import ubic.gemma.web.util.JSONUtil;

/**
 * Controller to signup new users.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="signupMultiActionController"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="mailMessage" ref="mailMessage"
 * @spring.property name="templateName" value="accountCreated.vm"
 * @spring.property name="methodNameResolver" ref="signupActions"
 */
public class SignupMultiActionController extends UserAuthenticatingMultiActionController {

    private static final int PASSWORD_LENGTH = 8;

    /**
     * AJAX entry point.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void onSubmit( HttpServletRequest request, HttpServletResponse response ) {

        Locale locale = request.getLocale();

        /* generate a password for this user. User can then change it. */
        String generatedPassword = RandomStringUtils.randomAlphanumeric( PASSWORD_LENGTH );
        String encryptedPassword = super.encryptPassword( generatedPassword, request );

        String username = request.getParameter( "username" );
        String email = request.getParameter( "email" );

        User user = User.Factory.newInstance();
        user.setPassword( encryptedPassword );
        user.setUserName( username );
        user.setEmail( email );
        user.setEnabled( true );
        addRole( user );

        User savedUser;
        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;
        try {
            savedUser = userService.create( user );
            signInUser( request, savedUser, generatedPassword );
            super.sendConfirmationEmail( request, user, locale, generatedPassword );
            jsonText = "{success:true}";
        } catch ( Exception e ) {
            e.printStackTrace();
            jsonText = "{ success: false, errors: { reason: '" + e.getLocalizedMessage() + ".' }}";
        } finally {
            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Adds a role to the user.
     * 
     * @param user
     */
    private void addRole( User user ) {

        // Set the default user role on this new user
        UserRole role = null;

        String message = "added by " + this.getClass().getName();

        // TODO If setAsAdmin is true, sets the user as an administrator. This was from the old spring mvc
        // boolean setAsAdmin = user.getAdminUser();
        // if ( setAsAdmin ) {
        // role = UserRole.Factory.newInstance( user.getUserName(), UserConstants.ADMIN_ROLE, message );
        // } else {
        role = UserRole.Factory.newInstance( user.getUserName(), UserConstants.USER_ROLE, message );
        // }

        user.getRoles().add( role );
    }
}