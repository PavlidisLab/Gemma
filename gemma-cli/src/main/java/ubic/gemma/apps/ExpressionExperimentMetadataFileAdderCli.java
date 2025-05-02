package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionMetadataChangelogFileService;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Add a metadata file to an experiment and record an entry in the changelog file.
 * @author poirigui
 */
public class ExpressionExperimentMetadataFileAdderCli extends ExpressionExperimentManipulatingCLI {

    private static final String CHANGELOG_ENTRY_OPTION = "ce";

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private ExpressionMetadataChangelogFileService expressionMetadataChangelogFileService;

    private Path filename;
    @Nullable
    private ExpressionExperimentMetaFileType fileType;
    @Nullable
    private String changelogEntry;

    public ExpressionExperimentMetadataFileAdderCli() {
        setRequireLogin();
        setAllowPositionalArguments();
        setSingleExperimentMode();
    }

    @Override
    public String getCommandName() {
        return "addMetadataFile";
    }

    @Override
    public String getShortDesc() {
        return "Add a metadata file to the given experiment and record an entry in the changelog file.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        OptionsUtils.addEnumOption( options, "fileType", "file-type", "Type of metadata file to be added. If left unset, a generic metadata file will be added.", ExpressionExperimentMetaFileType.class );
        options.addOption( CHANGELOG_ENTRY_OPTION, "changelog-entry", true, "Changelog entry to be added. If not supplied, a text editor will be prompted." );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.getArgList().size() != 1 ) {
            throw new ParseException( "Exactly one positional argument is required." );
        }
        filename = Paths.get( commandLine.getArgList().get( 0 ) );
        fileType = OptionsUtils.getEnumOptionValue( commandLine, "fileType" );
        changelogEntry = commandLine.getOptionValue( CHANGELOG_ENTRY_OPTION );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        try {
            String buf = generateChangelog( expressionExperiment );
            if ( fileType != null ) {
                expressionDataFileService.copyMetadataFile( expressionExperiment, filename, fileType, isForce() );
            }
            expressionDataFileService.copyMetadataFile( expressionExperiment, filename, filename.getFileName().toString(), isForce() );
            expressionMetadataChangelogFileService.addChangelogEntry( expressionExperiment, buf );
        } catch ( IOException | InterruptedException e ) {
            throw new RuntimeException( e );
        }
    }

    private String generateChangelog( ExpressionExperiment expressionExperiment ) throws IOException, InterruptedException {
        if ( changelogEntry != null ) {
            return changelogEntry;
        }
        String defaultText;
        try ( LockedPath mf = getMetadataFile( expressionExperiment ) ) {
            String what = fileType != null ? fileType.getDisplayName() : "metadata file " + mf.getPath().getFileName().toString();
            if ( Files.exists( mf.getPath() ) ) {
                defaultText = "Replace an existing " + what + ".";
                if ( !isForce() ) {
                    throw new IllegalStateException( "Metadata file already exist and the -force option is not set." );
                }
            } else {
                defaultText = "Add a new " + what + ".";
            }
        }
        return readChangelogEntryFromConsole( expressionExperiment, defaultText );
    }

    private LockedPath getMetadataFile( ExpressionExperiment expressionExperiment ) throws IOException {
        if ( fileType != null ) {
            return expressionDataFileService.getMetadataFile( expressionExperiment, fileType, false )
                    // this only happens for directory-structured metadata
                    .orElseThrow( () -> new UnsupportedOperationException( "Directory-structured metadata is not supported." ) );
        } else {
            return expressionDataFileService.getMetadataFile( expressionExperiment, filename.getFileName().toString(), false );
        }
    }
}
