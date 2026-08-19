package ubic.gemma.model.common.description;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the read-time URI canonicaliser — the stand-in for the parked database migration
 * ({@code scripts/sql/term_uri_migration.sql}).
 * <p>
 * The point of these tests is that the shim is not inert. A mapping table that loads to zero
 * rows, or a resource that silently fails to parse, looks exactly like a corpus with no
 * duplicates in it — so "it compiles" is not evidence that anything is being resolved.
 */
public class CharacteristicUtilsUriMigrationTest {

    private static final String OBO = "http://purl.obolibrary.org/obo/";

    @Test
    public void testTheMappingResourceActuallyLoads() {
        assertThat( CharacteristicUtils.remappedUriCount() )
                .as( "the shim is loaded and non-empty; zero would be indistinguishable from a clean corpus" )
                .isGreaterThan( 0 );
    }

    /** A CLO twin: the retired spelling resolves to the one we keep, and the label follows. */
    @Test
    public void testCloTwinResolvesToTheKeptTerm() {
        String retired = OBO + "CLO_0007365";      // 'LNCAP cell', 38 uses
        String kept = OBO + "CLO_0037116";         // 'LNCaP cell', 142 uses
        assertThat( CharacteristicUtils.canonicalUri( retired ) ).isEqualTo( kept );
        assertThat( CharacteristicUtils.canonicalLabel( retired, "LNCAP cell" ) )
                .as( "the label moves with the URI, or the row says one thing and means another" )
                .isEqualTo( "LNCaP cell" );
    }

    /** A bare CURIE is not an IRI; it expands to the OBO form. */
    @Test
    public void testBareCurieExpandsToAnIri() {
        assertThat( CharacteristicUtils.canonicalUri( "CL:0000236" ) ).isEqualTo( OBO + "CL_0000236" );
    }

    /** OBO IRIs separate with an underscore; a colon in that position is malformed. */
    @Test
    public void testColonFormIsRepairedToUnderscore() {
        assertThat( CharacteristicUtils.canonicalUri( OBO + "CL:0000115" ) ).isEqualTo( OBO + "CL_0000115" );
    }

    /** An id concatenated with itself resolves to nothing; the repair was label-verified. */
    @Test
    public void testConcatenatedIdIsRepaired() {
        assertThat( CharacteristicUtils.canonicalUri( OBO + "CL_0000669000669" ) ).isEqualTo( OBO + "CL_0000669" );
    }

    /**
     * The overwhelmingly common case: a term nobody remapped comes back untouched. A
     * canonicaliser that rewrote anything it did not recognise would corrupt the whole corpus
     * to fix 350 rows.
     */
    @Test
    public void testAnUnmappedTermIsReturnedUnchanged() {
        String mondo = OBO + "MONDO_0007254";
        assertThat( CharacteristicUtils.canonicalUri( mondo ) ).isSameAs( mondo );
        assertThat( CharacteristicUtils.canonicalLabel( mondo, "breast cancer" ) ).isEqualTo( "breast cancer" );
        assertThat( CharacteristicUtils.isRemappedUri( mondo ) ).isFalse();
    }

    /** Null in, null out — the VOs call this on nullable columns. */
    @Test
    public void testNullIsTolerated() {
        assertThat( CharacteristicUtils.canonicalUri( null ) ).isNull();
        assertThat( CharacteristicUtils.canonicalLabel( null, "free text" ) ).isEqualTo( "free text" );
        assertThat( CharacteristicUtils.isRemappedUri( null ) ).isFalse();
    }

    /**
     * Every mapping must be a fixed point: no from-URI may also be a to-URI, or the resolved
     * answer depends on how many times you resolve.
     */
    @Test
    public void testTheMappingHasNoChains() {
        for ( String from : new String[] { OBO + "CLO_0007365", "CL:0000236", OBO + "CL:0000115" } ) {
            String once = CharacteristicUtils.canonicalUri( from );
            assertThat( CharacteristicUtils.canonicalUri( once ) )
                    .as( "resolving twice must equal resolving once" ).isEqualTo( once );
        }
    }
}
