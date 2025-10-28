package ubic.gemma.core.analysis.singleCell.aggregate;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * High-level service for creating subsets and aggregating single-cell expression experiments.
 * <p>
 * This allows for subsetting and aggregating single-cell data in a single transaction. It also supports re-aggregation
 * from an existing subset structure.
 *
 * @author poirigui
 * @see SingleCellExpressionExperimentSubSetService
 * @see SingleCellExpressionExperimentCreateSubSetsAndAggregateService
 */
public interface SingleCellExpressionExperimentCreateSubSetsAndAggregateService {

    /**
     * Create subsets and aggregate by cell type.
     *
     * @see SingleCellExpressionExperimentSubSetService#createSubSetsByCellType(ExpressionExperiment, SingleCellExperimentSubSetsCreationConfig)
     * @see SingleCellExpressionExperimentAggregateService#aggregateVectorsByCellType(ExpressionExperiment, List, SingleCellAggregationConfig)
     */
    QuantitationType createSubSetsAndAggregateByCellType( ExpressionExperiment expressionExperiment, SingleCellExperimentSubSetsCreationConfig singleCellExperimentSubSetsCreationConfig,
            SingleCellAggregationConfig config );

    /**
     * Create subsets and aggregate by any cell-level characteristics.
     *
     * @see SingleCellExpressionExperimentSubSetService#createSubSets(ExpressionExperiment, CellLevelCharacteristics, ExperimentalFactor, Map, SingleCellExperimentSubSetsCreationConfig)
     * @see SingleCellExpressionExperimentAggregateService#aggregateVectors(ExpressionExperiment, QuantitationType, List, CellLevelCharacteristics, ExperimentalFactor, Map, SingleCellAggregationConfig)
     */
    QuantitationType createSubSetsAndAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt,
            CellLevelCharacteristics cta, ExperimentalFactor cellTypeFactor, Map<Characteristic, FactorValue> c2f,
            SingleCellExperimentSubSetsCreationConfig singleCellExperimentSubSetsCreationConfig,
            SingleCellAggregationConfig config );

    /**
     * Re-aggregate a dataset by cell type.
     *
     * @param dimension  a dimension to reuse for aggregating
     * @param previousQt a previous quantitation type the re-aggregated one is replacing, it will be removed prior to
     *                   the new one being added. Ignored if null.
     * @see SingleCellExpressionExperimentAggregateService#aggregateVectorsByCellType(ExpressionExperiment, List, SingleCellAggregationConfig)
     */
    QuantitationType redoAggregateByCellType( ExpressionExperiment expressionExperiment, BioAssayDimension dimension,
            @Nullable QuantitationType previousQt, SingleCellAggregationConfig config );

    /**
     * Re-aggregate a dataset by any cell-level characteristics.
     *
     * @param dimension  a dimension to reuse for aggregating
     * @param previousQt a previous quantitation type the re-aggregated one is replacing, it will be removed prior to
     *                   the new one being added. Ignored if null.
     * @see SingleCellExpressionExperimentAggregateService#aggregateVectors(ExpressionExperiment, QuantitationType, List, CellLevelCharacteristics, ExperimentalFactor, Map, SingleCellAggregationConfig)
     */
    QuantitationType redoAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt,
            CellLevelCharacteristics clc, ExperimentalFactor factor, Map<Characteristic, FactorValue> c2f,
            BioAssayDimension dimension, @Nullable QuantitationType previousQt, SingleCellAggregationConfig config );
}
