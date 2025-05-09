package ubic.gemma.core.loader.expression;

import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExpressionExperimentAggregatorService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

/**
 * A high-level service for deleting data and their associated files.
 * @author poirigui
 */
public interface DataDeleterService {

    /**
     * @see SingleCellExpressionExperimentService#removeSingleCellDataVectors(ExpressionExperiment, QuantitationType)
     * @see ExpressionDataFileService#deleteAllDataFiles(ExpressionExperiment, QuantitationType)
     */
    void deleteSingleCellData( ExpressionExperiment ee, QuantitationType qt );

    /**
     * @see SingleCellExpressionExperimentAggregatorService#removeAggregatedVectors(ExpressionExperiment, QuantitationType)
     * @see ExpressionDataFileService#deleteAllDataFiles(ExpressionExperiment, QuantitationType)
     */
    void deleteSingleCellDataAggregate( ExpressionExperiment ee, QuantitationType qt );

    /**
     * @see ExpressionExperimentService#removeRawDataVectors(ExpressionExperiment, QuantitationType)
     * @see ExpressionDataFileService#deleteAllDataFiles(ExpressionExperiment, QuantitationType)
     */
    void deleteRawData( ExpressionExperiment ee, QuantitationType qt );

    /**
     * @see ProcessedExpressionDataVectorService#removeProcessedDataVectors(ExpressionExperiment)
     * @see ExpressionDataFileService#deleteAllProcessedDataFiles(ExpressionExperiment)
     */
    void deleteProcessedData( ExpressionExperiment ee );
}
