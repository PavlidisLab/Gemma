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
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * Test for import that results in multiple factor values for the same factor on a single biomaterial.
 *
 * @author paul
 */
public class ExperimentalDesignImportDuplicateValueTest extends BaseSpringContextTest {

    private ExpressionExperiment ee;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private SimpleExpressionDataLoaderService s;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Before
    public void setUp() throws Exception {
        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();

        metaData.setShortName( randomName() );
        metaData.setDescription( "bar" );
        metaData.setTaxon( SimpleTaxonMetadata.forName( "human" ) );
        SimpleQuantitationTypeMetadata qtMetadata = new SimpleQuantitationTypeMetadata();
        qtMetadata.setName( "rma" );
        qtMetadata.setScale( ScaleType.LOG2 );
        qtMetadata.setType( StandardQuantitationType.AMOUNT );
        qtMetadata.setIsRatio( false );
        metaData.setQuantitationType( qtMetadata );

        SimplePlatformMetadata ad = new SimplePlatformMetadata();
        ad.setShortName( "gfoobly_" + randomName() );
        ad.setName( "foobly doo loo" );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        metaData.getArrayDesigns().add( ad );

        DoubleMatrix<String, String> matrix;
        try ( InputStream data = this.getClass()
                .getResourceAsStream( "/data/loader/expression/expdesign.import.testfull.data.txt" ) ) {
            matrix = new DoubleMatrixReader().read( data );
        }

        ee = s.create( metaData, matrix );

        ee = this.eeService.thawLite( ee );
    }

    /*
     * Note that this test will fail if you run it again on a dirty DB. Sorry!
     */
    @Test
    @Category(SlowTest.class)
    public final void testParse() throws Exception {

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/expdesign.import.testfull.txt" ) ) {
            experimentalDesignImporter.importDesign( ee, is );
        }

        Collection<BioMaterial> bms = new HashSet<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            bms.add( bm );
        }

        checkResults( bms );
    }

    private void checkResults( Collection<BioMaterial> bms ) {

        assertEquals( 17, ee.getExperimentalDesign().getExperimentalFactors().size() );

        for ( BioMaterial bm : bms ) {
            Collection<ExperimentalFactor> seenExperimentalFactors = new HashSet<>();
            for ( FactorValue fv : bm.getFactorValues() ) {

                if ( seenExperimentalFactors.contains( fv.getExperimentalFactor() ) ) {
                    for ( FactorValue ff : bm.getFactorValues() ) {
                        assertNotNull( ff.getId() );
                        if ( ff.getExperimentalFactor().equals( fv.getExperimentalFactor() ) ) {
                            log.info( bm + " : " + ff );
                        }
                    }

                    fail( fv.getExperimentalFactor() + " has more than one value for " + bm );
                }
                seenExperimentalFactors.add( fv.getExperimentalFactor() );
            }

        }
    }

}
