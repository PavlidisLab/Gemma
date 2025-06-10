package ubic.gemma.core.analysis.preprocess.filter;

import ubic.basecode.math.Constants;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;

/**
 * Remove rows that have a variance of zero (within a small constant)
 * @author paul
 */
public class ZeroVarianceFilter implements Filter<ExpressionDataDoubleMatrix> {

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter( RowLevelFilter.Method.VAR );
        rowLevelFilter.setLowCut( Constants.SMALLISH );
        rowLevelFilter.setRemoveAllNegative( false );
        rowLevelFilter.setUseAsFraction( false );
        return rowLevelFilter.filter( matrix );
    }

    @Override
    public String toString() {
        return "ZeroVarianceFilter Threshold=" + Constants.SMALLISH;
    }
}
