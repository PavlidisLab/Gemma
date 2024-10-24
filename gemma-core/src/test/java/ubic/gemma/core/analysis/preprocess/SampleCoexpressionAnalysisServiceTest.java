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
package ubic.gemma.core.analysis.preprocess;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import static org.junit.Assert.*;

/**
 * @author paul
 */
public class SampleCoexpressionAnalysisServiceTest extends BaseSpringContextTest {

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Test
    @Category(SlowTest.class)
    public void test() throws QuantitationTypeConversionException, FilteringException {
        ExpressionExperiment ee = super.getTestPersistentCompleteExpressionExperiment( false );
        assertFalse( sampleCoexpressionAnalysisService.hasAnalysis( ee ) );
        assertNull( sampleCoexpressionAnalysisService.loadFullMatrix( ee ) );
        assertNull( sampleCoexpressionAnalysisService.loadRegressedMatrix( ee ) );
        assertNull( sampleCoexpressionAnalysisService.loadBestMatrix( ee ) );

        processedExpressionDataVectorService.createProcessedDataVectors( ee, true );
        sampleCoexpressionAnalysisService.compute( ee, sampleCoexpressionAnalysisService.prepare( ee ) );
        DoubleMatrix<BioAssay, BioAssay> matrix = sampleCoexpressionAnalysisService.loadFullMatrix( ee );
        assertNotNull( matrix );
        assertTrue( sampleCoexpressionAnalysisService.hasAnalysis( ee ) );

        this.check( matrix );

        // recompute ...
        sampleCoexpressionAnalysisService.compute( ee, sampleCoexpressionAnalysisService.prepare( ee ) );
        matrix = sampleCoexpressionAnalysisService.loadFullMatrix( ee );
        assertNotNull( matrix );
        assertTrue( sampleCoexpressionAnalysisService.hasAnalysis( ee ) );

        this.check( matrix );

        matrix = sampleCoexpressionAnalysisService.loadRegressedMatrix( ee );
        assertNotNull( matrix );
        this.check( matrix );

        matrix = sampleCoexpressionAnalysisService.loadBestMatrix( ee );
        assertNotNull( matrix );

        this.check( matrix );
    }

    /**
     * Sanity checks: should be symmetric etc.
     *
     * @param matrix matrix
     */
    private void check( DoubleMatrix<BioAssay, BioAssay> matrix ) {
        for ( int i = 0; i < matrix.rows(); i++ ) {
            assertEquals( matrix.get( i, i ), 1.0, 0.001 );
            for ( int j = i + 1; j < matrix.rows(); j++ ) {
                assertEquals( matrix.get( j, i ), matrix.get( i, j ), 0.001 );
                assertTrue( Math.abs( matrix.get( j, i ) ) <= 1.0 && Math.abs( matrix.get( j, i ) ) >= 0.0 );
            }
        }
        assertEquals( 8, matrix.rows() );
    }
}
