/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.DataUpdater;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author ptan
 * @version $Id$
 */
public class MeanVarianceServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private MeanVarianceService meanVarianceService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService eeService;

    private ExpressionExperiment ee;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    private static ByteArrayConverter bac = new ByteArrayConverter();

    @After
    public void after() {
        try {

            eeService.delete( ee );

        } catch ( Exception e ) {

        }
    }

    @Test
    final public void testServiceCreateTwoColor() throws Exception {

        ee = eeService.findByShortName( "GSE2982" );
        if ( ee != null ) {
            eeService.delete( ee ); // might work, but array designs might be in the way.
        }

        geoService
                .setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "gse2982Short" ) ) );

        Collection<?> results = geoService.fetchAndLoad( "GSE2982", false, false, true, false );

        ee = ( ExpressionExperiment ) results.iterator().next();

        // important bit, need to createProcessedVectors manually before using it
        ee = processedExpressionDataVectorService.createProcessedDataVectors( ee );

        // update ArrayDesign to TWOCOLOR
        Collection<ArrayDesign> aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        ArrayDesign des = aas.iterator().next();
        des.setTechnologyType( TechnologyType.TWOCOLOR );
        arrayDesignService.update( des );

        // check that ArrayDesign is the right TechnologyType
        aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        des = aas.iterator().next();
        assertEquals( TechnologyType.TWOCOLOR, des.getTechnologyType() );

        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );

        // convert byte[] to array[]
        // warning: order may have changed
        double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
        double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );
        double[] lowessX = bac.byteArrayToDoubles( mvr.getLowessX() );
        double[] lowessY = bac.byteArrayToDoubles( mvr.getLowessY() );
        Arrays.sort( means );
        Arrays.sort( variances );
        Arrays.sort( lowessX );
        Arrays.sort( lowessY );

        int expectedLength = 97;
        assertEquals( expectedLength, means.length );
        assertEquals( expectedLength, variances.length );
        assertEquals( expectedLength, lowessX.length );
        assertEquals( expectedLength, lowessY.length );

        int idx = 0;
        assertEquals( 11.26844, means[idx], 0.0001 );
        assertEquals( 0.001446, variances[idx], 0.0001 );
        assertEquals( 11.2684, lowessX[idx], 0.0001 );
        assertEquals( 0.02119, lowessY[idx], 0.0001 );

        idx = expectedLength - 1;
        assertEquals( 14.374, means[idx], 0.0001 );
        assertEquals( 0.9971, variances[idx], 0.0001 );
        assertEquals( 14.374, lowessX[idx], 0.0001 );
        assertEquals( 0.446115, lowessY[idx], 0.0001 );
    }

    @Test
    final public void testServiceCreateOneColor() throws Exception {
        ee = eeService.findByShortName( "GSE2982" );
        if ( ee != null ) {
            eeService.delete( ee ); // might work, but array designs might be in the way.
        }

        geoService
                .setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "gse2982Short" ) ) );

        Collection<?> results = geoService.fetchAndLoad( "GSE2982", false, false, true, false );

        ee = ( ExpressionExperiment ) results.iterator().next();

        // important bit, need to createProcessedVectors manually before using it
        ee = processedExpressionDataVectorService.createProcessedDataVectors( ee );

        // update ArrayDesign to ONECOLOR
        Collection<ArrayDesign> aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        ArrayDesign des = aas.iterator().next();
        des.setTechnologyType( TechnologyType.ONECOLOR );
        arrayDesignService.update( des );

        // check that ArrayDesign is the right TechnologyType
        aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        des = aas.iterator().next();
        assertEquals( TechnologyType.ONECOLOR, des.getTechnologyType() );

        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );

        // convert byte[] to array[]
        // warning: order may have changed
        double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
        double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );
        double[] lowessX = bac.byteArrayToDoubles( mvr.getLowessX() );
        double[] lowessY = bac.byteArrayToDoubles( mvr.getLowessY() );
        Arrays.sort( means );
        Arrays.sort( variances );
        Arrays.sort( lowessX );
        Arrays.sort( lowessY );

        // check sizes
        int expectedMeanVarianceLength = 97;
        int expectedLowessLength = 31; // NAs removed
        assertEquals( expectedMeanVarianceLength, means.length );
        assertEquals( expectedMeanVarianceLength, variances.length );
        assertEquals( expectedLowessLength, lowessX.length );
        assertEquals( expectedLowessLength, lowessY.length );

        // check results
        int idx = 0;
        assertEquals( 5.74575, means[idx], 0.0001 );
        assertEquals( 0.0, variances[idx], 0.0001 );
        assertEquals( 5.74575, lowessX[idx], 0.0001 );
        assertEquals( 0.0, lowessY[idx], 0.0001 );

        idx = expectedLowessLength - 1;
        assertEquals( 15.33165, means[idx], 0.0001 );
        assertEquals( 0.77093, variances[idx], 0.0001 );
        assertEquals( 15.3372, lowessX[idx], 0.0001 );
        assertEquals( 0.07358, lowessY[idx], 0.0001 );
    }

    @Test
    final public void testServiceCreateCountData() throws Exception {

        // so it doesn't look for soft files
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

        ExpressionExperiment ee = eeService.findByShortName( "GSE29006" );
        if ( ee != null ) {
            eeService.delete( ee );
        }

        assertTrue( eeService.findByShortName( "GSE29006" ) == null );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE29006", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            throw new IllegalStateException( "Need to delete this data set before test is run" );
        }

        ee = eeService.thaw( ee );

        // Load the data from a text file.
        DoubleMatrixReader reader = new DoubleMatrixReader();

        InputStream countData = this.getClass().getResourceAsStream(
                "/data/loader/expression/flatfileload/GSE29006_expression_count.test.txt" );
        DoubleMatrix<String, String> countMatrix = reader.read( countData );

        InputStream rpkmData = this.getClass().getResourceAsStream(
                "/data/loader/expression/flatfileload/GSE29006_expression_RPKM.test.txt" );
        DoubleMatrix<String, String> rpkmMatrix = reader.read( rpkmData );

        List<String> probeNames = countMatrix.getRowNames();

        // we have to find the right generic platform to use.
        ArrayDesign targetArrayDesign = this.getTestPersistentArrayDesign( probeNames,
                taxonService.findByCommonName( "human" ) );
        targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

        try {
            dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, false );
            fail( "Should have gotten an exception" );
        } catch ( IllegalArgumentException e ) {
            // Expected
        }
        dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, true );

        ExpressionExperiment updatedee = eeService.thaw( ee );

        assertNotNull( updatedee.getId() );

        MeanVarianceRelation mvr = meanVarianceService.create( updatedee, true );

        // convert byte[] to array[]
        // warning: order may have changed
        double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
        double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );
        double[] lowessX = bac.byteArrayToDoubles( mvr.getLowessX() );
        double[] lowessY = bac.byteArrayToDoubles( mvr.getLowessY() );
        Arrays.sort( means );
        Arrays.sort( variances );
        Arrays.sort( lowessX );
        Arrays.sort( lowessY );

        // check sizes
        int expectedMeanVarianceLength = 199;
        int expectedLowessLength = 197; // NAs removed
        assertEquals( expectedMeanVarianceLength, means.length );
        assertEquals( expectedMeanVarianceLength, variances.length );
        assertEquals( expectedLowessLength, lowessX.length );
        assertEquals( expectedLowessLength, lowessY.length );

        int idx = 0;
        assertEquals( 1.037011, means[idx], 0.0001 );
        assertEquals( 0.00023724336, variances[idx], 0.000001 );
        assertEquals( 1.03701, lowessX[idx], 0.0001 );
        assertEquals( 0.02774, lowessY[idx], 0.0001 );

        idx = expectedLowessLength - 1;
        assertEquals( 15.23313, means[idx], 0.0001 );
        assertEquals( 4.84529, variances[idx], 0.0001 );
        assertEquals( 15.59225, lowessX[idx], 0.0001 );
        assertEquals( 0.96647, lowessY[idx], 0.0001 );
    }

}
