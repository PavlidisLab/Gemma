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
package edu.columbia.gemma.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrailService;
import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.CompositeSequenceService;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.security.ui.ManualAuthenticationProcessing;

/**
 * Use this to test the acegi functionality.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class SecurityIntegrationTest extends BaseServiceTestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test removing an arrayDesign without having the correct authorization privileges. You should get an
     * unsuccessfulAuthentication. Does not use mock objects because I need the data from the database. This test does
     * not use SpringContextUtil because I wanted to test the acegi implementation and SpringContextUtil grants
     * authorization for all users.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testRemoveArrayDesignWithoutAuthorizationWithoutMock() throws Exception {

        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        String[] paths = { "localTestDataSource.xml", "applicationContext-" + daoType + ".xml",
                "applicationContext-security.xml", servletContext + "-servlet.xml", "applicationContext-validation.xml" };

        BeanFactory cpCtx = new ClassPathXmlApplicationContext( paths );

        /* Manual Authentication */
        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) cpCtx
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( "keshav", "pavlab" );

        ArrayDesignService ads = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "deleteme" );
        ad = ( ArrayDesign ) this.getPersisterHelper().persist( ad );

        ads.remove( ad );
    }

    /**
     * Test removing an arrayDesign with correct authorization. The security interceptor should be called on this
     * method, as should the AddOrRemoveFromACLInterceptor. Does not use mock objects because I need to remove an
     * element from a live database. This test does not use SpringContextUtil because I wanted to test the acegi
     * implementation and SpringContextUtil grants authorization for all users.
     * 
     * @throws Exception
     */
    public void testRemoveArrayDesignWithoutMock() throws Exception {
        /*
         * Use this or your ManualAuthenticationProcessing solution. The authentication from a non-http client here
         * comes from Fish du jour - See SpringContextUtil BeanFactory ctx = SpringContextUtil.getApplicationContext(
         * false );
         */
        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        String[] paths = { "localTestDataSource.xml", "applicationContext-" + daoType + ".xml",
                "applicationContext-security.xml", servletContext + "-servlet.xml", "applicationContext-validation.xml" };

        BeanFactory cpCtx = new ClassPathXmlApplicationContext( paths );

        /* Manual Authentication */
        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) cpCtx
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( "keshav", "pavlab" );

        /* get bean and invoke methods. */
        ArrayDesignService ads = ( ArrayDesignService ) cpCtx.getBean( "arrayDesignService" );

        ArrayDesign ad = ads.findArrayDesignByName( "AD Foo" );
        if ( ad == null )
            log.info( "ArrayDesign does not exist" );

        else {
            ads.remove( ad );
        }
    }

    /**
     * Save an array design This test does not use SpringContextUtil because I wanted to test the acegi implementation
     * and SpringContextUtil grants authorization for all users.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testSaveArrayDesignWithoutMock() throws Exception {

        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        String[] paths = { "localTestDataSource.xml", "applicationContext-" + daoType + ".xml",
                "applicationContext-security.xml", servletContext + "-servlet.xml", "applicationContext-validation.xml" };

        BeanFactory cpCtx = new ClassPathXmlApplicationContext( paths );

        /* Manual Authentication */
        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) cpCtx
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( "keshav", "pavlab" );

        ArrayDesignService ads = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );
        AuditTrailService ats = ( AuditTrailService ) cpCtx.getBean( "auditTrailService" );
        ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( "AD Foo" );
        arrayDesign.setDescription( "a test ArrayDesign" );

        AuditTrail at = AuditTrail.Factory.newInstance();
        at = ats.create( at );
        arrayDesign.setAuditTrail( at );

        Contact c = Contact.Factory.newInstance();
        c.setName( "\' Design Provider Name\'" );
        at = AuditTrail.Factory.newInstance();
        at = ats.create( at );
        c.setAuditTrail( at );

        arrayDesign.setDesignProvider( c );

        CompositeSequence cs1 = CompositeSequence.Factory.newInstance();
        cs1.setName( "DE Bar1" );

        CompositeSequence cs2 = CompositeSequence.Factory.newInstance();
        cs2.setName( "DE Bar2" );

        Collection<DesignElement> col = new HashSet();
        col.add( cs1 );
        col.add( cs2 );

        /*
         * Note this sequence. Remember, inverse="true" if using this. If you do not make an explicit call to
         * cs1(2).setArrayDesign(arrayDesign), then inverse="false" must be set.
         */
        cs1.setArrayDesign( arrayDesign );
        cs2.setArrayDesign( arrayDesign );
        arrayDesign.setDesignElements( col );

        ads.findOrCreate( arrayDesign );

    }

    /**
     * Tests getting all design elements given authorization on an array design (ie. tests getting the 'owned objects'
     * given the authorization on the owner). This test was used to test the Acegi Security functionality. Mock objects
     * not used because I need the objects from the database. This test does not use SpringContextUtil because I wanted
     * to test the acegi implementation and SpringContextUtil grants authorization for all users.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testGetAllDesignElementsFromArrayDesignsWithoutMock() throws Exception {

        ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        String daoType = db.getString( "dao.type" );
        String servletContext = db.getString( "servlet.name.0" );
        String[] paths = { "localTestDataSource.xml", "applicationContext-" + daoType + ".xml",
                "applicationContext-security.xml", servletContext + "-servlet.xml", "applicationContext-validation.xml" };

        BeanFactory cpCtx = new ClassPathXmlApplicationContext( paths );

        /* Manual Authentication */
        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) cpCtx
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( "keshav", "pavlab" );

        CompositeSequenceService css = ( CompositeSequenceService ) ctx.getBean( "compositeSequenceService" );

        Collection<CompositeSequence> col = css.getAllCompositeSequences();
        for ( CompositeSequence cs : col ) {
            log.debug( cs );
        }

        if ( col.size() == 0 ) {
            fail( "User not authorized for to access at least one of the objects in the graph" );
        }
    }
}