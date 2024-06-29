package ubic.gemma.core.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.ArrayUtils;
import ubic.gemma.core.lang.Nullable;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Provide base implementation for all sorts of file services that serialize data in tabular format.
 */
public abstract class AbstractFileService<T> implements TsvFileService<T>, JsonFileService<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * @param extraHeaderComments additional header comments that will be included at the top of the TSV file.
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
     * @return a formatted double, an empty string if d is null or NaN or inf/-inf if infinite
     */
    protected String format( @Nullable Double d ) {
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

    protected String escapeTsv( String s ) {
        return s.replace( "\\", "\\\\" )
                .replace( "\n", "\\n" )
                .replace( "\t", "\\t" )
                .replace( "\r", "\\r" );
    }

    @Override
    public void writeJson( T entity, Writer writer ) throws IOException {
        objectMapper.writeValue( writer, entity );
    }

    @Override
    public void write( T entity, Writer writer, String contentType ) throws IOException {
        if ( "application/json".equalsIgnoreCase( contentType ) ) {
            writeJson( entity, writer );
        } else if ( "text/tab-separated-values".equalsIgnoreCase( contentType ) ) {
            writeTsv( entity, writer );
        } else {
            throw new IllegalArgumentException( "Unsupported content type: " + contentType );
        }
    }

    @Override
    public void write( T entity, File file, String contentType ) throws IOException {
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( file ) ) ) {
            write( entity, writer, contentType );
        }
    }
}
