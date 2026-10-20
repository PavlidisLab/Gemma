/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.model.common.search;

/**
 * How a {@link SearchResult} matched the query — the structured discriminant that a
 * {@code double} score cannot carry on its own.
 * <p>
 * The motivating case: {@code MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE} (0.9) and
 * {@code MATCH_BY_ALIAS_SCORE} (0.90) are numerically identical, so a client gating on
 * "score &lt; 1.0" cannot tell a safe alias hit (commit) from a bare prefix look-alike
 * (reject). For example the query {@code ANKA} (a Plasmodium strain, not a gene) matches
 * {@code Ankar} only because {@code ANKA} is a prefix of that longer symbol; that hit is
 * {@link #SYMBOL_PREFIX} and should not be auto-committed, whereas {@link #ALIAS} and
 * {@link #EXACT_SYMBOL} are safe. The score alone hides this; the match type surfaces it.
 * <p>
 * {@code getWireName()} tokens match the vocabulary the curation-UI / agent clients expect.
 * Only the kinds Gemma can actually produce are enumerated — there is no
 * {@code edit_distance} or {@code substring} value because the database gene path never
 * generates those (Levenshtein is a ranking tiebreaker, not a match generator, and a
 * leading-wildcard substring only fires on explicit user syntax).
 *
 * @author poirigui
 */
public enum SearchMatchType {

    /**
     * Matched a canonical identifier — numeric primary key, accession, NCBI/Ensembl gene id,
     * dataset/platform short name. High trust; resolves to at most one entity. Corresponds to
     * {@link SearchResult#isExactIdentifierMatch()}.
     */
    EXACT_IDENTIFIER( "exact_identifier" ),

    /**
     * Exact match on an official gene symbol. High trust.
     */
    EXACT_SYMBOL( "exact_symbol" ),

    /**
     * Inexact <em>prefix</em> match on an official symbol (SQL {@code LIKE 'foo%'}). LOW trust:
     * a short query can be a prefix of an unrelated longer symbol ({@code ANKA} → {@code Ankar}).
     * Treat as a candidate, not a commit, unless the query is itself a registered symbol/alias.
     */
    SYMBOL_PREFIX( "prefix" ),

    /**
     * Exact match on a registered gene alias/synonym. Safe to commit.
     */
    ALIAS( "alias" ),

    /**
     * Exact match on an official (long) name. Safe to commit.
     */
    OFFICIAL_NAME( "official_name" ),

    /**
     * Inexact match on an official name. Lower trust, like {@link #SYMBOL_PREFIX}.
     */
    OFFICIAL_NAME_PREFIX( "official_name_prefix" );

    private final String wireName;

    SearchMatchType( String wireName ) {
        this.wireName = wireName;
    }

    /**
     * Stable snake-case token for JSON wire contracts (matches the curation-UI / agent vocabulary).
     */
    public String getWireName() {
        return wireName;
    }
}
