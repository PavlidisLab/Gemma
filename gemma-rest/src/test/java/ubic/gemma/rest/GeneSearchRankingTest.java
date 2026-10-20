/*
 * The gemma-rest project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.search.SearchMatchType;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the secondary-sort behaviour of {@link GeneWebService#searchGenes} — the within-score-band
 * tiebreaker that lifts {@code Trp53} above unrelated alias-list members ({@code Hipk2},
 * {@code Muc1}, {@code Bcl3}) for query {@code tp53}. The end-to-end path is tested by
 * {@link GeneWebServiceTest}; this class exercises the pure tier / edit-distance helpers so
 * regressions are caught without spinning up a Spring context.
 */
class GeneSearchRankingTest {

    @Test
    void symbolTier_classifiesByQueryRelationship() {
        assertEquals( 0, GeneWebService.symbolTier( "trp53", "trp53" ) );  // exact
        assertEquals( 1, GeneWebService.symbolTier( "trp53b", "trp53" ) ); // startsWith
        assertEquals( 2, GeneWebService.symbolTier( "atrp53", "trp53" ) ); // endsWith
        assertEquals( 3, GeneWebService.symbolTier( "atrp53b", "trp53" ) );// contains (interior)
        assertEquals( 4, GeneWebService.symbolTier( "hipk2", "tp53" ) );   // no overlap → alias-only
        assertEquals( 5, GeneWebService.symbolTier( null, "tp53" ) );      // no symbol
    }

    @Test
    void editDistance_lifts_Trp53_above_unrelated_alias_hits_for_tp53() {
        String q = "tp53";
        int trp53 = GeneWebService.editDistanceOrMax( "trp53", q );
        int hipk2 = GeneWebService.editDistanceOrMax( "hipk2", q );
        int muc1 = GeneWebService.editDistanceOrMax( "muc1", q );
        int bcl3 = GeneWebService.editDistanceOrMax( "bcl3", q );
        assertTrue( trp53 < hipk2, "Trp53(d=" + trp53 + ") should be closer to tp53 than Hipk2(d=" + hipk2 + ")" );
        assertTrue( trp53 < muc1, "Trp53(d=" + trp53 + ") should be closer to tp53 than Muc1(d=" + muc1 + ")" );
        assertTrue( trp53 < bcl3, "Trp53(d=" + trp53 + ") should be closer to tp53 than Bcl3(d=" + bcl3 + ")" );
    }

    @Test
    void editDistance_nullSymbol_sinksToBottom() {
        assertEquals( Integer.MAX_VALUE, GeneWebService.editDistanceOrMax( null, "tp53" ) );
    }

    @Test
    void editDistanceClamped_collapsesDistantSymbolsIntoOneBucket() {
        // The Cx43 alias collision: query has no structural similarity with either symbol, so
        // raw Levenshtein at distance ≥ 3 is just coincidental letter overlap (Gja3's trailing
        // "3" accidentally matches Cx43's). Clamping at 2 collapses these into the same bucket
        // so downstream tiebreakers (popularity, length, alphabetical) decide.
        assertEquals( 2, GeneWebService.editDistanceClamped( "gja1", "cx43" ) );
        assertEquals( 2, GeneWebService.editDistanceClamped( "gja3", "cx43" ) );
        // ...without losing the proven single-typo signal that pins Trp53 above Hipk2 for tp53.
        assertEquals( 1, GeneWebService.editDistanceClamped( "trp53", "tp53" ) );
        assertEquals( 2, GeneWebService.editDistanceClamped( "hipk2", "tp53" ) );
    }

    @Test
    void rankingComparator_breaksCx43AliasCollisionByPopularityThenAlphabetical() {
        // Both Gja1 and Gja3 carry "Cx43" as an alias in NCBI — Gja1 legitimately (it IS Cx43),
        // Gja3 spuriously (Gja3 is Cx46; NCBI just lists "Cx43" on its alias row by mistake).
        // The full-text Lucene leg returns both with the same score. The within-band tiebreaker
        // must pick Gja1: edit-distance clamping equalises them, then popularity (10 EE
        // associations vs 0) makes the call.
        SearchResult<GeneValueObject> gja1 = mkGene( "Gja1", 14609L, 10 );
        SearchResult<GeneValueObject> gja3 = mkGene( "Gja3", 14611L, 0 );
        List<SearchResult<?>> raw = new ArrayList<>();
        raw.add( gja3 ); // start with the wrong order to prove the comparator does the work
        raw.add( gja1 );
        raw.sort( GeneWebService.searchRankingComparator( "cx43" ) );
        assertEquals( "Gja1", ( ( GeneValueObject ) raw.get( 0 ).getResultObject() ).getOfficialSymbol() );
        assertEquals( "Gja3", ( ( GeneValueObject ) raw.get( 1 ).getResultObject() ).getOfficialSymbol() );
    }

    @Test
    void rankingComparator_preservesTp53Trp53PromotionForTp53() {
        // Regression for the proven tp53 → Trp53 pin (commit 546b58267d). The new edit-distance
        // clamp + popularity steps must NOT regress this: Trp53 (distance 1, popularity high)
        // still beats Hipk2 / Muc1 / Bcl3 (distance ≥ 3, low popularity) for "tp53".
        SearchResult<GeneValueObject> trp53 = mkGene( "Trp53", 22059L, 50 );
        SearchResult<GeneValueObject> hipk2 = mkGene( "Hipk2", 15258L, 5 );
        SearchResult<GeneValueObject> bcl3 = mkGene( "Bcl3", 12051L, 5 );
        List<SearchResult<?>> raw = new ArrayList<>();
        raw.add( hipk2 );
        raw.add( bcl3 );
        raw.add( trp53 );
        raw.sort( GeneWebService.searchRankingComparator( "tp53" ) );
        assertEquals( "Trp53", ( ( GeneValueObject ) raw.get( 0 ).getResultObject() ).getOfficialSymbol() );
    }

    @Test
    void rankingComparator_popularityBreaksAliasCollisionEvenWhenAlphabeticalWouldNotHelp() {
        // Independent guard on the popularity step: the alphabetically-FIRST symbol is the
        // SPURIOUS hit. Without popularity, alphabetical would pick the wrong gene. With
        // popularity, the high-usage one wins regardless of letter order.
        SearchResult<GeneValueObject> spurious = mkGene( "Aaa1", 999L, 0 );   // low usage
        SearchResult<GeneValueObject> canonical = mkGene( "Zzz9", 1000L, 42 ); // high usage
        List<SearchResult<?>> raw = new ArrayList<>();
        raw.add( spurious );
        raw.add( canonical );
        raw.sort( GeneWebService.searchRankingComparator( "xx99" ) );
        assertEquals( "Zzz9", ( ( GeneValueObject ) raw.get( 0 ).getResultObject() ).getOfficialSymbol() );
    }

    @Test
    void searchMatchType_wireTokensMatchClientVocabulary() {
        // The curation-UI / agent clients gate on these snake_case tokens; pin them so a rename
        // can't silently break the contract (GENE_SEARCH_INEXACT_FUZZY_MATCH handoff, option 1).
        assertEquals( "exact_symbol", SearchMatchType.EXACT_SYMBOL.getWireName() );
        assertEquals( "alias", SearchMatchType.ALIAS.getWireName() );
        assertEquals( "prefix", SearchMatchType.SYMBOL_PREFIX.getWireName() );
        assertEquals( "official_name", SearchMatchType.OFFICIAL_NAME.getWireName() );
        assertEquals( "official_name_prefix", SearchMatchType.OFFICIAL_NAME_PREFIX.getWireName() );
        assertEquals( "exact_identifier", SearchMatchType.EXACT_IDENTIFIER.getWireName() );
    }

    @Test
    void matchTrust_ordersTrustedKindsAbovePrefixLookAlikes() {
        assertEquals( 0, GeneWebService.matchTrust( mkKind( SearchMatchType.EXACT_SYMBOL ) ) );
        assertEquals( 0, GeneWebService.matchTrust( mkKind( SearchMatchType.ALIAS ) ) );
        assertEquals( 0, GeneWebService.matchTrust( mkKind( SearchMatchType.OFFICIAL_NAME ) ) );
        assertEquals( 0, GeneWebService.matchTrust( mkKind( SearchMatchType.EXACT_IDENTIFIER ) ) );
        assertEquals( 2, GeneWebService.matchTrust( mkKind( SearchMatchType.SYMBOL_PREFIX ) ) );
        assertEquals( 2, GeneWebService.matchTrust( mkKind( SearchMatchType.OFFICIAL_NAME_PREFIX ) ) );
        assertEquals( 1, GeneWebService.matchTrust( mkKind( null ) ) ); // unclassified full-text
    }

    @Test
    void rankingComparator_demotesPrefixLookAlikeBelowAliasHit_ankaCase() {
        // The ANKA -> Ankar case generalised: a bare prefix hit shares the 0.9 band with a genuine
        // alias hit. symbolTier alone LIFTS the prefix (startsWith=1) above the alias (no-overlap=4);
        // matchTrust must override that so the trusted alias wins. We demote, not drop — a lone
        // prefix hit is still returned (labelled prefix) for the caller to reject.
        SearchResult<GeneValueObject> prefix = mkGeneWithKind( "Ankar", 1L, 0, SearchMatchType.SYMBOL_PREFIX );
        SearchResult<GeneValueObject> alias = mkGeneWithKind( "Xyz1", 2L, 0, SearchMatchType.ALIAS );
        List<SearchResult<?>> raw = new ArrayList<>();
        raw.add( prefix ); // wrong order first, to prove the comparator does the work
        raw.add( alias );
        raw.sort( GeneWebService.searchRankingComparator( "anka" ) );
        assertEquals( "Xyz1", ( ( GeneValueObject ) raw.get( 0 ).getResultObject() ).getOfficialSymbol(),
                "a trusted alias hit must outrank a prefix look-alike in the same score band" );
        assertEquals( "Ankar", ( ( GeneValueObject ) raw.get( 1 ).getResultObject() ).getOfficialSymbol() );
    }

    @Test
    void rankingComparator_sameTrustCollisionStillDecidedByPopularity() {
        // Guard that matchTrust does NOT disturb the proven within-kind tiebreakers: when both hits
        // are ALIAS (trust 0, tie), the Cx43 popularity step must still pick Gja1 over Gja3.
        SearchResult<GeneValueObject> gja1 = mkGeneWithKind( "Gja1", 14609L, 10, SearchMatchType.ALIAS );
        SearchResult<GeneValueObject> gja3 = mkGeneWithKind( "Gja3", 14611L, 0, SearchMatchType.ALIAS );
        List<SearchResult<?>> raw = new ArrayList<>();
        raw.add( gja3 );
        raw.add( gja1 );
        raw.sort( GeneWebService.searchRankingComparator( "cx43" ) );
        assertEquals( "Gja1", ( ( GeneValueObject ) raw.get( 0 ).getResultObject() ).getOfficialSymbol() );
    }

    private static SearchResult<GeneValueObject> mkGeneWithKind( String symbol, long id, int eeCount, SearchMatchType kind ) {
        SearchResult<GeneValueObject> sr = mkGene( symbol, id, eeCount );
        sr.setMatchKind( kind );
        return sr;
    }

    private static SearchResult<GeneValueObject> mkKind( @org.springframework.lang.Nullable SearchMatchType kind ) {
        SearchResult<GeneValueObject> sr = mkGene( "Sym", 1L, 0 );
        sr.setMatchKind( kind );
        return sr;
    }

    private static SearchResult<GeneValueObject> mkGene( String symbol, long id, int eeCount ) {
        GeneValueObject vo = new GeneValueObject();
        vo.setId( id );
        vo.setOfficialSymbol( symbol );
        vo.setAssociatedExperimentCount( eeCount );
        // All test SearchResults share the same Lucene score so the within-band tiebreaker
        // (the comparator under test) is what determines order.
        return SearchResult.from( Gene.class, vo, 0.9, null, "test" );
    }
}
