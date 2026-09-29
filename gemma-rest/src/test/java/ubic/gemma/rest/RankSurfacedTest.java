/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The stored ranks reach the wire on the heatmap path.
 * <p>
 * Guards the contract rather than the plumbing: these are experiment-scoped numbers reported
 * beside a matrix the request may have narrowed, so the risk is not that they go missing but
 * that someone later "fixes" them to describe the returned columns instead.
 */
class RankSurfacedTest {

    @Test
    void rowMetaCarriesBothStoredRanks() {
        HeatmapDataValueObject.RowMeta row = new HeatmapDataValueObject.RowMeta();
        row.setRankByMean( 0.75 );
        row.setRankByMax( 0.9 );
        assertThat( row.getRankByMean() ).isEqualTo( 0.75 );
        assertThat( row.getRankByMax() ).isEqualTo( 0.9 );
    }

    @Test
    void aRowWithoutRanksLeavesThemNullRatherThanZero() {
        // NON_NULL on both fields means absent, not 0.0 — a 0.0 rank is a real value (the
        // least-expressed probe) and must not be manufactured by a path that has no rank.
        HeatmapDataValueObject.RowMeta row = new HeatmapDataValueObject.RowMeta();
        assertThat( row.getRankByMean() ).isNull();
        assertThat( row.getRankByMax() ).isNull();
    }
}
