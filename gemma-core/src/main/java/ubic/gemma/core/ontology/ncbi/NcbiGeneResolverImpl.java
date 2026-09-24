package ubic.gemma.core.ontology.ncbi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.entrez.EntrezRetmode;
import ubic.gemma.core.loader.entrez.EntrezUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * {@link NcbiGeneResolver} over Entrez {@code esummary db=gene}.
 *
 * <h2>The response contract, measured 2026-09-12 rather than read from the docs</h2>
 * All three outcomes arrive as <b>HTTP 200</b> and are distinguished only by the body, which is why this
 * parses defensively rather than trusting the status line:
 * <ul>
 *   <li>live gene (22059): {@code name=Trp53}, {@code description=transformation related protein 53},
 *       {@code status=""}, {@code organism.scientificname=Mus musculus};</li>
 *   <li>withdrawn gene (918, {@code CD3W}): the same shape, but {@code status=2};</li>
 *   <li>fabricated id (999999999): {@code result.999999999.error = "cannot get document summary"}.</li>
 * </ul>
 * Anything else — a non-200, a body that is not JSON, a missing uid object — is
 * {@link NcbiUnavailableException}, never "no such gene". An outage must not look like a finding.
 * <p>
 * Both positive and negative answers are cached in the admin-evictable {@link #CACHE_NAME} cache: Entrez
 * allows 3 requests/s unauthenticated, and {@link EntrezUtils#doNicely} serializes calls to honour that, so
 * an uncached commit carrying many genotype statements would otherwise wait one request at a time.
 * <p>
 * 🛑 The {@code entrez.efetch.apikey} in use as of 2026-08-26 is REJECTED by NCBI (HTTP 400 with it, 200
 * without it). It is threaded through here the same way every other Entrez caller threads it, so blanking
 * the property is what restores this — a wrong key is worse than none.
 *
 * @author gemma
 */
@Slf4j
@Component
public class NcbiGeneResolverImpl implements NcbiGeneResolver {

    public static final String CACHE_NAME = "NcbiGeneResolver.genes";

    private static final int TIMEOUT_MS = 8000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${entrez.efetch.apikey}")
    @Nullable
    private String apiKey;

    @Autowired
    private CacheManager cacheManager;

    @Nullable
    private Cache cache;

    /** Lazily, because the CacheManager is not available at construction time on some boot orderings. */
    @Nullable
    private Cache getCache() {
        if ( cache == null ) {
            cache = cacheManager.getCache( CACHE_NAME );
        }
        return cache;
    }

    @Override
    public NcbiGeneRecord resolve( int ncbiId ) throws NcbiUnavailableException {
        Cache c = getCache();
        if ( c != null ) {
            Cache.ValueWrapper wrapper = c.get( ncbiId );
            if ( wrapper != null ) {
                return ( NcbiGeneRecord ) wrapper.get();
            }
        }
        NcbiGeneRecord record = query( ncbiId );
        if ( c != null ) {
            c.put( ncbiId, record );
        }
        return record;
    }

    private NcbiGeneRecord query( int ncbiId ) throws NcbiUnavailableException {
        URL url = EntrezUtils.summaryById( "gene", String.valueOf( ncbiId ), EntrezRetmode.JSON, apiKey );
        JsonNode root;
        try {
            root = EntrezUtils.doNicely( () -> fetch( url, ncbiId ), apiKey );
        } catch ( IOException e ) {
            throw new NcbiUnavailableException( "Failed to reach Entrez for gene " + ncbiId, e );
        }
        return parse( root, ncbiId );
    }

    private JsonNode fetch( URL url, int ncbiId ) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = ( HttpURLConnection ) url.openConnection();
            conn.setRequestMethod( "GET" );
            conn.setRequestProperty( "Accept", "application/json" );
            conn.setConnectTimeout( TIMEOUT_MS );
            conn.setReadTimeout( TIMEOUT_MS );
            int status = conn.getResponseCode();
            if ( status != HttpURLConnection.HTTP_OK ) {
                throw new IOException( "Entrez answered HTTP " + status + " for gene " + ncbiId );
            }
            try ( InputStream is = conn.getInputStream() ) {
                return objectMapper.readTree( is );
            }
        } finally {
            if ( conn != null ) {
                conn.disconnect();
            }
        }
    }

    /**
     * Read one uid object out of an {@code esummary db=gene} response.
     *
     * @throws NcbiUnavailableException when the envelope is not the shape documented on this class — the
     *                                 honest answer is "could not check", not "no such gene".
     */
    static NcbiGeneRecord parse( JsonNode root, int ncbiId ) throws NcbiUnavailableException {
        JsonNode result = root.path( "result" );
        if ( result.isMissingNode() || !result.isObject() ) {
            throw new NcbiUnavailableException( "Entrez response for gene " + ncbiId + " carries no 'result'." );
        }
        JsonNode uid = result.path( String.valueOf( ncbiId ) );
        if ( uid.isMissingNode() || !uid.isObject() ) {
            throw new NcbiUnavailableException( "Entrez response for gene " + ncbiId + " carries no uid object." );
        }
        if ( uid.hasNonNull( "error" ) ) {
            // "cannot get document summary" — NCBI has nothing under this id.
            return NcbiGeneRecord.notFound( ncbiId );
        }
        String symbol = uid.path( "name" ).asText( null );
        if ( StringUtils.isBlank( symbol ) ) {
            throw new NcbiUnavailableException( "Entrez summary for gene " + ncbiId + " has neither a name nor an error." );
        }
        String description = uid.path( "description" ).asText( null );
        String organism = uid.path( "organism" ).path( "scientificname" ).asText( null );
        // An empty status is a current record; any value (918 reads "2") means withdrawn or secondary.
        boolean live = StringUtils.isBlank( uid.path( "status" ).asText( "" ) );
        return new NcbiGeneRecord( ncbiId, symbol, description, organism, true, live );
    }
}
