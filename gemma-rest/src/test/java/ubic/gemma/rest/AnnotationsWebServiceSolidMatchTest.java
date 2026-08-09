package ubic.gemma.rest;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyTerm;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the "solid match" machinery behind {@code ?suppress_near_matches=true} and
 * the category-scoped promotion on {@code GET /annotations/search}: the designation-shape probe,
 * the exact-vs-near attribution split, and the canonical (hyphen- / cell-suffix-insensitive)
 * equality added to match attribution.
 *
 * <p>Cases are drawn from the drug / cell-line groundings reported in
 * {@code handoffs/AGENTS_ASK_2026_08_09_ANNOTATION_SEARCH_RANKING_FOR_DRUG_NAMES.md}.</p>
 */
class AnnotationsWebServiceSolidMatchTest {

    private static final String OBO_EXACT_SYNONYM = "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym";

    // ---- designation shape ---------------------------------------------------------------

    @Test
    void compoundCodesAreDesignations() {
        assertThat( AnnotationsWebService.isDesignationQuery( "MK-2206" ) ).isTrue();
        assertThat( AnnotationsWebService.isDesignationQuery( "MK2206" ) ).isTrue();
        assertThat( AnnotationsWebService.isDesignationQuery( "GSK2879552" ) ).isTrue();
        assertThat( AnnotationsWebService.isDesignationQuery( "GLP-1" ) ).isTrue();
    }

    @Test
    void cellLinesAndStrainsAreDesignations() {
        assertThat( AnnotationsWebService.isDesignationQuery( "NCI-H358" ) ).isTrue();
        assertThat( AnnotationsWebService.isDesignationQuery( "FTC-133" ) ).isTrue();
        assertThat( AnnotationsWebService.isDesignationQuery( "A549" ) ).isTrue();
        assertThat( AnnotationsWebService.isDesignationQuery( "C57BL/6J" ) ).isTrue();
    }

    @Test
    void descriptiveQueriesAreNotDesignations() {
        // Near-matches must survive for these: suppressing them returns nothing and pushes the
        // caller onto a fuzzy fallback, which is the failure mode the flag exists to prevent.
        assertThat( AnnotationsWebService.isDesignationQuery( "diamide" ) ).isFalse();
        assertThat( AnnotationsWebService.isDesignationQuery( "emtricitabine" ) ).isFalse();
        assertThat( AnnotationsWebService.isDesignationQuery( "high fat diet" ) ).isFalse();
        // Pure abbreviations carry no digit — FTC / TDF are a ranking problem, not a
        // near-match problem, and are handled by category promotion instead.
        assertThat( AnnotationsWebService.isDesignationQuery( "FTC" ) ).isFalse();
        assertThat( AnnotationsWebService.isDesignationQuery( "TDF" ) ).isFalse();
    }

    @Test
    void degenerateQueriesAreNotDesignations() {
        assertThat( AnnotationsWebService.isDesignationQuery( null ) ).isFalse();
        assertThat( AnnotationsWebService.isDesignationQuery( "" ) ).isFalse();
        assertThat( AnnotationsWebService.isDesignationQuery( "  " ) ).isFalse();
        assertThat( AnnotationsWebService.isDesignationQuery( "7" ) ).isFalse();
        assertThat( AnnotationsWebService.isDesignationQuery( "2206" ) ).isFalse();  // no letter
        // Multi-token: a designation is one coined token, even when it contains digits.
        assertThat( AnnotationsWebService.isDesignationQuery( "MK-2206 treatment" ) ).isFalse();
    }

    // ---- exact vs near attribution -------------------------------------------------------

    @Test
    void equalityTiersAreExact() {
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.PREFERRED_LABEL ) ).isTrue();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.EXACT_SYNONYM ) ).isTrue();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.NARROW_SYNONYM ) ).isTrue();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.RELATED_SYNONYM ) ).isTrue();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.BROAD_SYNONYM ) ).isTrue();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.ALT_LABEL ) ).isTrue();
    }

    @Test
    void neighbourhoodTiersAreNotExact() {
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.LABEL_PREFIX ) ).isFalse();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.LABEL_TOKENS ) ).isFalse();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.SYNONYM_TOKENS ) ).isFalse();
        assertThat( AnnotationsWebService.isExactAttribution( null ) ).isFalse();
    }

    // ---- canonical equality in attribution -----------------------------------------------

    @Test
    void unhyphenatedQueryStillMatchesHyphenatedLabel() {
        // Without canonical equality this lands as "no attribution" and suppression would drop
        // the one row the caller wanted.
        AnnotationsWebService.MatchAttribution m =
                AnnotationsWebService.computeMatchAttribution( term( "MK-2206" ), "MK2206" );
        assertThat( m ).isNotNull();
        assertThat( m.via ).isEqualTo( AnnotationsWebService.MatchedVia.PREFERRED_LABEL );
        assertThat( AnnotationsWebService.isExactAttribution( m.via ) ).isTrue();
    }

    @Test
    void bareCellLineNameMatchesCloCellSuffixedLabel() {
        AnnotationsWebService.MatchAttribution m =
                AnnotationsWebService.computeMatchAttribution( term( "NCI-H358 cell" ), "NCI-H358" );
        assertThat( m ).isNotNull();
        assertThat( m.via ).isEqualTo( AnnotationsWebService.MatchedVia.PREFERRED_LABEL );
    }

    @Test
    void abbreviationMatchesViaExactSynonym() {
        // The FTC → emtricitabine case: the label shares nothing with the query, so this hit can
        // only be recognised — and promoted over the identically-labelled MGI gene — through its
        // synonym.
        OntologyTerm t = term( "emtricitabine" );
        // Build the stub value BEFORE opening the when(...) — nesting mock creation inside
        // thenReturn(...) trips Mockito's UnfinishedStubbing check.
        Collection<AnnotationProperty> synonyms = annotations( "FTC" );
        when( t.getAnnotations( OBO_EXACT_SYNONYM ) ).thenReturn( synonyms );
        AnnotationsWebService.MatchAttribution m = AnnotationsWebService.computeMatchAttribution( t, "FTC" );
        assertThat( m ).isNotNull();
        assertThat( m.via ).isEqualTo( AnnotationsWebService.MatchedVia.EXACT_SYNONYM );
        assertThat( m.text ).isEqualTo( "FTC" );
        assertThat( AnnotationsWebService.isExactAttribution( m.via ) ).isTrue();
    }

    @Test
    void differentCompoundSharingAPrefixIsNotAnExactMatch() {
        // MK-8353 is the near-match that reached the agents' embedding fallback and came back as a
        // confident wrong grounding. It must not read as exact.
        AnnotationsWebService.MatchAttribution m =
                AnnotationsWebService.computeMatchAttribution( term( "MK-8353" ), "MK-2206" );
        assertThat( AnnotationsWebService.isExactAttribution( m != null ? m.via : null ) ).isFalse();
    }

    @Test
    void substringNeighbourIsNotAnExactMatch() {
        AnnotationsWebService.MatchAttribution m =
                AnnotationsWebService.computeMatchAttribution( term( "ganoderic acid MK" ), "MK-2206" );
        assertThat( AnnotationsWebService.isExactAttribution( m != null ? m.via : null ) ).isFalse();
    }

    @Test
    void relatedCellLineIsNotAnExactMatchForTheBareName() {
        // FTC-133 / FTC-238 are distinct lines; neither IS "FTC".
        AnnotationsWebService.MatchAttribution m =
                AnnotationsWebService.computeMatchAttribution( term( "FTC-133 cell" ), "FTC" );
        assertThat( AnnotationsWebService.isExactAttribution( m != null ? m.via : null ) ).isFalse();
    }

    // ---- label-only attribution (no ontology available) -----------------------------------

    @Test
    void labelEqualToQueryIsAttributedWithoutTheTerm() {
        // The NCBITaxon_1773 case: label exactly equals the query but the URI resolves to no
        // loaded term, so attribution used to be null and clients read that as "weak".
        AnnotationsWebService.MatchAttribution m = AnnotationsWebService.computeLabelAttribution(
                "Mycobacterium tuberculosis", "Mycobacterium tuberculosis" );
        assertThat( m ).isNotNull();
        assertThat( m.via ).isEqualTo( AnnotationsWebService.MatchedVia.PREFERRED_LABEL );
        assertThat( AnnotationsWebService.isExactAttribution( m.via ) ).isTrue();
    }

    @Test
    void labelOnlyAttributionUsesTheSameCanonicalEquality() {
        AnnotationsWebService.MatchAttribution m =
                AnnotationsWebService.computeLabelAttribution( "NCI-H358 cell", "NCIH358" );
        assertThat( m ).isNotNull();
        assertThat( m.via ).isEqualTo( AnnotationsWebService.MatchedVia.PREFERRED_LABEL );
    }

    @Test
    void labelOnlyAttributionReportsWeakerTiersHonestly() {
        assertThat( AnnotationsWebService.computeLabelAttribution( "aspirin-induced asthma", "aspirin" ).via )
                .isEqualTo( AnnotationsWebService.MatchedVia.LABEL_PREFIX );
        // No relationship at all stays null rather than being invented.
        assertThat( AnnotationsWebService.computeLabelAttribution( "menkes disease", "MK-8722" ) ).isNull();
        assertThat( AnnotationsWebService.computeLabelAttribution( null, "aspirin" ) ).isNull();
        assertThat( AnnotationsWebService.computeLabelAttribution( "aspirin", "  " ) ).isNull();
    }

    @Test
    void labelOnlyAttributionNeverClaimsASynonymTier() {
        // Without the term we cannot see synonyms; claiming one would be a fabricated provenance.
        for ( String label : new String[] { "emtricitabine", "acetylsalicylic acid", "paracetamol" } ) {
            AnnotationsWebService.MatchAttribution m =
                    AnnotationsWebService.computeLabelAttribution( label, "FTC" );
            if ( m != null ) {
                assertThat( m.via ).isIn( AnnotationsWebService.MatchedVia.PREFERRED_LABEL,
                        AnnotationsWebService.MatchedVia.LABEL_PREFIX,
                        AnnotationsWebService.MatchedVia.LABEL_TOKENS );
            }
        }
    }

    // ---- category keying + prefix tables ---------------------------------------------------

    @Test
    void obsoletedCategoryLabelsKeyToTheLiveCategory() {
        // EFO obsoleted its own `disease` in favour of MONDO's and relabelled the term
        // `obsolete_disease`. ~15k Gemma annotations still sit on that URI, so the category is
        // alive while its label stopped matching anything configured: category=disease worked,
        // the URI form silently did not, and /annotations/categories advertised no preference.
        assertThat( AnnotationsWebService.categoryKey( "obsolete_disease" ) ).isEqualTo( "disease" );
        assertThat( AnnotationsWebService.categoryKey( "disease" ) ).isEqualTo( "disease" );
        assertThat( AnnotationsWebService.categoryKey( "organism part" ) ).isEqualTo( "organismPart" );
        assertThat( AnnotationsWebService.categoryKey( "developmental stage" ) ).isEqualTo( "developmentalStage" );
        // "obsolete" as a word of its own is not a marker prefix
        assertThat( AnnotationsWebService.categoryKey( "obsolete" ) ).isEqualTo( "obsolete" );
    }

    @Test
    void prefixPropertyParsesIntoOrderedPerCategoryLists() {
        var parsed = AnnotationsWebService.parseCategoryPrefixProperty(
                "genotype:TGEMO_,GENO_,EFO_; organismPart:UBERON_,EMAPA_,EFO_ ;;bad_entry" );
        assertThat( parsed.get( "genotype" ) ).containsExactly( "TGEMO_", "GENO_", "EFO_" );
        // order is the preference order and must survive parsing
        assertThat( parsed.get( "organismPart" ) ).containsExactly( "UBERON_", "EMAPA_", "EFO_" );
        assertThat( parsed ).doesNotContainKey( "bad_entry" );
    }

    @Test
    void emptyPrefixPropertyYieldsNoPreferences() {
        assertThat( AnnotationsWebService.parseCategoryPrefixProperty( null ) ).isEmpty();
        assertThat( AnnotationsWebService.parseCategoryPrefixProperty( "  " ) ).isEmpty();
    }

    private static OntologyTerm term( String label ) {
        OntologyTerm t = mock( OntologyTerm.class );
        when( t.getLabel() ).thenReturn( label );
        when( t.getAnnotations( org.mockito.ArgumentMatchers.anyString() ) )
                .thenReturn( Collections.emptyList() );
        return t;
    }

    private static Collection<AnnotationProperty> annotations( String... contents ) {
        List<AnnotationProperty> out = new java.util.ArrayList<>();
        for ( String c : contents ) {
            AnnotationProperty ap = mock( AnnotationProperty.class );
            when( ap.getContents() ).thenReturn( c );
            out.add( ap );
        }
        return out;
    }
}
