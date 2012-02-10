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
package ubic.gemma.loader.expression.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
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
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.ontology.providers.MgedOntologyService;
import ubic.gemma.security.authorization.acl.AclTestUtils;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class ExperimentalDesignImporterTestB extends BaseSpringContextTest {

    ExpressionExperiment ee;

    @Autowired
    MgedOntologyService mos;

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    ExperimentalFactorService experimentalFactorService;

    @Autowired
    SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    BioMaterialService bioMaterialService;

    @Autowired
    ExperimentalDesignService experimentalDesignService;

    @Autowired
    AclTestUtils aclTestUtils;

    @After
    public void tearDown() {
        if ( ee != null ) {
            ee = eeService.load( ee.getId() );
            eeService.delete( ee );
        }
    }

    @Before
    public void setup() throws Exception {

        super.executeSqlScript( "/script/sql/add-fish-taxa.sql", false );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/expression/head.Gill2007gemmaExpressionData.txt" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        if ( !mos.isOntologyLoaded() ) {
            mos.startInitializationThread( true );
            while ( !mos.isOntologyLoaded() ) {
                Thread.sleep( 1000 );
                log.info( "Waiting for mgedontology to load" );
            }
        }

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
        ad.setShortName( randomName() );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        ad.setName( "foobly foo" );
        ad.setPrimaryTaxon( salmon );

        metaData.getArrayDesigns().add( ad );

        ee = simpleExpressionDataLoaderService.create( metaData, data );

        // eeService.thawLite( ee );
    }

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.simple.ExperimentalDesignImporterImpl#parse(java.io.InputStream)} .
     */
    @Test
    public final void testParseLoadDelete() throws Exception {

        InputStream is = this.getClass().getResourceAsStream(
                "/data/loader/expression/gill2007temperatureGemmaAnnotationData.txt" );

        experimentalDesignImporter.importDesign( ee, is, false );

        Collection<BioMaterial> bms = new HashSet<BioMaterial>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                bms.add( bm );
            }
        }

        checkResults( bms );

        this.aclTestUtils.checkEEAcls( ee );

        ee = this.expressionExperimentService.load( ee.getId() );
        ee = this.expressionExperimentService.thawLite( ee );
        int s = ee.getExperimentalDesign().getExperimentalFactors().size();
        ExperimentalFactor toDelete = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();

        experimentalFactorService.delete( toDelete );

        ee = this.expressionExperimentService.load( ee.getId() );
        ee = this.expressionExperimentService.thawLite( ee );

        assertEquals( s - 1, ee.getExperimentalDesign().getExperimentalFactors().size() );

        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                for ( FactorValue fv : bm.getFactorValues() ) {
                    assertTrue( !fv.getExperimentalFactor().equals( toDelete ) );
                }
            }
        }

    }

    /**
     * @param bms
     */
    private void checkResults( Collection<BioMaterial> bms ) {
        // check.
        assertEquals( 25, ee.getExperimentalDesign().getExperimentalFactors().size() );

        Collection<Long> seenFactorValueIds = new HashSet<Long>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {

            if ( ef.getName().equals( "Temperature treatment" ) ) {
                assertEquals( 3, ef.getFactorValues().size() );
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
                seenFactorValueIds.add( fv.getId() );
            }
        }

    }

}
