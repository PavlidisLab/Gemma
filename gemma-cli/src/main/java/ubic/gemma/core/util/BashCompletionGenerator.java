package ubic.gemma.core.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BashCompletionGenerator implements CompletionGenerator {

    private static final String INDENT = "    ";
    private final Set<String> subcommands;

    private String indent;

    public BashCompletionGenerator( Set<String> subcommands ) {
        this.subcommands = subcommands;
    }

    @Override
    public void beforeCompletion( PrintWriter writer ) {
        writer.printf( "function __gemma_cli_complete() {%n" );
        indent = INDENT;
        writer.append( indent ).printf( "COMPREPLY=()%n" );
    }

    @Override
    public void generateCompletion( Options options, PrintWriter writer ) {
        writer.append( indent ).printf( "if ! [[ \" ${COMP_WORDS[*]} \" =~ %s ]]; then%n", " (" + quoteRegex( subcommands.stream().map( String::trim ).collect( Collectors.joining( "|" ) ) ) + ")" );
        indent += INDENT;
        writer.append( indent ).printf( "COMPREPLY+=(" );
        generateWordsFromOptions( options, writer );
        writer.printf( ")%n" );
        writer.append( indent ).printf( "COMPREPLY+=(%s)%n", subcommands.stream().map( this::quoteIfNecessary ).collect( Collectors.joining( " " ) ) );
        indent = indent.substring( 0, indent.length() - INDENT.length() );
        writer.append( indent ).printf( "fi%n" );
    }

    @Override
    public void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) {
        writer.append( indent ).printf( "if [[ \" ${COMP_WORDS[*]} \" =~ %s ]]; then%n", quoteIfNecessary( " " + subcommand.trim() + " " ) );
        indent += INDENT;
        writer.append( indent ).printf( "COMPREPLY+=(" );
        generateWordsFromOptions( subcommandOptions, writer );
        writer.printf( ")%n" );
        indent = indent.substring( 0, indent.length() - INDENT.length() );
        writer.append( indent ).printf( "fi%n" );
    }

    private void generateWordsFromOptions( Options subcommandOptions, PrintWriter writer ) {
        boolean first = true;
        for ( Option option : subcommandOptions.getOptions() ) {
            if ( first ) {
                first = false;
            } else {
                writer.append( " " );
            }
            writer.printf( quoteIfNecessary( "-" + option.getOpt() ) );
            if ( option.getLongOpt() != null ) {
                writer.append( " " );
                writer.printf( quoteIfNecessary( "--" + option.getLongOpt() ) );
            }
        }
    }

    @Override
    public void afterCompletion( PrintWriter writer ) {
        indent = "";
        writer.printf( "}%n" );
        writer.printf( "complete -F __gemma_cli_complete gemma-cli%n" );
    }

    private String quoteRegex( String s ) {
        return Pattern.quote( s );
    }

    private String quoteIfNecessary( String s ) {
        return "'" + s.replaceAll( "'", "\\\\'" ).replaceAll( "\n", "\\\\n" ) + "'";
    }
}
