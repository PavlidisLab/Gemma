package edu.columbia.gemma.expression.arrayDesign;

import java.util.Collection;
import java.util.Iterator;

import org.easymock.MockControl;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.interceptor.ManualAuthenticationProcessing;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignServiceImplTest extends BaseServiceTestCase {

    private ArrayDesignServiceImpl arrayDesignService = new ArrayDesignServiceImpl();
    private ArrayDesignDao arrayDesignDaoMock = null;
    private MockControl control;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        control = MockControl.createControl( ArrayDesignDao.class );
        arrayDesignDaoMock = ( ArrayDesignDao ) control.getMock();
        arrayDesignService.setArrayDesignDao( arrayDesignDaoMock );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @throws Exception TODO add security to the method, then add it back to list of tests.
     */
    // public void testGetAllArrayDesigns() throws Exception {
    // // to implement this test, the mock dao has to save several objects.
    // Collection m = new HashSet();
    //
    // for ( int i = 0; i < 5; i++ ) {
    // ArrayDesign tad = ArrayDesign.Factory.newInstance();
    //
    // tad.setName( "Foo" + i );
    // if ( !m.add( tad ) ) throw new IllegalStateException( "Couldn't add to the collection - check equals" );
    // }
    //
    // // set the behavior for the DAO: get all array designs will retrive the collection.
    // arrayDesignDaoMock.getAllArrayDesigns();
    // control.setReturnValue( m );
    //
    // control.replay(); // switch from record mode to replay
    // arrayDesignService.getAllArrayDesigns();
    // control.verify(); // verify that expectations were met
    // }
    //
    // /**
    // * @throws Exception TODO add security to this test, then add it back to list of tests.
    // */
    // public void testSaveArrayDesign() throws Exception {
    // ArrayDesign tad = ArrayDesign.Factory.newInstance();
    //
    // tad.setName( "Foo" );
    //
    // // The expected behavior
    // arrayDesignDaoMock.create( tad );
    // control.setReturnValue( tad );
    //
    // control.replay(); // switch from record mode to replay
    // arrayDesignService.saveArrayDesign( tad );
    // control.verify(); // verify that expectations were met.
    // }
    /**
     * Test removing an arrayDesign without having the correct authorization privileges. You should get an
     * unsuccessfulAuthentication.
     */
    // public void testRemoveArrayDesignWithoutAuthorization() throws Exception {
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
    // // TODO add a more appropriate assertion (ie. specific to acegi security) and then uncomment this test.
    // assertNull( null, null );
    // }
    /**
     * Test removing an arrayDesign with correct authorization. The security interceptor should be called on this
     * method, as should the PersistAclInterceptorBackend.
     */
    public void testRemoveArrayDesign() throws Exception {
        BeanFactory ctx = SpringContextUtil.getApplicationContext();

        ManualAuthenticationProcessing manAuthentication = ( ManualAuthenticationProcessing ) ctx
                .getBean( "manualAuthenticationProcessing" );

        manAuthentication.validateRequest( "pavlab", "pavlab" );

        ArrayDesignService ads = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );

        Collection<ArrayDesign> col = ads.getAllArrayDesigns();
        if ( col.size() == 0 )
            log.info( "There are no arrayDesigns in database" );

        else {
            Iterator iter = col.iterator();
            ArrayDesign ad = ( ArrayDesign ) iter.next();

            ads.removeArrayDesign( ad );
        }
    }
}