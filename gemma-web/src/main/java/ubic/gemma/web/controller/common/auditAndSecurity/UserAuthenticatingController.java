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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.dao.DaoAuthenticationProvider;
import org.acegisecurity.providers.encoding.ShaPasswordEncoder;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.RequestUtil;
import ubic.gemma.web.controller.BaseFormController;

/**
 * Basic support for controllers that need to encrypt passwords and sign users in.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class UserAuthenticatingController extends BaseFormController {

    /**
     * 
     *
     */
    public UserAuthenticatingController() {
        super();
    }

    /**
     * @param user
     * @param request
     */
    protected void encryptPassword( UserUpdateCommand user, HttpServletRequest request ) {
        ProviderManager authenticationManager = getProviderManager( request );

        encryptPassword( user, authenticationManager );

    }

    /**
     * @param password
     * @param request
     * @return
     */
    protected String encryptPassword( String password, HttpServletRequest request ) {
        ProviderManager authenticationManager = getProviderManager( request );

        return encryptPassword( authenticationManager, password );

    }

    /**
     * @param user
     * @param authenticationManager
     */
    protected void encryptPassword( UserUpdateCommand user, ProviderManager authenticationManager ) {

        if ( authenticationManager == null ) {
            // when testing.
            return;
        }

        String unencryptedPassword;
        if ( user.getNewPassword() != null ) {
            unencryptedPassword = user.getNewPassword();
        } else {
            unencryptedPassword = user.getPassword();
        }
        assert StringUtils.isNotBlank( unencryptedPassword );
        String encryptedPassword = null;
        encryptedPassword = encryptPassword( authenticationManager, unencryptedPassword );
        assert encryptedPassword != null;
        user.setPassword( encryptedPassword );
    }

    /**
     * @param authenticationManager
     * @param unencryptedPassword
     * @param encryptedPassword
     * @return
     */
    protected String encryptPassword( ProviderManager authenticationManager, String unencryptedPassword ) {
        if ( authenticationManager == null ) {
            // this can happeng during testing. In which case we have to encrypt it by hand.
            log.warn( "Test environment: using SHA encoder" );
            ShaPasswordEncoder encoder = new ShaPasswordEncoder();
            return encoder.encodePassword( unencryptedPassword, ConfigUtils.getProperty( "gemma.salt" ) );
        }
        for ( Object provider : authenticationManager.getProviders() ) {
            if ( ( AuthenticationProvider ) provider instanceof DaoAuthenticationProvider ) {
                DaoAuthenticationProvider daoAuthenticationProvider = ( DaoAuthenticationProvider ) provider;
                return daoAuthenticationProvider.getPasswordEncoder().encodePassword( unencryptedPassword,
                        ConfigUtils.getProperty( "gemma.salt" ) );
            }
        }
        return null;
    }

    /**
     * @param request
     * @return
     */
    private ProviderManager getProviderManager( HttpServletRequest request ) {
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext( request.getSession()
                .getServletContext() );
        if ( ctx == null ) {
            // happens during testing.
            return null;
        }
        ProviderManager authenticationManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        return authenticationManager;
    }

    /**
     * @param request
     * @param user
     * @param unencryptedPassword
     */
    protected void signInUser( HttpServletRequest request, User user, String unencryptedPassword ) {
        assert user.getUserName() != null;
        Authentication auth = new UsernamePasswordAuthenticationToken( user.getUserName(), unencryptedPassword );

        ProviderManager providerManager = getProviderManager( request );

        if ( providerManager == null ) {
            return; // must be testing.
        }
        Authentication authentication = providerManager.doAuthentication( auth );
        assert authentication.isAuthenticated() : "New user " + user.getUserName()
                + " wasn't authenticated with password.";
        SecurityContextHolder.getContext().setAuthentication( authentication );
    }

    /**
     * @param request
     * @param user
     * @param locale
     * @param unencryptedPassword 
     */
    protected void sendConfirmationEmail( HttpServletRequest request, User user, Locale locale, String unencryptedPassword ) {
        // Send user an e-mail
        if ( log.isDebugEnabled() ) {
            log.debug( "Sending user '" + user.getUserName() + "' an account information e-mail" );
        }

        // Send an account information e-mail
        mailMessage.setSubject( getText( "signup.email.subject", locale ) );
        try {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put( "userName", user.getUserName() );
            model.put( "userPassword", unencryptedPassword );
            model.put( "message", getText( "signup.email.message", locale ) );
            model.put( "applicationURL", RequestUtil.getAppURL( request ) );
            sendEmail( user, this.templateName, model );
            this.saveMessage( request, "email.sent", user.getEmail(), "Confirmation email was sent to "
                    + user.getEmail() );
        } catch ( Exception e ) {
            log.error( "Couldn't send email to " + user, e );
        }
    }

}
