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
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.expression.AnalysisUtilService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class SVDServiceImplTest extends AbstractGeoServiceTest {

    @Autowired
    private AnalysisUtilService analysisUtilService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService eeService;

    private ExpressionExperiment ee;

    @Autowired
    ProcessedExpressionDataVectorService processedExpressionDataVectorService = null;

    @Test
    public void testsvd() throws Exception {

        geoService
                .setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "shortTest" ) ) );
        // also used in the GeoDatasetServiceIntegrationTest.
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE674", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( Collection<?> ) e.getData() ).iterator().next();
        }
        assertNotNull( ee );
        ee = eeService.thaw( ee );
        processedExpressionDataVectorService.createProcessedDataVectors( ee );

        ee = eeService.findByShortName( "GSE674" );

        assertNotNull( ee );

        SVDValueObject svd = svdService.svd( ee.getId() );

        assertNotNull( svd );

        assertNotNull( svd.getvMatrix() );
        assertEquals( 5, svd.getFactorCorrelations().size() );

        analysisUtilService.deleteOldAnalyses( ee );

    }

    /**
     * See bug 2139; two different sets of bioassays in the data.
     * 
     * @throws Exception
     */
    @Test
    public void testsvdGapped() throws Exception {

        geoService
                .setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "gse482short" ) ) );
        // also used in the GeoDatasetServiceIntegrationTest.
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE482", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            if ( e.getData() instanceof List ) {
                ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                ee = ( ExpressionExperiment ) e.getData();
            }
        }
        assertNotNull( ee );
        ee = eeService.thaw( ee );
        processedExpressionDataVectorService.createProcessedDataVectors( ee );

        ee = eeService.findByShortName( "GSE482" );

        assertNotNull( ee );

        SVDValueObject svd = svdService.svd( ee.getId() );

        assertNotNull( svd );
        assertNotNull( svd.getvMatrix() );

        assertEquals( 10, svd.getBioMaterialIds().length );
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
