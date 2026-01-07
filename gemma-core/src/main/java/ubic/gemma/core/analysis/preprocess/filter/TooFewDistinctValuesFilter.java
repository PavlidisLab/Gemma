package ubic.gemma.core.analysis.preprocess.filter;

import org.springframework.util.Assert;
import ubic.basecode.math.Constants;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;

/**
 * Remove rows that have a low diversity of values (equality judged based on tolerance set in {@link RowLevelFilter}).
 * <p>
 * This happens when people "set values less than 10 equal to 10" for example. This effectively filters rows that have
 * too many missing values, because missing values are counted as a single value.
 *
 * @author paul
 */
public class TooFewDistinctValuesFilter implements ExpressionDataFilter<ExpressionDataDoubleMatrix> {

    private final double threshold;

    public TooFewDistinctValuesFilter( double threshold ) {
        Assert.isTrue( threshold >= 0 && threshold <= 1, "Threshold must be between 0 and 1" );
        this.threshold = threshold;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter( RowLevelFilter.Method.DISTINCT_VALUES );
        /*
         * 0.5 means: 1/2 of the values must be distinct. Close to zero means none of the values are distinct. 1.0 means
         * they are all distinct.
         */
        rowLevelFilter.setLowCut( threshold );
        rowLevelFilter.setTolerance( Constants.SMALLISH );
        return rowLevelFilter.filter( matrix );
    }

    @Override
    public boolean appliesTo( ExpressionDataDoubleMatrix dataMatrix ) {
        return threshold > 0;
    }

    @Override
    public String toString() {
        return String.format( "TooFewDistinctValuesFilter Threshold=%.2f%%", 100 * threshold );
    }
}
