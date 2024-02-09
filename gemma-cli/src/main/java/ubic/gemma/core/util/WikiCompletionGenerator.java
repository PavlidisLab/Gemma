package ubic.gemma.core.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * Generator that produce Wiki markup to update user documentation at a glance.
 * <p>
 * This generator specifically produces <a href="https://confluence.atlassian.com/doc/confluence-wiki-markup-251003035.html">Confluence Wiki Markup</a>.
 * @author poirigui
 */
public class WikiCompletionGenerator extends AbstractCompletionGenerator {

    private final SortedMap<GemmaCLI.CommandGroup, SortedMap<String, CLI>> commands;

    public WikiCompletionGenerator( Options generalOptions, SortedMap<GemmaCLI.CommandGroup, SortedMap<String, CLI>> commands ) {
        super( generalOptions, commands );
        this.commands = commands;
    }

    @Override
    public void generateCompletion( PrintWriter writer ) {
        for ( GemmaCLI.CommandGroup commandGroup : commands.keySet() ) {
            writer.printf( "h2. %s%n", commandGroup.name() );
            writer.println();
            for ( CLI c : commands.get( commandGroup ).values() ) {
                writer.printf( "[%s]%s%n", c.getCommandName(), StringUtils.isNotBlank( c.getShortDesc() ) ? " - " + c.getShortDesc() : "" );
            }
            writer.println();
        }
        super.generateCompletion( writer );
    }

    @Override
    protected void generateGeneralCompletion( Options options, PrintWriter writer ) {
        writer.printf( "h2. General Options%n" );
        writer.println();
        generateOptionTable( options, writer );
    }

    @Override
    protected void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) {
        writer.printf( "h2. %s%n", subcommand );
        writer.println();
        if ( StringUtils.isNotBlank( subcommandDescription ) ) {
            writer.printf( "%s%n", subcommandDescription );
            writer.println();
        }
        writer.printf( "{{Usage: gemma-cli %s %s%s}}%n", subcommand,
                inlineSummaryOfOptions( subcommandOptions ),
                allowsPositionalArguments ? " ARGS..." : "" );
        writer.println();
        generateOptionTable( subcommandOptions, writer );
    }

    private String inlineSummaryOfOptions( Options options ) {
        return options.getOptions().stream()
                .map( this::inlineSummaryOfOption )
                .collect( Collectors.joining( " " ) );
    }

    private String inlineSummaryOfOption( Option option ) {
        StringBuilder sb = new StringBuilder();
        if ( !option.isRequired() ) {
            sb.append( '[' );
        }
        sb.append( "-" ).append( option.getOpt() );
        if ( option.getLongOpt() != null ) {
            sb.append( ",--" ).append( option.getLongOpt() );
        }
        if ( option.hasArg() ) {
            sb.append( "=" );
            sb.append( option.getArgName() != null ? option.getArgName() : "ARG" );
        }
        if ( !option.isRequired() ) {
            sb.append( ']' );
        }
        return sb.toString();
    }

    private void generateOptionTable( Options options, PrintWriter writer ) {
        writer.println( "||Flags||Description||" );
        for ( Option option : options.getOptions() ) {
            writer.printf( "|{{%s%s}}|%s|%n", "-" + option.getOpt(),
                    option.getLongOpt() != null ? ",--" + option.getLongOpt() : "",
                    option.getDescription() );
        }
        writer.println();
    }
}
