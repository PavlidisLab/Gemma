/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
/*
 * The Gemma project
 *
 * Copyright (c) 2008 Columbia University
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.analysis.preprocess.normalize.QuantileNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author pavlidis
 */
public class QuantileNormalizerTest {
    private static final Log log = LogFactory.getLog( QuantileNormalizerTest.class.getName() );

    private DoubleMatrix<String, String> tester;
    private QuantileNormalizer<String, String> qn;

    @BeforeEach
    public void setUp() throws Exception {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        tester = ubic.gemma.core.util.matrix.MatrixUtil.fromBaseCode( reader.read( this.getClass().getResourceAsStream( "/data/testdata.txt" ) ) );
        assert tester != null;

        qn = new QuantileNormalizer<>();
        QuantileNormalizerTest.log.debug( "Setup done" );
    }

    @AfterEach
    public void tearDown() {
        tester = null;
    }

    /*
     * Test method for 'ubic.gemma.core.analysis.preprocess.QuantileNormalizer.normalize(DenseDoubleMatrix2DNamed)'
     */
    @Test
    public void testNormalize() {
        DoubleMatrix<String, String> result = qn.normalize( tester );
        assertEquals( -0.525, result.get( 0, 9 ), 0.001 );

        for ( int i = 0; i < tester.columns(); i++ ) {
            assertEquals( tester.getColName( i ), result.getColName( i ) );
        }
        for ( int i = 0; i < tester.rows(); i++ ) {
            assertEquals( tester.getRowName( i ), result.getRowName( i ) );
        }
    }
}
