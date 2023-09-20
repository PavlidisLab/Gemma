package ubic.gemma.core.search.lucene;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;

public class SimpleMarkdownFormatter implements Formatter {
    @Override
    public String highlightTerm( String originalText, TokenGroup tokenGroup ) {
        if ( tokenGroup.getTotalScore() <= 0 ) {
            return originalText;
        }
        return "**" + originalText + "**";
    }
}
