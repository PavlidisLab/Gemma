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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.web.controller.BaseFormController;

/**
 * For display and editing of users.
 * 
 * @author pavlidis
 * @version $Id$
 * @see UserFormMultiActionController
 * @see SignupController
 */
@Controller
public class UserListController extends BaseFormController {

    @Autowired
    private UserManager userManager;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * AJAX entry point.
     * 
     * @return
     */
    public Collection<UserValueObject> getUsers() {
        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'getUsers' method..." );
        }

        Collection<UserValueObject> userValueObjects = new ArrayList<UserValueObject>();

        Collection<User> users = userService.loadAll();
        for ( User u : users ) {
            UserValueObject uv = new UserValueObject( u );
            userValueObjects.add( uv );
        }
        return userValueObjects;
    }

    @Override
    @RequestMapping(value = "/admin/activeUsers.html", method = RequestMethod.GET)
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        /*
         * FIXME: this lists all users, not the ones who are active.
         */
        return new ModelAndView( "/admin/activeUsers", "users", userService.loadAll() );
    }

    /**
     * Save or create the user. FIXME this is pretty inflexible - need to be able to reset password.
     * 
     * @param user
     */
    public void saveUser( UserValueObject user ) {

        String userName = user.getUserName();
        User u = userService.findByUserName( userName );

        UserDetailsImpl userDetails;

        boolean newUser = false;
        if ( u == null ) {
            userDetails = new UserDetailsImpl(
                    passwordEncoder.encodePassword( user.getPassword(), user.getUserName() ), user.getUserName(),
                    false, null, user.getEmail(), userManager.generateSignupToken( user.getUserName() ), new Date() );
        } else {
            u.setEmail( user.getEmail() );
            userDetails = new UserDetailsImpl( u );
        }

        /*
         * When changing the roles (from user to say, admin), we must first create a new or update an existing user,
         * THEN change the acl_permission's acl_object_identity and mask. This must be done in two separate steps: first
         * add the acl permissions to the acl_permission and acl_object_identity tables (when creating a new user), then
         * change the permissions.
         */

        if ( newUser ) {
            /*
             * FIXME: send the user an email message containing the account details and a confirmation link.
             */
            sendSignupConfirmationEmail( userDetails );
            userManager.createUser( userDetails );
        } else {
            userManager.updateUser( userDetails );
        }

    }

    /**
     * Send an email to request signup confirmation.
     * 
     * @param request
     * @param u
     */
    private void sendSignupConfirmationEmail( UserDetailsImpl u ) {

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom( ConfigUtils.getAdminEmailAddress() );
        mailMessage.setSubject( "An account was created for you on Gemma" );
        try {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put( "username", u.getUsername() );

            /*
             * FIXME: make this url configurable.
             */
            model.put( "confirmLink", "http://www.chibi.ubc.ca/Gemma/confirmRegistration.html?key="
                    + u.getSignupToken() );
            // model.put( "message", getText( "signup.email.message", request.getLocale() ) );

            /*
             * FIXME: make the template name configurable.
             */
            String templateName = "accountCreated.vm";
            // sendEmail( u.getUsername(), u.getEmail(), templateName, model );
            // this.saveMessage( request, "signup.email.sent", u.getEmail(),
            // "A confirmation email was sent. Please check your mail and click the link it contains" );
        } catch ( Exception e ) {
            log.error( "Couldn't send email to " + u.getEmail(), e );
        }

    }

}
