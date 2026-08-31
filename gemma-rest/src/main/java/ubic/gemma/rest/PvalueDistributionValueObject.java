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
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire payload for {@code GET /resultSets/{id}/pvalueDistribution}.
 * <p>
 * The stored histogram of the raw p-values for a differential-expression result set, optionally
 * down-binned by merging adjacent stored bins; bins are uniform-width over {@code [0, 1]}. Replaces
 * the UIB pattern of fetching the full result TSV and binning client-side.
 */
@Getter
@Schema(description = "Stored p-value histogram for a differential-expression result set.")
public class PvalueDistributionValueObject {

    @Schema(description = "Result set identifier.")
    private final Long resultSetId;

    @Schema(description = "Which p-value column the histogram covers. Always 'raw' (PVALUE) — only the raw "
            + "distribution is stored.",
            allowableValues = { "raw" })
    private final String column;

    @Schema(description = "Total number of p-values across all bins.")
    private final long n;

    @Schema(description = "Bin counts; length equals the requested `bins`. Bin i covers (i/bins, (i+1)/bins], "
            + "with the first bin also including 0.0 — the closed-on-the-right convention the stored "
            + "histogram was built with.")
    private final List<Bin> bins;

    public PvalueDistributionValueObject( Long resultSetId, String column, long[] counts ) {
        this.resultSetId = resultSetId;
        this.column = column;
        long total = 0;
        List<Bin> binList = new ArrayList<>( counts.length );
        double step = 1.0 / counts.length;
        for ( int i = 0; i < counts.length; i++ ) {
            double lo = i * step;
            double hi = ( i == counts.length - 1 ) ? 1.0 : ( i + 1 ) * step;
            binList.add( new Bin( lo, hi, counts[i] ) );
            total += counts[i];
        }
        this.n = total;
        this.bins = binList;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "A single histogram bin: half-open range (lo, hi] (the first bin also includes lo == 0.0).")
    public static class Bin {
        @Schema(description = "Lower edge of the bin (exclusive, except for the first bin which includes 0.0).")
        private final double lo;
        @Schema(description = "Upper edge of the bin (inclusive).")
        private final double hi;
        @Schema(description = "Number of p-values that fell into this bin.")
        private final long count;
    }
}
