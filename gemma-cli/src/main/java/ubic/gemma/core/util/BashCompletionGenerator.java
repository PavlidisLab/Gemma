package ubic.gemma.core.util;

import org.apache.commons.cli.Options;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;

public class BashCompletionGenerator implements CompletionGenerator {
    @Override
    public void generateCompletion( Options options, PrintWriter writer ) throws IOException {

    }

    @Override
    public void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) throws IOException {

    }
}