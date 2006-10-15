/*
 * The genTools project
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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;

/**
 * @author keshav
 * @version $Id$
 */
public class DefaultDataMatrixVisualizer implements DataMatrixVisualizer {
    private Log log = LogFactory.getLog( DefaultDataMatrixVisualizer.class );

    private boolean suppressVisualizations;

    private Color[] colorMap = ColorMap.REDGREEN_COLORMAP;

    protected List<String> rowLabels = null;

    protected List<String> colLabels = null;

    protected String imageFile = "Image file not set";

    /**
     * Cannot instantiate this.
     */
    public DefaultDataMatrixVisualizer() {
        throw new RuntimeException( "cannot instantiate using no-arg constructor" );
    }

    /**
     * @param imageFile
     */
    public DefaultDataMatrixVisualizer( String imageFile ) {
        this.imageFile = imageFile;
    }

    /**
     * @param data
     * @param rowLabels
     * @param colorMatrix
     */
    public ColorMatrix createColorMatrix( double[][] data, List<String> rowLabels, List<String> colLabels ) {
        assert rowLabels != null && colLabels != null : "Labels not set";

        DoubleMatrixNamed matrix = new DenseDoubleMatrix2DNamed( data );
        matrix.setRowNames( rowLabels );
        matrix.setColumnNames( colLabels );
        ColorMatrix colorMatrix = new ColorMatrix( matrix );

        return colorMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#getImageFile()
     */
    public String getImageFile() {
        return imageFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#saveImage(java.io.File)
     */
    public void saveImage( ColorMatrix colorMatrix ) throws IOException {
        this.saveImage( imageFile, colorMatrix );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#saveImage(java.io.File)
     */
    public void saveImage( File outFile, ColorMatrix colorMatrix ) throws IOException {
        this.saveImage( outFile.getAbsolutePath(), colorMatrix );
    }

    /**
     * @param outfile
     * @throws IOException
     */
    private void saveImage( String outfile, ColorMatrix colorMatrix ) throws IOException {
        // if ( outfile != null ) this.outfile = outfile;

        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );

        display.setCellSize( new Dimension( 16, 16 ) );
        display.saveImage( outfile );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#isSuppressVisualizations()
     */
    public boolean isSuppressVisualizations() {
        return suppressVisualizations;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#setSuppressVisualizations(boolean)
     */
    public void setSuppressVisualizations( boolean suppressVisualizations ) {
        this.suppressVisualizations = suppressVisualizations;
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
     * @see ubic.gemma.visualization.MatrixVisualizer#setRowLabels(java.util.List)
     */
    public void setRowLabels( List<String> rowLabels ) {
        this.rowLabels = rowLabels;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#getColLabels()
     */
    public List<String> getColLabels() {
        return colLabels;
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
     * @see ubic.gemma.visualization.MatrixVisualizer#getColorMap()
     */
    public Color[] getColorMap() {
        return colorMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.visualization.MatrixVisualizer#setColorMap(java.awt.Color[])
     */
    public void setColorMap( Color[] colorMap ) {
        this.colorMap = colorMap;
    }
}
