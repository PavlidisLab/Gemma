/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.analysis.preprocess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.io.reader.DoubleMatrixReader;
import baseCode.util.RCommand;
import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class QuantileNormalizerTest extends TestCase {
    private static Log log = LogFactory.getLog( QuantileNormalizerTest.class.getName() );

    DoubleMatrixNamed tester;

    public void setUp() throws Exception {

        log.debug( "Reading test data" );
        DoubleMatrixReader reader = new DoubleMatrixReader();
        tester = ( DoubleMatrixNamed ) reader.read( this.getClass().getResourceAsStream( "/data/testdata.txt" ) );
        assert tester != null;
        log.debug( "Setup done" );
    }

    public void tearDown() throws Exception {
        super.tearDown();

        tester = null;
    }

    /*
     * Test method for 'edu.columbia.gemma.analysis.preprocess.QuantileNormalizer.normalize(DenseDoubleMatrix2DNamed)'
     */
    public void testNormalize() {
        QuantileNormalizer qn = new QuantileNormalizer();
        DoubleMatrixNamed result = qn.normalize( tester );

        // d<-read.table("testdata.txt", header=T, row.names=1)
        // normalize.quantiles(as.matrix(d))[1,10]

        assertEquals( -0.525, result.getQuick( 0, 9 ), 0.001 );
    }

}
