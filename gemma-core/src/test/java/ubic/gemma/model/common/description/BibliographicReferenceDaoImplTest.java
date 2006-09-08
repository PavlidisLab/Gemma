/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.common.description;

import java.util.Iterator;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * This class tests the bibliographic reference data access object. It is also used to test some of the Hibernate
 * features.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceDaoImplTest extends BaseTransactionalSpringContextTest {

    private BibliographicReferenceDao bibliographicReferenceDao = null;
    private DatabaseEntry de = null;
    private SessionFactory sessionFactory = null;
    private BibliographicReference testBibRef = null;
    private PersisterHelper persisterHelper;
    ExternalDatabase ed = null;

    /*
     * Call to create should persist the BibliographicReference and DatabaseEntry (cascade=all).
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        persisterHelper = ( PersisterHelper ) this.getBean( "persisterHelper" );
        testBibRef = BibliographicReference.Factory.newInstance();

        de = this.getTestPersistentDatabaseEntry( "PubMed" );

        /* Set the DatabaseEntry. */
        testBibRef.setPubAccession( de );
        AuditTrail ad = AuditTrail.Factory.newInstance();
        ad = ( AuditTrail ) persisterHelper.persist( ad );

        testBibRef.setAuditTrail( ad );

        bibliographicReferenceDao.create( testBibRef );
    }

    /**
     * @param dao The dao to set.
     */
    public void setBibliographicReferenceDao( BibliographicReferenceDao dao ) {
        this.bibliographicReferenceDao = dao;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public final void testfind() throws Exception {
        BibliographicReference result = this.bibliographicReferenceDao.find( testBibRef );
        assertTrue( result != null );
    }

    /*
     * Class under test for Object findByExternalId(int, java.lang.String)
     */
    public final void testFindByExternalIdentString() {
        testBibRef = bibliographicReferenceDao.findByExternalId( de );
        assertTrue( testBibRef != null );
    }

    /*
     * 
     */
    public final void testFindByExternalIdentStringHQL() throws Exception {
        String query = "from BibliographicReferenceImpl b where b.pubAccession=:externalId";

        sessionFactory = ( SessionFactory ) getContext( super.getConfigLocations() ).getBean( "sessionFactory" );
        Session sess = sessionFactory.openSession();
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

}