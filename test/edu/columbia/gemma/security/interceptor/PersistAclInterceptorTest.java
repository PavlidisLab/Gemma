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
package edu.columbia.gemma.security.interceptor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.context.SecurityContextHolder;
import net.sf.acegisecurity.context.SecurityContextImpl;
import net.sf.acegisecurity.providers.ProviderManager;
import net.sf.acegisecurity.providers.TestingAuthenticationProvider;
import net.sf.acegisecurity.providers.TestingAuthenticationToken;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PersistAclInterceptorTest extends TestCase {
    private static Log log = LogFactory.getLog( PersistAclInterceptorTest.class.getName() );
    ArrayDesign ad = null;
    BeanFactory ctx = null;

    protected void setUp() throws Exception {
        super.setUp();
        ctx = SpringContextUtil.getApplicationContext( true );

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

        ad = ArrayDesign.Factory.newInstance();
        ad.setName( ( new Date() ).toString() );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Calling the method saveArrayDesign, which should have the PersistAclInterceptor.invoke called on it after the
     * actual method invocation.
     * 
     * FIXME how do we know if it worked?
     * 
     * @throws Exception
     */
    public void testSaveArrayDesign() throws Exception {
        log.info( "Testing saveArrayDesign(ArrayDesign ad)" );
        ArrayDesignService ads = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );
        ads.saveArrayDesign( ad );
    }

    /**
     * Tests an invalid method
     * 
     * @throws Exception
     */
    // public void testInvalidMethodToIntercept() throws Exception {
    // log.info( "Testing an invalid method to intercept" );
    //
    // ( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) ).getAllArrayDesigns();
    // }
}
