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

package ubic.gemma.loader.expression.geo;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class DataUpdaterIntegrationTest extends AbstractGeoServiceTest {
    @Autowired
    private GeoService geoService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private ProcessedExpressionDataVectorService dataVectorService;

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.geo.DataUpdater#addAffyExonArrayData(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     * .
     */
    @Test
    public void testAddAffyExonArrayDataExpressionExperiment() throws Exception {
        ExpressionExperiment ee;
        try {
            String path = getTestFileBasePath();
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE12135", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }

        /*
         * Add the raw data.
         */
        dataUpdater.addAffyExonArrayData( ee );

        ee = experimentService.load( ee.getId() );
    }

    @Test
    public void testAddAffyExonHuman() throws Exception {
        ExpressionExperiment ee; // GSE22498
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            Collection<?> results = geoService.fetchAndLoad( "GSE22498", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }
        dataUpdater.addAffyExonArrayData( ee );
        ee = experimentService.load( ee.getId() );
    }

    @Test
    public void testAddAffyExonRat() throws Exception {
        ExpressionExperiment ee; // GSE33597
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            Collection<?> results = geoService.fetchAndLoad( "GSE33597", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }
        dataUpdater.addAffyExonArrayData( ee );
        ee = experimentService.load( ee.getId() );

    }

    @Test
    public void testReplaceData() {

        /*
         * Load a regular data set that has one array design
         */

        /*
         * make up some fake data on another platform.
         */

        /*
         * Replace it.
         */

    }

    @Test
    public void testAddData() throws Exception {

        /*
         * Load a regular data set that has no data. Platform is (basically) irrelevant.
         */
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT ) );
        ExpressionExperiment ee;

        ExpressionExperiment oldee = experimentService.findByShortName( "GSE37646" );
        // if ( oldee != null ) experimentService.delete( oldee ); // maybe okay?

        try {
            // RNA-seq data.
            Collection<?> results = geoService.fetchAndLoad( "GSE37646", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            // log.warn( "Test skipped because GSE1133 was not removed from the system prior to test" );
            ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).get( 0 );
        }

        ee = experimentService.thawLite( ee );

        List<BioAssay> bioAssays = new ArrayList<BioAssay>( ee.getBioAssays() );
        assertEquals( 31, bioAssays.size() );

        List<BioMaterial> bms = new ArrayList<BioMaterial>();
        for ( BioAssay ba : bioAssays ) {
            if ( ba.getSamplesUsed().size() > 1 ) {
                throw new UnsupportedOperationException(
                        "Can't make new data from matrix that has multiple bioassays per biomaterial" );
            }
            bms.add( ba.getSamplesUsed().iterator().next() );
        }

        ArrayDesign targetArrayDesign = getTestPersistentArrayDesign( 100, true );

        DoubleMatrix<CompositeSequence, BioMaterial> rawMatrix = new DenseDoubleMatrix<CompositeSequence, BioMaterial>(
                targetArrayDesign.getCompositeSequences().size(), bms.size() );
        /*
         * make up some fake data on another platform, and match it to those samples
         */
        for ( int i = 0; i < rawMatrix.rows(); i++ ) {
            for ( int j = 0; j < rawMatrix.columns(); j++ ) {
                rawMatrix.set( i, j, ( i + 1 ) * ( j + 1 ) / 1000.0 );
            }
        }

        List<CompositeSequence> probes = new ArrayList<CompositeSequence>( targetArrayDesign.getCompositeSequences() );

        rawMatrix.setRowNames( probes );
        rawMatrix.setColumnNames( bms );

        QuantitationType qt = makeQt();

        ExpressionDataDoubleMatrix data = new ExpressionDataDoubleMatrix( ee, qt, rawMatrix );

        assertNotNull( data.getBestBioAssayDimension() );
        assertEquals( rawMatrix.columns(), data.getBestBioAssayDimension().getBioAssays().size() );
        assertEquals( probes.size(), data.getMatrix().rows() );

        /*
         * Replace it.
         */
        dataUpdater.replaceData( ee, targetArrayDesign, data );

        /*
         * Check
         */
        ExpressionExperiment updatedee = experimentService.thaw( experimentService.load( ee.getId() ) );

        assertEquals( 100, updatedee.getRawExpressionDataVectors().size() );
        assertEquals( 100, updatedee.getProcessedExpressionDataVectors().size() );

        Collection<DoubleVectorValueObject> processedDataArrays = dataVectorService.getProcessedDataArrays( updatedee );

        for ( DoubleVectorValueObject v : processedDataArrays ) {
            BioAssayDimension bad = v.getBioAssayDimension();
            assertEquals( 31, bad.getBioAssays().size() );

        }

    }

    private QuantitationType makeQt() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "foo" );
        qt.setDescription( "bar" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setScale( ScaleType.LINEAR );
        qt.setIsBackground( false );
        qt.setIsBackgroundSubtracted( true );
        qt.setIsNormalized( true );
        qt.setIsMaskedPreferred( true );
        qt.setIsPreferred( true );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return qt;
    }

}
