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
package edu.columbia.gemma.common.description;

import java.util.Iterator;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * This class tests the bibliographic reference data access object. It is also used to test some of the Hibernate
 * features.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceDaoImplTest extends BaseDAOTestCase {

    private BibliographicReferenceDao dao = null;
    private ExternalDatabaseDao exdbdao = null;
    private SessionFactory sf = null;
    private BibliographicReference testBibRef = null;
    private DatabaseEntry de = null;
    ExternalDatabase ed = null;

    /*
     * Call to create should persist the BibliographicReference and DatabaseEntry (cascade=all).
     */
    protected void setUp() throws Exception {
        super.setUp();
        testBibRef = BibliographicReference.Factory.newInstance();
        dao = ( BibliographicReferenceDao ) ctx.getBean( "bibliographicReferenceDao" );
        exdbdao = ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" );
        sf = ( SessionFactory ) ctx.getBean( "sessionFactory" );

        /*
         * BibliographicReference has composition relationship with DatabaseEntry.
         */
        de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "foo" );

        /*
         * DatabaseEntry has a non-composition association with ExternalDatabase. Due to this, create ExternalDatabase
         * and persist manually. If the relationship between DatabaseEntry and ExternalDatabase were composition, we
         * would create the externalDatabase, stuff it in a databaseEntry, and persist the databaseEntry (which would in
         * turn persist the externalDatabase).
         */
        ed = ExternalDatabase.Factory.newInstance();
        ed.setLocalInstallDbName( "testDatabase" );
        ed.setName( "database" );
        ed = ( ExternalDatabase ) exdbdao.create( ed );

        de.setExternalDatabase( ed );

        /* Set the DatabaseEntry. */
        testBibRef.setPubAccession( de );

        dao.create( testBibRef );
    }

    /*
     * Call to remove should delete both BibliographicReference and DatabaseEntry (composition and cascade=all).
     */
    protected void tearDown() throws Exception {

        dao.remove( testBibRef );
        exdbdao.remove( ed );
        dao = null;
        exdbdao = null;
    }

    /*
     * 
     */
    public final void testFindByExternalIdentStringHQL() throws Exception {
        String query = "from BibliographicReferenceImpl b where b.pubAccession=:externalId";

        Session sess = sf.openSession();
        Transaction trans = sess.beginTransaction();

        Query q = sess.createQuery( query );

        q.setParameter( "externalId", de );

        for ( Iterator it = q.iterate(); it.hasNext(); ) {
            BibliographicReference b = ( BibliographicReference ) it.next();
            assertEquals( testBibRef.getPubAccession(), b.getPubAccession() );
        }
        sess.flush();
        trans.commit();
        sess.close();

    }

    /*
     * Class under test for Object findByExternalId(int, java.lang.String)
     */
    public final void testFindByExternalIdentString() {
        testBibRef = dao.findByExternalId( de );
        assertTrue( testBibRef != null );
    }

    public final void testfind() throws Exception {
        BibliographicReference result = this.dao.find( testBibRef );
        assertTrue( result != null );
    }
}