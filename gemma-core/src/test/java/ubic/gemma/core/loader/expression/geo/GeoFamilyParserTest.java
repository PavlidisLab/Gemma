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
package ubic.gemma.core.loader.expression.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * @author pavlidis
 */
@Tag("slow")
public class GeoFamilyParserTest {

    private InputStream is;
    private GeoFamilyParser parser;

    @Test
    public void testParseBigA() throws Exception {
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/fullSizeTests/GSE1623_family.soft.txt.gz" ).getInputStream() );
        parser.parse( is );
        Assertions.assertEquals( 8, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    @Test
    public void testParseBigBPlatformOnly() throws Exception {
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/fullSizeTests/GSE1623_family.soft.txt.gz" ).getInputStream() );
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        Assertions.assertEquals( 0, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
        Assertions.assertEquals( 0, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeries().size() );
        Assertions.assertEquals( 1, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatforms().size() );
        GeoPlatform p = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatforms().values().iterator()
                .next();
        Assertions.assertEquals( 12488, p.getColumnData( "GB_ACC" ).size() );
    }

    @Test
    public void testParseDataset() throws Exception {
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/fullSizeTests/GDS100.soft.txt.gz" ).getInputStream() );
        parser.parse( is );
        Assertions.assertEquals( 8, ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().size() );
    }

    @Test
    public void testParseGenePix() throws Exception {
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/shortGenePix/GSE2221_family.soft.gz" ).getInputStream() );
        parser.parse( is );
        GeoSample sample = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().values().iterator()
                .next();
        Assertions.assertTrue( sample.isGenePix() );
        Assertions.assertEquals( 54, sample.getColumnNames().size() ); // includes ones we aren't using.
    }

    /*
     * Lacks data for some samples (on purpose)
     */
    @Test
    public void testParseGSE29014() throws Exception {
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE29014.soft.gz" ).getInputStream() );
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap()
                .get( "GSE29014" );

        Assertions.assertEquals( 78, series.getSamples().size() );
    }

    /*
     * Failed with a 'already a datum for CH1_BKG ... ' error. GSE1347 has same problem.
     */
    @Test
    public void testParseGse432() throws Exception {
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse432Short/GSE432_family.soft.gz" ).getInputStream() );
        parser.parse( is );
    }

    @Test
    public void testParseSAGE() throws Exception {
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse2122shortSage/GSE2122.soft.gz" ).getInputStream() );
        parser.parse( is );
        GeoSample sample = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSamples().values().iterator()
                .next();
        Assertions.assertTrue( !sample.hasUsableData() );
        Assertions.assertEquals( 4, sample.getColumnNames().size() ); // includes ones we aren't using.
    }

    @BeforeEach
    public void setUp() {
        parser = new GeoFamilyParser();
    }

}
