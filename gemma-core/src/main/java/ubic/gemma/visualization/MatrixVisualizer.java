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

/**
 * @author keshav
 * @version $Id$
 */
public interface MatrixVisualizer {

    /**
     * @param matrixVisualizationData
     */
    public void createVisualization( ExpressionDataMatrix matrixVisualizationData );

    /**
     * @param data
     */
    public void createVisualization( double[][] data );

    /**
     * @param rowLabels
     */
    public void setRowLabels( List<String> rowLabels );
    
    /**
     * @param rowLabels
     */
    public List<String> getRowLabels();

    /**
     * @param colLabels
     */
    public void setColLabels( List<String> colLabels );

    /**
     * @param colorMap
     */
    public void setColorMap( Color[] colorMap );
}
