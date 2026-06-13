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
package ubic.gemma.rest.ranking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Blends Lucene rank with the per-URI usage count.
 *
 * <p>Usage is a <strong>confidence signal</strong>, not a popularity contest: that Gemma
 * uses a term at all (usage &gt; 0) is the load-bearing bit. Magnitude provides a small
 * additional boost but saturates quickly so a high-usage but loosely-related hit can't
 * stomp a strong lexical match with usage 0 or 1.</p>
 *
 * <p>Score: {@code rankWeight * (1 / (1 + originalRank)) + usageWeight * usagePresenceScore(usage)},
 * where {@code usagePresenceScore} returns {@code 0} when usage is 0 and a value in
 * {@code [presenceFloor, 1.0]} when usage is positive — starting at {@code presenceFloor}
 * (default 0.7) for usage=1 and saturating at 1.0 around
 * {@code usageSaturation} (default 10). Higher score sorts earlier. Ties resolve by
 * original Lucene rank ascending (stable).</p>
 *
 * <p>Rationale: with the previous {@code log(1 + usage)} formulation a hit with usage=127
 * scored {@code 0.5 + 0.5 * log(128) ≈ 2.9}, dwarfing a strong lexical match with usage=0
 * scoring {@code 0.5}. After this change the same hit caps at {@code 0.5 + 0.5 * 1.0 = 1.0},
 * comparable to the rank component for top-rank candidates. See
 * {@code handoffs/ANNOTATIONS_SEARCH_OR_OVER_TOKENS_2026_06_12.md} and the user note:
 * "popular helps the agent have confidence the term is the right one to use; it wasn't the
 * intention that that would be a popularity contest".</p>
 */
@Component("usage")
public class UsageWeightedRankingStrategy implements AnnotationSearchRankingStrategy {

    public static final String NAME = "usage";

    private final double rankWeight;
    private final double usageWeight;
    private final double presenceFloor;
    private final int usageSaturation;

    public UsageWeightedRankingStrategy(
            @Value("${gemma.rest.annotationSearch.usage.rankWeight:0.5}") double rankWeight,
            @Value("${gemma.rest.annotationSearch.usage.usageWeight:0.5}") double usageWeight,
            @Value("${gemma.rest.annotationSearch.usage.presenceFloor:0.7}") double presenceFloor,
            @Value("${gemma.rest.annotationSearch.usage.saturation:10}") int usageSaturation ) {
        this.rankWeight = rankWeight;
        this.usageWeight = usageWeight;
        this.presenceFloor = presenceFloor;
        this.usageSaturation = Math.max( 1, usageSaturation );
    }

    /**
     * Convenience constructor that defaults presence-floor and usage-saturation to the
     * production values (0.7 / 10). For tests and any callers that don't want to pin all
     * four knobs.
     */
    public UsageWeightedRankingStrategy( double rankWeight, double usageWeight ) {
        this( rankWeight, usageWeight, 0.7, 10 );
    }

    @Override
    public List<CharacteristicValueObject> rank( String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri ) {
        int n = rawHits.size();
        List<Scored> scored = new ArrayList<>( n );
        for ( int i = 0; i < n; i++ ) {
            CharacteristicValueObject hit = rawHits.get( i );
            int usage = 0;
            String uri = hit.getValueUri();
            if ( uri != null ) {
                Integer u = usageCountsByUri.get( uri );
                if ( u != null ) {
                    usage = u;
                }
            }
            double rankComponent = rankWeight * ( 1.0 / ( 1.0 + i ) );
            double usageComponent = usageWeight * usagePresenceScore( usage );
            scored.add( new Scored( hit, i, rankComponent + usageComponent ) );
        }
        scored.sort( Comparator
                .comparingDouble( ( Scored s ) -> s.score ).reversed()
                .thenComparingInt( s -> s.originalRank ) );
        List<CharacteristicValueObject> out = new ArrayList<>( n );
        for ( Scored s : scored ) {
            out.add( s.hit );
        }
        return out;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean requiresUsageCounts() {
        return true;
    }

    /**
     * Map raw usage count to a bounded confidence score in {@code [0, 1]}.
     * <ul>
     *   <li>{@code usage == 0} → {@code 0.0} (no confidence boost — Gemma doesn't use this term)</li>
     *   <li>{@code usage > 0} → at least {@code presenceFloor} (the term <em>is</em> used)</li>
     *   <li>Magnitude scales {@code log(1+usage) / log(1+saturation)}, capped at 1.0</li>
     * </ul>
     * Visible for testing.
     */
    static double usagePresenceScore( int usage, double presenceFloor, int usageSaturation ) {
        if ( usage <= 0 ) return 0.0;
        double magnitudeRange = 1.0 - presenceFloor;
        double saturating = Math.min( 1.0,
                Math.log1p( usage ) / Math.log1p( usageSaturation ) );
        return presenceFloor + magnitudeRange * saturating;
    }

    private double usagePresenceScore( int usage ) {
        return usagePresenceScore( usage, presenceFloor, usageSaturation );
    }

    private static final class Scored {
        final CharacteristicValueObject hit;
        final int originalRank;
        final double score;

        Scored( CharacteristicValueObject hit, int originalRank, double score ) {
            this.hit = hit;
            this.originalRank = originalRank;
            this.score = score;
        }
    }
}
