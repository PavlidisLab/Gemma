package ubic.gemma.core.util;

import org.apache.commons.cli.Converter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import ubic.basecode.util.DateUtil;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OptionsUtils {

    /**
     * When parsing dates, use this as a reference for 'now'.
     */
    private static final Date DEFAULT_RELATIVE_TO = new Date();

    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();

    /**
     * Add a date option with support for fuzzy dates (i.e. one month ago).
     * @see DateConverterImpl
     */
    public static void addDateOption( String name, @Nullable String longOpt, String desc, Options options ) {
        options.addOption( Option.builder( name )
                .longOpt( longOpt )
                .desc( desc )
                .hasArg()
                .type( Date.class )
                .converter( new DateConverterImpl( DEFAULT_RELATIVE_TO, DEFAULT_TIME_ZONE ) ).build() );
    }

    /**
     * A converter for parsing dates supporting various formats.
     * <ul>
     *     <li>most ISO 8601 date and date time with or without UTC offset</li>
     *     <li>{@code +1d, -1m, -1h} as per {@link DateUtil#getRelativeDate(Date, String)}</li>
     *     <li>natural language (i.e. five hours ago, last week, etc. using {@link PrettyTimeParser}</li>
     * </ul>
     * @author poirigui
     */
    static class DateConverterImpl implements Converter<Date, ParseException> {

        /**
         * Exact date formats to attempt before resorting to natural language parsing.
         */
        private final SimpleDateFormat[] exactDateFormats = {
                // ISO 8601
                new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH ),
                new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH ),
                new SimpleDateFormat( "yyyy-MM-dd", Locale.ENGLISH ),
                new SimpleDateFormat( "yyyy-MM", Locale.ENGLISH ),
                new SimpleDateFormat( "yyyy", Locale.ENGLISH )
        };

        private final Date relativeTo;
        private final PrettyTimeParser parser;

        /**
         * @param relativeTo date relative to which duration are interpreted
         * @param timeZone   when parsing date, use this time zone as a reference
         */
        public DateConverterImpl( Date relativeTo, TimeZone timeZone ) {
            this.relativeTo = relativeTo;
            this.parser = new PrettyTimeParser( timeZone );
            for ( SimpleDateFormat format : exactDateFormats ) {
                format.setTimeZone( timeZone );
            }
        }

        @Override
        public Date apply( String string ) throws ParseException {
            for ( SimpleDateFormat format : exactDateFormats ) {
                try {
                    return format.parse( string );
                } catch ( ParseException e ) {
                    // ignore
                }
            }
            try {
                return DateUtil.getRelativeDate( relativeTo, string );
            } catch ( IllegalArgumentException e ) {
                // ignore
            }
            List<Date> candidates = parser.parse( string, relativeTo );
            if ( candidates.isEmpty() ) {
                throw new ParseException( "No suitable date found.", 0 );
            }
            if ( candidates.size() > 1 ) {
                throw new ParseException( "More than one date is specified.", 0 );
            }
            return candidates.iterator().next();
        }
    }
}
