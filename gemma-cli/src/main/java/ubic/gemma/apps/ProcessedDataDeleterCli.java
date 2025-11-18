package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataDeleterService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class ProcessedDataDeleterCli extends ExpressionExperimentVectorsManipulatingCli<ProcessedExpressionDataVector> {

    @Autowired
    private ExpressionDataDeleterService expressionDataDeleterService;

    public ProcessedDataDeleterCli() {
        super( ProcessedExpressionDataVector.class );
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "deleteProcessedData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Delete processed expression data";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        super.processExpressionExperiment( expressionExperiment );
        refreshExpressionExperimentFromGemmaWebSilently( expressionExperiment, true, true );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        expressionDataDeleterService.deleteProcessedData( ee );
        addSuccessObject( ee, qt, "Deleted processed data." );
    }
}
