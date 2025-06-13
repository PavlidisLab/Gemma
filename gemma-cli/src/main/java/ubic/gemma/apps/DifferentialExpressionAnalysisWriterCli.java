package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

    private String analysisIdentifier;

    private ExpressionDataFileResult result;

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
        addExpressionDataFileOptions( options, "differential expression data", false, false );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        result = getExpressionDataFileResult( commandLine );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        if ( result.isStandardLocation() ) {
            expressionDataFileService.writeOrLocateDiffExpressionDataFiles( expressionExperiment, isForce() );
        } else if ( result.getOutputDir() != null ) {
            expressionDataFileService.writeDiffExpressionDataFiles( expressionExperiment, result.getOutputDir() );
        }
    }
}
