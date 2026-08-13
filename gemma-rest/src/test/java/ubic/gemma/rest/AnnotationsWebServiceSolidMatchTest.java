package ubic.gemma.rest;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.lexical.LexicalOntologyTerm;
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyTerm;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final String OBO_RELATED_SYNONYM = "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym";

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
        // A narrower term carrying the query as one of its names is a more specific answer, not a
        // different one; an alternative label is a label rather than a neighbourhood claim.
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.NARROW_SYNONYM ) ).isTrue();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.ALT_LABEL ) ).isTrue();
    }

    /**
     * RELATED and BROAD are the scopes an ontology uses for a term in the neighbourhood rather than
     * a name for the thing itself, and they are almost always the wrong answer for a caller that
     * asked by name. A query of `H1` reaches `h1 horizontal cell` (CL_0004217) through a RELATED
     * synonym; admitting that scope put a retinal interneuron in the exact tier beside `H1-hESC`.
     */
    @Test
    void relatedAndBroadSynonymsAreNotEquality() {
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.RELATED_SYNONYM ) ).isFalse();
        assertThat( AnnotationsWebService.isExactAttribution( AnnotationsWebService.MatchedVia.BROAD_SYNONYM ) ).isFalse();
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
        // A term whose label shares nothing with the query can still be recognised — and promoted
        // over an identically-labelled hit in another namespace — through an EXACT synonym.
        OntologyTerm t = term( "a compound" );
        // Build the stub value BEFORE opening the when(...) — nesting mock creation inside
        // thenReturn(...) trips Mockito's UnfinishedStubbing check.
        Collection<AnnotationProperty> synonyms = annotations( "ABC" );
        when( t.getAnnotations( OBO_EXACT_SYNONYM ) ).thenReturn( synonyms );
        AnnotationsWebService.MatchAttribution m = AnnotationsWebService.computeMatchAttribution( t, "ABC" );
        assertThat( m ).isNotNull();
        assertThat( m.via ).isEqualTo( AnnotationsWebService.MatchedVia.EXACT_SYNONYM );
        assertThat( m.text ).isEqualTo( "ABC" );
        assertThat( AnnotationsWebService.isExactAttribution( m.via ) ).isTrue();
    }

    @Test
    void theRealFtcShapeIsNotSolid() {
        // This test used to BE the FTC case, and it encoded an ontology fact that is not true:
        // it stubbed `FTC` as emtricitabine's hasExactSynonym. Measured on gemma2 2026-08-13,
        // ChEBI files the abbreviation as a RELATED synonym, and on emtricitabine the string is
        // `(-)-FTC`, not `FTC`. Both halves matter, and each one alone defeats promotion:
        //
        //   ferroptocide   related_synonym  "FTC"      -> exact string, non-exact scope
        //   emtricitabine  related_synonym  "(-)-FTC"  -> non-exact scope AND non-equal string
        //
        // So neither ChEBI candidate passes `solid`, the only solid hit for `FTC` under
        // category=treatment is the MGI gene (whose URI carries no preferred prefix), every hit
        // lands in the same promotion tier and the stable sort is a no-op. The green version of
        // this test is why the false claim survived in the `category` docs for months.
        OntologyTerm ferroptocide = term( "ferroptocide" );
        Collection<AnnotationProperty> related = annotations( "FTC" );
        when( ferroptocide.getAnnotations( OBO_RELATED_SYNONYM ) ).thenReturn( related );
        AnnotationsWebService.MatchAttribution m =
                AnnotationsWebService.computeMatchAttribution( ferroptocide, "FTC" );
        assertThat( m ).isNotNull();
        assertThat( m.via ).isEqualTo( AnnotationsWebService.MatchedVia.RELATED_SYNONYM );
        assertThat( AnnotationsWebService.isExactAttribution( m.via ) )
                .withFailMessage( "a related synonym must not read as an exact match" )
                .isFalse();

        // And emtricitabine, the compound the corpus actually uses, is further away still: its
        // matched string canonicalises to "()ftc" — canonicaliseForExactMatch strips hyphens but
        // not parentheses — so relaxing the scope gate would promote ferroptocide (0 corpus uses)
        // and STILL leave emtricitabine behind. Only corpus usage separates this pair.
        assertThat( AnnotationsWebService.canonicaliseForExactMatch( "(-)-FTC" ) )
                .isNotEqualTo( AnnotationsWebService.canonicaliseForExactMatch( "FTC" ) );
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

    // ---- flat lexical terms (Cellosaurus / MGI) --------------------------------------------

    /**
     * A term from a flat vocabulary has to survive attribution. It reaches here for real: {@code
     * getTerm} fans out over the lexical services too, so a CVCL hit on {@code /annotations/search}
     * is enriched exactly like an EFO one.
     *
     * <p>Uses the production class rather than {@link #term(String)}, deliberately. The mock stubs
     * {@code getAnnotations(uri)} to empty, so it agrees with every implementation and can never
     * detect one that throws — which is how {@code UnsupportedOperationException: Use a
     * OntologyTermImpl} shipped to CAB on 2026-08-10 with the suite green.</p>
     */
    @Test
    void aFlatLexicalTermIsAttributedFromItsLabelInsteadOfThrowing() {
        OntologyTerm cvcl = new LexicalOntologyTerm( "https://www.cellosaurus.org/CVCL_1108", "Cal-33" );
        AnnotationsWebService.MatchAttribution m =
                AnnotationsWebService.computeMatchAttribution( cvcl, "Cal33" );
        assertThat( m ).isNotNull();
        // Canonical equality drops the hyphen, so the designation matches its own label.
        assertThat( m.via ).isEqualTo( AnnotationsWebService.MatchedVia.PREFERRED_LABEL );
        assertThat( AnnotationsWebService.isExactAttribution( m.via ) ).isTrue();
    }

    @Test
    void aFlatLexicalTermAnswersEveryPredicateProbeTheRestLayerMakes() {
        // The three probe surfaces: definition (IAO_0000115), the six synonym predicates, and
        // hasDbXref. All go through the single-URI overloads, which is what was missing.
        OntologyTerm cvcl = new LexicalOntologyTerm( "https://www.cellosaurus.org/CVCL_0330", "BV-2" );
        assertThat( cvcl.getAnnotation( "http://purl.obolibrary.org/obo/IAO_0000115" ) ).isNull();
        assertThat( cvcl.getAnnotations( OBO_EXACT_SYNONYM ) ).isEmpty();
        assertThat( cvcl.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasDbXref" ) ).isEmpty();
        // A non-matching query stays honestly unattributed rather than blowing up.
        assertThat( AnnotationsWebService.computeMatchAttribution( cvcl, "HT22" ) ).isNull();
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
        assertThat( AnnotationsWebService.categoryKey( "organism part" ) ).isEqualTo( "organismpart" );
        assertThat( AnnotationsWebService.categoryKey( "developmental stage" ) ).isEqualTo( "developmentalstage" );
        // "obsolete" as a word of its own is not a marker prefix
        assertThat( AnnotationsWebService.categoryKey( "obsolete" ) ).isEqualTo( "obsolete" );
    }

    /**
     * Every caller of canonicaliseForExactMatch is deciding an equality — the relevance tiers, the
     * near-match solid test, exact_label, and match attribution. Stored terms carry the submitter's
     * spacing (13,179 production values hold an internal double space), so the comparison must
     * collapse runs or a search for the clean spelling misses the stored one and vice versa.
     */
    @Test
    void exactMatchIgnoresInternalWhitespaceRuns() {
        assertThat( AnnotationsWebService.canonicaliseForExactMatch( "high  fat  diet" ) )
                .isEqualTo( AnnotationsWebService.canonicaliseForExactMatch( "high fat diet" ) );
        assertThat( AnnotationsWebService.canonicaliseForExactMatch( " cancer cell line " ) )
                .isEqualTo( AnnotationsWebService.canonicaliseForExactMatch( "cancer cell line" ) );
        // The hyphen and cell-suffix rules it already carried must still hold.
        assertThat( AnnotationsWebService.canonicaliseForExactMatch( "MEC-2  cell" ) )
                .isEqualTo( AnnotationsWebService.canonicaliseForExactMatch( "mec2" ) );
        // Distinct terms must not collapse into each other.
        assertThat( AnnotationsWebService.canonicaliseForExactMatch( "high fat diet" ) )
                .isNotEqualTo( AnnotationsWebService.canonicaliseForExactMatch( "low fat diet" ) );
    }

    @Test
    void everySpellingOfACategoryFoldsOntoOneKey() {
        // The trap this closes: `category=cellLine` — the spelling a client copies straight out of
        // annotation.category.prefixes — used to lowercase to "cellline" and match the "cellLine"
        // config key. An unrecognised category is a silent no-op, so the caller got no promotion
        // and no signal, while `category=cell line` worked. Asserted as a set collapsing to size 1
        // rather than against a literal, so the property survives a change of fold.
        assertThat( Stream.of( "cell line", "cellLine", "Cell Line", "CELL LINE", " cell_line ", "cell-line" )
                .map( AnnotationsWebService::categoryKey )
                .collect( Collectors.toSet() ) )
                .hasSize( 1 );
        // Distinct categories must not collide under the looser fold.
        assertThat( AnnotationsWebService.categoryKey( "cell line" ) )
                .isNotEqualTo( AnnotationsWebService.categoryKey( "cell type" ) );
    }

    @Test
    void configKeysAndCallerSpellingsMeetOnTheSameKey() {
        // Both sides run through categoryKey, so the property can be written in whichever spelling
        // reads best and still answer every spelling a caller sends.
        var parsed = AnnotationsWebService.parseCategoryPrefixProperty( "cellLine:CLO_,EFO_,CVCL_" );
        assertThat( parsed.get( AnnotationsWebService.categoryKey( "cell line" ) ) )
                .containsExactly( "CLO_", "EFO_", "CVCL_" );
        assertThat( parsed.get( AnnotationsWebService.categoryKey( "cellLine" ) ) )
                .containsExactly( "CLO_", "EFO_", "CVCL_" );
    }

    @Test
    void prefixPropertyParsesIntoOrderedPerCategoryLists() {
        var parsed = AnnotationsWebService.parseCategoryPrefixProperty(
                "genotype:TGEMO_,GENO_,EFO_; organismPart:UBERON_,EMAPA_,EFO_ ;;bad_entry" );
        assertThat( parsed.get( "genotype" ) ).containsExactly( "TGEMO_", "GENO_", "EFO_" );
        // order is the preference order and must survive parsing; the key is folded on the way in,
        // so the camelCase spelling in the property lands under the canonical key
        assertThat( parsed.get( "organismpart" ) ).containsExactly( "UBERON_", "EMAPA_", "EFO_" );
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
