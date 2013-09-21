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
package ubic.gemma.analysis.preprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.DatasetCombiner;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoConverterImpl;
import ubic.gemma.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.loader.expression.geo.GeoParseResult;
import ubic.gemma.loader.expression.geo.GeoSampleCorrespondence;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class TwoChannelMissingValuesTest extends BaseSpringContextTest {

    GeoConverter gc = new GeoConverterImpl();

    @Autowired
    private TwoChannelMissingValues tcmv;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private DesignElementDataVectorService dedvService;

    @Before
    public void setUp() throws Exception {
        executeSqlScript( "/script/sql/add-fish-taxa.sql", false );
    }

    /**
     * @throws Exception
     */
    @Test
    public void testMissingValue() throws Exception {
        ExpressionExperiment old = eeService.findByShortName( "GSE2221" );
        if ( old != null ) eeService.delete( old );
        // FIXME: Getthis test passingin release process (mvn release:perform fails) could not get release process to
        // pass with these tests (failed on final release couldn't reproduce)

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
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();

        expExp = persisterHelper.persist( expExp, persisterHelper.prepare( expExp ) );
        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );
        assertEquals( 500, calls.size() );
        BioAssayDimension dim = calls.iterator().next().getBioAssayDimension();

        // Spot check the results. For sample ME-TMZ, ID #27 should be 'true' and 26 should be false.
        ByteArrayConverter bac = new ByteArrayConverter();

        boolean foundA = false;
        boolean foundB = false;
        for ( DesignElementDataVector vector : calls ) {
            if ( vector.getDesignElement().getName().equals( "26" ) ) {
                byte[] dat = vector.getData();
                boolean[] row = bac.byteArrayToBooleans( dat );
                int i = 0;
                for ( BioAssay bas : dim.getBioAssays() ) {
                    if ( bas.getName().equals( "expression array ME-TMZ" ) ) {
                        assertTrue( !row[i] );
                        foundA = true;
                    }
                    i++;
                }
            }
            if ( vector.getDesignElement().getName().equals( "27" ) ) {
                byte[] dat = vector.getData();
                boolean[] row = bac.byteArrayToBooleans( dat );
                int i = 0;
                for ( BioAssay bas : dim.getBioAssays() ) {
                    if ( bas.getName().equals( "expression array ME-TMZ" ) ) {
                        assertTrue( row[i] );
                        foundB = true;
                    }
                    i++;
                }
            }
        }
        assertTrue( foundA && foundB );

    }

    /**
     * @param calls
     */
    private void print( Collection<RawExpressionDataVector> calls ) {
        ByteArrayConverter bac = new ByteArrayConverter();
        BioAssayDimension dim = calls.iterator().next().getBioAssayDimension();

        System.err.print( "\n" );
        for ( BioAssay bas : dim.getBioAssays() ) {
            System.err.print( "\t" + bas );
        }
        System.err.print( "\n" );
        for ( DesignElementDataVector vector : calls ) {
            System.err.print( vector.getDesignElement() );
            byte[] dat = vector.getData();
            boolean[] row = bac.byteArrayToBooleans( dat );
            for ( boolean b : row ) {
                System.err.print( "\t" + b );
            }
            System.err.print( "\n" );

        }
    }

    @Test
    final public void testMissingValueGSE523() throws Exception {
        ExpressionExperiment old = eeService.findByShortName( "GSE523" );
        if ( old != null ) eeService.delete( old );
        // FIXME: Getthis test passingin release process (mvn release:perform fails) could not get release process to
        // pass with these tests (failed on final release couldn't reproduce)

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE523_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE523" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();

        expExp = persisterHelper.persist( expExp, persisterHelper.prepare( expExp ) );
        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );

        assertEquals( 30, calls.size() );

    }

    /**
     * Was giving all missing values.
     * 
     * @throws Exception
     */
    @Test
    public void testMissingValueGSE11017() throws Exception {

        ExpressionExperiment old = eeService.findByShortName( "GSE11017" );
        if ( old != null ) eeService.delete( old );

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE11017.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE11017" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );

        assertNotNull( result );
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();

        expExp = persisterHelper.persist( expExp, persisterHelper.prepare( expExp ) );
        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );
        print( calls );
        assertEquals( 20, calls.size() );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( calls );

        ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData();
        missingValues.getQuantitationTypes().iterator().next().getDescription().contains( "signal threshold" );
        Boolean[][] mm = missingValues.getRawMatrix();
        boolean hasPresent = false;
        for ( int i = 0; i < mm.length; i++ ) {
            for ( int j = 0; j < mm[i].length; j++ ) {
                if ( mm[i][j] ) {
                    hasPresent = true;
                    break;
                }
            }
        }
        assertTrue( hasPresent );

    }

    /**
     * GSE56 is corrupt: there is no Channel 1 signal value in the data file.
     * 
     * @throws Exception
     */

    @Test
    public void testMissingValueGSE56() throws Exception {
        ExpressionExperiment old = eeService.findByShortName( "GSE56" );
        if ( old != null ) eeService.delete( old );
        // * FIXME: Get this test passing in release process (mvn release:perform fails) could not get release process
        // to pass with these tests (failed on final release couldn't reproduce)

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE56Short/GSE56_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE56" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );

        assertNotNull( result );
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();

        expExp = persisterHelper.persist( expExp, persisterHelper.prepare( expExp ) );
        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );

        assertEquals( 10, calls.size() );
    }

    /**
     * GSE56 is corrupt: there is no Channel 1 signal value in the data file.
     * 
     * @throws Exception
     */

    @Test
    public void testMissingValueGSE5091() throws Exception {
        ExpressionExperiment old = eeService.findByShortName( "GSE5091" );
        if ( old != null ) eeService.delete( old );
        // * FIXME: Get this test passing in release process (mvn release:perform fails) could not get release process
        // to pass with these tests (failed on final release couldn't reproduce)

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE5091Short/GSE5091_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE5091" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );

        gc = this.getBean( GeoConverter.class );

        Object result = this.gc.convert( series );

        assertNotNull( result );
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();

        expExp = persisterHelper.persist( expExp, persisterHelper.prepare( expExp ) );
        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );

        assertEquals( 10, calls.size() );
    }
}
