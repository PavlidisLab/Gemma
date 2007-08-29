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

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse; 
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserDao;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.util.UserConstants;

/**
 * @author pavlidis
 * @version $Id$
 */
public class UserFormControllerTest extends BaseSpringWebTest {

    UserFormController controller;
    UserDao userDao;

    /**
     * @throws Exception
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        controller = ( UserFormController ) getBean( "userFormController" );
        userDao = ( UserDao ) getBean( "userDao" );
    }

    public final void testAdminCancel() throws Exception {
        MockHttpServletRequest request = newPost( "/editUser.html" );
        request.setParameter( "from", "list" );
        request.setParameter( "cancel", "true" );

        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertNull( "Errors returned in model: " + errors, errors );

        assertEquals( "redirect:users.html", mv.getViewName() );
    }

    public final void testCancel() throws Exception {
        MockHttpServletRequest request = newPost( "/editProfile.html" );

        request.setParameter( "cancel", "true" );

        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertNull( "Errors returned in model: " + errors, errors );
        assertEquals( "mainMenu", mv.getViewName() );

    }

    public final void testAdminResetPassword() throws Exception {
        MockHttpServletRequest request = newPost( "/editUser.html" );

        String password = "mypassword";
        User u = createNewUser( request, "newuser", password );
        request.setRemoteUser( "administrator" );
        request.addParameter( "roles", UserConstants.ADMIN_ROLE );
        request.setParameter( "from", "list" );

        assertNotNull( u );

        request.setParameter( "id", u.getId().toString() );
        request.setParameter( "userName", u.getUserName() );
        request.setParameter( "newPassword", "whatever" );
        request.setParameter( "confirmNewPassword", "whatever" );
        request.setParameter( "name", u.getName() );
        request.setParameter( "lastName", u.getLastName() );
        request.setParameter( "passwordHint", "guess" );
        request.setParameter( "email", u.getEmail() );

        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertTrue( "Errors returned in model: " + errors, errors == null || errors.getErrorCount() == 0 );

        assertEquals( "redirect:users.html", mv.getViewName() );

    }

    public final void testChangePasswordForExisting() throws Exception {
        MockHttpServletRequest request = newPost( "/editProfile.html" );

        User u = createNewUser( request, "newuser", "testing" );
        request.setRemoteUser( u.getUserName() );
        request.addParameter( "roles", UserConstants.USER_ROLE );

        controller.signInUser( request, u, "testing" );

        assertNotNull( u );

        request.setParameter( "id", u.getId().toString() );
        request.setParameter( "userName", u.getUserName() );
        request.setParameter( "newPassword", "whatever" );
        request.setParameter( "confirmNewPassword", "whatever" );
        request.setParameter( "name", u.getName() );
        request.setParameter( "lastName", u.getLastName() );
        request.setParameter( "email", u.getEmail() );

        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView mv = controller.handleRequest( request, response );
        assertNotNull( mv );

        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertNull( "Errors returned in model: " + errors, errors );

        assertEquals( "/mainMenu.html", ( ( RedirectView ) mv.getView() ).getUrl() );// getViewName() );

    }

    public final void testCreateNewFromForm() throws Exception {
        MockHttpServletRequest request = newPost( "/editUser.html" );

        setNewUserParameters( request );
        request.setParameter( "from", "list" );
        request.setParameter( "Add", "true" );
        request.setRemoteUser( "administrator" );
        request.addParameter( "roles", UserConstants.ADMIN_ROLE );

        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv = controller.handleRequest( request, response );
        assertNotNull( mv );

        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertNull( "Errors returned in model: " + errors, errors );

        // go back to list view.

        assertEquals( "redirect:users.html", mv.getViewName() );
    }

    public final void testDeleteNewFromList() throws Exception {
        MockHttpServletRequest request = newPost( "/editUser.html" );

        createNewUser( request, "newuser", "testing" );
        request.setParameter( "from", "list" );
        request.setRemoteUser( "administrator" );
        request.setParameter( "delete", "true" );
        request.addUserRole( UserConstants.ADMIN_ROLE );

        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertNull( "Errors returned in model: " + errors, errors );

        // go back to list view.
        assertNotNull( mv );
        assertEquals( "redirect:users.html", mv.getViewName() );

    }

    public final void testEditExistingDontChangePassword() throws Exception {
        MockHttpServletRequest request = newPost( "/editProfile.html" );

        // just edit 'myself'; don't put the user info in the request.
        User u = createNewUser( request, "newuser", "testing" );
        request.setRemoteUser( u.getUserName() );
        controller.signInUser( request, u, "testing" );

        request.setParameter( "firstName", "Something new" );

        request.addParameter( "roles", UserConstants.USER_ROLE );
        request.setRemoteUser( u.getUserName() );

        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView mv = controller.handleRequest( request, response );

        assertNotNull( mv );

        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertNull( "Errors returned in model: " + errors, errors );
        assertEquals( "/mainMenu.html", ( ( RedirectView ) mv.getView() ).getUrl() );// getViewName() );

    }

    public final void testEditExistingAddRole() throws Exception {
        MockHttpServletRequest request = newPost( "/editProfile.html" );

        // just edit 'myself'; don't put the user info in the request. User is created with USER_ROLE set.
        User u = createNewUser( request, "newuser", "testing" );
        request.setRemoteUser( u.getUserName() );
        controller.signInUser( request, u, "testing" );

        request.addParameter( "roles", UserConstants.USER_ROLE );
        request.setRemoteUser( u.getUserName() );

        request.setParameter( "firstName", "Something new" );

        // note use of add
        request.addParameter( "roles", UserConstants.ADMIN_ROLE );

        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView mv = controller.handleRequest( request, response );

        assertNotNull( mv );

        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertNull( "Errors returned in model: " + errors, errors );
        assertEquals( "/mainMenu.html", ( ( RedirectView ) mv.getView() ).getUrl() );// getViewName() );

        // make sure the user actualy has the roles

        u = ( User ) this.userDao.load( u.getId() );
        assertNotNull( u );

        assertNotNull( u.getRoles() );
        assertEquals( 2, u.getRoles().size() );

    }

    public final void testEditFromListDontChangePassword() throws Exception {
        MockHttpServletRequest request = newPost( "/editUser.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();

        User u = createNewUser( request, "newuser", "testing" );

        request.setParameter( "id", u.getId().toString() );
        request.setParameter( "userName", u.getUserName() );
        request.setParameter( "name", "Something new" );
        request.setParameter( "lastName", u.getLastName() );
        request.setParameter( "email", u.getEmail() );

        request.setParameter( "from", "list" );
        request.setRemoteUser( "administrator" );

        request.addParameter( "roles", UserConstants.ADMIN_ROLE );

        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertNull( "Errors returned in model: " + errors, errors );

        assertNotNull( mv );
        assertEquals( "redirect:users.html", mv.getViewName() );
    }

    public final void testShowFormFromList() throws Exception {
        MockHttpServletRequest request = newGet( "/editUser.html?userName=test&from=list" );
        request.setParameter( "userName", "test" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv = controller.handleRequest( request, response );
        assertEquals( "userProfile", mv.getViewName() );
    }

    public final void testShowForm() throws Exception {
        MockHttpServletRequest request = newGet( "/editProfile.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setRemoteUser( "administrator" );
        request.addUserRole( UserConstants.ADMIN_ROLE );

        ModelAndView mv = controller.handleRequest( request, response );

        assertNotNull( mv );

        assertEquals( "userProfile", mv.getViewName() );
    }

    /**
     * @param request
     * @return
     */
    private User createNewUser( MockHttpServletRequest request, String userName, String password ) {

        User u = this.getTestPersistentUser( userName, password );

        assert u != null && u.getEmail() != null && u.getPassword() != null;

        if ( request != null ) {
            setUserParameters( request, u );
        }
        return u;

    }

    /**
     * Fill the request with a random new user.
     * 
     * @param request
     */
    private String setNewUserParameters( MockHttpServletRequest request ) {
        String newUserName = RandomStringUtils.randomAlphabetic( 6 );

        request.setParameter( "userName", newUserName );
        request.setParameter( "password", "testpassword" );

        request.setParameter( "name", "Joe" );
        request.setParameter( "lastName", "Blow" );
        request.setParameter( "institution", "UBC" );
        request.setParameter( "email", RandomStringUtils.randomAlphabetic( 6 ) + "@gemma.org" );
        request.setParameter( "passwordHint", "guess" );
        request.addParameter( "roles", UserConstants.USER_ROLE );
        return newUserName;
    }

    /**
     * Fill the request with a specific user.
     * 
     * @param request
     */
    private void setUserParameters( MockHttpServletRequest request, User u ) {
        request.setParameter( "id", u.getId().toString() );
        request.setParameter( "userName", u.getUserName() );
        request.setParameter( "newPassword", "whatever" );
        request.setParameter( "confirmNewPassword", "whatever" );
        request.setParameter( "name", u.getName() );
        request.setParameter( "lastName", u.getLastName() );
        request.setParameter( "institution", "UBC" );
        request.setParameter( "email", u.getEmail() );
        request.setParameter( "passwordHint", u.getPasswordHint() );
        for ( UserRole r : u.getRoles() ) {
            request.addParameter( "roles", r.getName() );
        }

    }

}
