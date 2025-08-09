package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.DataDeleterService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class ProcessedDataDeleterCli extends ExpressionExperimentVectorsManipulatingCli<ProcessedExpressionDataVector> {

    @Autowired
    private DataDeleterService dataDeleterService;

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
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        dataDeleterService.deleteProcessedData( ee );
        addSuccessObject( ee, qt, "Deleted processed data." );
    }
}
