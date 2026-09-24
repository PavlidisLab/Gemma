package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.options.DataFileOptionValue;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import org.springframework.lang.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Writes differential expression analysis files to disk.
 *
 * @author poirigui
 */
public class DifferentialExpressionAnalysisWriterCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Nullable
    private String analysisIdentifier;

    private DataFileOptionValue result;

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
        addDataFileOptions( options, "differential expression data", true );
        addSingleExperimentOption( options, "a", "analysis", true, "Identifier for an analysis." );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        analysisIdentifier = commandLine.getOptionValue( "a" );
        result = getDataFileOptionValue( commandLine, true );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        if ( analysisIdentifier != null ) {
            Path dest;
            DifferentialExpressionAnalysis analysis = entityLocator.locateDiffExAnalysis( expressionExperiment, analysisIdentifier );
            if ( result.isStandardLocation() ) {
                List<Path> written = new ArrayList<>(), found = new ArrayList<>();
                writeOrLocateInStandardLocation( analysis, written, found );
                if ( !found.isEmpty() ) {
                    addSuccessObject( expressionExperiment, String.format( "Found an existing differential expression analysis file at %s; use -%s to regenerate it.",
                            found.get( 0 ), FORCE_OPTION ) );
                    return;
                }
                dest = written.get( 0 );
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
                List<Path> written = new ArrayList<>(), found = new ArrayList<>();
                for ( DifferentialExpressionAnalysis analysis : differentialExpressionAnalysisService.findByExperiment( expressionExperiment, true ) ) {
                    writeOrLocateInStandardLocation( analysis, written, found );
                }
                if ( !found.isEmpty() ) {
                    addSuccessObject( String.format( "Found existing differential expression analysis files at %s; use -%s to regenerate them.",
                            found.stream().map( Path::toString ).collect( Collectors.joining( ", " ) ), FORCE_OPTION ) );
                    if ( written.isEmpty() ) {
                        return;
                    }
                }
                dest = written;
            } else if ( result.getOutputDir() != null ) {
                dest = expressionDataFileService.writeDiffExAnalysisArchiveFiles( expressionExperiment, result.getOutputDir(), isForce() );
            } else {
                throw new IllegalArgumentException( "Cannot write multiple diff. ex. archive files to an output file or standard output." );
            }
            addSuccessObject( String.format( "Wrote differential expression analysis files to %s.",
                    dest.stream().map( Path::toString ).collect( Collectors.joining( ", " ) ) ) );
        }
    }

    /**
     * Write the archive of an analysis in the standard location, or locate an existing one there.
     *
     * @param written receives the path if the archive was written
     * @param found   receives the path if an existing archive was returned as is
     */
    private void writeOrLocateInStandardLocation( DifferentialExpressionAnalysis analysis, List<Path> written, List<Path> found ) throws IOException {
        Path standardPath;
        try ( LockedPath lockedPath = expressionDataFileService.getDataFile( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis ), false ) ) {
            standardPath = lockedPath.getPath();
        }
        Object before = getFileVersion( standardPath );
        Path dest;
        try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileById( analysis.getId(), isForce() ) ) {
            dest = lockedPath.getPath();
        }
        if ( before != null && before.equals( getFileVersion( dest ) ) ) {
            found.add( dest );
        } else {
            written.add( dest );
        }
    }

    /**
     * The identity and modification time of the file at a path, or null if there is none. Writing a file in the
     * standard location replaces it with a new one, which changes both.
     */
    @Nullable
    private static Object getFileVersion( Path path ) throws IOException {
        try {
            BasicFileAttributes attributes = Files.readAttributes( path, BasicFileAttributes.class );
            return Arrays.asList( attributes.fileKey(), attributes.lastModifiedTime() );
        } catch ( NoSuchFileException e ) {
            return null;
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
