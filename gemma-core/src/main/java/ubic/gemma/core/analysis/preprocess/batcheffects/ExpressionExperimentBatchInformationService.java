package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.BatchEffectType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

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
     * Check if a given experiment has a significant batch confound.
     */
    boolean hasSignificantBatchConfound( ExpressionExperiment ee );

    /**
     * Obtain the significant batch confounds for a dataset.
     */
    List<BatchConfound> getSignificantBatchConfounds( ExpressionExperiment ee );

    /**
     * Obtain the significant batch confounds for a dataset subsets.
     */
    Map<ExpressionExperimentSubSet, List<BatchConfound>> getSignificantBatchConfoundsForSubsets( ExpressionExperiment ee );

    /**
     * Summarize the batch confounds for a given dataset or its subsets in an HTML string.
     * @return a summary or null if there is no batch confound
     */
    @Nullable
    String getBatchConfoundAsHtmlString( ExpressionExperiment ee );

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
