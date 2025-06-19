package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

/**
 * Writes differential expression analysis files to disk.
 * @author poirigui
 */
public class DifferentialExpressionAnalysisWriterCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private EntityLocator entityLocator;

    @Nullable
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
        addExpressionDataFileOptions( options, "differential expression data" );
        addSingleExperimentOption( options, "a", "analysis", true, "Identifier for an analysis." );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        analysisIdentifier = commandLine.getOptionValue( "a" );
        result = getExpressionDataFileResult( commandLine );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        if ( analysisIdentifier != null ) {
            DifferentialExpressionAnalysis analysis = entityLocator.locateDiffExAnalysis( expressionExperiment, analysisIdentifier );
            if ( result.isStandardLocation() ) {
                try ( LockedPath ignored = expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileById( analysis.getId(), isForce() ) ) {

                }
            } else if ( result.isStandardOutput() ) {
                expressionDataFileService.writeDiffExAnalysisArchiveFileById( analysis.getId(), getCliContext().getOutputStream() );
            } else {
                expressionDataFileService.writeDiffExAnalysisArchiveFileById( analysis.getId(), result.getOutputFile( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis ) ), isForce() );
            }
        } else {
            if ( result.isStandardLocation() ) {
                expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFiles( expressionExperiment, isForce() );
            } else if ( result.getOutputDir() != null ) {
                expressionDataFileService.writeDiffExAnalysisArchiveFiles( expressionExperiment, result.getOutputDir(), isForce() );
            } else {
                throw new IllegalArgumentException( "Cannot write multiple diff. ex. archive files to an output file or standard output." );
            }
        }
    }
}
