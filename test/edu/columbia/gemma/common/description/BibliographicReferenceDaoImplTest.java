package edu.columbia.gemma.common.description;

import java.util.Date;

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

    protected void setUp() throws Exception {
        super.setUp();

        dao = ( BibliographicReferenceDao ) ctx.getBean( "bibliographicReferenceDao" );
        dedao = ( DatabaseEntryDao ) ctx.getBean( "databaseEntryDao" );
        exdbdao = ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" );
    }

    protected void tearDown() throws Exception {
        dao = null;
        dedao = null;
        exdbdao = null;
    }

    /*
     * Class under test for Object findByExternalId(int, java.lang.String)
     */
    public final void testFindByExternalIdentString() {

        BibliographicReference f = BibliographicReference.Factory.newInstance();

        String random = ( new Date() ).toString();

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        DatabaseEntry deb = DatabaseEntry.Factory.newInstance();

        de.setAccession( "foo" );
        deb.setAccession( "bar" );

        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setLocalInstallDBName( "database" );
        ed.setIdentifier( "fooblydoobly" + random );
        exdbdao.create( ed );

        de.setExternalDatabase( ed );
        deb.setExternalDatabase( ed );
        dedao.create( de );
        dedao.create( deb );

        f.setPubAccession( de );
        f.setIdentifier( random );
        dao.create( f );

        f = dao.findByExternalId( de );

        assertTrue( f != null );
        assertTrue( dao.findByExternalId( deb ) == null );

        try {
            dao.create( f );
            fail( "Create didn't throw DataIntegrityViolationException" );
        } catch ( DataIntegrityViolationException e ) {
            assertNotNull( e );
            log.debug( "Good, expected exception: " + e.getMessage() );
        }

    }

}