package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Helper service for creating processed data vectors.
 * <p>
 * This shouldn't be used directly, instead use {@link ProcessedExpressionDataVectorService}.
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
     * @return the number of created vectors
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int createProcessedDataVectors( ExpressionExperiment expressionExperiment, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException;
}
