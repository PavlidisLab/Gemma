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
package ubic.gemma.analysis.preprocess;

import static org.junit.Assert.*;

import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.analysis.preprocess.normalize.TwoColorArrayLoessNormalizer;

/**
 * @author pavlidis
 * @version $Id$
 */
public class TwoColorArrayLoessNormalizerTest {
    private static Log log = LogFactory.getLog( TwoColorArrayLoessNormalizerTest.class );
    private TwoColorArrayLoessNormalizer normalizer;
    private boolean connected = false;

    /*
     * Test method for 'ubic.gemma.model.analysis.preprocess.TwoColorArrayLoessNormalizer.normalize(DoubleMatrixNamed,
     * DoubleMatrixNamed, DoubleMatrixNamed, DoubleMatrixNamed, DoubleMatrixNamed)'
     */
    @Test
    public void testNormalize() throws Exception {
        if ( !connected ) {
            log.warn( "Could not connect to R, skipping test." );
            return;
        }

        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrix<String, String> maGb = reader.read( new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/swirldata/maGb.small.sample.txt.gz" ) ) );
        DoubleMatrix<String, String> maGf = reader.read( new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/swirldata/maGf.small.sample.txt.gz" ) ) );
        DoubleMatrix<String, String> maRb = reader.read( new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/swirldata/maRb.small.sample.txt.gz" ) ) );
        DoubleMatrix<String, String> maRf = reader.read( new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/swirldata/maRf.small.sample.txt.gz" ) ) );
        assert maRf != null && maGf != null && maRb != null && maGb != null;
        DoubleMatrix<String, String> result = normalizer.normalize( maRf, maGf, maRb, maGb, null );

        assertEquals( 100, result.rows() );
        assertEquals( 4, result.columns() );
        // assertEquals( -0.2841363, result.get( 99, 2 ), 0.0001 ); // loess normaliation isn't deterministic in marray.

    }

    @Test
    public void testNormalizeNoBg() throws Exception {
        if ( !connected ) {
            log.warn( "Could not connect to R, skipping test." );
            return;
        }
        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrix<String, String> maRf = reader.read( new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/swirldata/maRf.small.sample.txt.gz" ) ) );
        DoubleMatrix<String, String> maGf = reader.read( new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/swirldata/maGf.small.sample.txt.gz" ) ) );

        assert maRf != null && maGf != null;

        DoubleMatrix<String, String> result = normalizer.normalize( maRf, maGf );

        assertEquals( 100, result.rows() );
        assertEquals( 4, result.columns() );
    }

    /*
     * @see TestCase#setUp()
     */
    @Before
    public final void setUp() throws Exception {
        connected = false;
        try {
            normalizer = new TwoColorArrayLoessNormalizer();
            connected = true;
        } catch ( Exception e ) {
            connected = false;
        }

    }

    /*
     * @see TestCase#tearDown()
     */
    @After
    public void tearDown() throws Exception {
        if ( connected ) normalizer.cleanup();
        normalizer = null;
    }

}
