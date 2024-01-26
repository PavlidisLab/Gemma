package ubic.gemma.core.util;

import org.apache.commons.cli.Options;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;

public interface CompletionGenerator {

    default void beforeCompletion( PrintWriter writer ) {
    }

    void generateCompletion( Options options, PrintWriter writer );

    void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer );

    default void afterCompletion( PrintWriter writer ) {

    }
}
