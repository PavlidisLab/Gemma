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
package ubic.gemma.visualization;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataMatrixVisualizerTest extends TestCase {

    private DefaultExpressionDataMatrixVisualizer matrixVisualizer = null;

    private ExpressionDataMatrix expressionDataMatrix = null;

    double[][] data = null;

    private File tmp = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        String[] rowLabels = { "a", "b", "c", "d", "e" };
        List<String> rowLabelsList = new ArrayList<String>();
        for ( int i = 0; i < rowLabels.length; i++ ) {
            rowLabelsList.add( i, rowLabels[i] );
        }

        data = new double[5][5];
        double d0[] = { 1, 2, 3, 4, 5 };
        double d1[] = { 5, 4, 3, 2, 1 };
        double d2[] = { 1, 2, 1, 2, 1 };
        double d3[] = { 9, 5, 12, 3, 8 };
        double d4[] = { 7, 22, 0.02, 3.4, 1.9 };

        data[0] = d0;
        data[1] = d1;
        data[2] = d2;
        data[3] = d3;
        data[4] = d4;

        List<DesignElementDataVector> vectorsPerExpressionExperiment = new ArrayList<DesignElementDataVector>();
        List<String> colLabelsList = new ArrayList<String>();
        ByteArrayConverter converter = new ByteArrayConverter();

        /* QuantitationType for each DesignElementDataVector */
        QuantitationType vectorQuantitationType = QuantitationType.Factory.newInstance();
        vectorQuantitationType.setName( "Test Quantitation Type." );
        vectorQuantitationType.setDescription( "A test quantitation type from ExpressionDataDoubleMatrixTest" );
        vectorQuantitationType.setGeneralType( GeneralType.QUANTITATIVE );
        vectorQuantitationType.setType( StandardQuantitationType.RATIO );
        vectorQuantitationType.setRepresentation( PrimitiveType.DOUBLE );
        vectorQuantitationType.setScale( ScaleType.LINEAR );
        vectorQuantitationType.setIsBackground( false );

        /* BioAssayDimension for each DesignElementDataVector */
        BioAssayDimension vectorBioAssayDimension = BioAssayDimension.Factory.newInstance();
        BioMaterial bm = null;
        List<BioAssay> assays = new ArrayList<BioAssay>(); // BioAssays
        for ( int i = 0; i < data[0].length; i++ ) {
            BioAssay assay = BioAssay.Factory.newInstance();
            assay.setName( "Test BioAssay " + i );
            bm = BioMaterial.Factory.newInstance();
            bm.setName( "Test BioMaterial " + i );
            Collection<BioMaterial> samplesUsed = new HashSet<BioMaterial>();
            samplesUsed.add( bm );
            assay.setSamplesUsed( samplesUsed );
            assays.add( assay );
        }

        Collection<DesignElement> designElements = new HashSet<DesignElement>();
        vectorBioAssayDimension.setBioAssays( assays );
        for ( int i = 0; i < data[0].length; i++ ) {
            colLabelsList.add( i, String.valueOf( i ) );

            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
            vector.setData( converter.doubleArrayToBytes( data[i] ) );
            vector.setQuantitationType( vectorQuantitationType );
            vector.setBioAssayDimension( vectorBioAssayDimension );

            DesignElement designElement = CompositeSequence.Factory.newInstance();
            designElement.setName( "Test DesignElement " + i );
            designElements.add( designElement );
            /* one vector/design element in this case */
            Collection<DesignElementDataVector> vectorsPerDesignElement = new HashSet<DesignElementDataVector>();
            vectorsPerDesignElement.add( vector );
            designElement.setDesignElementDataVectors( vectorsPerDesignElement );

            vector.setDesignElement( designElement );

            vectorsPerExpressionExperiment.add( i, vector );
        }

        tmp = File.createTempFile( "visualizationTest", ".png" );

        /* Create the expression experiment. */
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDesignElementDataVectors( vectorsPerExpressionExperiment );

        /* The expression data matrix */
        QuantitationType quantitationType = QuantitationType.Factory.newInstance();
        quantitationType.setName( "Test Quantitation Type." );
        quantitationType.setDescription( "A test quantitation type from ExpressionDataDoubleMatrixTest" );
        quantitationType.setGeneralType( GeneralType.QUANTITATIVE );
        quantitationType.setType( StandardQuantitationType.RATIO );
        quantitationType.setRepresentation( PrimitiveType.DOUBLE );
        quantitationType.setScale( ScaleType.LINEAR );
        quantitationType.setIsBackground( false );
        expressionDataMatrix = new ExpressionDataDoubleMatrix( ExpressionExperiment.Factory.newInstance(),
                designElements, quantitationType );

        matrixVisualizer = new DefaultExpressionDataMatrixVisualizer( expressionDataMatrix );

        matrixVisualizer.setRowLabels( rowLabelsList );
        matrixVisualizer.setColLabels( colLabelsList );

    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        matrixVisualizer = null;
        expressionDataMatrix = null;
        data = null;
        tmp.deleteOnExit();
    }

    /**
     * 
     *
     */
    public void testCreateVisualization() {

        ColorMatrix colorMatrix = matrixVisualizer.createColorMatrix( expressionDataMatrix );

        assertNotNull( colorMatrix );

    }
}
