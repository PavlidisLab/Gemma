package ubic.gemma.core.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NetUtils#bytePerSecondToDisplaySize}: the rate-formatter
 * used by download progress reporting. Pins the threshold boundaries (B vs KB vs
 * MB vs GB) and the two-decimal formatting contract — the rest of {@code NetUtils}
 * is FTP I/O that requires a live server and lives in network-tagged ITs.
 *
 * @author claude
 */
public class NetUtilsTest {

    /**
     * The implementation uses {@link String#format} without a locale, so on environments
     * with a comma decimal separator the formatted string differs. Force US locale for
     * substring asserts and use locale-aware values where needed.
     */
    private static String fmt( double v ) {
        Locale prev = Locale.getDefault();
        try {
            Locale.setDefault( Locale.US );
            return NetUtils.bytePerSecondToDisplaySize( v );
        } finally {
            Locale.setDefault( prev );
        }
    }

    @Test
    public void zeroBytesPerSecond_rendersAsBytesPerSecond() {
        assertThat( fmt( 0.0 ) ).isEqualTo( "0.00 B/s" );
    }

    @Test
    public void belowKilobyteThreshold_rendersAsBytes() {
        assertThat( fmt( 999.0 ) ).isEqualTo( "999.00 B/s" );
    }

    @Test
    public void atKilobyteThreshold_rendersAsKilobytes() {
        // exactly 1e3 = 1000 -> "1.00 KB/s"
        assertThat( fmt( 1000.0 ) ).isEqualTo( "1.00 KB/s" );
    }

    @Test
    public void belowMegabyteThreshold_rendersAsKilobytes() {
        assertThat( fmt( 999_000.0 ) ).isEqualTo( "999.00 KB/s" );
    }

    @Test
    public void atMegabyteThreshold_rendersAsMegabytes() {
        assertThat( fmt( 1_000_000.0 ) ).isEqualTo( "1.00 MB/s" );
    }

    @Test
    public void belowGigabyteThreshold_rendersAsMegabytes() {
        assertThat( fmt( 999_000_000.0 ) ).isEqualTo( "999.00 MB/s" );
    }

    @Test
    public void atGigabyteThreshold_rendersAsGigabytes() {
        assertThat( fmt( 1_000_000_000.0 ) ).isEqualTo( "1.00 GB/s" );
    }

    @Test
    public void aboveGigabytePastThreshold_stillRendersAsGigabytes() {
        // No tera/peta in the impl; everything above 1e9 stays GB/s.
        assertThat( fmt( 1_500_000_000_000.0 ) ).isEqualTo( "1500.00 GB/s" );
    }

    @Test
    public void fractionalBytes_preservesTwoDecimals() {
        assertThat( fmt( 1.5 ) ).isEqualTo( "1.50 B/s" );
    }

    @Test
    public void fractionalKilobytes_preservesTwoDecimals() {
        // 1234 B/s -> 1.234 KB/s -> "1.23 KB/s" (banker's rounding via %.2f)
        assertThat( fmt( 1234.0 ) ).isEqualTo( "1.23 KB/s" );
    }
}
