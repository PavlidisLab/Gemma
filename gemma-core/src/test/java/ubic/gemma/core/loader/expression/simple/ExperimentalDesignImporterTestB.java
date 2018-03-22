/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;
import ubic.gemma.core.testing.BaseSpringContextTest;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author paul
 */
public class ExperimentalDesignImporterTestB extends BaseSpringContextTest {

    private ExpressionExperiment ee;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private AclTestUtils aclTestUtils;

    @Before
    public void setup() throws Exception {

        super.executeSqlScript( "/script/sql/add-fish-taxa.sql", false );
        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        Taxon salmon = taxonService.findByCommonName( "salmonid" );

        // doesn't matter what it is for this test, but the test data are from salmon.
        assertNotNull( salmon );

        metaData.setShortName( RandomStringUtils.randomAlphabetic( 10 ) );
        metaData.setDescription( "bar" );
        metaData.setIsRatio( false );
        metaData.setTaxon( salmon );
        metaData.setQuantitationTypeName( "value" );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setShortName( this.randomName() );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        ad.setName( "foobly foo" );
        ad.setPrimaryTaxon( salmon );

        metaData.getArrayDesigns().add( ad );
        try (InputStream data = this.getClass()
                .getResourceAsStream( "/data/loader/expression/head.Gill2007gemmaExpressionData.txt" )) {

            ee = simpleExpressionDataLoaderService.create( metaData, data );

        }
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            ee = eeService.load( ee.getId() );
            eeService.remove( ee );
        }
    }

    @Test
    public final void testParseLoadDelete() throws Exception {

        try (InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/gill2007temperatureGemmaAnnotationData.txt" )) {

            experimentalDesignImporter.importDesign( ee, is );
        }

        this.checkResults();

        this.aclTestUtils.checkEEAcls( ee );

        ee = this.expressionExperimentService.load( ee.getId() );
        ee = expressionExperimentService.thawLite( ee );
        int s = ee.getExperimentalDesign().getExperimentalFactors().size();
        ExperimentalFactor toDelete = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();

        experimentalFactorService.delete( toDelete );

        ee = this.expressionExperimentService.load( ee.getId() );
        ee = expressionExperimentService.thawLite( ee );

        assertEquals( s - 1, ee.getExperimentalDesign().getExperimentalFactors().size() );

        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            for ( FactorValue fv : bm.getFactorValues() ) {
                assertTrue( !fv.getExperimentalFactor().equals( toDelete ) );
            }
        }
    }

    private void checkResults() {
        // check.
        assertEquals( 25, ee.getExperimentalDesign().getExperimentalFactors().size() );

        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {

            if ( ef.getName().equals( "Temperature treatment" ) ) {
                assertEquals( 3, ef.getFactorValues().size() );
            }

            for ( FactorValue fv : ef.getFactorValues() ) {
                ExperimentalDesignImporterTest.assertFv( fv );
            }
        }

    }

}
