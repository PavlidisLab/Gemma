package ubic.gemma.persistence.service.expression.experiment;

public enum SingleCellExpressionAggregationMethod {
    /**
     * Aggregate data by summing it.
     */
    SUM,
    /**
     * Equivalent to {@link #SUM} for log-transformed data.
     */
    LOG_SUM,
    /**
     * Equivalent to {@link #SUM} for data transformed by {@code log 1 + X}
     */
    LOG1P_SUM;
}
