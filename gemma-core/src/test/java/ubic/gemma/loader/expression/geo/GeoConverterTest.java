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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Unit test for GeoConversion
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
     * Test method for 'ubic.gemma.loader.expression.geo.GeoConverter.convertData(List<String>)'
     */
    public void testConvertDataIntegers() {
        List<Object> testList = new ArrayList<Object>();
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

    public void testConvertGenePix() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/shortGenePix/GSE2221_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2221" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /**
     * caused "GSM3059 had no platform assigned"
     * 
     * @throws Exception
     */
    public void testConvertGSE106() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse106Short/GSE106.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE106" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    @SuppressWarnings("unchecked")
    public void testImageClones() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL226_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL226" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;

        assertNotNull( ad );
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs != null && bs.getName().startsWith( "IMAGE" ) ) {
                return;
            }

        }
        fail( "No IMAGE clones!" );
    }

    /**
     * Has image clones.
     * 
     * @throws Exception
     */
    public final void testWithImages() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL890_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL890" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs != null && bs.getSequence() != null ) {
                return;
            }

        }
        fail( "No sequences!" );
    }

    /**
     * Platform has IMAGE:CCCCC in CLONE_ID column, no genbank accessions anywhere.
     * 
     * @throws Exception
     */
    public final void testWithImageNoGenbank() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL222_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL222" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            assertTrue( bs.getName().startsWith( "IMAGE" ) );
        }

    }

    public void testConvertWithNulls() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gds181Short/GSE96_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE96" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    public void testConvertDataDoubles() {
        List<Object> testList = new ArrayList<Object>();
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

    // We are more relaxed about number format errors; in valid values are put in as Missing (NaN)
    // public void testConvertDataMixed() {
    // List<String> testList = new ArrayList<String>();
    // testList.add( "1" ); // should trigger use of integers
    // testList.add( "2929202e-4" ); // uh-oh
    // testList.add( "-394949.44422" );
    // QuantitationType qt = QuantitationType.Factory.newInstance();
    // qt.setRepresentation( PrimitiveType.INT );
    // try {
    // gc.convertData( testList, qt );
    // fail( "Should have gotten an exception" );
    // } catch ( RuntimeException e ) {
    //
    // }
    // }

}
