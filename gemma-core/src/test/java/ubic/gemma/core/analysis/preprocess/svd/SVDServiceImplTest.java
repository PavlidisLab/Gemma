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
package ubic.gemma.core.analysis.preprocess.svd;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.expression.AnalysisUtilService;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest5;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author paul
 */
@Category(SlowTest.class)
@ExtendWith(NetworkAvailableExtension.class)
public class SVDServiceImplTest extends AbstractGeoServiceTest5 {

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService = null;
    @Autowired
    private AnalysisUtilService analysisUtilService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private GeoService geoService;
    @Autowired
    private ExpressionExperimentService eeService;
    private ExpressionExperiment ee;

    @Test
    @NetworkAvailable(url = EntrezUtils.ESEARCH)
    public void testSvd() throws Exception {

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "shortTest" ) ) );
        // also used in the GeoDatasetServiceIntegrationTest.
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE674", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( Collection<?> ) e.getData() ).iterator().next();
        }
        assertNotNull( ee );
        ee = eeService.thaw( ee );
        processedExpressionDataVectorService.createProcessedDataVectors( ee, false );

        ee = eeService.findByShortName( "GSE674" );

        assertNotNull( ee );

        SVDResult svd = svdService.svd( ee );

        assertNotNull( svd );

        assertNotNull( svd.getVMatrix() );
        assertEquals( 5, svd.getFactorCorrelations().size() );

        analysisUtilService.deleteOldAnalyses( ee );

    }

    /*
     * See bug 2139; two different sets of bioassays in the data.
     */
    @Test
    @NetworkAvailable(url = EntrezUtils.ESEARCH)
    public void testSvdGapped() throws Exception {

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gse482short" ) ) );
        // also used in the GeoDatasetServiceIntegrationTest.
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE482", false, true, false );
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
        processedExpressionDataVectorService.createProcessedDataVectors( ee, false );

        ee = eeService.findByShortName( "GSE482" );

        assertNotNull( ee );

        SVDResult svd = svdService.svd( ee );

        assertNotNull( svd );
        assertNotNull( svd.getVMatrix() );

        assertEquals( 10, svd.getBioMaterials().size() );
    }

    @AfterEach
    public void tearDown() {
        if ( ee != null ) {
            try {
                eeService.remove( ee );
            } catch ( Exception ignored ) {

            }
        }
    }

}
