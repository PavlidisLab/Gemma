package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import javax.annotation.Nullable;
import java.util.Date;

import static ubic.gemma.cli.util.OptionsUtils.addDateOption;

public class UpdateEe2AdCli extends AbstractAuthenticatedCLI {

    private static final String
            SINCE_OPTION = "s",
            TRUNCATE_OPTION = "truncate";

    private Date sinceLastUpdate;
    private boolean truncate;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private GemmaRestApiClient gemmaRestApiClient;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateEe2Ad";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Update the EXPRESSION_EXPERIMENT2ARRAY_DESIGN table.";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        addDateOption( SINCE_OPTION, "since", "Only update platforms from experiments updated since the given date", options );
        options.addOption( TRUNCATE_OPTION, "truncate", false, "Truncate the table before updating it" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( SINCE_OPTION ) ) {
            sinceLastUpdate = commandLine.getParsedOptionValue( SINCE_OPTION );
        } else {
            sinceLastUpdate = null;
        }
        truncate = commandLine.hasOption( TRUNCATE_OPTION );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        int updated = tableMaintenanceUtil.updateExpressionExperiment2ArrayDesignEntries( sinceLastUpdate, truncate );
        if ( updated > 0 ) {
            try {
                gemmaRestApiClient.perform( "/datasets/platforms/refresh" );
                log.info( "Refreshed EE2AD associations from " + gemmaRestApiClient.getHostUrl() );
            } catch ( Exception e ) {
                log.warn( "Failed to refresh EE2AD from " + gemmaRestApiClient.getHostUrl(), e );
            }
        }
    }
}
