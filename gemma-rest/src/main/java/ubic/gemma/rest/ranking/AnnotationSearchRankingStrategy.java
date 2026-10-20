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

import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.List;
import java.util.Map;

/**
 * Strategy hook for re-ordering a flat list of annotation-search hits before they are wrapped in
 * the response value-object and returned to the client.
 * <p>
 * Implementations get the original query string, the Lucene-ordered hits (original index = list
 * position), and a per-URI usage-count map (number of distinct experiments that reference each
 * URI). They must return a new list — same hits, possibly re-ordered — and must be stable and
 * side-effect free. They must not mutate the input list.
 * <p>
 * See {@code handoffs/RECCE_ANNOTATION_SEARCH_RANKING.md} for the motivating design.
 *
 * @see LuceneOrderRankingStrategy default no-op
 * @see UsageWeightedRankingStrategy pulls high-usage terms forward
 * @see TokenCoverageRankingStrategy bumps hits covering more query tokens
 */
public interface AnnotationSearchRankingStrategy {

    /**
     * Re-order {@code rawHits} according to this strategy.
     *
     * @param originalQuery     the original query string the user typed; whitespace-tokenised
     *                          and lowercased by token-coverage strategies; may be blank.
     * @param rawHits           the Lucene-ordered hits; index in this list is the "original rank"
     *                          used by rank-aware strategies. Must not be mutated.
     * @param usageCountsByUri  per-URI count of distinct experiments referencing the URI; may
     *                          be empty.
     * @return a new list of the same hits in the desired display order.
     */
    List<CharacteristicValueObject> rank(
            String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri );

    /**
     * Re-order {@code rawHits} with the per-string corpus prior available in addition to the usage
     * counts.
     * <p>
     * Callers should invoke this overload; it defaults to the three-argument {@link #rank} so a
     * strategy that has no use for the prior needs no changes. Only strategies that return
     * {@code true} from {@link #requiresStringPrior()} receive a populated map.
     *
     * @param stringPriorByUri per-URI count of distinct experiments on which a prior curator wrote
     *                         the query string itself as the annotation's original value; may be
     *                         empty. Distinct from {@code usageCountsByUri}, which counts every use
     *                         of the URI regardless of what was written.
     */
    default List<CharacteristicValueObject> rank(
            String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri,
            Map<String, Integer> stringPriorByUri ) {
        return rank( originalQuery, rawHits, usageCountsByUri );
    }

    /**
     * Re-order {@code rawHits} with the per-URI matched text available in addition to the counts.
     * <p>
     * Callers should invoke this overload; it defaults to the four-argument {@link #rank} so a
     * strategy with no use for the matched text needs no changes.
     * <p>
     * This exists because a coverage-scoring strategy that reads only {@code hit.getValue()} scores
     * <strong>zero</strong> for a hit that matched through a synonym — its label shares nothing
     * with the query, which is the entire reason the synonym exists. {@code dmso} finding
     * <em>dimethyl sulfoxide</em> worked only by accident, both candidates scoring 0 coverage so
     * usage broke the tie. The attribution pass already computes this string for the whole
     * candidate set before truncation, so passing it costs nothing.
     *
     * @param matchedTextByUri per-URI text that actually matched the query — a preferred label, a
     *                         declared synonym, or an alternate label — as reported by
     *                         {@code matchedText}; may be empty, and may omit URIs whose
     *                         attribution could not be resolved.
     */
    default List<CharacteristicValueObject> rank(
            String originalQuery,
            List<CharacteristicValueObject> rawHits,
            Map<String, Integer> usageCountsByUri,
            Map<String, Integer> stringPriorByUri,
            Map<String, String> matchedTextByUri ) {
        return rank( originalQuery, rawHits, usageCountsByUri, stringPriorByUri );
    }

    /**
     * Short stable name used as the value of the {@code ?rank=} query parameter and as the bean
     * name in the strategy registry. Lowercase, single word.
     */
    String getName();

    /**
     * Whether this strategy reads the {@code usageCountsByUri} map during {@link #rank}.
     * Default {@code false}; strategies that do (usage, composite) override to {@code true}.
     * When {@code false}, callers can skip the count lookup entirely for the candidate set
     * and only compute counts for the truncated top-N display payload — turning the dominant
     * cost on the typeahead path (~2-3s for a 400-1000 candidate IN-clause against the
     * characteristic-by-uri index) into a much cheaper top-N query.
     */
    default boolean requiresUsageCounts() {
        return false;
    }

    /**
     * Whether this strategy reads the {@code stringPriorByUri} map during {@link #rank}.
     * Default {@code false}; {@link CommonalityRankingStrategy} overrides to {@code true}.
     * <p>
     * Kept separate from {@link #requiresUsageCounts()} rather than folded into one "needs corpus
     * stats" flag because the two queries have different costs and answer different questions: a
     * strategy that wants the per-string prior should not be made to pay for the usage scan, or
     * the other way round.
     */
    default boolean requiresStringPrior() {
        return false;
    }
}
