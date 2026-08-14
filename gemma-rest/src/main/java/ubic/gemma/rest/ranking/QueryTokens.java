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

import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * The one query tokeniser the annotation-search stack uses.
 *
 * <p>Moved here from {@code AnnotationsWebService}, which had the careful implementation, while
 * {@link CompositeRankingStrategy} and {@link TokenCoverageRankingStrategy} each carried their own
 * {@code query.toLowerCase().split("\\s+")}. That is worse than it sounds, because coverage is
 * scored by substring containment: with no stop-word strip, {@code the} in
 * "cell line of the liver" scores against <em>theca cell</em> and {@code of} against
 * <em>profile</em>, handing free coverage to labels that share nothing but two letters. On a real
 * gold pair, "epithelium of esophagus", the {@code of} token did exactly that.</p>
 */
public final class QueryTokens {

    /**
     * Conservative stop-word list. Tokens this generic don't carry meaning for ontology lookup.
     * Lucene's StandardAnalyzer already removes most; this set covers the cases where we tokenise
     * client-side (the token-coverage filter and the coverage rankers) before the query has been
     * through Lucene's analyzer.
     */
    private static final java.util.Set<String> SEARCH_STOP_WORDS = java.util.Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if",
            "in", "into", "is", "it", "of", "on", "or", "such", "that", "the",
            "their", "then", "there", "these", "they", "this", "to", "was",
            "will", "with"
    );

    /**
     * Minimum length for a token to be considered "content". Single characters drop out — they're
     * either part numbers ({@code "2"} in {@code "uzh 2 cell"}) that survive Lucene's analyser but
     * don't help filter candidates, or stop-words.
     */
    private static final int MIN_CONTENT_TOKEN_LENGTH = 2;

    /**
     * Tokenise an arbitrary user query into "content" tokens: lowercase, split on runs of
     * non-alphanumeric characters, drop tokens shorter than {@link #MIN_CONTENT_TOKEN_LENGTH},
     * drop stop-words.
     *
     * <p>Returned in encounter order, deduplicated; empty list when the input is null / blank /
     * all-stop-words. Callers should treat an empty list as "no token-coverage constraint applies —
     * fall back to Lucene's order".</p>
     */
    public static List<String> contentTokens( @Nullable String query ) {
        if ( query == null ) return Collections.emptyList();
        String lower = query.toLowerCase( Locale.ROOT );
        String[] parts = lower.split( "[^a-z0-9]+" );
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for ( String p : parts ) {
            if ( p.length() < MIN_CONTENT_TOKEN_LENGTH ) continue;
            if ( SEARCH_STOP_WORDS.contains( p ) ) continue;
            seen.add( p );
        }
        return new java.util.ArrayList<>( seen );
    }

    private QueryTokens() {
    }
}
