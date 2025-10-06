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
package ubic.gemma.core.loader.expression.simple;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimplePlatformMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleQuantitationTypeMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleTaxonMetadata;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * @author Paul
 */
@Category(SlowTest.class)
public class ExperimentalDesignImporterTest extends BaseSpringContextTest {

    private final String adName = RandomStringUtils.insecure().nextAlphabetic( 10 );
    private ExpressionExperiment ee;
    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @Autowired
    private AclTestUtils aclTestUtils;

    static void assertFv( FactorValue fv ) {
        if ( fv.getCharacteristics().size() > 0 ) {
            Characteristic c = fv.getCharacteristics().iterator().next();
            assertNotNull( c.getValue() );
            assertNotNull( c.getCategoryUri() );
        } else {
            assertNotNull( fv.getValue() + " should have a measurement or a characteristic", fv.getMeasurement() );
        }
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            eeService.remove( ee );
        }
    }

    @Before
    public void setUp() throws Exception {
        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();

        String eeShortName = RandomStringUtils.insecure().nextAlphabetic( 11 );
        metaData.setShortName( eeShortName );
        metaData.setName( "foo" );
        metaData.setDescription( "bar" );
        metaData.setTaxon( SimpleTaxonMetadata.forName( "human" ) );
        SimpleQuantitationTypeMetadata qtMetadata = new SimpleQuantitationTypeMetadata();
        qtMetadata.setName( "rma" );
        qtMetadata.setType( StandardQuantitationType.AMOUNT );
        qtMetadata.setScale( ScaleType.LOG2 );
        qtMetadata.setIsRatio( false );
        metaData.setQuantitationType( qtMetadata );

        SimplePlatformMetadata ad = new SimplePlatformMetadata();
        ad.setShortName( adName );
        ad.setName( "foobly foo" );
        ad.setTechnologyType( TechnologyType.ONECOLOR );

        metaData.getArrayDesigns().add( ad );
        DoubleMatrix<String, String> matrix;
        try ( InputStream data = this.getClass()
                .getResourceAsStream( "/data/loader/expression/experimentalDesignTestData.txt" ) ) {
            matrix = new DoubleMatrixReader().read( data );
        }
        ee = simpleExpressionDataLoaderService.create( metaData, matrix );
        ee = this.eeService.thawLite( ee );
    }

    @Test
    public final void testParse() throws Exception {
        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/experimentalDesignTest.txt" ) ) {

            experimentalDesignImporter.importDesign( ee, is );
        }
        ee = this.eeService.thawLite( ee );
        Collection<BioMaterial> bms = new HashSet<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            bms.add( bm );
        }
        this.checkResults( bms );
        this.aclTestUtils.checkEEAcls( ee );
    }

    @Test
    public final void testParseDryRun() throws Exception {

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/experimentalDesignTest.txt" ) ) {

            experimentalDesignImporter.importDesign( ee, is );
        }

        ee = this.eeService.thawLite( ee );
        assertEquals( 4, ee.getExperimentalDesign().getExperimentalFactors().size() );

    }

    @Test(expected = Exception.class)
    public final void testParseFailedDryRun() throws Exception {

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/experimentalDesignTestBad.txt" ) ) {

            experimentalDesignImporter.importDesign( ee, is );
            fail( "Should have gotten an Exception" );

        }

    }

    /*
     * test case where the design file has extra information not relevant to the current samples.
     */
    @Test
    public final void testParseWhereExtraValue() throws Exception {

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/experimentalDesignTestExtra.txt" ) ) {

            experimentalDesignImporter.importDesign( ee, is );
        }

        ee = this.eeService.thawLite( ee );

        Collection<BioMaterial> bms = new HashSet<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            bms.add( bm );
        }
        this.aclTestUtils.checkEEAcls( ee );

        this.checkResults( bms );
    }

    private void checkResults( Collection<BioMaterial> bms ) {
        // check.
        assertEquals( 4, ee.getExperimentalDesign().getExperimentalFactors().size() );

        boolean foundpmi = false;
        Collection<Long> seenFactorValueIds = new HashSet<>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {

            if ( ef.getName().equals( "Profile" ) ) {
                assertEquals( FactorType.CATEGORICAL, ef.getType() );
                assertEquals( 2, ef.getFactorValues().size() );
            } else if ( ef.getName().equals( "PMI (h)" ) ) {
                assertEquals( FactorType.CONTINUOUS, ef.getType() );
                assertEquals( 8, ef.getFactorValues().size() );
            }

            for ( FactorValue fv : ef.getFactorValues() ) {
                ExperimentalDesignImporterTest.assertFv( fv );

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
