package ubic.gemma.core.loader.expression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExpressionExperimentAggregatorService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

@Service
@Transactional(propagation = Propagation.NEVER)
public class DataDeleterServiceImpl implements DataDeleterService {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentAggregatorService singleCellExpressionExperimentAggregatorService;

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
    public void deleteSingleCellDataAggregate( ExpressionExperiment ee, QuantitationType qt ) {
        singleCellExpressionExperimentAggregatorService.removeAggregatedVectors( ee, qt );
        expressionDataFileService.deleteAllDataFiles( ee, qt );
    }

    @Override
    public void deleteRawData( ExpressionExperiment ee, QuantitationType qt ) {
        expressionExperimentService.removeRawDataVectors( ee, qt );
        expressionDataFileService.deleteAllDataFiles( ee, qt );
    }

    @Override
    public void deleteProcessedData( ExpressionExperiment ee ) {
        processedExpressionDataVectorService.removeProcessedDataVectors( ee );
        expressionDataFileService.deleteAllProcessedDataFiles( ee );
    }
}
