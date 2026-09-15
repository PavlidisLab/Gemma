package ubic.gemma.core.ontology.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OntologyXref#normalizeCurie(String)} is the single point every cross-reference passes through
 * on its way into an index, so what it drops is dropped everywhere and silently.
 *
 * @author Gemma
 */
class OntologyXrefTest {

    /**
     * 🛑 MGI publishes its identifiers as the last path segment in CURIE form
     * ({@code .../allele/MGI:3524957}) rather than the OBO underscore form, and TGEMO cross-references
     * them in exactly that shape. This returned null, so those cross-references were dropped without
     * trace -- the term simply appeared to have none.
     */
    @Test
    void aUrlWhoseLastSegmentIsAlreadyACurieIsNormalized() {
        assertThat( OntologyXref.normalizeCurie( "https://www.informatics.jax.org/allele/MGI:3524957" ) )
                .isEqualTo( "MGI:3524957" );
        assertThat( OntologyXref.normalizeCurie( "https://www.informatics.jax.org/strain/MGI:3611279" ) )
                .isEqualTo( "MGI:3611279" );
        assertThat( OntologyXref.normalizeCurie( "https://www.informatics.jax.org/marker/MGI:4461397" ) )
                .isEqualTo( "MGI:4461397" );
    }

    /** The OBO form still wins, and it is the one that must not change. */
    @Test
    void theOboUnderscoreFormIsUnchanged() {
        assertThat( OntologyXref.normalizeCurie( "http://purl.obolibrary.org/obo/MONDO_0004975" ) )
                .isEqualTo( "MONDO:0004975" );
        assertThat( OntologyXref.normalizeCurie( "http://purl.obolibrary.org/obo/DOID_10652" ) )
                .isEqualTo( "DOID:10652" );
    }

    /** A bare CURIE, which is what the flat reports carry, is passed through with the prefix upper-cased. */
    @Test
    void aBareCurieIsUpperCasedAndKept() {
        assertThat( OntologyXref.normalizeCurie( "doid:1206" ) ).isEqualTo( "DOID:1206" );
        assertThat( OntologyXref.normalizeCurie( "  MGI:1857444  " ) ).isEqualTo( "MGI:1857444" );
    }

    /** A URL carrying no identifier of either shape is still nothing, and must not become one. */
    @Test
    void aUrlWithNoIdentifierIsStillNothing() {
        assertThat( OntologyXref.normalizeCurie( "https://www.jax.org/strain/005864" ) ).isNull();
        assertThat( OntologyXref.normalizeCurie( "https://www.alzforum.org/research-models/ps2app" ) ).isNull();
        assertThat( OntologyXref.normalizeCurie( null ) ).isNull();
        assertThat( OntologyXref.normalizeCurie( "   " ) ).isNull();
    }
}
