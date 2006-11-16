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

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.expression.geo.DatasetCombiner;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.loader.expression.geo.GeoParseResult;
import ubic.gemma.loader.expression.geo.GeoSampleCorrespondence;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class TwoChannelMissingValuesTest extends TestCase {

    GeoConverter gc = new GeoConverter();

    public void testMissingValue() throws Exception {
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
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection ) result ).iterator().next();

        TwoChannelMissingValues tcmv = new TwoChannelMissingValues();

        Collection<DesignElementDataVector> calls = tcmv.computeMissingValues( expExp, 2.0 );

        assertEquals( 500, calls.size() );

        ByteArrayConverter bac = new ByteArrayConverter();

        BioAssayDimension dim = calls.iterator().next().getBioAssayDimension();

        /*
         * Spot check the results. For sample ME-TMZ, ID #27 should be 'true' and 26 should be false.
         */
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

//        System.err.print( "\n" );
//        for ( BioAssay bas : dim.getBioAssays() ) {
//            System.err.print( "\t" + bas );
//        }
//        for ( DesignElementDataVector vector : calls ) {
//            System.err.print( vector.getDesignElement() );
//            byte[] dat = vector.getData();
//            boolean[] row = bac.byteArrayToBooleans( dat );
//            for ( boolean b : row ) {
//                System.err.print( "\t" + b );
//            }
//            System.err.print( "\n" );
//        }

        assertTrue( foundA && foundB );

    }

}
