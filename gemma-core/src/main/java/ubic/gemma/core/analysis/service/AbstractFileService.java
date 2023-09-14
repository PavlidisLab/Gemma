package ubic.gemma.core.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Provide base implementation for all sorts of file services that serialize data in tabular format.
 */
public abstract class AbstractFileService<T> implements TsvFileService<T>, JsonFileService<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * @return a formatted double according to {@link AbstractFileService#getNumberFormat()}, an empty string if
     * d is null or NaN or inf/-inf if infinite
     */
    protected String format( @Nullable Double d ) {
        if ( d == null || d.isNaN() ) {
            return "";
        } else if ( d.isInfinite() ) {
            return d > 0 ? "inf" : "-inf";
        } else {
            return getNumberFormat().format( d );
        }
    }

    @Override
    public void writeTsv( T entity, File file ) throws IOException {
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( file ) ) ) {
            writeTsv( entity, writer );
        }
    }

    @Override
    public void writeJson( T entity, Writer writer ) throws IOException {
        objectMapper.writeValue( writer, entity );
    }

    @Override
    public void writeJson( T entity, File file ) throws IOException {
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( file ) ) ) {
            writeJson( entity, writer );
        }
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
