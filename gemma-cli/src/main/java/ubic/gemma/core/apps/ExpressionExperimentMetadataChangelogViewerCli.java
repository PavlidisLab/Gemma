package ubic.gemma.core.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionMetadataChangelogFileService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * CLI tool for viewing the changelog file of an experiment.
 * @author poirigui
 */
public class ExpressionExperimentMetadataChangelogViewerCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionMetadataChangelogFileService expressionMetadataChangelogFileService;

    @Nullable
    @Override
    public String getCommandName() {
        return "viewChangelog";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "View changelogs for the given experiment's metadata.";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        try {
            getCliContext().getOutputStream().print( expressionMetadataChangelogFileService.readChangelog( expressionExperiment ) );
        } catch ( IOException e ) {
            addErrorObject( expressionExperiment, e );
        }
    }
}
