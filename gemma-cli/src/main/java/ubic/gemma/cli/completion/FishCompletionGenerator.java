package ubic.gemma.cli.completion;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.util.Assert;
import ubic.gemma.cli.main.GemmaCLI;
import ubic.gemma.cli.util.EnumeratedByCommandConverter;
import ubic.gemma.cli.util.EnumeratedConverter;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static ubic.gemma.core.util.ShellUtils.quoteIfNecessary;

/**
 * Generates fish completion script.
 * @author poirigui
 */
@CommonsLog
public class FishCompletionGenerator extends AbstractCompletionGenerator {

    private final String executableName;
    private final String allSubcommands;
    private final MessageSource messageSource;
    private final Locale locale;

    public FishCompletionGenerator( String executableName, Set<String> allSubcommands, MessageSource messageSource, Locale locale ) {
        Assert.isTrue( StringUtils.isNotBlank( executableName ), "Executable name cannot be blank." );
        this.executableName = executableName;
        this.allSubcommands = allSubcommands.stream().filter( StringUtils::isNotBlank ).sorted().collect( Collectors.joining( " " ) );
        this.messageSource = messageSource;
        this.locale = locale;
    }

    @Override
    public void beforeCompletion( PrintWriter writer ) {
        writer.printf( "set gemma_all_subcommands %s%n", quoteIfNecessary( allSubcommands ) );
        // erase all existing completions
        writer.printf( "complete -e %s%n", quoteIfNecessary( executableName ) );
    }

    @Override
    public void generateCompletion( Options options, PrintWriter writer ) {
        for ( Option o : options.getOptions() ) {
            writer.printf( "complete -c %s -n %s%s%s%s%s%s%s%n",
                    quoteIfNecessary( executableName ),
                    // prevents global options from being suggested after a subcommand
                    "\"not __fish_seen_subcommand_from $gemma_all_subcommands\"",
                    ( o.getOpt().length() == 1 ? " -s " : " -o " ) + o.getOpt(),
                    o.getLongOpt() != null ? " -l " + o.getLongOpt() : "",
                    o.hasArg() ? " -r" : "",
                    getPossibleValues( o ),
                    isFileOption( o ) ? " -F" : " -f",
                    o.getDescription() != null ? " --description " + quoteIfNecessary( o.getDescription() ) : "" );
        }
    }

    @Override
    public void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) {
        // -f prevents files from being suggested as subcommand
        // FIXME: add -k, but the order has to be reversed
        writer.printf( "complete -c %s -n %s -f -a %s%s%n",
                executableName,
                // prevents subcommands to be suggested after a subcommand
                "\"not __fish_seen_subcommand_from $gemma_all_subcommands\"",
                quoteIfNecessary( subcommand ),
                StringUtils.isNotBlank( subcommandDescription ) ? " --description " + quoteIfNecessary( subcommandDescription ) : "" );
        // prevents files from being suggested if the command does not allow positional arguments
        if ( !allowsPositionalArguments ) {
            writer.printf( "complete -c %s -n %s -f%n", executableName, quoteIfNecessary( "__fish_seen_subcommand_from " + subcommand ) );
        }
        for ( Option o : subcommandOptions.getOptions() ) {
            writer.printf( "complete -c %s -n %s%s%s%s%s%s%s%n",
                    executableName,
                    quoteIfNecessary( "__fish_seen_subcommand_from " + subcommand ),
                    ( o.getOpt().length() == 1 ? " -s " : " -o " ) + o.getOpt(),
                    o.getLongOpt() != null ? " -l " + o.getLongOpt() : "",
                    o.hasArg() ? " -r" : "",
                    getPossibleValues( o ),
                    isFileOption( o ) ? " -F" : " -f",
                    StringUtils.isNotBlank( o.getDescription() ) ? " --description " + quoteIfNecessary( o.getDescription() ) : "" );
        }
    }

    @Override
    public void afterCompletion( PrintWriter writer ) {
        writer.printf( "set -e gemma_all_subcommands%n" );
    }

    private String getPossibleValues( Option o ) {
        if ( o.getConverter() instanceof EnumeratedByCommandConverter ) {
            String[] cli = ( ( EnumeratedByCommandConverter<?, ?> ) o.getConverter() ).getPossibleValuesCommand();
            if ( GemmaCLI.GEMMA_CLI_EXE.equals( cli[0] ) ) {
                cli = Arrays.copyOf( cli, cli.length );
                cli[0] = executableName;
            }
            return " -a " + quoteIfNecessary( "(" + String.join( " ", cli ) + " 2>/dev/null)" );
        } else if ( o.getConverter() instanceof EnumeratedConverter ) {
            String possibleValues = ( ( EnumeratedConverter<?, ?> ) o.getConverter() )
                    .getPossibleValues()
                    .entrySet()
                    .stream()
                    // TODO: properly escape the description
                    .map( v -> v.getKey() + "\t" + resolveDescription( v.getKey(), v.getValue() ) )
                    .collect( Collectors.joining( "\n" ) );
            return " -a " + quoteIfNecessary( "(echo -e \"" + possibleValues + "\" 2>/dev/null)" );
        } else {
            return "";
        }
    }

    private String resolveDescription( String k, MessageSourceResolvable v ) {
        try {
            return messageSource.getMessage( v, locale );
        } catch ( NoSuchMessageException e ) {
            log.warn( "Could not resolve a description for " + k, e );
            return "";
        }
    }
}
