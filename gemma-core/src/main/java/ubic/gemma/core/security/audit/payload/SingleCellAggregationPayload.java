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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.security.audit.payload;

import org.springframework.lang.Nullable;
import ubic.gemma.core.security.audit.AuditEventPayload;

import java.util.List;

/**
 * Structured payload for the {@code DataAddedEvent} written at the end of
 * {@code SingleCellExpressionExperimentAggregateServiceImpl#aggregateVectors}.
 * Captures the same structured fields the legacy free-form {@code DETAIL}
 * string encoded:
 * <ul>
 *   <li>{@code sourceQuantitationType} — display string of the source single-cell QT.</li>
 *   <li>{@code singleCellDimension} — display string of the source single-cell dimension.</li>
 *   <li>{@code mask} — optional mask token (the {@code config.getMask()} value if non-null).</li>
 *   <li>{@code aggregatedAssays} — one entry per aggregated pseudo-bulk assay, carrying the
 *       same per-assay metrics (number of cells, design elements, masked cells, library
 *       size, library-size adjustment) the legacy detail string encoded.</li>
 * </ul>
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d.
 */
public record SingleCellAggregationPayload(
        String sourceQuantitationType,
        String singleCellDimension,
        @Nullable String mask,
        List<AggregatedAssay> aggregatedAssays
) implements AuditEventPayload {

    /**
     * Per-assay metrics for an aggregated pseudo-bulk assay. All optional fields
     * mirror the conditional branches in the legacy {@code StringBuilder} detail
     * — they are {@code null} / {@code 0} when the corresponding branch did not
     * fire.
     */
    public record AggregatedAssay(
            String bioAssay,
            @Nullable Integer numberOfCells,
            @Nullable Integer numberOfDesignElements,
            @Nullable Integer numberOfCellsByDesignElements,
            @Nullable Integer maskedCells,
            @Nullable Integer totalCells,
            @Nullable Double librarySize,
            @Nullable Double unadjustedLibrarySize
    ) {
    }
}
