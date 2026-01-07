package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataDeleterService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class RawDataDeleterCli extends ExpressionExperimentVectorsManipulatingCli<RawExpressionDataVector> {

    @Autowired
    private ExpressionDataDeleterService expressionDataDeleterService;

    public RawDataDeleterCli() {
        super( RawExpressionDataVector.class );
        setDefaultToPreferredQuantitationType();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "deleteRawData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Delete raw expression data";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        super.processExpressionExperiment( expressionExperiment );
        refreshExpressionExperimentFromGemmaWebSilently( expressionExperiment, true, true );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) {
        expressionDataDeleterService.deleteRawData( ee, qt );
        addSuccessObject( ee, qt, "Deleted raw data." );
    }
}
