package ubic.gemma.core.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionChangelogFileService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author poirigui
 */
public class ExpressionExperimentChangelogViewerCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionChangelogFileService expressionChangelogFileService;

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
            System.out.print( expressionChangelogFileService.readChangelog( expressionExperiment ) );
        } catch ( IOException e ) {
            addErrorObject( expressionExperiment, e );
        }
    }
}
