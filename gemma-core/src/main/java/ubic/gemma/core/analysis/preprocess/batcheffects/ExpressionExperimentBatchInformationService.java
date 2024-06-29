package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import ubic.gemma.core.lang.Nullable;
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
}
