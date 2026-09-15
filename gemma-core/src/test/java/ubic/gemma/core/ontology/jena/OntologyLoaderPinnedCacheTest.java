package ubic.gemma.core.ontology.jena;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A pinned ontology cache must load the bytes on disk and contact the remote <em>not at all</em>.
 *
 * <p>Sibling to {@link OntologyLoaderConditionalGetTest}, and the distinction between them is the
 * point. Conditional GET solves a COST problem — don't re-transfer an unchanged source. Pinning
 * solves a REPRODUCIBILITY problem — don't let a restart decide to adopt a new release at all. A
 * 304 still asks the question, and on the day upstream answers "changed", the vocabulary swaps
 * underneath a corpus that was annotated against the old one.</p>
 *
 * <p>Observed on frink 2026-08-15: CHEBI's recorded validator stopped matching upstream, so an
 * ordinary restart began pulling 866 MB at ~174 KB/s — 73 minutes, and a different CHEBI at the
 * end of it. Nobody asked for that ontology to move.</p>
 *
 * <p>The assertions count requests against a real loopback server rather than mocking, so "did not
 * contact the remote" is measured rather than assumed — a mock would only confirm our own belief
 * about which call sites exist.</p>
 */
class OntologyLoaderPinnedCacheTest {

    private static final String OWL = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n"
            + "  <owl:Class rdf:about=\"http://example.org/Pinned\"/>\n"
            + "</rdf:RDF>\n";

    private static final String CACHE_NAME = "pinnedCacheTestOntology";

    private HttpServer server;
    private String url;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
        server.createContext( "/onto.owl", this::handle );
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/onto.owl";
        cleanCache();
    }

    /** Counts EVERY request, including HEAD and conditional GET — a pinned load must make none. */
    private void handle( HttpExchange ex ) throws IOException {
        requests.incrementAndGet();
        byte[] body = OWL.getBytes( StandardCharsets.UTF_8 );
        if ( "HEAD".equals( ex.getRequestMethod() ) ) {
            ex.getResponseHeaders().add( "Content-Length", String.valueOf( body.length ) );
            ex.sendResponseHeaders( 200, -1 );
            ex.close();
            return;
        }
        ex.getResponseHeaders().add( "ETag", "\"pinned-test\"" );
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

    private OntModel load( boolean allowRemote ) throws IOException {
        return OntologyLoader.createMemoryModel( url, CACHE_NAME, CACHE_NAME, false,
                OntModelSpec.OWL_MEM, allowRemote );
    }

    @Test
    void aPinnedLoadReadsTheCacheAndMakesNoRequestAtAll() throws Exception {
        load( true );                       // seed the cache normally
        int afterSeed = requests.get();
        assertThat( afterSeed ).isGreaterThan( 0 );

        OntModel pinned = load( false );

        assertThat( pinned ).isNotNull();
        assertThat( pinned.getOntClass( "http://example.org/Pinned" ) ).isNotNull();
        // Not "no body transferred" — no request of any kind, conditional or HEAD included.
        assertThat( requests.get() )
                .as( "a pinned load must not contact the remote at all" )
                .isEqualTo( afterSeed );
    }

    @Test
    void repeatedPinnedLoadsStayOffTheNetwork() throws Exception {
        load( true );
        int afterSeed = requests.get();
        for ( int i = 0; i < 3; i++ ) {
            load( false );
        }
        assertThat( requests.get() ).isEqualTo( afterSeed );
    }

    /**
     * A cold volume has nothing to pin to. Seeding once beats failing the boot, and the warning
     * on that path is what keeps the one fetch from being a silent surprise.
     */
    @Test
    void withNothingCachedAPinnedLoadSeedsTheCacheOnce() throws Exception {
        assertThat( OntologyLoader.getDiskCachePath( CACHE_NAME ) ).doesNotExist();

        OntModel first = load( false );

        assertThat( first ).isNotNull();
        assertThat( requests.get() ).isGreaterThan( 0 );
        assertThat( OntologyLoader.getDiskCachePath( CACHE_NAME ) ).exists();

        int afterSeed = requests.get();
        load( false );
        assertThat( requests.get() )
                .as( "once seeded, pinning takes over" )
                .isEqualTo( afterSeed );
    }

    /**
     * The escape hatch has to keep working: POST /admin/ontologies/{name}/refresh passes
     * forceLoad=true, which is what allowRemote=true represents here. Without this, pinning would
     * mean an ontology could never be updated at all.
     */
    @Test
    void anExplicitRefreshStillReachesTheRemote() throws Exception {
        load( true );
        int afterSeed = requests.get();
        load( false );
        assertThat( requests.get() ).isEqualTo( afterSeed );

        load( true );

        assertThat( requests.get() )
                .as( "forceLoad must bypass the pin, or nothing could ever be updated" )
                .isGreaterThan( afterSeed );
    }
}
