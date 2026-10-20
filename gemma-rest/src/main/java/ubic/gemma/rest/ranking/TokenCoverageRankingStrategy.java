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
 * Re-orders hits by what fraction of the query tokens appear in the hit's lowercased value.
 * Primary sort: coverage descending. Secondary sort: original Lucene rank ascending (stable
 * relative to the input order for hits with identical coverage).
 * <p>
 * Empty / blank queries are short-circuited: the input order is returned unchanged so the
 * caller's choice of upstream ordering wins.
 */
@Component("coverage")
public class TokenCoverageRankingStrategy implements AnnotationSearchRankingStrategy {

    public static final String NAME = "coverage";

    @Override
    public List<CharacteristicValueObject> rank( String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri ) {
        if ( originalQuery == null || originalQuery.trim().isEmpty() ) {
            return new ArrayList<>( rawHits );
        }
        Set<String> tokens = tokenise( originalQuery );
        if ( tokens.isEmpty() ) {
            return new ArrayList<>( rawHits );
        }
        int n = rawHits.size();
        List<Scored> scored = new ArrayList<>( n );
        for ( int i = 0; i < n; i++ ) {
            CharacteristicValueObject hit = rawHits.get( i );
            scored.add( new Scored( hit, i, coverage( hit, tokens ) ) );
        }
        scored.sort( Comparator
                .comparingDouble( ( Scored s ) -> s.coverage ).reversed()
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
     * Content tokens, shared with the relevance tiers via {@link QueryTokens}. Was a bare
     * whitespace split; since coverage is scored by substring containment, an unstripped
     * {@code the} scored against <em>theca cell</em>.
     */
    private static Set<String> tokenise( String query ) {
        return new LinkedHashSet<>( QueryTokens.contentTokens( query ) );
    }

    private static double coverage( CharacteristicValueObject hit, Set<String> tokens ) {
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
        final double coverage;

        Scored( CharacteristicValueObject hit, int originalRank, double coverage ) {
            this.hit = hit;
            this.originalRank = originalRank;
            this.coverage = coverage;
        }
    }
}
