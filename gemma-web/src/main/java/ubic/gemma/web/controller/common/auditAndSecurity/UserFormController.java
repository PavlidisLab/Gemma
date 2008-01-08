package ubic.gemma.web.controller.common.auditAndSecurity;

import java.util.Collection;
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
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.Constants;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserExistsException;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.util.BeanPropertyCompleter;
import ubic.gemma.util.UserConstants;
import ubic.gemma.web.propertyeditor.UserRolesPropertyEditor;

/**
 * For editing users, or showing lists of users (for admins). Regular users clicking on "edit profile" get to here as do
 * admins clicking on 'edit user' or 'add' in the table view. Upon success, regular user s go to the main menu, admins
 * working from the list view go back to the list.
 * <p>
 * Heavily modified from mraible's Appfuse.
 * 
 * @author pavlidis
 * @author keshav
 * @author mraible
 * @version $Id$
 * @spring.bean id="userFormController"
 * @spring.property name="commandName" value="user"
 * @spring.property name="commandClass" value="ubic.gemma.web.controller.common.auditAndSecurity.UserUpdateCommand"
 * @spring.property name="validator" ref="userValidator"
 * @spring.property name="formView" value="userProfile"
 * @spring.property name="successView" value="redirect:users.html"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="mailEngine" ref="mailEngine"
 * @spring.property name="mailMessage" ref="mailMessage"
 * @spring.property name="templateName" value="accountCreated.vm"
 */
public class UserFormController extends UserAuthenticatingController {

    /**
     * 
     *
     */
    public UserFormController() {
        super();
    }

    @Override
    public String getCancelViewName( HttpServletRequest request ) {
        if ( StringUtils.equals( request.getParameter( "from" ), "list" ) ) {
            log.info( "Admin cancelled" );
            return getSuccessView();
        }

        log.info( "User cancelled" );
        return "mainMenu";

    }

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        if ( log.isDebugEnabled() ) {
            log.debug( "entering 'onSubmit' method..." );
        }

        UserUpdateCommand user = ( UserUpdateCommand ) command;
        Locale locale = request.getLocale();

        if ( request.getParameter( "delete" ) != null ) {
            this.getUserService().delete( user.getUserName() );
            saveMessage( request, "user.deleted", user.getName(), "User deleted" );
            log.debug( "Deleted " + user );
            return new ModelAndView( getSuccessView() );
        }

        String unencryptedPassword = null;
        boolean passwordChange = user.getNewPassword() != null;
        if ( passwordChange ) { // also the case if this is a new user.
            assert user.getNewPassword().equals( user.getConfirmNewPassword() ); // should be done by validator
            log.debug( "New password, encrypting" );
            unencryptedPassword = user.getNewPassword();
            String encryptedPassword = super.encryptPassword( user.getNewPassword(), request );
            user.setPassword( encryptedPassword );
        }

        updateRoles( request, user );

        boolean userIsNew = false;
        if ( user.getId() == null ) {
            userIsNew = true;
            try {
                log.debug( "Creating new " + user );
                user = new UserUpdateCommand( this.getUserService().create( user.asUser() ) );
            } catch ( UserExistsException e ) {
                log.warn( e.getMessage() );

                errors.rejectValue( "userName", "errors.existing.user", new Object[] { user.getUserName(),
                        user.getEmail() }, "duplicate user" );

                return showForm( request, response, errors );
            }
        } else {
            log.debug( "Updating " + user );

            // We have to get the original version from the database, and update the fields. Otherwise hibernate isn't
            // happy. Roles were updated above so should be a persistent collection. Other collections haven't been
            // touched
            User u = this.getUserService().load( user.getId() );
            BeanPropertyCompleter.complete( u, user.asUser(), true );
            this.getUserService().update( u );
        }

        if ( !StringUtils.equals( request.getParameter( "from" ), "list" ) ) {
            // user is editing their profile
            HttpSession session = request.getSession();
            session.setAttribute( Constants.USER_KEY, user );
            // FIXME not using saveMessage with the user.update key because locale cannot be found. See bug 805.
            // saveMessage( request, "user.updated", user.getUserName(), "User saved" );
            saveMessage( request, "User saved" );

            /* note difference here from signup: we don't log the user in -- unless the user changed their own password */
            if ( passwordChange && request.getRemoteUser().equals( user.getUserName() ) ) {
                log.debug( "Re-authenticating user " + user + " and updating tokens because password changed" );
                signInUser( request, user.asUser(), unencryptedPassword );
            }

            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ) ); // breaks on 8443?
        } else if ( userIsNew ) {
            assert StringUtils.equals( request.getParameter( "from" ), "list" );

            // administrator is adding a new user manually, so we don't log them in. We return to the list
            super.sendConfirmationEmail( request, user.asUser(), locale );
            saveMessage( request, "user.added", user.getUserName(), "User added successfully" );
            return new ModelAndView( getSuccessView() );

        } else {
            assert StringUtils.equals( request.getParameter( "from" ), "list" );
            // existing, came from list view. Maybe we should send an email if their password changed (or in any case)
            saveMessage( request, "user.updated.byAdmin", user.getUserName(), "User updated successfully" );
            return new ModelAndView( getSuccessView() );
        }

    }

    /**
     * @param request
     */
    private void checkForCookieLogin( HttpServletRequest request ) {
        // if user logged in with remember me, display a warning that they can't change passwords
        log.debug( "checking for remember me login..." );

        AuthenticationTrustResolver resolver = new AuthenticationTrustResolverImpl();
        SecurityContext ctx = SecurityContextHolder.getContext();

        if ( ctx.getAuthentication() != null ) {
            Authentication auth = ctx.getAuthentication();

            if ( resolver.isRememberMe( auth ) ) {
                request.getSession().setAttribute( "cookieLogin", "true" );

                // add warning message
                saveMessage( request, "Remember me is active, so password cannot be changed" );
                // saveMessage( request, "userProfile.cookieLogin", "Remember me is active, so password cannot be
                // changed" );
            }
        }
    }

    /**
     * Update the user's roles, if requested.
     * 
     * @param request
     * @param user
     */
    private void updateRoles( HttpServletRequest request, UserUpdateCommand user ) {
        String[] userRoles = request.getParameterValues( "roles" );

        if ( userRoles != null ) {
            // for some reason, Spring seems to hang on to the roles in
            // the User object, even though isSessionForm() == false
            user.getRoles().clear();
            for ( int i = 0; i < userRoles.length; i++ ) {
                String roleName = userRoles[i];
                log.debug( "User has role " + roleName );
                UserRole role = UserRole.Factory.newInstance( user.getUserName(), roleName,
                        "Added by userFormController" );

                // avoid adding the same role twice.
                if ( user.getRoles().size() > 0 ) {
                    for ( UserRole existingRole : user.getRoles() ) {
                        if ( !existingRole.getName().equals( roleName ) ) {
                            user.getRoles().add( role );
                        }
                    }
                } else {
                    user.getRoles().add( role );
                }

            }
        }

        assert user.getRoles().size() > 0 : "all users have to have some role!";
    }

    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        if ( !isFormSubmission( request ) ) {

            /* this is passed in if we are using editUser from form */
            String username = ServletRequestUtils.getStringParameter( request, "userName" );

            checkForCookieLogin( request );

            UserUpdateCommand user = null;

            if ( request.getRequestURI().indexOf( "editProfile" ) > -1 ) {
                // editProfile should not be called with user parameter passed in: user is editing
                // themselves.
                User u = this.getUserService().findByUserName( request.getRemoteUser() );
                if ( u == null ) {
                    throw new IllegalArgumentException( "User " + request.getRemoteUser() + " not found in the system" );
                }
                user = new UserUpdateCommand( u );
            } else if ( !StringUtils.isBlank( username ) ) {
                user = new UserUpdateCommand( this.getUserService().findByUserName( username ) );
            } else {
                user = new UserUpdateCommand();
                UserRole role = UserRole.Factory.newInstance();
                role.setName( UserConstants.USER_ROLE );
                role.setUserName( user.getUserName() );
                user.getRoles().add( role );
            }

            assert user.getRoles() != null && user.getRoles().size() > 0;
            return user;
        }
        return super.formBackingObject( request );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.BaseFormController#initBinder(javax.servlet.http.HttpServletRequest,
     *      org.springframework.web.bind.ServletRequestDataBinder)
     */
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        binder.registerCustomEditor( Collection.class, new UserRolesPropertyEditor() );
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
            /*
             * if URL is "editProfile" - make sure it's the current user reject if username passed in or "list"
             * parameter passed in someone that is trying this probably knows the AppFuse code but it's a legitimate
             * bug, so I'll fix it. ;-)
             */
            if ( ( request.getParameter( "userName" ) != null ) || ( request.getParameter( "from" ) != null ) ) {
                response.sendError( HttpServletResponse.SC_FORBIDDEN );
                log.warn( "User '" + request.getRemoteUser() + "' is trying to edit user '"
                        + request.getParameter( "userName" ) + "'" );

                return null;
            }
        }

        /*
         * prevent ordinary users from calling a GET on editUser.html unless a bind error exists.
         */
        if ( ( request.getRequestURI().indexOf( "editUser" ) > -1 )
                && ( !request.isUserInRole( UserConstants.ADMIN_ROLE ) && ( errors.getErrorCount() == 0 ) &&
                /*
                 * be nice to server-side validation for editProfile
                 */
                ( request.getRemoteUser() != null ) ) ) { // be nice to unit tests
            response.sendError( HttpServletResponse.SC_FORBIDDEN );

            return null;
        }

        return super.showForm( request, response, errors );

    }
}
