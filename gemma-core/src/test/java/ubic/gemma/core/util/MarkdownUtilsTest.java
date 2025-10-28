package ubic.gemma.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.util.MarkdownUtils.escapeMarkdown;
import static ubic.gemma.core.util.MarkdownUtils.formatMarkdownCodeSpan;

public class MarkdownUtilsTest {

    @Test
    public void testEscapeMarkdown() {
        assertEquals( "\\# Heading", escapeMarkdown( "# Heading" ) );
        assertEquals( "\\*bold\\*", escapeMarkdown( "*bold*" ) );
        assertEquals( "\\*bold\\*", escapeMarkdown( "*bold*" ) );
        assertEquals( " ", escapeMarkdown( " " ) );
        assertEquals( " \\ ", escapeMarkdown( "  " ) );
        assertEquals( " \\ \\ \\ ", escapeMarkdown( "    " ) );
        assertEquals( "I love \\ \\ Markdown", escapeMarkdown( "I love   Markdown" ) );
        assertEquals( "\\\\", escapeMarkdown( "\\" ) );
        assertEquals( "\\tThis is an indented block", escapeMarkdown( "\tThis is an indented block" ) );
        assertEquals( "\\<span\\>test\\<\\/span\\>", escapeMarkdown( "<span>test</span>" ) );
    }

    @Test
    public void testFormatMarkdownCodeSpan() {
        assertEquals( "`test`", formatMarkdownCodeSpan( "test" ) );
        assertEquals( "``te`st``", formatMarkdownCodeSpan( "te`st" ) );
        assertEquals( "```te``st```", formatMarkdownCodeSpan( "te``st" ) );
        assertEquals( "``te` `st``", formatMarkdownCodeSpan( "te` `st" ) );
    }
}