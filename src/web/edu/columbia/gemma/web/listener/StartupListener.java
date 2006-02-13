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
package edu.columbia.gemma.web.listener;
 
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.encoding.Md5PasswordEncoder;
import org.acegisecurity.providers.rememberme.RememberMeAuthenticationProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.WebApplicationContextUtils;

import edu.columbia.gemma.common.auditAndSecurity.UserRole;
import edu.columbia.gemma.common.auditAndSecurity.UserRoleDao;
import edu.columbia.gemma.util.LabelValue;
import edu.columbia.gemma.web.Constants;

/**
 * StartupListener class used to initialize and database settings and populate any application-wide drop-downs.
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @web.listener
 */
public class StartupListener extends ContextLoaderListener implements ServletContextListener {
    private static final Log log = LogFactory.getLog( StartupListener.class );

    @SuppressWarnings("unchecked")
    @Override
    public void contextInitialized( ServletContextEvent event ) {
        if ( log.isDebugEnabled() ) {
            log.debug( "initializing context..." );
        }

        // call Spring's context ContextLoaderListener to initialize
        // all the context files specified in web.xml
        super.contextInitialized( event );

        ServletContext context = event.getServletContext();

        // Orion starts Servlets before Listeners, so check if the config
        // object already exists
        Map<String, Object> config = ( Map<String, Object> ) context.getAttribute( Constants.CONFIG );

        if ( config == null ) {
            config = new HashMap<String, Object>();
        }

        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( context );

        boolean encryptPassword = false;
        try {
            ProviderManager provider = ( ProviderManager ) ctx.getBean( "authenticationManager" );
            for ( Iterator it = provider.getProviders().iterator(); it.hasNext(); ) {
                AuthenticationProvider p = ( AuthenticationProvider ) it.next();
                if ( p instanceof RememberMeAuthenticationProvider ) {
                    config.put( "rememberMeEnabled", Boolean.TRUE );
                }
            }

            if ( ctx.containsBean( "passwordEncoder" ) ) {
                encryptPassword = true;
                config.put( Constants.ENCRYPT_PASSWORD, Boolean.TRUE );
                String algorithm = "SHA";
                if ( ctx.getBean( "passwordEncoder" ) instanceof Md5PasswordEncoder ) {
                    algorithm = "MD5";
                }
                config.put( Constants.ENC_ALGORITHM, algorithm );
            }
        } catch ( NoSuchBeanDefinitionException n ) {
            // ignore, should only happen when testing
        }

        context.setAttribute( Constants.CONFIG, config );

        // output the retrieved values for the Init and Context Parameters
        if ( log.isDebugEnabled() ) {
            log.debug( "Remember Me Enabled? " + config.get( "rememberMeEnabled" ) );
            log.debug( "Encrypt Passwords? " + encryptPassword );
            if ( encryptPassword ) {
                log.debug( "Encryption Algorithm: " + config.get( Constants.ENC_ALGORITHM ) );
            }
            log.debug( "Populating drop-downs..." );
        }

        setupContext( context );
    }

    @SuppressWarnings("unchecked")
    public static void setupContext( ServletContext context ) {
        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext( context );

        // mimic the functionality of the LookupManager in Appfuse.
        UserRoleDao mgr = ( UserRoleDao ) ctx.getBean( "userRoleDao" );
        Set<LabelValue> roleList = new HashSet<LabelValue>();

        // get list of possible roles
        Collection<UserRole> roles = mgr.loadAll();
        for ( UserRole role : roles ) {
            roleList.add( new LabelValue( role.getName(), role.getName() ) );
        }

        context.setAttribute( Constants.AVAILABLE_ROLES, roleList );

        if ( log.isDebugEnabled() ) {
            log.debug( "Drop-down initialization complete [OK]" );
        }
    }
}
