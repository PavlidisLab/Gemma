package ubic.gemma.core.util;

import org.apache.commons.cli.Options;
import ubic.gemma.core.apps.GemmaCLI;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Map;
import java.util.SortedMap;

public abstract class AbstractCompletionGenerator implements CompletionGenerator {

    protected final Options generalOptions;
    private final SortedMap<GemmaCLI.CommandGroup, SortedMap<String, CLI>> commands;

    protected AbstractCompletionGenerator( Options generalOptions, SortedMap<GemmaCLI.CommandGroup, SortedMap<String, CLI>> commands ) {
        this.generalOptions = generalOptions;
        this.commands = commands;
    }

    @Override
    public void generateCompletion( PrintWriter completionWriter ) {
        generateGeneralCompletion( generalOptions, completionWriter );
        for ( Map.Entry<GemmaCLI.CommandGroup, SortedMap<String, CLI>> group : commands.entrySet() ) {
            for ( CLI cli : group.getValue().values() ) {
                generateSubcommandCompletion( cli.getCommandName(), cli.getOptions(), cli.getShortDesc(), cli.allowPositionalArguments(), completionWriter );
            }
        }
    }

    protected abstract void generateGeneralCompletion( Options options, PrintWriter writer );

    protected abstract void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer );
}
