package ubic.gemma.core.ontology.providers;

import ubic.gemma.core.config.Configuration;
import ubic.gemma.core.ontology.lexical.AbstractLexicalOntologyService;
import ubic.gemma.core.ontology.lexical.LexicalTerm;
import ubic.gemma.core.ontology.model.OntologyResource;
import ubic.gemma.core.ontology.model.OntologyTerm;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MeSH entry terms served as extra <em>search strings</em> for MONDO disease terms.
 * <p>
 * The OBO disease ontologies publish formal nomenclature ("malignant neoplasm of larynx"); MeSH
 * entry terms are the phrasing clinicians and authors actually write ("non-insulin-dependent
 * diabetes mellitus", "Gorlin Goltz syndrome"). A disease named from a GEO series title therefore
 * often fails to ground even though the concept is present in MONDO. This service closes that gap
 * by indexing 8,158 MeSH strings that MONDO lacks, under the MONDO URI they belong to.
 * <p>
 * <b>This service mints no URIs of its own.</b> Every term it serves is a MONDO term; it only
 * contributes additional ways to spell one. That is why {@link #getTerm(String)} and
 * {@link #getAllURIs()} deliberately answer as if empty — see below.
 *
 * <h2>The table is precomputed, not derived at runtime</h2>
 * The MeSH-to-MONDO join needs all of MeSH, mondo.obo and mondo.sssom.tsv (roughly 80 MB of
 * source) to produce well under a megabyte of output, and the join rules are where all the risk
 * lives. Both facts argue for doing it once, offline, and reviewing the diff:
 * {@code scripts/build_mesh_disease_synonyms.py} builds
 * {@code mesh-disease-synonyms.tsv} with a provenance header and a {@code _meta.json} sidecar
 * carrying source versions and sha256. That script's {@code --check} mode reports when MONDO or
 * MeSH have moved on, which is the hook for a scheduled refresh.
 *
 * <h2>Why the synonyms here are safe to index</h2>
 * Only MONDO-asserted joins are used (MONDO's own {@code MESH:} xrefs plus its SSSOM
 * {@code skos:exactMatch} rows), restricted to the MeSH disease branch (tree numbers {@code C*}
 * and {@code F03}), and any MeSH descriptor resolving to more than one live MONDO term is dropped
 * rather than guessed at. The route through DOID's {@code MESH:} xrefs is excluded: it is 8.9%
 * ambiguous, and for the laryngeal case it offers both "larynx cancer" and "benign laryngeal
 * neoplasm" — a wrong-branch match is worse than no match.
 * <p>
 * Only each descriptor's <em>preferred</em> MeSH concept contributes. MeSH states every other
 * concept's relation to it, and across the disease branch those are 3,859 narrower, 723 related and
 * 139 broader — never an exact synonym. So a non-preferred concept's terms are always the wrong
 * breadth for the MONDO term the descriptor maps to. This is also why "laryngeal cancer" is
 * <em>not</em> here: MeSH marks it narrower than the "Laryngeal Neoplasms" heading that MONDO maps
 * to MONDO:0021071, and indexing it would ground a cancer to its benign-or-malignant parent.
 * Reaching MONDO:0002352 needs a concept-level mapping, which is a separate table.
 *
 * <h2>Ranking</h2>
 * Inherited {@link #isSupplementary()} keeps these hits below every conventional-ontology hit, and
 * {@code OntologyServiceImpl.findTerms} dedupes them on the term. A URI MONDO itself already
 * returned is therefore never re-added from here: this source can only fill a gap, never displace
 * or outrank the real MONDO term.
 * <p>
 * Honors {@code load.meshDiseaseSynonyms} in {@code basecode.properties}; disabled by default. The
 * table is a classpath resource, so enabling it costs no download.
 */
public class MeshDiseaseSynonymOntologyService extends AbstractLexicalOntologyService {

    private static final String NAME = "MeSH Disease Synonyms";
    private static final String CACHE_NAME = "meshDiseaseSynonyms";

    /** Every URI served here is a MONDO term; nothing else is admitted. */
    public static final String URI_PREFIX = "http://purl.obolibrary.org/obo/MONDO_";

    private static final String RESOURCE = "/ubic/gemma/core/ontology/mesh-disease-synonyms.tsv";

    public MeshDiseaseSynonymOntologyService() {
        super( NAME, CACHE_NAME, RESOURCE,
                Boolean.TRUE.equals( Configuration.getBoolean( "load." + CACHE_NAME ) ) );
    }

    @Override
    protected InputStream openSource( boolean forceReload ) throws IOException {
        InputStream is = MeshDiseaseSynonymOntologyService.class.getResourceAsStream( RESOURCE );
        if ( is == null ) {
            throw new IOException( "The MeSH disease synonym table is missing from the classpath at " + RESOURCE
                    + "; rebuild it with scripts/build_mesh_disease_synonyms.py." );
        }
        return is;
    }

    /**
     * Read the precomputed table, grouping its rows into one term per MONDO URI.
     * <p>
     * Rows are {@code mondo_uri, mondo_label, synonym, mesh_id}, sorted by URI, behind {@code #}
     * provenance comments and a header line. The MONDO source release is lifted out of the
     * {@code # mondo:} comment so {@code /admin/ontologies} reports which release the strings were
     * joined against — the table is only as current as that.
     */
    @Override
    protected Collection<LexicalTerm> parse( InputStream is ) throws IOException {
        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, List<String>> synonyms = new LinkedHashMap<>();
        String builtOn = null;
        String mondoRelease = null;
        int rows = 0;
        try ( BufferedReader r = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
            String line;
            boolean headerSeen = false;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.startsWith( "#" ) ) {
                    if ( line.startsWith( "# mondo:" ) ) {
                        mondoRelease = line.substring( "# mondo:".length() ).trim();
                    } else if ( line.startsWith( "# built:" ) ) {
                        builtOn = line.substring( "# built:".length() ).trim();
                    }
                    continue;
                }
                if ( line.isEmpty() ) {
                    continue;
                }
                if ( !headerSeen ) {
                    // the column header; everything after it is data
                    headerSeen = true;
                    continue;
                }
                String[] f = line.split( "\t", -1 );
                if ( f.length < 3 ) {
                    continue;
                }
                String uri = f[0].trim();
                String label = f[1].trim();
                String synonym = f[2].trim();
                if ( uri.isEmpty() || synonym.isEmpty() ) {
                    continue;
                }
                labels.putIfAbsent( uri, label.isEmpty() ? null : label );
                synonyms.computeIfAbsent( uri, k -> new ArrayList<>() ).add( synonym );
                rows++;
            }
        }
        List<LexicalTerm> terms = new ArrayList<>( labels.size() );
        for ( Map.Entry<String, String> e : labels.entrySet() ) {
            terms.add( new LexicalTerm( e.getKey(), e.getValue(),
                    synonyms.getOrDefault( e.getKey(), Collections.emptyList() ) ) );
        }
        this.version = mondoRelease != null
                ? mondoRelease + ( builtOn != null ? " (table built " + builtOn + ")" : "" )
                : builtOn;
        log.info( "Parsed {} MeSH synonyms over {} MONDO terms.", rows, terms.size() );
        return terms;
    }

    /**
     * Always {@code null}: MONDO owns these URIs, and this service must not answer for them.
     * <p>
     * {@code OntologyServiceImpl.getTerm} takes the first non-null answer across all services and
     * {@code getTerms} unions them, so answering here would let a term carrying only a label and no
     * definition, parents or children stand in for — or duplicate — the real MONDO term. Search is
     * unaffected: {@code findTerm} rehydrates hits from this service's own state, not through this
     * method.
     */
    @Nullable
    @Override
    public OntologyTerm getTerm( String uri ) {
        return null;
    }

    @Nullable
    @Override
    public OntologyResource getResource( String uri ) {
        return null;
    }

    /** Empty for the same reason as {@link #getTerm(String)} — these URIs are MONDO's to enumerate. */
    @Override
    public Set<String> getAllURIs() {
        return Collections.emptySet();
    }
}
