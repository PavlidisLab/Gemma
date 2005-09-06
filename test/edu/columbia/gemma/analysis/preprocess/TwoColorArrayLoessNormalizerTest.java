package edu.columbia.gemma.analysis.preprocess;

import java.util.zip.GZIPInputStream;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.io.reader.DoubleMatrixReader;
import edu.columbia.gemma.tools.MArrayRaw;
import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TwoColorArrayLoessNormalizerTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'edu.columbia.gemma.analysis.preprocess.TwoColorArrayLoessNormalizer.normalize(DoubleMatrixNamed,
     * DoubleMatrixNamed, DoubleMatrixNamed, DoubleMatrixNamed, DoubleMatrixNamed)'
     */
    public void testNormalizeDoubleMatrixNamedDoubleMatrixNamedDoubleMatrixNamedDoubleMatrixNamedDoubleMatrixNamed()
            throws Exception {

        // we mimic this analysis in R like this:
        // maGb<-read.table("maGb.saml

        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrixNamed maGb = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGb.sample.txt.gz" ) ) );
        DoubleMatrixNamed maGf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGf.sample.txt.gz" ) ) );
        DoubleMatrixNamed maRb = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRb.sample.txt.gz" ) ) );
        DoubleMatrixNamed maRf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRf.sample.txt.gz" ) ) );
        TwoColorArrayLoessNormalizer normalizer = new TwoColorArrayLoessNormalizer();
        DoubleMatrixNamed result = normalizer.normalize( maRf, maGf, maRb, maGb, null );

        // g[100,3] = -0.2841363
        assertEquals( 8448, result.rows() );
        assertEquals( 4, result.columns() );
        // assertEquals( -0.2841363, result.get( 99, 2 ), 0.0001 ); // loess normaliation isn't deterministic in marray.

    }

}
