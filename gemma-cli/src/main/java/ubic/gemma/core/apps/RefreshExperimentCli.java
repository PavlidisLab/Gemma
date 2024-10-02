package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class RefreshExperimentCli extends ExpressionExperimentManipulatingCLI {

    private boolean refreshVectors;

    private boolean refreshReports;

    public RefreshExperimentCli() {
        super();
        setUseReferencesIfPossible();
    }

    @Override
    public String getCommandName() {
        return "refreshExperiment";
    }

    @Override
    public String getShortDesc() {
        return "Refresh the cache for experiments on the Gemma Website";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addOption( "v", "refreshVectors", false, "Refresh cache of raw and processed data vectors" );
        options.addOption( "r", "refreshReports", false, "Refresh cache of experiment reports (i.e. batch information, diff ex. analyses, etc.)" );
        addThreadsOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        refreshVectors = commandLine.hasOption( 'v' );
        refreshReports = commandLine.hasOption( 'r' );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment bas ) {
        getBatchTaskExecutor().execute( () -> {
            try {
                refreshExpressionExperimentFromGemmaWeb( bas, refreshVectors, refreshReports );
                addSuccessObject( "ExpressionExperiment with ID " + bas.getId() );
            } catch ( Exception e ) {
                addErrorObject( "ExpressionExperiment with ID " + bas.getId(), e );
            }
        } );
    }
}
