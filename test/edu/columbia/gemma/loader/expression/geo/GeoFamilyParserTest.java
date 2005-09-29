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
package edu.columbia.gemma.loader.expression.geo;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoFamilyParserTest extends TestCase {

    InputStream is;
    GeoFamilyParser parser;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        parser = new GeoFamilyParser();
    }

    public void testParseShortFamily() throws Exception {
        is = this.getClass().getResourceAsStream( "/data/geo/soft_ex_affy.txt" );
        parser.parse( is );
        assertEquals( 3, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    public void testParseBigA() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/geo/GSE1623_family.soft.txt.gz" ) );
        parser.parse( is );
        assertEquals( 8, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    public void testParseBigB() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/geo/GSE993_family.soft.txt.gz" ) );
        parser.parse( is );
        assertEquals( 1, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    public void testParseDataset() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/geo/GDS100.soft.txt.gz" ) );
        parser.parse( is );
        assertEquals( 8, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

}
