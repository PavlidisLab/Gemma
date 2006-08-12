/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;

/**
 * A value object to visualize the ExpressionDataMatrix
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataMatrixVisualization implements MatrixVisualizer, Serializable {
    private Log log = LogFactory.getLog( this.getClass() );

    private static final long serialVersionUID = -5075323948059345296L;

    private ExpressionDataMatrix expressionDataMatrix = null;

    private String outfile = "visualization.png";

    private ColorMatrix colorMatrix = null;

    private List<String> rowLabels = null;

    private List<String> colLabels = null;

    private Color[] colorMap = ColorMap.REDGREEN_COLORMAP; // TODO add support for color map

    private int rowNameXCoord = 0;
    private Map<String, Integer> rowNameYCoords = null;

    private boolean suppressVisualizations;

    /**
     * Create visualization for the implicit expressionDataMatrix
     */
    public void createVisualization() {
        createVisualization( this.expressionDataMatrix );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(ubic.gemma.visualization.MatrixVisualizationData)
     */
    @SuppressWarnings("unchecked")
    public void createVisualization( ExpressionDataMatrix expressionDataMatrix ) {

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
                System.out.println( "Setting row names" );
                rowLabels = new ArrayList();
            }
            log.debug( designElement.getName() );
            rowLabels.add( designElement.getName() );
            i++;
        }

        if ( colLabels == null ) {
            System.out.println( "Column labels not set.  Using defaults" );
            colLabels = new ArrayList();
            for ( int j = 0; j < data[0].length; j++ ) {
                colLabels.add( String.valueOf( j ) );
            }
        }
        createVisualization( data );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#createVisualization(double[][])
     */
    public void createVisualization( double[][] data ) {
        assert rowLabels != null && colLabels != null : "Labels not set";

        DoubleMatrixNamed matrix = new DenseDoubleMatrix2DNamed( data );
        matrix.setRowNames( rowLabels );
        matrix.setColumnNames( colLabels );
        colorMatrix = new ColorMatrix( matrix );
    }

    /**
     * @return ExpressionDataMatrix
     */
    public ExpressionDataMatrix getExpressionDataMatrix() {
        return expressionDataMatrix;
    }

    /**
     * @param expressionDataMatrix
     */
    public void setExpressionDataMatrix( ExpressionDataMatrix expressionDataMatrix ) {
        this.expressionDataMatrix = expressionDataMatrix;
    }

    /**
     * @return String
     */
    public String getOutfile() {
        return outfile;
    }

    /**
     * @param outfile
     */
    public void setOutfile( String outfile ) {
        this.outfile = outfile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#setColLabels(java.util.List)
     */
    public void setColLabels( List<String> colNames ) {
        this.colLabels = colNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#setRowLabels(java.util.List)
     */
    public void setRowLabels( List<String> rowLabels ) {
        this.rowLabels = rowLabels;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#getRowLabels()
     */
    public List<String> getRowLabels() {
        return rowLabels;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#setColorMap(null[])
     */
    public void setColorMap( Color[] colorMap ) {
        this.colorMap = colorMap;
    }

    /**
     * Saves the image to outfile
     * 
     * @param outfile
     */
    public void saveImage( String outfile ) throws IOException {
        if ( outfile != null ) this.outfile = outfile;

        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );

        display.setCellSize( new Dimension( 16, 16 ) );
        display.saveImage( outfile );
    }

    /**
     * @param outFile
     * @throws IOException
     */
    public void saveImage( File outFile ) throws IOException {
        this.saveImage( outFile.getAbsolutePath() );
    }

    /**
     * Draw the dynamic image and write to stream
     * 
     * @param stream
     * @return String
     * @throws IOException
     */
    public String drawDynamicImage( OutputStream stream ) throws IOException {
        log.warn( "drawing dynamic image" );

        ExpressionDataMatrixProducerImpl producer = new ExpressionDataMatrixProducerImpl();
        producer.setColorMatrix( this.colorMatrix );

        String type = producer.createDynamicImage( stream, true, true );

        log.debug( "returning content type " + type );

        return type;

    }

    /**
     * @return ColorMatrix
     */
    public ColorMatrix getColorMatrix() {
        return colorMatrix;
    }

    /**
     * @return int
     */
    public int getRowNameXCoord() {
        return rowNameXCoord;
    }

    /**
     * @return Map
     */
    public Map<String, Integer> getRowNameYCoords() {
        return rowNameYCoords;
    }

    /**
     * @param suppressVisualizations
     */
    public void setSuppressVisualizations( boolean suppressVisualizations ) {
        this.suppressVisualizations = suppressVisualizations;
    }

    /**
     * @return Returns the suppressVisualizations.
     */
    public boolean isSuppressVisualizations() {
        return suppressVisualizations;
    }
}
