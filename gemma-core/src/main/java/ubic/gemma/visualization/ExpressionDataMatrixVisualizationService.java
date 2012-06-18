/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.visualization;

import java.util.Collection;

import org.jfree.chart.JFreeChart;

import ubic.basecode.graphics.MatrixDisplay;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 * @deprecated
 */
@Deprecated
public interface ExpressionDataMatrixVisualizationService {

    /**
     * Generates a color mosaic (also known as a heat map).
     * 
     * @param expressionDataMatrix
     * @return JMatrixDisplay
     */
    public abstract MatrixDisplay<String, String> createHeatMap(
            ExpressionDataMatrix<Double> expressionDataMatrix );

    /**
     * Generates an x y line chart (also known as a profile).
     * 
     * @param title
     * @param dataCol
     * @param numProfiles
     * @return JFreeChart
     * @deprecated
     */
    @Deprecated
    public abstract JFreeChart createXYLineChart( String title, Collection<double[]> dataCol, int numProfiles );

    /**
     * Normalize (centered on the mean) and clip the data. That is, a z-score is calculated for each data point. If the
     * threshold is null, the data will not be clipped.
     * <p>
     * More information on z-scores can be found at http://en.wikipedia.org/wiki/Z_score.
     * </p>
     * FIXME move this somewhere non-visualization related.
     * 
     * @param expressionDataDoubleMatrix
     * @param threshold The threhold at which the data will be clipped (ie. 2 clips the data at -2 and +2).
     * @return ExpressionDataMatrix
     */
    public abstract ExpressionDataMatrix<Double> standardizeExpressionDataDoubleMatrix(
            ExpressionDataMatrix<Double> expressionDataMatrix, Double threshold );

}