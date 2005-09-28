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
package edu.columbia.gemma;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import junit.framework.TestCase;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.context.SecurityContextHolder;
import net.sf.acegisecurity.context.SecurityContextImpl;
import net.sf.acegisecurity.providers.ProviderManager;
import net.sf.acegisecurity.providers.TestingAuthenticationProvider;
import net.sf.acegisecurity.providers.TestingAuthenticationToken;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.security.ui.ManualAuthenticationProcessing;
import edu.columbia.gemma.util.ConvertUtil;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * A service is a class that uses daos. To test the service without testing the daos (and using the database), we use
 * mock daos.
 * <p>
 * This code from AppFuse. Provides utilities to read in objects from resources (properties files).
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @author raible
 * @version $Id$
 */
public class BaseServiceTestCase extends TestCase {
    // ~ Static fields/initializers =============================================

    protected final Log log = LogFactory.getLog( getClass() );
    protected ResourceBundle rb;

    protected BeanFactory ctx = SpringContextUtil.getApplicationContext( true );

    /* authentication */
    ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctx
            .getBean( "manualAuthenticationProcessing" );

    // ~ Constructors ===========================================================

    public BaseServiceTestCase() {

        // see http://fishdujour.typepad.com/blog/2005/02/junit_testing_w.html
        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "test", "test", new GrantedAuthority[] {
                new GrantedAuthorityImpl( "user" ), new GrantedAuthorityImpl( "administrator" ) } );

        // Override the regular spring configuration
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        List<TestingAuthenticationProvider> list = new ArrayList<TestingAuthenticationProvider>();
        list.add( new TestingAuthenticationProvider() );
        providerManager.setProviders( list );

        // Create and store the Acegi SecureContext into the ContextHolder.
        SecurityContextImpl secureContext = new SecurityContextImpl();
        secureContext.setAuthentication( token );
        SecurityContextHolder.setContext( secureContext );

        // Since a ResourceBundle is not required for each class, just
        // do a simple check to see if one exists
        String className = this.getClass().getName();

        try {
            rb = ResourceBundle.getBundle( className );
        } catch ( MissingResourceException mre ) {
            // log.warn("No resource bundle found for: " + className);
        }
    }

    // ~ Methods ================================================================

    /**
     * Utility method to populate a javabean-style object with values from a Properties file
     * 
     * @param obj
     * @return
     * @throws Exception
     */
    protected Object populate( Object obj ) throws Exception {
        // loop through all the beans methods and set its properties from
        // its .properties file
        Map map = ConvertUtil.convertBundleToMap( rb );

        BeanUtils.copyProperties( obj, map );

        return obj;
    }
}