package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Helper service for creating processed data vectors.
 * <p>
 * This shouldn't be used directly, instead use {@link ProcessedExpressionDataVectorService}.
 *
 * @author Paul
 * @author poirigui
 */
interface ProcessedExpressionDataVectorCreationHelperService {

    /**
     * Read everything the processing pipeline needs, in a transaction of its own.
     * <p>
     * This is the first of three steps that used to be one method. The middle step,
     * {@link #normalizeProcessedData}, takes 44 minutes on a large experiment and touches no entity; the last,
     * {@link #replaceProcessedDataVectors}, is the only one that writes. Keeping them apart is what stops a
     * pooled connection being held across the arithmetic — see {@code TransactionSpanningComputeRuleTest}.
     *
     * @param expressionExperiment       ee
     * @param ignoreQuantitationMismatch use raw data to infer scale type and the adequate transformation for
     *                                   producing processed EVs instead of relying on the QT
     * @param summary                    summary object to populate
     * @param maskOutliers               whether to blank the values of assays flagged as outliers. Always true
     *                                   when creating the stored processed data; false only for the
     *                                   sample-correlation matrix, which is the evidence a curator reviews an
     *                                   outlier call against and is useless with the flagged sample removed.
     * @return the pipeline's state, with detached entity references — see {@link ComputedProcessedData}
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    ComputedProcessedData readProcessedDataInputs( ExpressionExperiment expressionExperiment,
            boolean ignoreQuantitationMismatch, ProcessedExpressionDataVectorCreationSummary summary,
            boolean maskOutliers ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException;

    /**
     * Quantile-normalize in place, with no transaction open.
     * <p>
     * 🛑 Deliberately carries no {@code @Transactional} and must not acquire one. It reads only the plain arrays
     * on {@code computed}; a transaction here would hold a pooled connection for the whole normalization
     * without ever using it, which is the defect this split exists to remove.
     */
    void normalizeProcessedData( ComputedProcessedData computed, ProcessedExpressionDataVectorCreationSummary summary );

    /**
     * Replace the experiment's processed vectors with the computed ones, in a single write transaction.
     * <p>
     * 🛑 The removal happens here rather than before the computation on purpose. It used to run first, inside
     * the same transaction as the compute, where a failure rolled it back. Now that the compute is outside any
     * transaction, removing first would commit a deletion and then fail, leaving the experiment with no
     * processed vectors.
     *
     * @return the created quantitation type
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    QuantitationType replaceProcessedDataVectors( ExpressionExperiment expressionExperiment,
            ComputedProcessedData computed, ProcessedExpressionDataVectorCreationSummary summary );

    /**
     * Assemble the computed data into a matrix without persisting any of it.
     * <p>
     * 🛑 This is NOT the dataset's processed data and must not be stored or served as such. Everything in Gemma
     * — differential expression, SVD, visualization, export — reads the stored, masked vectors, and that is
     * deliberate. The single consumer is the sample-correlation matrix: it is the evidence a curator reviews an
     * outlier call against, and masking wrote the flagged sample's correlations out of it, so the call could
     * not be reviewed afterwards. The mask goes in BEFORE quantile normalization, so the values cannot be
     * recovered from the stored vectors; rebuilding from raw is the only way to get them.
     * <p>
     * No transaction: it builds objects from what {@link #readProcessedDataInputs} already read.
     *
     * @see ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService
     */
    ExpressionDataDoubleMatrix toMatrix( ExpressionExperiment expressionExperiment, ComputedProcessedData computed );
}
