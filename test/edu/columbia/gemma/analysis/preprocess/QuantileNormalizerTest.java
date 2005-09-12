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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.io.reader.DoubleMatrixReader;

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
    QuantileNormalizer qn;

    private boolean connected = false;

    public void setUp() throws Exception {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        tester = ( DoubleMatrixNamed ) reader.read( this.getClass().getResourceAsStream( "/data/testdata.txt" ) );
        assert tester != null;

        try {
            qn = new QuantileNormalizer();
            connected = true;
        } catch ( Exception e ) {
            connected = false;
        }

        log.debug( "Setup done" );
    }

    public void tearDown() throws Exception {
        super.tearDown();
        tester = null;
        qn.cleanup();
    }

    /*
     * Test method for 'edu.columbia.gemma.analysis.preprocess.QuantileNormalizer.normalize(DenseDoubleMatrix2DNamed)'
     */
    public void testNormalize() {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        DoubleMatrixNamed result = qn.normalize( tester );

        // d<-read.table("testdata.txt", header=T, row.names=1)
        // normalize.quantiles(as.matrix(d))[1,10]

        assertEquals( -0.525, result.getQuick( 0, 9 ), 0.001 );
    }

}
