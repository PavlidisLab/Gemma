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
package ubic.gemma.model.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.orm.hibernate3.HibernateObjectRetrievalFailureException;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceDao;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignDaoImplTest extends BaseTransactionalSpringContextTest {
    ArrayDesign ad;
    ArrayDesignDao arrayDesignDao;
    ExternalDatabaseDao externalDatabaseDao;

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @param externalDatabaseDao the externalDatabaseDao to set
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    public void testCascadeCreateCompositeSequences() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        flushSession(); // fails without this.
        ad = arrayDesignDao.find( ad );
        CompositeSequence cs = ad.getCompositeSequences().iterator().next();

        assertNotNull( cs.getId() );
        assertNotNull( cs.getArrayDesign().getId() );
    }

    public void testCascadeDeleteOrphanCompositeSequences() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        CompositeSequence cs = ad.getCompositeSequences().iterator().next();
        ad.getCompositeSequences().remove( cs );
        cs.setArrayDesign( null );
        arrayDesignDao.update( ad );
        assertEquals( 2, ad.getCompositeSequences().size() );
    }

    // FIXME: Need to add a meaning test of reporters
    // public void testCascadeDeleteOrphanReporters() {
    // Reporter cs = ad.getReporters().iterator().next();
    // ad.getReporters().remove( cs );
    // cs.setArrayDesign( null );
    // arrayDesignDao.update( ad );
    // assertEquals( 2, ad.getReporters().size() );
    // }

    public void testFindWithExternalReference() {
        ad = ArrayDesign.Factory.newInstance();
        String name = RandomStringUtils.randomAlphabetic( 20 ) + "_arraydesign";
        ad.setName( name );

        String gplToFind = getGpl();

        assignExternalReference( ad, gplToFind );
        assignExternalReference( ad, getGpl() );
        ad = ( ArrayDesign ) persisterHelper.persist( ad );

        ArrayDesign toFind = ArrayDesign.Factory.newInstance();

        // artficial, wouldn't normally have multiple GEO acc
        assignExternalReference( toFind, getGpl() );
        assignExternalReference( toFind, getGpl() );
        assignExternalReference( toFind, gplToFind );
        ArrayDesign found = arrayDesignDao.find( toFind );

        assertNotNull( found );
    }

    private String getGpl() {
        return "GPL" + RandomStringUtils.randomNumeric( 4 );
    }

    public void testFindWithExternalReferenceNotFound() {
        ad = ArrayDesign.Factory.newInstance();
        assignExternalReference( ad, getGpl() );
        assignExternalReference( ad, getGpl() );
        String name = RandomStringUtils.randomAlphabetic( 20 ) + "_arraydesign";
        ad.setName( name );
        ad = ( ArrayDesign ) persisterHelper.persist( ad );

        ArrayDesign toFind = ArrayDesign.Factory.newInstance();

        // artficial, wouldn't normally have multiple GEO acc
        assignExternalReference( toFind, getGpl() );
        assignExternalReference( toFind, getGpl() );
        ArrayDesign found = arrayDesignDao.find( toFind );

        assertNull( found );
    }

    public void testLoadCompositeSequences() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Collection actualValue = arrayDesignDao.loadCompositeSequences( ad.getId() );
        assertEquals( 3, actualValue.size() );
        assertTrue( actualValue.iterator().next() instanceof CompositeSequence );
    }

    // public void testLoadReporters() {
    // ad = ( ArrayDesign ) persisterHelper.persist( ad );
    // Collection actualValue = arrayDesignDao.loadReporters( ad.getId() );
    // assertEquals( 3, actualValue.size() );
    // assertTrue( actualValue.iterator().next() instanceof Reporter );
    // }

    /*
     * Test method for 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoImpl.numCompositeSequences(ArrayDesign)'
     */
    public void testNumCompositeSequencesArrayDesign() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Integer actualValue = arrayDesignDao.numCompositeSequences( ad.getId() );
        Integer expectedValue = 3;
        assertEquals( expectedValue, actualValue );
    }

    /*
     * Test method for 'ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoImpl.numReporters(ArrayDesign)'
     */
    public void testNumReportersArrayDesign() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Integer actualValue = arrayDesignDao.numReporters( ad.getId() );
        Integer expectedValue = 3;
        assertEquals( expectedValue, actualValue );
    }

    public void testDelete() {

        ad = ( ArrayDesign ) persisterHelper.persist( ad );

        Collection<CompositeSequence> seqs = ad.getCompositeSequences();
        Collection<Long> seqIds = new ArrayList<Long>();
        for ( CompositeSequence seq : seqs ) {
            if ( seq.getId() == null ) {
                continue; // why?
            }
            seqIds.add( seq.getId() );
        }
        arrayDesignDao.remove( ad );

        CompositeSequenceDao compositeSequenceDao = ( CompositeSequenceDao ) getBean( "compositeSequenceDao" );
        for ( Long id : seqIds ) {
            try {
                CompositeSequence cs = ( CompositeSequence ) compositeSequenceDao.load( id );
                fail( cs + " was still in the system" );
            } catch ( HibernateObjectRetrievalFailureException e ) {
                // ok
            }
        }

    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        // Create Array design
        ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 20 ) + "_arraydesign" );

        // Create the composite Sequences
        CompositeSequence c1 = CompositeSequence.Factory.newInstance();
        c1.setName( RandomStringUtils.randomAlphabetic( 20 ) + "_cs" );
        CompositeSequence c2 = CompositeSequence.Factory.newInstance();
        c2.setName( RandomStringUtils.randomAlphabetic( 20 ) + "_cs" );
        CompositeSequence c3 = CompositeSequence.Factory.newInstance();
        c3.setName( RandomStringUtils.randomAlphabetic( 20 ) + "_cs" );

        // Fill in associations between compositeSequences and arrayDesign
        c1.setArrayDesign( ad );
        c2.setArrayDesign( ad );
        c3.setArrayDesign( ad );

        // Create the Reporters
        Reporter r1 = Reporter.Factory.newInstance();
        r1.setName( "rfoo" );
        Reporter r2 = Reporter.Factory.newInstance();
        r2.setName( "rbar" );
        Reporter r3 = Reporter.Factory.newInstance();
        r3.setName( "rfar" );

        // Fill in associations between reporters and CompositeSequences
        r1.setCompositeSequence( c1 );
        r2.setCompositeSequence( c2 );
        r3.setCompositeSequence( c3 );

        c1.getComponentReporters().add( r1 );
        c2.getComponentReporters().add( r2 );
        c3.getComponentReporters().add( r3 );

        ad.getCompositeSequences().add( c1 );
        ad.getCompositeSequences().add( c2 );
        ad.getCompositeSequences().add( c3 );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        if ( ad != null && ad.getId() != null ) {
            log.info( "Deleting " + ad );
            this.arrayDesignDao.remove( ad );
        }
    }

    /**
     * @param accession
     */
    private void assignExternalReference( ArrayDesign toFind, String accession ) {
        ExternalDatabase geo = externalDatabaseDao.findByName( "GEO" );
        assert geo != null;

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setExternalDatabase( geo );

        de.setAccession( accession );

        toFind.getExternalReferences().add( de );
    }

}
