package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import javax.annotation.Nullable;
import java.util.Date;

import static ubic.gemma.cli.util.EntityOptionsUtils.addPlatformOption;
import static ubic.gemma.cli.util.OptionsUtils.*;

public class UpdateGene2CsCli extends AbstractAuthenticatedCLI {

    private static final String
            PLATFORM_OPTION = "a",
            TRUNCATE_OPTION = "truncate",
            SINCE_OPTION = "s",
            FORCE_OPTION = "f";

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private GemmaRestApiClient gemmaRestApiClient;

    @Autowired
    private EntityLocator entityLocator;

    @Nullable
    private String platformIdentifier;
    @Nullable
    private Date sinceLastUpdate;
    private boolean truncate;
    private boolean force;

    @Override
    public String getCommandName() {
        return "updateGene2Cs";
    }

    @Override
    public String getShortDesc() {
        return "Update the GENE2CS table.";
    }

    @Override
    protected void buildOptions( Options options ) {
        addPlatformOption( options, PLATFORM_OPTION, "array", "Only update GENE2CS entries for a particular platform." );
        addDateOption( SINCE_OPTION, "since", "Only update GENE2CS entries from platforms update since the given date", options );
        options.addOption( TRUNCATE_OPTION, "truncate", false, "Truncate the table before updating it" );
        options.addOption( FORCE_OPTION, "force", false, "Force update of the GENE2CS table, even if no platforms have been updated since the last update." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        sinceLastUpdate = OptionsUtils.getParsedOptionValue( commandLine, SINCE_OPTION,
                requires( allOf( toBeUnset( PLATFORM_OPTION ), toBeUnset( TRUNCATE_OPTION ) ) ) );
        truncate = hasOption( commandLine, TRUNCATE_OPTION,
                requires( allOf( toBeUnset( PLATFORM_OPTION ), toBeUnset( SINCE_OPTION ) ) ) );
        force = commandLine.hasOption( FORCE_OPTION );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        int updated;
        if ( platformIdentifier != null ) {
            ArrayDesign platform = entityLocator.locateArrayDesign( platformIdentifier );
            updated = tableMaintenanceUtil.updateGene2CsEntries( platform, force );
        } else {
            updated = tableMaintenanceUtil.updateGene2CsEntries( sinceLastUpdate, truncate, force );
        }
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
