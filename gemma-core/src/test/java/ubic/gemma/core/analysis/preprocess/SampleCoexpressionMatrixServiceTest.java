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
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.testing.BaseSpringContextTest;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author paul
 */
public class SampleCoexpressionMatrixServiceTest extends BaseSpringContextTest {

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private SampleCoexpressionMatrixService sampleCoexpressionMatrixService;

    @Test
    public void test() {
        ExpressionExperiment ee = super.getTestPersistentCompleteExpressionExperiment( false );

        processedExpressionDataVectorService.computeProcessedExpressionData( ee );
        DoubleMatrix<BioAssay, BioAssay> matrix = sampleCoexpressionMatrixService.create( ee );

        this.check( matrix );

        // recompute ...
        matrix = sampleCoexpressionMatrixService.create( ee );

        this.check( matrix );

        matrix = sampleCoexpressionMatrixService.findOrCreate( ee );

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
