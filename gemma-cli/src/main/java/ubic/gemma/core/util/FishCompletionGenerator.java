package ubic.gemma.core.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * Generates fish completion script.
 * @author poirigui
 */
public class FishCompletionGenerator extends AbstractCompletionGenerator {

    private final String allSubcommands;

    public FishCompletionGenerator( Options generalOptions, SortedMap<GemmaCLI.CommandGroup, SortedMap<String, CLI>> commands ) {
        super( generalOptions, commands );
        this.allSubcommands = commands.values().stream().map( Map::keySet ).flatMap( Collection::stream ).filter( StringUtils::isNotBlank ).collect( Collectors.joining( " " ) );
    }

    @Override
    protected void generateGeneralCompletion( Options options, PrintWriter writer ) {
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
    protected void generateCommandGroupSection( GemmaCLI.CommandGroup commandGroup, PrintWriter writer ) {
        // TODO: check if fish supports grouping commands
    }

    @Override
    protected void generateSubcommandCompletion( String subcommand, Options subcommandOptions, String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) {
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
                    // TODO: add -F here, but this is not supported on our server
                    isFileOption( o ) ? "" : " -f",
                    StringUtils.isNotBlank( o.getDescription() ) ? " --description " + quoteIfNecessary( o.getDescription() ) : "" );
        }
    }

    private boolean isFileOption( Option o ) {
        return File.class.equals( o.getType() )
                || o.getOpt().toLowerCase().contains( "file" )
                || ( o.getLongOpt() != null && o.getLongOpt().toLowerCase().contains( "file" ) );
    }

    private String quoteIfNecessary( String s ) {
        return "'" + s.replaceAll( "'", "'\"'\"'" ).replaceAll( "\n", "\\\\n" ) + "'";
    }
}
