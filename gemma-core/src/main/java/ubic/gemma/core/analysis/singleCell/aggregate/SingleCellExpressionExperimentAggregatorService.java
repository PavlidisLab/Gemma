package ubic.gemma.core.analysis.singleCell.aggregate;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.List;

/**
 * Aggregate single-cell vectors.
 */
public interface SingleCellExpressionExperimentAggregatorService {

    /**
     * Aggregate preferred single-cell data vectors by the preferred cell type assignment.
     * <p>
     * Data is transformed to {@code log2cpm} only if it is of {@link StandardQuantitationType#COUNT} type and using one
     * of the following scale type:
     * <ul>
     *     <li>{@link ScaleType#LINEAR}</li>
     *     <li>{@link ScaleType#COUNT}</li>
     *     <li>{@link ScaleType#LOG2}</li>
     *     <li>{@link ScaleType#LOG10}</li>
     *     <li>{@link ScaleType#LOG1P}</li>
     *     <li>{@link ScaleType#LN}</li>
     * </ul>
     *
     * @param qt            single-cell quantitation type to aggregate
     * @param cellBAs       samples to aggregate vectors for
     * @param makePreferred make the resulting QT preferred
     * @return the quantitation type of the newly created vectors
     * @throws UnsupportedScaleTypeForAggregationException if data of the given scale type cannot be aggregated
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    QuantitationType aggregateVectors( ExpressionExperiment ee, QuantitationType qt, List<BioAssay> cellBAs, boolean makePreferred ) throws UnsupportedScaleTypeForAggregationException;

    /**
     * Remove aggregated vectors for the given quantitation type.
     * <p>
     * This performs additional cleanups such as removing unused dimension(s), resetting single-cell sparsity metrics
     * from the {@link BioAssay}s, etc.
     * @see ExpressionExperimentService#removeRawDataVectors(ExpressionExperiment, QuantitationType)
     * @return the number of vectors removed
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeAggregatedVectors( ExpressionExperiment ee, QuantitationType qt );
}
