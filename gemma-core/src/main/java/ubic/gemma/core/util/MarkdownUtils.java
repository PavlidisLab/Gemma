package ubic.gemma.core.util;

import org.apache.commons.lang3.StringUtils;

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

    /**
     * Format a string as a Markdown code span.
     * <p>
     * This takes care of escaping backticks by surrounding the text with double backticks (or more).
     */
    public static String formatMarkdownCodeSpan( @Nullable String text ) {
        if ( text == null ) {
            return null;
        }
        int maxNumberOfConsecutiveBackticks = 0;
        int numberOfConsecutiveBackticks = 0;
        for ( char c : text.toCharArray() ) {
            if ( c == '`' ) {
                numberOfConsecutiveBackticks++;
            } else {
                maxNumberOfConsecutiveBackticks = Math.max( maxNumberOfConsecutiveBackticks, numberOfConsecutiveBackticks );
                numberOfConsecutiveBackticks = 0;
            }
        }
        String bt = StringUtils.repeat( '`', maxNumberOfConsecutiveBackticks + 1 );
        return bt + text + bt;
    }
}
