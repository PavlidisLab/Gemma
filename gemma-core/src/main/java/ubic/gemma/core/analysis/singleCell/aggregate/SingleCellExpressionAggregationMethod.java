package ubic.gemma.core.analysis.singleCell.aggregate;

/**
 * Methods for aggregating single-cell expression data.
 * @author poirigui
 */
public enum SingleCellExpressionAggregationMethod {
    /**
     * Aggregate data by summing it.
     * <p>
     * A pseudo-count of {@code 0.5} is added at the end.
     */
    SUM,
    /**
     * Equivalent to {@link #SUM} for log-transformed data.
     * <p>
     * A pseudo-count of {@code 0.5} is added at the end before performing the final log-transformation.
     */
    LOG_SUM,
    /**
     * Equivalent to {@link #SUM} for data transformed by {@code log 1 + X}
     * <p>
     * No pseudo-count is added.
     */
    LOG1P_SUM;
}
