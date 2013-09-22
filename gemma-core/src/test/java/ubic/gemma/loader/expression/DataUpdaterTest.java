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

package ubic.gemma.loader.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.DataUpdater;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

/**
 * @author paul
 * @version $Id$
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
            experimentService.delete( e1 );
        }

        ExpressionExperiment e2 = experimentService.findByShortName( "GSE19166" );
        if ( e2 != null ) {
            experimentService.delete( e2 );
        }

        ExpressionExperiment e3 = experimentService.findByShortName( "GSE37646" );
        if ( e3 != null ) {
            experimentService.delete( e3 );
        }

        if ( targetArrayDesign != null ) arrayDesignService.remove( targetArrayDesign );
    }

    /**
     * @throws Exception
     */
    @Test
    public void testAddData() throws Exception {

        /*
         * Load a regular data set that has no data. Platform is (basically) irrelevant.
         */
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath() ) );
        ExpressionExperiment ee;

        // ExpressionExperiment oldee = experimentService.findByShortName( "GSE37646" );
        // if ( oldee != null ) experimentService.delete( oldee ); // maybe okay?

        try {
            // RNA-seq data.
            Collection<?> results = geoService.fetchAndLoad( "GSE37646", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            // log.warn( "Test skipped because GSE37646 was not removed from the system prior to test" );
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }

        ee = experimentService.thawLite( ee );

        List<BioAssay> bioAssays = new ArrayList<BioAssay>( ee.getBioAssays() );
        assertEquals( 31, bioAssays.size() );

        List<BioMaterial> bms = new ArrayList<BioMaterial>();
        for ( BioAssay ba : bioAssays ) {

            bms.add( ba.getSampleUsed() );
        }

        targetArrayDesign = getTestPersistentArrayDesign( 100, true );

        DoubleMatrix<CompositeSequence, BioMaterial> rawMatrix = new DenseDoubleMatrix<CompositeSequence, BioMaterial>(
                targetArrayDesign.getCompositeSequences().size(), bms.size() );
        /*
         * make up some fake data on another platform, and match it to those samples
         */
        for ( int i = 0; i < rawMatrix.rows(); i++ ) {
            for ( int j = 0; j < rawMatrix.columns(); j++ ) {
                rawMatrix.set( i, j, ( i + 1 ) * ( j + 1 ) * Math.random() / 100.0 );
            }
        }

        List<CompositeSequence> probes = new ArrayList<CompositeSequence>( targetArrayDesign.getCompositeSequences() );

        rawMatrix.setRowNames( probes );
        rawMatrix.setColumnNames( bms );

        QuantitationType qt = makeQt( true );

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

        /*
         * Check
         */
        ExpressionExperiment updatedee = experimentService.thaw( ee );

        for ( BioAssay ba : updatedee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );
        }

        assertEquals( 100, updatedee.getRawExpressionDataVectors().size() );

        for ( RawExpressionDataVector v : updatedee.getRawExpressionDataVectors() ) {
            assertTrue( v.getQuantitationType().getIsPreferred() );
        }

        assertEquals( 100, updatedee.getProcessedExpressionDataVectors().size() );

        Collection<DoubleVectorValueObject> processedDataArrays = dataVectorService.getProcessedDataArrays( updatedee );

        for ( DoubleVectorValueObject v : processedDataArrays ) {
            assertEquals( 31, v.getBioAssays().size() );
        }

        /*
         * Test adding data (non-preferred)
         */
        qt = makeQt( false );
        ExpressionDataDoubleMatrix moreData = new ExpressionDataDoubleMatrix( updatedee, qt, rawMatrix );
        ee = dataUpdater.addData( updatedee, targetArrayDesign, moreData );

        updatedee = experimentService.thaw( ee );
        try {
            // add preferred data twice.
            dataUpdater.addData( updatedee, targetArrayDesign, data );
            fail( "Should have gotten an exception" );
        } catch ( IllegalArgumentException e ) {
            // okay.
        }

        dataUpdater.deleteData( updatedee, qt );
    }

    /**
     * More realistic test of RNA seq. GSE19166
     * 
     * @throws Exception
     */
    @Test
    public void testLoadRNASeqData() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        ExpressionExperiment ee;
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE19166", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }

        ee = experimentService.thaw( ee );

        // Load the data from a text file.
        DoubleMatrixReader reader = new DoubleMatrixReader();

        InputStream countData = this.getClass().getResourceAsStream(
                "/data/loader/expression/flatfileload/GSE19166_expression_count.test.txt" );
        DoubleMatrix<String, String> countMatrix = reader.read( countData );

        InputStream rpkmData = this.getClass().getResourceAsStream(
                "/data/loader/expression/flatfileload/GSE19166_expression_RPKM.test.txt" );
        DoubleMatrix<String, String> rpkmMatrix = reader.read( rpkmData );

        List<String> probeNames = countMatrix.getRowNames();

        assertEquals( 199, probeNames.size() );

        // we have to find the right generic platform to use.
        targetArrayDesign = this.getTestPersistentArrayDesign( probeNames, taxonService.findByCommonName( "human" ) );
        targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

        assertEquals( 199, targetArrayDesign.getCompositeSequences().size() );

        dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, false );

        /*
         * Check
         */
        ExpressionExperiment updatedee = experimentService.thaw( ee );

        for ( BioAssay ba : updatedee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );
        }

        // GSM475204 GSM475205 GSM475206 GSM475207 GSM475208 GSM475209
        // 3949585 3929008 3712314 3693219 3574068 3579631

        ExpressionDataDoubleMatrix mat = dataMatrixService.getProcessedExpressionDataMatrix( updatedee );
        assertEquals( 199, mat.rows() );
        Double[] column = mat.getColumn( 0 );
        double sum = Descriptive.sum( new DoubleArrayList( ArrayUtils.toPrimitive( column ) ) );
        assertEquals( 3949585, sum, 0.01 );

        boolean found = false;
        for ( BioAssay ba : updatedee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );

            assertEquals( 36, ba.getSequenceReadLength().intValue() );

            if ( ba.getDescription().contains( "GSM475204" ) ) {
                assertEquals( 3949585, ba.getSequenceReadCount().intValue() );
                found = true;
            }
        }

        assertTrue( found );

        assertEquals( 398, updatedee.getRawExpressionDataVectors().size() );

        assertEquals( 199, updatedee.getProcessedExpressionDataVectors().size() );

        Collection<DoubleVectorValueObject> processedDataArrays = dataVectorService.getProcessedDataArrays( updatedee );
        assertEquals( 199, processedDataArrays.size() );

        for ( DoubleVectorValueObject v : processedDataArrays ) {
            assertEquals( 6, v.getBioAssays().size() );

        }
        assertTrue( !dataVectorService.getProcessedDataVectors( experimentService.load( updatedee.getId() ) ).isEmpty() );

    }

    /**
     * Test case where some samples cannot be used.
     * 
     * @throws Exception
     */
    @Test
    public void testLoadRNASeqDataWithMissingSamples() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        ExpressionExperiment ee = experimentService.findByShortName( "GSE29006" );
        if ( ee != null ) {
            experimentService.delete( ee );
        }

        assertTrue( experimentService.findByShortName( "GSE29006" ) == null );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE29006", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            throw new IllegalStateException( "Need to delete this data set before test is run" );
        }

        ee = experimentService.thaw( ee );

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
        targetArrayDesign = this.getTestPersistentArrayDesign( probeNames, taxonService.findByCommonName( "human" ) );
        targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

        try {
            dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, false );
            fail( "Should have gotten an exception" );
        } catch ( IllegalArgumentException e ) {
            // Expected
        }
        dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, true );
        /*
         * Check
         */
        ExpressionExperiment updatedee = experimentService.thaw( ee );

        for ( BioAssay ba : updatedee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );
        }

        ExpressionDataDoubleMatrix mat = dataMatrixService.getProcessedExpressionDataMatrix( ee );
        assertEquals( 199, mat.rows() );
        Double[] column = mat.getColumn( 0 );
        double sum = Descriptive.sum( new DoubleArrayList( ArrayUtils.toPrimitive( column ) ) );
        assertEquals( 437324.86, sum, 0.01 );

        assertEquals( 4, updatedee.getBioAssays().size() );

        assertEquals( 398, updatedee.getRawExpressionDataVectors().size() );

        assertEquals( 199, updatedee.getProcessedExpressionDataVectors().size() );

        Collection<DoubleVectorValueObject> processedDataArrays = dataVectorService.getProcessedDataArrays( updatedee );

        assertEquals( 199, processedDataArrays.size() );

        boolean found = false;
        for ( BioAssay ba : updatedee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );

            assertEquals( 36, ba.getSequenceReadLength().intValue() );

            if ( ba.getDescription().contains( "GSM718709" ) ) {
                assertEquals( 320383, ba.getSequenceReadCount().intValue() );
                found = true;
            }
        }
        assertTrue( found );

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
        return qt;
    }
}
