package ubic.gemma.rest.providers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.rest.util.Sendfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the sendfile-vs-content-encoding interaction in {@link SendfileProvider}.
 * <p>
 * Tomcat sendfile writes the file at the connector level and never touches the entity stream, so if a
 * {@code WriterInterceptor} has wrapped that stream for compression, taking the sendfile path emits unencoded bytes
 * under an encoding header — a response no conforming client can read. The provider has to notice and stand down.
 */
public class SendfileProviderTest {

    private static final String CONTENT = "probe_id\tpvalue\n1\t0.01\n";

    @TempDir
    Path tmp;

    private SendfileProvider provider;
    private HttpServletRequest request;
    private Path file;

    @BeforeEach
    public void setUp() throws IOException {
        file = tmp.resolve( "resultSet_1.tsv" );
        Files.write( file, CONTENT.getBytes( StandardCharsets.UTF_8 ) );
        request = mock( HttpServletRequest.class );
        // Tomcat advertises sendfile support for this request.
        when( request.getAttribute( "org.apache.tomcat.sendfile.support" ) ).thenReturn( Boolean.TRUE );
        provider = new SendfileProvider();
        ReflectionTestUtils.setField( provider, "request", request );
        ReflectionTestUtils.setField( provider, "sendfileEnabled", true );
    }

    @Test
    public void plainStreamTakesTheSendfilePath() throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();

        writeTo( headers, entityStream );

        verify( request ).setAttribute( "org.apache.tomcat.sendfile.filename", file.toString() );
        assertThat( headers.getFirst( "Content-Length" ) ).isEqualTo( ( long ) CONTENT.length() );
        // Tomcat sends the bytes; nothing goes through the entity stream.
        assertThat( entityStream.size() ).isZero();
    }

    /**
     * The guard. An encoder wrapped the stream, so sendfile must be skipped and the file written through the
     * wrapper instead — otherwise the client gets plain text labelled {@code Content-Encoding: gzip}.
     */
    @Test
    public void wrappedStreamFallsBackToStreamingThroughTheEncoder() throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle( "Content-Encoding", "gzip" );
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try ( GZIPOutputStream encoded = new GZIPOutputStream( sink ) ) {
            writeTo( headers, encoded );
        }

        verify( request, never() ).setAttribute( eq( "org.apache.tomcat.sendfile.filename" ), any() );
        // What reached the client is genuinely gzipped, and inflates back to the file.
        assertThat( sink.toByteArray() ).startsWith( (byte) 0x1f, (byte) 0x8b );
        assertThat( inflate( sink.toByteArray() ) ).isEqualTo( CONTENT );
    }

    /**
     * A {@code Content-Encoding} header alone must NOT disable sendfile: that is exactly the situation of the
     * endpoints declaring {@code @GZIP(alreadyCompressed = true)}, whose header is appended after the encoder has
     * already declined to wrap the stream. Treating the header as the signal would quietly drop every large file
     * download off the zero-copy path.
     */
    @Test
    public void encodingHeaderWithoutAWrappedStreamStillUsesSendfile() throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle( "Content-Encoding", "gzip" );

        writeTo( headers, new ByteArrayOutputStream() );

        verify( request ).setAttribute( "org.apache.tomcat.sendfile.filename", file.toString() );
    }

    // ---- helpers ---------------------------------------------------------

    private void writeTo( MultivaluedMap<String, Object> headers, OutputStream entityStream ) throws IOException {
        provider.writeTo( Sendfile.of( file ), Sendfile.class, Sendfile.class, new java.lang.annotation.Annotation[0],
                MediaType.APPLICATION_OCTET_STREAM_TYPE, headers, entityStream );
    }

    private static String inflate( byte[] gz ) throws IOException {
        try ( GZIPInputStream in = new GZIPInputStream( new java.io.ByteArrayInputStream( gz ) ) ) {
            return new String( in.readAllBytes(), StandardCharsets.UTF_8 );
        }
    }
}
