package ubic.gemma.core.apps;

import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

@Component
public class RefreshExperimentCli extends ExpressionExperimentManipulatingCLI {

    @Value("${gemma.hosturl}")
    private String hostUrl;

    public RefreshExperimentCli() {
        setUseReferencesIfPossible( true );
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "refreshExperiment";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Refresh the given experiments on the Gemma Website at " + hostUrl;
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        addThreadsOption( options );
    }

    @Override
    protected void doWork() {
        for ( BioAssaySet bas : expressionExperiments ) {
            getBatchTaskExecutor().execute( () -> {
                if ( bas instanceof ExpressionExperiment ) {
                    try {
                        refreshExpressionExperimentFromGemmaWeb( ( ExpressionExperiment ) bas, true, true );
                        addSuccessObject( "ExpressionExperiment with ID " + bas.getId() );
                    } catch ( Exception e ) {
                        addErrorObject( "ExpressionExperiment with ID " + bas.getId(), e );
                    }
                } else {
                    addErrorObject( "BioAssaySet with ID " + bas.getId(), "Only ExpressionExperiment can be refreshed." );
                }
            } );
        }
    }
}
