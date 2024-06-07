package ubic.gemma.core.apps;

import org.apache.commons.cli.Options;
import ubic.gemma.core.util.CLI;

import javax.annotation.Nullable;

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
    public GemmaCLI.CommandGroup getCommandGroup() {
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
    public int executeCommand( String... args ) {
        return 0;
    }
}
