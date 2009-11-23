/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.AbstractGeoServiceTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ProcessedExpressionDataCreateServiceTest extends AbstractGeoServiceTest {

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    GeoDatasetService geoService;

    @Autowired
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    ExpressionExperiment ee = null;

    @SuppressWarnings("unchecked")
    @Test
    public void testComputeDevRankForExpressionExperimentB() throws Exception {

        String path = getTestFileBasePath();

        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                    + "GSE5949short" ) );
            Collection<ExpressionExperiment> results = geoService.fetchAndLoad( "GSE5949", false, true, false, false,
                    false );
            this.ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            this.ee = ( ExpressionExperiment ) e.getData();
        }

        

        eeService.thawLite( ee );

        

        Collection<ProcessedExpressionDataVector> preferredVectors = processedExpressionDataVectorCreateService
                .computeProcessedExpressionData( ee );

        

        for ( ProcessedExpressionDataVector d : preferredVectors ) {
            assertTrue( d.getQuantitationType().getIsMaskedPreferred() );
            assertNotNull( d.getRankByMean() );
            assertNotNull( d.getRankByMax() );
        }
    }

}
