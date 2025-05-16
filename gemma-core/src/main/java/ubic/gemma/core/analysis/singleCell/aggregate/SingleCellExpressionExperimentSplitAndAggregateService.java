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
 * High-level service for splitting and aggregating single-cell expression experiments.
 * <p>
 * This allows for splitting and aggregating single-cell data in a single transaction. It also support re-aggregation
 * @author poirigui
 * @see SingleCellExpressionExperimentSplitService
 * @see SingleCellExpressionExperimentSplitAndAggregateService
 */
public interface SingleCellExpressionExperimentSplitAndAggregateService {

    /**
     * Split and aggregate by cell type.
     * @see SingleCellExpressionExperimentSplitService#splitByCellType(ExpressionExperiment, SplitConfig)
     * @see SingleCellExpressionExperimentAggregatorService#aggregateVectorsByCellType(ExpressionExperiment, List, AggregateConfig)
     */
    QuantitationType splitAndAggregateByCellType( ExpressionExperiment expressionExperiment, SplitConfig splitConfig,
            AggregateConfig config );

    /**
     * Split and aggregate by any cell-level characteristics.
     * @see SingleCellExpressionExperimentSplitService#split(ExpressionExperiment, CellLevelCharacteristics, ExperimentalFactor, Map, SplitConfig)
     * @see SingleCellExpressionExperimentAggregatorService#aggregateVectors(ExpressionExperiment, QuantitationType, List, CellLevelCharacteristics, ExperimentalFactor, Map, AggregateConfig)
     */
    QuantitationType splitAndAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt,
            CellLevelCharacteristics cta, ExperimentalFactor cellTypeFactor, Map<Characteristic, FactorValue> c2f,
            SplitConfig splitConfig,
            AggregateConfig config );

    /**
     * Re-aggregate a dataset by cell type.
     * @param dimension  a dimension to reuse for aggregating
     * @param previousQt a previous quantitation type the re-aggregated one is replacing, it will be removed prior to
     *                   the new one being added. Ignored if null.
     * @see SingleCellExpressionExperimentAggregatorService#aggregateVectorsByCellType(ExpressionExperiment, List, AggregateConfig)
     */
    QuantitationType redoAggregateByCellType( ExpressionExperiment expressionExperiment, BioAssayDimension dimension,
            @Nullable QuantitationType previousQt, AggregateConfig config );

    /**
     * Re-aggregate a dataset by any cell-level characteristics.
     * @param dimension  a dimension to reuse for aggregating
     * @param previousQt a previous quantitation type the re-aggregated one is replacing, it will be removed prior to
     *                   the new one being added. Ignored if null.
     * @see SingleCellExpressionExperimentAggregatorService#aggregateVectors(ExpressionExperiment, QuantitationType, List, CellLevelCharacteristics, ExperimentalFactor, Map, AggregateConfig)
     */
    QuantitationType redoAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt,
            CellLevelCharacteristics clc, ExperimentalFactor factor, Map<Characteristic, FactorValue> c2f,
            BioAssayDimension dimension, @Nullable QuantitationType previousQt, AggregateConfig config );
}
