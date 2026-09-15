package ubic.gemma.core.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@code NetUtils}.
 * <p>
 * Two groups. {@link NetUtils#bytePerSecondToDisplaySize} is the rate-formatter used by download
 * progress reporting: the threshold boundaries (B vs KB vs MB vs GB) and the two-decimal
 * formatting contract are pinned below.
 * <p>
 * {@link NetUtils#ftpDownloadFile} is exercised against a mocked {@link FTPClient}, so no server is
 * involved. The cases that matter are the wrong-length ones: a transfer can report success and
 * still leave a file that does not match the remote size — short, or LONGER than the remote when a
 * run of already-written bytes is replayed mid-stream. Nothing used to compare the two, so the bad
 * file was accepted, cached, and only failed later in whatever read it. A corrupt GEO
 * {@code _RAW.tar} surfaced as {@code Invalid byte <n> at offset 0 ... len=8} from the tar reader,
 * which reads like a bad archive rather than a bad transfer.
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

    // ---- ftpDownloadFile: what landed on disk has to match the remote size ----

    /**
     * A client that reports {@code bytesDelivered} bytes written for a remote file advertised at
     * {@code remoteSize}.
     */
    private static FTPClient clientDelivering( String seekFile, long remoteSize, int bytesDelivered ) throws IOException {
        FTPClient f = mock( FTPClient.class );
        when( f.isConnected() ).thenReturn( true );
        FTPFile remote = new FTPFile();
        remote.setName( seekFile );
        remote.setSize( remoteSize );
        when( f.listFiles( seekFile ) ).thenReturn( new FTPFile[] { remote } );
        when( f.retrieveFile( eq( seekFile ), any( OutputStream.class ) ) ).thenAnswer( inv -> {
            OutputStream os = inv.getArgument( 1 );
            os.write( new byte[bytesDelivered] );
            return true;
        } );
        return f;
    }

    @Test
    public void downloadMatchingTheRemoteSize_succeedsAndKeepsTheFile( @TempDir Path tmp ) throws IOException {
        File out = tmp.resolve( "GSE1_RAW.tar" ).toFile();
        FTPClient f = clientDelivering( "GSE1_RAW.tar", 1024, 1024 );

        assertThat( NetUtils.ftpDownloadFile( f, "GSE1_RAW.tar", out, false ) ).isTrue();
        assertThat( out ).exists().hasSize( 1024 );
    }

    /**
     * The fault actually observed against NCBI: the transfer replays a run of bytes and the local
     * file ends up bigger than the remote.
     */
    @Test
    public void overlongDownload_isRejectedAndTheFileRemoved( @TempDir Path tmp ) throws IOException {
        File out = tmp.resolve( "GSE1_RAW.tar" ).toFile();
        FTPClient f = clientDelivering( "GSE1_RAW.tar", 1024, 1200 );

        assertThatThrownBy( () -> NetUtils.ftpDownloadFile( f, "GSE1_RAW.tar", out, false ) )
                .isInstanceOf( IOException.class )
                .hasMessageContaining( "1200" )
                .hasMessageContaining( "1024" );
        assertThat( out ).doesNotExist();
    }

    @Test
    public void shortDownload_isRejectedAndTheFileRemoved( @TempDir Path tmp ) throws IOException {
        File out = tmp.resolve( "GSE1_RAW.tar" ).toFile();
        FTPClient f = clientDelivering( "GSE1_RAW.tar", 1024, 900 );

        assertThatThrownBy( () -> NetUtils.ftpDownloadFile( f, "GSE1_RAW.tar", out, false ) )
                .isInstanceOf( IOException.class );
        assertThat( out ).doesNotExist();
    }

    /**
     * A wrong-length file must not survive to be mistaken for a complete download by the
     * skip-if-already-correct check on the next run.
     */
    @Test
    public void aRejectedDownloadIsNotReusedByTheSkipCheck( @TempDir Path tmp ) throws IOException {
        File out = tmp.resolve( "GSE1_RAW.tar" ).toFile();

        assertThatThrownBy( () -> NetUtils.ftpDownloadFile( clientDelivering( "GSE1_RAW.tar", 1024, 1200 ),
                "GSE1_RAW.tar", out, false ) ).isInstanceOf( IOException.class );

        // second attempt gets it right; it must actually transfer rather than skip
        FTPClient good = clientDelivering( "GSE1_RAW.tar", 1024, 1024 );
        assertThat( NetUtils.ftpDownloadFile( good, "GSE1_RAW.tar", out, false ) ).isTrue();
        verify( good ).retrieveFile( eq( "GSE1_RAW.tar" ), any( OutputStream.class ) );
        assertThat( out ).hasSize( 1024 );
    }

    /**
     * The skip path pre-staging relies on: a local file already matching the remote size is used
     * as-is and no transfer happens.
     */
    @Test
    public void localFileAlreadyMatchingTheRemoteSize_skipsTheDownload( @TempDir Path tmp ) throws IOException {
        File out = tmp.resolve( "GSE1_RAW.tar" ).toFile();
        java.nio.file.Files.write( out.toPath(), new byte[1024] );
        FTPClient f = clientDelivering( "GSE1_RAW.tar", 1024, 1024 );

        assertThat( NetUtils.ftpDownloadFile( f, "GSE1_RAW.tar", out, false ) ).isTrue();
        verify( f, never() ).retrieveFile( eq( "GSE1_RAW.tar" ), any( OutputStream.class ) );
    }

    @Test
    public void forceOverridesTheSkip( @TempDir Path tmp ) throws IOException {
        File out = tmp.resolve( "GSE1_RAW.tar" ).toFile();
        java.nio.file.Files.write( out.toPath(), new byte[1024] );
        FTPClient f = clientDelivering( "GSE1_RAW.tar", 1024, 1024 );

        assertThat( NetUtils.ftpDownloadFile( f, "GSE1_RAW.tar", out, true ) ).isTrue();
        verify( f ).retrieveFile( eq( "GSE1_RAW.tar" ), any( OutputStream.class ) );
    }
}
