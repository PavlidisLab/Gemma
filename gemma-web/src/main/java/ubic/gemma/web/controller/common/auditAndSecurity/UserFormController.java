package ubic.gemma.web.controller.common.auditAndSecurity;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationTrustResolver;
import org.acegisecurity.AuthenticationTrustResolverImpl;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.Constants;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserExistsException;
import ubic.gemma.model.common.auditAndSecurity.UserImpl;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.model.common.auditAndSecurity.UserRoleService;

/**
 * For editing users.
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="userFormController" name="/editProfile.html /editUser.html"
 * @spring.property name="commandName" value="user"
 * @spring.property name="commandClass" value="ubic.gemma.model.common.auditAndSecurity.UserImpl"
 * @spring.property name="validator" ref="userValidator"
 * @spring.property name="formView" value="userProfile"
 * @spring.property name="successView" value="redirect:users.html"
 * @spring.property name="userRoleService" ref="userRoleService"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="message" ref="mailMessage"
 * @spring.property name="templateName" value="accountCreated.vm"
 */
public class UserFormController extends UserAuthenticatingController {

    private UserRoleService userRoleService;

   

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'onSubmit' method..." );
        }

        User user = ( User ) command;
        Locale locale = request.getLocale();

        if ( request.getParameter( "delete" ) != null ) {
            this.getUserService().removeUser( user.getUserName() );
            saveMessage( request, getText( "user.deleted", user.getFullName(), locale ) );
            return new ModelAndView( getSuccessView() );
        }

        String unencryptedPassword = user.getPassword();
        super.encryptPassword( user, request );

        updateRoles( request, user );

        try {
            this.getUserService().saveUser( user );
        } catch ( UserExistsException e ) {
            log.warn( e.getMessage() );

            errors.rejectValue( "username", "errors.existing.user",
                    new Object[] { user.getUserName(), user.getEmail() }, "duplicate user" );

            // redisplay the unencrypted passwords
            user.setPassword( unencryptedPassword );
            user.setConfirmPassword( unencryptedPassword );

            return showForm( request, response, errors );
        }

        if ( !StringUtils.equals( request.getParameter( "from" ), "list" ) ) {
            HttpSession session = request.getSession();
            session.setAttribute( Constants.USER_KEY, user );

            saveMessage( request, getText( "user.saved", user.getFullName(), locale ) );

            // return to main Menu
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }
        if ( StringUtils.isBlank( request.getParameter( "version" ) ) ) {
            signInUser( request, user, unencryptedPassword );
            super.sendConfirmationEmail( request, user, locale );
            saveMessage( request, getText( "user.added", user.getFullName(), locale ) );
            return showNewForm( request, response );
        }
        saveMessage( request, getText( "user.updated.byAdmin", user.getFullName(), locale ) );

        return showForm( request, response, errors );

    }

    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        if ( request.getParameter( "cancel" ) != null ) {
            if ( !StringUtils.equals( request.getParameter( "from" ), "list" ) ) {
                saveMessage( request, getText( "errors.cancel",   request.getLocale() ) );
                return new ModelAndView( new RedirectView( "mainMenu.html" ) );
            }
            return new ModelAndView( getSuccessView() );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param roleManager The roleManager to set.
     */
    public void setUserRoleService( UserRoleService userRoleService ) {
        this.userRoleService = userRoleService;
    }

    /**
     * Update the user's roles, if requested.
     * 
     * @param request
     * @param user
     */
    private void updateRoles( HttpServletRequest request, User user ) {
        String[] userRoles = request.getParameterValues( "userRoles" );

        if ( userRoles != null ) {
            // for some reason, Spring seems to hang on to the roles in
            // the User object, even though isSessionForm() == false
            user.getRoles().clear();
            for ( int i = 0; i < userRoles.length; i++ ) {
                String roleName = userRoles[i];
                UserRole role = this.userRoleService.getRole( roleName );
                role.setUserName( user.getUserName() ); // FIXME = UserRoleService should set this.
                user.getRoles().add( role );
            }
        }
    }

    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        String username = request.getParameter( "userName" );

        // if user logged in with remember me, display a warning that they can't change passwords
        log.debug( "checking for remember me login..." );

        AuthenticationTrustResolver resolver = new AuthenticationTrustResolverImpl();
        SecurityContext ctx = SecurityContextHolder.getContext();

        if ( ctx.getAuthentication() != null ) {
            Authentication auth = ctx.getAuthentication();

            if ( resolver.isRememberMe( auth ) ) {
                request.getSession().setAttribute( "cookieLogin", "true" );

                // add warning message
                saveMessage( request, getText( "userProfile.cookieLogin", request.getLocale() ) );
            }
        }

        // We use UserImpl so we expose the roleList() method.
        UserImpl user = null;

        if ( request.getRequestURI().indexOf( "editProfile" ) > -1 ) {
            user = ( UserImpl ) this.getUserService().getUser( request.getRemoteUser() );
        } else if ( !StringUtils.isBlank( username ) && !"".equals( request.getParameter( "version" ) ) ) {
            user = ( UserImpl ) this.getUserService().getUser( username );
        }

        if ( user == null ) {
            log.info( "User " + username + " does not exist, creating anew" );
            // Creating new user.
            user = ( UserImpl ) User.Factory.newInstance();
            UserRole role = UserRole.Factory.newInstance();
            role.setName( Constants.USER_ROLE );
            role.setUserName( user.getUserName() ); // FIXME = UserRoleService should set this.
            user.getRoles().add( role );
        }

        assert user != null;
        user.setConfirmPassword( user.getPassword() );
        return user;

    }

    @Override
    @SuppressWarnings("unused")
    protected void onBind( HttpServletRequest request, Object command ) throws Exception {
        // if the user is being deleted, turn off validation
        if ( request.getParameter( "delete" ) != null ) {
            super.setValidateOnBinding( false );
        } else {
            super.setValidateOnBinding( true );
        }
    }

    @Override
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
