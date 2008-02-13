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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.reader.DoubleMatrixReader;

/**
 * @author pavlidis
 * @version $Id$
 */
public class RMATest extends TestCase {

    private static Log log = LogFactory.getLog( RMATest.class.getName() );
    RMA aa;
    DoubleMatrixNamed celmatrix;
    ArrayDesign arrayDesign;
    InputStream is;

    boolean connected = false;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // test data are from the affybatch.example in the affy package.
        DoubleMatrixReader reader = new DoubleMatrixReader();
        is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/testShortCel.txt.gz" ) );
        if ( is == null ) throw new IOException();
        celmatrix = reader.read( is );
        is.close();
        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setName( "cdfenv.example" );

        try {
            aa = new RMA();
            connected = true;
        } catch ( Exception e ) {
            connected = false;
        }
    }

    /*
     * Test method for 'ubic.gemma.tools.AffyAnalyze.rma(DoubleMatrixNamed, ArrayDesign)'
     */
    public void testRma() {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        aa.setArrayDesign( arrayDesign );
        DoubleMatrixNamed result = aa.summarize( celmatrix );
        assertTrue( result != null );
        assertEquals( 150, result.rows() );
        assertEquals( 3, result.columns() );
        assertEquals( "A28102_at", result.getRowName( 0 ) );

        // values come from
        // exprs(bg.correct.rma(affybatch.example))[11,3]

        assertEquals( 7.000993, result.get( 10, 2 ), 0.0001 );
    }

}
