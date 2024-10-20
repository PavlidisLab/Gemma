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
package ubic.gemma.core.analysis.preprocess;

import org.hibernate.ObjectNotFoundException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author ptan
 */
@Category(GeoTest.class)
public class MeanVarianceServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private MeanVarianceService meanVarianceService;
    @Autowired
    private GeoService geoService;
    @Autowired
    private DataUpdater dataUpdater;
    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    private AclTestUtils aclTestUtils;

    private ExpressionExperiment ee;

    @After
    public void after() {
        if ( ee != null ) {
            eeService.remove( ee );
        }
    }

    @Test
    @Category(SlowTest.class)
    final public void testServiceCreateTwoColor() throws Exception {
        prepareGSE2892();

        QuantitationType qt = this.createOrUpdateQt( ScaleType.LOG2 );
        qt.setIsNormalized( false );
        quantitationTypeService.update( qt );

        // update ArrayDesign to TWOCOLOR
        Collection<ArrayDesign> aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        ArrayDesign des = aas.iterator().next();
        des.setTechnologyType( TechnologyType.TWOCOLOR );
        arrayDesignService.update( des );

        aclTestUtils.checkHasAcl( des );

        // check that ArrayDesign is the right TechnologyType
        aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        des = aas.iterator().next();
        assertEquals( TechnologyType.TWOCOLOR, des.getTechnologyType() );

        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );

        aclTestUtils.checkEEAcls( ee );
        ee = eeService.thaw( ee );
        assertEquals( 97, ee.getProcessedExpressionDataVectors().size() );

        // convert byte[] to array[]
        // warning: order may have changed
        double[] means = mvr.getMeans();
        double[] variances = mvr.getVariances();
        Arrays.sort( means );
        Arrays.sort( variances );

        int expectedLength = 72; // after filtering
        assertEquals( expectedLength, means.length );
        assertEquals( expectedLength, variances.length );

        int idx = 0;
        assertEquals( -0.34836, means[idx], 0.0001 );
        assertEquals( 0.001569, variances[idx], 0.0001 );

        idx = expectedLength - 1;
        assertEquals( 0.05115, means[idx], 0.0001 );
        assertEquals( 0.12014, variances[idx], 0.0001 );

    }

    @Test
    @Category(SlowTest.class)
    final public void testServiceCreateOneColor() throws Exception {
        prepareGSE2892();

        QuantitationType qt = this.createOrUpdateQt( ScaleType.LOG2 );
        qt.setIsNormalized( false );
        quantitationTypeService.update( qt );

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

        // warning: order may have changed
        double[] means = mvr.getMeans();
        double[] variances = mvr.getVariances();
        Arrays.sort( means );
        Arrays.sort( variances );

        // check sizes
        int expectedMeanVarianceLength = 72;
        int expectedLowessLength = 72; // NAs removed
        assertEquals( expectedMeanVarianceLength, means.length );
        assertEquals( expectedMeanVarianceLength, variances.length );

        // check results
        int idx = 0;
        assertEquals( -0.3484, means[idx], 0.0001 );
        assertEquals( 0.001569, variances[idx], 0.0001 );

        idx = expectedLowessLength - 1;
        assertEquals( 0.05115, means[idx], 0.0001 );
        assertEquals( 0.12014, variances[idx], 0.0001 );

    }

    @Test
    @Category(SlowTest.class)
    final public void testServiceCreateCountData() throws Exception {

        // so it doesn't look for soft files
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

        ExpressionExperiment ee2 = eeService.findByShortName( "GSE29006" );
        if ( ee2 != null )
            eeService.remove( ee2 );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE29006", false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AccessDeniedException e ) {
            // see https://github.com/PavlidisLab/Gemma/issues/206
            Assume.assumeNoException( e );
        } catch ( AlreadyExistsInSystemException e ) {
            throw new IllegalStateException( "Need to remove this data set before test is run" );
        }

        try {
            ee = eeService.thaw( ee );
        } catch ( ObjectNotFoundException e ) {
            Assume.assumeNoException( e );
        }

        this.createOrUpdateQt( ScaleType.COUNT );

        // Load the data from a text file.
        DoubleMatrixReader reader = new DoubleMatrixReader();

        try ( InputStream countData = this.getClass()
                .getResourceAsStream( "/data/loader/expression/flatfileload/GSE29006_expression_count.test.txt" );

                InputStream rpkmData = this.getClass().getResourceAsStream(
                        "/data/loader/expression/flatfileload/GSE29006_expression_RPKM.test.txt" ) ) {
            DoubleMatrix<String, String> countMatrix = reader.read( countData );
            DoubleMatrix<String, String> rpkmMatrix = reader.read( rpkmData );

            List<String> probeNames = countMatrix.getRowNames();

            // we have to find the right generic platform to use.
            ArrayDesign targetArrayDesign = this
                    .getTestPersistentArrayDesign( probeNames, taxonService.findByCommonName( "human" ) );
            targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

            try {
                dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, false );
                fail( "Should have gotten an exception" );
            } catch ( IllegalArgumentException e ) {
                // Expected
            }
            dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, true );
        }

        ee = eeService.thaw( this.ee );

        assertNotNull( ee.getId() );

        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );

        // warning: order may have changed
        double[] means = mvr.getMeans();
        double[] variances = mvr.getVariances();
        if ( means != null ) {
            Arrays.sort( means );
        }
        if ( variances != null ) {
            Arrays.sort( variances );
        }

        // check sizes
        int expectedMeanVarianceLength = 199;
        int expectedLowessLength = 197; // NAs removed
        assert means != null;
        assertEquals( expectedMeanVarianceLength, means.length );
        assert variances != null;
        assertEquals( expectedMeanVarianceLength, variances.length );

        int idx = 0;
        assertEquals( 1.037011, means[idx], 0.0001 );
        assertEquals( 0.00023724336, variances[idx], 0.000001 );

        idx = expectedLowessLength - 1;
        assertEquals( 15.23313, means[idx], 0.0001 );
        assertEquals( 4.84529, variances[idx], 0.0001 );
    }

    @Test
    @Category(SlowTest.class)
    final public void testServiceCreateExistingEe() throws Exception {
        prepareGSE2892();

        // no MeanVarianceRelation exists yet
        ee = eeService.load( ee.getId() );
        assertNotNull( ee );
        assertNotNull( ee.getId() );
        MeanVarianceRelation oldMvr = ee.getMeanVarianceRelation();
        assertNull( oldMvr );
        Long oldEeId = ee.getId();

        // first time we create a MeanVarianceRelation
        ee = eeService.load( ee.getId() );
        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );
        assertEquals( oldEeId, ee.getId() );
        assertNotNull( mvr );
        oldMvr = mvr;

        // now that the MeanVarianceRelation exists
        // try loading ee again by just using an eeId
        // and see if we get a no Session error
        ee = eeService.load( ee.getId() );
        mvr = meanVarianceService.create( ee, true );
        assertEquals( oldEeId, ee.getId() );
        assertNotSame( oldMvr, mvr );
    }

    private QuantitationType createOrUpdateQt( ScaleType scale ) {
        QuantitationType qt = eeService.getPreferredQuantitationType( ee );
        if ( qt == null ) {
            qt = QuantitationType.Factory.newInstance();
            qt.setName( "testQt" );
            qt.setScale( scale );
            qt.setIsPreferred( true );
            qt.setRepresentation( PrimitiveType.DOUBLE );
            qt.setIsMaskedPreferred( false );
            qt.setIsRatio( false );
            qt.setIsNormalized( false );
            qt.setIsBackground( false );
            qt.setGeneralType( GeneralType.QUANTITATIVE );
            qt.setType( StandardQuantitationType.AMOUNT );
            qt.setIsBackgroundSubtracted( false );
            qt.setIsBatchCorrected( false );
            qt.setIsRecomputedFromRawData( false );
            qt = quantitationTypeService.create( qt );
            ee.getQuantitationTypes().add( qt );
        } else {
            qt.setScale( scale );
            quantitationTypeService.update( qt );
        }
        return qt;
    }

    private void prepareGSE2892() throws Exception {
        Collection<?> results;
        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gse2982Short" ) ) );
            results = geoService.fetchAndLoad( "GSE2982", false, false, false );
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ( Collection<ExpressionExperiment> ) e.getData() ).iterator().next();
            Assume.assumeNoException( e );
            return;
        }

        // scale is not correctly recorded.
        ee = ( ExpressionExperiment ) results.iterator().next();
        QuantitationType qt = this.createOrUpdateQt( ScaleType.LOG2 );
        assertNotNull( qt );

        // not parsed properly
        ArrayDesign ad = eeService.getArrayDesignsUsed( ee ).iterator().next();
        ad.setTechnologyType( TechnologyType.TWOCOLOR );
        arrayDesignService.update( ad );

        qt.setIsNormalized( true );
        quantitationTypeService.update( qt );

        // important bit, need to createProcessedVectors manually before using it
        processedExpressionDataVectorService.createProcessedDataVectors( ee, false );
    }
}
