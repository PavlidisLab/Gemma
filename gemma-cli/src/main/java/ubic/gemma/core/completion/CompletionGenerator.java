package ubic.gemma.core.completion;

import org.apache.commons.cli.Options;

import javax.annotation.Nullable;
import java.io.PrintWriter;

public interface CompletionGenerator {

    /**
     * Executed before any completions has been generated.
     */
    void beforeCompletion( PrintWriter writer );

    /**
     * Generate completion for the given options.
     */
    void generateCompletion( Options options, PrintWriter writer );

    /**
     * Generate completions for the given subcommand.
     */
    void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer );

    /**
     * Executed after all completions have been generated.
     */
    void afterCompletion( PrintWriter writer );
}
