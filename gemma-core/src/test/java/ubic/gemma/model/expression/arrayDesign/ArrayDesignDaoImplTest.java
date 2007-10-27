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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceDao;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignDaoImplTest extends BaseSpringContextTest {

    private static final String DEFAULT_TAXON = "Mus musculus";

    ArrayDesign ad;
    ArrayDesignDao arrayDesignDao;
    ExternalDatabaseDao externalDatabaseDao;
    BioSequenceDao bioSequenceDao;

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @param bioSequenceDao the bioSequenceDao to set
     */
    public void setBioSequenceDao( BioSequenceDao bioSequenceDao ) {
        this.bioSequenceDao = bioSequenceDao;
    }

    /**
     * @param externalDatabaseDao the externalDatabaseDao to set
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    public void testGetExpressionExperimentsById() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Collection ee = arrayDesignDao.getExpressionExperiments( ad );
        assertNotNull( ee );
    }

    public void testNumBioSequencesById() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        long num = arrayDesignDao.numBioSequences( ad );
        assertNotNull( num );
    }

    public void testNumBlatResultsById() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        long num = arrayDesignDao.numBlatResults( ad );
        assertNotNull( num );
    }

    public void testNumGenesById() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        long num = arrayDesignDao.numGenes( ad );
        assertNotNull( num );
    }

    public void testCompositeSequenceWithoutBioSequences() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Collection cs = arrayDesignDao.compositeSequenceWithoutBioSequences( ad );
        assertNotNull( cs );
    }

    public void testCompositeSequenceWithoutBlatResults() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Collection cs = arrayDesignDao.compositeSequenceWithoutBlatResults( ad );
        assertNotNull( cs );
    }

    public void testCompositeSequenceWithoutGenes() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Collection cs = arrayDesignDao.compositeSequenceWithoutGenes( ad );
        assertNotNull( cs );
    }

    public void testCascadeCreateCompositeSequences() {
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        flushSession(); // fails without this.
        ad = arrayDesignDao.find( ad );
        arrayDesignDao.thaw( ad );
        CompositeSequence cs = ad.getCompositeSequences().iterator().next();

        assertNotNull( cs.getId() );
        assertNotNull( cs.getArrayDesign().getId() );
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
                if ( cs != null ) fail( cs + " was still in the system" );
            } catch ( HibernateObjectRetrievalFailureException e ) {
                // ok
            }
        }

    }

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

    /*
     * A test of getting a taxon assciated with an arrayDesign
     */
    public void testGetTaxon() {

        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Taxon tax = arrayDesignDao.getTaxon( ad.getId() );
        assertEquals( DEFAULT_TAXON, tax.getScientificName() );

    }

    public void testLoadCompositeSequences() {
       
        ad = ( ArrayDesign ) persisterHelper.persist( ad );
        Collection actualValue = arrayDesignDao.loadCompositeSequences( ad.getId() );
        assertEquals( 3, actualValue.size() );
        assertTrue( actualValue.iterator().next() instanceof CompositeSequence );
    }

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

    public void testCountAll() {
        long count = arrayDesignDao.countAll();
        assertNotNull( count );
        assertTrue( count > 0 );
    }

    public void testLoadAllValueObjects() {
        Collection vos = arrayDesignDao.loadAllValueObjects();
        assertNotNull( vos );
    }

    public void testUpdateSubsumingStatus() throws Exception {
        endTransaction();
        ArrayDesign subsumer = this.getTestPersistentArrayDesign( 10, false );
        flushAndClearSession();
        ArrayDesign subsumee = this.getTestPersistentArrayDesign( 5, false );
        flushAndClearSession();
        boolean actualValue = arrayDesignDao.updateSubsumingStatus( subsumer, subsumee );
        assertTrue( !actualValue );
        actualValue = arrayDesignDao.updateSubsumingStatus( subsumee, subsumer );
        assertTrue( !actualValue );
    }

    public void testUpdateSubsumingStatusTrue() throws Exception {
        endTransaction();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "subsuming_arraydesign" );

        // Create the composite Sequences
        CompositeSequence c1 = CompositeSequence.Factory.newInstance();
        c1.setName( "bar" );
        Taxon tax = Taxon.Factory.newInstance();
        tax.setScientificName( DEFAULT_TAXON );
        BioSequence bs = BioSequence.Factory.newInstance( tax );
        bs.setName( "fred" );
        bs.setSequence( "CG" );
        bs.setTaxon( tax );
        c1.setBiologicalCharacteristic( bs );
        ad.getCompositeSequences().add( c1 );

        CompositeSequence c3 = CompositeSequence.Factory.newInstance();
        c3.setName( "foo" );
        tax.setScientificName( DEFAULT_TAXON );
        BioSequence bsb = BioSequence.Factory.newInstance( tax );
        bsb.setName( "barney" );
        bsb.setSequence( "CAAAAG" );
        bsb.setTaxon( tax );
        c3.setBiologicalCharacteristic( bsb );
        ad.getCompositeSequences().add( c3 );

        ad = ( ArrayDesign ) persisterHelper.persist( ad );

        ArrayDesign subsumedArrayDesign = ArrayDesign.Factory.newInstance();
        subsumedArrayDesign.setName( "subsumed_arraydesign" );

        // Create the composite Sequences
        CompositeSequence c2 = CompositeSequence.Factory.newInstance();
        tax.setScientificName( DEFAULT_TAXON );
        c2.setName( "bar" ); // same as one on other AD.
        c2.setBiologicalCharacteristic( bs ); // same as one on other AD.
        subsumedArrayDesign.getCompositeSequences().add( c2 );
        c2.setArrayDesign( subsumedArrayDesign );

        subsumedArrayDesign = ( ArrayDesign ) persisterHelper.persist( subsumedArrayDesign );

        flushAndClearSession();

        boolean actualValue = arrayDesignDao.updateSubsumingStatus( ad, subsumedArrayDesign );
        assertTrue( actualValue );

        actualValue = arrayDesignDao.updateSubsumingStatus( subsumedArrayDesign, ad );
        assertTrue( !actualValue );
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

    private String getGpl() {
        return "GPL" + RandomStringUtils.randomNumeric( 4 );
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();

        // Create Array design
        ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 20 ) + "_arraydesign" );
        ad.setShortName( ad.getName() );

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

        Taxon tax = Taxon.Factory.newInstance();
        tax.setScientificName( DEFAULT_TAXON );
        BioSequence bs = BioSequence.Factory.newInstance( tax );
        bs.setName( RandomStringUtils.randomAlphabetic( 10 ) );
        bs.setSequence( RandomStringUtils.random( 40, "ATCG" ) );
        bs.setTaxon( tax );

        c1.setBiologicalCharacteristic( bs );
        c2.setBiologicalCharacteristic( bs );
        c3.setBiologicalCharacteristic( bs );

        ad.getCompositeSequences().add( c1 );
        ad.getCompositeSequences().add( c2 );
        ad.getCompositeSequences().add( c3 );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        if ( ad != null && ad.getId() != null ) {
            log.info( "Deleting " + ad );
            // also remove the sequences
            // arrayDesignDao.thaw( ad );
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                bioSequenceDao.remove( cs.getBiologicalCharacteristic() );
            }

            this.arrayDesignDao.remove( ad );

        }
    }

}
