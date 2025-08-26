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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

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
        addExpressionDataFileOptions( options, "differential expression data", true );
        addSingleExperimentOption( options, "a", "analysis", true, "Identifier for an analysis." );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        analysisIdentifier = commandLine.getOptionValue( "a" );
        result = getExpressionDataFileResult( commandLine, true );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        if ( analysisIdentifier != null ) {
            Path dest;
            DifferentialExpressionAnalysis analysis = entityLocator.locateDiffExAnalysis( expressionExperiment, analysisIdentifier );
            if ( result.isStandardLocation() ) {
                try ( LockedPath ignored = expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileById( analysis.getId(), isForce() ) ) {
                    dest = ignored.getPath();
                }
            } else if ( result.isStandardOutput() ) {
                dest = null;
                expressionDataFileService.writeDiffExAnalysisArchiveFileById( analysis.getId(), getCliContext().getOutputStream() );
            } else {
                dest = result.getOutputFile( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis ) );
                try ( OutputStream out = openOutputFile( dest ) ) {
                    expressionDataFileService.writeDiffExAnalysisArchiveFileById( analysis.getId(), out );
                }
            }
            addSuccessObject( expressionExperiment, String.format( "Wrote differential expression analysis file to %s.",
                    dest != null ? dest : "the standard output" ) );
        } else {
            Collection<Path> dest;
            if ( result.isStandardLocation() ) {
                dest = expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFiles( expressionExperiment, isForce() );
            } else if ( result.getOutputDir() != null ) {
                dest = expressionDataFileService.writeDiffExAnalysisArchiveFiles( expressionExperiment, result.getOutputDir(), isForce() );
            } else {
                throw new IllegalArgumentException( "Cannot write multiple diff. ex. archive files to an output file or standard output." );
            }
            addSuccessObject( String.format( "Wrote differential expression analysis files to %s.",
                    dest.stream().map( Path::toString ).collect( Collectors.joining( ", " ) ) ) );
        }
    }

    private OutputStream openOutputFile( Path fileName ) throws IOException {
        if ( fileName.toString().endsWith( ".gz" ) ) {
            return new GZIPOutputStream( Files.newOutputStream( fileName ) );
        } else {
            return Files.newOutputStream( fileName );
        }
    }
}
