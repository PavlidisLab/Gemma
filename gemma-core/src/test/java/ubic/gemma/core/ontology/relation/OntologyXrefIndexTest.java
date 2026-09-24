package ubic.gemma.core.ontology.relation;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyXref;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inverting an ontology's cross-references.
 *
 * <p>This index is what turns "CLO says this cell line came from a patient with {@code DOID:3458}" into
 * a MONDO term Gemma can actually store, and it is the piece everything else waits on: without it the
 * only thing CLO's disease targets could be compared against was a label, which is how {@code B-cell}
 * and {@code lymphoma.} became bugs.</p>
 */
class OntologyXrefIndexTest {

    private static final String BREAST_ADENOCARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0004988";
    private static final String ADENOCARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0004970";
    private static final String LUNG_ADENOCARCINOMA = "http://purl.obolibrary.org/obo/MONDO_0005061";

    private static OntologyXref xref( String term, String curie, OntologyXref.Strength strength ) {
        return new OntologyXref( term, curie, strength );
    }

    @Test
    void aForeignIdentifierResolvesToTheTermThatClaimsIt() {
        OntologyXrefIndex index = OntologyXrefIndex.build( Collections.singletonList(
                xref( BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ) ) );

        assertThat( index.resolve( "DOID:3458" ) ).containsExactly( BREAST_ADENOCARCINOMA );
    }

    /**
     * A CLO restriction hands over an OBO PURL, a MONDO cross-reference is written as a CURIE, and both
     * have to hit the same entry. Keying on only one form means the translation quietly never fires.
     */
    @Test
    void aPurlAndACurieAreTheSameKey() {
        OntologyXrefIndex index = OntologyXrefIndex.build( Collections.singletonList(
                xref( BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ) ) );

        assertThat( index.resolve( "http://purl.obolibrary.org/obo/DOID_3458" ) )
                .containsExactly( BREAST_ADENOCARCINOMA );
        assertThat( index.resolve( "doid:3458" ) ).containsExactly( BREAST_ADENOCARCINOMA );
    }

    /**
     * 🛑 The mapping is many-to-many in <b>both</b> directions, and neither side gets collapsed. Two
     * terms claiming one identifier is a genuine ambiguity; picking one here would be picking a disease
     * on the caller's behalf, silently.
     */
    @Test
    void oneIdentifierClaimedByTwoTermsReturnsBoth() {
        OntologyXrefIndex index = OntologyXrefIndex.build( Arrays.asList(
                xref( BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ),
                xref( ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ) ) );

        assertThat( index.resolve( "DOID:3458" ) )
                .containsExactlyInAnyOrder( BREAST_ADENOCARCINOMA, ADENOCARCINOMA );
    }

    @Test
    void oneTermCarryingManyIdentifiersIsReachableByEachOfThem() {
        OntologyXrefIndex index = OntologyXrefIndex.build( Arrays.asList(
                xref( BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ),
                xref( BREAST_ADENOCARCINOMA, "NCIT:C5214", OntologyXref.Strength.UNSPECIFIED ),
                xref( BREAST_ADENOCARCINOMA, "UMLS:C0858252", OntologyXref.Strength.UNSPECIFIED ) ) );

        assertThat( index.size() ).isEqualTo( 3 );
        assertThat( index.resolve( "NCIT:C5214" ) ).containsExactly( BREAST_ADENOCARCINOMA );
        assertThat( index.resolve( "UMLS:C0858252" ) ).containsExactly( BREAST_ADENOCARCINOMA );
        assertThat( index.countsByPrefix() )
                .containsEntry( "DOID", 1 ).containsEntry( "NCIT", 1 ).containsEntry( "UMLS", 1 );
    }

    /**
     * 🛑 The reason the qualifier is carried at all. A narrow cross-reference resolved as though it were
     * exact is a different, entirely plausible disease reported with full confidence — no exception, no
     * empty result, nothing for a reader to notice.
     */
    @Test
    void aNarrowMappingIsNotSubstitutedByDefault() {
        OntologyXrefIndex index = OntologyXrefIndex.build( Collections.singletonList(
                xref( LUNG_ADENOCARCINOMA, "DOID:3910", OntologyXref.Strength.NARROW ) ) );

        assertThat( index.resolve( "DOID:3910" ) ).isEmpty();
        // still reachable for a caller widening a query rather than choosing a term to store
        assertThat( index.resolve( "DOID:3910", false ) ).containsExactly( LUNG_ADENOCARCINOMA );
        assertThat( index.getStrength( "DOID:3910", LUNG_ADENOCARCINOMA ) )
                .isEqualTo( OntologyXref.Strength.NARROW );
    }

    /**
     * The same pair asserted twice at different strengths keeps the stronger claim, because the two are
     * not in conflict: one of them is a bare cross-reference the other qualifies.
     */
    @Test
    void theStrongerClaimWinsWhenAPairIsAssertedTwice() {
        OntologyXrefIndex index = OntologyXrefIndex.build( Arrays.asList(
                xref( BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.UNSPECIFIED ),
                xref( BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ) ) );

        assertThat( index.getStrength( "DOID:3458", BREAST_ADENOCARCINOMA ) )
                .isEqualTo( OntologyXref.Strength.EXACT );
    }

    @Test
    void anUnknownIdentifierResolvesToNothingRatherThanFailing() {
        OntologyXrefIndex index = OntologyXrefIndex.build( Collections.singletonList(
                xref( BREAST_ADENOCARCINOMA, "DOID:3458", OntologyXref.Strength.EXACT ) ) );

        assertThat( index.resolve( "DOID:99999" ) ).isEmpty();
        assertThat( index.resolve( null ) ).isEmpty();
        assertThat( index.resolve( "not an identifier" ) ).isEmpty();
        assertThat( OntologyXrefIndex.empty().resolve( "DOID:3458" ) ).isEmpty();
        assertThat( OntologyXrefIndex.empty().isEmpty() ).isTrue();
    }

    @Test
    void normalizationHandlesTheFormsThatOccurInPractice() {
        assertThat( OntologyXref.normalizeCurie( "NCIt:C5214" ) ).isEqualTo( "NCIT:C5214" );
        assertThat( OntologyXref.normalizeCurie( " DOID:3458 " ) ).isEqualTo( "DOID:3458" );
        assertThat( OntologyXref.normalizeCurie( "http://purl.obolibrary.org/obo/MONDO_0004988" ) )
                .isEqualTo( "MONDO:0004988" );
        assertThat( OntologyXref.normalizeCurie( "http://www.ebi.ac.uk/efo/EFO_0000408" ) )
                .isEqualTo( "EFO:0000408" );
        assertThat( OntologyXref.normalizeCurie( "" ) ).isNull();
        assertThat( OntologyXref.normalizeCurie( null ) ).isNull();
        assertThat( OntologyXref.normalizeCurie( "nocolon" ) ).isNull();
        assertThat( OntologyXref.normalizeCurie( "http://example.org/nolocalname" ) ).isNull();
    }
}
