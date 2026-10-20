package ubic.gemma.rest.providers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.junit.jupiter.api.Test;
import ubic.gemma.rest.AnalysisResultSetsWebService;
import ubic.gemma.rest.annotations.GZIP;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins which of the two {@code Content-Encoding} decorators fires, for each shape of the {@link GZIP} annotation.
 * <p>
 * The distinction matters because the two run on opposite sides of Jersey's {@code GZipEncoder}:
 * {@link GzipHeaderDecorator} sets the header before it, which is what makes the encoder compress, while
 * {@link GzipHeaderDecoratorAfterGZipEncoder} sets it after, labelling a payload that is already compressed. Pick
 * the wrong one and the response is either double-compressed or labelled gzip while being plain text.
 */
public class GzipHeaderDecoratorTest {

    private static final String TSV = "text/tab-separated-values; charset=UTF-8";

    private final GzipHeaderDecorator before = new GzipHeaderDecorator();
    private final GzipHeaderDecoratorAfterGZipEncoder after = new GzipHeaderDecoratorAfterGZipEncoder();

    /**
     * Annotation shapes under test, read back off these methods by reflection so the compiler builds the real
     * annotation instances — including wrapping repeated {@code @GZIP} into its container.
     */
    @SuppressWarnings("unused")
    private static class Fixtures {
        @GZIP
        void unconditional() {
        }

        @GZIP(alreadyCompressed = true)
        void preCompressed() {
        }

        @GZIP(mediaTypes = TSV)
        void tsvOnly() {
        }

        /** The {@code /resultSets/{resultSet}} shape: encoder-compressed JSON, pre-compressed TSV. */
        @GZIP(mediaTypes = MediaType.APPLICATION_JSON)
        @GZIP(mediaTypes = TSV, alreadyCompressed = true)
        void perRepresentation() {
        }
    }

    @Test
    public void unconditionalGzipIsHandledBeforeTheEncoder() throws IOException {
        assertThat( encodingSetBy( before, "unconditional", MediaType.APPLICATION_JSON ) ).isEqualTo( "gzip" );
        assertThat( encodingSetBy( after, "unconditional", MediaType.APPLICATION_JSON ) ).isNull();
    }

    @Test
    public void preCompressedGzipIsHandledAfterTheEncoder() throws IOException {
        assertThat( encodingSetBy( before, "preCompressed", TSV ) ).isNull();
        assertThat( encodingSetBy( after, "preCompressed", TSV ) ).isEqualTo( "gzip" );
    }

    @Test
    public void mediaTypeRestrictionIsHonoured() throws IOException {
        assertThat( encodingSetBy( before, "tsvOnly", TSV ) ).isEqualTo( "gzip" );
        assertThat( encodingSetBy( before, "tsvOnly", MediaType.APPLICATION_JSON ) ).isNull();
    }

    /**
     * The repeated-annotation case. A method carrying two {@code @GZIP} reaches the interceptor as a single
     * {@code @GZIPs} container; before it was unwrapped, neither decorator matched and the endpoint quietly lost
     * compression on both representations.
     */
    @Test
    public void repeatedGzipCompressesJsonThroughTheEncoder() throws IOException {
        assertThat( encodingSetBy( before, "perRepresentation", MediaType.APPLICATION_JSON ) ).isEqualTo( "gzip" );
        assertThat( encodingSetBy( after, "perRepresentation", MediaType.APPLICATION_JSON ) ).isNull();
    }

    @Test
    public void repeatedGzipLabelsTsvWithoutInvokingTheEncoder() throws IOException {
        assertThat( encodingSetBy( before, "perRepresentation", TSV ) ).isNull();
        assertThat( encodingSetBy( after, "perRepresentation", TSV ) ).isEqualTo( "gzip" );
    }

    /**
     * Neither decorator may leave a representation both unlabelled and uncompressed — that is the silent-regression
     * shape, and it is what a dropped container annotation looks like from the outside.
     */
    @Test
    public void everyRepresentationOfTheRepeatedShapeGetsExactlyOneDecorator() throws IOException {
        for ( String contentType : new String[] { MediaType.APPLICATION_JSON, TSV } ) {
            int decorators = 0;
            if ( encodingSetBy( before, "perRepresentation", contentType ) != null ) decorators++;
            if ( encodingSetBy( after, "perRepresentation", contentType ) != null ) decorators++;
            assertThat( decorators )
                    .withFailMessage( "expected exactly one decorator to handle %s, got %d", contentType, decorators )
                    .isEqualTo( 1 );
        }
    }

    /**
     * The {@code GET /resultSets} list route, driven off its real annotations rather than a fixture.
     * <p>
     * Every row of that listing repeats its parent {@code analysis} and its {@code experimentalFactors} verbatim,
     * so the body is dominated by duplicated text: measured on gemma2 at {@code ?limit=20}, 229,515 bytes
     * uncompressed against 5,777 gzipped. Compression is the whole reason the route is affordable, and losing it
     * is invisible from the response body — only the missing {@code Content-Encoding} header shows it.
     */
    @Test
    public void resultSetsListingIsCompressedThroughTheEncoder() throws IOException {
        Annotation[] route = annotationsOfRoute( AnalysisResultSetsWebService.class, "getResultSets" );
        assertThat( encodingSetBy( before, route, MediaType.APPLICATION_JSON ) ).isEqualTo( "gzip" );
        assertThat( encodingSetBy( after, route, MediaType.APPLICATION_JSON ) ).isNull();
    }

    // ---- helpers ---------------------------------------------------------

    /**
     * Run one decorator over a context carrying {@code methodName}'s annotations and the given response
     * {@code Content-Type}.
     *
     * @return the {@code Content-Encoding} the decorator set, or null if it set none.
     */
    private String encodingSetBy( AbstractGzipHeaderDecorator decorator, String methodName, String contentType )
            throws IOException {
        return encodingSetBy( decorator, annotationsOf( methodName ), contentType );
    }

    private String encodingSetBy( AbstractGzipHeaderDecorator decorator, Annotation[] annotations, String contentType )
            throws IOException {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle( "Content-Type", contentType );
        WriterInterceptorContext context = mock( WriterInterceptorContext.class );
        when( context.getAnnotations() ).thenReturn( annotations );
        when( context.getHeaders() ).thenReturn( headers );
        decorator.aroundWriteTo( context );
        Object encoding = headers.getFirst( "Content-Encoding" );
        return encoding != null ? encoding.toString() : null;
    }

    private static Annotation[] annotationsOf( String methodName ) {
        return annotationsOfRoute( Fixtures.class, methodName );
    }

    /**
     * The annotations Jersey would hand the interceptor for a resource method, looked up by name so the test does
     * not have to restate the route's parameter list.
     */
    private static Annotation[] annotationsOfRoute( Class<?> resource, String methodName ) {
        for ( Method m : resource.getDeclaredMethods() ) {
            if ( m.getName().equals( methodName ) ) {
                return m.getAnnotations();
            }
        }
        throw new IllegalArgumentException( "No method named " + methodName + " on " + resource.getName() );
    }
}
