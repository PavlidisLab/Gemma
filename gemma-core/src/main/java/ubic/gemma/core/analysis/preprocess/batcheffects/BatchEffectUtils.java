package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.BatchEffectType;

import javax.annotation.Nullable;

public class BatchEffectUtils {

    private static final double BATCH_EFFECT_THRESHOLD = 0.01;

    /**
     * Obtain a {@link BatchEffectType} describing the batch effect state of the given batch details.
     */
    public static BatchEffectType getBatchEffectType( BatchEffectDetails beDetails ) {
        if ( beDetails.hasSingletonBatches() ) {
            return BatchEffectType.SINGLETON_BATCHES_FAILURE;
        } else if ( beDetails.hasUninformativeBatchInformation() ) {
            return BatchEffectType.UNINFORMATIVE_HEADERS_FAILURE;
        } else if ( !beDetails.hasBatchInformation() ) {
            return BatchEffectType.NO_BATCH_INFO;
        } else if ( beDetails.hasProblematicBatchInformation() ) {
            return BatchEffectType.PROBLEMATIC_BATCH_INFO_FAILURE;
        } else if ( beDetails.isSingleBatch() ) {
            return BatchEffectType.SINGLE_BATCH_SUCCESS;
        } else if ( beDetails.dataWasBatchCorrected() ) {
            // Checked for in ExpressionExperimentDetails.js::renderStatus()
            return BatchEffectType.BATCH_CORRECTED_SUCCESS;
        } else {
            BatchEffectDetails.BatchEffectStatistics batchEffectStatistics = beDetails.getBatchEffectStatistics();
            if ( batchEffectStatistics == null ) {
                return BatchEffectType.BATCH_EFFECT_UNDETERMINED_FAILURE;
            } else if ( batchEffectStatistics.getPvalue() < BATCH_EFFECT_THRESHOLD ) {
                // this means there was a batch effect but we couldn't correct it
                return BatchEffectType.BATCH_EFFECT_FAILURE;
            } else {
                return BatchEffectType.NO_BATCH_EFFECT_SUCCESS;
            }
        }
    }

    /**
     * Obtain a string describing the summary statistics of a batch effect is present in the given batch details.
     * @return summary statistics or null if there is no batch effect
     */
    @Nullable
    public static String getBatchEffectStatistics( BatchEffectDetails beDetails ) {
        if ( beDetails.getBatchEffectStatistics() != null ) {
            return String.format( "This data set may have a batch artifact (PC %d), p=%.5g",
                    beDetails.getBatchEffectStatistics().getComponent(),
                    beDetails.getBatchEffectStatistics().getPvalue() );
        }
        return null;
    }
}
