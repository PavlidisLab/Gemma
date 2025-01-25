package ubic.gemma.core.util;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * Various utilities for manipulating model classes.
 */
public class StringUtils {

    /**
     * Abbreviate the value of a field stored with UTF-8 if it exceeds a certain length in bytes.
     * @see org.apache.commons.lang3.StringUtils#abbreviate(String, int)
     */
    @Nullable
    public static String abbreviateInUTF8Bytes( @Nullable String value, String abbrevMarker, int maxLengthInBytes ) {
        int zl = abbrevMarker.getBytes( StandardCharsets.UTF_8 ).length;
        if ( maxLengthInBytes < zl + 1 ) {
            throw new IllegalArgumentException( "The maximum length must be at least one byte more than the length of the marker in bytes." );
        }
        if ( value == null ) {
            return value;
        }
        // worst case scenario: all characters are 4 bytes long
        if ( value.length() * 4 <= maxLengthInBytes ) {
            return value;
        }
        int byteLength = value.getBytes( StandardCharsets.UTF_8 ).length;
        if ( byteLength > maxLengthInBytes ) {
            value = truncateWhenUTF8( value, maxLengthInBytes - zl );
            return value + abbrevMarker;
        }
        return value;
    }

    /**
     * Abbreviate a string with a suffix as per {@link #abbreviateInUTF8Bytes(String, String, int)}.
     * <p>
     * This produce strings of the form: {@code some text{abbrevMarker}suffix} such that the length of the string in
     * bytes is at most the given maximum.
     */
    public static String abbreviateWithSuffixInUTF8Bytes( @Nullable String value, String suffix, String abbrevMarker, int maxLengthInBytes ) {
        return abbreviateInUTF8Bytes( value, abbrevMarker, maxLengthInBytes - suffix.getBytes( StandardCharsets.UTF_8 ).length ) + suffix;
    }

    /**
     * Borrowed from <a href="https://stackoverflow.com/questions/119328/how-do-i-truncate-a-java-string-to-fit-in-a-given-number-of-bytes-once-utf-8-en">How do I truncate a java string to fit in a given number of bytes, once UTF-8 encoded? on Stackoverflow</a>.
     */
    private static String truncateWhenUTF8( String s, int maxBytes ) {
        int b = 0;
        for ( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt( i );

            // ranges from http://en.wikipedia.org/wiki/UTF-8
            int skip = 0;
            int more;
            if ( c <= 0x007f ) {
                more = 1;
            } else if ( c <= 0x07FF ) {
                more = 2;
            } else if ( c <= 0xd7ff ) {
                more = 3;
            } else if ( c <= 0xDFFF ) {
                // surrogate area, consume next char as well
                more = 4;
                skip = 1;
            } else {
                more = 3;
            }

            if ( b + more > maxBytes ) {
                return s.substring( 0, i );
            }
            b += more;
            i += skip;
        }
        return s;
    }
}
