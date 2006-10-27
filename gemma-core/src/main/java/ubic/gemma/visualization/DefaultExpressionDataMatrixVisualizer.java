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

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.gui.ColorMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * A value object to visualize the ExpressionDataMatrix
 * 
 * @author keshav
 * @version $Id$
 */
public class DefaultExpressionDataMatrixVisualizer extends DefaultDataMatrixVisualizer implements
        ExpressionDataMatrixVisualizer, Serializable {
    private Log log = LogFactory.getLog( this.getClass() );

    private static final long serialVersionUID = -5075323948059345296L;

    private ExpressionDataMatrix expressionDataMatrix = null;

    /**
     * Do not instantiate. This is to be "inpected" by java constructs that require an official java bean. When we say
     * inspected, we mean it is never actually invoked but the signature checked using a string comparison. Invocation
     * will result in a RuntimeException
     */
    public DefaultExpressionDataMatrixVisualizer() {
        super();
    }

    /**
     * @param expressionDataMatrix
     * @param imageFile
     */
    public DefaultExpressionDataMatrixVisualizer( ExpressionDataMatrix expressionDataMatrix, String imageFile ) {
        super( imageFile );
        this.expressionDataMatrix = expressionDataMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#getExpressionDataMatrix()
     */
    public ExpressionDataMatrix getExpressionDataMatrix() {
        return expressionDataMatrix;
    }

    // /**
    // * @param expressionDataMatrix
    // * @return Collection<double[]>
    // */
    // public Collection<double[]> getRowData( ExpressionDataMatrix expressionDataMatrix ) {
    // // TODO make part of interface
    // // TODO used?
    // if ( expressionDataMatrix == null || expressionDataMatrix.getDesignElements() == null ) {
    // throw new IllegalArgumentException( "ExpressionDataMatrix apparently has no data" );
    // }
    //
    // Collection<DesignElement> deCol = expressionDataMatrix.getDesignElements();
    //
    // Collection<double[]> dataCol = new HashSet<double[]>();
    //
    // ByteArrayConverter byteArrayConverter = new ByteArrayConverter();
    //
    // int i = 0;
    // for ( DesignElement designElement : deCol ) {
    // Collection<DesignElementDataVector> vectors = ( ( CompositeSequence ) designElement )
    // .getDesignElementDataVectors();
    // Iterator iter = vectors.iterator();
    // DesignElementDataVector vector = ( DesignElementDataVector ) iter.next();
    //
    // double[] data = byteArrayConverter.byteArrayToDoubles( vector.getData() );
    //
    // dataCol.add( data );
    //
    // i++;
    // }
    //
    // return dataCol;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)
     */
    public ColorMatrix createColorMatrix( ExpressionDataMatrix expressionDataMatrix ) {
        // TODO not used?

        Map<DesignElement, Integer> rowMap = expressionDataMatrix.getRowMap(); // row labels

        Map<BioAssay, Integer> columnMap = expressionDataMatrix.getColumnMap(); // column labels

        if ( expressionDataMatrix == null || rowMap.size() == 0 ) {
            throw new IllegalArgumentException( "ExpressionDataMatrix apparently has no data" );
        }

        double[][] data = new double[rowMap.size()][];
        int i = 0;
        for ( DesignElement designElement : rowMap.keySet() ) {
            log.warn( "design element: " + designElement );
            Double[] row = ( Double[] ) expressionDataMatrix.getRow( designElement );
            data[i] = ArrayUtils.toPrimitive( row );
            rowLabels.add( designElement.getName() );
            i++;
        }

        // TODO add (bioassay) column labels.
        if ( colLabels.size() == 0 ) {
            for ( int j = 0; j < data[0].length; j++ ) {
                colLabels.add( String.valueOf( j ) );
            }
        }
        return this.createColorMatrix( data, rowLabels, colLabels );
    }
}
