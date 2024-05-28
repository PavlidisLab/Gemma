package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

@Component
public class RefreshExperimentCli extends ExpressionExperimentManipulatingCLI {

    private boolean refreshVectors;

    private boolean refreshReports;

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
        return "Refresh the given experiments on the Gemma Website";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addOption( "v", "refreshVectors", false, "Refresh raw and processed data vectors" );
        options.addOption( "r", "refreshReports", false, "Refresh experiment reports (i.e. batch information, diff ex. analyses, etc.)" );
        addThreadsOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        refreshVectors = commandLine.hasOption( 'v' );
        refreshReports = commandLine.hasOption( 'r' );
    }

    @Override
    protected void doWork() {
        for ( BioAssaySet bas : expressionExperiments ) {
            getBatchTaskExecutor().execute( () -> {
                if ( bas instanceof ExpressionExperiment ) {
                    try {
                        refreshExpressionExperimentFromGemmaWeb( ( ExpressionExperiment ) bas, refreshVectors, refreshReports );
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
