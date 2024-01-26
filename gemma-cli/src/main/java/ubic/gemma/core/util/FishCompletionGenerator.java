package ubic.gemma.core.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

public class FishCompletionGenerator implements CompletionGenerator {

    private final String allSubcommands;

    public FishCompletionGenerator( Set<String> allSubcommands ) {
        this.allSubcommands = allSubcommands.stream().filter( StringUtils::isNotBlank ).collect( Collectors.joining( " " ) );
    }

    @Override
    public void generateCompletion( Options options, PrintWriter writer ) {
        for ( Option o : options.getOptions() ) {
            writer.printf( "complete -c gemma-cli -n %s %s%s%s%s%n",
                    // prevents global options from being suggested after a subcommand
                    quoteIfNecessary( "not __fish_seen_subcommand_from " + allSubcommands ),
                    ( o.getOpt().length() == 1 ? " -s " : " -o " ) + o.getOpt(),
                    o.getLongOpt() != null ? " -l " + o.getLongOpt() : "",
                    o.hasArg() ? " -r" : "",
                    o.getDescription() != null ? " --description " + quoteIfNecessary( o.getDescription() ) : "" );
        }
    }

    @Override
    public void generateSubcommandCompletion( String subcommand, Options subcommandOptions, String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) {
        for ( Option o : subcommandOptions.getOptions() ) {
            // -f prevents files from being suggested as subcommand
            writer.printf( "complete -c gemma-cli -n %s -f -a %s%s%s%n",
                    // prevents subcommands to be suggested after a subcommand
                    quoteIfNecessary( "not __fish_seen_subcommand_from " + allSubcommands ),
                    quoteIfNecessary( subcommand ),
                    // prevents files from being suggested if the command does not allow positional arguments
                    allowsPositionalArguments ? "" : " -f",
                    StringUtils.isNotBlank( subcommandDescription ) ? " --description " + quoteIfNecessary( subcommandDescription ) : "" );
            writer.printf( "complete -c gemma-cli -n %s%s%s%s%s%n",
                    quoteIfNecessary( "__fish_seen_subcommand_from " + subcommand ),
                    ( o.getOpt().length() == 1 ? " -s " : " -o " ) + o.getOpt(),
                    o.getLongOpt() != null ? " -l " + o.getLongOpt() : "",
                    o.hasArg() ? " -r" : "",
                    StringUtils.isNotBlank( o.getDescription() ) ? " --description " + quoteIfNecessary( o.getDescription() ) : "" );
        }
    }

    private String quoteIfNecessary( String s ) {
        return "'" + s.replaceAll( "'", "\\\\'" ).replaceAll( "\n", "\\\\n" ) + "'";
    }
}
