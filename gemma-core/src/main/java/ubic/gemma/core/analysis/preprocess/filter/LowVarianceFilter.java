package ubic.gemma.core.analysis.preprocess.filter;

import org.springframework.util.Assert;
import ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.ScaleType;

/**
 * Filter rows with low variance by keeping those with variance above a given cut-off.
 * <p>
 * The data must be on a {@link ScaleType#LOG2} scale. If the data is not on a log2 scale, use
 * {@link ScaleTypeConversionUtils} to convert it.
 *
 * @author paul
 */
public class LowVarianceFilter implements ExpressionDataFilter<ExpressionDataDoubleMatrix> {

    /**
     * Default low variance cut-off.
     * <p>
     * The value has been chosen for data on a log2 scale, adjust as necessary.
     */
    public static final double DEFAULT_LOW_VARIANCE_CUT = 0.01;

    private final double lowVarianceCut;

    public LowVarianceFilter() {
        this( DEFAULT_LOW_VARIANCE_CUT );
    }

    /**
     * @param lowVarianceCut minimum variance to keep a row, inclusive.
     */
    public LowVarianceFilter( double lowVarianceCut ) {
        Assert.isTrue( lowVarianceCut >= 0, "Low Variance Cut must be zero or greater." );
        this.lowVarianceCut = lowVarianceCut;
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) {
        Assert.isTrue( dataMatrix.getQuantitationType().getScale() == ScaleType.LOG2,
                "Data must be on a log2 scale for this filter." );
        RowLevelFilter rowLevelFilter = new RowLevelFilter( RowLevelFilter.Method.VAR );
        rowLevelFilter.setLowCut( lowVarianceCut );
        return rowLevelFilter.filter( dataMatrix );
    }

    @Override
    public boolean appliesTo( ExpressionDataDoubleMatrix dataMatrix ) {
        return lowVarianceCut > 0;
    }

    @Override
    public String toString() {
        return String.format( "LowVarianceFilter Low=%.2e", lowVarianceCut );
    }
}
