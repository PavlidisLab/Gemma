package ubic.gemma.core.analysis.singleCell.aggregate;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.List;
import java.util.Map;

/**
 * Aggregate single-cell vectors.
 * @author poirigui
 */
public interface SingleCellExpressionExperimentAggregateService {

    /**
     * Aggregate preferred single-cell data vectors by the preferred cell type assignment and the only cell type
     * factor of the experiment.
     * @throws IllegalStateException if there is no preferred cell type assignment or if there is no cell type factor in
     * the experimental design
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    QuantitationType aggregateVectorsByCellType( ExpressionExperiment ee, List<BioAssay> cellBAs, SingleCellAggregationConfig config ) throws SingleCellAggregationException;

    /**
     * Aggregate preferred single-cell data vectors by the given cell-level characteristics.
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
     * @param qt                       single-cell quantitation type to aggregate
     * @param cellBAs                  samples to aggregate vectors for
     * @param cellLevelCharacteristics cell-level characteristics to use to aggregate
     * @param cellTypeMapping          association between cell-level characteristics and factor values
     * @return the quantitation type of the newly created vectors
     * @throws UnsupportedScaleTypeForSingleCellAggregationException if data of the given scale type cannot be aggregated
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    QuantitationType aggregateVectors( ExpressionExperiment ee, QuantitationType qt, List<BioAssay> cellBAs,
            CellLevelCharacteristics cellLevelCharacteristics, ExperimentalFactor factor,
            Map<Characteristic, FactorValue> cellTypeMapping,
            SingleCellAggregationConfig config ) throws SingleCellAggregationException;

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

    /**
     * Remove aggregated vectors for the given quantitation type.
     * @param keepDimension if true, preserve the {@link ubic.gemma.model.expression.bioAssayData.BioAssayDimension} of
     *                      the vectors. Use this to re-aggregate the vectors with the same dimension.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    int removeAggregatedVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension );
}
