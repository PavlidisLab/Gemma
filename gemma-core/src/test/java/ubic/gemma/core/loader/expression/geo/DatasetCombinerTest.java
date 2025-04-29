/*
 * The Gemma project
 *
 * Copyright (c) 2006-2010 University of British Columbia
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.expression.geo.model.GeoDataset;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * @author pavlidis
 */
@Category(SlowTest.class)
public class DatasetCombinerTest {

    private static final Log log = LogFactory.getLog( DatasetCombinerTest.class.getName() );
    private Collection<GeoDataset> gds;

    @Test
    public void testFindGDSGrouping() throws Exception {
        assumeThatResourceIsAvailable( EntrezUtils.ESEARCH );
        Collection<String> result = DatasetCombiner.findGDSforGSE( "GSE674", Settings.getString( "ncbi.efetch.apikey" ) );
        assertEquals( 2, result.size() );
        assertTrue( result.contains( "GDS472" ) && result.contains( "GDS473" ) );
    }

    @Test
    public void testFindGSE13() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE13Short/GDS44.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE13Short/GSE13_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE13Short/GDS52.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        DatasetCombinerTest.log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection<String> c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        }

        assertTrue( result.getCorrespondingSamples( "GSM623" ).contains( "GSM650" ) );
        assertTrue( result.getCorrespondingSamples( "GSM612" ).contains( "GSM638" ) );
        assertEquals( 1, result.getCorrespondingSamples( "GSM618" ).size() );
        assertEquals( 33, numBioMaterials ); // used to be 28
    }

    @Test
    public void testFindGSE267() throws Exception {
        assumeThatResourceIsAvailable( EntrezUtils.ESEARCH );
        Collection<String> result = DatasetCombiner.findGDSforGSE( "GSE267", Settings.getString( "ncbi.efetch.apikey" ) );
        assertEquals( 0, result.size() );
    }

    /*
     * Has multiple platforms, but no GES's are defined
     */
    @Test
    public void testFindGSE3193() throws Exception {

        GeoFamilyParser parser = new GeoFamilyParser();

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE3193Short/GSE3193_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        // GeoDataset gd = parseResult.getDatasets().values().iterator().next();
        GeoSeries gse = parseResult.getSeries().values().iterator().next();

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gse );

        DatasetCombinerTest.log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            it.next();
            // assertTrue( c.size() == 1 );
            numBioMaterials++;
        }
        assertEquals( 57, numBioMaterials ); // note, i'm not at all sure these are right! this used to be 60.
    }

    @Test
    public void testFindGSE469() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE469Short/GDS233.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE469Short/GSE469_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE469Short/GDS234.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        DatasetCombinerTest.log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection<String> c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        }
        // there are some questionable matches, but I can't really tell!
        assertEquals( 54, numBioMaterials );
        assertEquals( 1, result.getCorrespondingSamples( "GSM4301" ).size() );
    }

    @Test
    public void testFindGSE493() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE493Short/GDS215.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE493Short/GSE493_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE493Short/GDS258.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        DatasetCombinerTest.log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection<String> c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        } // there are some questionable matches, but I can't really tell!
        assertEquals( 10, numBioMaterials );
        assertTrue( result.getCorrespondingSamples( "GSM4362" ).contains( "GSM4363" ) );
        assertTrue( result.getCorrespondingSamples( "GSM4366" ).contains( "GSM4368" ) );
        assertEquals( 1, result.getCorrespondingSamples( "GSM4371" ).size() );
    }

    /*
     * Fairly hard case; twelve samples, 3 array design each sample run on each array design
     */
    @Test
    public void testFindGSE611() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE611Short/GDS428.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE611Short/GSE611_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE611Short/GDS429.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE611Short/GDS430.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }
        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 3, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection<String> c = it.next();
            assertEquals( 3, c.size() );
            numBioMaterials++;
        }
        assertEquals( 4, numBioMaterials );

        DatasetCombinerTest.log.debug( result );
    }

    @Test
    public void testFindGSE88() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE88Short/GDS184.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE88Short/GSE88_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        GeoDataset gd = parseResult.getDatasets().values().iterator().next();
        GeoSeries gse = parseResult.getSeries().values().iterator().next();
        gd.getSeries().add( gse );
        gds = new HashSet<>();
        gds.add( gd );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        DatasetCombinerTest.log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection<String> c = it.next();
            assertTrue( c.size() == 1 );
            numBioMaterials++;
        }
        assertEquals( 31, numBioMaterials );

    }

    @Test
    public void testFindGSE91() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE91Short/GDS168.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE91Short/GSE91_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE91Short/GDS169.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        DatasetCombinerTest.log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection<String> c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        }
        assertEquals( 9, numBioMaterials );
        assertTrue( result.getCorrespondingSamples( "GSM2560" ).contains( "GSM2561" ) );
        assertTrue( result.getCorrespondingSamples( "GSM2573" ).contains( "GSM2574" ) );
        assertEquals( 1, result.getCorrespondingSamples( "GSM2564" ).size() );

    }

    @Test
    public void testFindGSECorrespondence() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/twoDatasets/GDS472.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }
        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/twoDatasets/GSE674_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/twoDatasets/GDS473.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );

        gds = parseResult.getDatasets().values();

        assertEquals( 2, gds.size() );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        DatasetCombinerTest.log.debug( result );
        assertEquals( 15, result.size() );

        // these are just all the sample names.
        String[] keys = new String[] { "GSM10354", "GSM10355", "GSM10356", "GSM10359", "GSM10360", "GSM10361",
                "GSM10362", "GSM10363", "GSM10364", "GSM10365", "GSM10366", "GSM10367", "GSM10368", "GSM10369",
                "GSM10370", "GSM10374", "GSM10375", "GSM10376", "GSM10377", "GSM10378", "GSM10379", "GSM10380",
                "GSM10381", "GSM10382", "GSM10383", "GSM10384", "GSM10385", "GSM10386", "GSM10387", "GSM10388" };

        for ( String string : keys ) {
            assertEquals( "Wrong result for " + string + ", expected 2", 2,
                    result.getCorrespondingSamples( string ).size() );
        }
        assertTrue( result.getCorrespondingSamples( "GSM10354" ).contains( "GSM10374" ) );
        assertTrue( result.getCorrespondingSamples( "GSM10374" ).contains( "GSM10354" ) );
    }

    /*
     * This has just a single data set but results in a "no platform assigned" error.
     */
    @Test
    public void testGDS186() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse106Short/GDS186.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse106Short/GSE106.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        GeoDataset gd = parseResult.getDatasets().values().iterator().next();
        GeoSeries gse = parseResult.getSeries().values().iterator().next();
        gd.getSeries().add( gse );
        gds = new HashSet<>();
        gds.add( gd );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        DatasetCombinerTest.log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection<String> c = it.next();
            assertTrue( c.size() == 1 );
            numBioMaterials++;
        }
        assertEquals( 11, numBioMaterials );
    }

    /*
     * A difficult case, lots of singletons.
     */
    @Test
    public void testGSE465() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE465Short/GDS214.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE465Short/GSE465_family.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE465Short/GDS262.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE465Short/GDS263.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE465Short/GDS264.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE465Short/GDS265.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE465Short/GDS270.soft.gz" ).getInputStream() ) ) {
            parser.parse( is );
        }

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 6, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        DatasetCombinerTest.log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection<String> c = it.next();
            assertTrue( "Unexpected group size: " + c.size(),
                    c.size() == 1 || c.size() == 2 || c.size() == 6 || c.size() == 5 );
            numBioMaterials++;
        }
        assertEquals( 30, numBioMaterials );

    }
}
