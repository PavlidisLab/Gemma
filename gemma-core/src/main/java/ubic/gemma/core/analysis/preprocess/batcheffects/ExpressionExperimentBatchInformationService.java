package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.BatchEffectType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * Provides status of batch information for datasets.
 */
public interface ExpressionExperimentBatchInformationService {

    /**
     * Check if the given experiment has batch information.
     * <p>
     * This does not imply that the batch information is usable or valid. Use {@link #checkHasUsableBatchInfo(ExpressionExperiment)}
     * for that purpose.
     */
    boolean checkHasBatchInfo( ExpressionExperiment ee );

    /**
     * Check if the given experiment has usable batch information.
     */
    boolean checkHasUsableBatchInfo( ExpressionExperiment ee );

    /**
     * Checks the experiment for a batch confound.
     *
     * @param ee the experiment to check.
     * @return a string describing the batch confound, or null if there was no batch confound.[FIXME: String return value is unsafe]
     */
    @Nullable
    String getBatchConfound( ExpressionExperiment ee );

    /**
     * Obtain the full batch effect details of a given experiment.
     * @param ee experiment
     * @return details for the principal component most associated with batches (even if it isn't "significant"). Note
     * that we don't look at every component, just the first few.
     */
    BatchEffectDetails getBatchEffectDetails( ExpressionExperiment ee );

    /**
     * Obtain a {@link BatchEffectType} describing the batch effect state of the given experiment.
     * @param ee the experiment to get the batch effect for.
     */
    BatchEffectType getBatchEffect( ExpressionExperiment ee );

    /**
     * Obtain a string describing the summary statistics of a batch effect is present in the given experiment.
     * @return summary statistics or null if there is no batch effect
     */
    @Nullable
    String getBatchEffectStatistics( ExpressionExperiment ee );
}
