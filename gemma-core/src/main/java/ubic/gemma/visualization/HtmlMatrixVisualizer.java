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
import java.util.Collection;
import java.util.List;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * @author keshav
 * @version $Id$
 */
public class HtmlMatrixVisualizer implements MatrixVisualizer {

    private MatrixVisualizationData matrixVisualizationData = null;
    private List<String> rowNames = null;
    private List<String> colNames = null;
    private String outfile = "gemma-core/src/main/java/ubic/gemma/visualization/outImage.png";

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

        assert rowNames != null && colNames != null : "Labels not set";

        Collection<DesignElement> deCol = matrixVisualizationData.getDesignElements();
        ByteArrayConverter byteArrayConverter = new ByteArrayConverter();
        double[][] data = new double[deCol.size()][];
        int i = 0;
        for ( DesignElement designElement : deCol ) {
            data[i] = byteArrayConverter.byteArrayToDoubles( designElement.getDesignElementDataVector().getData() );
            i++;
        }

        DoubleMatrixNamed matrix = new DenseDoubleMatrix2DNamed( data );
        matrix.setRowNames( rowNames );
        matrix.setColumnNames( colNames );
        ColorMatrix colorMatrix = new ColorMatrix( matrix );
        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );
        try {
            display.saveImage( outfile );
            display.setLabelsVisible( true );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(ubic.gemma.visualization.MatrixVisualizationData,
     *      java.lang.String)
     */
    public void createVisualization( MatrixVisualizationData matrixVisualizationData, String outfile ) {
        this.outfile = outfile;

        createVisualization( matrixVisualizationData );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(double[][])
     */
    public void createVisualization( double[][] data ) {
        assert rowNames != null && colNames != null : "Labels not set";

        DoubleMatrixNamed matrix = new DenseDoubleMatrix2DNamed( data );
        matrix.setRowNames( rowNames );
        matrix.setColumnNames( colNames );
        ColorMatrix colorMatrix = new ColorMatrix( matrix );
        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );
        try {
            display.saveImage( outfile );
            display.setLabelsVisible( true );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(double[][], java.lang.String)
     */
    public void createVisualization( double[][] data, String outfile ) {
        this.outfile = outfile;

        createVisualization( data );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#setColLabels(java.util.List)
     */
    public void setColLabels( List<String> colNames ) {
        this.colNames = colNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#setRowLabels(java.util.List)
     */
    public void setRowLabels( List<String> rowNames ) {
        this.rowNames = rowNames;
    }

}
