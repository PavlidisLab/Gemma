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
package edu.columbia.gemma.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static Log log = LogFactory.getLog( AffyAnalyzeTest.class.getName() );
    AffyAnalyze aa;
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
            aa = new AffyAnalyze();
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
        aa = null;
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.AffyBatch(DenseDoubleMatrix2DNamed, ArrayDesign)'
     */
    public void testAffyBatch() {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        aa.AffyBatch( celmatrix, arrayDesign );
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.rma(DenseDoubleMatrix2DNamed, ArrayDesign)'
     */
    public void testRma() {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
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
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        // aa.normalize( celmatrix, arrayDesign );
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.backgroundTreat(DenseDoubleMatrix2DNamed, ArrayDesign)'
     */
    public void testBackgroundTreat() {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        // aa.backgroundTreat( celmatrix, arrayDesign );
    }

}
