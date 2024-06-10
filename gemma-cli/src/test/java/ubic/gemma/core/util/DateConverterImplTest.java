package ubic.gemma.core.util;

import org.junit.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class DateConverterImplTest {

    private final Date relativeTo = new Date();
    private final DateConverterImpl c = new DateConverterImpl( relativeTo, TimeZone.getTimeZone( "America/Vancouver" ) );

    @Test
    public void test() throws ParseException {
        assertThat( c.apply( "2019-01-01" ) )
                .hasYear( 2019 ).hasMonth( 1 ).hasDayOfMonth( 1 );
        assertThat( c.apply( "2019-01-01 02:12:11" ) )
                .hasYear( 2019 ).hasMonth( 1 ).hasDayOfMonth( 1 );
    }

    @Test
    public void testIso8601() throws ParseException {
        assertThat( c.apply( "2019" ) )
                .hasYear( 2019 ).hasMonth( 1 ).hasDayOfMonth( 1 );
        assertThat( c.apply( "2019-01" ) )
                .hasYear( 2019 ).hasMonth( 1 ).hasDayOfMonth( 1 );
        assertThat( c.apply( "2019-01-01" ) )
                .hasYear( 2019 ).hasMonth( 1 ).hasDayOfMonth( 1 );
        assertThat( c.apply( "2019-01-01" ) )
                .hasYear( 2019 ).hasMonth( 1 ).hasDayOfMonth( 1 );
        assertThat( c.apply( "2019-01-01T02:12:11" ) )
                .hasYear( 2019 ).hasMonth( 1 ).hasDayOfMonth( 1 )
                .hasHourOfDay( 2 ).hasMinute( 12 ).hasSecond( 11 );
        assertThat( c.apply( "2019-01-01T02:12:11Z" ) )
                .hasYear( 2018 ).hasMonth( 12 ).hasDayOfMonth( 31 )
                .hasHourOfDay( 18 ).hasMinute( 12 ).hasSecond( 11 );
        assertThat( c.apply( "2019-01-01T02:12:11+05:00" ) )
                .hasYear( 2018 ).hasMonth( 12 ).hasDayOfMonth( 31 )
                .hasHourOfDay( 13 ).hasMinute( 12 ).hasSecond( 11 );
        assertThat( c.apply( "2019-01-01T02:12:11-05:00" ) )
                .hasYear( 2018 ).hasMonth( 12 ).hasDayOfMonth( 31 )
                .hasHourOfDay( 23 ).hasMinute( 12 ).hasSecond( 11 );
    }

    @Test
    public void testWords() throws ParseException {
        assertThat( c.apply( "now" ) ).isCloseTo( relativeTo, 10 );

        assertThat( c.apply( "yesterday" ) )
                .isBefore( relativeTo );
        assertThat( c.apply( "today" ) )
                .isCloseTo( relativeTo, 10 );
        assertThat( c.apply( "tomorrow" ) )
                .isAfter( relativeTo );

        c.apply( "last week" );
        c.apply( "last month" );
        c.apply( "last year" );

        c.apply( "next week" );
        c.apply( "next month" );
        c.apply( "next year" );

        c.apply( "two weeks ago" );
        c.apply( "one month ago" );
        c.apply( "two months ago" );
    }
}