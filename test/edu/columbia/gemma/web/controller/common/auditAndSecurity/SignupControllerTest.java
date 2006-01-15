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
package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;

import edu.columbia.gemma.BaseControllerTestCase;
import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserRole;
import edu.columbia.gemma.common.auditAndSecurity.UserService;

/**
 * Tests the SignupController. Is also used to add an admin.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author keshav
 * @version $Id$
 */
public class SignupControllerTest extends BaseControllerTestCase {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    SignupController signupController;

    User testUser;
    UserRole ur;
    UserService userService;

    /**
     * @throws Exception
     */
    public void setUp() throws Exception {

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        signupController = ( SignupController ) ctx.getBean( "signupController" );

        testUser = User.Factory.newInstance();
        ur = UserRole.Factory.newInstance();
        userService = ( UserService ) ctx.getBean( "userService" );

    }

    /**
     * Tear down objects.
     */
    public void tearDown() {
        signupController = null;
    }

    /**
     * Tests the SignupController
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testOnSubmit() throws Exception {

        request.setContextPath( "/Gemma" );
        request.setServletPath( "/test.signup.html" );
        request.setLocalName( "en_US" );

        String rand = ( new Date() ).toString();

        String adminName = "admin";
        String userName = "user";
        User checkUser = userService.getUser( adminName );

        if ( ( checkUser == null ) ) {
            testUser.setUserName( adminName );
            ur.setUserName( adminName );
            ur.setName( adminName );

        } else {
            testUser.setUserName( rand );
            ur.setUserName( rand );
            ur.setName( userName );
        }

        Set set = new HashSet();
        testUser.setRoles( set );

        testUser.setPassword( "dc76e9f0c0006e8f919e0c515c66dbba3982f785" );
        testUser.setConfirmPassword( "dc76e9f0c0006e8f919e0c515c66dbba3982f785" );
        testUser.setPasswordHint( "test hint" );

        testUser.getRoles().add( ur );

        signupController.onSubmit( request, response, testUser, ( BindException ) null );

        // assertEquals( "pubMedList", mav.getViewName() );
        // FIXME this test has no fail condition.
    }

}