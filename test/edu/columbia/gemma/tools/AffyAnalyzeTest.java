package edu.columbia.gemma.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import baseCode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import baseCode.dataStructure.matrix.NamedMatrix;
import baseCode.io.reader.DoubleMatrixReader;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyAnalyzeTest extends TestCase {
    // private static Log log = LogFactory.getLog( AffyAnalyzeTest.class.getName() );
    AffyAnalyze aa;
    DenseDoubleMatrix2DNamed celmatrix;
    ArrayDesign arrayDesign;
    InputStream is;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        // test data are from the affybatch.example in the affy package.
        DoubleMatrixReader reader = new DoubleMatrixReader();
        is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/testShortCel.txt.gz" ) );
        if ( is == null ) throw new IOException();
        celmatrix = ( DenseDoubleMatrix2DNamed ) reader.read( is );
        is.close();
        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( "cdfenv.example" );
        aa = new AffyAnalyze();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        aa = null;
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.AffyBatch(DenseDoubleMatrix2DNamed, ArrayDesign)'
     */
    public void testAffyBatch() {
        aa.AffyBatch( celmatrix, arrayDesign );
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.rma(DenseDoubleMatrix2DNamed, ArrayDesign)'
     */
    public void testRma() {
        NamedMatrix result = aa.rma( celmatrix, arrayDesign );
        assertTrue( result != null );
        assertEquals( 150, result.rows() );
        assertEquals( 3, result.columns() );
        assertEquals( "A28102_at", result.getRowName( 0 ) );
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.normalize(DenseDoubleMatrix2DNamed, ArrayDesign)'
     */
    public void testNormalize() {
        aa.normalize( celmatrix, arrayDesign );
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.backgroundTreat(DenseDoubleMatrix2DNamed, ArrayDesign)'
     */
    public void testBackgroundTreat() {
        aa.backgroundTreat( celmatrix, arrayDesign );
    }

}
