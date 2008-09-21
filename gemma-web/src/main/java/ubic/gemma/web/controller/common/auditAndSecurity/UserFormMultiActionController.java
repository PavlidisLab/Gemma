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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.web.util.JSONUtil;

/**
 * Controller to edit profile of users.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="userFormMultiActionController"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="mailMessage" ref="mailMessage"
 * @spring.property name="methodNameResolver" ref="editUserActions"
 */
public class UserFormMultiActionController extends UserAuthenticatingMultiActionController {

    public void loadUser( HttpServletRequest request, HttpServletResponse response ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication.isAuthenticated();

        if ( !isAuthenticated ) {
            log.error( "User not authenticated.  Cannot populate user data." );
            return;
        }

        String username = authentication.getPrincipal().toString();
        User user = userService.findByUserName( username );
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = "{\"user\": {\"data\": [ {\"class\":\"ubic.gemma.model.common.auditAndSecurity.User\",\"id\":"
                + user.getId() + ",\"username\":\"" + user.getUserName() + "\" } ] } }";
        // String jsonText = "{user: {username:"+user.getUserName()+"}}";
        try {
            jsonUtil.writeToResponse( jsonText );
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * AJAX entry point.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public void onSubmit( HttpServletRequest request, HttpServletResponse response ) {

        String email = request.getParameter( "email" );
        String firstname = request.getParameter( "firstname" );
        String lastname = request.getParameter( "lastname" );
        String password = request.getParameter( "password" );
        String passwordConfirm = request.getParameter( "passwordConfirm" );

        /*
         * Pulling username out of security context to ensure users are logged in and can only update themselves.
         */
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userService.findByUserName( username );
        if ( !StringUtils.equals( password, passwordConfirm ) ) {
            throw new RuntimeException( "Passwords do not match." );
        }
        String encryptedPassword = super.encryptPassword( password, request );
        user.setPassword( encryptedPassword );

        if ( StringUtils.isNotBlank( firstname ) ) {
            user.setName( firstname );
        }

        if ( StringUtils.isNotBlank( lastname ) ) {
            user.setName( lastname );
        }

        user.setEmail( email );

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;
        try {
            userService.update( user );
            jsonText = "{success:true}";
        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage() );
            jsonText = jsonUtil.getJSONErrorMessage( e );
            log.info( jsonText );
        } finally {
            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }
}