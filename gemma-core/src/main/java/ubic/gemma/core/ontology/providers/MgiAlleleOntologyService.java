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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <a href="https://www.informatics.jax.org/">MGI</a> mouse alleles as a flat lexical source, so an annotation can
 * carry the allele that MGI's disease-model relations are keyed on.
 * <p>
 * {@link ubic.gemma.core.ontology.relation.MgiRelationProducer} stores MGI's allele → disease relations under
 * {@code https://www.informatics.jax.org/allele/MGI:<id>}, and a relation reaches an experiment only through an
 * annotation VALUE carrying its subject's URI. No term with that URI existed to annotate with: cab measured 5,981
 * allele subjects and 0 annotated experiments on 2026-09-14. Paul, 2026-09-14: the allele is curated as the
 * annotation value.
 * <p>
 * Parses {@code MGI_PhenotypicAllele.rpt}, which covered 5,896 of the 5,904 alleles MGI's disease reports name, out
 * of 141,486 alleles (measured 2026-09-14). The label is the allele symbol ({@code Smn1<tm5(Smn1/SMN2)Mrph>}); the
 * allele name and MGI's synonym column ({@code Smn allele C}) are indexed as synonyms. Supplementary, like
 * {@link MgiStrainOntologyService}: ranked below the conventional ontologies.
 * <p>
 * Honors {@code url.mgiAllele} and {@code load.mgiAllele} in {@code basecode.properties}. Disabled by default.
 */
public class MgiAlleleOntologyService extends AbstractLexicalOntologyService {

    private static final String NAME = "MGI alleles";
    private static final String CACHE_NAME = "mgiAllele";

    /** The report is of mouse alleles and has no species column; see {@link MgiStrainOntologyService} on the same inference. */
    private static final List<LexicalTermMetadata.Taxon> MOUSE =
            List.of( new LexicalTermMetadata.Taxon( 10090, "Mus musculus (Mouse)" ) );

    /** Canonical, resolvable MGI allele URI prefix; {@code <prefix>MGI:3794202}. The OBO PURL form 404s. */
    public static final String URI_PREFIX = "https://www.informatics.jax.org/allele/";

    // column positions in MGI_PhenotypicAllele.rpt, 0-based
    private static final int ID = 0, SYMBOL = 1, NAME_COLUMN = 2, TYPE = 3, ATTRIBUTE = 4, MARKER_ID = 6,
            MARKER_SYMBOL = 7, SYNONYMS = 11;

    public MgiAlleleOntologyService() {
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
            log.info( "No ontology cache dir configured; streaming MGI_PhenotypicAllele.rpt directly from {}.", url );
            return URI.create( url ).toURL().openStream();
        }
        if ( !forceReload && cache.isFile() && cache.length() > 0 ) {
            log.info( "Using cached MGI_PhenotypicAllele.rpt at {} ({} bytes).", cache, cache.length() );
            return new FileInputStream( cache );
        }
        File parent = cache.getParentFile();
        if ( parent != null ) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        log.info( "Downloading MGI_PhenotypicAllele.rpt from {} to {} ...", url, cache );
        try ( InputStream in = URI.create( url ).toURL().openStream() ) {
            Files.copy( in, cache.toPath(), StandardCopyOption.REPLACE_EXISTING );
        }
        return new FileInputStream( cache );
    }

    /**
     * Parse {@code MGI_PhenotypicAllele.rpt}: one allele per line, tab-delimited, accession in column 1, symbol in
     * column 2, name in column 3, allele type and attributes in 4 and 5, the gene's accession and symbol in 7 and 8
     * (empty for a transgene), and {@code |}-separated synonyms in 12. Skips {@code #} comment lines and rows without
     * an {@code MGI:} accession or a symbol.
     */
    @Override
    protected Collection<LexicalTerm> parse( InputStream is ) throws IOException {
        List<LexicalTerm> terms = new ArrayList<>();
        try ( BufferedReader r = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
            String line;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.isEmpty() || line.charAt( 0 ) == '#' ) {
                    continue;
                }
                String[] f = line.split( "\t", -1 );
                String id = cell( f, ID );
                String symbol = cell( f, SYMBOL );
                if ( id == null || !id.startsWith( "MGI:" ) || symbol == null ) {
                    continue;
                }
                Set<String> synonyms = new LinkedHashSet<>();
                String name = cell( f, NAME_COLUMN );
                if ( name != null ) {
                    synonyms.add( name );
                }
                String synonymCell = cell( f, SYNONYMS );
                if ( synonymCell != null ) {
                    for ( String s : synonymCell.split( "\\|" ) ) {
                        if ( !s.isBlank() ) {
                            synonyms.add( s.trim() );
                        }
                    }
                }
                synonyms.remove( symbol );
                terms.add( new LexicalTerm( URI_PREFIX + id, symbol, new ArrayList<>( synonyms ),
                        new LexicalTermMetadata( MOUSE, null, null, null, describe( f ), null ) ) );
            }
        }
        log.info( "Parsed {} MGI allele terms.", terms.size() );
        return terms;
    }

    /** e.g. {@code Targeted; Null/knockout; allele of Smn1 (MGI:98357)}, or null when the row says nothing. */
    @Nullable
    private static String describe( String[] f ) {
        List<String> parts = new ArrayList<>();
        String type = cell( f, TYPE );
        if ( type != null ) {
            parts.add( type );
        }
        String attribute = cell( f, ATTRIBUTE );
        if ( attribute != null ) {
            parts.add( attribute );
        }
        String markerSymbol = cell( f, MARKER_SYMBOL );
        if ( markerSymbol != null ) {
            String markerId = cell( f, MARKER_ID );
            parts.add( "allele of " + markerSymbol + ( markerId != null ? " (" + markerId + ")" : "" ) );
        }
        return parts.isEmpty() ? null : String.join( "; ", parts );
    }

    @Nullable
    private static String cell( String[] f, int i ) {
        if ( i >= f.length ) {
            return null;
        }
        String s = f[i].trim();
        return s.isEmpty() ? null : s;
    }
}
