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
package edu.columbia.gemma.loader.expression.geo;

import java.util.ArrayList;
import java.util.List;

import edu.columbia.gemma.common.quantitationtype.PrimitiveType;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;

import junit.framework.TestCase;
import baseCode.io.ByteArrayConverter;

/**
 * Unit test for GeoConversion. Not for integration tests.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoConverterTest extends TestCase {

    GeoConverter gc = new GeoConverter();
    ByteArrayConverter bac = new ByteArrayConverter();

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
     * Test method for 'edu.columbia.gemma.loader.expression.geo.GeoConverter.convertData(List<String>)'
     */
    public void testConvertDataIntegers() {
        List<String> testList = new ArrayList<String>();
        testList.add( "1" );
        testList.add( "2929202" );
        testList.add( "-394949" );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setRepresentation( PrimitiveType.INT );
        byte[] actualResult = gc.convertData( testList, qt );
        int[] revertedResult = bac.byteArrayToInts( actualResult );
        assertEquals( revertedResult[0], 1 );
        assertEquals( revertedResult[1], 2929202 );
        assertEquals( revertedResult[2], -394949 );
    }

    public void testConvertDataDoubles() {
        List<String> testList = new ArrayList<String>();
        testList.add( "1.1" );
        testList.add( "2929202e-4" );
        testList.add( "-394949.44422" );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setRepresentation( PrimitiveType.DOUBLE );
        byte[] actualResult = gc.convertData( testList, qt );
        double[] revertedResult = bac.byteArrayToDoubles( actualResult );
        assertEquals( revertedResult[0], 1.1, 0.00001 );
        assertEquals( revertedResult[1], 2929202e-4, 0.00001 );
        assertEquals( revertedResult[2], -394949.44422, 0.00001 );
    }

    public void testConvertDataMixed() {
        List<String> testList = new ArrayList<String>();
        testList.add( "1" ); // should trigger use of integers
        testList.add( "2929202e-4" ); // uh-oh
        testList.add( "-394949.44422" );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setRepresentation( PrimitiveType.INT );
        try {
            gc.convertData( testList, qt );
            fail( "Should have gotten an exception" );
        } catch ( RuntimeException e ) {

        }
    }

}
