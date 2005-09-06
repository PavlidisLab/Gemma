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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
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
public class AffyBatchTest extends TestCase {
    private static Log log = LogFactory.getLog( AffyBatchTest.class.getName() );
    AffyBatch aa;
    DoubleMatrixNamed celmatrix;
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
        celmatrix = ( DoubleMatrixNamed ) reader.read( is );
        is.close();
        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( "cdfenv.example" );

        try {
            aa = new AffyBatch();
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
     * Test method for 'edu.columbia.gemma.tools.AffyAnalyze.AffyBatch(DoubleMatrixNamed, ArrayDesign)'
     */
    public void testAffyBatch() {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        aa.makeAffyBatch( celmatrix, arrayDesign );
    }

}
