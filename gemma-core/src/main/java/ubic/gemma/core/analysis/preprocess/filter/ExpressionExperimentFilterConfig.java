package ubic.gemma.core.analysis.preprocess.filter;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds settings for filtering with {@link ExpressionExperimentFilter}.
 *
 * @author Paul
 */
@Data
public class ExpressionExperimentFilterConfig implements Serializable {

    public static final boolean DEFAULT_MASK_OUTLIERS = true;
    public static final double DEFAULT_DISTINCT_VALUE_FRACTION = 0.5;
    public static final double DEFAULT_HIGH_EXPRESSION_CUT = 1.0;
    // changed from 0.3, because now we remove the "no-gene" probes by default.
    public static final double DEFAULT_LOW_EXPRESSION_CUT = 0.2;
    public static final double DEFAULT_LOW_VARIANCE_CUT = LowVarianceFilter.DEFAULT_LOW_VARIANCE_CUT;
    public static final double DEFAULT_MIN_PRESENT_FRACTION = RepetitiveValuesFilter.DEFAULT_MINIMUM_FRACTION_OF_UNIQUE_VALUES;
    public static final int DEFAULT_MIN_PRESENT_COUNT = 7;

    /**
     * Mask outliers in the data by replacing the affected columns with {@link Double#NaN}.
     * <p>
     * This filter is sensitive to multi-assay matrices and will only mask design elements that belong to the affected
     * assays.
     * <p>
     * The default is to mask outliers which is sensible for most applications.
     *
     * @see OutliersFilter
     */
    private boolean maskOutliers = DEFAULT_MASK_OUTLIERS;

    /**
     * Low cut for expression.
     * <p>
     * Design elements expressed below the cut will be filtered out.
     * <p>
     * This threshold applies on the rank. A value of 0.9 will drop the top 10% of the data.
     * <p>
     * Set this to one to disable.
     */
    private double lowExpressionCut = DEFAULT_LOW_EXPRESSION_CUT;

    /**
     * High cut for expression.
     * <p>
     * Design elements expressed above the cut will be filtered out.
     * <p>
     * This threshold applies on the rank of gene expression. A value of 0.9 will drop the top 10% of the data.
     * <p>
     * Set this to 1.0 to disable.
     */
    private double highExpressionCut = DEFAULT_HIGH_EXPRESSION_CUT;

    /**
     * This threshold applies on the variance of the row.
     * <p>
     * The threshold applies to data on a log2-scale, so a value of 0.01 will retain values with a standard deviation of
     * 0.1.
     * <p>
     * Set this to zero disable.
     *
     * @see LowVarianceFilter
     */
    private double lowVarianceCut = DEFAULT_LOW_VARIANCE_CUT;

    /**
     * Minimum fraction of distinct values for a design element to be retained.
     */
    private double lowDistinctValueCut = DEFAULT_DISTINCT_VALUE_FRACTION;

    /**
     * Minimum fraction of samples that must have a value (or a present call if a present/absent boolean matrix is
     * provided) for the design element to be retained.
     *
     * @see RowMissingValueFilter
     */
    private double minPresentFraction = DEFAULT_MIN_PRESENT_FRACTION;

    /**
     * Minimum number of samples for keeping rows when min-present filtering. Rows with more missing values
     * than this are always removed. This can be increased by the use of the min fraction present filter which sets a
     * fraction.
     *
     * @see RowMissingValueFilter
     */
    private int minPresentCount = DEFAULT_MIN_PRESENT_COUNT;

    /**
     * If true, the {@link ExpressionExperimentFilter#MINIMUM_DESIGN_ELEMENTS} is ignored.
     */
    private boolean ignoreMinimumDesignElementsThreshold = false;

    /**
     * If true, {@link ExpressionExperimentFilter#MINIMUM_SAMPLES} is ignored.
     */
    private boolean ignoreMinimumSamplesThreshold = false;

    /**
     * Set to true if design elements lacking associated BioSequences for the element should be removed.
     * <p>
     * Note that this filter is ignored if ALL design elements lack sequences.
     *
     * @see RowsWithSequencesFilter
     */
    private boolean requireSequences = true;
}
