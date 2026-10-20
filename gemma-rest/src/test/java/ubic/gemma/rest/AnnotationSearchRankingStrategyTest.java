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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.rest.ranking.CompositeRankingStrategy;
import ubic.gemma.rest.ranking.LuceneOrderRankingStrategy;
import ubic.gemma.rest.ranking.TokenCoverageRankingStrategy;
import ubic.gemma.rest.ranking.UsageWeightedRankingStrategy;
import java.util.Arrays;
import ubic.gemma.rest.ranking.QueryTokens;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-data tests for the three ranking strategies — no Spring, no Mockito beyond the input
 * fixture. The fixture is the 435-hit staging snapshot of {@code /annotations/search?query=chronic+itch}
 * captured in {@code RECCE_ANNOTATION_SEARCH_RANKING.md}.
 */
public class AnnotationSearchRankingStrategyTest {

    private static final String FIXTURE = "/data/annotations/search-chronic-itch.staging-2026-05-23.json";

    private static List<CharacteristicValueObject> hits;
    private static Map<String, Integer> usageCounts;
    /** Map from valueUri -> recorded lucene_rank for assertions. */
    private static Map<String, Integer> luceneRanks;

    @BeforeAll
    public static void loadFixture() throws IOException {
        ObjectMapper om = new ObjectMapper();
        try ( InputStream in = AnnotationSearchRankingStrategyTest.class.getResourceAsStream( FIXTURE ) ) {
            assertThat( in ).as( "fixture must be on classpath: %s", FIXTURE ).isNotNull();
            JsonNode root = om.readTree( in );
            JsonNode hitsArr = root.get( "hits" );
            hits = new ArrayList<>( hitsArr.size() );
            usageCounts = new HashMap<>( hitsArr.size() );
            luceneRanks = new HashMap<>( hitsArr.size() );
            for ( JsonNode h : hitsArr ) {
                String value = h.get( "value" ).asText();
                String valueUri = h.hasNonNull( "valueUri" ) ? h.get( "valueUri" ).asText() : null;
                String category = h.hasNonNull( "category" ) ? h.get( "category" ).asText() : null;
                String categoryUri = h.hasNonNull( "categoryUri" ) ? h.get( "categoryUri" ).asText() : null;
                int usageCount = h.hasNonNull( "usageCount" ) ? h.get( "usageCount" ).asInt() : 0;
                int luceneRank = h.get( "lucene_rank" ).asInt();
                CharacteristicValueObject vo = new CharacteristicValueObject( value, valueUri, category, categoryUri );
                hits.add( vo );
                if ( valueUri != null ) {
                    usageCounts.put( valueUri, usageCount );
                    luceneRanks.put( valueUri, luceneRank );
                }
            }
        }
    }

    @Test
    public void luceneOrder_keepsRawOrder() {
        List<CharacteristicValueObject> ranked = new LuceneOrderRankingStrategy().rank( "chronic itch", hits, usageCounts );
        assertThat( ranked ).hasSize( hits.size() );
        // The fixture's first 10 hits are exactly lucene_rank 0..9 — assert by valueUri equality.
        for ( int i = 0; i < 10; i++ ) {
            assertThat( ranked.get( i ).getValueUri() )
                    .as( "lucene-order position %d", i )
                    .isEqualTo( hits.get( i ).getValueUri() );
        }
    }

    @Test
    public void usageWeighted_pullsHighUsageForward() {
        // Highest-usage term in the fixture: "alcohol abuse" (MONDO_0002046), usageCount=44, lucene_rank=344.
        String highUsageUri = "http://purl.obolibrary.org/obo/MONDO_0002046";
        int originalRank = luceneRanks.get( highUsageUri );
        assertThat( usageCounts.get( highUsageUri ) ).isEqualTo( 44 );
        assertThat( originalRank ).isEqualTo( 344 );

        UsageWeightedRankingStrategy strategy = new UsageWeightedRankingStrategy( 0.3, 0.7 );
        List<CharacteristicValueObject> ranked = strategy.rank( "chronic itch", hits, usageCounts );

        int newPos = indexOfUri( ranked, highUsageUri );
        assertThat( newPos )
                .as( "MONDO_0002046 should move forward under usage-weighted ranking (was at %d)", originalRank )
                .isGreaterThanOrEqualTo( 0 )
                .isLessThan( originalRank );
        // No hits dropped.
        assertThat( ranked ).hasSize( hits.size() );
    }

    @Test
    public void tokenCoverage_putsBothTokensFirst() {
        // The real "chronic itch" query has no two-token-covering term in the fixture, but the
        // strategy still has to run cleanly on this input.
        TokenCoverageRankingStrategy strategy = new TokenCoverageRankingStrategy();
        List<CharacteristicValueObject> rankedNoMatch = strategy.rank( "chronic itch", hits, usageCounts );
        assertThat( rankedNoMatch ).hasSize( hits.size() );

        // Now plant a synthetic both-tokens-covered hit at the END of the input — it should land at position 0.
        List<CharacteristicValueObject> withSynthetic = new ArrayList<>( hits );
        CharacteristicValueObject synthetic = new CharacteristicValueObject(
                "chronic itch syndrome", "http://example.test/synthetic/chronic-itch" );
        withSynthetic.add( synthetic );

        List<CharacteristicValueObject> ranked = strategy.rank( "chronic itch", withSynthetic, usageCounts );
        assertThat( ranked ).hasSize( withSynthetic.size() );
        assertThat( ranked.get( 0 ).getValueUri() )
                .as( "synthetic two-token-covering hit should land at position 0" )
                .isEqualTo( "http://example.test/synthetic/chronic-itch" );
    }

    @Test
    public void tokenCoverage_handlesEmptyQuery() {
        TokenCoverageRankingStrategy strategy = new TokenCoverageRankingStrategy();
        List<CharacteristicValueObject> ranked = strategy.rank( "", hits, Collections.emptyMap() );
        assertThat( ranked ).hasSize( hits.size() );
        // Empty query => input order preserved.
        for ( int i = 0; i < Math.min( 10, hits.size() ); i++ ) {
            assertThat( ranked.get( i ).getValueUri() ).isEqualTo( hits.get( i ).getValueUri() );
        }
    }

    @Test
    public void composite_pullsHighCoveragePlusHighUsageToTop() {
        // Plant a synthetic hit that hits BOTH primary signals: full token coverage on "chronic itch"
        // AND a high usage count. It should outscore every native fixture hit (which all max out at
        // ~50% coverage on "chronic itch") and land at position 0.
        List<CharacteristicValueObject> withSynthetic = new ArrayList<>( hits );
        Map<String, Integer> usagePlus = new HashMap<>( usageCounts );
        CharacteristicValueObject synthetic = new CharacteristicValueObject(
                "chronic itch syndrome", "http://example.test/synthetic/chronic-itch-strong" );
        withSynthetic.add( synthetic );
        usagePlus.put( synthetic.getValueUri(), 50 );

        CompositeRankingStrategy strategy = new CompositeRankingStrategy( 0.5, 0.3, 0.2 );
        List<CharacteristicValueObject> ranked = strategy.rank( "chronic itch", withSynthetic, usagePlus );

        assertThat( ranked ).hasSize( withSynthetic.size() );
        assertThat( ranked.get( 0 ).getValueUri() )
                .as( "composite should pull dual-signal synthetic to position 0" )
                .isEqualTo( synthetic.getValueUri() );
    }

    @Test
    public void composite_handlesEmptyUsageMap() {
        // With no usage data the strategy degrades to a coverage+rank blend; must not blow up
        // or drop hits.
        CompositeRankingStrategy strategy = new CompositeRankingStrategy( 0.5, 0.3, 0.2 );
        List<CharacteristicValueObject> ranked = strategy.rank( "chronic itch", hits, Collections.emptyMap() );
        assertThat( ranked ).hasSize( hits.size() );
    }

    @Test
    public void composite_handlesEmptyQuery() {
        // Empty query => coverage component is 0 for every hit; usage + rank decide. Must not throw.
        CompositeRankingStrategy strategy = new CompositeRankingStrategy( 0.5, 0.3, 0.2 );
        List<CharacteristicValueObject> ranked = strategy.rank( "", hits, usageCounts );
        assertThat( ranked ).hasSize( hits.size() );
    }

    @Test
    public void composite_differsFromBothComponentsOnMixedSignal() {
        // Composite output should not coincide exactly with either coverage-only or usage-only
        // ordering on the staging fixture — proves the blend is doing real work.
        CompositeRankingStrategy composite = new CompositeRankingStrategy( 0.5, 0.3, 0.2 );
        TokenCoverageRankingStrategy coverage = new TokenCoverageRankingStrategy();
        UsageWeightedRankingStrategy usage = new UsageWeightedRankingStrategy( 0.3, 0.7 );

        List<String> compositeOrder = uriOrder( composite.rank( "chronic itch", hits, usageCounts ) );
        List<String> coverageOrder = uriOrder( coverage.rank( "chronic itch", hits, usageCounts ) );
        List<String> usageOrder = uriOrder( usage.rank( "chronic itch", hits, usageCounts ) );

        assertThat( compositeOrder )
                .as( "composite must differ from coverage-only on mixed-signal fixture" )
                .isNotEqualTo( coverageOrder );
        assertThat( compositeOrder )
                .as( "composite must differ from usage-only on mixed-signal fixture" )
                .isNotEqualTo( usageOrder );
    }

    @Test
    public void usageWeighted_neverDisplacesTheTopLexicalHit() {
        // The property the production defaults exist to hold, and the one whose absence produced
        // `malignant melanoma` -> `gastric cancer`. The usage term tops out at usageWeight; the
        // rank term is rankWeight for the hit at position 0. Keep usageWeight below rankWeight and
        // no amount of corpus popularity can take position 0 from an exact lexical match; let them
        // meet and the ranker quietly becomes "every used term above every unused one".
        //
        // Deliberately built with the PRODUCTION constructor (no weights passed) so this fails if
        // someone retunes past the invariant rather than only if they change this test.
        CharacteristicValueObject topLexical = new CharacteristicValueObject( "melanoma",
                "http://example.org/TOP_LEXICAL", null, null );
        CharacteristicValueObject wildlyPopular = new CharacteristicValueObject( "breast cancer",
                "http://example.org/POPULAR", null, null );
        List<CharacteristicValueObject> input = new ArrayList<>();
        input.add( topLexical );
        for ( int i = 0; i < 50; i++ ) {
            input.add( new CharacteristicValueObject( "filler " + i,
                    "http://example.org/FILLER_" + i, null, null ) );
        }
        input.add( wildlyPopular );
        Map<String, Integer> counts = new HashMap<>();
        counts.put( "http://example.org/TOP_LEXICAL", 0 );
        counts.put( "http://example.org/POPULAR", 100000 );

        List<CharacteristicValueObject> ranked =
                new UsageWeightedRankingStrategy( 0.5, 0.3, 0.2, 100 ).rank( "melanoma", input, counts );

        assertThat( indexOfUri( ranked, "http://example.org/TOP_LEXICAL" ) )
                .as( "a usage-0 exact match at rank 0 must survive a usage-100000 hit 51 places below it" )
                .isEqualTo( 0 );
        assertThat( indexOfUri( ranked, "http://example.org/POPULAR" ) )
                .as( "usage must still lift the popular hit above the unused filler it started behind" )
                .isEqualTo( 1 );
    }

    @Test
    public void composite_scoresCoverageAgainstTheMatchedSynonymNotJustTheLabel() {
        // `dmso` shares no token with "dimethyl sulfoxide", so label-only coverage scored the right
        // answer at 0.0 -- identical to every irrelevant hit -- and it led on gemma2 only because
        // the deuterated variant also scored 0 and usage broke the tie. Give the ranker the string
        // that actually matched and the synonym hit is scored on what it matched.
        CharacteristicValueObject deuterated = new CharacteristicValueObject( "dimethyl sulfoxide-d6",
                "http://purl.obolibrary.org/obo/CHEBI_D6", null, null );
        CharacteristicValueObject dmso = new CharacteristicValueObject( "dimethyl sulfoxide",
                "http://purl.obolibrary.org/obo/CHEBI_28262", null, null );
        // Lucene order puts the deuterated variant first, as it does live.
        List<CharacteristicValueObject> hits = Arrays.asList( deuterated, dmso );
        Map<String, Integer> noCounts = Collections.emptyMap();
        CompositeRankingStrategy composite = new CompositeRankingStrategy( 0.5, 0.3, 0.2 );

        assertThat( indexOfUri( composite.rank( "dmso", hits, noCounts ), "http://purl.obolibrary.org/obo/CHEBI_28262" ) )
                .as( "without the matched text, coverage is 0 for both and Lucene order stands" )
                .isEqualTo( 1 );

        Map<String, String> matched = new HashMap<>();
        matched.put( "http://purl.obolibrary.org/obo/CHEBI_28262", "DMSO" );
        assertThat( indexOfUri( composite.rank( "dmso", hits, noCounts, Collections.emptyMap(), matched ),
                "http://purl.obolibrary.org/obo/CHEBI_28262" ) )
                .as( "the hit whose declared synonym IS the query must lead" )
                .isEqualTo( 0 );
    }

    @Test
    public void queryTokens_dropStopWordsThatCoverageWouldOtherwiseMatchAsSubstrings() {
        // Coverage is substring containment, so an unstripped stop-word is not merely noise: `the`
        // scores against "theca cell" and `of` against "profile". "epithelium of esophagus" is a
        // real curated label, and its `of` handed a third of the score to any label with those two
        // letters anywhere.
        assertThat( QueryTokens.contentTokens( "epithelium of esophagus" ) )
                .containsExactly( "epithelium", "esophagus" );
        assertThat( QueryTokens.contentTokens( "cell line of the liver" ) )
                .containsExactly( "cell", "line", "liver" );
        // Single characters go too; identifier queries still split into usable content tokens.
        assertThat( QueryTokens.contentTokens( "MK-2206" ) ).containsExactly( "mk", "2206" );
        assertThat( QueryTokens.contentTokens( "  " ) ).isEmpty();
        assertThat( QueryTokens.contentTokens( null ) ).isEmpty();
    }

    private static List<String> uriOrder( List<CharacteristicValueObject> list ) {
        List<String> out = new ArrayList<>( list.size() );
        for ( CharacteristicValueObject vo : list ) {
            out.add( vo.getValueUri() );
        }
        return out;
    }

    private static int indexOfUri( List<CharacteristicValueObject> list, String uri ) {
        for ( int i = 0; i < list.size(); i++ ) {
            if ( uri.equals( list.get( i ).getValueUri() ) ) {
                return i;
            }
        }
        return -1;
    }
}
