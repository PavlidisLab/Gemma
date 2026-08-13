package ubic.gemma.core.ontology.jena;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.jena.ontology.OntModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ontology disk cache used to be a FAILURE fallback only: {@code readModelFromUrl} downloaded
 * the source unconditionally on every init and read the cached copy solely from the {@code catch}
 * block. For CHEBI that is an 826 MB pull from EBI at ~480 KB/s — about half an hour — repeated on
 * every container recreate, even though a byte-identical copy was sitting on the persistent volume.
 *
 * <p>Diagnosed on frink 2026-08-13: a complete {@code chebiOntology} from the previous boot sat
 * next to a freshly-growing {@code chebiOntology.tmp}, because nothing recorded a validator to
 * decide "the local copy is current" with.
 *
 * <p>These tests run a real loopback {@link HttpServer} rather than mocking, because the behaviour
 * under test lives in {@code HttpURLConnection}'s handling of conditional requests — a mock would
 * assert our own assumptions about 304 instead of the JDK's actual semantics.
 */
class OntologyLoaderConditionalGetTest {

    /** Minimal but valid RDF/XML, so Jena parses it rather than throwing. */
    private static final String OWL = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n"
            + "  <owl:Class rdf:about=\"http://example.org/T1\"/>\n"
            + "</rdf:RDF>\n";

    private static final String ETAG = "\"abc123\"";
    private static final String CACHE_NAME = "conditionalGetTestOntology";

    private HttpServer server;
    private String url;
    /** Counts requests that actually transferred a body. */
    private final AtomicInteger bodyServed = new AtomicInteger();
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
        server.createContext( "/onto.owl", this::handle );
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/onto.owl";
        cleanCache();
    }

    private void handle( HttpExchange ex ) throws IOException {
        requests.incrementAndGet();
        String inm = ex.getRequestHeaders().getFirst( "If-None-Match" );
        if ( ETAG.equals( inm ) ) {
            ex.getResponseHeaders().add( "ETag", ETAG );
            ex.sendResponseHeaders( 304, -1 );
            ex.close();
            return;
        }
        byte[] body = OWL.getBytes( StandardCharsets.UTF_8 );
        bodyServed.incrementAndGet();
        ex.getResponseHeaders().add( "ETag", ETAG );
        ex.getResponseHeaders().add( "Last-Modified", "Tue, 07 Jul 2026 16:57:44 GMT" );
        ex.sendResponseHeaders( 200, body.length );
        try ( OutputStream os = ex.getResponseBody() ) {
            os.write( body );
        }
    }

    @AfterEach
    void stopServer() {
        if ( server != null ) {
            server.stop( 0 );
        }
        cleanCache();
    }

    private void cleanCache() {
        //noinspection ResultOfMethodCallIgnored
        OntologyLoader.getDiskCachePath( CACHE_NAME ).delete();
        //noinspection ResultOfMethodCallIgnored
        OntologyLoader.getOldDiskCachePath( CACHE_NAME ).delete();
        //noinspection ResultOfMethodCallIgnored
        OntologyLoader.getTmpDiskCachePath( CACHE_NAME ).delete();
        //noinspection ResultOfMethodCallIgnored
        OntologyLoader.getValidatorMarkerPath( CACHE_NAME ).delete();
    }

    private OntModel load() throws IOException {
        return OntologyLoader.createMemoryModel( url, CACHE_NAME, CACHE_NAME, false,
                org.apache.jena.ontology.OntModelSpec.OWL_MEM );
    }

    @Test
    void theFirstLoadDownloadsAndRecordsAValidator() throws Exception {
        OntModel m = load();
        assertThat( m ).isNotNull();
        assertThat( bodyServed.get() ).isEqualTo( 1 );
        assertThat( OntologyLoader.getDiskCachePath( CACHE_NAME ) ).exists();
        File validator = OntologyLoader.getValidatorMarkerPath( CACHE_NAME );
        assertThat( validator ).exists();
        assertThat( new String( Files.readAllBytes( validator.toPath() ), StandardCharsets.UTF_8 ) )
                .contains( "etag=" + ETAG )
                .contains( "lastModified=" );
    }

    @Test
    void aSecondLoadSendsIfNoneMatchAndTransfersNoBody() throws Exception {
        load();
        assertThat( bodyServed.get() ).isEqualTo( 1 );

        OntModel second = load();

        assertThat( second ).isNotNull();
        assertThat( requests.get() )
                .as( "the second load must still ASK the server -- freshness is not assumed" )
                .isEqualTo( 2 );
        assertThat( bodyServed.get() )
                .as( "but it must not transfer the body again; this is the 826 MB / 30 minutes" )
                .isEqualTo( 1 );
    }

    @Test
    void aChangedUpstreamStillReDownloads() throws Exception {
        load();
        assertThat( bodyServed.get() ).isEqualTo( 1 );

        // Upstream publishes a new release: the recorded ETag no longer matches, so the server
        // answers 200 and we must take the new body. Conditional GET must not become a way to
        // pin a stale ontology forever.
        Files.write( OntologyLoader.getValidatorMarkerPath( CACHE_NAME ).toPath(),
                "etag=\"stale-etag\"\n".getBytes( StandardCharsets.UTF_8 ) );

        load();

        assertThat( bodyServed.get() )
                .as( "a genuine upstream change must still be picked up" )
                .isEqualTo( 2 );
    }

    @Test
    void a304DoesNotLookLikeAnOntologyChange() throws Exception {
        load();
        load(); // 304

        // hasChanged() compares the current cache against `.old` to decide whether to reindex.
        // If the 304 path left `.old` stale, every boot would report a change that did not happen
        // and reindex the ontology -- trading a download for a rebuild and fixing nothing.
        assertThat( OntologyLoader.hasChanged( CACHE_NAME ) )
                .as( "an unchanged ontology must not trigger a reindex" )
                .isFalse();
    }
}
