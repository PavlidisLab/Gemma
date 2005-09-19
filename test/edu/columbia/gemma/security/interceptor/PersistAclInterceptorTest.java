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

import java.util.Date;

import junit.framework.TestCase;

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
     * @throws Exception
     */
    public void testSaveArrayDesign() throws Exception {
        log.info( "Testing saveArrayDesign(ArrayDesign ad)" );

        ( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) ).saveArrayDesign( ad );
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
