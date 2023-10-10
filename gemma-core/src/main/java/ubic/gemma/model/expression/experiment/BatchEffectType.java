package ubic.gemma.model.expression.experiment;

/**
 * Represents a batch effect.
 * @author poirigui
 */
public enum BatchEffectType {
    NO_BATCH_INFO,
    SINGLETON_BATCHES_FAILURE,
    UNINFORMATIVE_HEADERS_FAILURE,
    PROBLEMATIC_BATCH_INFO_FAILURE,
    BATCH_EFFECT_FAILURE,
    BATCH_CORRECTED_SUCCESS,
    SINGLE_BATCH_SUCCESS,
    NO_BATCH_EFFECT_SUCCESS
}
