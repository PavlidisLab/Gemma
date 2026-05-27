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
package ubic.gemma.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the secondary-sort behaviour of {@link GeneWebService#searchGenes} — the within-score-band
 * tiebreaker that lifts {@code Trp53} above unrelated alias-list members ({@code Hipk2},
 * {@code Muc1}, {@code Bcl3}) for query {@code tp53}. The end-to-end path is tested by
 * {@link GeneWebServiceTest}; this class exercises the pure tier / edit-distance helpers so
 * regressions are caught without spinning up a Spring context.
 */
class GeneSearchRankingTest {

    @Test
    void symbolTier_classifiesByQueryRelationship() {
        assertEquals( 0, GeneWebService.symbolTier( "trp53", "trp53" ) );  // exact
        assertEquals( 1, GeneWebService.symbolTier( "trp53b", "trp53" ) ); // startsWith
        assertEquals( 2, GeneWebService.symbolTier( "atrp53", "trp53" ) ); // endsWith
        assertEquals( 3, GeneWebService.symbolTier( "atrp53b", "trp53" ) );// contains (interior)
        assertEquals( 4, GeneWebService.symbolTier( "hipk2", "tp53" ) );   // no overlap → alias-only
        assertEquals( 5, GeneWebService.symbolTier( null, "tp53" ) );      // no symbol
    }

    @Test
    void editDistance_lifts_Trp53_above_unrelated_alias_hits_for_tp53() {
        String q = "tp53";
        int trp53 = GeneWebService.editDistanceOrMax( "trp53", q );
        int hipk2 = GeneWebService.editDistanceOrMax( "hipk2", q );
        int muc1 = GeneWebService.editDistanceOrMax( "muc1", q );
        int bcl3 = GeneWebService.editDistanceOrMax( "bcl3", q );
        assertTrue( trp53 < hipk2, "Trp53(d=" + trp53 + ") should be closer to tp53 than Hipk2(d=" + hipk2 + ")" );
        assertTrue( trp53 < muc1, "Trp53(d=" + trp53 + ") should be closer to tp53 than Muc1(d=" + muc1 + ")" );
        assertTrue( trp53 < bcl3, "Trp53(d=" + trp53 + ") should be closer to tp53 than Bcl3(d=" + bcl3 + ")" );
    }

    @Test
    void editDistance_nullSymbol_sinksToBottom() {
        assertEquals( Integer.MAX_VALUE, GeneWebService.editDistanceOrMax( null, "tp53" ) );
    }
}
