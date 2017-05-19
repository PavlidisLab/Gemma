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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.expression.experiment.service.ExperimentalDesignService;
import ubic.gemma.core.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;

/**
 * @author paul
 * @version $Id$
 */
public class ExperimentalDesignImporterTestC extends AbstractGeoServiceTest {

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
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AclTestUtils aclTestUtils;

    @After
    public void tearDown() {
        if ( ee != null ) {
            ee = eeService.load( ee.getId() );
            eeService.delete( ee );
        }
    }

    @Before
    public void setup() throws Exception {

        /*
         * Have to add ssal for this platform.
         */

        Taxon salmon = taxonService.findByScientificName( "atlantic salmon" );

        // if ( salmon == null ) {
        super.executeSqlScript( "/script/sql/add-fish-taxa.sql", false );
        salmon = taxonService.findByCommonName( "atlantic salmon" );
        // }

        assertNotNull( salmon );

        /*
         * Load the array design (platform).
         */
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                getTestFileBasePath( "designLoadTests" ) ) );
        geoService.fetchAndLoad( "GPL2899", true, true, false, false );
        ArrayDesign ad = arrayDesignService.findByShortName( "GPL2899" );

        assertNotNull( ad );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        metaData.setShortName( RandomStringUtils.randomAlphabetic( 10 ) );
        metaData.setDescription( "bar" );
        metaData.setIsRatio( false );
        metaData.setTaxon( salmon );
        metaData.setQuantitationTypeName( "value" );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );

        metaData.getArrayDesigns().add( ad );

        try (InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/designLoadTests/expressionDataBrain2003TestFile.txt" );) {

            ee = simpleExpressionDataLoaderService.create( metaData, data );
        }
        // eeService.thawLite( ee );
    }

    /**
     * Test method for
     * {@link ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporterImpl#parse(java.io.InputStream)} .
     */
    @Test
    public final void testUploadBadDesign() throws Exception {

        /*
         * The following file has a bug in it. It should fail.
         */
        try (InputStream is = this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/designLoadTests/annotationLoadFileBrain2003FirstBadFile.txt" );) {

            try {
                experimentalDesignImporter.importDesign( ee, is, true ); // dry run, should fail.
                fail( "Should have gotten an error when loading a bad file" );
            } catch ( IOException ok ) {
                // ok
            }
            is.close();
        }
        /*
         * make sure we didn't load anything
         */
        ee = this.expressionExperimentService.load( ee.getId() );
        ee = this.expressionExperimentService.thawLite( ee );
        assertEquals( 0, ee.getExperimentalDesign().getExperimentalFactors().size() );

        /*
         * Now try the good one.
         */
        try (InputStream is = this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/designLoadTests/annotationLoadFileBrain2003SecondGoodFile.txt" );) {

            experimentalDesignImporter.importDesign( ee, is, true ); // dry run, should pass
        }

        /*
         * Reopen the file.
         */
        try (InputStream is = this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/designLoadTests/annotationLoadFileBrain2003SecondGoodFile.txt" );) {
            experimentalDesignImporter.importDesign( ee, is, false ); // not a dry run, should pass.
        }

        ee = this.expressionExperimentService.load( ee.getId() );
        this.aclTestUtils.checkEEAcls( ee );
        ee = this.expressionExperimentService.thawLite( ee );

        assertEquals( 3, ee.getExperimentalDesign().getExperimentalFactors().size() );

        Collection<BioMaterial> bms = new HashSet<BioMaterial>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            bms.add( bm );

        }

        checkResults( bms );

        int s = ee.getExperimentalDesign().getExperimentalFactors().size();
        ExperimentalFactor toDelete = ee.getExperimentalDesign().getExperimentalFactors().iterator().next();

        /*
         * Test of deleting a factor. FIXME: this should be a single service call, However, it's not that easy to do. If
         * you run this in a single transaction, you invariably get session errors. See {@link
         * ExperimentalDesignController}
         */

        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            boolean removed = false;
            for ( Iterator<FactorValue> fIt = bm.getFactorValues().iterator(); fIt.hasNext(); ) {
                if ( fIt.next().getExperimentalFactor().equals( toDelete ) ) {
                    fIt.remove();
                    removed = true;
                }
            }
            if ( removed ) {
                bioMaterialService.update( bm );
            }
        }
        ee.getExperimentalDesign().getExperimentalFactors().remove( toDelete );
        experimentalFactorService.delete( toDelete );
        experimentalDesignService.update( ee.getExperimentalDesign() );

        assertEquals( s - 1, ee.getExperimentalDesign().getExperimentalFactors().size() );

    }

    /**
     * @param bms
     */
    private void checkResults( Collection<BioMaterial> bms ) {
        // check.
        assertEquals( 3, ee.getExperimentalDesign().getExperimentalFactors().size() );

        Collection<Long> seenFactorValueIds = new HashSet<Long>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {

            if ( ef.getName().equals( "SamplingLocation" ) ) {
                assertEquals( 6, ef.getFactorValues().size() );
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