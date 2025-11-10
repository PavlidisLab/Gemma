package ubic.gemma.cli.completion;

import lombok.Setter;
import lombok.Value;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.cli.util.OptionsUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static ubic.gemma.cli.util.OptionsUtils.isFileOption;

/**
 * Generate Wiki pages according to <a href="https://confluence.atlassian.com/doc/confluence-storage-format-790796544.html">Confluence Storage Format</a>
 *
 * @author poirigui
 */
public class ConfluenceWikiHtmlGenerator {

    private final Path outputDirectory;

    /**
     * Suffix to append to each page name.
     */
    @Setter
    private String executableName = "gemma-cli";

    /**
     * Suffix to append to each page name.
     */
    @Setter
    private String pageSuffix = "";

    public ConfluenceWikiHtmlGenerator( Path outputDirectory ) {
        this.outputDirectory = outputDirectory;
    }

    public void generateGeneralPage( SortedMap<CLI.CommandGroup, SortedMap<String, CommandMeta>> commands, Options options ) {
        try ( PrintWriter pw = new PrintWriter( Files.newOutputStream( resolveWikiPage( "List of Gemma CLI Tools" + pageSuffix ) ) ) ) {
            pw.printf( "<h2>General Options</h2>" );
            pw.printf( "<ac:structured-macro ac:name=\"code\" ac:schema-version=\"1\" ac:macro-id=\"%s\">",
                    UUID.nameUUIDFromBytes( ( "Usage of " + executableName ).getBytes() ) );
            pw.print( "<ac:parameter ac:name=\"title\">Usage</ac:parameter>" );
            pw.print( "<ac:parameter ac:name=\"language\">bash</ac:parameter>" );
            pw.printf( "<ac:plain-text-body><![CDATA[%s %s]]></ac:plain-text-body>",
                    escapeCDATA( executableName ),
                    escapeCDATA( StringUtils.strip( inlineSummaryOfRequiredOptions( options ) + " [OPTIONS...] <COMMAND> [COMMAND OPTIONS...]" ) ) );
            pw.printf( "</ac:structured-macro>" );
            generateOptionTable( options, pw );
            generateListOfAvailableCommands( commands, pw );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void generateSubcommandPage( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments ) {
        if ( subcommand.startsWith( "ubic.gemma.apps" ) ) {
            return;
        }
        try ( PrintWriter pw = new PrintWriter( Files.newBufferedWriter( resolveWikiPage( "List of Gemma CLI Tools" + pageSuffix + "/" + subcommand + pageSuffix ) ) ) ) {
            if ( StringUtils.isNotBlank( subcommandDescription ) ) {
                pw.printf( "<p>%s</p>", escapeHtml4( subcommandDescription ) );
            }
            pw.printf( "<ac:structured-macro ac:name=\"code\" ac:schema-version=\"1\" ac:macro-id=\"%s\">",
                    UUID.nameUUIDFromBytes( ( "Usage of " + executableName + " " + subcommand ).getBytes() ) );
            pw.print( "<ac:parameter ac:name=\"title\">Usage</ac:parameter>" );
            pw.print( "<ac:parameter ac:name=\"language\">bash</ac:parameter>" );
            pw.printf( "<ac:plain-text-body><![CDATA[%s %s %s%s]]></ac:plain-text-body>",
                    escapeCDATA( executableName ),
                    escapeCDATA( subcommand ),
                    escapeCDATA( StringUtils.strip( inlineSummaryOfRequiredOptions( subcommandOptions ) + " [COMMAND OPTIONS...]" ) ),
                    allowsPositionalArguments ? " [ARGS...]" : "" );
            pw.printf( "</ac:structured-macro>" );
            generateOptionTable( subcommandOptions, pw );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private void generateListOfAvailableCommands( SortedMap<CLI.CommandGroup, SortedMap<String, CommandMeta>> commands, PrintWriter writer ) {
        writer.print( "<h2>Commands available</h2>" );
        for ( CLI.CommandGroup commandGroup : commands.keySet() ) {
            writer.printf( "<h3>%s</h3>", escapeHtml4( commandGroup.name() ) );
            for ( CommandMeta c : commands.get( commandGroup ).values() ) {
                if ( StringUtils.isNotBlank( c.getCommandName() ) ) {
                    writer.printf( "<p><ac:link><ri:page ri:content-title=\"%s\"><ac:link-body>%s</ac:link-body></ri:page></ac:link>%s</p>",
                            escapeHtml4( c.getCommandName() + pageSuffix ),
                            escapeHtml4( c.getCommandName() ),
                            escapeHtml4( StringUtils.isNotBlank( c.getShortDesc() ) ? " - " + c.getShortDesc() : "" ) );
                }
            }
        }
    }

    private String inlineSummaryOfRequiredOptions( Options options ) {
        return options.getOptions().stream()
                .filter( Option::isRequired )
                .map( this::formatOption )
                .collect( Collectors.joining( " " ) );
    }

    private void generateOptionTable( Options options, PrintWriter writer ) {
        writer.print( "<table>" );
        writer.print( "<thead>" );
        writer.print( "<tr>" );
        writer.print( "<th>Option</th>" );
        writer.print( "<th>Description</th>" );
        writer.print( "</tr>" );
        writer.print( "</thead>" );
        writer.print( "<tbody>" );
        for ( Option option : options.getOptions() ) {
            writer.print( "<tr>" );
            writer.printf( "<td><code>%s</code></td>", escapeHtml4( formatOption( option ) ) );
            writer.printf( "<td>%s</td>", option.getDescription() != null ? escapeHtml4( option.getDescription() ) : "" );
            writer.print( "</tr>" );
        }
        writer.print( "</tbody>" );
        writer.print( "</table>" );
    }

    private String formatOption( Option option ) {
        StringBuilder sb = new StringBuilder();
        sb.append( OptionsUtils.formatOption( option ) );
        if ( option.hasArg() ) {
            sb.append( "=" );
            sb.append( option.getArgName() != null ? option.getArgName() : isFileOption( option ) ? "FILE" : "ARG" );
        }
        return sb.toString();
    }

    private Path resolveWikiPage( String pageName ) throws IOException {
        Path p = outputDirectory.resolve( pageName );
        Files.createDirectories( p );
        return outputDirectory.resolve( pageName ).resolve( p.getFileName() + ".txt" );
    }

    private String escapeCDATA( String cdata ) {
        // TODO:
        return cdata;
    }

    @Value
    public static
    class CommandMeta {
        @Nullable
        String commandName;
        @Nullable
        String shortDesc;
    }
}
