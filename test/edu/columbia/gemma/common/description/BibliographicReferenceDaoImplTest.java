package edu.columbia.gemma.common.description;

import java.util.Date;
import java.util.Iterator;

import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceDaoImplTest extends BaseDAOTestCase {

    private final Log log = LogFactory.getLog( BibliographicReferenceDaoImplTest.class );
    private BibliographicReferenceDao dao = null;
    private DatabaseEntryDao dedao = null;
    private ExternalDatabaseDao exdbdao = null;
    private SessionFactory sf = null;
    private BibliographicReference testBibRef = null;
    private DatabaseEntry de = null;
    private DatabaseEntry deb = null;
    ExternalDatabase ed = null;

    protected void setUp() throws Exception {
        super.setUp();
        testBibRef = BibliographicReference.Factory.newInstance();
        dao = ( BibliographicReferenceDao ) ctx.getBean( "bibliographicReferenceDao" );
        dedao = ( DatabaseEntryDao ) ctx.getBean( "databaseEntryDao" );
        exdbdao = ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" );
        sf = ( SessionFactory ) ctx.getBean( "sessionFactory" );

        de = DatabaseEntry.Factory.newInstance();
        deb = DatabaseEntry.Factory.newInstance();

        de.setAccession( "foo" );
        deb.setAccession( "bar" );

        String random = ( new Date() ).toString();
        ed = ExternalDatabase.Factory.newInstance();
        ed.setLocalInstallDbName( "database" );
        ed.setIdentifier( "fooblydoobly" + random );
        exdbdao.create( ed );

        de.setExternalDatabase( ed );
        deb.setExternalDatabase( ed );
        dedao.create( de );
        dedao.create( deb );

        testBibRef.setPubAccession( de );
        testBibRef.setIdentifier( random );
        dao.create( testBibRef );
    }

    protected void tearDown() throws Exception {
        dao.remove( testBibRef );
        exdbdao.remove( ed );
        dao = null;
        dedao = null;
        exdbdao = null;
    }

    public final void testFindByExternalIdentStringHQL() throws Exception {
        String query = "from BibliographicReferenceImpl b where b.pubAccession=:externalId";

        Session sess = sf.openSession();
        Transaction trans = sess.beginTransaction();

        Query q = sess.createQuery( query );

        q.setParameter( "externalId", de );

        for ( Iterator it = q.iterate(); it.hasNext(); ) {
            BibliographicReference b = ( BibliographicReference ) it.next();
            assertEquals( testBibRef, b );
        }
        sess.flush();
        trans.commit();
        sess.close();

    }

    /*
     * Class under test for Object findByExternalId(int, java.lang.String)
     */
//    public final void testFindByExternalIdentString() {
//
//        testBibRef = dao.findByExternalId( de );
//
//        assertTrue( testBibRef != null );
//        assertTrue( dao.findByExternalId( deb ) == null );
//
//        try {
//            dao.create( testBibRef );
//            fail( "Create didn't throw DataIntegrityViolationException" );
//        } catch ( DataIntegrityViolationException e ) {
//            assertNotNull( e );
//            log.info( "Good, expected exception: " + e.getMessage() );
//        }
//
//    }

}