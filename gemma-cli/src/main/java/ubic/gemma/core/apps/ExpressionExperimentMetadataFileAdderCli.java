package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionChangelogFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Add a metadata file to an experiment.
 * @author poirigui
 */
public class ExpressionExperimentMetadataFileAdderCli extends ExpressionExperimentManipulatingCLI {

    private static final String CHANGELOG_ENTRY_OPTION = "ce";

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private ExpressionChangelogFileService expressionChangelogFileService;

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
                buf = readChangelogEntryFromConsole( expressionExperiment );
            }
            expressionDataFileService.copyMetadataFile( expressionExperiment, filename, filename.getFileName().toString(), isForce() );
            expressionChangelogFileService.appendToChangelog( expressionExperiment, buf );
        } catch ( IOException | InterruptedException e ) {
            throw new RuntimeException( e );
        }
    }
}
