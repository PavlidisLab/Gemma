/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.expression.diff;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.experiment.ExperimentalDesignUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author paul
 */
public class BaselineDetectionTest extends AbstractGeoServiceTest {

    @Autowired
    protected GeoService geoService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    private ExpressionExperiment ee;

    @Before
    public void setUp() throws Exception {

        String path = FileTools.resourceToPath( "/data/loader/expression/geo/gse18162Short" );
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE18162", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            // OK.
            if ( e.getData() instanceof List ) {
                ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                ee = ( ExpressionExperiment ) e.getData();
            }
        }
        ee = this.eeService.thawLite( ee );
        if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            ee = eeService.load( ee.getId() );
            assertNotNull( ee );
            ee = this.eeService.thawLite( ee );

            try ( InputStream is = this.getClass()
                    .getResourceAsStream( "/data/loader/expression/geo/gse18162Short/design.txt" ) ) {
                experimentalDesignImporter.importDesign( ee, is );
            }
            ee = eeService.load( ee.getId() );
            assertNotNull( ee );
            ee = this.eeService.thawLite( ee );
        }
        // end setup
    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            try {
                eeService.remove( ee );
            } catch ( Exception ignored ) {

            }
        }
    }

    @Test
    @Category(SlowTest.class)
    public void testFetchAndLoadGSE18162() {

        Map<ExperimentalFactor, FactorValue> baselineLevels = BaselineSelection
                .getBaselineLevels( ee.getExperimentalDesign().getExperimentalFactors() );

        assertEquals( 2, baselineLevels.size() ); // the batch DOES get a baseline. IF we change that then we change
        // this test.
        for ( ExperimentalFactor ef : baselineLevels.keySet() ) {
            if ( ef.getName().equals( ExperimentalDesignUtils.BATCH_FACTOR_NAME ) )
                continue;
            FactorValue fv = baselineLevels.get( ef );
            assertEquals( "Control_group", fv.getValue() );
        }

    }
}
