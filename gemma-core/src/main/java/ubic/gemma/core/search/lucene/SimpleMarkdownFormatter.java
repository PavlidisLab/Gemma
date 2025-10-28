package ubic.gemma.core.search.lucene;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;

import static ubic.gemma.core.util.MarkdownUtils.escapeMarkdown;

/**
 * Highlight results in a simple Markdown format.
 * @author poirigui
 */
public class SimpleMarkdownFormatter implements Formatter {
    @Override
    public String highlightTerm( String originalText, TokenGroup tokenGroup ) {
        if ( tokenGroup.getTotalScore() <= 0 ) {
            return escapeMarkdown( originalText );
        }
        return "**" + escapeMarkdown( originalText ) + "**";
    }
}
