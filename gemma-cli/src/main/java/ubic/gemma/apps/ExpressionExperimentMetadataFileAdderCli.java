package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionMetadataChangelogFileService;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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
        options.addOption( CHANGELOG_ENTRY_OPTION, "changelog-entry", true, "Changelog entry to be add. If not supplied, a text editor will be prompted." );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.getArgList().size() != 1 ) {
            throw new ParseException( "Exactly one positional argument is required." );
        }
        filename = Paths.get( commandLine.getArgList().get( 0 ) );
        changelogEntry = commandLine.getOptionValue( CHANGELOG_ENTRY_OPTION );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        try {
            String buf;
            if ( changelogEntry != null ) {
                buf = changelogEntry;
            } else {
                String defaultText;
                Optional<LockedPath> mf = expressionDataFileService.getMetadataFile( expressionExperiment, filename.getFileName().toString(), false );
                if ( mf.isPresent() ) {
                    defaultText = "Replace an existing metadata file " + filename.getFileName().toString() + ".";
                    mf.get().close();
                    if ( !isForce() ) {
                        throw new IllegalStateException( "Metadata file already exist and the -force option is not set." );
                    }
                } else {
                    defaultText = "Add a new metadata file " + filename.getFileName().toString() + ".";
                }
                buf = readChangelogEntryFromConsole( expressionExperiment, defaultText );
            }
            expressionDataFileService.copyMetadataFile( expressionExperiment, filename, filename.getFileName().toString(), isForce() );
            expressionMetadataChangelogFileService.addChangelogEntry( expressionExperiment, buf );
        } catch ( IOException | InterruptedException e ) {
            throw new RuntimeException( e );
        }
    }
}
