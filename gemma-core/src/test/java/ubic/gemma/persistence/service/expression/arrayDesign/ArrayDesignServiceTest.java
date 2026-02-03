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
package ubic.gemma.persistence.service.expression.arrayDesign;

import gemma.gsec.SecurityService;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.UnresolvableObjectException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
public class ArrayDesignServiceTest extends BaseSpringContextTest {

    private static final String DEFAULT_TAXON = "Mus musculus";

    private ArrayDesign ad;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private SecurityService securityService;

    @Before
    public void setUp() throws Exception {

        // Create Array design, don't persist it.
        ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.insecure().nextAlphabetic( 20 ) + "_arraydesign" );
        ad.setShortName( ad.getName() );

        // Create the composite Sequences
        CompositeSequence c1 = CompositeSequence.Factory.newInstance();
        c1.setName( RandomStringUtils.insecure().nextAlphabetic( 20 ) + "_cs" );
        CompositeSequence c2 = CompositeSequence.Factory.newInstance();
        c2.setName( RandomStringUtils.insecure().nextAlphabetic( 20 ) + "_cs" );
        CompositeSequence c3 = CompositeSequence.Factory.newInstance();
        c3.setName( RandomStringUtils.insecure().nextAlphabetic( 20 ) + "_cs" );

        // Fill in associations between compositeSequences and arrayDesign
        c1.setArrayDesign( ad );
        c2.setArrayDesign( ad );
        c3.setArrayDesign( ad );

        Taxon tax = this.getTaxon( "mouse" );

        ad.setPrimaryTaxon( tax );

        BioSequence bs = BioSequence.Factory.newInstance( tax );
        bs.setName( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        bs.setSequence( RandomStringUtils.insecure().next( 40, "ATCG" ) );
        bs.setTaxon( tax );

        c1.setBiologicalCharacteristic( bs );
        c2.setBiologicalCharacteristic( bs );
        c3.setBiologicalCharacteristic( bs );

        ad.getCompositeSequences().add( c1 );
        ad.getCompositeSequences().add( c2 );
        ad.getCompositeSequences().add( c3 );
    }

    @Test
    public void testCascadeCreateCompositeSequences() {
        ad = persisterHelper.persist( ad );

        ad = arrayDesignService.find( ad );
        assertNotNull( ad );
        ad = arrayDesignService.thaw( ad );
        CompositeSequence cs = ad.getCompositeSequences().iterator().next();

        assertNotNull( cs.getId() );
        assertNotNull( cs.getArrayDesign().getId() );

    }

    @Test
    public void testCountAll() {
        ad = persisterHelper.persist( ad );
        long count = arrayDesignService.countAll();
        assertTrue( count > 0 );
    }

    @Test
    public void testDelete() {

        ad = persisterHelper.persist( ad );

        Collection<CompositeSequence> seqs = ad.getCompositeSequences();
        Collection<Long> seqIds = new ArrayList<>();
        for ( CompositeSequence seq : seqs ) {
            if ( seq.getId() == null ) {
                continue; // why?
            }
            seqIds.add( seq.getId() );
        }

        // just a wrinkle to this test -- ensure ACLs are there
        securityService.isPublic( ad );

        arrayDesignService.remove( ad );

        ad = null;

        for ( Long id : seqIds ) {
            try {
                CompositeSequence cs = compositeSequenceService.load( id );
                if ( cs != null )
                    fail( cs + " was still in the system" );
            } catch ( UnresolvableObjectException e ) {
                // ok
            }
        }

    }

    @Test
    public void testFindWithExternalReference() {
        ad = ArrayDesign.Factory.newInstance();
        String name = RandomStringUtils.insecure().nextAlphabetic( 20 ) + "_arraydesign";
        ad.setName( name );
        ad.setShortName( name );
        ad.setPrimaryTaxon( this.getTaxon( "mouse" ) );

        String gplToFind = this.getGpl();

        this.assignExternalReference( ad, gplToFind );
        this.assignExternalReference( ad, this.getGpl() );
        ad = persisterHelper.persist( ad );

        ArrayDesign toFind = ArrayDesign.Factory.newInstance();
        toFind.setPrimaryTaxon( this.getTaxon( "mouse" ) );

        // artificial, wouldn't normally have multiple GEO acc
        this.assignExternalReference( toFind, this.getGpl() );
        this.assignExternalReference( toFind, this.getGpl() );
        this.assignExternalReference( toFind, gplToFind );
        ArrayDesign found = arrayDesignService.find( toFind );

        assertNotNull( found );

        arrayDesignService.remove( ad );
    }

    @Test
    public void testFindWithExternalReferenceNotFound() {
        ad = ArrayDesign.Factory.newInstance();
        this.assignExternalReference( ad, this.getGpl() );
        this.assignExternalReference( ad, this.getGpl() );
        String name = RandomStringUtils.insecure().nextAlphabetic( 20 ) + "_arraydesign";
        ad.setName( name );
        ad.setShortName( name );
        ad.setPrimaryTaxon( this.getTaxon( "mouse" ) );
        ad = persisterHelper.persist( ad );

        ArrayDesign toFind = ArrayDesign.Factory.newInstance();
        toFind.setPrimaryTaxon( this.getTaxon( "mouse" ) );

        // artificial, wouldn't normally have multiple GEO acc
        this.assignExternalReference( toFind, this.getGpl() );
        this.assignExternalReference( toFind, this.getGpl() );
        ArrayDesign found = arrayDesignService.find( toFind );

        assertNull( found );

        arrayDesignService.remove( ad );
    }

    @Test
    public void testGetExpressionExperimentsById() {
        ad = persisterHelper.persist( ad );
        Collection<ExpressionExperiment> ee = arrayDesignService.getExpressionExperiments( ad );
        assertNotNull( ee );
    }

    /**
     * Test retrieving multiple taxa for an arraydesign where hibernate query is not restricted to return just 1 taxon.
     */
    @Test
    public void testGetTaxaFromBioSequencesMultipleTaxonForArray() {
        String taxonName2 = "Fish_" + RandomStringUtils.insecure().nextAlphabetic( 4 );

        Taxon secondTaxon = Taxon.Factory.newInstance();
        secondTaxon.setScientificName( taxonName2 );
        secondTaxon.setNcbiId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 5 ) ) );
        secondTaxon.setIsGenesUsable( true );

        for ( int i = 0; i < 3; i++ ) {

            CompositeSequence c1 = CompositeSequence.Factory.newInstance();
            c1.setName( RandomStringUtils.insecure().nextAlphabetic( 20 ) );
            BioSequence bs = BioSequence.Factory.newInstance( secondTaxon );
            bs.setName( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
            bs.setSequence( RandomStringUtils.insecure().next( 40, "ATCG" ) );

            c1.setBiologicalCharacteristic( bs );

            c1.setArrayDesign( ad );
            ad.getCompositeSequences().add( c1 );
        }

        ad = persisterHelper.persist( ad );

        Collection<Taxon> taxa = arrayDesignService.getTaxaFromBioSequences( ad );
        assertEquals( 2, taxa.size() );

        Collection<String> list = new ArrayList<>();
        for ( Taxon taxon : taxa ) {
            list.add( taxon.getScientificName() );
        }
        assertTrue( "Should have found " + taxonName2, list.contains( taxonName2 ) );
        assertTrue( "Should have found " + ArrayDesignServiceTest.DEFAULT_TAXON,
                list.contains( ArrayDesignServiceTest.DEFAULT_TAXON ) );

    }

    /*
     * Test retrieving one taxa for an arraydesign where hibernate query is not restricted to return just 1 taxon.
     */
    @Test
    public void testGetTaxaFromBioSequencesOneTaxonForArray() {
        ad = persisterHelper.persist( ad );
        Collection<Taxon> taxa = arrayDesignService.getTaxaFromBioSequences( ad );
        assertEquals( 1, taxa.size() );
        Taxon tax = taxa.iterator().next();
        assertEquals( ArrayDesignServiceTest.DEFAULT_TAXON, tax.getScientificName() );

    }

    /*
     * Test retrieving one taxa for an arraydesign where hibernate query is restricted to return just 1 taxon.
     */
    @Test
    public void testGetTaxon() {
        ad = persisterHelper.persist( ad );
        Taxon tax = arrayDesignService.getTaxaFromBioSequences( ad ).iterator().next();
        assertEquals( ArrayDesignServiceTest.DEFAULT_TAXON, tax.getScientificName() );
    }

    @Test
    public void testLoadAllValueObjects() {
        Collection<ArrayDesignValueObject> vos = arrayDesignService.loadAllValueObjects();
        assertNotNull( vos );
    }

    /**
     * Test to ensure that if one taxon is present on an array only 1 string is returned with taxon name
     */
    @Test
    public void testLoadAllValueObjectsOneTaxon() {
        ad = persisterHelper.persist( ad );

        Collection<Long> ids = new HashSet<>();
        ids.add( ad.getId() );
        Collection<ArrayDesignValueObject> vos = arrayDesignService.loadValueObjectsByIds( ids );
        assertNotNull( vos );
        assertEquals( 1, vos.size() );
        String taxon = vos.iterator().next().getTaxon();

        assertEquals( "mouse", taxon );

    }

    @Test
    public void testLoadCompositeSequences() {
        ad = persisterHelper.persist( ad );
        Collection<CompositeSequence> actualValue = arrayDesignService.getCompositeSequences( ad );
        assertEquals( 3, actualValue.size() );
    }

    @Test
    public void testCountBioSequencesById() {
        ad = persisterHelper.persist( ad );
        Long num = arrayDesignService.countBioSequences( ad );
        assertNotNull( num );
    }

    @Test
    public void testCountBlatResultsById() {
        ad = persisterHelper.persist( ad );
        Long num = arrayDesignService.countBlatResults( ad );
        assertNotNull( num );
    }

    /*
     * Test method for
     * 'ArrayDesignServiceImpl.numCompositeSequences(ArrayDesign)'
     */
    @Test
    public void testNumCompositeSequencesArrayDesign() {
        ad = persisterHelper.persist( ad );
        Long actualValue = arrayDesignService.countCompositeSequences( ad );
        Long expectedValue = 3L;
        assertEquals( expectedValue, actualValue );
    }

    @Test
    public void testCountGenes() {
        ad = persisterHelper.persist( ad );
        assertEquals( 0, arrayDesignService.countGenes( ad, true ) );
        assertEquals( 0, arrayDesignService.countGenes( ad, false ) );
        assertEquals( 0, arrayDesignService.countCompositeSequencesWithGenes( ad, true ) );
        assertEquals( 0, arrayDesignService.countCompositeSequencesWithGenes( ad, false ) );
        assertEquals( 0, arrayDesignService.countCompositeSequencesWithGenes( true ) );
        assertEquals( 0, arrayDesignService.countCompositeSequencesWithGenes( false ) );
    }

    @Test
    public void testThaw() {
        ad = super.getTestPersistentArrayDesign( 5, true );

        assertNotNull( ad.getId() );
        ad = arrayDesignService.load( ad.getId() );
        assertNotNull( ad );
        ad = arrayDesignService.thaw( ad );

        // make sure we can do this...
        //noinspection ResultOfMethodCallIgnored
        ad.getPrimaryTaxon().equals( this.taxonService.load( 1L ) );

        auditTrailService.addUpdateEvent( ad, "testing" );

        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            //noinspection ResultOfMethodCallIgnored
            cs.getBiologicalCharacteristic().getName();
        }
    }

    @Test
    public void testUpdateSubsumingStatus() {
        ArrayDesign subsumer = this.getTestPersistentArrayDesign( 10, false );

        ArrayDesign subsumee = this.getTestPersistentArrayDesign( 5, false );

        boolean actualValue = arrayDesignService.updateSubsumingStatus( subsumer, subsumee );
        assertFalse( actualValue );
        actualValue = arrayDesignService.updateSubsumingStatus( subsumee, subsumer );
        assertFalse( actualValue );
    }

    @Test
    public void testUpdateSubsumingStatusTrue() {
        ad = ArrayDesign.Factory.newInstance();
        ad.setName( "subsuming_arraydesign" );

        // Create the composite Sequences
        CompositeSequence c1 = CompositeSequence.Factory.newInstance();
        c1.setName( "bar" );

        Taxon tax = this.getTaxon( "mouse" );

        BioSequence bs = BioSequence.Factory.newInstance( tax );
        bs.setName( "fred" );
        bs.setSequence( "CG" );
        bs.setTaxon( tax );
        c1.setBiologicalCharacteristic( bs );
        ad.getCompositeSequences().add( c1 );

        CompositeSequence c3 = CompositeSequence.Factory.newInstance();
        c3.setName( "foo" );
        BioSequence bsb = BioSequence.Factory.newInstance( tax );
        bsb.setName( "barney" );
        bsb.setSequence( "CAAAAG" );
        bsb.setTaxon( tax );
        c3.setBiologicalCharacteristic( bsb );
        ad.getCompositeSequences().add( c3 );

        ad.setPrimaryTaxon( tax );

        ad = persisterHelper.persist( ad );
        ad = arrayDesignService.thaw( ad );

        ArrayDesign subsumedArrayDesign = ArrayDesign.Factory.newInstance();
        subsumedArrayDesign.setName( "subsumed_arraydesign" );
        subsumedArrayDesign.setPrimaryTaxon( tax );

        // Create the composite Sequences
        CompositeSequence c2 = CompositeSequence.Factory.newInstance();
        c2.setName( "bar" ); // same as one on other AD.
        c2.setBiologicalCharacteristic( bs ); // same as one on other AD.
        subsumedArrayDesign.getCompositeSequences().add( c2 );
        c2.setArrayDesign( subsumedArrayDesign );

        subsumedArrayDesign = persisterHelper.persist( subsumedArrayDesign );
        subsumedArrayDesign = arrayDesignService.thaw( subsumedArrayDesign );
        // flushAndClearSession();

        boolean actualValue = arrayDesignService.updateSubsumingStatus( ad, subsumedArrayDesign );
        assertTrue( actualValue );

        actualValue = arrayDesignService.updateSubsumingStatus( subsumedArrayDesign, ad );
        assertFalse( actualValue );
    }

    private void assignExternalReference( ArrayDesign toFind, String accession ) {
        ExternalDatabase geo = externalDatabaseService.findByName( ExternalDatabases.GEO );
        assert geo != null;

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setExternalDatabase( geo );

        de.setAccession( accession );

        toFind.getExternalReferences().add( de );
    }

    private String getGpl() {
        return "GPL" + RandomStringUtils.insecure().nextNumeric( 4 );
    }

}
