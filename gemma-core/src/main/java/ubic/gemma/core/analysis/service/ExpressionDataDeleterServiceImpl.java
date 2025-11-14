package ubic.gemma.core.analysis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExpressionExperimentAggregateService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

@Service
@Transactional(propagation = Propagation.NEVER)
public class ExpressionDataDeleterServiceImpl implements ExpressionDataDeleterService {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentAggregateService singleCellExpressionExperimentAggregateService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Override
    public void deleteSingleCellData( ExpressionExperiment ee, QuantitationType qt ) {
        singleCellExpressionExperimentService.removeSingleCellDataVectors( ee, qt );
        expressionDataFileService.deleteAllDataFiles( ee, qt );
    }

    @Override
    public void deleteRawData( ExpressionExperiment ee, QuantitationType qt ) {
        ee = expressionExperimentService.thaw( ee );
        if ( singleCellExpressionExperimentAggregateService.isAggregated( ee, qt ) ) {
            singleCellExpressionExperimentAggregateService.removeAggregatedVectors( ee, qt );
        } else {
            expressionExperimentService.removeRawDataVectors( ee, qt );
        }
        expressionDataFileService.deleteAllDataFiles( ee, qt );
    }

    @Override
    public void deleteProcessedData( ExpressionExperiment ee ) {
        ee = expressionExperimentService.thaw( ee );
        processedExpressionDataVectorService.removeProcessedDataVectors( ee );
        expressionDataFileService.deleteAllProcessedDataFiles( ee );
    }
}
