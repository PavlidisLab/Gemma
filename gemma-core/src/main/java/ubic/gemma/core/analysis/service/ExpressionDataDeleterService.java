package ubic.gemma.core.analysis.service;

import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExpressionExperimentAggregateService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

/**
 * A high-level service for deleting data and their associated files.
 *
 * @author poirigui
 */
public interface ExpressionDataDeleterService {

    /**
     * @see SingleCellExpressionExperimentService#removeSingleCellDataVectors(ExpressionExperiment, QuantitationType)
     * @see ExpressionDataFileService#deleteAllDataFiles(ExpressionExperiment, QuantitationType)
     */
    void deleteSingleCellData( ExpressionExperiment ee, QuantitationType qt );

    /**
     * Delete raw data vectors and associated data files.
     * <p>
     * This method will detect aggregated single-cell data vector and perform additional cleanups.
     *
     * @see ExpressionExperimentService#removeRawDataVectors(ExpressionExperiment, QuantitationType)
     * @see SingleCellExpressionExperimentAggregateService#removeAggregatedVectors(ExpressionExperiment, QuantitationType, boolean)
     * @see ExpressionDataFileService#deleteAllDataFiles(ExpressionExperiment, QuantitationType)
     */
    void deleteRawData( ExpressionExperiment ee, QuantitationType qt );

    /**
     * @see ProcessedExpressionDataVectorService#removeProcessedDataVectors(ExpressionExperiment)
     * @see ExpressionDataFileService#deleteAllProcessedDataFiles(ExpressionExperiment)
     */
    void deleteProcessedData( ExpressionExperiment ee );
}
