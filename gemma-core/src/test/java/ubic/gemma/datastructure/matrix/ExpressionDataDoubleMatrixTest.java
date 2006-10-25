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

import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataDoubleMatrixTest extends BaseSpringContextTest {
    SimpleExpressionExperimentMetaData metaData = null;

    ExpressionExperiment ee = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction();

        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "new ad" );
        metaData.setArrayDesign( ad );

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

        // FIXME don't need to load db, but reusing this for now.
        /* read file and load data in database */
        ee = service.load( metaData, data );

        /* thaw ee after loading in database */
        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        eeService.thaw( ee );

        setComplete();

        assertNotNull( ee );
        assertEquals( 200, ee.getDesignElementDataVectors().size() );
        assertEquals( 59, ee.getBioAssays().size() );
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

        ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        ArrayDesign adFromDb = adService.findArrayDesignByName( "new ad" );

        Collection designElements = adFromDb.getCompositeSequences();

        /* Constructor 1 */
        ExpressionDataDoubleMatrix expressionDataDoubleMatrix = new ExpressionDataDoubleMatrix( ee, designElements,
                quantitationType );

        /* ASSERTIONS */

        /* Get valid DesignElement from database */
        CompositeSequenceService csService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );
        Collection<CompositeSequence> csCol = csService.findByName( "228766_at" ); // FIXME why collection?
        assertNotNull( csCol );
        assertTrue( csCol.size() > 0 );

        /* Get row for design element '228766_at' from ExpressionDataDoubleMatrix. */
        CompositeSequence csFromDb = csCol.iterator().next();
        Double[] row = expressionDataDoubleMatrix.getRow( csFromDb );
        assertNotNull( row );
        for ( int i = 0; i < row.length; i++ ) {
            log.debug( row[i] );
        }

        /* Get valid BioAssay from database */
        BioAssayService bsService = ( BioAssayService ) this.getBean( "bioAssayService" );
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setName( "8.1" );
        BioAssay bioAssayFromDb = bsService.findOrCreate( ba );

        /* Get column for bioassay '8.1' from ExpressionDataDoubleMatrix. */
        // Double[] column = expressionDataDoubleMatrix.getColumn( bioAssayFromDb );
        // assertNotNull( column ); //TODO implement column

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
