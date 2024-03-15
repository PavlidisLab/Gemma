package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.persistence.service.TableMaintenanceUtil;

import javax.annotation.Nullable;

public class UpdateEE2CCli extends AbstractAuthenticatedCLI {

    private static final String TRUNCATE_OPTION = "truncate";

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    private boolean truncate;

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( TRUNCATE_OPTION, "truncate", false, "Truncate the table before updating it" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        truncate = commandLine.hasOption( TRUNCATE_OPTION );
    }

    @Override
    protected void doWork() throws Exception {
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, truncate );
    }

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
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }
}
