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
import ubic.gemma.rest.ranking.LuceneOrderRankingStrategy;
import ubic.gemma.rest.ranking.TokenCoverageRankingStrategy;
import ubic.gemma.rest.ranking.UsageWeightedRankingStrategy;

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

    private static int indexOfUri( List<CharacteristicValueObject> list, String uri ) {
        for ( int i = 0; i < list.size(); i++ ) {
            if ( uri.equals( list.get( i ).getValueUri() ) ) {
                return i;
            }
        }
        return -1;
    }
}
