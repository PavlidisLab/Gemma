package ubic.gemma.model.expression.experiment;

/**
 * Represents a batch effect.
 * @author poirigui
 */
public enum BatchEffectType {
    /**
     * Indicate that there is no batch information available.
     */
    NO_BATCH_INFO,
    /**
     * Batch contains singleton (i.e. batch with a single experiment) thus preventing any variance estimate.
     */
    SINGLETON_BATCHES_FAILURE,
    /**
     * Batch information is uninformative.
     */
    UNINFORMATIVE_HEADERS_FAILURE,
    /**
     * Batch information is problematic.
     */
    PROBLEMATIC_BATCH_INFO_FAILURE,
    /**
     * Indicate that there is a batch effect. It might be correctable.
     */
    BATCH_EFFECT_FAILURE,
    /**
     * Indicate that there was a significant batch effect that was corrected.
     */
    BATCH_CORRECTED_SUCCESS,
    /**
     * Indicate that there is a single batch and thus there cannot be a batch effect.
     */
    SINGLE_BATCH_SUCCESS,
    /**
     * Indicate that all information necessary is present, but the batch effect could not be determined.
     * <p>
     * This can result from a failure to perform SVD or to find a suitable batch factor in the experimental design.
     */
    BATCH_EFFECT_UNDETERMINED_FAILURE,
    /**
     * Indicate that there is no batch effect. Yay!
     */
    NO_BATCH_EFFECT_SUCCESS
}
