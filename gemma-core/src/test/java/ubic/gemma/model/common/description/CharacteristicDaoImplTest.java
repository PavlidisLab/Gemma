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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialDao;
import ubic.gemma.model.expression.biomaterial.BioMaterialImpl;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignDao;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.expression.experiment.ExpressionExperimentImpl;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueDao;
import ubic.gemma.model.expression.experiment.FactorValueImpl;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author luke
 * @version $Id$
 */
public class CharacteristicDaoImplTest extends BaseSpringContextTest {

    CharacteristicDao characteristicDao;
    ExpressionExperimentDao eeDao;
    BioMaterialDao bmDao;
    ExperimentalDesignDao edDao;
    ExperimentalFactorDao efDao;
    FactorValueDao fvDao;
    
    ExpressionExperiment ee;
    Characteristic eeChar1;
    Characteristic eeChar2;
    BioMaterial bm;
    Characteristic bmChar;
    FactorValue fv;
    Characteristic fvChar;

    /**
     * @exception Exception
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        
        ee = this.getTestPersistentCompleteExpressionExperiment();
        ee.setCharacteristics( getTestPersistentCharacteristics( 2 ) );
        Characteristic[] eeChars = ee.getCharacteristics().toArray( new Characteristic[0] );
        eeChar1 = eeChars[0];
        eeChar2 = eeChars[1];
        eeDao.update( ee );
        
        BioAssay ba = ee.getBioAssays().toArray( new BioAssay[0] )[0];
        bm = ba.getSamplesUsed().toArray( new BioMaterial[0] )[0];
        bm.setCharacteristics( getTestPersistentCharacteristics( 1 ) );
        bmChar = bm.getCharacteristics().toArray( new Characteristic[0] )[0];
        bmDao.update( bm );
        
        ExperimentalDesign ed = ee.getExperimentalDesign();
        ed.setExperimentalFactors( testHelper.getExperimentalFactors( ed ) );
        efDao.create( ed.getExperimentalFactors() );
        edDao.update( ed );

        ExperimentalFactor ef = ed.getExperimentalFactors().toArray( new ExperimentalFactor[0] )[0];
        ef.setFactorValues( testHelper.getFactorValues( ef ) );
        fvDao.create( ef.getFactorValues() );
        efDao.update( ef );
        
        fv = ef.getFactorValues().toArray( new FactorValue[0] )[0];
        fv.setCharacteristics( getTestPersistentCharacteristics( 1 ) );
        fvChar = fv.getCharacteristics().toArray( new Characteristic[0] )[0];
        fvDao.update( fv );

//        DatabaseEntry accessionEntry = this.getTestPersistentDatabaseEntry();
//        accession = accessionEntry.getAccession();
//        ed = accessionEntry.getExternalDatabase();
//        ee.setAccession( accessionEntry );
//
//        Contact c = this.getTestPersistentContact();
//        this.contactName = c.getName();
//        ee.setOwner( c );

        // Creating the bibReference association doesn't work like this
        // BibliographicReferenceService bibRefService = ( BibliographicReferenceService ) this
        // .getBean( "bibliographicReferenceService" );
        // primary = BibliographicReference.Factory.newInstance();
        // primary.setPubAccession( accessionEntry );
        // primary.setTitle( "Primary" );
        // primary = bibRefService.create( primary );
        //        
        // ee.setPrimaryPublication( primary );
        //        
        // other = BibliographicReference.Factory.newInstance();
        // other.setPubAccession( accessionEntry );
        // other.setTitle( "other" );
        // other = bibRefService.create( other );
        //        
        // Collection<BibliographicReference> others = new HashSet<BibliographicReference>();
        // others.add( other );
        // ee.setOtherRelevantPublications( others ) ;

//        characteristicDao.update( ee );
//        characteristicDao.thaw( ee );

    }
    
    private Collection<Characteristic> getTestPersistentCharacteristics( int n ) {
        Collection<Characteristic> chars = new HashSet<Characteristic>();
        for ( int i=0; i<n; ++i ) {
            Characteristic c = Characteristic.Factory.newInstance();
            c.setCategory( "test" );
            c.setValue( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            characteristicDao.create( c );
            chars.add( c );
        }
        return chars;
    }
    
    public final void testFindByParentClass() {
        Map<Characteristic, Object> charToParent;
        charToParent = characteristicDao.findByParentClass( ExpressionExperimentImpl.class );
        assertEquals( ee, charToParent.get( eeChar1 ) );
        assertEquals( ee, charToParent.get( eeChar2 ) );
        charToParent = characteristicDao.findByParentClass( BioMaterialImpl.class );
        assertEquals( bm, charToParent.get( bmChar ) );
        charToParent = characteristicDao.findByParentClass( FactorValueImpl.class );
        assertEquals( fv, charToParent.get( fvChar ) );
    }
    
    public final void testGetParents() {
        Map<Characteristic, Object> charToParent;
        charToParent = characteristicDao.getParents( ExpressionExperimentImpl.class, Arrays.asList( new Characteristic[] { eeChar1 } ) );
        assertEquals( ee, charToParent.get( eeChar1 ) );
        assertEquals( null, charToParent.get( eeChar2 ) );
    }

//    public final void testFindByAccession() throws Exception {
//        DatabaseEntry accessionEntry = DatabaseEntry.Factory.newInstance( ed );
//        accessionEntry.setAccession( accession );
//
//        ExpressionExperiment expressionExperiment = characteristicDao.findByAccession( accessionEntry );
//        assertNotNull( expressionExperiment );
//    }
//
//    @SuppressWarnings("unchecked")
//    public final void testGetDesignElementDataVectors() throws Exception {
//        Collection<DesignElement> designElements = new HashSet<DesignElement>();
//        QuantitationType quantitationType = ee.getDesignElementDataVectors().iterator().next().getQuantitationType();
//        Collection<DesignElementDataVector> allv = ee.getDesignElementDataVectors();
//        Iterator<DesignElementDataVector> it = allv.iterator();
//        for ( int i = 0; i < 2; i++ ) {
//            designElements.add( it.next().getDesignElement() );
//        }
//
//        Collection<DesignElementDataVector> vectors = characteristicDao.getDesignElementDataVectors(
//                designElements, quantitationType );
//
//        assertEquals( 2, vectors.size() );
//
//    }
//
//    @SuppressWarnings("unchecked")
//    public final void testGetDesignElementDataVectorsByQt() throws Exception {
//        QuantitationType quantitationType = ee.getDesignElementDataVectors().iterator().next().getQuantitationType();
//        Collection<QuantitationType> quantitationTypes = new HashSet<QuantitationType>();
//        quantitationTypes.add( quantitationType );
//
//        log.info( "***********************" );
//        Collection<DesignElementDataVector> vectors = characteristicDao.getDesignElementDataVectors( ee,
//                quantitationTypes );
//        log.info( "***********************" );
//        assertEquals( 12, vectors.size() );
//
//    }
//
//    @SuppressWarnings("unchecked")
//    public final void testGetSamplingOfVectors() throws Exception {
//        QuantitationType quantitationType = ee.getDesignElementDataVectors().iterator().next().getQuantitationType();
//        Collection<DesignElementDataVector> vectors = characteristicDao
//                .getSamplingOfVectors( quantitationType, 2 );
//
//        assertEquals( 2, vectors.size() );
//
//    }
//
//    @SuppressWarnings("unchecked")
//    public final void testGetQuantitationTypes() throws Exception {
//        Collection<QuantitationType> types = characteristicDao.getQuantitationTypes( ee );
//        assertEquals( 2, types.size() );
//    }
//
//    @SuppressWarnings("unchecked")
//    public final void testGetQuantitationTypesForArrayDesign() throws Exception {
//        ArrayDesign ad = ee.getDesignElementDataVectors().iterator().next().getDesignElement().getArrayDesign();
//        Collection<QuantitationType> types = characteristicDao.getQuantitationTypes( ee, ad );
//        assertEquals( 2, types.size() );
//    }
//
//    @SuppressWarnings("unchecked")
//    public final void testGetPerTaxonCount() throws Exception {
//        Map<String, Long> counts = characteristicDao.getPerTaxonCount();
//        assertNotNull( counts );
//    }
//
//    public final void testLoadAllValueObjects() throws Exception {
//        Collection list = characteristicDao.loadAllValueObjects();
//        assertNotNull( list );
//    }

    // Test needs to be run against the production db. Comment out the onsetup and on tear down before running on
    // production.
    // The test db is just to trivial a db for this test to ever fail.
    // there were issues with loadValueObjects not returning all the specified value objects
    // because of join issues (difference between left join and inner join). Made this test to quickly test if it was
    // working or not.
    // public final void testVerifyLoadValueObjects() throws Exception {
    //               
    // Collection<ExpressionExperiment> eeAll = characteristicDao.loadAll();
    //        
    // Collection<Long> ids = new LinkedHashSet<Long>();
    // for ( ExpressionExperiment ee : eeAll ) {
    // ids.add( ee.getId() );
    // }
    // log.debug( "loadAll: " + ids.toString() );
    //
    // Collection<ExpressionExperimentValueObject> valueObjs = characteristicDao.loadValueObjects( ids );
    //        
    // Collection<Long> idsAfter = new LinkedHashSet<Long>();
    // for (ExpressionExperimentValueObject ee : valueObjs){
    // idsAfter.add( ee.getId());
    // }
    //        
    // log.debug( "loadValueObjects: " + idsAfter.toString() );
    //        
    // Collection<Long> removedIds = new LinkedHashSet<Long>(ids);
    // removedIds.removeAll( idsAfter );
    //        
    // log.debug( "Intersection of EEs: " + removedIds.toString() );
    // assertEquals(idsAfter.size(), ids.size());
    // }

    /**
     * @param characteristicDao the characteristicDao to set
     */
    public void setCharacteristicDao( CharacteristicDao characteristicDao ) {
        this.characteristicDao = characteristicDao;
    }

    /**
     * @param expressionExperimentDao the characteristicDao to set
     */
    public void setExpressionExperimentDao( ExpressionExperimentDao expressionExperimentDao ) {
        this.eeDao = expressionExperimentDao;
    }

    /**
     * @param bmDao the bmDao to set
     */
    public void setBioMaterialDao( BioMaterialDao bmDao ) {
        this.bmDao = bmDao;
    }

    /**
     * @param edDao the edDao to set
     */
    public void setExperimentalDesignDao( ExperimentalDesignDao edDao ) {
        this.edDao = edDao;
    }

    /**
     * @param efDao the efDao to set
     */
    public void setExperimentalFactorDao( ExperimentalFactorDao efDao ) {
        this.efDao = efDao;
    }

    /**
     * @param fvDao the fvDao to set
     */
    public void setFactorValueDao( FactorValueDao fvDao ) {
        this.fvDao = fvDao;
    }

//    @SuppressWarnings("unchecked")
//    public void testGetByTaxon() throws Exception {
//        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
//        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
//                .getBean( "expressionExperimentService" );
//
//        Taxon taxon = taxonService.findByCommonName( "mouse" );
//        Collection<ExpressionExperiment> list = characteristicDao.findByTaxon( taxon );
//        assertNotNull( list );
//        Taxon checkTaxon = eeService.getTaxon( list.iterator().next().getId() );
//        assertEquals( taxon, checkTaxon );
//
//    }

    // Creating test data for this was difficult. Needed to use a current data base for this test to work.
    // public void testFindByGene() throws Exception {
    // GeneService geneS = (GeneService) this.getBean( "geneService" );
    // Collection<Gene> genes = geneS.findByOfficialSymbol( "grin1" );
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    // Collection<Long> results = eeService.findByGene( genes.iterator().next());
    // log.info( results );
    // assertEquals(89, results.size() );
    //        
    // }

    // This test uses the DB
    // public void testFindByBibliographicReference(){
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    // BibliographicReferenceService bibRefService = ( BibliographicReferenceService ) this
    // .getBean( "bibliographicReferenceService" );
    //        
    // BibliographicReference bibRef = bibRefService.load( new Long(111 ));
    //
    // Collection<ExpressionExperiment> foundEEs = eeService.findByBibliographicReference( bibRef );
    // assertEquals(1,foundEEs.size());
    // assertEquals(new Long(8), (Long) foundEEs.iterator().next().getId());
    //       
    // }

}
