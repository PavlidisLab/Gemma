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
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.gui.ColorMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * A value object to visualize the ExpressionDataMatrix
 * 
 * @author keshav
 * @version $Id$
 */
public class DefaultExpressionDataMatrixVisualizer extends DefaultDataMatrixVisualizer implements
        ExpressionDataMatrixVisualizer, Serializable {

    private Log log = LogFactory.getLog( DefaultExpressionDataMatrixVisualizer.class );

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
     */
    public DefaultExpressionDataMatrixVisualizer( ExpressionDataMatrix expressionDataMatrix ) {
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)
     */
    @SuppressWarnings("unchecked")
    public ColorMatrix createColorMatrix( ExpressionDataMatrix expressionDataMatrix ) {
        Collection<DesignElement> rowMap = expressionDataMatrix.getRowElements(); // row labels

        if ( expressionDataMatrix == null || rowMap.size() == 0 ) {
            throw new IllegalArgumentException( "ExpressionDataMatrix apparently has no data" );
        }

        double[][] data = new double[rowMap.size()][];
        int i = 0;
        for ( DesignElement designElement : rowMap ) {
            Double[] row = ( Double[] ) expressionDataMatrix.getRow( designElement );
            data[i] = ArrayUtils.toPrimitive( row );
            rowLabels.add( designElement.getName() );
            i++;
        }

         int j = 0;
        while ( j < data[0].length ) {
            Collection<BioMaterial> bioMaterials = expressionDataMatrix.getBioMaterialsForColumn( j );
            if ( bioMaterials == null || bioMaterials.size() == 0 ) {
                log.warn( "No BioMaterials found for index + " + j + ". Setting label to column number " + j );
                colLabels.add( String.valueOf( j ) );
            } else {
                if ( bioMaterials.size() > 1 )
                    log.warn( "More than one BioMaterial. Using first one found as column label." );

                Iterator iter = bioMaterials.iterator();
                while ( iter.hasNext() ) {
                    BioMaterial bm = ( BioMaterial ) iter.next();
                    if ( !colLabels.contains( bm.getName() ) ) {
                        log.debug( "adding label " + bm.getName() );
                        colLabels.add( bm.getName() );
                    }
                }

            }
            j++;
        }

//        for ( int k = 0; k < data[0].length; k++ ) {
//            colLabels.add( String.valueOf( k ) );
//        }

        return this.createColorMatrix( data, rowLabels, colLabels );
    }
}
