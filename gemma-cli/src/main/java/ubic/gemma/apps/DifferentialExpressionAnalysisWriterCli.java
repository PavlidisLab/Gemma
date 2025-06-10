package ubic.gemma.apps;

import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Writes differential expression analysis files to disk.
 * @author poirigui
 */
public class DifferentialExpressionAnalysisWriterCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Override
    public String getCommandName() {
        return "getDiffExAnalysis";
    }

    @Override
    public String getShortDesc() {
        return "Write differential expression data files to the standard location.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        addForceOption( options );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        expressionDataFileService.writeOrLocateDiffExpressionDataFiles( expressionExperiment, isForce() );
    }
}
