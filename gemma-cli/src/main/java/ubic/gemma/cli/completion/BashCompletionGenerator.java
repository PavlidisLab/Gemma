package ubic.gemma.cli.completion;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.util.ShellUtils.quoteIfNecessary;

public class BashCompletionGenerator extends AbstractCompletionGenerator {

    private static final String INDENT = "    ";

    private final String executableName;

    /**
     * A list of all possible subcommands.
     */
    private final Set<String> subcommands;

    /**
     * Current indentation level.
     */
    private String indent = "";

    public BashCompletionGenerator( String executableName, Set<String> subcommands ) {
        this.executableName = executableName;
        this.subcommands = new TreeSet<>( subcommands );
    }

    @Override
    public void beforeCompletion( PrintWriter writer ) {
        writer.println( "function " + mangle( "__" + executableName + "_complete" ) + "() {" );
        pushIndent();
        writer.append( indent ).println( "COMPREPLY=()" );
        writer.append( indent ).println( "words=\"${COMP_WORDS[*]}\"" );
        writer.append( indent ).println( "current_option=\"${COMP_WORDS[$COMP_CWORD-1]}\"" );

    }

    @Override
    public void generateCompletion( Options options, PrintWriter writer ) {
        writer.append( indent )
                .printf( "if ! [[ \" $words \" =~ ' '(%s)' ' ]]; then%n",
                        subcommands.stream().map( String::trim ).map( this::quoteRegex ).collect( Collectors.joining( "|" ) ) );
        pushIndent();
        generateWordsFromOptions( options, writer );
        generateWordsFromStrings( subcommands, writer );
        popIndent();
        writer.append( indent ).println( "fi" );
    }

    @Override
    public void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) {
        writer.append( indent ).printf( "if [[ \" $words \" =~ %s ]]; then%n", quoteIfNecessary( " " + subcommand.trim() + " " ) );
        pushIndent();
        generateWordsFromOptions( subcommandOptions, writer );
        popIndent();
        writer.append( indent ).println( "fi" );
    }

    private void generateWordsFromOptions( Options options, PrintWriter writer ) {
        Set<String> optionsThatRequireArg = new TreeSet<>();
        Set<String> fileOptions = new TreeSet<>();
        Set<String> strings = new TreeSet<>();
        for ( Option option : options.getOptions() ) {
            List<String> words = new ArrayList<>( 2 );
            words.add( "-" + option.getOpt() );
            if ( option.getLongOpt() != null ) {
                words.add( "--" + option.getLongOpt() );
            }
            strings.addAll( words );
            if ( isFileOption( option ) ) {
                fileOptions.addAll( words );
            }
            if ( option.hasArg() ) {
                optionsThatRequireArg.addAll( words );
            }
        }

        if ( !optionsThatRequireArg.isEmpty() ) {
            writer.append( indent ).printf( "if ! [[ \"$current_option\" =~ (%s) ]]; then%n", String.join( "|", optionsThatRequireArg ) );
            pushIndent();
            generateWordsFromStrings( strings, writer );
            popIndent();
            writer.append( indent ).println( "fi" );
        } else {
            generateWordsFromStrings( strings, writer );
        }

        // suggest files if the before-last word is an option that takes a file
        if ( !fileOptions.isEmpty() ) {
            writer.append( indent ).printf( "if [[ \"$current_option\" =~ (%s) ]]; then%n", String.join( "|", fileOptions ) );
            pushIndent();
            writer.append( indent ).println( "mapfile -t -O \"${#COMPREPLY[@]}\" COMPREPLY < <(compgen -f -- \"$2\")" );
            popIndent();
            writer.append( indent ).println( "fi" );
        }
    }

    private void generateWordsFromStrings( Collection<String> strings, PrintWriter writer ) {
        writer.append( indent ).append( "mapfile -t -O \"${#COMPREPLY[@]}\" COMPREPLY < <(compgen -W \"" );
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
        writer.println( " -- \"$2\")" );
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
        writer.println( "complete -o filenames -o bashdefault -F " + mangle( "__" + executableName + "_complete" ) + " " + quoteIfNecessary( executableName ) );
    }

    /**
     * Mangle a string to form a valid bash identifier.
     * <p>
     * Only alphanumeric characters and underscores are allowed. Invalid characters are replaced with '_'.
     */
    private String mangle( String s ) {
        return s.replaceAll( "[^a-z0-9_]", "_" );
    }

    private String quoteRegex( String s ) {
        // FIXME: properly escape regex characters in command name
        return s;
    }
}
