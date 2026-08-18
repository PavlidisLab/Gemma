package ubic.gemma.core.ontology.providers;

import ubic.gemma.core.config.Configuration;
import ubic.gemma.core.ontology.jena.OntologyLoader;
import ubic.gemma.core.ontology.lexical.AbstractLexicalOntologyService;
import ubic.gemma.core.ontology.lexical.LexicalTerm;
import ubic.gemma.core.ontology.lexical.LexicalTermMetadata;

import javax.annotation.Nullable;

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
 * that OBO directly (per the "no OWL, only OBO" constraint). Emitted URIs use the canonical resolvable form
 * {@code https://www.cellosaurus.org/CVCL_<id>} (the OBO PURL form 404s, so it is not minted).
 * <p>
 * Honors {@code url.cellosaurus} and {@code load.cellosaurus} in {@code basecode.properties}. Disabled
 * by default; enabled on the production instance.
 * <p>
 * 🛑 <b>No species filtering.</b> Every cell line is indexed regardless of taxon — human, mouse, rat,
 * zebrafish, insect, plant. Restricting the catalogue to the species Gemma currently cares about was
 * considered and rejected: the scope of the project changes, a filter baked in here would silently drop
 * hits the day it widened, and the failure would look like "the resolver cannot find this cell line"
 * rather than like a policy. Instead each term reports its species (see {@link LexicalTermMetadata}) and
 * the CALLER decides what is in scope, with the NCBI taxon id in hand to do it precisely.
 * <p>
 * Per-term metadata carried out of the OBO: species (multi-valued — hybridomas derive from two organisms),
 * cell-line type and donor sex (both live in {@code subset:} and are split apart here), the free-text
 * {@code comment:}, and the {@code Problematic cell line:} flag Cellosaurus uses to mark misidentified or
 * contaminated lines. That last one is advisory information for a curator, NOT something to annotate an
 * experiment with.
 * <p>
 * <b>Memory:</b> retaining the comments costs roughly 55 MB of resident heap — they are ~46% of the
 * 118 MB OBO by bytes. That is a deliberate, measured trade for making every cell-line hit
 * self-describing, and it is why this service stays opt-in.
 */
public class CellosaurusOntologyService extends AbstractLexicalOntologyService {

    private static final String NAME = "Cellosaurus";
    private static final String CACHE_NAME = "cellosaurus";

    /** Canonical, resolvable Cellosaurus URI prefix; {@code <prefix>CVCL_1234}. */
    public static final String URI_PREFIX = "https://www.cellosaurus.org/";

    private static final Pattern SYNONYM = Pattern.compile( "\"([^\"]*)\"" );

    /** {@code xref: NCBI_TaxID:10090 ! Mus musculus (Mouse)} — the only place the OBO states species. */
    private static final Pattern TAXID_XREF =
            Pattern.compile( "^xref:\\s*NCBI_TaxID:(\\d+)\\s*(?:!\\s*(.*))?$" );

    /**
     * {@code comment: "Problematic cell line: Misidentified/contaminated. ..."} — Cellosaurus records the
     * reason inline in the free-text comment, so the flag has to be read out of it rather than off a field
     * of its own.
     */
    private static final Pattern PROBLEMATIC =
            Pattern.compile( "Problematic cell line:\\s*([^.]*)\\." );

    /**
     * The {@code subset:} field carries TWO unrelated axes — what kind of cell line it is, and the sex of
     * the donor. Splitting them is the whole point: "Female" is not a cell-line type, and a client reading
     * a single {@code subset} list has to know Cellosaurus's vocabulary to tell which is which.
     * <p>
     * These are the sex values declared by {@code subsetdef:} in the header; everything else declared there
     * is a cell-line type. Matching against the closed sex list rather than a closed type list is
     * deliberate — Cellosaurus adds cell-line types between releases (there are 14 today), and an unknown
     * new one should surface as a type rather than be silently dropped.
     */
    private static final java.util.Set<String> SEX_SUBSETS = java.util.Set.of(
            "Female", "Male", "Mixed_sex", "Sex_ambiguous", "Sex_unspecified" );

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
     * Stream-parse a Cellosaurus OBO into lexical terms. Keeps {@code id}, {@code name}, each
     * {@code synonym}, and the descriptive metadata ({@code xref: NCBI_TaxID}, {@code subset:},
     * {@code comment:}) per {@code [Term]}; skips {@code [Typedef]} stanzas and obsolete terms. Tolerates
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
            List<LexicalTermMetadata.Taxon> species = new ArrayList<>();
            List<String> types = new ArrayList<>();
            String sex = null;
            String comment = null;
            String line;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.startsWith( "[" ) ) {
                    if ( inTerm ) {
                        addTerm( terms, id, label, synonyms, obsolete, species, types, sex, comment );
                    }
                    inTerm = line.startsWith( "[Term]" );
                    id = null;
                    label = null;
                    synonyms = new ArrayList<>();
                    obsolete = false;
                    species = new ArrayList<>();
                    types = new ArrayList<>();
                    sex = null;
                    comment = null;
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
                } else if ( line.startsWith( "xref:" ) ) {
                    Matcher m = TAXID_XREF.matcher( line.trim() );
                    if ( m.matches() ) {
                        String taxLabel = m.group( 2 ) != null ? m.group( 2 ).trim() : null;
                        species.add( new LexicalTermMetadata.Taxon( Integer.parseInt( m.group( 1 ) ),
                                taxLabel == null || taxLabel.isEmpty() ? null : taxLabel ) );
                    }
                } else if ( line.startsWith( "subset:" ) ) {
                    String s = line.substring( "subset:".length() ).trim();
                    if ( s.isEmpty() ) {
                        continue;
                    }
                    if ( SEX_SUBSETS.contains( s ) ) {
                        sex = s.replace( '_', ' ' );
                    } else {
                        types.add( s.replace( '_', ' ' ) );
                    }
                } else if ( line.startsWith( "comment:" ) ) {
                    Matcher m = SYNONYM.matcher( line );
                    comment = m.find() ? m.group( 1 ) : line.substring( "comment:".length() ).trim();
                }
            }
            if ( inTerm ) {
                addTerm( terms, id, label, synonyms, obsolete, species, types, sex, comment );
            }
        }
        log.info( "Parsed {} Cellosaurus cell-line terms.", terms.size() );
        return terms;
    }

    private static void addTerm( List<LexicalTerm> terms, String id, String label, List<String> synonyms,
            boolean obsolete, List<LexicalTermMetadata.Taxon> species, List<String> types,
            @Nullable String sex, @Nullable String comment ) {
        if ( id == null || obsolete || !id.startsWith( "CVCL_" ) ) {
            return;
        }
        String problematic = null;
        if ( comment != null ) {
            Matcher m = PROBLEMATIC.matcher( comment );
            if ( m.find() ) {
                problematic = m.group( 1 ).trim();
            }
        }
        // An entry can declare more than one type subset; join rather than pick, so nothing is invented.
        String cellLineType = types.isEmpty() ? null : String.join( "; ", types );
        LexicalTermMetadata meta = new LexicalTermMetadata( species, cellLineType, sex, null, comment, problematic );
        terms.add( new LexicalTerm( URI_PREFIX + id, label, synonyms, meta ) );
    }
}
