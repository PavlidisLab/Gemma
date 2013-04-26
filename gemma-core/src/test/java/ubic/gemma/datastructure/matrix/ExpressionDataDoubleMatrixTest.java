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
package ubic.gemma.datastructure.matrix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.analysis.service.SampleRemoveService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataDoubleMatrixTest extends AbstractGeoServiceTest {

    private SimpleExpressionExperimentMetaData metaData = null;

    private ExpressionExperiment ee = null;
    private ExpressionExperiment newee = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DesignElementDataVectorService designElementDataVectorService;

    @Autowired
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedDataVectorService;

    @Autowired
    protected GeoService geoService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService = null;

    @Autowired
    private SampleRemoveService sampleRemoveService;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {

        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();

        metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "new ad" );
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        taxon.setIsGenesUsable( true );
        taxon.setIsSpecies( true );
        metaData.setTaxon( taxon );
        metaData.setName( "ee" );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" );

        DoubleMatrix<String, String> matrix = simpleExpressionDataLoaderService.parse( data );
        ee = simpleExpressionDataLoaderService.convert( metaData, matrix );

        assertNotNull( ee );
        assertEquals( 200, ee.getRawExpressionDataVectors().size() );
        assertEquals( 59, ee.getBioAssays().size() );

    }

    /**
     * Tests the construction of an ExpressionDataDoubleMatrix
     * 
     * @throws IOException
     */
    @Test
    public void testConstructExpressionDataDoubleMatrix() {

        /* test creating the ExpressionDataDoubleMatrix */
        QuantitationType quantitationType = QuantitationType.Factory.newInstance();
        quantitationType.setName( metaData.getQuantitationTypeName() );
        quantitationType.setIsPreferred( true );
        quantitationType.setRepresentation( PrimitiveType.DOUBLE );
        quantitationType.setIsMaskedPreferred( false );
        quantitationType.setIsRatio( true );
        quantitationType.setIsBackground( false );
        quantitationType.setIsBackgroundSubtracted( true );
        quantitationType.setIsNormalized( true );

        Collection<RawExpressionDataVector> designElementDataVectors = ee.getRawExpressionDataVectors();
        Collection<CompositeSequence> designElements = new HashSet<CompositeSequence>();
        for ( DesignElementDataVector designElementDataVector : designElementDataVectors ) {
            CompositeSequence de = designElementDataVector.getDesignElement();
            designElements.add( de );
        }

        /* Constructor 1 */
        ExpressionDataDoubleMatrix expressionDataDoubleMatrix = new ExpressionDataDoubleMatrix(
                designElementDataVectors );

        /* Assertions */
        CompositeSequence deToQuery = designElements.iterator().next();

        Double[] row = expressionDataDoubleMatrix.getRow( deToQuery );
        assertNotNull( row );
        for ( int i = 0; i < row.length; i++ ) {
            log.debug( row[i] );
        }

        Double[][] dMatrix = expressionDataDoubleMatrix.getRawMatrix();
        assertEquals( dMatrix.length, 200 );
        assertEquals( dMatrix[0].length, 59 );

    }

    /**
     * This is a self-contained test. That is, it does not depend on the setup in {@link onSetUpInTransaction}. It tests
     * creating an {@link ExpressionDataDoubleMatrix} using real values from the Gene Expression Omnibus (GEO). That is,
     * we have obtained information from GSE994. The probe sets used are 218120_s_at and 121_at, and the samples used
     * are GSM15697 and GSM15744. Specifically, we the Gemma objects that correspond to the GEO objects are:
     * <p>
     * DesignElement 1 = 218120_s_at, DesignElement 2 = 121_at
     * <p>
     * BioAssay 1 = "Current Smoker 73", BioAssay 2 = "Former Smoker 34"
     * <p>
     * BioMaterial 1 = "GSM15697", BioMaterial 2 = "GSM15744"
     * <p>
     * BioAssayDimension = "GSM15697, GSM15744" (the names of all the biomaterials).
     */
    @Test
    public void testConstructExpressionDataDoubleMatrixWithGeoValues() {
        ByteArrayConverter bac = new ByteArrayConverter();

        ee = ExpressionExperiment.Factory.newInstance();

        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "VALUE" );
        qt.setIsBackgroundSubtracted( false );
        qt.setIsNormalized( false );
        qt.setIsBackground( false );
        qt.setIsRatio( false );
        qt.setIsPreferred( true );
        qt.setIsMaskedPreferred( false );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        BioAssayDimension bioAssayDimension = BioAssayDimension.Factory.newInstance();
        bioAssayDimension.setName( "GSM15697, GSM15744" );

        Collection<BioAssay> assays = new LinkedHashSet<BioAssay>();

        BioAssay assay1 = BioAssay.Factory.newInstance();
        assay1.setName( "Current Smoker 73" );

        BioMaterial sample1 = BioMaterial.Factory.newInstance();
        sample1.setName( "GSM15697" );

        assay1.setSampleUsed( sample1 );

        assays.add( assay1 );

        BioAssay assay2 = BioAssay.Factory.newInstance();
        assay2.setName( "Former Smoker 34" );

        BioMaterial sample2 = BioMaterial.Factory.newInstance();
        sample2.setName( "GSM15744" );

        assay2.setSampleUsed( sample2 );

        assays.add( assay2 );

        bioAssayDimension.setBioAssays( assays );

        Collection<DesignElementDataVector> vectors1 = new LinkedHashSet<DesignElementDataVector>();
        RawExpressionDataVector vector1 = RawExpressionDataVector.Factory.newInstance();
        double[] ddata1 = { 74.9, 101.7 };
        byte[] bdata1 = bac.doubleArrayToBytes( ddata1 );
        vector1.setData( bdata1 );
        vector1.setQuantitationType( qt );
        vector1.setBioAssayDimension( bioAssayDimension );
        vectors1.add( vector1 );

        Collection<DesignElementDataVector> vectors2 = new LinkedHashSet<DesignElementDataVector>();
        RawExpressionDataVector vector2 = RawExpressionDataVector.Factory.newInstance();
        double[] ddata2 = { 404.6, 318.7 };
        byte[] bdata2 = bac.doubleArrayToBytes( ddata2 );
        vector2.setData( bdata2 );
        vector2.setQuantitationType( qt );
        vector2.setBioAssayDimension( bioAssayDimension );
        vectors2.add( vector2 );

        Collection<CompositeSequence> designElements = new LinkedHashSet<CompositeSequence>();

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "test ar" );

        CompositeSequence de1 = CompositeSequence.Factory.newInstance();
        de1.setName( "218120_s_at" );
        vector1.setDesignElement( de1 );

        BioSequence bs1 = BioSequence.Factory.newInstance();
        bs1.setName( "test1" );

        de1.setBiologicalCharacteristic( bs1 );

        de1.setArrayDesign( ad );

        CompositeSequence de2 = CompositeSequence.Factory.newInstance();
        de2.setName( "121_at" );

        BioSequence bs2 = BioSequence.Factory.newInstance();
        bs2.setName( "test2" );

        de2.setBiologicalCharacteristic( bs2 );
        de2.setArrayDesign( ad );
        vector2.setDesignElement( de2 );

        designElements.add( de1 );
        designElements.add( de2 );

        Collection<RawExpressionDataVector> eeVectors = new LinkedHashSet<RawExpressionDataVector>();
        eeVectors.add( vector1 );
        eeVectors.add( vector2 );

        ee.setRawExpressionDataVectors( eeVectors );

        ExpressionDataDoubleMatrix expressionDataMatrix = new ExpressionDataDoubleMatrix( eeVectors );

        assertNotNull( expressionDataMatrix );
        assertEquals( expressionDataMatrix.rows(), 2 );
        assertEquals( expressionDataMatrix.columns(), 2 );
    }

    @Test
    public void testMatrixConversion() throws Exception {

        try {

            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "" ) ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE8294", false, true, false, false );
            newee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
        }

        newee = expressionExperimentService.thaw( newee );
        // make sure we really thaw them, so we can get the design element sequences.

        Collection<RawExpressionDataVector> vectors = newee.getRawExpressionDataVectors();
        designElementDataVectorService.thaw( vectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );
        ExpressionDataDoubleMatrix matrix = builder.getPreferredData();
        assertTrue( !Double.isNaN( matrix.get( 10, 0 ) ) );
        assertEquals( 66, matrix.rows() );
        assertEquals( 9, matrix.columns() );

        /*
         * Additional tests for files and outlier marking.
         */

        processedDataVectorService.computeProcessedExpressionData( newee );

        File f1 = expressionDataFileService.writeOrLocateDataFile( newee, true, true );
        assertNotNull( f1 );
        assertTrue( f1.exists() );

        expressionDataFileService.deleteAllFiles( newee );
        assertTrue( !f1.exists() );

        /*
         * outlier removal.
         */
        BioAssay tba = newee.getBioAssays().iterator().next();
        sampleRemoveService.markAsMissing( tba );

        assertTrue( tba.getIsOutlier() );

        newee = expressionExperimentService.thaw( newee );
        Collection<ProcessedExpressionDataVector> vecs = newee.getProcessedExpressionDataVectors();

        this.designElementDataVectorService.thaw( vecs );

        assertTrue( !vecs.isEmpty() );

        ExpressionDataMatrixBuilder matrixBuilder = new ExpressionDataMatrixBuilder( vecs );
        ExpressionDataDoubleMatrix data = matrixBuilder.getProcessedData();

        assertNotNull( data );

        assertTrue( Double.isNaN( data.getColumn( tba )[10] ) );

        sampleRemoveService.unmarkAsMissing( tba );
        newee = expressionExperimentService.thaw( expressionExperimentService.load( newee.getId() ) );
        vecs = newee.getProcessedExpressionDataVectors();

        this.designElementDataVectorService.thaw( vecs );

        assertTrue( !vecs.isEmpty() );

        matrixBuilder = new ExpressionDataMatrixBuilder( vecs );
        data = matrixBuilder.getProcessedData();
        assertTrue( !tba.getIsOutlier() );
        assertTrue( !Double.isNaN( data.getColumn( tba )[10] ) );

    }

    @After
    public void tearDown() {
        try {
            if ( ee != null && ee.getId() != null ) {
                expressionExperimentService.delete( ee );
            }
            if ( newee != null && newee.getId() != null ) {
                expressionExperimentService.delete( newee );
            }
        } catch ( Exception e ) {

        }

    }
}
