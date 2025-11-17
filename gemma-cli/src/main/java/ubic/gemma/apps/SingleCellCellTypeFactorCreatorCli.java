package ubic.gemma.apps;

import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

public class SingleCellCellTypeFactorCreatorCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    public SingleCellCellTypeFactorCreatorCli() {
    }

    @Override
    public String getCommandName() {
        return "createCellTypeFactor";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        addForceOption( options );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        singleCellExpressionExperimentService.createCellTypeFactor( expressionExperiment, true, isForce() );
        refreshExpressionExperimentFromGemmaWebSilently( expressionExperiment, false, false );
    }
}
