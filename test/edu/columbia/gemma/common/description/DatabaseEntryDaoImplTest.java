package edu.columbia.gemma.common.description;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DatabaseEntryDaoImplTest extends BaseDAOTestCase {
    private static Log log = LogFactory.getLog( DatabaseEntryDaoImplTest.class.getName() );
    DatabaseEntryDao deDao;
    DatabaseEntry de;

    ExternalDatabaseDao exdbDao;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        deDao = ( DatabaseEntryDao ) ctx.getBean( "databaseEntryDao" );
        exdbDao = ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" );
        de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "bar" );
        ExternalDatabase ed = exdbDao.findByName( "PubMed" );
        assert ed != null;
        de.setExternalDatabase( ed );
        de = deDao.create( de );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        deDao.remove( de );
    }

    /*
     * Class under test for DatabaseEntry findOrCreate(DatabaseEntry)
     */
    public void testFindOrCreateDatabaseEntry() {
        deDao.remove( de );
        DatabaseEntry actualReturn = deDao.findOrCreate( de );
        assertEquals( de.getAccession(), actualReturn.getAccession() );
    }

    /*
     * Class under test for edu.columbia.gemma.common.description.DatabaseEntry
     * find(edu.columbia.gemma.common.description.DatabaseEntry)
     */
    public void testFindDatabaseEntry() {
        DatabaseEntry actualReturn = deDao.find( de );
        assertEquals( de, actualReturn );
    }

}
