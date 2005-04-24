package edu.columbia.gemma.expression.arrayDesign;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.easymock.MockControl;

import edu.columbia.gemma.BaseServiceTestCase;

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

    public void testGetArrayDesigns() throws Exception {
        // to implement this test, the mock dao has to save several objects.
        Collection m = new HashSet();

        for ( int i = 0; i < 5; i++ ) {
            ArrayDesign tad = ArrayDesign.Factory.newInstance();
            
            tad.setName( "Foo" + i );
            if ( !m.add( tad ) ) throw new IllegalStateException( "Couldn't add to the collection - check equals" );
        }

        // set the behavior for the DAO: get all array designs will retrive the collection.
        arrayDesignDaoMock.getAllArrayDesigns();
        control.setReturnValue( m );

        control.replay(); // switch from record mode to replay
        arrayDesignService.getAllArrayDesigns();
        control.verify(); // verify that expectations were met
    }

    public void testSaveArrayDesign() throws Exception {
        ArrayDesign tad = ArrayDesign.Factory.newInstance();
        
        tad.setName( "Foo" );

        // The expected behavior
        arrayDesignDaoMock.create( tad );
        control.setReturnValue( tad );

        control.replay(); // switch from record mode to replay
        arrayDesignService.saveArrayDesign( tad );
        control.verify(); // verify that expectations were met.
    }

    public void testRemoveArrayDesign() throws Exception {
        // TODO Implement removeArrayDesign().
    }

}