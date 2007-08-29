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

import ubic.gemma.testing.BaseSpringWebTest;

import com.dumbster.smtp.SimpleSmtpServer;

/**
 * Tests the SignupController.
 * 
 * @author keshav
 * @version $Id$
 */
public class SignupControllerTest extends BaseSpringWebTest {

    UserAuthenticatingController controller;

    /**
     * @throws Exception
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        controller = ( UserAuthenticatingController ) getBean( "signupController" );

    }

    /**
     * Tests the SignupController
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testSignup() throws Exception {

        SimpleSmtpServer server = SimpleSmtpServer.start( MAIL_PORT );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = newPost( "/signup.html" );
        request.addParameter( "userName", RandomStringUtils.randomAlphabetic( 6 ) );
        request.addParameter( "newPassword", "testpassword" );
        request.addParameter( "confirmNewPassword", "testpassword" );
        request.addParameter( "name", "My" );
        request.addParameter( "lastName", "Nameis" );
        request.addParameter( "email", RandomStringUtils.randomAlphabetic( 6 ) + "@gemma.org" );
        request.addParameter( "passwordHint", "guess" );

        ModelAndView mv = controller.handleRequest( request, response );
        Errors errors = ( Errors ) mv.getModel().get( BindingResult.MODEL_KEY_PREFIX + "user" );
        assertTrue( "Errors returned in model: " + errors, errors == null );

        server.stop();
        assertEquals( "Wrong number of emails recieved", 1, server.getReceivedEmailSize() );

        // verify that success messages are in the request
        assertNotNull( request.getSession().getAttribute( "messages" ) );

        assertEquals( "redirect:mainMenu.html", mv.getViewName() );
    }

    public void testDisplayForm() throws Exception {
        MockHttpServletRequest request = newGet( "/signup.html" );
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mv = controller.handleRequest( request, response );
        assertEquals( "returned incorrect view name", "signup", mv.getViewName() );
    }
}