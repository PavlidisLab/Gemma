package ubic.gemma.core.search.lucene;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * A safer substitute for {@link org.apache.lucene.search.highlight.SimpleHTMLFormatter} that escape existing HTML tags
 * and use lowercase {@code <b>} tags.
 * @author poirigui
 */
public class SimpleHTMLFormatter implements Formatter {
    @Override
    public String highlightTerm( String originalText, TokenGroup tokenGroup ) {
        if ( tokenGroup.getTotalScore() <= 0 ) {
            return escapeHtml4( originalText );
        }
        return "<b>" + escapeHtml4( originalText ) + "</b>";
    }
}
