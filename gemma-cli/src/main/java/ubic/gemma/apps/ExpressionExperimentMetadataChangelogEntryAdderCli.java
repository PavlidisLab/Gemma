package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionMetadataChangelogFileService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * CLI tool for adding manual entries to the changelog file of an experiment.
 * @author poirigui
 */
public class ExpressionExperimentMetadataChangelogEntryAdderCli extends ExpressionExperimentManipulatingCLI {

    private static final String CHANGELOG_ENTRY_OPTION = "ce";

    @Autowired
    private ExpressionMetadataChangelogFileService expressionMetadataChangelogFileService;

    @Nullable
    private String changelogEntry;

    public ExpressionExperimentMetadataChangelogEntryAdderCli() {
        setRequireLogin();
        setSingleExperimentMode();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "addChangelogEntry";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Add a record to the changelog for the given experiment.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( CHANGELOG_ENTRY_OPTION, "changelog-entry", true, "The changelog entry to add. If not supplied, a text editor will be prompted." );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        changelogEntry = commandLine.getOptionValue( CHANGELOG_ENTRY_OPTION );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws IOException {
        try {
            String buf;
            if ( changelogEntry != null ) {
                buf = changelogEntry;
            } else {
                buf = readChangelogEntryFromConsole( expressionExperiment, null );
            }
            expressionMetadataChangelogFileService.addChangelogEntry( expressionExperiment, buf );
            addSuccessObject( expressionExperiment, "Added entry to the changelog." );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
    }
}
