package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserService;
import edu.columbia.gemma.web.Constants;

/**
 * Simple class to retrieve a list of users from the database. From appfuse.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="userController" name="/users.html"
 * @spring.property name="userService" ref="userService"
 */
public class UserController implements Controller {
    private transient final Log log = LogFactory.getLog( UserController.class );
    private UserService userService = null;

    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'handleRequest' method..." );
        }
        
        return new ModelAndView( "userList", Constants.USER_LIST, userService.getUsers( User.Factory.newInstance() ) );
    }

    public void setUserService( UserService userService ) {
        this.userService = userService;
    }
}
