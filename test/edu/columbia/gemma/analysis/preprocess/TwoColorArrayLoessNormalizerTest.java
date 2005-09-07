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

import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
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
public class TwoColorArrayLoessNormalizerTest extends TestCase {
    TwoColorArrayLoessNormalizer normalizer;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        normalizer = new TwoColorArrayLoessNormalizer();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        normalizer.cleanup();
    }

    /*
     * Test method for 'edu.columbia.gemma.analysis.preprocess.TwoColorArrayLoessNormalizer.normalize(DoubleMatrixNamed,
     * DoubleMatrixNamed, DoubleMatrixNamed, DoubleMatrixNamed, DoubleMatrixNamed)'
     */
    public void testNormalize() throws Exception {

        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrixNamed maGb = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGb.sample.txt.gz" ) ) );
        DoubleMatrixNamed maGf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGf.sample.txt.gz" ) ) );
        DoubleMatrixNamed maRb = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRb.sample.txt.gz" ) ) );
        DoubleMatrixNamed maRf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRf.sample.txt.gz" ) ) );

        DoubleMatrixNamed result = normalizer.normalize( maRf, maGf, maRb, maGb, null );

        assertEquals( 8448, result.rows() );
        assertEquals( 4, result.columns() );
        // assertEquals( -0.2841363, result.get( 99, 2 ), 0.0001 ); // loess normaliation isn't deterministic in marray.

    }

    public void testNormalizeNoBg() throws Exception {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrixNamed maRf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRf.sample.txt.gz" ) ) );
        DoubleMatrixNamed maGf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGf.sample.txt.gz" ) ) );
        DoubleMatrixNamed result = normalizer.normalize( maRf, maGf );

        assertEquals( 8448, result.rows() );
        assertEquals( 4, result.columns() );
    }

}
