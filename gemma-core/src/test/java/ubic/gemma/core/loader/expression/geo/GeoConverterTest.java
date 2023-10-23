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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.ClassPathResource;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Unit test for GeoConversion Added extension BaseSpringContextTest as want Taxon Service to be called
 *
 * @author pavlidis
 */
@Category(SlowTest.class)
public class GeoConverterTest extends BaseSpringContextTest {

    private final ByteArrayConverter bac = new ByteArrayConverter();
    @Autowired
    private GeoConverter gc;

    /*
     * Bug 3976: make sure we skip non-expression data sets.
     *
     */
    @Test
    public final void test5C() {
        // GSE35721
        GeoDomainObjectGenerator g = new GeoDomainObjectGenerator();
        GeoSeries series = ( GeoSeries ) g.generate( "GSE35721" ).iterator().next();
        @SuppressWarnings("unchecked")
        Collection<ExpressionExperiment> r = ( Collection<ExpressionExperiment> ) this.gc
                .convert( series );
        assertTrue( r.isEmpty() );
    }

    @Test
    public void testConvertDataDoubles() {
        List<Object> testList = new ArrayList<>();
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

    @Test
    public void testConvertDataIntegers() {
        List<Object> testList = new ArrayList<>();
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
        InputStream is = new GZIPInputStream( new ClassPathResource( "/data/loader/expression/geo/shortGenePix/GSE2221_family.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2221" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /*
     * caused "GSM3059 had no platform assigned"
     *
     */
    @Test
    public void testConvertGSE106() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse106Short/GSE106.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE106" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /*
     * This is one of our longer/slower tests.
     *
     */
    @Test
    @Category(SlowTest.class)
    public void testConvertGSE18Stress() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GSE18.soft.gz" ).getInputStream() );

        GeoFamilyParser parser = new GeoFamilyParser();

        parser.parse( is );

        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS15.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS16.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS17.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS18.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS19.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS20.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS21.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS30.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS31.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS33.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS34.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS35.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS36.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS108.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS111.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS112.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS113.soft.gz" ).getInputStream() );
        parser.parse( is );
        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse18short/GDS115.soft.gz" ).getInputStream() );
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE18" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        ExpressionExperiment result = ( ExpressionExperiment ) ( ( Set<?> ) this.gc.convert( series ) ).iterator()
                .next();

        assertEquals( 156, result.getBioAssays().size() );

    }

    // No SAGE allowed
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE2122SAGE() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse2122shortSage/GSE2122.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2122" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 0, ees.size() ); // SAGE, rejected

    }

    /*
     * Lacks data for some samples (on purpose)
     *
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE29014() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE29014.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap()
                .get( "GSE29014" );
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
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse2982Short/GSE2982_family.soft.gz" ).getInputStream() );
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
        for ( RawExpressionDataVector dedv : ee.getRawExpressionDataVectors() ) {
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

    /*
     * Note: this series has some samples that don't have all the quantitation types.
     */
    @Test
    public void testConvertGSE360() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE360Short/GSE360_family.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE360" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /*
     * Has two species.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE3791() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse3791Short/GSE3791_family.soft.gz" ).getInputStream() );
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

    /*
     * Gets no 'preferred' quantitation type. - it should find one.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE404() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse404Short/GSE404_family.soft.gz" ).getInputStream() );
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
        boolean ok = this.checkQts( ee );
        assertTrue( ok );
    }

    /*
     * See bug 3328 - we don't want to use IMAGE clone IDs
     */
    @Test
    public void testConvertGSE4229IMAGE() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse4229Short/GSE4229.soft.gz" ).getInputStream() );
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

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGse59() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE59Short/GSE59_family.soft.gz" ).getInputStream() );
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
        boolean ok = this.checkQts( ee );

        assertTrue( ok );
    }

    /*
     * Was yielding a 'ArrayDesigns must be converted before datasets'.
     */
    @SuppressWarnings("unchecked")
    @Test
    @Category(SlowTest.class)
    public void testConvertGSE60() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse60Short/GSE60_family.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse60Short/GDS75.soft.gz" ).getInputStream() );
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

    /*
     * NPE in converter, not reproduced here.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE8134() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE8134_family.soft.gz" ).getInputStream() );
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

    /*
     * Case where the same sample can be in multiple series, we had problems with it.
     */
    @Test
    public void testConvertMultiSeriesPerSample() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE3193Short/GSE3193_family.soft.gz" ).getInputStream() );
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
        Map<ArrayDesign, Integer> ads = new HashMap<>();
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
            switch ( ad.getName() ) {
                case "SHAC":
                    assertEquals( 8, count.intValue() ); // ok

                    break;
                case "SVJ":
                    assertEquals( 1, count.intValue() );// ok

                    break;
                case "SVL_SVM_SVN_SVO":
                    assertEquals( 32, count.intValue() );
                    break;
                case "SVC":
                    assertEquals( 44, count.intValue() );
                    break;
                default:
                    fail( "Name was " + ad.getName() );
                    break;
            }
        }
    }

    /*
     * See bug 3163.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertMultiTaxonDatasetGSE7540() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE7540.soft.gz" ).getInputStream() );
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

    @Test
    public void testConvertWithLotsOfPlatforms() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse3500Short/GSE3500_family.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE3500" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /*
     * Ends up with too few vectors, because of a problem with the quantitation type guesser.
     */
    @Test
    public void testFetchAndLoadGSE8294() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE8294_family.soft.gz" ).getInputStream() );
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

    /*
     * Should result in the rejection of 'irrelevant' probes.
     */
    @Test
    public void testGPL6096ExonArray() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GPL6096_family.soft.gz" ).getInputStream() ) ) {

            parser.parse( is );
        }

        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap()
                .get( "GPL6096" );

        gc.setElementLimitForStrictness( 500 );
        Object result = this.gc.convert( platform );
        assertNotNull( result );
    }

    /*
     * bug 3393/1709 - case change in probe names messing things up.
     */
    @Test
    public final void testGSE44903() throws Exception {

        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE44903.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap()
                .get( "GSE44903" );
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

    /*
     * Has only one GDS in GEOs when there really should be two. Bug 1829.
     */
    @SuppressWarnings("unchecked")
    @Test
    @Category(SlowTest.class)
    public final void testGSE8872() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse8872short/GSE8872_family.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/gse8872short/GDS2942.soft.gz" ).getInputStream() );
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

    /*
     * GSE4047 is an example of where some of the samples used have channel 1 and channel 2 taxon different. And thus an
     * exception should be thrown
     */
    @Test
    public void testIllegalArgumentExceptionChannel1Channel2taxonDifferent() throws Exception {
        try {
            InputStream is = new GZIPInputStream(
                    new ClassPathResource( "/data/loader/expression/geo/GSE4047_family.soft.gz" ).getInputStream() );
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap()
                    .get( "GSE4047" );
            DatasetCombiner datasetCombiner = new DatasetCombiner();
            GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
            series.setSampleCorrespondence( correspondence );
            this.gc.convert( series );
            fail();

        } catch ( IllegalArgumentException e ) {
            assertTrue( e.getMessage().startsWith( "Sample has two different organisms; One channel taxon is" ) );
            // assertEquals(
            // "Channel 1 taxon is Danio rerio Channel 2 taxon is Pomacentrus moluccensis Check that is expected for
            // sample GSM104737",
            // e.getMessage() );
        }
    }

    /*
     * Tests that if platform is defined as having multiple organisms but no column can be found that defines the taxon
     * at the probe level then an Exception is thrown
     */
    @Test
    public void testIllegalArgumentExceptionMultipleTaxaOnArrayWithNoOrganismColumn() throws Exception {
        try {
            InputStream is = new GZIPInputStream(
                    new ClassPathResource( "/data/loader/expression/geo/GPL226_family.soft.gz" ).getInputStream() );
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.setProcessPlatformsOnly( true ); // should not be necessary.
            parser.parse( is );
            GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap()
                    .get( "GPL226" );
            // add an extra organism to the platform to make a pretend 2 orgnaism array
            platform.addToOrganisms( "Rattus norvegicus" );
            this.gc.convert( platform );

            // thrown an error
            fail();
        } catch ( IllegalArgumentException e ) {
            // assertEquals(
            // "2 taxon found on platform: Mus musculus: Rattus norvegicus but there is no probe specific taxon Column
            // found for platform GPL226",
            // e.getMessage() );
        }

    }

    /*
     * We should not longer use IMAGE:XXXXX as the sequence name.
     */
    @Test
    public void testImageClones() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GPL226_family.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap()
                .get( "GPL226" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;

        assertNotNull( ad );
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            assertTrue( "Got: " + bs.getName(),
                    !bs.getName().startsWith( "IMAGE" ) || bs.getSequenceDatabaseEntry() == null );
        }
    }

    @Test
    public void testParseGSE18707() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE18707.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap()
                .get( "GSE18707" );
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

    /*
     * bug 3415. The samples do not have the same quantitation types. In some it is "detection pval" and others
     * "raw_value". We would reject "detection pval" but if the qt already has the name "raw_value" we won't. The way
     * the current setup works, we sort of throw up our hands and keep the data, even though it is messed up.
     * <p>
     * This tests that behaviour. If we start rejecting the data, this test will fail (note that rejecting the data is
     * not unreasonable).
     */
    @Test
    public void testParseGSE44625() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE44625.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoParseResult result = ( GeoParseResult ) parser.getResults().iterator().next();

        GeoSeries series = result.getSeries().values().iterator().next();

        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object convert = this.gc.convert( series );
        ExpressionExperiment ee = ( ExpressionExperiment ) ( ( Collection<?> ) convert ).iterator().next();

        assertEquals( 2, ee.getQuantitationTypes().size() );

        assertNotNull( ee.getTaxon() );

        // Confirmation for #908 that we take in the right data from sample characteristics.
        boolean found1 = false;
        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( Characteristic c : ba.getSampleUsed().getCharacteristics() ) {
               // log.info( c );
                String category = c.getCategory();
                if ( !category.equals( "molecular entity" ) && !category.equals( "labelling" ) && !category.equals( "BioSource" ) ) { // we lose these original strings, or they have diff format; not important.
                    assertNotNull( c.getOriginalValue() );
                    assertTrue( c.getOriginalValue().contains( ":" ) ); // for this particular dataset; just a check that we are storing the actual original, not just the second half.
                }
                assertNotNull( category );
                if ( !category.equals( "strain" ) )
                    assertTrue( !c.getValue().contains( ":" ) ); // this is actually allowed if there's : in the value itself...Crl:CD1(ICR)
                if ( category.equals( "weight" ) ) { // some crazy non-standard category.
                    found1 = true;
                }
            }
        }

        assertTrue( found1 );

    }

    /*
     * Ensure that if platform has one taxon then taxon is still set correctly
     */
    @Test
    public void testSingleTaxonOnArrayWithNoOrganismColumn() throws Exception {

        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GPL226_family.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap()
                .get( "GPL226" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;

        assertNotNull( ad );
        Set<Taxon> listPossibleTaxonValues = new HashSet<>();
        BioSequence bs;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            bs = cs.getBiologicalCharacteristic();
            if ( bs != null ) {
                listPossibleTaxonValues.add( bs.getTaxon() );
            }
        }
        assertEquals( 1, listPossibleTaxonValues.size() );
    }

    /*
     * Test splitting
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSplitByPlatform() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE493Short/GSE493_family.soft.gz" ).getInputStream() );
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

    /*
     * Platform has IMAGE:CCCCC in CLONE_ID column, no genbank accessions anywhere.
     */
    @Test
    public final void testWithImageNoGenbank() throws Exception {
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GPL222_family.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap()
                .get( "GPL222" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            assertTrue( bs.getName().startsWith( "IMAGE" ) );
        }

    }

    /*
     * Has image clones.
     */
    @Test
    public final void testWithImages() throws Exception {
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        try ( InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GPL890_family.soft.gz" ).getInputStream() ) ) {

            parser.parse( is );
        }
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap()
                .get( "GPL890" );
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

    @Test
    public final void testMakeTitle() throws Exception {
        GeoConverterImpl gi = new GeoConverterImpl();
        assertEquals( "foo", gi.makeTitle( "foo", null ) );
        assertEquals( "foo - bar", gi.makeTitle( "foo", "bar" ) );
        assertEquals( "Transcriptome comparison of PAX6 ablated mouse beta cells to WT beta cells, "
                        + "ChIP-seq analysis of PAX6 bound sites both in mouse and human beta cell lines (Min6 and EndoC), "
                        + "and ChIP-seq analysis fo histone mark H3K9ac on mouse pancreatic ... - Mus musculus",
                gi.makeTitle(
                        "Transcriptome comparison of PAX6 ablated mouse beta cells to WT beta cells, ChIP-seq analysis of PAX6 "
                                + "bound sites both in mouse and human beta cell lines "
                                + "(Min6 and EndoC), and ChIP-seq analysis fo histone mark H3K9ac on mouse pancreatic beta cells.",
                        "Mus musculus" ) );
        assertTrue( gi.makeTitle(
                "Transcriptome comparison of PAX6 ablated mouse beta cells to WT beta cells, ChIP-seq analysis of PAX6 bound sites "
                        + "both in mouse and human beta cell lines "
                        + "(Min6 and EndoC), and ChIP-seq analysis fo histone mark H3K9ac on mouse pancreatic beta cells.",
                "Mus musculus" ).length() <= 255 );
    }

    private boolean checkQts( ExpressionExperiment ee ) {
        boolean ok = false;
        for ( DesignElementDataVector dedv : ee.getRawExpressionDataVectors() ) {
            QuantitationType qt = dedv.getQuantitationType();
            if ( qt.getIsPreferred() ) {
                ok = true;
                assertEquals( "VALUE", qt.getName() );
            }
        }
        return ok;
    }

}
