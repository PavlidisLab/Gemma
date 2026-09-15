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
     * Populate the processed data for the given experiment. For two-channel studies, the missing value information
     * should already have been computed. If the values already exist, they will be re-written. The data will be
     * quantile normalized (with some exceptions: ratios and count data will not be normalized).
     *
     * @param expressionExperiment       ee
     * @param ignoreQuantitationMismatch use raw data to infer scale type and the adequate transformation for producing
     *                                   processed EVs instead of relying on the QT
     * @param summary                    summary object to populate
     * @return the number of created vectors
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    QuantitationType createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch, ProcessedExpressionDataVectorCreationSummary summary ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException;

    /**
     * Run the same processing pipeline as {@link #createProcessedDataVectors} but skip the outlier mask, persist
     * nothing, and hand back the resulting matrix.
     * <p>
     * 🛑 This is NOT the dataset's processed data and must not be stored or served as such. Everything in Gemma --
     * differential expression, SVD, visualization, export -- reads the stored, masked vectors, and that is
     * deliberate. The single consumer of this is the sample-correlation matrix: it is the evidence a curator
     * reviews an outlier call against, and masking wrote the flagged sample's correlations out of it, so the call
     * could not be reviewed afterwards.
     * <p>
     * The mask goes in BEFORE quantile normalization, so the values cannot be recovered from the stored vectors --
     * rebuilding from raw is the only way to get them, which is why this exists rather than an unmasking read.
     *
     * @see ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    ExpressionDataDoubleMatrix computeUnmaskedProcessedDataMatrix( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException;
}
