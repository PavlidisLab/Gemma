package ubic.gemma.core.ontology.providers;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves user-supplied text (a REST path segment, a CLI argument) to an {@link OntologyService}.
 * <p>
 * {@link OntologyService#getName()} alone is not usable for this: it reads {@code dc:title} out of the
 * loaded model, so it is {@code null} for the ontologies that don't declare one (TGEMO, CHEBI, EFO, OBI,
 * SO, CLO, the unified TDB) and, where it is declared, it contains spaces — "PATO - the Phenotype And
 * Trait Ontology" is not something anyone wants to type or put in a URL. This resolver instead accepts
 * any of an ontology's spellings:
 * <ol>
 * <li>its well-known abbreviation — {@code CLO}, {@code HPO}, {@code TGEMO}, {@code GO}, …</li>
 * <li>its {@link OntologyService#getIdentifier() identifier} (the cache name, e.g. {@code cellLineOntology})</li>
 * <li>its implementing class name, full or with the {@code OntologyService} suffix dropped
 *     ({@code CellLineOntologyService}, {@code CellLine})</li>
 * <li>its {@code dc:title}, when the ontology is loaded and declares one</li>
 * </ol>
 * Matching ignores case, whitespace and punctuation, so {@code cell-line-ontology}, {@code Cell Line
 * Ontology} and {@code cellLineOntology} are all the same key.
 *
 * @author Gemma
 */
public class OntologyServiceResolver {

    /**
     * Well-known abbreviations, keyed by {@link OntologyService#getIdentifier()} (i.e. the cache name).
     * <p>
     * These are the OBO id-spaces / community names people actually use; they are not derivable from the
     * cache name or the class name, which is why they are listed here rather than computed. An ontology
     * missing from this table is still resolvable by identifier, class name or {@code dc:title}.
     */
    private static final Map<String, List<String>> ABBREVIATIONS;

    static {
        Map<String, List<String>> m = new HashMap<>();
        m.put( "gemmaOntology", Collections.singletonList( "TGEMO" ) );
        // NB the key is the cache name, which is not always the property suffix: EFO is configured under
        // url.efOntology but caches as experimentalFactorOntology. GET /admin/ontologies lists the
        // identifier each service actually reports — check there when adding a row.
        m.put( "experimentalFactorOntology", Collections.singletonList( "EFO" ) );
        m.put( "geneOntology", Collections.singletonList( "GO" ) );
        m.put( "cellTypeOntology", Collections.singletonList( "CL" ) );
        m.put( "cellLineOntology", Collections.singletonList( "CLO" ) );
        m.put( "cellosaurus", Collections.singletonList( "CVCL" ) );
        m.put( "chebiOntology", Collections.singletonList( "CHEBI" ) );
        m.put( "mondoOntology", Collections.singletonList( "MONDO" ) );
        m.put( "uberonOntology", Collections.singletonList( "UBERON" ) );
        m.put( "patoOntology", Collections.singletonList( "PATO" ) );
        m.put( "obiOntology", Collections.singletonList( "OBI" ) );
        m.put( "seqOntology", Collections.singletonList( "SO" ) );
        m.put( "unitsOntology", Collections.singletonList( "UO" ) );
        m.put( "medicOntology", Collections.singletonList( "MEDIC" ) );
        m.put( "mgiStrain", Collections.singletonList( "MGI" ) );
        m.put( "humanPhenotypeOntology", Arrays.asList( "HPO", "HP" ) );
        m.put( "mammalPhenotypeOntology", Arrays.asList( "MPO", "MP" ) );
        m.put( "mouseDevelOntology", Collections.singletonList( "EMAPA" ) );
        m.put( "unified", Collections.singletonList( "TDB" ) );
        ABBREVIATIONS = Collections.unmodifiableMap( m );
    }

    /**
     * Resolve a user-supplied token to one of the given ontologies.
     * <p>
     * Identifiers, abbreviations and class names are matched first, across every ontology, so a
     * {@code dc:title} can never shadow another ontology's identifier.
     *
     * @return the matching ontology, or empty if nothing matches
     */
    public static Optional<OntologyService> resolve( Collection<OntologyService> ontologies, @Nullable String token ) {
        String key = normalize( token );
        if ( key.isEmpty() ) {
            return Optional.empty();
        }
        for ( OntologyService o : ontologies ) {
            for ( String candidate : primaryNames( o ) ) {
                if ( key.equals( normalize( candidate ) ) ) {
                    return Optional.of( o );
                }
            }
        }
        // second pass: dc:title, which is only available once the model is loaded
        for ( OntologyService o : ontologies ) {
            if ( key.equals( normalize( title( o ) ) ) ) {
                return Optional.of( o );
            }
        }
        return Optional.empty();
    }

    /**
     * Obtain every spelling {@link #resolve(Collection, String)} accepts for an ontology, most
     * human-friendly first. Suitable for showing in a "refresh by" listing.
     */
    public static Set<String> getNames( OntologyService ontology ) {
        Set<String> names = new LinkedHashSet<>( primaryNames( ontology ) );
        String title = title( ontology );
        if ( title != null && !title.isEmpty() ) {
            names.add( title );
        }
        return names;
    }

    /**
     * The abbreviation to lead with when naming an ontology to a human, falling back to the identifier.
     */
    public static String getPreferredName( OntologyService ontology ) {
        return primaryNames( ontology ).iterator().next();
    }

    /**
     * Names that don't require the ontology to be loaded: abbreviation, identifier, class names.
     */
    private static Set<String> primaryNames( OntologyService ontology ) {
        Set<String> names = new LinkedHashSet<>();
        String identifier = null;
        try {
            identifier = ontology.getIdentifier();
        } catch ( RuntimeException ignored ) {
            // a bean that can't even name itself is not resolvable by identifier; fall through to the class name
        }
        if ( identifier != null && !identifier.isEmpty() ) {
            names.addAll( ABBREVIATIONS.getOrDefault( identifier, Collections.emptyList() ) );
            names.add( identifier );
        }
        String className = getClassName( ontology );
        names.add( className );
        String shortName = className.replaceFirst( "(Ontology)?Service$", "" );
        if ( !shortName.isEmpty() ) {
            names.add( shortName );
        }
        return names;
    }

    /**
     * Simple name of the deepest non-synthetic class, so Spring CGLIB and Mockito proxy subclasses
     * ({@code ChebiOntologyService$$EnhancerBySpringCGLIB$$…}) still match on the real class name.
     */
    private static String getClassName( OntologyService ontology ) {
        Class<?> c = ontology.getClass();
        while ( c != null && c.getSimpleName().contains( "$" ) ) {
            c = c.getSuperclass();
        }
        return c == null ? "" : c.getSimpleName();
    }

    @Nullable
    private static String title( OntologyService ontology ) {
        try {
            return ontology.getName();
        } catch ( RuntimeException e ) {
            // getName() reads the loaded model; a bean in a bad state must not break resolution of the others
            return null;
        }
    }

    private static String normalize( @Nullable String s ) {
        return s == null ? "" : s.toLowerCase().replaceAll( "[^a-z0-9]", "" );
    }
}
