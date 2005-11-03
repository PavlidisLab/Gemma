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
package edu.columbia.gemma.expression.arrayDesign;

import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.CompositeSequenceService;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.security.ui.ManualAuthenticationProcessing;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * Use this to test the acegi functionality. Namely, this is a good test to illustrate how getting This test class
 * represents an integration style test.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignServiceImplIntegrationTest extends BaseServiceTestCase {

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
     * unsuccessfulAuthentication. Does not use mock objects because I need the data from the database.
     * 
     * @throws Exception
     */
    // public void testRemoveArrayDesignWithoutAuthorizationWithoutMock() throws Exception {
    // BeanFactory ctx = SpringContextUtil.getApplicationContext();
    //
    // ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctx
    // .getBean( "manualAuthenticationProcessing" );
    //
    // manAuthentication.validateRequest( "KiranKeshav", "7Champin" );
    //
    // ArrayDesignService ads = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );
    //
    // Collection<ArrayDesign> col = ads.getAllArrayDesigns();
    // if ( col.size() == 0 )
    // log.info( "There are no arrayDesigns in database" );
    //
    // else {
    // Iterator iter = col.iterator();
    // ArrayDesign ad = ( ArrayDesign ) iter.next();
    //
    // ads.removeArrayDesign( ad );
    // }
    //
    // 
    // assertNull( null, null );
    // }
    /**
     * Test removing an arrayDesign with correct authorization. The security interceptor should be called on this
     * method, as should the AddOrRemoveFromACLInterceptor. Does not use mock objects because I need to remove an
     * element from a live database.
     * 
     * @throws Exception
     */
     public void testRemoveArrayDesignWithoutMock() throws Exception {
    
     /*
     * Use this or your ManualAuthenticationProcessing solution. The authentication from a non-http client here
     * comes from Fish du jour - See SpringContextUtil BeanFactory ctx = SpringContextUtil.getApplicationContext(
     * false );
     */
            
     /* Context Setup - I did not use SpringContextUtil because this creates a principal provides both admin and user
     rights.
     * This test is used to go deeper and actually test out the security so I don't want to use it.
     */
     ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
     String daoType = db.getString( "dao.type" );
     String servletContext = db.getString( "servlet.name.0" );
     String[] paths = { "applicationContext-localDataSource.xml", "applicationContext-" + daoType + ".xml",
     "applicationContext-security.xml", servletContext + "-servlet.xml", "applicationContext-validation.xml" };
    
     BeanFactory cpCtx = new ClassPathXmlApplicationContext( paths );
    
     /* Manual Authentication*/
     ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) cpCtx
     .getBean( "manualAuthenticationProcessing" );
            
     manAuthentication.validateRequest( "pavlab", "pavlab" );
            
     /* get bean and invoke methods. */
     ArrayDesignService ads = ( ArrayDesignService ) cpCtx.getBean( "arrayDesignService" );
    
     ArrayDesign ad = ads.findArrayDesignByName( "AD Foo" );
     if ( ad == null )
     log.info( "ArrayDesign does not exist" );
    
     else
     ads.removeArrayDesign( ad );
     }
    /**
     * Save an array design
     * 
     * @throws Exception
     */
//     public void testSaveArrayDesignWithoutMock() {
//            
//     ArrayDesignService ads = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );
//            
//     ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
//     arrayDesign.setName( "AD Foo" );
//     arrayDesign.setDescription( "a test ArrayDesign" );
//            
//     CompositeSequence cs1 = CompositeSequence.Factory.newInstance();
//     cs1.setName( "DE Bar1" );
//            
//     CompositeSequence cs2 = CompositeSequence.Factory.newInstance();
//     cs2.setName( "DE Bar2" );
//            
//     Collection<DesignElement> col = new HashSet();
//     col.add( cs1 );
//     col.add( cs2 );
//     // Note this sequence. Remember, inverse="true" if using this. If you do not make
//     // an explicit call to cs1(2).setArrayDesign(arrayDesign), then inverse="false" must be set.
//     cs1.setArrayDesign( arrayDesign );
//     cs2.setArrayDesign( arrayDesign );
//     arrayDesign.setDesignElements( col );
//            
//     try {
//     ArrayDesign ad = ads.saveArrayDesign( arrayDesign );
//            
//     } catch ( ArrayDesignExistsException e ) {
//            
//     }
//            
//     }
    /**
     * Will test getting all design elements given authorization on an array design (ie. tests getting the 'owned
     * objects' given the authorization on an owner). This test was used to test the Acegi Security functionality. Mock
     * objects not used because I need the objects from the database.
     * 
     * @throws Exception
     */
    // @SuppressWarnings("unchecked")
    // public void testGetAllDesignElementsFromArrayDesignsWithoutMock() {
    // CompositeSequenceService css = ( CompositeSequenceService ) ctx.getBean( "compositeSequenceService" );
    //    
    // Collection<CompositeSequence> col = css.getAllCompositeSequences();
    // for ( CompositeSequence cs : col ) {
    // log.debug( cs );
    // }
    //    
    // if ( col.size() == 0 ) fail( "User not authorized for to access at least one of the objects in the graph" );
    // }
}