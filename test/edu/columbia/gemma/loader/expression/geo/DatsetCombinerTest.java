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
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DatsetCombinerTest extends TestCase {

    DatasetCombiner dc;
    Collection<GeoDataset> gds;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        dc = new DatasetCombiner();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'edu.columbia.gemma.loader.expression.geo.DatsetCombiner.findGDSGrouping(String)'
     */
    public void testFindGDSGrouping() throws Exception {
        Collection result = dc.findGDSGrouping( "GSE674" );
        assertTrue( result.contains( "GDS472" ) && result.contains( "GDS473" ) );
    }

    /*
     * Test method for 'edu.columbia.gemma.loader.expression.geo.DatsetCombiner.findGSECorrespondence(Collection<GeoDataset>)'
     */
    public void testFindGSECorrespondence() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/geo/GDS472.soft.gz" ) );
        parser.parse( is );
        is.close();
        is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/geo/GDS473.soft.gz" ) );
        parser.parse( is );
        is.close();
        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        GeoSampleCorrespondence result = dc.findGSECorrespondence( gds );

        String[] keys = new String[] { "GSM10354", "GSM10355", "GSM10356", "GSM10359", "GSM10360", "GSM10361",
                "GSM10362", "GSM10363", "GSM10364", "GSM10365", "GSM10366", "GSM10367", "GSM10368", "GSM10369",
                "GSM10370", "GSM10374", "GSM10375", "GSM10376", "GSM10377", "GSM10378", "GSM10379", "GSM10380",
                "GSM10381", "GSM10382", "GSM10383", "GSM10384", "GSM10385", "GSM10386", "GSM10387", "GSM10388" };

        for ( int i = 0; i < keys.length; i++ ) {
            String string = keys[i];
            assertTrue( result.getCorrespondingSamples( string ).size() == 1 );
        }
        assertTrue( result.getCorrespondingSamples( "GSM10354" ).iterator().next().equals( "GSM10374" ) );
        assertTrue( result.getCorrespondingSamples( "GSM10374" ).iterator().next().equals( "GSM10354" ) );
    }
}
