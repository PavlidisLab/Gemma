package ubic.gemma.core.ontology.providers;

import ubic.gemma.core.config.Configuration;
import ubic.gemma.core.ontology.jena.OntologyLoader;
import ubic.gemma.core.ontology.lexical.AbstractLexicalOntologyService;
import ubic.gemma.core.ontology.lexical.LexicalTerm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <a href="https://www.cellosaurus.org/">Cellosaurus</a> served as a flat lexical cell-line
 * name-resolution source, as a backup for the Cell Line Ontology (CLO) in cell-line searches.
 * <p>
 * Cellosaurus is not an ontology — it is a ~169k-entry catalogue of cell lines with dense synonyms
 * and no subsumption hierarchy — so it is loaded via {@link AbstractLexicalOntologyService} (a Lucene
 * lexical index) rather than as a Jena {@code OntModel}. It is distributed as OBO; this parser reads
 * that OBO directly (per the "no OWL, only OBO" constraint), keeping only {@code id} / {@code name} /
 * {@code synonym} per {@code [Term]}. Emitted URIs use the canonical resolvable form
 * {@code https://www.cellosaurus.org/CVCL_<id>} (the OBO PURL form 404s, so it is not minted).
 * <p>
 * Honors {@code url.cellosaurus} and {@code load.cellosaurus} in {@code basecode.properties}. Disabled
 * by default; enabled on the production instance.
 */
public class CellosaurusOntologyService extends AbstractLexicalOntologyService {

    private static final String NAME = "Cellosaurus";
    private static final String CACHE_NAME = "cellosaurus";

    /** Canonical, resolvable Cellosaurus URI prefix; {@code <prefix>CVCL_1234}. */
    public static final String URI_PREFIX = "https://www.cellosaurus.org/";

    private static final Pattern SYNONYM = Pattern.compile( "\"([^\"]*)\"" );

    public CellosaurusOntologyService() {
        super( NAME, CACHE_NAME,
                Configuration.getString( "url." + CACHE_NAME ),
                Boolean.TRUE.equals( Configuration.getBoolean( "load." + CACHE_NAME ) ) );
    }

    @Override
    protected InputStream openSource( boolean forceReload ) throws IOException {
        String url = getUrl();
        if ( url == null ) {
            throw new IOException( "No url." + CACHE_NAME + " configured." );
        }
        File cache = OntologyLoader.getDiskCachePath( CACHE_NAME );
        if ( cache == null ) {
            log.info( "No ontology cache dir configured; streaming Cellosaurus OBO directly from {}.", url );
            return URI.create( url ).toURL().openStream();
        }
        if ( !forceReload && cache.isFile() && cache.length() > 0 ) {
            log.info( "Using cached Cellosaurus OBO at {} ({} bytes).", cache, cache.length() );
            return new FileInputStream( cache );
        }
        File parent = cache.getParentFile();
        if ( parent != null ) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        log.info( "Downloading Cellosaurus OBO from {} to {} ...", url, cache );
        try ( InputStream in = URI.create( url ).toURL().openStream() ) {
            Files.copy( in, cache.toPath(), StandardCopyOption.REPLACE_EXISTING );
        }
        return new FileInputStream( cache );
    }

    /**
     * Stream-parse a Cellosaurus OBO into lexical terms. Keeps {@code id}, {@code name} and each
     * {@code synonym} per {@code [Term]}; skips {@code [Typedef]} stanzas and obsolete terms. Tolerates
     * Cellosaurus's {@code name:} written with no space after the colon.
     */
    @Override
    protected Collection<LexicalTerm> parse( InputStream is ) throws IOException {
        List<LexicalTerm> terms = new ArrayList<>();
        try ( BufferedReader r = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
            boolean inTerm = false;
            String id = null;
            String label = null;
            List<String> synonyms = new ArrayList<>();
            boolean obsolete = false;
            String line;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.startsWith( "[" ) ) {
                    if ( inTerm ) {
                        addTerm( terms, id, label, synonyms, obsolete );
                    }
                    inTerm = line.startsWith( "[Term]" );
                    id = null;
                    label = null;
                    synonyms = new ArrayList<>();
                    obsolete = false;
                    continue;
                }
                if ( !inTerm ) {
                    if ( line.startsWith( "data-version:" ) ) {
                        this.version = line.substring( "data-version:".length() ).trim();
                    }
                    continue;
                }
                if ( line.startsWith( "id:" ) ) {
                    id = line.substring( "id:".length() ).trim();
                } else if ( line.startsWith( "name:" ) ) {
                    label = line.substring( "name:".length() ).trim();
                } else if ( line.startsWith( "synonym:" ) ) {
                    Matcher m = SYNONYM.matcher( line );
                    if ( m.find() ) {
                        synonyms.add( m.group( 1 ) );
                    }
                } else if ( line.startsWith( "is_obsolete:" ) && line.contains( "true" ) ) {
                    obsolete = true;
                }
            }
            if ( inTerm ) {
                addTerm( terms, id, label, synonyms, obsolete );
            }
        }
        log.info( "Parsed {} Cellosaurus cell-line terms.", terms.size() );
        return terms;
    }

    private static void addTerm( List<LexicalTerm> terms, String id, String label, List<String> synonyms, boolean obsolete ) {
        if ( id == null || obsolete || !id.startsWith( "CVCL_" ) ) {
            return;
        }
        terms.add( new LexicalTerm( URI_PREFIX + id, label, synonyms ) );
    }
}
