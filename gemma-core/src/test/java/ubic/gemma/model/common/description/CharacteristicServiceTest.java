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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentImpl;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author luke
 * @version $Id$
 */
public class CharacteristicServiceTest extends BaseSpringContextTest {

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private BioMaterialService bmService;

    @Autowired
    private FactorValueService fvService;

    private ExpressionExperiment ee;
    private Characteristic eeChar1;
    private Characteristic eeChar2;
    private BioMaterial bm;
    private FactorValue fv;
    private boolean setupDone = false;

    /**
     * @exception Exception
     */
    @Before
    public void setup() throws Exception {

        if ( !setupDone ) {
            ee = this.getTestPersistentBasicExpressionExperiment();
            ee.setCharacteristics( getTestPersistentCharacteristics( 2 ) );
            Characteristic[] eeChars = ee.getCharacteristics().toArray( new Characteristic[0] );
            eeChar1 = eeChars[0];
            eeChar2 = eeChars[1];
            eeService.update( ee );

            BioAssay ba = ee.getBioAssays().toArray( new BioAssay[0] )[0];
            bm = ba.getSampleUsed();
            bm.setCharacteristics( getTestPersistentCharacteristics( 1 ) );
            bmService.update( bm );

            for ( ExperimentalFactor ef : testHelper.getExperimentalFactors( ee.getExperimentalDesign() ) ) {
                eeService.addFactor( ee, ef );
            }

            ExperimentalFactor ef = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();

            for ( FactorValue f : testHelper.getFactorValues( ef ) ) {
                eeService.addFactorValue( ee, f );
            }

            fv = ef.getFactorValues().iterator().next();
            fv.setCharacteristics( getTestPersistentCharacteristics( 1 ) );
            fvService.update( fv );
        }

    }

    @Test
    public final void testGetParents() {
        Map<Characteristic, Object> charToParent;
        charToParent = characteristicService.getParents( Arrays.asList( new Characteristic[] { eeChar1 } ) );
        assertEquals( ee, charToParent.get( eeChar1 ) );
        assertEquals( null, charToParent.get( eeChar2 ) );
    }

    @Test
    public final void testGetParentsWithClazzConstraint() {
        Map<Characteristic, Object> charToParent;
        charToParent = characteristicService.getParents(
                Arrays.asList( new Class<?>[] { ExpressionExperimentImpl.class } ),
                Arrays.asList( new Characteristic[] { eeChar1 } ) );
        assertEquals( ee, charToParent.get( eeChar1 ) );
        assertEquals( null, charToParent.get( eeChar2 ) );
    }

    /**
     * @param n
     * @return
     */
    private Collection<Characteristic> getTestPersistentCharacteristics( int n ) {
        Collection<Characteristic> chars = new HashSet<Characteristic>();
        for ( int i = 0; i < n; ++i ) {
            Characteristic c = Characteristic.Factory.newInstance();
            c.setCategory( "test" );
            c.setValue( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            characteristicService.create( c );
            chars.add( c );
        }
        return chars;
    }

    // @SuppressWarnings("unchecked")
    // public void testGetByTaxon() throws Exception {
    // TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
    // ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    //
    // Taxon taxon = taxonService.findByCommonName( "mouse" );
    // Collection<ExpressionExperiment> list = characteristicService.findByTaxon( taxon );
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
