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
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.junit.Test;

import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSample;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.loader.expression.geo.model.GeoValues;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GeoFamilyParserTest extends TestCase {

    InputStream is;
    GeoFamilyParser parser;

    @Test
    public void testParseBigA() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/fullSizeTests/GSE1623_family.soft.txt.gz" ) );
        parser.parse( is );
        assertEquals( 8, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    /**
     * This was getting garbled because not all samples have all the same quantitation types in the same order etc.
     * 
     * @throws Exception
     */
    // public void testParseGse59() throws Exception {
    // is = new GZIPInputStream( this.getClass().getResourceAsStream(
    // "/data/loader/expression/geo/GSE59Short/GSE59_family.soft.gz" ) );
    // parser.parse( is );
    // GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE59" );
    // // GeoValues values = series.getValues();
    // System.err.print( values );
    // }
    // /**
    // * This data set has a lot of changes in the column ordering, etc. for samples
    // *
    // * @throws Exception
    // */
    // @SuppressWarnings("unchecked")
    // public void testParseGSE3500() throws Exception {
    // is = new GZIPInputStream( this.getClass().getResourceAsStream(
    // "/data/loader/expression/geo/gse3500Short/GSE3500_family.soft.gz" ) );
    // parser.setAgressiveQtRemoval( true );
    // parser.parse( is );
    // GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE3500" );
    // GeoValues values = series.getValues();
    // // System.err.print( values );
    // }
    // /**
    // * Failed assertio durin gstoring CH1_BKG_MEAN
    // *
    // * @throws Exception
    // */
    // public void testParseGse2776() throws Exception {
    // is = new GZIPInputStream( this.getClass().getResourceAsStream(
    // "/data/loader/expression/geo/gse2776Short/GSE2776.soft.gz" ) );
    // parser.parse( is );
    // }
    public void testParseBigBPlatformOnly() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/fullSizeTests/GSE1623_family.soft.txt.gz" ) );
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        assertEquals( 0, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
        assertEquals( 0, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeries().size() );
        assertEquals( 1, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatforms().size() );
        GeoPlatform p = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatforms().values().iterator()
                .next();
        assertEquals( 12488, p.getColumnData( "GB_ACC" ).size() );
    }

    // /**
    // * This is a SAGE file, with repeated tags. - we don't support this.
    // *
    // * @throws Exception
    // */
    // public void testParseBigB() throws Exception {
    // is = new GZIPInputStream( this.getClass().getResourceAsStream(
    // "/data/loader/expression/geo/fullSizeTests/GSE993_family.soft.txt.gz" ) );
    // parser.parse( is );
    // assertEquals( 1, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    // }

    public void testParseDataset() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/fullSizeTests/GDS100.soft.txt.gz" ) );
        assert is != null;
        parser.parse( is );
        assertEquals( 8, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    @Test
    public void testParseGenePix() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/shortGenePix/GSE2221_family.soft.gz" ) );
        parser.parse( is );
        GeoSample sample = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().values()
                .iterator().next();
        assertTrue( sample.isGenePix() );
        assertEquals( 54, sample.getColumnNames().size() ); // includes ones we aren't using.
    }

    /**
     * Lacks data for some samples (on purpose)
     * 
     * @throws Exception
     */
    @Test
    public void testParseGSE29014() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/loader/expression/geo/GSE29014.soft.gz" ) );
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE29014" );

        assertEquals( 78, series.getSamples().size() );
        //
        // GeoPlatform platform = series.getSamples().iterator().next().getPlatforms().iterator().next();
        // /*
        // * However, 3 of those samples have no data.
        // */
        GeoValues values = series.getValues();
        // System.err.println( values );

    }

    /**
     * Failed with a 'already a datum for CH1_BKG ... ' error. GSE1347 has same problem.
     * 
     * @throws Exception
     */
    @Test
    public void testParseGse432() throws Exception {
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse432Short/GSE432_family.soft.gz" ) );
        parser.parse( is );
        // GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE432"
        // );
        // GeoValues values = series.getValues();
        // System.err.print( values );
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        parser = new GeoFamilyParser();
    }

}
