package ubic.gemma.core.analysis.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.ArrayUtils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Provide base implementation for all sorts of file services that serialize data in tabular format.
 * @param <T>
 */
public abstract class AbstractTsvFileService<T> implements TsvFileService<T> {

    /**
     * Get a number formatter suitable for reading from and writing into a tabular format.
     * @return a locale-independent {@link NumberFormat} object
     */
    protected NumberFormat getNumberFormat() {
        // show 4 significant digits
        DecimalFormat numberFormat = new DecimalFormat( "0.###E0" );
        numberFormat.setDecimalFormatSymbols( DecimalFormatSymbols.getInstance( Locale.ENGLISH ) );
        numberFormat.setRoundingMode( RoundingMode.HALF_UP );
        return numberFormat;
    }

    /**
     * Preconfigure a {@link CSVFormat.Builder} with desirable defaults.
     * @param extraHeaderComments additional header comments that will be included at the top of the TSV file.
     * @return
     */
    protected CSVFormat.Builder getTsvFormatBuilder( String... extraHeaderComments ) {
        return CSVFormat.Builder.create( CSVFormat.TDF )
                .setCommentMarker( '#' )
                .setHeaderComments( ArrayUtils.addAll( new String[] {
                        "If you use this file for your research, please cite:",
                        "Lim et al. (2021) Curation of over 10 000 transcriptomic studies to enable data reuse.",
                        "Database, baab006 (doi:10.1093/database/baab006)." }, extraHeaderComments ) );
    }

    /**
     * Get the delimiter used within column.
     */
    protected String getSubDelimiter() {
        return "|";
    }

    /**
     * Format a {@link Double} for TSV.
     * @param d a double to format
     * @return a formatted double according to {@link AbstractTsvFileService#getNumberFormat()} or an empty string if
     * d is null
     */
    protected String format( Double d ) {
        return d == null ? "" : getNumberFormat().format( d );
    }

}
