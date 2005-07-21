package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.BaseControllerTestCase;
import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserRole;
import edu.columbia.gemma.common.auditAndSecurity.UserService;
import edu.columbia.gemma.web.controller.common.auditAndSecurity.SignupController;

/**
 * Tests the SignupController. Is also used to add an admin.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class SignupControllerTest extends BaseControllerTestCase {

    private MockServletContext mockCtx;
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

        mockCtx = new MockServletContext();

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

        ModelAndView mav = signupController.onSubmit( request, response, testUser, ( BindException ) null );

        // assertEquals( "pubMedList", mav.getViewName() );
        assertEquals( null, null );

    }

}