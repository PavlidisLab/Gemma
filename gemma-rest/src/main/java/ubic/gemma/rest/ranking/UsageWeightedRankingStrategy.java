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
 * Blends Lucene rank with the per-URI usage count: pulls terms that are actually being applied
 * to experiments toward the top without abandoning relevance order entirely.
 * <p>
 * score = {@code rankWeight * (1 / (1 + originalRank)) + usageWeight * log(1 + usageCount)}.
 * Higher score sorts earlier. Ties resolve by original Lucene rank ascending (stable).
 */
@Component("usage")
public class UsageWeightedRankingStrategy implements AnnotationSearchRankingStrategy {

    public static final String NAME = "usage";

    private final double rankWeight;
    private final double usageWeight;

    public UsageWeightedRankingStrategy(
            @Value("${gemma.rest.annotationSearch.usage.rankWeight:0.5}") double rankWeight,
            @Value("${gemma.rest.annotationSearch.usage.usageWeight:0.5}") double usageWeight ) {
        this.rankWeight = rankWeight;
        this.usageWeight = usageWeight;
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
            double usageComponent = usageWeight * Math.log( 1.0 + usage );
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
