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
      arrayDesignService.setArrayDesign( arrayDesignDaoMock );
   }

   /*
    * @see TestCase#tearDown()
    */
   protected void tearDown() throws Exception {
      super.tearDown();
   }

   public void testGetArrayDesigns() {
      // to implement this test, the mock dao has to save several objects.
      Collection m = new HashSet();
      control.reset();
      for ( int i = 0; i < 5; i++ ) {
         ArrayDesign tad = new ArrayDesignImpl();
         String id = ( new Date() ).toString();
         tad.setIdentifier( id );
         tad.setName( "Foo" + i );
         m.add( tad );
      }
      arrayDesignDaoMock.getAllArrayDesigns();
      control.setReturnValue( m );

      control.replay(); // switch from record mode to replay
      Collection allDesigns = arrayDesignService.getAllArrayDesigns();
      assertEquals( allDesigns, m );
      control.verify(); // verify that expectations were met
   }

   public void testSaveArrayDesign() {
      ArrayDesign tad = new ArrayDesignImpl();
      String id = ( new Date() ).toString();
      tad.setIdentifier( id );
      tad.setName( "Foo" );

      // The expected behavior
      arrayDesignDaoMock.findByName( "Foo" ); // Todo - method doesn't exist yet.

      control.replay(); // switch from record mode to replay
      arrayDesignService.saveArrayDesign( tad );
      control.verify(); // verify that expectations were met.

   }

   public void testRemoveArrayDesign() {
      // TODO Implement removeArrayDesign().
   }

}