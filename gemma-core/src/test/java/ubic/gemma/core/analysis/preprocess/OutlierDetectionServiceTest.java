/*
 * The gemma project
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

package ubic.gemma.core.analysis.preprocess;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

import java.util.Collection;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OutlierDetectionService}
 *
 * @author ptan
 */
@ContextConfiguration
public class OutlierDetectionServiceTest extends BaseTest {

    private static final int MATRIX_SIZE = 20;

    @Configuration
    @TestComponent
    static class CC {
        @Bean
        public OutlierDetectionService outlierDetectionService() {
            return new OutlierDetectionServiceImpl( mock() );
        }

        @Bean
        public SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService() {
            return mock();
        }
    }

    private final Random random = new Random( 123 );

    @Autowired
    private OutlierDetectionService outlierDetectionService;

    @Test
    public void testIdentifyOutliers() {
        DoubleMatrix<BioAssay, BioAssay> sampleCorrelationMatrix = this.createMockMatrix();

        // 1 outlier initially
        Collection<OutlierDetails> output = outlierDetectionService.identifyOutliersByMedianCorrelation( sampleCorrelationMatrix );
        assertEquals( 0, output.size() );

        // modify 2 samples to be outliers
        int outlierIdx = 1;
        int outlierIdx2 = 14;
        for ( int j = 0; j < sampleCorrelationMatrix.columns(); j++ ) {
            Double val = 0.8 + ( random.nextDouble() / 10 );
            sampleCorrelationMatrix.set( j, outlierIdx, val );
            sampleCorrelationMatrix.set( outlierIdx, j, val );
            val = 0.8 + ( random.nextDouble() / 10 );
            sampleCorrelationMatrix.set( j, outlierIdx2, val );
            sampleCorrelationMatrix.set( outlierIdx2, j, val );
        }

        BioAssay ol1 = sampleCorrelationMatrix.getColName( outlierIdx );
        BioAssay ol2 = sampleCorrelationMatrix.getColName( outlierIdx2 );

        // now we expect one new outlier from the modified matrix
        output = outlierDetectionService.identifyOutliersByMedianCorrelation( sampleCorrelationMatrix );
        assertEquals( 2, output.size() );


        boolean found1 = false;
        boolean found2 = false;
        for ( OutlierDetails outlier : output ) {
            Long s = outlier.getBioAssayId();
            if ( ol1.getId().equals( s ) ) {
                found1 = true;
            } else if ( ol2.getId().equals( s ) ) {
                found2 = true;
            }
        }

        assert ( found1 && found2 );

    }

    private DoubleMatrix<BioAssay, BioAssay> createMockMatrix() {
        DoubleMatrix<BioAssay, BioAssay> matrix = new DenseDoubleMatrix<>( OutlierDetectionServiceTest.MATRIX_SIZE, OutlierDetectionServiceTest.MATRIX_SIZE );
        for ( int i = 0; i < OutlierDetectionServiceTest.MATRIX_SIZE; i++ ) {
            BioAssay sample = BioAssay.Factory.newInstance( RandomStringUtils.insecure().nextAlphabetic( 8 ) );
            sample.setId( i + 1L );
            matrix.setRowName( sample, i );
            matrix.setColumnName( sample, i );
            for ( int j = 0; j < OutlierDetectionServiceTest.MATRIX_SIZE; j++ ) {
                matrix.set( i, j, 0.9 + ( random.nextDouble() / 10 ) );
            }
        }
        return matrix;
    }

}
