package ubic.gemma.cli.completion;

import org.apache.commons.cli.Options;
import ubic.gemma.cli.util.CLI;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.SortedMap;

/**
 * Wraps {@link ConfluenceWikiHtmlGenerator} so that we can use it as a {@link CompletionGenerator}.
 *
 * @author poirigui
 */
public class WikiCompletionGenerator implements CompletionGenerator {

    private final SortedMap<CLI.CommandGroup, SortedMap<String, ConfluenceWikiHtmlGenerator.CommandMeta>> commands;

    public WikiCompletionGenerator( String executableName, SortedMap<CLI.CommandGroup, SortedMap<String, ConfluenceWikiHtmlGenerator.CommandMeta>> commands, String pageSuffix, Path outputDir ) {
        this.commands = commands;
        ConfluenceWikiHtmlGenerator cg = new ConfluenceWikiHtmlGenerator( outputDir );
        cg.setExecutableName( executableName );
        cg.setPageSuffix( pageSuffix );
        this.wikiGenerator = cg;
    }

    public final ConfluenceWikiHtmlGenerator wikiGenerator;

    @Override
    public void beforeCompletion( PrintWriter writer ) {

    }

    @Override
    public void generateCompletion( Options options, PrintWriter writer ) {
        wikiGenerator.generateGeneralPage( commands, options );
    }

    @Override
    public void generateSubcommandCompletion( String subcommand, Options subcommandOptions, @Nullable String subcommandDescription, boolean allowsPositionalArguments, PrintWriter writer ) {
        if ( subcommand.startsWith( "ubic.gemma.apps" ) ) {
            return; // skip pages for aliases
        }
        wikiGenerator.generateSubcommandPage( subcommand, subcommandOptions, subcommandDescription, allowsPositionalArguments );
    }

    @Override
    public void afterCompletion( PrintWriter writer ) {
    }
}
