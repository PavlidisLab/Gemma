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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataDoubleMatrixTest extends BaseSpringContextTest {

    SimpleExpressionExperimentMetaData metaData = null;

    DoubleMatrixNamed matrix = null;

    ExpressionExperiment ee = null;

    ExpressionExperimentService expressionExperimentService;
    ArrayDesignService adService;
    Collection<ArrayDesign> ads;
    protected AbstractGeoService geoService;

    @SuppressWarnings("unchecked")
    @Override
    protected void onTearDownAfterTransaction() throws Exception {
        super.onTearDownAfterTransaction();
        if ( ee != null && ee.getId() != null ) {
            expressionExperimentService.delete( ee );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction();
        ads = new HashSet<ArrayDesign>();
        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "new ad" );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        metaData.setTaxon( taxon );
        metaData.setName( "ee" );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.RATIO );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" );

        matrix = service.parse( data );
        ee = service.convert( metaData, matrix );

        assertNotNull( ee );
        assertEquals( 200, ee.getDesignElementDataVectors().size() );
        assertEquals( 59, ee.getBioAssays().size() );

        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
        geoService.setLoadPlatformOnly( false );
    }

    /**
     * For bug 553
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testMatrixConversionGSE611() throws Exception {
        endTransaction();
        ExpressionExperiment newee;
        try {
            String path = ConfigUtils.getString( "gemma.home" );
            assert path != null;
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "GSE611Short" ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE611" );

            // geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
            // Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService
            // .fetchAndLoad( "GSE611" );
            newee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) e.getData();
        }

        expressionExperimentService.thaw( newee );
        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( newee );
        QuantitationType qt = quantitationTypes.iterator().next();
        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( newee, qt );
        assertEquals( 30, matrix.rows() );
        assertEquals( 4, matrix.columns() );
    }

    /**
     * For bug 553 - original file is corrupted, so this test doesn't really help that much.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testMatrixConversionGSE2870() throws Exception {
        endTransaction();
        ExpressionExperiment newee;
        try {
            String path = ConfigUtils.getString( "gemma.home" );
            assert path != null;
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "GSE2870Short" ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE2870" );
            newee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) e.getData();
        }

        expressionExperimentService.thaw( newee );
        Collection<QuantitationType> quantitationTypes = expressionExperimentService.getQuantitationTypes( newee );
        QuantitationType qt = quantitationTypes.iterator().next();
        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( newee, qt );
        assertEquals( 30, matrix.rows() );
        assertEquals( 4, matrix.columns() );
    }

    /**
     * Tests the construction of an ExpressionDataDoubleMatrix
     * 
     * @throws IOException
     */
    public void testConstructExpressionDataDoubleMatrix() throws IOException {

        /* test creating the ExpressionDataDoubleMatrix */
        QuantitationType quantitationType = QuantitationType.Factory.newInstance();
        quantitationType.setName( metaData.getQuantitationTypeName() );
        quantitationType.setDescription( metaData.getQuantitationTypeDescription() );
        quantitationType.setGeneralType( GeneralType.QUANTITATIVE );
        quantitationType.setType( metaData.getType() );
        quantitationType.setRepresentation( PrimitiveType.DOUBLE );
        quantitationType.setScale( metaData.getScale() );
        quantitationType.setIsBackground( false );

        Collection<DesignElementDataVector> designElementDataVectors = ee.getDesignElementDataVectors();
        Collection<DesignElement> designElements = new HashSet<DesignElement>();
        for ( DesignElementDataVector designElementDataVector : designElementDataVectors ) {
            DesignElement de = designElementDataVector.getDesignElement();
            Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();
            vectors.add( designElementDataVector ); // associate vectors with design elements in memory
            de.setDesignElementDataVectors( vectors );
            designElements.add( de );
        }

        /* Constructor 1 */
        ExpressionDataDoubleMatrix expressionDataDoubleMatrix = new ExpressionDataDoubleMatrix( ee, designElements,
                quantitationType );

        /* Assertions */
        DesignElement deToQuery = designElements.iterator().next();

        Double[] row = expressionDataDoubleMatrix.getRow( deToQuery );
        assertNotNull( row );
        for ( int i = 0; i < row.length; i++ ) {
            log.debug( row[i] );
        }

        /* Get valid BioAssay from database */// TODO implement column
        // BioAssayService bsService = ( BioAssayService ) this.getBean( "bioAssayService" );
        // BioAssay ba = BioAssay.Factory.newInstance();
        // ba.setName( "8.1" );
        // BioAssay bioAssayFromDb = bsService.findOrCreate( ba );
        //
        /* Get column for bioassay '8.1' from ExpressionDataDoubleMatrix. */
        // Double[] column = expressionDataDoubleMatrix.getColumn( bioAssayFromDb );
        // assertNotNull( column );
        /* Get the matrix */
        Double[][] dMatrix = expressionDataDoubleMatrix.getMatrix();
        assertEquals( dMatrix.length, 200 );
        assertEquals( dMatrix[0].length, 59 );

        /* Constructor 2 */
        // ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee, quantitationType );
        /* Constructor 3 */
        // ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix(ee.getDesignElementDataVectors());
    }
}
