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
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceDaoImplTest extends BaseDAOTestCase {

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

        ed = ExternalDatabase.Factory.newInstance();
        ed.setLocalInstallDbName( "database" );

        exdbdao.create( ed );

        de.setExternalDatabase( ed );
        deb.setExternalDatabase( ed );
        dedao.create( de );
        dedao.create( deb );

        testBibRef.setPubAccession( de );

        dao.create( testBibRef );
    }

    protected void tearDown() throws Exception {
        // dedao.remove( de );
        // dedao.remove( deb );
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
        assertTrue( dao.findByExternalId( deb ) == null );

        // create assigns a new id, so this just creates yet another bibliographic reference.
        // try {
        // dao.create( testBibRef );
        // fail( "Create didn't throw DataIntegrityViolationException" );
        // } catch ( DataIntegrityViolationException e ) {
        // assertNotNull( e );
        // log.info( "Good, expected exception" );
        // }

    }

}