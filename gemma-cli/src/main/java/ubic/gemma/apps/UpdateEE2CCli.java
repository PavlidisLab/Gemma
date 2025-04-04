package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import javax.annotation.Nullable;
import java.util.Date;

import static ubic.gemma.cli.util.OptionsUtils.addDateOption;

public class UpdateEE2CCli extends AbstractAuthenticatedCLI {

    private static final String
            TRUNCATE_OPTION = "truncate",
            SINCE_OPTION = "s";

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private GemmaRestApiClient gemmaRestApiClient;

    private boolean truncate;
    private Date sinceLastUpdate;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateEe2c";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Update the EXPRESSION_EXPERIMENT2CHARACTERISTIC table";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( TRUNCATE_OPTION, "truncate", false, "Truncate the table before updating it" );
        addDateOption( SINCE_OPTION, "since", "Only update characteristics from experiments updated since the given date", options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        truncate = commandLine.hasOption( TRUNCATE_OPTION );
        if ( commandLine.hasOption( SINCE_OPTION ) ) {
            sinceLastUpdate = commandLine.getParsedOptionValue( SINCE_OPTION );
        } else {
            sinceLastUpdate = null;
        }
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        int updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( sinceLastUpdate, truncate );
        if ( updated > 0 ) {
            try {
                gemmaRestApiClient.perform( "/datasets/annotations/refresh" );
                log.info( "Refreshed all EE2C associations from " + gemmaRestApiClient.getHostUrl() );
            } catch ( Exception e ) {
                log.warn( "Failed to refresh EE2C from " + gemmaRestApiClient.getHostUrl(), e );
            }
        }
    }
}
