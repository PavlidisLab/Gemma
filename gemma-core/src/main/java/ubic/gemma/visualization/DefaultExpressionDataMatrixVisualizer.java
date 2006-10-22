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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataDesignElementDataVectorMatrix;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
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

    private ExpressionDataDesignElementDataVectorMatrix expressionDataMatrix = null;

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
    public DefaultExpressionDataMatrixVisualizer( ExpressionDataDesignElementDataVectorMatrix expressionDataMatrix, String imageFile ) {
        super( imageFile );
        this.expressionDataMatrix = expressionDataMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#getExpressionDataMatrix()
     */
    public ExpressionDataDesignElementDataVectorMatrix getExpressionDataMatrix() {
        return expressionDataMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)
     */
    public ColorMatrix createColorMatrix( ExpressionDataDesignElementDataVectorMatrix expressionDataMatrix ) {

        if ( expressionDataMatrix == null || expressionDataMatrix.getDesignElements() == null ) {
            throw new IllegalArgumentException( "ExpressionDataMatrix apparently has no data" );
        }

        Collection<DesignElement> deCol = expressionDataMatrix.getDesignElements();

        ByteArrayConverter byteArrayConverter = new ByteArrayConverter();
        double[][] data = new double[deCol.size()][];
        int i = 0;
        for ( DesignElement designElement : deCol ) {
            Collection<DesignElementDataVector> vectors = ( ( CompositeSequence ) designElement )
                    .getDesignElementDataVectors();
            Iterator iter = vectors.iterator();
            DesignElementDataVector vector = ( DesignElementDataVector ) iter.next();

            data[i] = byteArrayConverter.byteArrayToDoubles( vector.getData() );

            if ( rowLabels == null ) {
                log.debug( "Setting row names" );
                rowLabels = new ArrayList<String>();
            }
            // log.debug( designElement.getName() );
            rowLabels.add( designElement.getName() );
            i++;
        }

        if ( colLabels == null ) {
            log.warn( "Column labels not set.  Using defaults" );
            colLabels = new ArrayList<String>();
            for ( int j = 0; j < data[0].length; j++ ) {
                colLabels.add( String.valueOf( j ) );
            }
        }
        return this.createColorMatrix( data, rowLabels, colLabels );
    }

    /**
     * Draw the dynamic image and write to stream
     * 
     * @param stream
     * @return String
     * @throws IOException
     */
    public String drawDynamicImage( OutputStream stream, ColorMatrix colorMatrix ) throws IOException {
        // TODO move me to another implementation of MatrixVisualizer
        log.warn( "drawing dynamic image" );

        ExpressionDataMatrixProducerImpl producer = new ExpressionDataMatrixProducerImpl();
        producer.setColorMatrix( colorMatrix );

        String type = producer.createDynamicImage( stream, true, true );

        log.debug( "returning content type " + type );

        return type;
    }
}
