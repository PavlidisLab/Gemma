package ubic.gemma.core.ontology.chembl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the parts of ChEMBL identification that must hold without a network: which
 * spellings of a code get tried, and what a resolved compound does and does not offer a caller.
 */
class ChemblCodeResolverImplTest {

    @Test
    void separatorSpellingsAreAllTried() {
        // ChEMBL stores WP 1066 spaced and LLY-283 hyphenated; the submitter writes either.
        List<String> variants = ChemblCodeResolverImpl.spellingVariants( "wp1066" );
        assertThat( variants ).contains( "wp1066", "wp-1066", "wp 1066" );

        assertThat( ChemblCodeResolverImpl.spellingVariants( "lly-283" ) )
                .contains( "lly-283", "lly 283", "lly283" );
    }

    @Test
    void variantsAreDeduplicatedAndFaithfulFirst() {
        List<String> variants = ChemblCodeResolverImpl.spellingVariants( "cpi203" );
        assertThat( variants.get( 0 ) ).isEqualTo( "cpi203" );
        assertThat( variants ).doesNotHaveDuplicates();
    }

    @Test
    void aCodeWithNoNumberYieldsOnlyItself() {
        // The prefix/number split must not fire on something that is not code-shaped.
        assertThat( ChemblCodeResolverImpl.spellingVariants( "aspirin" ) ).containsExactly( "aspirin" );
    }

    /**
     * The identification is advisory. It names a compound; it never hands back something to
     * annotate with, because the URI a curator commits has to come from a vocabulary Gemma loads.
     */
    @Test
    void aFoundCompoundOffersANameAndProvenanceButNoAnnotatableUri() {
        ChemblCompound c = new ChemblCompound( "wy-14643", "CHEMBL295416", "PIRINIXIC ACID",
                "wy-14643", "ChEMBL_37" );

        assertThat( c.isFound() ).isTrue();
        assertThat( c.getSearchableName() ).isEqualTo( "PIRINIXIC ACID" );
        // Provenance: what said so, from which release, and where to check it.
        assertThat( c.getChemblId() ).isEqualTo( "CHEMBL295416" );
        assertThat( c.getRelease() ).isEqualTo( "ChEMBL_37" );
        assertThat( c.getMatchedSynonym() ).isEqualTo( "wy-14643" );
        assertThat( c.getSourceUrl() ).isEqualTo(
                "https://www.ebi.ac.uk/chembl/compound_report_card/CHEMBL295416/" );
    }

    /**
     * ChEMBL has entries it has never named. There is nothing to bridge with there — searching
     * Gemma for the code again would just repeat the query that already missed.
     */
    @Test
    void anUnnamedCompoundHasNothingToBridgeWith() {
        ChemblCompound c = new ChemblCompound( "lly-283", "CHEMBL4168754", null, "lly-283", "ChEMBL_37" );

        assertThat( c.isFound() ).isTrue();
        assertThat( c.getSearchableName() ).isNull();
        assertThat( c.getSourceUrl() ).isNotNull();
    }

    /**
     * Over-the-wire guard, excluded from the default run. It exists because the whole design turns
     * on one contract detail: the exact-synonym filter identifies {@code wy-14643} as pirinixic
     * acid, where the fuzzy {@code /molecule/search} endpoint returns a different molecule. If EBI
     * ever changes that filter, this feature starts making things up and nothing else would notice.
     */
    @Test
    @Tag("network")
    void exactSynonymLookupIdentifiesPirinixicAcidOverTheWire() {
        ChemblCodeResolverImpl resolver = new ChemblCodeResolverImpl();
        ReflectionTestUtils.setField( resolver, "baseUrl", "https://www.ebi.ac.uk" );
        ReflectionTestUtils.setField( resolver, "timeoutMs", 15000 );
        ReflectionTestUtils.setField( resolver, "enabled", true );

        ChemblCompound c = resolver.identify( "wy-14643" );

        assertThat( c ).isNotNull();
        assertThat( c.getChemblId() ).isEqualTo( "CHEMBL295416" );
        assertThat( c.getSearchableName() ).isEqualToIgnoringCase( "pirinixic acid" );
        assertThat( c.getRelease() ).startsWith( "ChEMBL" );
    }

    @Test
    @Tag("network")
    void anUnknownCodeIsReportedUnidentifiedRatherThanGuessed() {
        ChemblCodeResolverImpl resolver = new ChemblCodeResolverImpl();
        ReflectionTestUtils.setField( resolver, "baseUrl", "https://www.ebi.ac.uk" );
        ReflectionTestUtils.setField( resolver, "timeoutMs", 15000 );
        ReflectionTestUtils.setField( resolver, "enabled", true );

        // In ChEMBL's fuzzy search this returns CHEMBL5219793 with no supporting synonym; the
        // exact filter correctly declines to identify it.
        assertThat( resolver.identify( "iub288" ) ).isNull();
    }

    @Test
    void disabledResolverNeverReachesOut() {
        ChemblCodeResolverImpl resolver = new ChemblCodeResolverImpl();
        ReflectionTestUtils.setField( resolver, "baseUrl", "http://invalid.invalid" );
        ReflectionTestUtils.setField( resolver, "timeoutMs", 1 );
        ReflectionTestUtils.setField( resolver, "enabled", false );

        assertThat( resolver.identify( "wy-14643" ) ).isNull();
    }

    @Test
    void notFoundIsASentinelNotAnIdentification() {
        ChemblCompound c = ChemblCompound.notFound( "nsc80997" );

        assertThat( c.isFound() ).isFalse();
        assertThat( c.getChemblId() ).isNull();
        assertThat( c.getSearchableName() ).isNull();
        assertThat( c.getSourceUrl() ).isNull();
    }
}
