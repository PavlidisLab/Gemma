package ubic.gemma.core.analysis.expression.diff;

import ubic.gemma.core.analysis.preprocess.filter.Filter;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.filter.RepetitiveValuesFilter;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;

/**
 * Filter used for performing DEA.
 *
 * @author poirigui
 */
public class DifferentialExpressionAnalysisFilter implements Filter<ExpressionDataDoubleMatrix> {

    public static final int DEFAULT_MINIMUM_NUMBER_OF_ASSAYS_TO_APPLY_FILTER = RepetitiveValuesFilter.DEFAULT_MINIMUM_NUMBER_OF_ASSAYS_TO_APPLY_FILTER;
    public static final double DEFAULT_MINIMUM_FRACTION_OF_UNIQUE_VALUES = RepetitiveValuesFilter.DEFAULT_MINIMUM_FRACTION_OF_UNIQUE_VALUES;

    public enum Mode {
        AUTODETECT,
        NOMINAL,
        RANK
    }

    private final RepetitiveValuesFilter filter;

    public DifferentialExpressionAnalysisFilter( DifferentialExpressionAnalysisConfig config ) {
        filter = new RepetitiveValuesFilter();
        if ( config.getFilterMode() != null ) {
            switch ( config.getFilterMode() ) {
                case AUTODETECT:
                    filter.setMode( RepetitiveValuesFilter.Mode.AUTODETECT );
                    break;
                case RANK:
                    filter.setMode( RepetitiveValuesFilter.Mode.RANK );
                    break;
                case NOMINAL:
                    filter.setMode( RepetitiveValuesFilter.Mode.NOMINAL );
                    break;
                default:
                    throw new IllegalArgumentException( "Unsupported filter mode " + config.getFilterMode() + "." );
            }
        }
        if ( config.getMinimumNumberOfAssaysToApplyFilter() != null ) {
            filter.setMinimumNumberOfAssaysToApplyFilter( config.getMinimumNumberOfAssaysToApplyFilter() );
        }
        if ( config.getMinimumFractionOfUniqueValues() != null ) {
            filter.setMinimumFractionOfUniqueValues( config.getMinimumFractionOfUniqueValues() );
        }
    }

    @Override
    public ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix dataMatrix ) throws FilteringException {
        return filter.filter( dataMatrix );
    }

    public void setMinimumNumberOfAssaysToApplyFilter( int minAssays ) {
        filter.setMinimumNumberOfAssaysToApplyFilter( minAssays );
    }

    public void setMinimumFractionOfUniqueValues( double minFraction ) {
        filter.setMinimumFractionOfUniqueValues( minFraction );
    }

    @Override
    public String toString() {
        return filter.toString();
    }
}
