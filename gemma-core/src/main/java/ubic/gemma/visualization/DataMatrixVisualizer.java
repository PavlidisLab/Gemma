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
import java.util.List;

import ubic.basecode.gui.ColorMatrix;

/**
 * @author keshav
 * @version $Id$
 */
public interface DataMatrixVisualizer {

    /**
     * @param data
     * @param rowLabels
     * @param colLabels
     * @return ColorMatrix
     */
    public ColorMatrix createColorMatrix( double[][] data, List<String> rowLabels, List<String> colLabels );

    /**
     * @return Color[]
     */
    public Color[] getColorMap();

    /**
     * @param colorMap
     */
    public void setColorMap( Color[] colorMap );
}
