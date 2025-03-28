package ubic.gemma.core.util;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static ubic.gemma.core.util.Constants.GEMMA_CITATION_NOTICE;
import static ubic.gemma.core.util.Constants.GEMMA_LICENSE_NOTICE;

/**
 * Bunch of utilities for writing data to TSV.
 * @author poirigui
 */
public class TsvUtils {

    public static final char COMMENT = '#';

    /**
     * Delimiter used when printing a list of strings in a column.
     */
    public static final char SUB_DELIMITER = '|';

    private static final DecimalFormat smallNumberFormat, midNumberFormat, largeNumberFormat;

    /**
     * <a href="https://en.wikipedia.org/wiki/ISO_8601>ISO 8601</a> date format.
     */
    private static final DateFormat dateFormat = new StdDateFormat();

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance( Locale.ENGLISH );
        symbols.setNaN( "" );
        symbols.setInfinity( "inf" );

        // show 4 significant digits
        smallNumberFormat = new DecimalFormat( "0.###E0" );
        smallNumberFormat.setDecimalFormatSymbols( symbols );
        smallNumberFormat.setRoundingMode( RoundingMode.HALF_UP );

        // show from one up to 4 decimal places
        midNumberFormat = new DecimalFormat( "0.0###" );
        midNumberFormat.setDecimalFormatSymbols( symbols );
        midNumberFormat.setRoundingMode( RoundingMode.HALF_UP );

        // only show leading digits and at least one decimal place
        largeNumberFormat = new DecimalFormat( "0.0" );
        largeNumberFormat.setDecimalFormatSymbols( symbols );
        largeNumberFormat.setRoundingMode( RoundingMode.HALF_UP );
    }

    /**
     * Preconfigure a {@link CSVFormat.Builder} with desirable defaults
     * @param what                a short description of what data is being written
     * @param buildInfo           build information to include in the header
     * @param extraHeaderComments additional header comments that will be included at the top of the TSV file, right
     *                            after the {@link Constants#GEMMA_CITATION_NOTICE} and {@link Constants#GEMMA_LICENSE_NOTICE}
     */
    public static CSVFormat.Builder getTsvFormatBuilder( String what, BuildInfo buildInfo, String... extraHeaderComments ) {
        Date timestamp = new Date();
        List<String> headerComments = new ArrayList<>( 4 + GEMMA_CITATION_NOTICE.length + extraHeaderComments.length );
        headerComments.add( what + " generated by Gemma " + buildInfo.getVersion() + " on " + format( timestamp ) );
        headerComments.add( "" );
        headerComments.addAll( Arrays.asList( GEMMA_CITATION_NOTICE ) );
        headerComments.add( "" );
        headerComments.add( GEMMA_LICENSE_NOTICE );
        if ( extraHeaderComments.length > 0 ) {
            headerComments.add( "" );
            headerComments.addAll( Arrays.asList( extraHeaderComments ) );
        }
        return CSVFormat.Builder
                .create( CSVFormat.TDF )
                .setCommentMarker( COMMENT )
                .setHeaderComments( headerComments.toArray( new String[0] ) );
    }

    public static double parseDouble( @Nullable String val ) {
        val = StringUtils.stripToNull( val );
        if ( val == null ) {
            return Double.NaN;
        } else if ( val.equals( "inf" ) ) {
            return Double.POSITIVE_INFINITY;
        } else if ( val.equals( "-inf" ) ) {
            return Double.NEGATIVE_INFINITY;
        } else {
            return Double.parseDouble( StringUtils.strip( val ) );
        }
    }

    @Nullable
    public static Long parseLong( @Nullable String val ) {
        return StringUtils.isNotBlank( val ) ? Long.parseLong( StringUtils.strip( val ) ) : null;
    }

    @Nullable
    public static Integer parseInt( @Nullable String val ) {
        return StringUtils.isNotBlank( val ) ? Integer.parseInt( StringUtils.strip( val ) ) : null;
    }

    @Nullable
    public static Boolean parseBoolean( @Nullable String val ) {
        return StringUtils.isNotBlank( val ) ? Boolean.parseBoolean( StringUtils.strip( val ) ) : null;
    }

    /**
     * Format a {@link Double} for TSV.
     * @param d a double to format
     * @return a formatted double, an empty string if d is null or NaN or inf/-inf if infinite
     */
    public static String format( @Nullable Double d ) {
        if ( d == null ) {
            return "";
        } else {
            return format( d.doubleValue() );
        }
    }

    public static String format( double d ) {
        if ( d == 0.0 ) {
            return "0.0";
        } else if ( Math.abs( d ) < 1e-4 ) {
            return smallNumberFormat.format( d );
        } else if ( Math.abs( d ) < 1e3 ) {
            return midNumberFormat.format( d );
        } else {
            return largeNumberFormat.format( d );
        }
    }

    public static String format( @Nullable Integer i ) {
        if ( i == null ) {
            return "";
        } else {
            return format( i.intValue() );
        }
    }

    public static String format( int i ) {
        return String.valueOf( i );
    }

    public static String format( @Nullable Long l ) {
        if ( l == null ) {
            return "";
        } else {
            return format( l.longValue() );
        }
    }

    public static String format( long l ) {
        return String.valueOf( l );
    }

    /**
     * Format a {@link String} for TSV.
     */
    public static String format( @Nullable String s ) {
        if ( s == null ) {
            return "";
        }
        return s.replace( "\\", "\\\\" )
                .replace( "\n", "\\n" )
                .replace( "\t", "\\t" )
                .replace( "\r", "\\r" );
    }

    public static String format( @Nullable Date d ) {
        if ( d == null ) {
            return "";
        }
        return dateFormat.format( d );
    }

    public static String format( @Nullable Object object ) {
        if ( object instanceof Double ) {
            return format( ( Double ) object );
        } else if ( object instanceof Integer ) {
            return format( ( Integer ) object );
        } else if ( object instanceof Long ) {
            return format( ( Long ) object );
        } else if ( object instanceof Date ) {
            return format( ( Date ) object );
        } else if ( object != null ) {
            return format( object.toString() );
        } else {
            return "";
        }
    }

    /**
     * Format a string as a TSV comment.
     * <p>
     * This will prepend a {@link #COMMENT} on each line that do not start with the character.
     */
    public static String formatComment( @Nullable String comment ) {
        if ( StringUtils.isBlank( comment ) ) {
            return "";
        }
        return ( comment.charAt( 0 ) != COMMENT ? COMMENT + " " : "" ) + comment.replaceAll( "\n([^" + COMMENT + "])", "\n" + COMMENT + " $1" );
    }
}
