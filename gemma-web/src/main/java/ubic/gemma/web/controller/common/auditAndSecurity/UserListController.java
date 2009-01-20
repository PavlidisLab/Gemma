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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ubic.gemma.Constants;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserExistsException;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.util.SecurityUtil;

/**
 * Simple class to retrieve a list of users from the database. From appfuse.
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="userListController"
 * @spring.property name="userService" ref="userService"
 */
public class UserListController implements Controller {
    private transient final Log log = LogFactory.getLog( UserListController.class );
    private UserService userService = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.Controller#handleRequest(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unused")
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        // TODO remove this method (is spring mvc based). Also remove /users.html and activeUsers.html from the
        // security context xml file
        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'handleRequest' method..." );
        }

        return new ModelAndView( "userList", Constants.USER_LIST, userService.loadAll() );
    }

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

    /**
     * Save the user.
     * 
     * @param user
     */
    public void saveUser( UserValueObject user ) throws UserExistsException {

        String userName = user.getUserName();
        User u = userService.findByUserName( userName );
        /* other fields */
        String email = user.getEmail();
        boolean enabled = user.isEnabled();
        String role = user.getRole();
        if ( u == null ) {
            /* new user */
            u = User.Factory.newInstance();
            u.setUserName( userName );
            u.setEmail( email );
            u.setEnabled( enabled );
            SecurityUtil.addRole( u, role );

            userService.create( u );
        } else {
            /* update user */
            userService.update( u );
        }
        // TODO when changing the roles (from user to say, admin), must also change the acl_permission's
        // acl_object_identity and mask
    }

    /**
     * @param userService
     */
    public void setUserService( UserService userService ) {
        this.userService = userService;
    }

}
