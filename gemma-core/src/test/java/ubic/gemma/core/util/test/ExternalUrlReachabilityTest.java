package ubic.gemma.core.util.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Out-of-band reachability probe for every external URL Gemma talks to.
 *
 * <p>This test is tagged {@code @Tag("network")} so it is excluded from the default {@code mvn verify}
 * pass (the parent {@code pom.xml} adds {@code network} to surefire's {@code excludedGroups}). It is
 * intended to be invoked explicitly, e.g. from a nightly CI job or ad hoc by a developer chasing a
 * loader regression caused by an upstream endpoint move.</p>
 *
 * <h2>How to run</h2>
 * <pre>{@code
 * mvn -pl gemma-core test -Dtest=ExternalUrlReachabilityTest -DexcludedGroups=
 * }</pre>
 * Override of {@code excludedGroups} clears the {@code network} exclusion injected by the parent pom
 * so this single test fires. (The default {@code -DexcludedGroups=integration,network} would skip it.)
 *
 * <h2>Output</h2>
 * Writes a structured JSON report to {@code gemma-core/target/external-url-reachability.json}. The
 * schema is intentionally flat so an ops dashboard or CI artifact can consume it directly:
 * <pre>{@code
 * {
 *   "timestamp": "2026-05-21T11:34:56Z",
 *   "totalCount": 28, "okCount": 25, "failCount": 3,
 *   "results": [
 *     {"label": "...", "category": "...", "url": "...", "ok": true,  "status": 200, "elapsedMs": 412},
 *     {"label": "...", "category": "...", "url": "...", "ok": false, "status": -1,  "elapsedMs": 5012, "error": "ConnectException: ..."}
 *   ]
 * }
 * }</pre>
 *
 * <h2>Failure behaviour</h2>
 * <ul>
 *   <li>If <em>all</em> URLs fail the test SKIPS (via {@link Assumptions#assumeTrue}) — the host's network
 *       is probably down and the result would be uninformative.</li>
 *   <li>If <em>some</em> URLs fail the test PASSES — the JSON dashboard alerts on per-URL drift.</li>
 * </ul>
 *
 * <h2>Adding a new endpoint</h2>
 * Append an {@link Endpoint} to {@link #ENDPOINTS}. Pick a meaningful {@code label} (used by the
 * dashboard); set {@code ftp=true} for plain {@code ftp://} URLs so the probe uses {@link FTPClient}
 * instead of HTTP.
 */
@Tag("network")
public class ExternalUrlReachabilityTest {

    private static final Logger log = LoggerFactory.getLogger( ExternalUrlReachabilityTest.class );

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    /**
     * NCBI's documented eutils rate cap is 3 req/s without an API key, 10 req/s with one. Probing
     * every eutils endpoint in parallel via ForkJoinPool blows past that and the server returns
     * HTTP 429. We serialize the eutils probes and sleep this many ms between them to stay under
     * the anonymous cap with a safety margin.
     */
    private static final long EUTILS_INTERVAL_MS = 400;

    private record Endpoint(String label, String category, String url, boolean ftp) {

        /** True for NCBI eutils endpoints — these must be probed serially under the 3 req/s cap. */
        boolean isEutils() {
            return url.contains( "eutils.ncbi.nlm.nih.gov" );
        }
    }

    /**
     * Curated inventory of external endpoints Gemma depends on. Not exhaustive — covers the
     * loader categories that have historically drifted (NCBI, EBI, OBO Foundry ontologies,
     * BioMart/Ensembl, CELLxGENE, UCSC). Add new entries when wiring a new external service.
     */
    private static final List<Endpoint> ENDPOINTS = List.of(
            // --- NCBI Entrez / E-utilities -------------------------------------------------------
            new Endpoint( "NCBI eutils - einfo", "Entrez",
                    "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/einfo.fcgi", false ),
            new Endpoint( "NCBI eutils - esearch", "Entrez",
                    "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=gemma&retmax=1", false ),
            new Endpoint( "NCBI eutils - esummary", "Entrez",
                    "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=20051986", false ),
            new Endpoint( "NCBI eutils - efetch", "Entrez",
                    "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=20051986&retmode=xml", false ),
            new Endpoint( "NCBI eutils - elink", "Entrez",
                    "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&db=gene&id=20051986", false ),
            new Endpoint( "NCBI pubmed landing", "Entrez",
                    "https://pubmed.ncbi.nlm.nih.gov/20051986/", false ),

            // --- NCBI FTP ------------------------------------------------------------------------
            new Endpoint( "NCBI FTP root", "NCBI-FTP",
                    "ftp://ftp.ncbi.nlm.nih.gov/", true ),
            new Endpoint( "NCBI gene/DATA README", "NCBI-FTP",
                    "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README", true ),
            new Endpoint( "NCBI taxonomy taxdump", "NCBI-FTP",
                    "ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/", true ),
            new Endpoint( "NCBI HomoloGene last-archive", "NCBI-FTP",
                    "ftp://ftp.ncbi.nlm.nih.gov/pub/HomoloGene/last-archive/", true ),

            // --- NCBI GEO ------------------------------------------------------------------------
            new Endpoint( "GEO query landing", "GEO",
                    "https://www.ncbi.nlm.nih.gov/geo/", false ),
            new Endpoint( "GEO acc.cgi (GSE1)", "GEO",
                    "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE1&targ=gse&form=xml&view=brief", false ),
            new Endpoint( "GEO FTP series", "GEO",
                    "ftp://ftp.ncbi.nlm.nih.gov/geo/series/", true ),
            new Endpoint( "GEO FTP platforms", "GEO",
                    "ftp://ftp.ncbi.nlm.nih.gov/geo/platforms/", true ),
            new Endpoint( "GEO FTP datasets", "GEO",
                    "ftp://ftp.ncbi.nlm.nih.gov/geo/datasets/", true ),

            // --- NCBI SRA ------------------------------------------------------------------------
            new Endpoint( "NCBI SRA term search", "SRA",
                    "https://www.ncbi.nlm.nih.gov/sra?term=SRX1620346", false ),

            // --- EBI -----------------------------------------------------------------------------
            new Endpoint( "EBI FTP root", "EBI-FTP",
                    "ftp://ftp.ebi.ac.uk/", true ),
            new Endpoint( "EBI GOA human", "EBI-FTP",
                    "ftp://ftp.ebi.ac.uk/pub/databases/GO/goa/HUMAN/gene_association.goa_human.gz", true ),
            new Endpoint( "EBI ArrayExpress experiments", "EBI-FTP",
                    "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/", true ),
            new Endpoint( "EBI EFO ontology", "Ontology",
                    "https://www.ebi.ac.uk/efo/efo.owl", false ),

            // --- Gene Ontology -------------------------------------------------------------------
            // ftp.geneontology.org was retired; the canonical download host moved to HTTP at
            // current.geneontology.org (latest release) and release.geneontology.org (versioned).
            new Endpoint( "GO current go.obo", "GO",
                    "http://current.geneontology.org/ontology/go.obo", false ),
            new Endpoint( "GO download catalog", "GO",
                    "https://geneontology.org/docs/download-ontology/", false ),

            // --- OBO Foundry ontologies (default.properties + basecode.properties) ---------------
            new Endpoint( "Uberon ontology", "Ontology",
                    "http://purl.obolibrary.org/obo/uberon.owl", false ),
            new Endpoint( "Cell Ontology (CL)", "Ontology",
                    "http://purl.obolibrary.org/obo/cl.owl", false ),
            new Endpoint( "Gene Ontology (GO)", "Ontology",
                    "http://purl.obolibrary.org/obo/go.owl", false ),
            new Endpoint( "Disease Ontology (DOID)", "Ontology",
                    "http://purl.obolibrary.org/obo/doid.owl", false ),
            new Endpoint( "Mondo Disease Ontology", "Ontology",
                    "http://purl.obolibrary.org/obo/mondo.owl", false ),
            new Endpoint( "PATO Ontology", "Ontology",
                    "http://purl.obolibrary.org/obo/pato/pato.owl", false ),
            new Endpoint( "Human Phenotype Ontology", "Ontology",
                    "http://purl.obolibrary.org/obo/hp.owl", false ),
            new Endpoint( "Mammalian Phenotype Ontology", "Ontology",
                    "http://purl.obolibrary.org/obo/mp.owl", false ),
            new Endpoint( "Cell Line Ontology (CLO)", "Ontology",
                    "http://purl.obolibrary.org/obo/clo.owl", false ),
            new Endpoint( "ChEBI Ontology", "Ontology",
                    "http://purl.obolibrary.org/obo/chebi.owl", false ),
            new Endpoint( "EMAPA (mouse devel) Ontology", "Ontology",
                    "http://purl.obolibrary.org/obo/emapa.owl", false ),
            new Endpoint( "Sequence Ontology (SO)", "Ontology",
                    "http://purl.obolibrary.org/obo/so.owl", false ),
            new Endpoint( "Units Ontology (UO)", "Ontology",
                    "http://purl.obolibrary.org/obo/uo.owl", false ),
            new Endpoint( "OBI Ontology", "Ontology",
                    "http://purl.obolibrary.org/obo/obi.owl", false ),
            new Endpoint( "TGEMO (Gemma) ontology", "Ontology",
                    "https://raw.githubusercontent.com/PavlidisLab/TGEMO/master/TGEMO.OWL", false ),

            // --- BioMart / Ensembl ---------------------------------------------------------------
            new Endpoint( "Ensembl GRCh37 biomart martservice", "BioMart",
                    "http://grch37.ensembl.org/biomart/martservice", false ),

            // --- CELLxGENE -----------------------------------------------------------------------
            new Endpoint( "CELLxGENE API collections index", "CELLxGENE",
                    "https://api.cellxgene.cziscience.com/dp/v1/collections/index", false ),
            new Endpoint( "CELLxGENE API datasets index", "CELLxGENE",
                    "https://api.cellxgene.cziscience.com/dp/v1/datasets/index", false ),

            // --- UCSC ----------------------------------------------------------------------------
            new Endpoint( "UCSC Cell Browser", "UCSC",
                    "https://cells.ucsc.edu/", false ),

            // --- Gemma's own self-reference ------------------------------------------------------
            new Endpoint( "Gemma host (PURL)", "Gemma",
                    "https://gemma.msl.ubc.ca/", false )
    );

    @Test
    void probeAll() throws IOException {
        Instant when = Instant.now();

        // Split into eutils (must be serialized to stay under NCBI's 3 req/s anonymous cap) and
        // everything else (safe to probe in parallel).
        List<Endpoint> eutils = ENDPOINTS.stream()
                .filter( Endpoint::isEutils )
                .collect( Collectors.toList() );
        List<Endpoint> rest = ENDPOINTS.stream()
                .filter( e -> !e.isEutils() )
                .collect( Collectors.toList() );

        List<ProbeResult> results = new ArrayList<>( ENDPOINTS.size() );

        // Parallel pass over everything that doesn't rate-limit us.
        ForkJoinPool pool = new ForkJoinPool( Math.min( 16, Math.max( 1, rest.size() ) ) );
        try {
            List<ProbeResult> parallelResults = pool.submit( () ->
                    rest.parallelStream()
                            .map( ExternalUrlReachabilityTest::probe )
                            .collect( Collectors.toList() )
            ).join();
            results.addAll( parallelResults );
        } finally {
            pool.shutdown();
        }

        // Serial pass over NCBI eutils with a sleep between calls so we honour the 3 req/s cap.
        for ( int i = 0; i < eutils.size(); i++ ) {
            if ( i > 0 ) {
                try {
                    Thread.sleep( EUTILS_INTERVAL_MS );
                } catch ( InterruptedException ie ) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            results.add( probe( eutils.get( i ) ) );
        }

        writeJsonReport( when, results );

        long okCount = results.stream().filter( ProbeResult::ok ).count();
        long failCount = results.size() - okCount;
        log.info( "External URL reachability: {}/{} OK, {} failed.",
                okCount, results.size(), failCount );
        for ( ProbeResult r : results ) {
            if ( !r.ok() ) {
                log.warn( "  FAIL [{}] {} -> {} ({}ms)", r.endpoint().category(), r.endpoint().url(), r.error(), r.elapsedMs() );
            }
        }

        // If literally everything failed, the host network is almost certainly down — skip rather
        // than fail so this doesn't pollute a dashboard with false positives.
        Assumptions.assumeTrue( okCount > 0,
                "No external URLs reachable - host network appears down; skipping." );
    }

    private static ProbeResult probe( Endpoint e ) {
        long start = System.currentTimeMillis();
        try {
            if ( e.ftp() ) {
                return probeFtp( e, start );
            } else {
                return probeHttp( e, start );
            }
        } catch ( Exception ex ) {
            return new ProbeResult( e, false, -1, System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName() + ": " + safeMsg( ex.getMessage() ) );
        }
    }

    private static ProbeResult probeHttp( Endpoint e, long start ) throws IOException {
        URL url = URI.create( e.url() ).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout( CONNECT_TIMEOUT_MS );
            conn.setReadTimeout( READ_TIMEOUT_MS );
            conn.setRequestMethod( "HEAD" );
            conn.setInstanceFollowRedirects( true );
            conn.setRequestProperty( "User-Agent", "Gemma-URL-Reachability-Probe/1.0" );
            int status = conn.getResponseCode();
            // Some servers reject HEAD (405) — retry as GET with Range: bytes=0-0 to keep payload small.
            if ( status == 405 || status == 501 ) {
                conn.disconnect();
                HttpURLConnection get = (HttpURLConnection) url.openConnection();
                try {
                    get.setConnectTimeout( CONNECT_TIMEOUT_MS );
                    get.setReadTimeout( READ_TIMEOUT_MS );
                    get.setRequestMethod( "GET" );
                    get.setInstanceFollowRedirects( true );
                    get.setRequestProperty( "User-Agent", "Gemma-URL-Reachability-Probe/1.0" );
                    get.setRequestProperty( "Range", "bytes=0-0" );
                    status = get.getResponseCode();
                } finally {
                    get.disconnect();
                }
            }
            boolean ok = status >= 200 && status < 400;
            return new ProbeResult( e, ok, status, System.currentTimeMillis() - start,
                    ok ? null : "HTTP " + status );
        } finally {
            conn.disconnect();
        }
    }

    private static ProbeResult probeFtp( Endpoint e, long start ) throws IOException {
        URI uri = URI.create( e.url() );
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 21 : uri.getPort();
        String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();

        FTPClient ftp = new FTPClient();
        ftp.setConnectTimeout( CONNECT_TIMEOUT_MS );
        ftp.setDefaultTimeout( CONNECT_TIMEOUT_MS );
        try {
            ftp.connect( host, port );
            ftp.setSoTimeout( READ_TIMEOUT_MS );
            int reply = ftp.getReplyCode();
            if ( !FTPReply.isPositiveCompletion( reply ) ) {
                return new ProbeResult( e, false, reply, System.currentTimeMillis() - start,
                        "FTP connect rejected: " + reply );
            }
            if ( !ftp.login( "anonymous", "gemma-probe@ubic.ca" ) ) {
                return new ProbeResult( e, false, ftp.getReplyCode(), System.currentTimeMillis() - start,
                        "FTP login failed: " + ftp.getReplyString().trim() );
            }
            ftp.enterLocalPassiveMode();
            // Try to STAT the path; STAT works for both files and directories and avoids a data
            // connection. Fall back to CWD for directories if STAT is refused.
            int statReply = ftp.stat( path );
            boolean ok = FTPReply.isPositiveCompletion( statReply ) || FTPReply.isPositiveIntermediate( statReply );
            if ( !ok ) {
                // Try CWD as a directory existence check
                ok = ftp.changeWorkingDirectory( path );
                statReply = ftp.getReplyCode();
            }
            return new ProbeResult( e, ok, statReply, System.currentTimeMillis() - start,
                    ok ? null : "FTP " + statReply + " " + ftp.getReplyString().trim() );
        } finally {
            try {
                if ( ftp.isConnected() ) {
                    ftp.logout();
                    ftp.disconnect();
                }
            } catch ( IOException ignored ) {
                // best effort
            }
        }
    }

    private static void writeJsonReport( Instant when, List<ProbeResult> results ) throws IOException {
        Path target = Paths.get( "target" );
        if ( !Files.exists( target ) ) {
            Files.createDirectories( target );
        }
        Path out = target.resolve( "external-url-reachability.json" );

        Map<String, Object> root = new LinkedHashMap<>();
        root.put( "timestamp", when.toString() );
        root.put( "totalCount", results.size() );
        root.put( "okCount", results.stream().filter( ProbeResult::ok ).count() );
        root.put( "failCount", results.stream().filter( r -> !r.ok() ).count() );

        List<Map<String, Object>> rows = new ArrayList<>( results.size() );
        for ( ProbeResult r : results ) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put( "label", r.endpoint().label() );
            row.put( "category", r.endpoint().category() );
            row.put( "url", r.endpoint().url() );
            row.put( "ftp", r.endpoint().ftp() );
            row.put( "ok", r.ok() );
            row.put( "status", r.status() );
            row.put( "elapsedMs", r.elapsedMs() );
            if ( r.error() != null ) {
                row.put( "error", r.error() );
            }
            rows.add( row );
        }
        root.put( "results", rows );

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable( SerializationFeature.INDENT_OUTPUT );
        mapper.writeValue( out.toFile(), root );
        log.info( "External URL reachability JSON written to {}", out.toAbsolutePath() );
    }

    private static String safeMsg( String msg ) {
        return msg == null ? "<no message>" : msg;
    }

    private record ProbeResult(Endpoint endpoint, boolean ok, int status, long elapsedMs, String error) {}
}
