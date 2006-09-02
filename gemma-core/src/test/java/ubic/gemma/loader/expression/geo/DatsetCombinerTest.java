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
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;
import ubic.gemma.loader.expression.geo.model.GeoDataset;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DatsetCombinerTest extends TestCase {

    private static Log log = LogFactory.getLog( DatsetCombinerTest.class.getName() );
    Collection<GeoDataset> gds;

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
     * Test method for 'ubic.gemma.loader.expression.geo.DatsetCombiner.findGDSGrouping(String)'
     */
    public void testFindGDSGrouping() throws Exception {
        try {
            Collection result = DatasetCombiner.findGDSforGSE( "GSE674" );
            assertTrue( result.contains( "GDS472" ) && result.contains( "GDS473" ) );
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof java.net.UnknownHostException ) {
                log.warn( "Test skipped due to unknown host exception" );
                return;
            }
            throw e;
        }
    }

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.DatsetCombiner.findGSECorrespondence(Collection<GeoDataset>)'
     */
    public void testFindGSECorrespondence() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/twoDatasets/GDS472.soft.gz" ) );
        assert is != null;
        parser.parse( is );
        is.close();
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/twoDatasets/GDS473.soft.gz" ) );
        parser.parse( is );
        is.close();
        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );

        gds = parseResult.getDatasets().values();
        GeoSampleCorrespondence result = DatasetCombiner.findGSECorrespondence( gds );

        String[] keys = new String[] { "GSM10354", "GSM10355", "GSM10356", "GSM10359", "GSM10360", "GSM10361",
                "GSM10362", "GSM10363", "GSM10364", "GSM10365", "GSM10366", "GSM10367", "GSM10368", "GSM10369",
                "GSM10370", "GSM10374", "GSM10375", "GSM10376", "GSM10377", "GSM10378", "GSM10379", "GSM10380",
                "GSM10381", "GSM10382", "GSM10383", "GSM10384", "GSM10385", "GSM10386", "GSM10387", "GSM10388" };

        for ( int i = 0; i < keys.length; i++ ) {
            String string = keys[i];
            assertTrue( result.getCorrespondingSamples( string ).size() == 2 );
        }
        assertTrue( result.getCorrespondingSamples( "GSM10354" ).contains( "GSM10374" ) );
        assertTrue( result.getCorrespondingSamples( "GSM10374" ).contains( "GSM10354" ) );
    }

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.DatsetCombiner.findGSECorrespondence(Collection<GeoDataset>)'
     */
    public void testFindGSECorrespondenceThreeDatasets() throws Exception {

        // GSE479 GDS242-244 fits the bill (MG-U74A,B and C (v2)

        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS242.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS243.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS244.soft.gz" ) );
        parser.parse( is );
        assert is != null;
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );

        String[] keys = new String[] { "GSM4076", "GSM4047", "GSM4071", "GSM4052", "GSM4077", "GSM4070", "GSM4053",
                "GSM4046", "GSM4089", "GSM4082", "GSM4083", "GSM4088", "GSM4058", "GSM4065", "GSM4064", "GSM4059",
                "GSM4055", "GSM4079", "GSM4048", "GSM4078", "GSM4072", "GSM4054", "GSM4073", "GSM4049", "GSM4090",
                "GSM4067", "GSM4061", "GSM4085", "GSM4060", "GSM4091", "GSM4084", "GSM4066", "GSM4081", "GSM4087",
                "GSM4086", "GSM4062", "GSM4057", "GSM4056", "GSM4080", "GSM4063", "GSM4068", "GSM4044", "GSM4050",
                "GSM4051", "GSM4069", "GSM4045", "GSM4074", "GSM4075" };

        gds = parseResult.getDatasets().values();

        GeoSampleCorrespondence result = DatasetCombiner.findGSECorrespondence( gds );

        // System.err.println( result );

        for ( int i = 0; i < keys.length; i++ ) {
            String string = keys[i];
            assertTrue( result.getCorrespondingSamples( string ).size() == 3 );
        }
        assertTrue( result.getCorrespondingSamples( "GSM4076" ).contains( "GSM4078" ) );
        assertTrue( result.getCorrespondingSamples( "GSM4080" ).contains( "GSM4084" ) );
    }
}
