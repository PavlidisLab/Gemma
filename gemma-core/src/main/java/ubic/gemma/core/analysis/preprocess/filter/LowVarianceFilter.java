package ubic.gemma.core.analysis.preprocess.filter;

import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;

/**
 * @author paul
 */
public class LowVarianceFilter implements Filter<ExpressionDataDoubleMatrix> {

    private final double lowVarianceCut;

    /**
     * @param lowVarianceCut minimum variance to keep a row.
     */
    public LowVarianceFilter( double lowVarianceCut ) {
        this.lowVarianceCut = lowVarianceCut;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setMethod( RowLevelFilter.Method.VAR );
        rowLevelFilter.setLowCut( lowVarianceCut, false );
        rowLevelFilter.setRemoveAllNegative( false );
        return rowLevelFilter.filter( dataMatrix );
    }

    @Override
    public String toString() {
        return "LowVarianceFilter Low=" + lowVarianceCut;
    }
}
