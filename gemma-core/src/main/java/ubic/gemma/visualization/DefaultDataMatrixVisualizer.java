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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;

/**
 * @author keshav
 * @version $Id$
 */
public class DefaultDataMatrixVisualizer implements DataMatrixVisualizer {
    private Log log = LogFactory.getLog( DefaultDataMatrixVisualizer.class );

    private Color[] colorMap = ColorMap.REDGREEN_COLORMAP;

    protected List<String> rowLabels = null;

    protected List<String> colLabels = null;

    /**
     * Cannot instantiate this.
     */
    public DefaultDataMatrixVisualizer() {
        this.rowLabels = new ArrayList<String>();
        this.colLabels = new ArrayList<String>();
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
