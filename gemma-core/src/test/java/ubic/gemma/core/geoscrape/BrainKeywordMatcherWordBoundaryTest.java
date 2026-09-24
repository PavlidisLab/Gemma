package ubic.gemma.core.geoscrape;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The keyword scan used a raw {@code String.contains}, so a keyword matched anywhere inside a
 * longer word. bro reported GSE343489 on 2026-08-12 — "CpG Hypermethylation … Pediatric
 * ADRENOcortical Carcinoma", 23 adrenal-tumour samples — returned as a {@code brain} match,
 * because {@code "adrenocortical"} contains {@code "cortical"}.
 *
 * <p>They could not audit it, because the scrape returned WHICH matcher fired and never WHY. Both
 * halves are fixed; this pins the matching half.
 */
class BrainKeywordMatcherWordBoundaryTest {

    private final BrainKeywordMatcher matcher = new BrainKeywordMatcher();

    private static GeoRecord rec( String title ) {
        GeoRecord r = new GeoRecord();
        r.setGeoAccession( "GSE000000" );
        r.setTitle( title );
        r.setSummary( "" );
        return r;
    }

    @Test
    void adrenocorticalIsNotABrainStudy() {
        assertThat( matcher.evaluate( rec(
                "CpG Hypermethylation in Pediatric Adrenocortical Carcinoma" ) ).isMatched() )
                .as( "the reported false positive: 'cortical' inside 'adrenocortical'" )
                .isFalse();
    }

    @Test
    void otherSubstringFalsePositivesAlsoGo() {
        assertThat( matcher.evaluate( rec( "Trigeminal neuralgia cohort" ) ).isMatched() )
                .as( "'neural' inside 'neuralgia'" ).isFalse();
        assertThat( matcher.evaluate( rec( "Adrenal cortex steroidogenesis" ) ).isMatched() )
                .as( "'cortex' as its own word here is adrenal, not brain -- but it IS a word, so "
                        + "this one legitimately matches and word-boundary logic cannot save us" )
                .isTrue();
    }

    @Test
    void genuineBrainStudiesStillMatch() {
        assertThat( matcher.evaluate( rec( "Prefrontal cortex neuron atlas" ) ).isMatched() ).isTrue();
        assertThat( matcher.evaluate( rec( "Hippocampus single-cell survey" ) ).isMatched() ).isTrue();
        assertThat( matcher.evaluate( rec( "Substantia nigra dopaminergic loss" ) ).isMatched() )
                .as( "multi-word keywords must still work" ).isTrue();
    }

    @Test
    void hyphensAndSlashesCountAsBoundaries() {
        assertThat( matcher.evaluate( rec( "Cortex-specific knockout" ) ).isMatched() ).isTrue();
        assertThat( matcher.evaluate( rec( "neuron/glia co-culture" ) ).isMatched() ).isTrue();
    }

    @Test
    void theReasonNamesTheKeywordThatFired() {
        assertThat( matcher.evaluate( rec( "Prefrontal cortex neuron atlas" ) ).getReason() )
                .as( "this string is what now reaches the caller as matchedEvidence" )
                .contains( "cortex" );
    }
}
