package ubic.gemma.apps;

import org.apache.commons.cli.Options;
import org.ocpsoft.prettytime.shade.edu.emory.mathcs.backport.java.util.Collections;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.cli.util.CliContext;

import javax.annotation.Nullable;
import java.util.List;

public class TestCli implements CLI {

    @Nullable
    @Override
    public String getCommandName() {
        return "";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return null;
    }

    @Override
    public Options getOptions() {
        return null;
    }

    @Override
    public boolean allowPositionalArguments() {
        return false;
    }

    @Override
    public int executeCommand( CliContext ctx ) {
        return 0;
    }
}
