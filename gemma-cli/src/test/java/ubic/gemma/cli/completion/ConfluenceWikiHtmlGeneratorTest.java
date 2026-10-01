package ubic.gemma.cli.completion;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfluenceWikiHtmlGeneratorTest {

    /**
     * Confluence stores {@code &mdash;} as the character itself, so a page carrying a named entity reads back shorter
     * than it was uploaded and rclone fails the deploy with "corrupted on transfer: sizes differ".
     */
    @Test
    void testNonAsciiIsWrittenLiterallyAndMarkupIsEscaped( @TempDir Path dir ) throws Exception {
        ConfluenceWikiHtmlGenerator generator = new ConfluenceWikiHtmlGenerator( dir );
        Options options = new Options();
        options.addOption( Option.builder( "markOnly" ).desc( "already aligned — a <b> & \"c\"" ).build() );
        generator.generateSubcommandPage( "blatPlatform", options, "desc — here", false );
        String page = Files.readString( dir.resolve( "List of Gemma CLI Tools/blatPlatform/blatPlatform.txt" ), StandardCharsets.UTF_8 );
        assertThat( page )
                .contains( "already aligned — a &lt;b&gt; &amp; &quot;c&quot;" )
                .contains( "<p>desc — here</p>" )
                .doesNotContain( "&mdash;" );
    }
}
