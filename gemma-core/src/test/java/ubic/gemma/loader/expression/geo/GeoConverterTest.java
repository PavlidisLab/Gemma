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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Unit test for GeoConversion Added extension BaseSpringContextTest as want Taxon Service to be called
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoConverterTest extends BaseSpringContextTest {

    @Autowired
    private GeoConverter gc;

    private ByteArrayConverter bac = new ByteArrayConverter();

    private boolean skipSlowTests = true;

    private static boolean doneSetup = false;

    @Before
    public void setUp() throws Exception {
        if ( doneSetup ) return;
        super.executeSqlScript( "/script/sql/add-fish-taxa.sql", true );

        doneSetup = true;
    }

    /**
     * quantitation type problem. See bug 1760
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test5091() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        try (InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE5091Short/GSE5091_family.soft.gz" ) );) {

            parser.parse( is );
        }

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE5091" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );

        ExpressionExperiment ee = ees.iterator().next();
        for ( QuantitationType qt : ee.getQuantitationTypes() ) {
            if ( qt.getName().equals( "VALUE" ) ) {
                assertEquals( PrimitiveType.DOUBLE, qt.getRepresentation() );
                return;
            }
        }
        fail( "Didn't find the 'value' quantitation type" );
    }

    /**
     * GSE2388 is an example of where the array and sample taxon do not match. This test checks that the biomaterial and
     * array taxons are set correctly.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    @Transactional
    public void testArrayTaxonDifferentToSampleTaxon() throws Exception {

        Taxon rainbowTrout = taxonService.findByAbbreviation( "omyk" );
        assertNotNull( rainbowTrout );
        Taxon atlanticSalm = taxonService.findByAbbreviation( "ssal" );
        assertNotNull( atlanticSalm );
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE2388_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2388" );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL966" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        // assert that the biomaterials have been set as one taxon
        Object seriesResult = gc.convert( series );
        assertNotNull( seriesResult );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) seriesResult;
        ExpressionExperiment exper = ees.iterator().next();
        Collection<BioAssay> bioassays = exper.getBioAssays();
        BioMaterial material = bioassays.iterator().next().getSampleUsed();
        Taxon taxon = material.getSourceTaxon();
        assertEquals( "Oncorhynchus kisutch", taxon.getScientificName() );

        // assert that the platform is another taxon

        Object resultPlatForm = gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) resultPlatForm;
        assertNotNull( ad );
        Set<Taxon> taxa = new HashSet<Taxon>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {

            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs != null ) {
                assertNotNull( bs.getTaxon() );
                log.info( bs.getTaxon() );
                taxa.add( bs.getTaxon() );
            }
        }
        // can be empty taxon if the probe does not have a sequence which is why taxon size is 3.
        assertEquals( 2, taxa.size() );
        assertTrue( taxa.contains( rainbowTrout ) );
        assertTrue( taxa.contains( atlanticSalm ) );

    }

    @Test
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

    /*
     * Test method for 'ubic.gemma.loader.expression.geo.GeoConverter.convertData(List<String>)'
     */
    @Test
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

    @Test
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
    @Test
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

    /**
     * This is one of our longer/slower tests.
     * 
     * @throws Exception
     */
    @Test
    public void testConvertGSE18Stress() throws Exception {
        if ( this.skipSlowTests ) {
            return;
        }

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GSE18.soft.gz" ) );

        GeoFamilyParser parser = new GeoFamilyParser();

        parser.parse( is );

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS15.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS16.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS17.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS18.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS19.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS20.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS21.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS30.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS31.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS33.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS34.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS35.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS36.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS108.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS111.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS112.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS113.soft.gz" ) );
        parser.parse( is );
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse18short/GDS115.soft.gz" ) );
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE18" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        ExpressionExperiment result = ( ExpressionExperiment ) ( ( Set<?> ) this.gc.convert( series ) ).iterator()
                .next();

        assertEquals( 156, result.getBioAssays().size() );

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE2122SAGE() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse2122shortSage/GSE2122.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2122" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );

        ExpressionExperiment ee = ees.iterator().next();
        assertEquals( 4, ee.getBioAssays().size() );
        assertEquals( 0, ee.getRawExpressionDataVectors().size() );

    }

    /**
     * Lacks data for some samples (on purpose)
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE29014() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE29014.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE29014" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE2982() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse2982Short/GSE2982_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2982" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
        ExpressionExperiment ee = ees.iterator().next();
        boolean ok = false;
        for ( DesignElementDataVector dedv : ee.getRawExpressionDataVectors() ) {
            QuantitationType qt = dedv.getQuantitationType();
            if ( qt.getIsPreferred() ) {
                ok = true;
                assertEquals( "VALUE", qt.getName() );
                assertEquals( StandardQuantitationType.AMOUNT, qt.getType() );
                assertTrue( qt.getIsRatio() );
            }
        }
        assertTrue( ok );
    }

    /**
     * Note: this series has some samples that don't have all the quantitation types.
     * 
     * @throws Exception
     */
    @Test
    public void testConvertGSE360() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE360Short/GSE360_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE360" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /**
     * Has two species.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE3791() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse3791Short/GSE3791_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE3791" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 2, ees.size() );
    }

    /**
     * Gets no 'preferred' quantitation type. - it should find one.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE404() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse404Short/GSE404_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE404" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );

        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;

        ExpressionExperiment ee = ees.iterator().next();
        boolean ok = false;
        for ( DesignElementDataVector dedv : ee.getRawExpressionDataVectors() ) {
            QuantitationType qt = dedv.getQuantitationType();
            if ( qt.getIsPreferred() ) {
                ok = true;
                assertEquals( "VALUE", qt.getName() );
            }
        }

        assertTrue( ok );
    }

    /**
     * See bug 3328 - we don't want to use IMAGE clone IDs
     * 
     * @throws Exception
     */
    @Test
    public void testConvertGSE4229IMAGE() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse4229Short/GSE4229.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE4229" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        @SuppressWarnings("unchecked")
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        ExpressionExperiment ee = ees.iterator().next();
        ArrayDesign platform = ee.getBioAssays().iterator().next().getArrayDesignUsed();

        BioSequence seq = platform.getCompositeSequences().iterator().next().getBiologicalCharacteristic();
        assertNotNull( seq.getSequenceDatabaseEntry() );
        String acc = seq.getSequenceDatabaseEntry().getAccession();
        assertEquals( "Genbank", seq.getSequenceDatabaseEntry().getExternalDatabase().getName() );
        assertTrue( !acc.startsWith( "IMAGE" ) );
    }

    /**
     * Problem with QT being interpreted as String instead of Double.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE5091() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        try (InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE5091Short/GSE5091_family.soft.gz" ) );) {

            parser.parse( is );
        }
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE5091" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );

        ExpressionExperiment ee = ees.iterator().next();

        Collection<QuantitationType> quantitationTypes = ee.getQuantitationTypes();

        for ( QuantitationType quantitationType : quantitationTypes ) {
            // log.info(quantitationType);
            if ( quantitationType.getName().equals( "VALUE" ) ) {
                /*
                 * Here's the problem. Of course it works fine...
                 */
                assertEquals( PrimitiveType.DOUBLE, quantitationType.getRepresentation() );
                assertTrue( quantitationType.getIsPreferred() );
                return;
            }
        }

        fail( "Expected to find 'VALUE' with type Double" );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGse59() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE59Short/GSE59_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE59" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        // GeoValues values = series.getValues();
        // System.err.print( values );
        Object result = this.gc.convert( series );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        ExpressionExperiment ee = ees.iterator().next();
        boolean ok = false;
        for ( DesignElementDataVector dedv : ee.getRawExpressionDataVectors() ) {
            QuantitationType qt = dedv.getQuantitationType();

            if ( qt.getIsPreferred() ) {
                ok = true;
                assertEquals( "VALUE", qt.getName() );
                // ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( ee, dedv.getBioAssayDimension(), qt
                // );
                // System.err.println( qt );
                // System.err.print( mat );
            }
        }

        assertTrue( ok );
    }

    /**
     * Was yielding a 'ArrayDesigns must be converted before datasets'.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE60() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse60Short/GSE60_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse60Short/GDS75.soft.gz" ) );
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE60" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
        ExpressionExperiment ee = ees.iterator().next();
        assertEquals( 133, ee.getBioAssays().size() );
        // used to be more but we reject mote qts now.
        assertEquals( 480, ee.getRawExpressionDataVectors().size() );
    }

    /**
     * NPE in converter, not reproduced here.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE8134() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE8134_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE8134" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
    }

    /**
     * Case where the same sample can be in multiple series, we had problems with it.
     * 
     * @throws Exception
     */
    @Test
    public void testConvertMultiSeriesPerSample() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE3193Short/GSE3193_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE3193" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        ExpressionExperiment ee = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();
        // assertEquals( 85, ee.getBioAssays().size() );
        Map<ArrayDesign, Integer> ads = new HashMap<ArrayDesign, Integer>();
        for ( BioAssay b : ee.getBioAssays() ) {
            if ( ads.containsKey( b.getArrayDesignUsed() ) ) {
                ads.put( b.getArrayDesignUsed(), ads.get( b.getArrayDesignUsed() ) + 1 );
            } else {
                ads.put( b.getArrayDesignUsed(), 1 );
            }
        }
        assertEquals( 4, ads.size() );
        for ( ArrayDesign ad : ads.keySet() ) {
            Integer count = ads.get( ad );
            if ( ad.getName().equals( "SHAC" ) ) {
                assertEquals( 8, count.intValue() ); // ok
            } else if ( ad.getName().equals( "SVJ" ) ) {
                assertEquals( 1, count.intValue() );// ok
            } else if ( ad.getName().equals( "SVL_SVM_SVN_SVO" ) ) {
                assertEquals( 32, count.intValue() );
            } else if ( ad.getName().equals( "SVC" ) ) {
                assertEquals( 44, count.intValue() );
            } else {
                fail( "Name was " + ad.getName() );
            }
        }
    }

    /**
     * See bug 3163.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertMultiTaxonDatasetGSE7540() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE7540.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE7540" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;

        // Should get one for chimp, one for human.
        assertEquals( 2, ees.size() );

        boolean found1 = false, found2 = false;
        for ( ExpressionExperiment ee : ees ) {
            if ( ee.getBioAssays().size() == 15 ) {
                found1 = true;
            } else if ( ee.getBioAssays().size() == 22 ) {
                found2 = true;
            }
        }

        assertTrue( "Failed to set up the chimp data", found1 );
        assertTrue( "Failed to set up the human data", found2 );

    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertMultiTaxonPlatformGSE28843() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE28843.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE28843" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
        Taxon primaryTaxon = ees.iterator().next().getBioAssays().iterator().next().getArrayDesignUsed()
                .getPrimaryTaxon();
        assertEquals( "salmonid", primaryTaxon.getCommonName() );
    }

    @Test
    public void testConvertWithLotsOfPlatforms() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse3500Short/GSE3500_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE3500" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /**
     * Ends up with too few vectors, because of a problem with the quantitation type guesser.
     * 
     * @throws Exception
     */
    @Test
    public void testFetchAndLoadGSE8294() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE8294_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE8294" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Set<?> result = ( Set<?> ) this.gc.convert( series );
        ExpressionExperiment e = ( ExpressionExperiment ) result.iterator().next();
        assertEquals( 66, e.getRawExpressionDataVectors().size() );
    }

    /**
     * Test logic to evaluate a primary array taxon Either from platform taxon, common parent taxon or probe taxon.
     * 
     * @throws Exception
     */
    @Test
    public final void testGetPrimaryArrayTaxon() throws Exception {
        Collection<Taxon> platformTaxa = new HashSet<Taxon>();
        Collection<String> probeTaxa = new ArrayList<String>();
        Taxon salmonid = taxonService.findByCommonName( "salmonid" );
        Taxon rainbowTrout = taxonService.findByAbbreviation( "omyk" );
        Taxon atlanticSalm = taxonService.findByAbbreviation( "ssal" );

        assertNotNull( rainbowTrout );
        assertNotNull( atlanticSalm );

        atlanticSalm.setParentTaxon( salmonid );
        rainbowTrout.setParentTaxon( salmonid );
        Taxon human = taxonService.findByCommonName( "human" );

        platformTaxa.add( atlanticSalm );
        probeTaxa.add( "ssal" );
        probeTaxa.add( "omyk" );
        probeTaxa.add( "ssal" );
        // test get primary taxon from the array design platform if only one
        Taxon primaryTaxon = this.gc.getPrimaryArrayTaxon( platformTaxa, probeTaxa );
        assertEquals( "atlantic salmon", primaryTaxon.getCommonName() );
        // test that can work out parent taxon
        platformTaxa.add( rainbowTrout );
        Taxon primaryTaxonTwo = this.gc.getPrimaryArrayTaxon( platformTaxa, probeTaxa );
        assertEquals( "salmonid", primaryTaxonTwo.getCommonName() );

        // test that if no common parent taxon take most common taxon on probe
        platformTaxa.add( human );
        Taxon primaryTaxonThree = this.gc.getPrimaryArrayTaxon( platformTaxa, probeTaxa );
        assertEquals( "atlantic salmon", primaryTaxonThree.getCommonName() );

    }

    /**
     * Should result in the rejection of 'irrelevant' probes.
     * 
     * @throws Exception
     */
    @Test
    public void testGPL6096ExonArray() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        try (InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL6096_family.soft.gz" ) );) {

            parser.parse( is );
        }

        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL6096" );

        gc.setElementLimitForStrictness( 500 );
        Object result = this.gc.convert( platform );
        assertNotNull( result );
        // ArrayDesign ad = ( ArrayDesign ) result;
        // FIXME currently we reject probes, so this count is different.
        // assertEquals( 168, ad.getCompositeSequences().size() );
    }

    /**
     * bug 3393/1709 - case change in probe names messing things up.
     * 
     * @throws Exception
     */
    @Test
    public final void testGSE44903() throws Exception {

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE44903.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE44903" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Set<?> result = ( Set<?> ) this.gc.convert( series );
        ExpressionExperiment e = ( ExpressionExperiment ) result.iterator().next();

        /*
         * This is the current condition, because the probes are not matched up.
         */
        assertEquals( 0, e.getRawExpressionDataVectors().size() );
    }

    /**
     * Has only one GDS in GEOs when there really should be two. Bug 1829.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void testGSE8872() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse8872short/GSE8872_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse8872short/GDS2942.soft.gz" ) );
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE8872" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
    }

    /**
     * GSE4047 is an example of where some of the samples used have channel 1 and channel 2 taxon different. And thus an
     * exception should be thrown
     * 
     * @throws Exception
     */
    @Test
    public void testIllegalArgumentExceptionChannel1Channel2taxonDifferent() throws Exception {
        try {
            InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                    "/data/loader/expression/geo/GSE4047_family.soft.gz" ) );
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get(
                    "GSE4047" );
            DatasetCombiner datasetCombiner = new DatasetCombiner();
            GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
            series.setSampleCorrespondence( correspondence );
            this.gc.convert( series );
            fail();

        } catch ( IllegalArgumentException e ) {
            assertTrue( e.getMessage().startsWith( "Sample has two different organisms; One channel taxon is" ) );
            // assertEquals(
            // "Channel 1 taxon is Danio rerio Channel 2 taxon is Pomacentrus moluccensis Check that is expected for sample GSM104737",
            // e.getMessage() );
        }
    }

    /**
     * Tests that if platform is defined as having multiple organisms but no column can be found that defines the taxon
     * at the probe level then an Exception is thrown
     * 
     * @throws Exception
     */
    @Test
    public void testIllegalArgumentExceptionMultipleTaxaOnArrayWithNoOrganismColumn() throws Exception {
        try {
            InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                    "/data/loader/expression/geo/GPL226_family.soft.gz" ) );
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.setProcessPlatformsOnly( true ); // should not be necessary.
            parser.parse( is );
            GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                    "GPL226" );
            // add an extra organism to the platform to make a pretend 2 orgnaism array
            platform.addToOrganisms( "Rattus norvegicus" );
            this.gc.convert( platform );

            // thrown an error
            fail();
        } catch ( IllegalArgumentException e ) {
            // assertEquals(
            // "2 taxon found on platform: Mus musculus: Rattus norvegicus but there is no probe specific taxon Column found for platform GPL226",
            // e.getMessage() );
        }

    }

    /**
     * We should not longer use IMAGE:XXXXX as the sequence name.
     * 
     * @throws Exception
     */
    @Test
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
            assertTrue( "Got: " + bs.getName(), !bs.getName().startsWith( "IMAGE" )
                    || bs.getSequenceDatabaseEntry() == null );
        }
    }

    /**
     * Method to test that an array design can have multiple taxa stored against it and that if abbreviations used as
     * probe names mapped to the scientific names correctly if the abbreviation is stored in DB.
     * 
     * @throws Exception
     */
    @Test
    public void testMultipleTaxaIdentifiedBYAbbreviationsOnArrayWithOrganismColumn() throws Exception {

        Taxon rainbowTroat = taxonService.findByAbbreviation( "omyk" );
        Taxon whiteFish = taxonService.findByAbbreviation( "cclu" );
        Taxon rainbowSmelt = taxonService.findByAbbreviation( "omor" );
        Taxon atlanticSalm = taxonService.findByAbbreviation( "ssal" );

        assertNotNull( atlanticSalm );

        gc = this.getBean( GeoConverter.class ); // prototype bean.

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL2899_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        // parse only the plaform
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );

        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL2899" );
        Object result = gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;

        assertNotNull( ad );
        Set<Taxon> taxa = new HashSet<Taxon>();
        BioSequence bs = null;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {

            bs = cs.getBiologicalCharacteristic();
            if ( bs != null ) {
                assertNotNull( bs.getTaxon() );
                taxa.add( bs.getTaxon() );
            }
        }
        assertEquals( 4, taxa.size() );

        // original file has five taxa, test file just kept four.
        assertTrue( taxa.contains( atlanticSalm ) );
        assertTrue( taxa.contains( rainbowTroat ) );
        assertTrue( taxa.contains( whiteFish ) );
        assertTrue( taxa.contains( rainbowSmelt ) );
    }

    @Test
    public void testParseGSE18707() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE18707.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE18707" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Set<?> result = ( Set<?> ) this.gc.convert( series );
        ExpressionExperiment e = ( ExpressionExperiment ) result.iterator().next();
        assertEquals( 100, e.getRawExpressionDataVectors().size() );
        assertEquals( 1, e.getQuantitationTypes().size() ); // this is normal, before any processing.

        QuantitationType qt = e.getQuantitationTypes().iterator().next();
        assertEquals( "Processed Affymetrix Rosetta intensity values", qt.getDescription() );

    }

    /**
     * bug 3415. The samples do not have the same quantitation types. In some it is "detection pval" and others
     * "raw_value". We would reject "detection pval" but if the qt already has the name "raw_value" we won't. The way
     * the current setup works, we sort of throw up our hands and keep the data, even though it is messed up.
     * <p>
     * This tests that behaviour. If we start rejecting the data, this test will fail (note that rejecting the data is
     * not unreasonable).
     * 
     * @throws Exception
     */
    @Test
    public void testParseGSE44625() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE44625.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoParseResult result = ( GeoParseResult ) parser.getResults().iterator().next();

        GeoSeries series = result.getSeries().values().iterator().next();

        series.getValues();

        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object convert = this.gc.convert( series );
        ExpressionExperiment ee = ( ExpressionExperiment ) ( ( Collection<?> ) convert ).iterator().next();

        assertEquals( 2, ee.getQuantitationTypes().size() );
    }

    /**
     * Ensure that if platform has one taxon then taxon is still set correctly
     */
    @Test
    public void testSingleTaxonOnArrayWithNoOrganismColumn() throws Exception {

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL226_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL226" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;

        assertNotNull( ad );
        Set<Taxon> listPossibleTaxonValues = new HashSet<Taxon>();
        BioSequence bs = null;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            bs = cs.getBiologicalCharacteristic();
            if ( bs != null ) {
                listPossibleTaxonValues.add( bs.getTaxon() );
            }
        }
        assertEquals( 1, listPossibleTaxonValues.size() );
    }

    /**
     * Test splitting
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSplitByPlatform() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE493Short/GSE493_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE493" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        this.gc.setSplitByPlatform( true );
        Object result = this.gc.convert( series );
        assertNotNull( result );

        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 2, ees.size() );
        for ( ExpressionExperiment ee : ees ) {
            // 4 on one platform, 10 on the other. This is a bit sloppy but good enuf for now.
            assertTrue( ee.getBioAssays().size() == 4 || ee.getBioAssays().size() == 10 );
        }
    }

    /**
     * Platform has IMAGE:CCCCC in CLONE_ID column, no genbank accessions anywhere.
     * 
     * @throws Exception
     */
    @Test
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

    /**
     * Has image clones.
     * 
     * @throws Exception
     */
    @Test
    public final void testWithImages() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        try (InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL890_family.soft.gz" ) );) {

            parser.parse( is );
        }
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

}
