/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.loader.expression.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.AclTestUtils;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author Paul
 * @version $Id$
 */
public class ExperimentalDesignImporterTest extends BaseSpringContextTest {

    private ExpressionExperiment ee;

    private String adName = RandomStringUtils.randomAlphabetic( 10 );

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @Autowired
    private AclTestUtils aclTestUtils;

    @After
    public void tearDown() {
        if ( ee != null ) {
            eeService.delete( ee );
        }
    }

    @Before
    public void setup() throws Exception {

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/expression/experimentalDesignTestData.txt" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        Taxon human = taxonService.findByCommonName( "human" );

        String eeShortName = RandomStringUtils.randomAlphabetic( 10 );
        metaData.setShortName( eeShortName );
        metaData.setDescription( "bar" );
        metaData.setIsRatio( false );
        metaData.setTaxon( human );
        metaData.setQuantitationTypeName( "rma" );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();

        ad.setShortName( adName );
        ad.setName( "foobly foo" );
        ad.setPrimaryTaxon( human );
        ad.setTechnologyType( TechnologyType.ONECOLOR );

        metaData.getArrayDesigns().add( ad );

        ee = simpleExpressionDataLoaderService.create( metaData, data );

        ee = eeService.thawLite( ee );
    }

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.simple.ExperimentalDesignImporterImpl#parse(java.io.InputStream)} .
     */
    @Test
    public final void testParse() throws Exception {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/experimentalDesignTest.txt" );

        experimentalDesignImporter.importDesign( ee, is, false );

        ee = eeService.thawLite( ee );

        Collection<BioMaterial> bms = new HashSet<BioMaterial>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            bms.add( bm );
        }

        checkResults( bms );

        this.aclTestUtils.checkEEAcls( ee );

    }

    @Test
    public final void testParseDryRun() throws Exception {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/experimentalDesignTest.txt" );

        experimentalDesignImporter.importDesign( ee, is, true );

        ee = eeService.thawLite( ee );
        assertEquals( 4, ee.getExperimentalDesign().getExperimentalFactors().size() );

    }

    @Test
    public final void testParseFailedDryRun() {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/experimentalDesignTestBad.txt" );

        try {
            experimentalDesignImporter.importDesign( ee, is, true );
            fail( "Should have gotten an Exception" );
        } catch ( Exception e ) {
            // ok
        }

    }

    /**
     * test case where the design file has extra information not relevant to the current samples.
     * 
     * @throws Exception
     */
    @Test
    public final void testParseWhereExtraValue() throws Exception {

        InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/experimentalDesignTestExtra.txt" );

        experimentalDesignImporter.importDesign( ee, is, false );
        ee = eeService.thawLite( ee );

        Collection<BioMaterial> bms = new HashSet<BioMaterial>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            bms.add( bm );
        }
        this.aclTestUtils.checkEEAcls( ee );

        checkResults( bms );
    }

    /**
     * @param bms
     */
    private void checkResults( Collection<BioMaterial> bms ) {
        // check.
        assertEquals( 4, ee.getExperimentalDesign().getExperimentalFactors().size() );

        boolean foundpmi = false;
        Collection<Long> seenFactorValueIds = new HashSet<Long>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {

            if ( ef.getName().equals( "Profile" ) ) {
                assertEquals( FactorType.CATEGORICAL, ef.getType() );
                assertEquals( 2, ef.getFactorValues().size() );
            } else if ( ef.getName().equals( "PMI (h)" ) ) {
                assertEquals( FactorType.CONTINUOUS, ef.getType() );
                assertEquals( 8, ef.getFactorValues().size() );
            }

            for ( FactorValue fv : ef.getFactorValues() ) {
                if ( fv.getCharacteristics().size() > 0 ) {
                    VocabCharacteristic c = ( VocabCharacteristic ) fv.getCharacteristics().iterator().next();
                    assertNotNull( c.getValue() );
                    assertNotNull( c.getCategoryUri() );
                } else {
                    assertNotNull( fv.getValue() + " should have a measurement or a characteristic",
                            fv.getMeasurement() );
                }

                if ( fv.getExperimentalFactor().getName().equals( "PMI (h)" ) ) {
                    foundpmi = true;
                    assertNotNull( fv.getMeasurement() ); // continuous
                }

                seenFactorValueIds.add( fv.getId() );
            }
        }

        assertTrue( foundpmi );

        for ( BioMaterial bm : bms ) {
            assertEquals( 4, bm.getFactorValues().size() );
            for ( FactorValue fv : bm.getFactorValues() ) {
                assertTrue( seenFactorValueIds.contains( fv.getId() ) );
            }

        }
    }

}
