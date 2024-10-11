package ubic.gemma.core.datastructure.matrix.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Bunch of utilities for writing matrices to TSV.
 */
public class TsvUtils {

    public static final String[] GEMMA_CITATION_NOTICE = new String[] {
            "If you use this file for your research, please cite:",
            "Lim et al. (2021) Curation of over 10 000 transcriptomic studies to enable data reuse.",
            "Database, baab006 (doi:10.1093/database/baab006)." };

    /**
     * Delimiter used when printing a list of strings in a column.
     */
    public static final char SUB_DELIMITER = '|';

    private static final DecimalFormat smallNumberFormat, midNumberFormat, largeNumberFormat;

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
     * Preconfigure a {@link CSVFormat.Builder} with desirable defaults.
     * @param extraHeaderComments additional header comments that will be included at the top of the TSV file, right
     *                            after the {@link #GEMMA_CITATION_NOTICE}
     */
    public static CSVFormat.Builder getTsvFormatBuilder( String... extraHeaderComments ) {
        return CSVFormat.Builder.create( CSVFormat.TDF )
                .setCommentMarker( '#' )
                .setHeaderComments( ArrayUtils.addAll( GEMMA_CITATION_NOTICE, extraHeaderComments ) );
    }

    /**
     * Format a {@link Double} for TSV.
     * @param d a double to format
     * @return a formatted double, an empty string if d is null or NaN or inf/-inf if infinite
     */
    public static String format( @Nullable Double d ) {
        if ( d == null ) {
            return "";
        } else if ( d == 0.0 ) {
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
            return String.valueOf( i.intValue() );
        }
    }

    public static String format( @Nullable Long l ) {
        if ( l == null ) {
            return "";
        } else {
            return String.valueOf( l.longValue() );
        }
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

    public static String format( @Nullable Object object ) {
        if ( object instanceof Double ) {
            return format( ( Double ) object );
        } else if ( object instanceof Integer ) {
            return format( ( Integer ) object );
        } else if ( object instanceof Long ) {
            return format( ( Long ) object );
        } else if ( object != null ) {
            return format( object.toString() );
        } else {
            return "";
        }
    }
}
