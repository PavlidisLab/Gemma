package ubic.gemma.core.util;

import javax.annotation.Nullable;

/**
 * Utilities for handling Markdown text.
 * @author poirigui
 */
public class MarkdownUtils {

    /**
     * Escape special Markdown characters in a string.
     * <p>
     * This is aiming at escaping [Common Mark](https://spec.commonmark.org/) markdown.
     */
    public static String escapeMarkdown( @Nullable String text ) {
        if ( text == null ) {
            return null;
        }
        return text
                // ASCII punctuation characters
                .replaceAll( "\\p{Punct}", "\\\\$0" )
                // prevent tab expansion
                .replaceAll( "\\t", "\\\\t" )
                // prevents successive whitespaces from being collapsed
                .replaceAll( "(?<=\\s)\\s", "\\\\$0" );
    }
}
