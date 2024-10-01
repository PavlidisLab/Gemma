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
package ubic.gemma.core.analysis.preprocess;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.core.loader.expression.geo.*;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;
import static ubic.gemma.persistence.util.ByteArrayUtils.byteArrayToBooleans;

/**
 * @author pavlidis
 */
public class TwoChannelMissingValuesTest extends BaseSpringContextTest {

    private GeoConverter gc = new GeoConverterImpl();

    @Autowired
    private TwoChannelMissingValues tcmv;

    @Autowired
    private ExpressionExperimentService eeService;

    @Test
    @Category(SlowTest.class)
    public void testMissingValue() throws Exception {
        ExpressionExperiment old = eeService.findByShortName( "GSE2221" );
        if ( old != null )
            eeService.remove( old );

        InputStream is = new GZIPInputStream( new ClassPathResource( "/data/loader/expression/geo/shortGenePix/GSE2221_family.soft.gz" ).getInputStream() );
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

        boolean foundA = false;
        boolean foundB = false;
        for ( DesignElementDataVector vector : calls ) {
            if ( vector.getDesignElement().getName().equals( "26" ) ) {
                byte[] dat = vector.getData();
                boolean[] row = byteArrayToBooleans( dat );
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
                boolean[] row = byteArrayToBooleans( dat );
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

    @Test
    @Category(SlowTest.class)
    final public void testMissingValueGSE523() throws Exception {
        ExpressionExperiment old = eeService.findByShortName( "GSE523" );
        if ( old != null )
            eeService.remove( old );

        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE523_family.soft.gz" ).getInputStream() );
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
     */
    @Test
    @Category(SlowTest.class)
    public void testMissingValueGSE11017() throws Exception {

        ExpressionExperiment old = eeService.findByShortName( "GSE11017" );
        if ( old != null )
            eeService.remove( old );

        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE11017.soft.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap()
                .get( "GSE11017" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );

        assertNotNull( result );
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();

        expExp = persisterHelper.persist( expExp, persisterHelper.prepare( expExp ) );

        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );
        // print( calls );
        assertEquals( 20, calls.size() );

        boolean hasNewQT = false;
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( expExp );
        for ( QuantitationType qt : qts ) {
            if ( qt.getType().equals( StandardQuantitationType.PRESENTABSENT ) ) {
                hasNewQT = true;
                break;
            }
        }

        assertTrue( hasNewQT );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( calls );

        ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData();
        assertTrue( missingValues.getQuantitationTypes().iterator().next().getDescription()
                .contains( "signal threshold" ) );
        Boolean[][] mm = missingValues.getRawMatrix();
        boolean hasPresent = false;
        for ( Boolean[] aMm : mm ) {
            for ( Boolean anAMm : aMm ) {
                if ( anAMm ) {
                    hasPresent = true;
                    break;
                }
            }
        }
        assertTrue( hasPresent );

    }

    /**
     * GSE56 is corrupt: there is no Channel 1 signal value in the data file.
     */
    @Test
    @Category(SlowTest.class)
    public void testMissingValueGSE56() throws Exception {
        ExpressionExperiment old = eeService.findByShortName( "GSE56" );
        if ( old != null )
            eeService.remove( old );

        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/expression/geo/GSE56Short/GSE56_family.soft.gz" ).getInputStream() );
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
     * Debug code.
     */
    @SuppressWarnings("unused")
    private void print( Collection<RawExpressionDataVector> calls ) {
        BioAssayDimension dim = calls.iterator().next().getBioAssayDimension();

        System.err.print( "\n" );
        for ( BioAssay bas : dim.getBioAssays() ) {
            System.err.print( "\t" + bas );
        }
        System.err.print( "\n" );
        for ( DesignElementDataVector vector : calls ) {
            System.err.print( vector.getDesignElement() );
            byte[] dat = vector.getData();
            boolean[] row = byteArrayToBooleans( dat );
            for ( boolean b : row ) {
                System.err.print( "\t" + b );
            }
            System.err.print( "\n" );

        }
    }
}
