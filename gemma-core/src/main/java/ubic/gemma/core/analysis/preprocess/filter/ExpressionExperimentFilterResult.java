package ubic.gemma.core.analysis.preprocess.filter;

import lombok.Data;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;

/**
 * Hold the results of filtering with {@link ExpressionExperimentFilter}.
 *
 * @author poirigui
 * @see ExpressionExperimentFilter#filter(ExpressionDataDoubleMatrix, ExpressionExperimentFilterResult)
 */
@Data
public class ExpressionExperimentFilterResult {
    private int startingRows;
    private int startingColumns;
    private boolean noSequencesFilterApplied;
    private int afterNoSequencesFilter;
    private boolean AffyControlsFilterApplied;
    private int afterAffyControlsFilter;
    private boolean isOutliersFilterApplied;
    private int afterOutliersFilter;
    private int columnsAfterOutliersFilter;
    private boolean minPresentFilterApplied;
    private int afterMinPresentFilter;
    private boolean zeroVarianceFilterApplied;
    private int afterZeroVarianceFilter;
    private boolean lowExpressionFilterApplied;
    private int afterLowExpressionFilter;
    private boolean lowVarianceFilterApplied;
    private int afterLowVarianceFilter;
    private int finalRows;
    private int finalColumns;
}
