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

    static ExpressionExperiment ee;
    static Characteristic eeChar1;
    static Characteristic eeChar2;
    static BioMaterial bm;
    static Characteristic bmChar;
    static FactorValue fv;
    static Characteristic fvChar;
    static boolean setupDone = false;

    /**
     * @exception Exception
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        if ( !setupDone ) {
            ee = this.getTestPersistentBasicExpressionExperiment();
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
        }

    }

    private Collection<Characteristic> getTestPersistentCharacteristics( int n ) {
        Collection<Characteristic> chars = new HashSet<Characteristic>();
        for ( int i = 0; i < n; ++i ) {
            Characteristic c = Characteristic.Factory.newInstance();
            c.setCategory( "test" );
            c.setValue( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            characteristicDao.create( c );
            chars.add( c );
        }
        return chars;
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public final void testGetParents() {
        Map<Characteristic, Object> charToParent;
        charToParent = characteristicDao.getParents( ExpressionExperimentImpl.class, Arrays
                .asList( new Characteristic[] { eeChar1 } ) );
        assertEquals( ee, charToParent.get( eeChar1 ) );
        assertEquals( null, charToParent.get( eeChar2 ) );
    }

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

    // @SuppressWarnings("unchecked")
    // public void testGetByTaxon() throws Exception {
    // TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    //
    // Taxon taxon = taxonService.findByCommonName( "mouse" );
    // Collection<ExpressionExperiment> list = characteristicDao.findByTaxon( taxon );
    // assertNotNull( list );
    // Taxon checkTaxon = eeService.getTaxon( list.iterator().next().getId() );
    // assertEquals( taxon, checkTaxon );
    //
    // }

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
