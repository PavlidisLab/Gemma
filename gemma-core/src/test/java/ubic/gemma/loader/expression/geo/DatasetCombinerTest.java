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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.geo.model.GeoDataset;
import ubic.gemma.loader.expression.geo.model.GeoSeries;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DatasetCombinerTest extends TestCase {

    private static Log log = LogFactory.getLog( DatasetCombinerTest.class.getName() );
    Collection<GeoDataset> gds;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
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

    // todo: add test of findGSECorrespondence( GeoSeries series ) when there is no dataset.

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.DatsetCombiner.findGSECorrespondence(Collection<GeoDataset>)'
     */
    public void testFindGSECorrespondence() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/twoDatasets/GDS472.soft.gz" ) );
        parser.parse( is );
        is.close();
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/twoDatasets/GSE674_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/twoDatasets/GDS473.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );

        gds = parseResult.getDatasets().values();

        assertEquals( 2, gds.size() );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );
        assertEquals( 15, result.size() );

        // these are just all the sample names.
        String[] keys = new String[] { "GSM10354", "GSM10355", "GSM10356", "GSM10359", "GSM10360", "GSM10361",
                "GSM10362", "GSM10363", "GSM10364", "GSM10365", "GSM10366", "GSM10367", "GSM10368", "GSM10369",
                "GSM10370", "GSM10374", "GSM10375", "GSM10376", "GSM10377", "GSM10378", "GSM10379", "GSM10380",
                "GSM10381", "GSM10382", "GSM10383", "GSM10384", "GSM10385", "GSM10386", "GSM10387", "GSM10388" };

        for ( int i = 0; i < keys.length; i++ ) {
            String string = keys[i];
            assertEquals( "Wrong result for " + keys[i] + ", expected 2", 2, result.getCorrespondingSamples( string )
                    .size() );
        }
        assertTrue( result.getCorrespondingSamples( "GSM10354" ).contains( "GSM10374" ) );
        assertTrue( result.getCorrespondingSamples( "GSM10374" ).contains( "GSM10354" ) );
    }

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.DatsetCombiner.findGSECorrespondence(Collection<GeoDataset>)'
     * This is a really hard case because the sample names are very similar. It has 8 samples, each on three arrays.
     */
    public void testFindGSECorrespondenceThreeDatasets() throws Exception {

        // GSE479 GDS242-244 fits the bill (MG-U74A,B and C (v2)

        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS242.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GSE479_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS243.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/threeDatasets/GDS244.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );

        String[] keys = new String[] { "GSM4045", "GSM4047", "GSM4049", "GSM4051", "GSM4053", "GSM4055", "GSM4057",
                "GSM4059", "GSM4061", "GSM4063", "GSM4065", "GSM4067", "GSM4069", "GSM4071", "GSM4073", "GSM4075",
                "GSM4077", "GSM4081", "GSM4079", "GSM4083", "GSM4085", "GSM4087", "GSM4089", "GSM4091" };

        assert keys.length == 24;
        gds = parseResult.getDatasets().values();

        // These are 'loner' samples that aren't part of this series.
        // "GSM4076","GSM4052","GSM4070","GSM4046","GSM4082", "GSM4088","GSM4058","GSM4064","GSM4048",
        // "GSM4078","GSM4072","GSM4054","GSM4090", "GSM4060","GSM4084","GSM4066", "GSM4086","GSM4062",
        // "GSM4056","GSM4080", "GSM4068","GSM4044","GSM4050","GSM4074",

        /**
         * <pre>
         *                                                            GSM4045     PGA-MFD-CtrPD1-1aAv2-s2a
         *                                                            GSM4047     PGA-MFD-CtrPD1-1aBv2-s2
         *                                                            GSM4049     PGA-MFD-CtrPD1-1aCv2-s2
         *                                                            GSM4051     PGA-MFD-CtrPD1-2aAv2-s2b
         *                                                            GSM4053     PGA-MFD-CtrPD1-2aBv2-s2
         *                                                            GSM4055     PGA-MFD-CtrPD1-2aCv2-s2
         *                                                            GSM4057     PGA-MFD-CtrPD5-1aAv2-s2
         *                                                            GSM4059     PGA-MFD-CtrPD5-1aBv2-s2
         *                                                            GSM4061     PGA-MFD-CtrPD5-1aCv2-s2
         *                                                            GSM4063     PGA-MFD-CtrPD5-2aAv2-s2
         *                                                            GSM4065     PGA-MFD-CtrPD5-2aBv2-s2
         *                                                            GSM4067     PGA-MFD-CtrPD5-2aCv2-s2
         *                                                            GSM4069     PGA-MFD-MutantPD1-1aAv2-s2b
         *                                                            GSM4071     PGA-MFD-MutantPD1-1aBv2-s2
         *                                                            GSM4073     PGA-MFD-MutantPD1-1aCv2-s2
         *                                                            GSM4075     PGA-MFD-MutantPD1-2aAv2-s2a
         *                                                            GSM4077     PGA-MFD-MutantPD1-2aBv2-s2
         *                                                            GSM4079     PGA-MFD-MutantPD1-2aCv2-s2
         *                                                            GSM4081     PGA-MFD-MutantPD5-1aAv2-s2
         *                                                            GSM4083     PGA-MFD-MutantPD5-1aBv2-s2
         *                                                            GSM4085     PGA-MFD-MutantPD5-1aCv2-s2
         *                                                            GSM4087     PGA-MFD-MutantPD5-2aAv2-s2
         *                                                            GSM4089     PGA-MFD-MutantPD5-2aBv2-s2
         *                                                            GSM4091     PGA-MFD-MutantPD5-2aCv2-s2
         * </pre>
         */

        assertEquals( 3, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );
        log.debug( result );
        assertEquals( 9, result.size() ); // used to be 8..

        for ( int i = 0; i < keys.length; i++ ) {
            String string = keys[i];
            assertNotNull( "Got null for " + string, result.getCorrespondingSamples( string ) );
            // assertTrue( "Wrong result for " + keys[i] + ", expected 3, got "
            // + result.getCorrespondingSamples( string ).size(),
            // result.getCorrespondingSamples( string ).size() == 3 );
        }
        assertTrue( result.getCorrespondingSamples( "GSM4051" ).contains( "GSM4053" ) );
        assertTrue( result.getCorrespondingSamples( "GSM4083" ).contains( "GSM4085" ) );

    }

    /**
     * Fairly hard case; twelve samples, 3 array design each sample run on each array design
     * 
     * @throws Exception
     */
    public void testFindGSE611() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE611Short/GDS428.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE611Short/GSE611_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE611Short/GDS429.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE611Short/GDS430.soft.gz" ) );
        parser.parse( is );
        is.close();
        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 3, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertEquals( 3, c.size() );
            numBioMaterials++;
        }
        assertEquals( 4, numBioMaterials );

        log.debug( result );
    }

    /**
     * Really hard case.
     * 
     * @throws Exception
     */
    public void testFindGSE1133Human() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse1133Short/GDS594.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse1133Short/GSE1133_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse1133Short/GDS596.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        }
        assertEquals( 159, numBioMaterials ); // used to be 158

    }

    /**
     * @throws Exception
     */
    public void testFindGSE91() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE91Short/GDS168.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE91Short/GSE91_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE91Short/GDS169.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        }
        assertEquals( 9, numBioMaterials );
        assertTrue( result.getCorrespondingSamples( "GSM2560" ).contains( "GSM2561" ) );
        assertTrue( result.getCorrespondingSamples( "GSM2573" ).contains( "GSM2574" ) );
        assertEquals( 1, result.getCorrespondingSamples( "GSM2564" ).size() );

    }

    /**
     * @throws Exception
     */
    public void testFindGSE13() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE13Short/GDS44.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE13Short/GSE13_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE13Short/GDS52.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        }

        assertTrue( result.getCorrespondingSamples( "GSM623" ).contains( "GSM650" ) );
        assertTrue( result.getCorrespondingSamples( "GSM612" ).contains( "GSM638" ) );
        assertEquals( 1, result.getCorrespondingSamples( "GSM618" ).size() );
        assertEquals( 33, numBioMaterials ); // used to be 28
    }

    /**
     * @throws Exception
     */
    public void testFindGSE469() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE469Short/GDS233.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE469Short/GSE469_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE469Short/GDS234.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        }
        // there are some questionable matches, but I can't really tell!
        assertEquals( 54, numBioMaterials );
        assertEquals( 1, result.getCorrespondingSamples( "GSM4301" ).size() );
    }

    /**
     * A difficult case, lots of singletons.
     * 
     * @throws Exception
     */
    public void testGSE465() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE465Short/GDS214.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE465Short/GSE465_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE465Short/GDS262.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE465Short/GDS263.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE465Short/GDS264.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE465Short/GDS265.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE465Short/GDS270.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 6, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( "Unexpected group size: " + c.size(), c.size() == 1 || c.size() == 2 || c.size() == 6
                    || c.size() == 5 );
            numBioMaterials++;
        }
        assertEquals( 30, numBioMaterials );

    }

    public void testFindGSE493() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE493Short/GDS215.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE493Short/GSE493_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE493Short/GDS258.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        gds = parseResult.getDatasets().values();
        assertEquals( 2, gds.size() );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( c.size() == 1 || c.size() == 2 );
            numBioMaterials++;
        } // there are some questionable matches, but I can't really tell!
        assertEquals( 10, numBioMaterials );
        assertTrue( result.getCorrespondingSamples( "GSM4362" ).contains( "GSM4363" ) );
        assertTrue( result.getCorrespondingSamples( "GSM4366" ).contains( "GSM4368" ) );
        assertEquals( 1, result.getCorrespondingSamples( "GSM4371" ).size() );
    }

    public void testFindGSE88() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE88Short/GDS184.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE88Short/GSE88_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        GeoDataset gd = parseResult.getDatasets().values().iterator().next();
        GeoSeries gse = parseResult.getSeries().values().iterator().next();
        gd.getSeries().add( gse );
        gds = new HashSet<GeoDataset>();
        gds.add( gd );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( c.size() == 1 );
            numBioMaterials++;
        }
        assertEquals( 31, numBioMaterials );

    }

    /**
     * Has multiple platforms, but no GES's are defined.
     * 
     * @throws Exception
     */
    public void testFindGSE3193() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE3193Short/GSE3193_family.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        // GeoDataset gd = parseResult.getDatasets().values().iterator().next();
        GeoSeries gse = parseResult.getSeries().values().iterator().next();

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gse );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            it.next();
            // assertTrue( c.size() == 1 );
            numBioMaterials++;
        }
        assertEquals( 57, numBioMaterials ); // note, i'm not at all sure these are right! this used to be 60.
    }

    /**
     * This has just a single data set but results in a "no platform assigned" error.
     * 
     * @throws Exception
     */
    public void testGDS186() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse106Short/GDS186.soft.gz" ) );
        parser.parse( is );
        is.close();

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse106Short/GSE106.soft.gz" ) );
        parser.parse( is );
        is.close();

        GeoParseResult parseResult = ( ( GeoParseResult ) parser.getResults().iterator().next() );
        GeoDataset gd = parseResult.getDatasets().values().iterator().next();
        GeoSeries gse = parseResult.getSeries().values().iterator().next();
        gd.getSeries().add( gse );
        gds = new HashSet<GeoDataset>();
        gds.add( gd );

        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence result = datasetCombiner.findGSECorrespondence( gds );

        log.debug( result );

        Iterator<Set<String>> it = result.iterator();
        int numBioMaterials = 0;
        while ( it.hasNext() ) {
            Collection c = it.next();
            assertTrue( c.size() == 1 );
            numBioMaterials++;
        }
        assertEquals( 11, numBioMaterials );
    }

}
