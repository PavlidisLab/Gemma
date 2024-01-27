package ubic.gemma.core.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BashCompletionGenerator implements CompletionGenerator {

    private static final String INDENT = "    ";
    private final Set<String> subcommands;

    private String indent = "";

    public BashCompletionGenerator( Set<String> subcommands ) {
        this.subcommands = subcommands;
    }

    @Override
    public void beforeCompletion( PrintWriter writer ) {
        writer.println( "function __gemma_cli_complete() {" );
        pushIndent();
        writer.append( indent ).println( "COMPREPLY=()" );
    }

    @Override
    public void generateCompletion( Options options, PrintWriter writer ) {
        writer.append( indent )
                .printf( "if ! [[ \" ${COMP_WORDS[*]} \" =~ ' ('%s') ' ]]; then%n",
                        quoteRegex( subcommands.stream().map( String::trim ).collect( Collectors.joining( "|" ) ) ) );
        pushIndent();
        generateWordsFromOptions( options, writer );
        generateWordsFromStrings( subcommands, writer );
        popIndent();
        writer.append( indent ).println( "fi" );
    }

    @Override
    public void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) {
        writer.append( indent ).printf( "if [[ \" ${COMP_WORDS[*]} \" =~ %s ]]; then%n", quoteIfNecessary( " " + subcommand.trim() + " " ) );
        pushIndent();
        generateWordsFromOptions( subcommandOptions, writer );
        popIndent();
        writer.append( indent ).println( "fi" );
    }

    private void generateWordsFromOptions( Options subcommandOptions, PrintWriter writer ) {
        Set<String> strings = new HashSet<>();
        for ( Option option : subcommandOptions.getOptions() ) {
            strings.add( "-" + option.getOpt() );
            if ( option.getLongOpt() != null ) {
                strings.add( "--" + option.getLongOpt() );
            }
        }
        generateWordsFromStrings( strings, writer );
    }

    private void generateWordsFromStrings( Collection<String> strings, PrintWriter writer ) {
        writer.append( indent ).append( "COMPREPLY+=( $(compgen -W \"" );
        boolean first = true;
        for ( String string : strings ) {
            if ( first ) {
                first = false;
            } else {
                writer.append( " " );
            }
            writer.append( quoteIfNecessary( string ) );
        }
        writer.append( '"' );
        writer.println( " -- \"$2\" ) )" );
    }

    private void pushIndent() {
        indent += INDENT;
    }

    private void popIndent() {
        indent = indent.substring( 0, indent.length() - INDENT.length() );
    }

    @Override
    public void afterCompletion( PrintWriter writer ) {
        popIndent();
        writer.println( "}" );
        writer.println( "complete -F __gemma_cli_complete gemma-cli" );
    }

    private String quoteRegex( String s ) {
        return Pattern.quote( s );
    }

    private String quoteIfNecessary( String s ) {
        return "'" + s.replaceAll( "'", "\\\\'" ).replaceAll( "\n", "\\\\n" ) + "'";
    }
}
