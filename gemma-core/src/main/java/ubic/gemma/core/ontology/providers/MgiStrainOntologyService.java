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

/**
 * <a href="https://www.informatics.jax.org/">MGI</a> (Mouse Genome Informatics) mouse strains as a
 * flat lexical strain name-resolution source, to annotate the complicated mouse strains/genotypes
 * that EFO/TGEMO don't enumerate (the ~84k coisogenic + ~17k congenic mutant strains).
 * <p>
 * MGI is not an ontology and ships these as tab-delimited reports, not OBO/OWL. This provider parses
 * {@code MGI_Strain.rpt} directly (columns: {@code MGI:<id>} · strain nomenclature · strain type) into
 * the shared {@link AbstractLexicalOntologyService} Lucene index — the same pattern as Cellosaurus.
 * Emitted URIs use the canonical resolvable form {@code https://www.informatics.jax.org/strain/MGI:<id>}
 * (the OBO PURL form 404s). {@code MGI_Strain.rpt} carries no synonyms, so matching is nomenclature-only
 * for now (a synonym source — a strain-synonym report or MouseMine — is a documented follow-up).
 * <p>
 * Honors {@code url.mgiStrain} and {@code load.mgiStrain} in {@code basecode.properties}. Disabled by
 * default; enabled on the production instance.
 */
public class MgiStrainOntologyService extends AbstractLexicalOntologyService {

    private static final String NAME = "MGI";
    private static final String CACHE_NAME = "mgiStrain";

    /** Canonical, resolvable MGI strain URI prefix; {@code <prefix>MGI:2160170}. */
    public static final String URI_PREFIX = "https://www.informatics.jax.org/strain/";

    public MgiStrainOntologyService() {
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
            log.info( "No ontology cache dir configured; streaming MGI_Strain.rpt directly from {}.", url );
            return URI.create( url ).toURL().openStream();
        }
        if ( !forceReload && cache.isFile() && cache.length() > 0 ) {
            log.info( "Using cached MGI_Strain.rpt at {} ({} bytes).", cache, cache.length() );
            return new FileInputStream( cache );
        }
        File parent = cache.getParentFile();
        if ( parent != null ) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        log.info( "Downloading MGI_Strain.rpt from {} to {} ...", url, cache );
        try ( InputStream in = URI.create( url ).toURL().openStream() ) {
            Files.copy( in, cache.toPath(), StandardCopyOption.REPLACE_EXISTING );
        }
        return new FileInputStream( cache );
    }

    /**
     * Parse {@code MGI_Strain.rpt}: one strain per line, tab-delimited, with the accession
     * ({@code MGI:<id>}) in column 1 and the strain nomenclature in column 2. Skips {@code #} comment
     * lines and rows without an {@code MGI:} accession or a name.
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
                int tab1 = line.indexOf( '\t' );
                if ( tab1 <= 0 ) {
                    continue;
                }
                String id = line.substring( 0, tab1 ).trim();
                if ( !id.startsWith( "MGI:" ) ) {
                    continue;
                }
                int tab2 = line.indexOf( '\t', tab1 + 1 );
                String name = ( tab2 < 0 ? line.substring( tab1 + 1 ) : line.substring( tab1 + 1, tab2 ) ).trim();
                if ( name.isEmpty() ) {
                    continue;
                }
                terms.add( new LexicalTerm( URI_PREFIX + id, name ) );
            }
        }
        log.info( "Parsed {} MGI strain terms.", terms.size() );
        return terms;
    }
}
