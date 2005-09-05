package edu.columbia.gemma.analysis.preprocess;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class RMATest extends TestCase {

    private static Log log = LogFactory.getLog( RMATest.class.getName() );
    RMA aa;
    DenseDoubleMatrix2DNamed celmatrix;
    ArrayDesign arrayDesign;
    InputStream is;

    boolean connected = false;

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

        try {
            aa = new RMA();
            connected = true;
        } catch ( RuntimeException e ) {
            connected = false;
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.rma(DenseDoubleMatrix2DNamed, ArrayDesign)'
     */
    public void testRma() {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        aa.setArrayDesign( arrayDesign );
        NamedMatrix result = aa.summarize( celmatrix );
        assertTrue( result != null );
        assertEquals( 150, result.rows() );
        assertEquals( 3, result.columns() );
        assertEquals( "A28102_at", result.getRowName( 0 ) );
    }

}
