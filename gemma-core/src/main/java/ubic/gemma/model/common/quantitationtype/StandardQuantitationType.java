package ubic.gemma.model.common.quantitationtype;

public enum StandardQuantitationType {
    PRESENTABSENT,
    FAILED,
    /**
     * Referring to a measured or derived "amount", indicating the relative or absolute level of something. Typically an
     * expression level or expression ratio. This is intentionally very generic.
     */
    AMOUNT,
    CONFIDENCEINDICATOR,
    CORRELATION,
    /**
     * Indicates value is a count, such as the number of sequencing reads.
     */
    COUNT,
    /**
     * Used to represent a value for a spatial coordinate
     */
    COORDINATE,
    /**
     * Standard deviations from the mean
     */
    ZSCORE,
    OTHER;
}