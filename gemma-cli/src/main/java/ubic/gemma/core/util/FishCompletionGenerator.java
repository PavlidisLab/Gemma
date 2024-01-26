package ubic.gemma.core.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates fish completion script.
 * @author poirigui
 */
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
        // -f prevents files from being suggested as subcommand
        // FIXME: add -k, but the order has to be reversed
        writer.printf( "complete -c gemma-cli -n %s -f -a %s%s%n",
                // prevents subcommands to be suggested after a subcommand
                quoteIfNecessary( "not __fish_seen_subcommand_from " + allSubcommands ),
                quoteIfNecessary( subcommand ),
                StringUtils.isNotBlank( subcommandDescription ) ? " --description " + quoteIfNecessary( subcommandDescription ) : "" );
        // prevents files from being suggested if the command does not allow positional arguments
        if ( !allowsPositionalArguments ) {
            writer.printf( "complete -c gemma-cli -n %s -f%n", quoteIfNecessary( "__fish_seen_subcommand_from " + subcommand ) );
        }
        for ( Option o : subcommandOptions.getOptions() ) {
            writer.printf( "complete -c gemma-cli -n %s%s%s%s%s%s%n",
                    quoteIfNecessary( "__fish_seen_subcommand_from " + subcommand ),
                    ( o.getOpt().length() == 1 ? " -s " : " -o " ) + o.getOpt(),
                    o.getLongOpt() != null ? " -l " + o.getLongOpt() : "",
                    o.hasArg() ? " -r" : "",
                    // specifying -F is necessary if the CLI does not allow positional arguments
                    isFileOption( o ) ? " -F" : " -f",
                    StringUtils.isNotBlank( o.getDescription() ) ? " --description " + quoteIfNecessary( o.getDescription() ) : "" );
        }
    }

    private boolean isFileOption( Option o ) {
        return File.class.equals( o.getType() )
                || o.getOpt().toLowerCase().contains( "file" )
                || ( o.getLongOpt() != null && o.getLongOpt().toLowerCase().contains( "file" ) );
    }

    private String quoteIfNecessary( String s ) {
        return "'" + s.replaceAll( "'", "\\\\'" ).replaceAll( "\n", "\\\\n" ) + "'";
    }
}
