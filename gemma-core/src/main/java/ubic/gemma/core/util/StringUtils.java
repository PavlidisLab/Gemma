package ubic.gemma.core.util;

import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Various utilities for manipulating strings.
 * <p>
 * This is mean to extend missing functionality in {@link org.apache.commons.lang3.StringUtils}.
 * @author poirigui
 */
public class StringUtils {

    /**
     * Abbreviate the value of a field stored with the given charset if it exceeds a certain length in bytes.
     * @see org.apache.commons.lang3.StringUtils#abbreviate(String, int)
     */
    @Nullable
    public static String abbreviateInBytes( @Nullable String value, String abbrevMarker, int maxLengthInBytes, Charset charset ) {
        return abbreviateInBytes( value, abbrevMarker, maxLengthInBytes, false, charset );
    }

    /**
     * Abbreviate the value of a field stored with the given charset if it exceeds a certain length in bytes.
     * @see org.apache.commons.lang3.StringUtils#abbreviate(String, int)
     * @param stripBeforeAddingMarker if true, the string will be stripped before adding the marker
     */
    @Nullable
    public static String abbreviateInBytes( @Nullable String value, String abbrevMarker, int maxLengthInBytes, boolean stripBeforeAddingMarker, Charset charset ) {
        int zl = sizeInBytes( abbrevMarker, charset );
        if ( maxLengthInBytes < zl + 1 ) {
            throw new IllegalArgumentException( "The maximum length must be at least one byte more than the length of the marker in bytes." );
        }
        if ( value == null ) {
            return null;
        }
        // worst case scenario: all characters are 4 bytes long
        if ( largestSizeInBytes( value, charset ) <= maxLengthInBytes ) {
            return value;
        }
        int byteLength = sizeInBytes( value, charset );
        if ( byteLength > maxLengthInBytes ) {
            value = truncateInBytes( value, maxLengthInBytes - zl, charset );
            if ( stripBeforeAddingMarker ) {
                value = org.apache.commons.lang3.StringUtils.strip( value );
            }
            return value + abbrevMarker;
        }
        return value;
    }

    /**
     * Abbreviate a string with a suffix as per {@link #abbreviateInBytes(String, String, int, boolean, Charset)}.
     * <p>
     * This produce strings of the form: {@code some text{abbrevMarker}suffix} such that the length of the string in
     * bytes is at most the given maximum.
     */
    public static String abbreviateWithSuffix( @Nullable String value, String suffix, String abbrevMarker, int maxLengthInBytes, boolean stripBeforeAddingMarker, Charset charset ) {
        return abbreviateInBytes( value, abbrevMarker, maxLengthInBytes - sizeInBytes( suffix, charset ), stripBeforeAddingMarker, charset ) + suffix;
    }

    /**
     * @see org.apache.commons.lang3.StringUtils#truncate(String, int)
     */
    public static String truncateInBytes( String s, int maxBytes, Charset charset ) {
        if ( charset.equals( StandardCharsets.UTF_8 ) ) {
            return truncateWhenUTF8( s, maxBytes );
        } else if ( charset.equals( StandardCharsets.US_ASCII ) || charset.equals( StandardCharsets.ISO_8859_1 ) ) {
            return org.apache.commons.lang3.StringUtils.truncate( s, maxBytes );
        } else if ( charset.equals( StandardCharsets.UTF_16 ) || charset.equals( StandardCharsets.UTF_16BE ) || charset.equals( StandardCharsets.UTF_16LE ) ) {
            // all these encodings use 2 bytes per character
            return org.apache.commons.lang3.StringUtils.truncate( s, maxBytes / 2 );
        }
        throw new UnsupportedOperationException( "Unsupported charset for truncation: " + charset );
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

    private static int sizeInBytes( String s, Charset charset ) {
        if ( charset.equals( StandardCharsets.UTF_8 ) ) {
            return s.getBytes( charset ).length;
        } else if ( charset.equals( StandardCharsets.US_ASCII ) || charset.equals( StandardCharsets.ISO_8859_1 ) ) {
            return s.length();
        } else if ( charset.equals( StandardCharsets.UTF_16 ) || charset.equals( StandardCharsets.UTF_16BE ) || charset.equals( StandardCharsets.UTF_16LE ) ) {
            return 2 * s.length();
        } else {
            throw new UnsupportedOperationException( "Unsupported charset for size calculation: " + charset );
        }
    }

    private static int largestSizeInBytes( String s, Charset charset ) {
        if ( charset.equals( StandardCharsets.UTF_8 ) ) {
            return 4 * s.length();
        } else if ( charset.equals( StandardCharsets.US_ASCII ) || charset.equals( StandardCharsets.ISO_8859_1 ) ) {
            return s.length();
        } else if ( charset.equals( StandardCharsets.UTF_16 ) || charset.equals( StandardCharsets.UTF_16BE ) || charset.equals( StandardCharsets.UTF_16LE ) ) {
            return 2 * s.length();
        } else {
            throw new UnsupportedOperationException( "Unsupported charset for maximum size calculation: " + charset );
        }
    }

    public static String appendWithDelimiter( @Nullable String s, String suffix ) {
        return appendWithDelimiter( s, suffix, " " );
    }

    /**
     * Append a suffix to a string, with a delimiter if the string is not empty.
     * <p>
     * If the string already ends with the delimiter, the suffix is appended directly.
     */
    public static String appendWithDelimiter( @Nullable String s, String suffix, String delimiter ) {
        Assert.notNull( suffix );
        if ( s == null || s.isEmpty() ) {
            return suffix;
        } else if ( s.endsWith( delimiter ) ) {
            return s + suffix;
        } else {
            return s + delimiter + suffix;
        }
    }
}
