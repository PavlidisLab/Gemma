/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.context.SecurityContextImpl;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.TestingAuthenticationProvider;
import org.acegisecurity.providers.TestingAuthenticationToken;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpringContextUtil {
    private static Log log = LogFactory.getLog( SpringContextUtil.class.getName() );
    private static BeanFactory ctx = null;

    /**
     * @param testing If true, it will get a test configured-BeanFactory
     * @return BeanFactory
     */
    public static BeanFactory getApplicationContext( boolean testing ) {
        if ( ctx == null ) {
            String[] paths = getConfigLocations( testing );
            ctx = new ClassPathXmlApplicationContext( paths );
            if ( ctx != null )
                log.info( "Got context" );
            else
                log.error( "Failed to load context" );
        }
        return ctx;
    }

    /**
     * For use in tests only.
     * <p>
     * see http://fishdujour.typepad.com/blog/2005/02/junit_testing_w.html
     */
    public static void grantAuthorityForTests() {

        assert ctx != null;

        // 
        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "pavlab", "pavlab", new GrantedAuthority[] {
                new GrantedAuthorityImpl( "user" ), new GrantedAuthorityImpl( "admin" ) } );

        // Override the regular spring configuration
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        List<TestingAuthenticationProvider> list = new ArrayList<TestingAuthenticationProvider>();
        list.add( new TestingAuthenticationProvider() );
        providerManager.setProviders( list );

        // Create and store the Acegi SecureContext into the ContextHolder.
        SecurityContextImpl secureContext = new SecurityContextImpl();
        secureContext.setAuthentication( token );
        SecurityContextHolder.setContext( secureContext );
    }

    /**
     * Xml
     * 
     * @return XmlWebApplicationContext
     */
    public static BeanFactory getXmlWebApplicationContext(boolean testing) {
        if ( ctx == null ) {
            String[] paths = getConfigLocations(testing);
            ctx = new XmlWebApplicationContext();
            ( ( XmlWebApplicationContext ) ctx ).setConfigLocations( paths );
            ( ( XmlWebApplicationContext ) ctx ).setServletContext( new MockServletContext( "" ) );
            ( ( XmlWebApplicationContext ) ctx ).refresh();
            if ( ctx != null )
                log.info( "Got context" );
            else
                log.error( "Failed to load context" );
        }
        return ctx;
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @return
     */
    public static String[] getConfigLocations() {
        return getConfigLocations( false );
    }

    /**
     * Find the configuration file locations. The files must be in your class path for this to work.
     * 
     * @param testing - if true, it will use the test configuration.
     * @return
     */
    public static String[] getConfigLocations( boolean testing ) {
        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        // TODO: these files need to be found automatically, not hard-coded.

        if ( testing ) {
            log.info( "************** Using test configuration ***************" );
            return new String[] { "localTestDataSource.xml", "applicationContext-" + daoType + ".xml",
                    "applicationContext-security.xml", servletContext + "-servlet.xml",
                    "applicationContext-validation.xml" };
        }
        return new String[] { "applicationContext-localDataSource.xml", "applicationContext-" + daoType + ".xml",
                "applicationContext-security.xml", servletContext + "-servlet.xml", "applicationContext-validation.xml" };

    }

}
