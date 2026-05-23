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
     * Short stable name used as the value of the {@code ?rank=} query parameter and as the bean
     * name in the strategy registry. Lowercase, single word.
     */
    String getName();
}
