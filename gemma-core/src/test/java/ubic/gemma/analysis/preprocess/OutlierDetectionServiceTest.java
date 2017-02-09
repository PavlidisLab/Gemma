/*
 * The gemma-core project
 * 
 * Copyright (c) 2013 University of British Columbia
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

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Tests for {@link OutlierDetectionService}
 * 
 * @author ptan
 * @version $Id$
 */
public class OutlierDetectionServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private OutlierDetectionService outlierDetectionService;

    /**
     * Test method for
     * {@link ubic.gemma.analysis.preprocess.OutlierDetectionService#hasOutliers(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     * .
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testIdentifyOutliers() throws URISyntaxException {
        ExpressionExperiment ee = eeService.findByShortName( "GSE2982" );

        if ( ee == null ) {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "gse2982Short" ) ) );

            Collection<?> results = geoService.fetchAndLoad( "GSE2982", false, false, true, false );

            ee = ( ExpressionExperiment ) results.iterator().next();
        }

        ee = processedExpressionDataVectorService.createProcessedDataVectors( ee );

        DoubleMatrix<BioAssay, BioAssay> sampleCorrelationMatrix = sampleCoexpressionMatrixService.findOrCreate( ee );

        // no outliers initially
        Collection<OutlierDetails> output = outlierDetectionService.identifyOutliers( ee, sampleCorrelationMatrix );
        assertEquals( 0, output.size() );

        // modify a sample to be the outlier
        int outlierIdx = 0;
        for ( int j = 0; j < sampleCorrelationMatrix.columns(); j++ ) {
            sampleCorrelationMatrix.set( j, outlierIdx, -0.5 + j / 100.0 );
            sampleCorrelationMatrix.set( outlierIdx, j, -0.5 + j / 100.0 );
        }

        // now we expect one outlier from the modified matrix
        output = outlierDetectionService.identifyOutliers( ee, sampleCorrelationMatrix );
        assertEquals( 1, output.size() );
        assertEquals( sampleCorrelationMatrix.getColName( outlierIdx ), output.iterator().next().getBioAssay() );
    }

}
