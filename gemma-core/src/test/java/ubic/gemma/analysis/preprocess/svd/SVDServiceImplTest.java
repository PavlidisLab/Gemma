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
package ubic.gemma.analysis.preprocess.svd;

import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * @author paul
 * @version $Id$
 */
public class SVDServiceImplTest extends AbstractGeoServiceTest {

    @Autowired
    SVDService svdService;

    @Autowired
    protected GeoDatasetService geoService;

    @Autowired
    ExpressionExperimentService eeService;

    ExpressionExperiment ee;

    @Autowired
    ProcessedExpressionDataVectorService processedExpressionDataVectorService = null;

    @Test
    public void testsvd() throws Exception {
        ee = eeService.findByShortName( "GSE674" );

        assertNotNull( ee );

        ee = eeService.thaw( ee );
        SVDValueObject svd = svdService.svd( ee );

        assertNotNull( svd.getvMatrix() );
    }

    @Before
    public void setUp() throws Exception {
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "shortTest" ) );
        // also used in the GeoDatasetServiceIntegrationTest.
        try {
            Collection<?> results = geoService.fetchAndLoad( "GDS472", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) e.getData();
        }
        assertNotNull( ee );
        ee = eeService.thaw( ee );
        processedExpressionDataVectorService.createProcessedDataVectors( ee );

    }

    @After
    public void tearDown() {
        if ( ee != null ) {
            try {
                eeService.delete( ee );
            } catch ( Exception e ) {

            }
        }
    }

}
