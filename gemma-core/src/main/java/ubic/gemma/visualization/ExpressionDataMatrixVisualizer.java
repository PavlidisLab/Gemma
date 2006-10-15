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

import ubic.basecode.gui.ColorMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;

/**
 * @author keshav
 * @version $Id$
 */
public interface ExpressionDataMatrixVisualizer extends DataMatrixVisualizer {

    /**
     * @param matrixVisualizationData
     */
    public ColorMatrix createColorMatrix( ExpressionDataMatrix matrixVisualizationData );

    /**
     * Returns the data matrix to be visualized.
     * 
     * @return ExpressionDataMatrix
     */
    public ExpressionDataMatrix getExpressionDataMatrix();

    /**
     * Returns the suppressVisualizations.
     * 
     * @return boolean
     */
    public boolean isSuppressVisualizations();

    /**
     * @param suppressVisualizations
     */
    public void setSuppressVisualizations( boolean suppressVisualizations );

}
