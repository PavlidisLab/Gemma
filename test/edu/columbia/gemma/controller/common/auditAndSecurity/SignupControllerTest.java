package edu.columbia.gemma.controller.common.auditAndSecurity;

import java.util.Date;
import java.util.HashSet;
import java.util.ResourceBundle;
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
 * 
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 */
public class SignupControllerTest extends BaseControllerTestCase{
    
    private MockServletContext mockCtx;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    SignupController signupController;
    
    User testUser;
    UserRole ur;
    UserService userService;

    public void setUp() throws Exception {
        
        mockCtx = new MockServletContext();
        
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        
        signupController = ( SignupController ) ctx.getBean( "signupController" );
        
        testUser = User.Factory.newInstance();
        ur = UserRole.Factory.newInstance();
        userService = ( UserService ) ctx.getBean( "userService" );
        
    }

    public void tearDown() {
        signupController = null;
    }

    public void testOnSubmit() throws Exception {

        request.setContextPath( "/Gemma" );
        request.setServletPath("/test.signup.html");
        request.setLocalName("en_US");
        
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
        testUser.setUserRoles( set );

        testUser.setPassword( "root" );
        testUser.setConfirmPassword( "root" );
        testUser.setPasswordHint( "test hint" );

        testUser.getUserRoles().add( ur );

        
        
        ModelAndView mav = signupController.onSubmit( request, response, testUser, (BindException) null);

        //assertEquals( "pubMedList", mav.getViewName() );
        assertEquals( null, null );

    }
    
    protected String[] getConfigLocations() {

        ResourceBundle db = ResourceBundle.getBundle( "testResources" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        // Make sure you have the /web on the junit classpath.
        String[] paths = { "applicationContext-dataSource.xml", "applicationContext-" + daoType + ".xml",
                servletContext + "-servlet.xml" };

        return paths;
    }

}