package ubic.gemma.visualization;

import java.util.Collection;

import org.jfree.chart.JFreeChart;

import ubic.basecode.graphics.MatrixDisplay;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;

public interface ExpressionDataMatrixVisualizationService {

    /**
     * Generates a color mosaic (also known as a heat map).
     * 
     * @param expressionDataMatrix
     * @return JMatrixDisplay
     */
    public abstract MatrixDisplay createHeatMap( ExpressionDataMatrix<Double> expressionDataMatrix );

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