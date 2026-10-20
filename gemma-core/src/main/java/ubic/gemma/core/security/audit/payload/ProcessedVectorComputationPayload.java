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

/**
 * Structured payload for {@code ProcessedVectorComputationEvent} writes.
 * Captures the same fields the legacy free-form {@code DETAIL} string
 * encoded:
 * <ul>
 *   <li>{@code rawQuantitationType} — display string of the raw QT used as input.</li>
 *   <li>{@code processedQuantitationType} — display string of the freshly-created processed QT.</li>
 *   <li>{@code numberOfMaskedMissingValues} — count of cells masked due to missing-value detection (0 = omitted in legacy detail).</li>
 *   <li>{@code numberOfMaskedOutliers} — count of cells masked due to outlier flagging (0 = omitted in legacy detail).</li>
 *   <li>{@code quantileNormalized} — true if the processed vectors were quantile-normalized.</li>
 *   <li>{@code comment} — optional free-form comment from the creation summary.</li>
 * </ul>
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d.
 */
public record ProcessedVectorComputationPayload(
        @Nullable String rawQuantitationType,
        @Nullable String processedQuantitationType,
        int numberOfMaskedMissingValues,
        int numberOfMaskedOutliers,
        boolean quantileNormalized,
        @Nullable String comment
) implements AuditEventPayload {
}
