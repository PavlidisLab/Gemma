package ubic.gemma.core.analysis.preprocess.filter;

import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;

/**
 * Remove rows that have a low diversity of values (equality judged based on tolerance set in {@link RowLevelFilter}).
 * This happens when people "set values less than 10 equal to 10" for example. This effectively filters rows that have
 * too many missing values, because missing values are counted as a single value.
 * @author paul
 */
public class TooFewDistinctValuesFilter implements Filter<ExpressionDataDoubleMatrix> {

    private final double tolerance;
    private final double threshold;

    public TooFewDistinctValuesFilter( double tolerance, double threshold ) {
        this.tolerance = tolerance;
        this.threshold = threshold;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setMethod( RowLevelFilter.Method.DISTINCTVALUES );
        rowLevelFilter.setTolerance( tolerance );
        rowLevelFilter.setRemoveAllNegative( false );

        /*
         * 0.5 means: 1/2 of the values must be distinct. Close to zero means none of the values are distinct. 1.0 means
         * they are all distinct.
         */
        rowLevelFilter.setLowCut( threshold );
        rowLevelFilter.setUseAsFraction( false );
        return rowLevelFilter.filter( matrix );
    }
}
