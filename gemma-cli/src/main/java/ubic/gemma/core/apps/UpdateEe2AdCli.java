package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import javax.annotation.Nullable;
import java.util.Date;

public class UpdateEe2AdCli extends AbstractAuthenticatedCLI {

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    private Date since;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateEe2Ad";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Update the EXPRESSION_EXPERIMENT2ARRAY_DESIGN table";
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        addDateOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        since = getLimitingDate();
    }

    @Override
    protected void doWork() throws Exception {
        tableMaintenanceUtil.updateExpressionExperiment2ArrayDesignEntries( since );
    }
}
