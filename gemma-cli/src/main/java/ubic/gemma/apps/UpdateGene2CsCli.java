package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import javax.annotation.Nullable;

public class UpdateGene2CsCli extends AbstractAuthenticatedCLI {

    private static final String FORCE_OPTION = "f";

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private GemmaRestApiClient gemmaRestApiClient;

    private boolean force;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateGene2Cs";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Update the GENE2CS table.";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( FORCE_OPTION, "force", false, "Force update of the GENE2CS table, even if no platforms have been updated since the last update." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        force = commandLine.hasOption( FORCE_OPTION );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        int updated = tableMaintenanceUtil.updateGene2CsEntries( force );
        if ( updated > 0 ) {
            try {
                gemmaRestApiClient.perform( "/genes/probes/refresh" );
                log.info( "Refreshed GENE2CS associations from " + gemmaRestApiClient.getHostUrl() );
            } catch ( Exception e ) {
                log.warn( "Failed to update GENE2CS from " + gemmaRestApiClient.getHostUrl(), e );
            }
        }
    }
}
