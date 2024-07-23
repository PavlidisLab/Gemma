package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import javax.annotation.Nullable;

public class UpdateGene2CsCli extends AbstractAuthenticatedCLI {

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateGene2Cs";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Update the GENE2CS table";
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.MISC;
    }

    @Override
    protected void buildOptions( Options options ) {

    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {

    }

    @Override
    protected void doWork() throws Exception {
        tableMaintenanceUtil.updateGene2CsEntries();
    }
}
