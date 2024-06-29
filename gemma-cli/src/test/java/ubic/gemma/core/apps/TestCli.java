package ubic.gemma.core.apps;

import org.apache.commons.cli.Options;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.core.util.CLI;

public class TestCli implements CLI {

    @Nullable
    @Override
    public String getCommandName() {
        return "";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    @Override
    public Options getOptions() {
        return new Options();
    }

    @Override
    public boolean allowPositionalArguments() {
        return false;
    }

    @Override
    public int executeCommand( String... args ) {
        return 0;
    }
}
