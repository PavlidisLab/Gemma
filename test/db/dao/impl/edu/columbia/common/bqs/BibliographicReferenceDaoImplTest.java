package edu.columbia.common.bqs;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;

import edu.columbia.BaseDAOTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceDaoImplTest extends BaseDAOTestCase {

   private BibliographicReferenceDao dao = null;

   protected void setUp() throws Exception {
      Log log = LogFactory.getLog( BibliographicReferenceDaoImplTest.class );
      dao = ( BibliographicReferenceDao ) ctx.getBean( "bibliographicReferenceDao" );
   }

   protected void tearDown() throws Exception {
      dao = null;
   }

   /*
    * Class under test for Object findByExternalId(int, java.lang.String)
    */
   public final void testFindByExternalIdintString() {

      BibliographicReference f = new BibliographicReferenceImpl();

      String random = ( new Date() ).toString();

      f.setExternalId( random );
      f.setIdentifier( random );
      dao.create( f );

      f = dao.findByExternalId( random );

      assertTrue( f != null );
      assertTrue( dao.findByExternalId( "192029" ) == null );

      try {
         dao.create( f );
         fail( "Create didn't throw DataIntegrityViolationException" );
      } catch ( DataIntegrityViolationException e ) {
         assertNotNull( e );
         log.debug( "Good, expected exception: " + e.getMessage() );
      }

   }

}