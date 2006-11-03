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
package ubic.gemma.loader.expression.geo;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSample;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GeoFamilyParserTest extends TestCase {

    InputStream is;
    GeoFamilyParser parser;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        parser = new GeoFamilyParser();
    }

    public void testParseShortFamily() throws Exception {
        is = this.getClass().getResourceAsStream( "/data/loader/expression/geo/soft_ex_affy.txt" );
        parser.parse( is );
        assertEquals( 3, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    public void testParseBigA() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/fullSizeTests/GSE1623_family.soft.txt.gz" ) );
        parser.parse( is );
        assertEquals( 8, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    public void testParseBigB() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/fullSizeTests/GSE993_family.soft.txt.gz" ) );
        parser.parse( is );
        assertEquals( 1, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    public void testParseGenePix() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/shortGenePix/GSE2221_family.soft.gz" ) );
        parser.parse( is );
        GeoSample sample = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().values()
                .iterator().next();
        assertTrue( sample.isGenePix() );
        assertEquals( 54, sample.getColumnNames().size() ); // includes ones we aren't using.
    }

    public void testParseBigBPlatformOnly() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/fullSizeTests/GSE993_family.soft.txt.gz" ) );
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        assertEquals( 0, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
        assertEquals( 0, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeries().size() );
        assertEquals( 1, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatforms().size() );
        GeoPlatform p = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatforms().values().iterator()
                .next();
        assertEquals( 32097, p.getColumnData( "ORF" ).size() );
    }

    public void testParseDataset() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/fullSizeTests/GDS100.soft.txt.gz" ) );
        assert is != null;
        parser.parse( is );
        assertEquals( 8, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

}
