package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;

public class SingleCellSparsityMetricsUpdaterCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateSingleCellSparsityMetrics";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Update sparsity metrics for single cell datasets";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        singleCellExpressionExperimentService.updateSparsityMetrics( expressionExperiment );
        addSuccessObject( expressionExperiment, "Updated sparsity metrics." );
        try {
            refreshExpressionExperimentFromGemmaWeb( expressionExperiment, false, false );
        } catch ( Exception e ) {
            addWarningObject( expressionExperiment, "Failed to refresh dataset from Gemma Web.", e );
        }
    }
}
