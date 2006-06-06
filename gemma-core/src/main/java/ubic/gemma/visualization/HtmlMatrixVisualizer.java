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

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.JMatrixDisplay;

/**
 * @author keshav
 * @version $Id$
 */
public class HtmlMatrixVisualizer implements MatrixVisualizer {

    private MatrixVisualizationData matrixVisualizationData = null;

    /**
     * @return MatrixVisualizationData
     */
    public MatrixVisualizationData getMatrixVisualizationData() {
        return matrixVisualizationData;
    }

    /**
     * @param matrixVisualizationData
     */
    public void setMatrixVisualizationData( MatrixVisualizationData matrixVisualizationData ) {
        this.matrixVisualizationData = matrixVisualizationData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(ubic.gemma.visualization.MatrixVisualizationData)
     */
    public void createVisualization( MatrixVisualizationData matrixVisualizationData ) {

        // DoubleMatrixNamed matrix = DenseDoubleMatrix2DNamed();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(double[][])
     */
    public void createVisualization( double[][] data ) {
        DoubleMatrixNamed matrix = new DenseDoubleMatrix2DNamed( data );
        // JMatrixDisplay matrixDisplay = new JMatrixDisplay( matrix );
        // matrixDisplay.setLabelsVisible( false );

        // try {
        // matrixDisplay.saveImage( "output.png" );
        // } catch ( IOException e ) {
        // System.err.println( "Could not print image" );
        // e.printStackTrace();
        // }
    }

}
