/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.core.loader.expression;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.loader.util.TestUtils;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author paul
 */
public class DataUpdaterTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private ProcessedExpressionDataVectorService dataVectorService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionDataMatrixService dataMatrixService;

    private ArrayDesign targetArrayDesign;

    @After
    public void tearDown() {
        ExpressionExperiment e1 = experimentService.findByShortName( "GSE29006" );
        if ( e1 != null ) {
            experimentService.remove( e1 );
        }

        ExpressionExperiment e2 = experimentService.findByShortName( "GSE19166" );
        if ( e2 != null ) {
            experimentService.remove( e2 );
        }

        ExpressionExperiment e3 = experimentService.findByShortName( "GSE37646" );
        if ( e3 != null ) {
            experimentService.remove( e3 );
        }

        if ( targetArrayDesign != null )
            arrayDesignService.remove( targetArrayDesign );

    }

    @Test
    @Category(SlowTest.class)
    public void testAddData() throws Exception {

        /*
         * Load a regular data set that has no data. Platform is (basically) irrelevant.
         */
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
        ExpressionExperiment ee;

        // These tests were failing due to too many requests being made to geo, this is a workaround
        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException ex ) {
            Thread.currentThread().interrupt();
        }

        try {
            // RNA-seq data.
            Collection<?> results = geoService.fetchAndLoad( "GSE37646", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            // log.warn( "Test skipped because GSE37646 was not removed from the system prior to test" );
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }

        ee = experimentService.thawLite( ee );

        List<BioAssay> bioAssays = new ArrayList<>( ee.getBioAssays() );
        assertEquals( 31, bioAssays.size() );

        List<BioMaterial> bms = new ArrayList<>();
        for ( BioAssay ba : bioAssays ) {

            bms.add( ba.getSampleUsed() );
        }

        targetArrayDesign = this.getTestPersistentArrayDesign( 100, true );

        DoubleMatrix<CompositeSequence, BioMaterial> rawMatrix = new DenseDoubleMatrix<>(
                targetArrayDesign.getCompositeSequences().size(), bms.size() );
        /*
         * make up some fake data on another platform, and match it to those samples
         */
        for ( int i = 0; i < rawMatrix.rows(); i++ ) {
            for ( int j = 0; j < rawMatrix.columns(); j++ ) {
                rawMatrix.set( i, j, ( i + 1 ) * ( j + 1 ) * Math.random() / 100.0 );
            }
        }

        List<CompositeSequence> probes = new ArrayList<>( targetArrayDesign.getCompositeSequences() );

        rawMatrix.setRowNames( probes );
        rawMatrix.setColumnNames( bms );

        QuantitationType qt = this.makeQt( true );

        ExpressionDataDoubleMatrix data = new ExpressionDataDoubleMatrix( ee, qt, rawMatrix );

        assertNotNull( data.getBestBioAssayDimension() );
        assertEquals( rawMatrix.columns(), data.getBestBioAssayDimension().getBioAssays().size() );
        assertEquals( probes.size(), data.getMatrix().rows() );

        /*
         * Replace it.
         */
        ee = dataUpdater.replaceData( ee, targetArrayDesign, data );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );
        }

        ee = experimentService.thaw( ee );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );
        }

        assertEquals( 100, ee.getRawExpressionDataVectors().size() );

        for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
            assertTrue( v.getQuantitationType().getIsPreferred() );
        }

        assertEquals( 100, ee.getProcessedExpressionDataVectors().size() );

        Collection<DoubleVectorValueObject> processedDataArrays = dataVectorService.getProcessedDataArrays( ee );

        for ( DoubleVectorValueObject v : processedDataArrays ) {
            assertEquals( 31, v.getBioAssays().size() );
        }

        /*
         * Test adding data (non-preferred)
         */
        qt = this.makeQt( false );
        ExpressionDataDoubleMatrix moreData = new ExpressionDataDoubleMatrix( ee, qt, rawMatrix );
        ee = dataUpdater.addData( ee, targetArrayDesign, moreData );

        ee = experimentService.thaw( ee );
        try {
            // add preferred data twice.
            dataUpdater.addData( ee, targetArrayDesign, data );
            fail( "Should have gotten an exception" );
        } catch ( IllegalArgumentException e ) {
            // okay.
        }

        dataUpdater.deleteData( ee, qt );
    }

    /*
     * More realistic test of RNA seq. GSE19166. Test re-loading as well.
     */
    @Test
    @Category(SlowTest.class)
    public void testLoadRNASeqData() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        ExpressionExperiment ee;

        // These tests were failing due to too many requests being made to geo, this is a workaround
        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException ex ) {
            Thread.currentThread().interrupt();
        }

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE19166", false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }

        ee = experimentService.thaw( ee );

        // Load the data from a text file.
        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrix<String, String> countMatrix;
        DoubleMatrix<String, String> rpkmMatrix;
        try (InputStream countData = this.getClass()
                .getResourceAsStream( "/data/loader/expression/flatfileload/GSE19166_expression_count.test.txt" );

                InputStream rpkmData = this.getClass().getResourceAsStream(
                        "/data/loader/expression/flatfileload/GSE19166_expression_RPKM.test.txt" )) {
            countMatrix = reader.read( countData );
            rpkmMatrix = reader.read( rpkmData );
        }

        List<String> probeNames = countMatrix.getRowNames();

        assertEquals( 199, probeNames.size() );

        // we have to find the right generic platform to use.
        targetArrayDesign = this
                .getTestPersistentArrayDesign( probeNames, taxonService.findByCommonName( "human" ) );
        targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

        assertEquals( 199, targetArrayDesign.getCompositeSequences().size() );

        // Main step.
        dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, false );
        ee = experimentService.load( ee.getId() );
        ee = experimentService.thaw( ee );

        // should have: log2cpm, counts, rpkm, and counts-masked ('preferred')
        assertEquals( 4, ee.getQuantitationTypes().size() );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );
        }

        assertNotNull( ee.getNumberOfDataVectors() );
        assertEquals( 199, ee.getNumberOfDataVectors().intValue() );

        // GSM475204 GSM475205 GSM475206 GSM475207 GSM475208 GSM475209
        // 3949585 3929008 3712314 3693219 3574068 3579631

        ExpressionDataDoubleMatrix mat = dataMatrixService.getProcessedExpressionDataMatrix( ee );
        assertEquals( 199, mat.rows() );

        TestUtils.assertBAs( ee, targetArrayDesign, "GSM475204", 3949585 );

        assertEquals( 3 * 199, ee.getRawExpressionDataVectors().size() );

        assertEquals( 199, ee.getProcessedExpressionDataVectors().size() );

        for ( ProcessedExpressionDataVector v : ee.getProcessedExpressionDataVectors() ) {
            assertNotNull( "Vector rank was not populated (max)", v.getRankByMax() );
            assertNotNull( "Vector rank was not populated (mean)", v.getRankByMean() );
        }

        Collection<DoubleVectorValueObject> processedDataArrays = dataVectorService.getProcessedDataArrays( ee );
        assertEquals( 199, processedDataArrays.size() );

        for ( DoubleVectorValueObject v : processedDataArrays ) {
            assertEquals( 6, v.getBioAssays().size() );

        }
        assertTrue( !dataVectorService.getProcessedDataVectors( experimentService.load( ee.getId() ) ).isEmpty() );

        // Call it again to test that we don't leak QTs
        dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, false );
        ee = experimentService.load( ee.getId() );
        ee = this.experimentService.thawLite( ee );
        assertEquals( 4, ee.getQuantitationTypes().size() );

    }

    /*
     * Test case where some samples cannot be used.
     */
    @Test
    @Category(SlowTest.class)
    public void testLoadRNASeqDataWithMissingSamples() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        ExpressionExperiment ee = experimentService.findByShortName( "GSE29006" );
        if ( ee != null ) {
            experimentService.remove( ee );
        }

        assertTrue( experimentService.findByShortName( "GSE29006" ) == null );

        // These tests were failing due to too many requests being made to geo, this is a workaround
        try {
            Thread.sleep( 1000 );
        } catch ( InterruptedException ex ) {
            Thread.currentThread().interrupt();
        }

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE29006", false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            throw new IllegalStateException( "Need to remove this data set before test is run" );
        }

        ee = experimentService.thaw( ee );

        // Load the data from a text file.
        DoubleMatrixReader reader = new DoubleMatrixReader();

        try (InputStream countData = this.getClass()
                .getResourceAsStream( "/data/loader/expression/flatfileload/GSE29006_expression_count.test.txt" );

                InputStream rpkmData = this.getClass().getResourceAsStream(
                        "/data/loader/expression/flatfileload/GSE29006_expression_RPKM.test.txt" )) {
            DoubleMatrix<String, String> countMatrix = reader.read( countData );
            DoubleMatrix<String, String> rpkmMatrix = reader.read( rpkmData );

            List<String> probeNames = countMatrix.getRowNames();

            // we have to find the right generic platform to use.
            targetArrayDesign = this
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

        /*
         * Check
         */
        ee = experimentService.thaw( ee );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );
        }

        ExpressionDataDoubleMatrix mat = dataMatrixService.getProcessedExpressionDataMatrix( ee );
        assertEquals( 199, mat.rows() );
        assertTrue( mat.getQuantitationTypes().iterator().next().getName().startsWith( "log2cpm" ) );

        assertEquals( 4, ee.getBioAssays().size() );

        assertEquals( 199 * 3, ee.getRawExpressionDataVectors().size() );

        assertEquals( 199, ee.getProcessedExpressionDataVectors().size() );

        Collection<DoubleVectorValueObject> processedDataArrays = dataVectorService.getProcessedDataArrays( ee );

        assertEquals( 199, processedDataArrays.size() );

        TestUtils.assertBAs( ee, targetArrayDesign, "GSM718709", 320383 );

        for ( DoubleVectorValueObject v : processedDataArrays ) {
            assertEquals( 4, v.getBioAssays().size() );
        }

    }

    private QuantitationType makeQt( boolean preferred ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "foo" );
        qt.setDescription( "bar" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setScale( ScaleType.LINEAR );
        qt.setIsBackground( false );
        qt.setIsRatio( false );
        qt.setIsBackgroundSubtracted( true );
        qt.setIsNormalized( true );
        qt.setIsMaskedPreferred( true );
        qt.setIsPreferred( preferred );
        qt.setIsBatchCorrected( false );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsBatchCorrected( false );
        qt.setIsRecomputedFromRawData( true );
        return qt;
    }
}
