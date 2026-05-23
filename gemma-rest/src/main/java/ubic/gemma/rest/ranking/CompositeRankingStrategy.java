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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Combined ranker that blends all three primary signals into a single score per hit:
 * <pre>
 *   score = coverageWeight * tokenCoverageFraction(query, hit.value)
 *         + usageWeight    * normalizedLog(hit.usageCount)
 *         + rankWeight     * 1 / (1 + originalRank)
 * </pre>
 * <p>
 * The usage component is normalised against {@link #MAX_EXPECTED_USAGE} via {@code log1p}
 * so that a single very-high-usage term cannot drown out the other two signals. The cap is
 * picked at 100 because the staging fixture's top usageCount was 44 ("alcohol abuse"); a
 * 2-3x headroom over the observed maximum is the saturation point we want.
 * <p>
 * Defaults favour coverage (the most semantically meaningful signal) over usage (a popularity
 * prior) over rank (a relevance fallback). Override via
 * {@code gemma.rest.annotationSearch.composite.{coverage,usage,rank}Weight}.
 * <p>
 * Ties resolve by original Lucene rank ascending (stable relative to the input order). Empty /
 * blank queries make the coverage component zero for every hit, so usage + rank decide the order.
 */
@Component("composite")
public class CompositeRankingStrategy implements AnnotationSearchRankingStrategy {

    public static final String NAME = "composite";

    /**
     * Saturation point for {@code normalizedLog(usageCount)}. Picked at 100 to give ~2x headroom
     * over the staging fixture's observed max usageCount of 44 ("alcohol abuse" / MONDO_0002046).
     */
    static final int MAX_EXPECTED_USAGE = 100;

    private static final double LOG1P_MAX_USAGE = Math.log1p( MAX_EXPECTED_USAGE );

    private final double coverageWeight;
    private final double usageWeight;
    private final double rankWeight;

    public CompositeRankingStrategy(
            @Value("${gemma.rest.annotationSearch.composite.coverageWeight:0.5}") double coverageWeight,
            @Value("${gemma.rest.annotationSearch.composite.usageWeight:0.3}") double usageWeight,
            @Value("${gemma.rest.annotationSearch.composite.rankWeight:0.2}") double rankWeight ) {
        this.coverageWeight = coverageWeight;
        this.usageWeight = usageWeight;
        this.rankWeight = rankWeight;
    }

    @Override
    public List<CharacteristicValueObject> rank( String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri ) {
        Set<String> tokens = tokenise( originalQuery );
        int n = rawHits.size();
        List<Scored> scored = new ArrayList<>( n );
        for ( int i = 0; i < n; i++ ) {
            CharacteristicValueObject hit = rawHits.get( i );
            double coverage = tokens.isEmpty() ? 0.0 : tokenCoverageFraction( hit, tokens );
            int usage = 0;
            String uri = hit.getValueUri();
            if ( uri != null && usageCountsByUri != null ) {
                Integer u = usageCountsByUri.get( uri );
                if ( u != null ) {
                    usage = u;
                }
            }
            double usageNorm = normalizedLog( usage );
            double rankComponent = 1.0 / ( 1.0 + i );
            double score = coverageWeight * coverage
                    + usageWeight * usageNorm
                    + rankWeight * rankComponent;
            scored.add( new Scored( hit, i, score ) );
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

    /**
     * Saturating log normalisation: {@code min(1, log1p(usage) / log1p(MAX_EXPECTED_USAGE))}.
     * Returns a value in [0, 1].
     */
    private static double normalizedLog( int usageCount ) {
        if ( usageCount <= 0 ) {
            return 0.0;
        }
        return Math.min( 1.0, Math.log1p( usageCount ) / LOG1P_MAX_USAGE );
    }

    private static Set<String> tokenise( String query ) {
        if ( query == null || query.trim().isEmpty() ) {
            return Collections.emptySet();
        }
        String[] parts = query.toLowerCase( Locale.ROOT ).trim().split( "\\s+" );
        if ( parts.length == 0 ) {
            return Collections.emptySet();
        }
        Set<String> out = new LinkedHashSet<>( parts.length );
        out.addAll( Arrays.asList( parts ) );
        out.removeIf( String::isEmpty );
        return out;
    }

    private static double tokenCoverageFraction( CharacteristicValueObject hit, Set<String> tokens ) {
        String value = hit.getValue();
        if ( value == null || value.isEmpty() ) {
            return 0.0;
        }
        String lower = value.toLowerCase( Locale.ROOT );
        int matched = 0;
        for ( String tok : tokens ) {
            if ( lower.contains( tok ) ) {
                matched++;
            }
        }
        return ( double ) matched / ( double ) tokens.size();
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
