/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.analysis.preprocess;

import gemma.gsec.SecurityService;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;

/**
 *
 *
 * @author paul
 */
public class SplitExperimentTest extends BaseSpringContextTest {

    @Autowired
    private SplitExperimentService splitService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private PreprocessorService preprocessor;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private SecurityService securityService;

    private Collection<ExpressionExperiment> ees;

    @Test
    @Category(SlowTest.class)
    public void testSplitGSE17183ByOrganismPart() throws Exception {

        String geoId = "GSE17183";

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/preprocess" ) ) );

        try {
            //noinspection unchecked
            ees = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad( geoId, false, false, false );
        } catch ( AlreadyExistsInSystemException e ) {
            //noinspection unchecked
            ees = ( Collection<ExpressionExperiment> ) e.getData();
            assumeNoException( e );
        }

        ExpressionExperiment ee = ees.iterator().next();
        assertNotNull( ee );

        ee = eeService.thaw( ee );

        securityService.makePublic( ee );

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/analysis/preprocess/2877_GSE17183_expdesign.data.txt" ) ) {
            assertNotNull( is );
            experimentalDesignImporter.importDesign( ee, is );
        }

        preprocessor.process( ee ); // to mimic real life better

        ExperimentalFactor splitOn = null;
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getName().toLowerCase().startsWith( "organism.part" ) ) {
                splitOn = ef;
            }
        }

        assertNotNull( splitOn );

        ExpressionExperimentSet results = splitService.split( ee, splitOn, true );
        assertEquals( splitOn.getFactorValues().size(), results.getExperiments().size() );

        for ( BioAssaySet b : results.getExperiments() ) {
            ExpressionExperiment e = eeService.thaw( ( ExpressionExperiment ) b );

            Collection<RawExpressionDataVector> rvs = e.getRawExpressionDataVectors();
            assertEquals( 100, rvs.size() );

            Collection<ProcessedExpressionDataVector> pvs = e.getProcessedExpressionDataVectors();
            assertEquals( 100, pvs.size() );

            RawExpressionDataVector rv = rvs.iterator().next();
            assertTrue( rv.getQuantitationType().getIsPreferred() );
            assertEquals( 2, e.getOtherParts().size() );

        }
    }

    @Test
    @Category(SlowTest.class)
    public void testSplitGSE123753ByCollectionOfMaterial() throws Exception {

        String geoId = "GSE123753";

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/preprocess" ) ) );

        try {
            //noinspection unchecked
            ees = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad( geoId, false, false, false );
        } catch ( AlreadyExistsInSystemException e ) {
            //noinspection unchecked
            ees = ( ( Collection<ExpressionExperiment> ) e.getData() );
            assumeNoException( e );
        }

        ExpressionExperiment ee = ees.iterator().next();
        assertNotNull( ee );

        ee = eeService.thaw( ee );

        securityService.makePublic( ee );

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/analysis/preprocess/17525_GSE123753_expdesign.data.txt" ) ) {
            assertNotNull( is );
            experimentalDesignImporter.importDesign( ee, is );
        }

        // we can't really process the data since there are no attached datasets to the GEO series

        ExperimentalFactor splitOn = null;
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( ef.getName().toLowerCase().startsWith( "collection.of.material" ) ) {
                splitOn = ef;
            }
        }

        assertNotNull( splitOn );

        ExpressionExperimentSet results = splitService.split( ee, splitOn, false );
        assertEquals( splitOn.getFactorValues().size(), results.getExperiments().size() );
    }

    @After
    public void teardown() throws Exception {
        if ( ees != null ) {
            eeService.remove( ees );
        }
    }
}
