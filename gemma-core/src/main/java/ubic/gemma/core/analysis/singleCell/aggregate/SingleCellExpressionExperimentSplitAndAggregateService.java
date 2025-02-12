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
    QuantitationType splitAndAggregateByCellType( ExpressionExperiment expressionExperiment, SplitConfig splitConfig, AggregateConfig config );

    QuantitationType splitAndAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt,
            CellLevelCharacteristics cta, ExperimentalFactor cellTypeFactor, Map<Characteristic, FactorValue> c2f,
            SplitConfig splitConfig,
            AggregateConfig config );

    /**
     * Re-aggregate a dataset by cell type.
     * @see SingleCellExpressionExperimentAggregatorService#aggregateVectorsByCellType(ExpressionExperiment, List, AggregateConfig)
     */
    QuantitationType redoAggregateByCellType( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, @Nullable QuantitationType previousQt, AggregateConfig config );

    /**
     * Re-aggregate a dataset.
     * @param expressionExperiment
     * @param scQt                 a single-cell quantitation type containing data to aggregate
     * @param cta                  a cell-level characteristics to aggregate by
     * @param cellTypeFactor       a factor representing the cell type
     * @param c2f                  a mapping of cell characteristics to factor values
     * @param dimension            a dimension
     * @param previousQt           a previous quantitation type the re-aggregated one is replacing, it will be removed
     *                             prior to the new one being added. Ignored if null.
     * @return
     */
    QuantitationType redoAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt, CellLevelCharacteristics cta, ExperimentalFactor cellTypeFactor, Map<Characteristic, FactorValue> c2f, BioAssayDimension dimension, @Nullable QuantitationType previousQt, AggregateConfig config );
}
