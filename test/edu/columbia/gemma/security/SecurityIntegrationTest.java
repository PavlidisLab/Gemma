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

import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrailService;
import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.CompositeSequenceService;
import edu.columbia.gemma.security.ui.ManualAuthenticationProcessing;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * Use this to test the acegi functionality.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class SecurityIntegrationTest extends BaseDAOTestCase {

    private static String testUser = "administrator";
    private static String testPassword = "admintoast";
    private BeanFactory ctxLocal;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        // super.setUp(); // Don't do this or you get the security settings for tests.

        ctxLocal = SpringContextUtil.getXmlWebApplicationContext( true, true );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test removing an arrayDesign without having the correct authorization privileges. You should get an
     * unsuccessfulAuthentication. Does not use mock objects because I need the data from the database
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testRemoveArrayDesignWithoutAuthorizationWithoutMock() throws Exception {
        /* Manual Authentication */
        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctxLocal
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( testUser, testPassword );

        ArrayDesignService ads = ( ArrayDesignService ) ctxLocal.getBean( "arrayDesignService" );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "deleteme" );
        ad = ( ArrayDesign ) this.getPersisterHelper().persist( ad );

        ads.remove( ad );
    }

    /**
     * Test removing an arrayDesign with correct authorization. The security interceptor should be called on this
     * method, as should the AddOrRemoveFromACLInterceptor. Does not use mock objects because I need to remove an
     * element from a live database.
     * 
     * @throws Exception
     */
    public void testRemoveArrayDesignWithoutMock() throws Exception {

        /* Manual Authentication */
        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctxLocal
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( testUser, testPassword );

        /* get bean and invoke methods. */
        ArrayDesignService ads = ( ArrayDesignService ) ctxLocal.getBean( "arrayDesignService" );

        ArrayDesign ad = ads.findArrayDesignByName( "AD Foo" );
        if ( ad == null )
            log.info( "ArrayDesign does not exist" );

        else {
            ads.remove( ad );
        }
    }

    /**
     * Save an array design.
     * 
     * @throws Exception
     */
    public void testSaveArrayDesignWithoutMock() throws Exception {

        /* Manual Authentication */
        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctxLocal
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( testUser, testPassword );

        ArrayDesignService ads = ( ArrayDesignService ) ctxLocal.getBean( "arrayDesignService" );
        AuditTrailService ats = ( AuditTrailService ) ctxLocal.getBean( "auditTrailService" );
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

        Collection<CompositeSequence> col = new HashSet<CompositeSequence>();
        col.add( cs1 );
        col.add( cs2 );

        /*
         * Note this sequence. Remember, inverse="true" if using this. If you do not make an explicit call to
         * cs1(2).setArrayDesign(arrayDesign), then inverse="false" must be set.
         */
        cs1.setArrayDesign( arrayDesign );
        cs2.setArrayDesign( arrayDesign );
        arrayDesign.setCompositeSequences( col );

        ads.findOrCreate( arrayDesign );

    }

    /**
     * Tests getting all design elements given authorization on an array design (ie. tests getting the 'owned objects'
     * given the authorization on the owner). This test was used to test the Acegi Security functionality. Mock objects
     * not used because I need the objects from the database.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testGetAllDesignElementsFromArrayDesignsWithoutMock() throws Exception {

        /* Manual Authentication */
        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctxLocal
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( testUser, testPassword );

        CompositeSequenceService css = ( CompositeSequenceService ) ctxLocal.getBean( "compositeSequenceService" );

        Collection<CompositeSequence> col = css.getAllCompositeSequences();
        for ( CompositeSequence cs : col ) {
            log.debug( cs );
        }

        if ( col.size() == 0 ) {
            fail( "User not authorized for to access at least one of the objects in the graph" );
        }
    }
}