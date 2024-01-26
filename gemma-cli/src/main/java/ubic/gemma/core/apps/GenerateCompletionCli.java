package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.CLI;

import javax.annotation.Nullable;
import java.util.List;

public class GenerateCompletionCli extends AbstractCLI {

    @Autowired
    private List<CLI> clis;

    @Override
    protected void buildOptions( Options options ) {

    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {

    }

    @Override
    protected void doWork() throws Exception {

    }

    @Nullable
    @Override
    public String getCommandName() {
        return null;
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return null;
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return null;
    }
}
