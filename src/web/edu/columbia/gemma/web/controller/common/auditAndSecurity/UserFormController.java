package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.common.auditAndSecurity.UserRole;
import edu.columbia.gemma.common.auditAndSecurity.UserRoleService;
import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserExistsException;
import edu.columbia.gemma.util.RequestUtil;
import edu.columbia.gemma.util.StringUtil;
import edu.columbia.gemma.web.Constants;
import edu.columbia.gemma.web.controller.BaseFormController;

/**
 * Implementation of <strong>SimpleFormController</strong> that interacts with the {@link UserManager} to
 * retrieve/persist values to the database.
 * <p>
 * Based on Appfuse code.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="userFormController" name="/editProfile.html /editUser.html"
 * @spring.property name="commandName" value="user"
 * @spring.property name="commandClass" value="edu.columbia.gemma.common.auditAndSecurity.User"
 * @spring.property name="validator" ref="userValidator"
 * @spring.property name="formView" value="userProfile"
 * @spring.property name="successView" value="redirect:users.html"
 * @spring.property name="userRoleService" ref="userRoleService"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="message" ref="mailMessage"
 * @spring.property name="templateName" value="accountCreated.vm"
 */
public class UserFormController extends BaseFormController {
    private UserRoleService userRoleService;

    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'onSubmit' method..." );
        }

        User user = ( User ) command;
        Locale locale = request.getLocale();
   
        if ( request.getParameter( "delete" ) != null ) {
            userService.removeUser( user.getUserName() );
            saveMessage( request, getText( "user.deleted", user.getFullName(), locale ) );

            return new ModelAndView( getSuccessView() );
        }
        if ( "true".equals( request.getParameter( "encryptPass" ) ) ) {
            String algorithm = ( String ) getConfiguration().get( Constants.ENC_ALGORITHM );

            if ( algorithm == null ) { // should only happen for test case

                if ( log.isDebugEnabled() ) {
                    log.debug( "assuming testcase, setting algorithm to 'SHA'" );
                }

                algorithm = "SHA";
            }

            user.setPassword( StringUtil.encodePassword( user.getPassword(), algorithm ) );
            user.setConfirmPassword( StringUtil.encodePassword( user.getConfirmPassword(), algorithm ) ); // this was not included in appfuse -- probably not persisted.
        }

        String[] userRoles = request.getParameterValues( "userRoles" );

        if ( userRoles != null ) {
            // for some reason, Spring seems to hang on to the roles in
            // the User object, even though isSessionForm() == false
            user.getRoles().clear();
            for ( int i = 0; i < userRoles.length; i++ ) {
                String roleName = userRoles[i];
                userService.addRole( user, userRoleService.getRole( roleName ) );
            }
        }

        try {
            userService.saveUser( user );
        } catch ( UserExistsException e ) {
            log.warn( e.getMessage() );

            errors.rejectValue( "username", "errors.existing.user",
                    new Object[] { user.getUserName(), user.getEmail() }, "duplicate user" );

            // redisplay the unencrypted passwords
            user.setPassword( user.getConfirmPassword() );

            return showForm( request, response, errors );
        }

        if ( !StringUtils.equals( request.getParameter( "from" ), "list" ) ) {
            HttpSession session = request.getSession();
            session.setAttribute( Constants.USER_KEY, user );

            // update the user's remember me cookie if they didn't login
            // with a cookie
            if ( ( RequestUtil.getCookie( request, Constants.LOGIN_COOKIE ) != null )
                    && ( session.getAttribute( "cookieLogin" ) == null ) ) {
                // delete all user cookies and add a new one
                userService.removeLoginCookies( user.getUserName() );

                String autoLogin = userService.createLoginCookie( user.getUserName() );
                RequestUtil.setCookie( response, Constants.LOGIN_COOKIE, autoLogin, request.getContextPath() );
            }

            saveMessage( request, getText( "user.saved", user.getFullName(), locale ) );

            // return to main Menu
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        } else {
            if ( StringUtils.isBlank( request.getParameter( "version" ) ) ) {
                saveMessage( request, getText( "user.added", user.getFullName(), locale ) );

                // Send an account information e-mail
                message.setSubject( getText( "signup.email.subject", locale ) );
                sendUserMessage( user, getText( "newuser.email.message", user.getFullName(), locale ), RequestUtil
                        .getAppURL( request ) );

                return showNewForm( request, response );
            } else {
                saveMessage( request, getText( "user.updated.byAdmin", user.getFullName(), locale ) );
            }
        }

        return showForm( request, response, errors );
    }

    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            if ( !StringUtils.equals( request.getParameter( "from" ), "list" ) ) {
                return new ModelAndView( new RedirectView( "mainMenu.html" ) );
            } else {
                return new ModelAndView( getSuccessView() );
            }
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param roleManager The roleManager to set.
     */
    public void setUserRoleService( UserRoleService userRoleService ) {
        this.userRoleService = userRoleService;
    }

    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        String username = request.getParameter( "username" );

        if ( request.getSession().getAttribute( "cookieLogin" ) != null ) {
            saveMessage( request, getText( "userProfile.cookieLogin", request.getLocale() ) );
        }

        User user = null;

        if ( request.getRequestURI().indexOf( "editProfile" ) > -1 ) {
            user = userService.getUser( getUser( request ).getUserName() );
        } else if ( !StringUtils.isBlank( username ) /* && !"".equals( request.getParameter( "version" ) ) */) { // we
            // don't
            // have
            // 'version'.
            user = userService.getUser( username );
        } else {
            user = User.Factory.newInstance();
            UserRole newRole = UserRole.Factory.newInstance();
            // user.setUserName( username );
            newRole.setName( Constants.USER_ROLE );
            // newRole.setUserName( username );
            userService.addRole( user, newRole );
        }

        user.setConfirmPassword( user.getPassword() );

        return user;
    }

    protected void onBind( HttpServletRequest request, Object command ) throws Exception {
        // if the user is being deleted, turn off validation
        if ( request.getParameter( "delete" ) != null ) {
            super.setValidateOnBinding( false );
        } else {
            super.setValidateOnBinding( true );
        }
    }

    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getRequestURI().indexOf( "editProfile" ) > -1 ) {
            // if URL is "editProfile" - make sure it's the current user
            // reject if username passed in or "list" parameter passed in
            // someone that is trying this probably knows the AppFuse code
            // but it's a legitimate bug, so I'll fix it. ;-)
            if ( ( request.getParameter( "username" ) != null ) || ( request.getParameter( "from" ) != null ) ) {
                response.sendError( HttpServletResponse.SC_FORBIDDEN );
                log.warn( "User '" + request.getRemoteUser() + "' is trying to edit user '"
                        + request.getParameter( "username" ) + "'" );

                return null;
            }
        }

        // prevent ordinary users from calling a GET on editUser.html
        // unless a bind error exists.
        if ( ( request.getRequestURI().indexOf( "editUser" ) > -1 )
                && ( !request.isUserInRole( Constants.ADMIN_ROLE ) && ( errors.getErrorCount() == 0 ) && // be nice
                // to
                // server-side
                // validation
                // for
                // editProfile
                ( request.getRemoteUser() != null ) ) ) { // be nice to unit tests
            response.sendError( HttpServletResponse.SC_FORBIDDEN );

            return null;
        }

        return super.showForm( request, response, errors );
    }
}
