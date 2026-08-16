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
package ubic.gemma.core.ontology.search;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * Shared query shaping for the ontology Lucene indexes.
 */
public final class OntologyQueries {

    private OntologyQueries() {
    }

    /**
     * Default share of a query's terms a document must carry to be a candidate.
     *
     * <p>0.67 rather than 1.0 on the recorded pilot: over 60 TUNE queries the two were
     * indistinguishable on MRR (~1pp) while full AND is the more brittle of the pair, since a
     * single token absent from an otherwise correct term drops it entirely.</p>
     */
    public static final double DEFAULT_MIN_SHOULD_MATCH = 0.67;

    /**
     * Require a document to carry a share of the query's terms, instead of any one of them.
     *
     * <p>{@link org.apache.lucene.queryparser.classic.QueryParser} defaults to OR, so every clause
     * is optional and a document matches on a single shared token. On an ontology index that is not
     * a lax setting so much as an absent one: {@code Gorlin Goltz Syndrome} returned
     * <b>{@code down syndrome}</b> as its first MONDO hit, and {@code Myelopathy} returned
     * {@code spinal cord injury} — confident, wrong, and rank 1, because "syndrome" alone was
     * enough to be retrieved. Two different queries sharing one common word returned the same
     * terms.</p>
     *
     * <p>Applied at the top level only, and only when every clause is SHOULD. A query carrying
     * explicit {@code +}/{@code -} operators is the caller stating their own requirements, and
     * those are left exactly as written. Nested clauses are untouched: this constrains how many of
     * the user's terms must appear, not how each one matches.</p>
     *
     * <p>Floor semantics, as Solr's {@code mm} uses: 3 terms require 2, 4 require 2, 6 require 4.
     * The one departure is a floor of two for any multi-term query — 67% of two rounds down to
     * one, which would leave the commonest shape (two-word disease names) exactly as unconstrained
     * as before.</p>
     *
     * @param query the parsed query
     * @param ratio share of terms required, clamped to (0, 1]
     * @return the query with a minimum-should-match applied, or {@code query} unchanged when it has
     *         no top-level optional clauses to constrain
     */
    public static Query withMinimumShouldMatch( Query query, double ratio ) {
        if ( !( query instanceof BooleanQuery ) ) {
            return query;
        }
        BooleanQuery bq = ( BooleanQuery ) query;
        if ( bq.getMinimumNumberShouldMatch() > 0 ) {
            return query;   // already constrained; do not second-guess it
        }
        int shouldClauses = 0;
        for ( BooleanClause clause : bq.clauses() ) {
            if ( clause.getOccur() != BooleanClause.Occur.SHOULD ) {
                // MUST / MUST_NOT / FILTER means the caller expressed their own requirements.
                return query;
            }
            shouldClauses++;
        }
        int required = requiredClauses( shouldClauses, ratio );
        if ( required <= 1 ) {
            return query;
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for ( BooleanClause clause : bq.clauses() ) {
            builder.add( clause );
        }
        builder.setMinimumNumberShouldMatch( required );
        return builder.build();
    }

    /**
     * How many of {@code n} optional clauses a document must carry.
     *
     * @return 1 for a single-clause query (nothing to constrain), otherwise at least 2 and never
     *         more than {@code n}
     */
    static int requiredClauses( int n, double ratio ) {
        if ( n <= 1 ) {
            return n;
        }
        double r = Math.min( Math.max( ratio, 0.0 ), 1.0 );
        int required = ( int ) Math.floor( r * n );
        return Math.max( 2, Math.min( n, required ) );
    }
}
