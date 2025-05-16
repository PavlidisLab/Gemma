package ubic.gemma.model.common.quantitationtype;

public enum StandardQuantitationType {
    /**
     * Indicate if a value is present or absent.
     * <p>
     * This type is generally used for masking another data vector.
     * <p>
     * Only {@link PrimitiveType#BOOLEAN} is allowed.
     */
    PRESENTABSENT,
    /**
     * Indicate if a failure occurred for a given value or not.
     * <p>
     * Only {@link PrimitiveType#BOOLEAN} is allowed.
     */
    FAILED,
    /**
     * Referring to a measured or derived "amount", indicating the relative or absolute level of something.
     * <p>
     * Typically, an expression level or expression ratio. This is intentionally very generic.
     * <p>
     * Only {@link PrimitiveType#INT}, {@link PrimitiveType#LONG} and {@link PrimitiveType#DOUBLE} are allowed.
     */
    AMOUNT,
    /**
     * Indicate a confidence level.
     * <p>
     * Only {@link PrimitiveType#DOUBLE} is allowed.
     */
    CONFIDENCEINDICATOR,
    /**
     * Indicate a correlation value.
     * <p>
     * Only {@link PrimitiveType#DOUBLE} is allowed.
     */
    CORRELATION,
    /**
     * Indicates value is a count, such as the number of sequencing reads.
     * <p>
     * Only {@link PrimitiveType#INT}, {@link PrimitiveType#LONG} and {@link PrimitiveType#DOUBLE} are allowed. In the
     * case of a double representation, the counts may be *adjusted* or transformed as per {@link ScaleType}.
     */
    COUNT,
    /**
     * Used to represent a value for a spatial coordinate.
     * <p>
     * This is not really supported, so no representation is specified.
     */
    COORDINATE,
    /**
     * Standard deviations from the mean.
     * <p>
     * Only {@link PrimitiveType#DOUBLE} is allowed.
     */
    ZSCORE,
    /**
     * Use this for all other types including unknown.
     */
    OTHER;
}