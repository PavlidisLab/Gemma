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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import gemma.gsec.SecurityService;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

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

    private ExpressionExperiment ee;

    private Collection<ExpressionExperiment> results;

    @Before
    public void setup() throws Exception, PreprocessingException {
        ee = null;
        results = new HashSet<>();
    }

    @Test
    public void testSplitGSE17183ByOrganismPart() throws Exception, PreprocessingException {

        String geoId = "GSE17183";

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/preprocess" ) ) );

        @SuppressWarnings("unchecked")
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad( geoId, false, false, false );
        ee = ees.iterator().next();

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

        results = splitService.split( ee, splitOn, true );
        assertEquals( splitOn.getFactorValues().size(), results.size() );

        for ( ExpressionExperiment e : results ) {
            e = eeService.thaw( e );

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
    public void testSplitGSE123753ByCollectionOfMaterial() throws Exception {

        String geoId = "GSE123753";

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( FileTools.resourceToPath( "/data/analysis/preprocess" ) ) );

        @SuppressWarnings("unchecked")
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad( geoId, false, false, false );
        ee = ees.iterator().next();

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

        results = splitService.split( ee, splitOn, false );
        assertEquals( splitOn.getFactorValues().size(), results.size() );
    }

    @After
    public void teardown() throws Exception {
        if ( ee != null ) {
            eeService.remove( ee );
        }
        for ( ExpressionExperiment splitEE : results ) {
            eeService.remove( splitEE );
        }
    }

}
