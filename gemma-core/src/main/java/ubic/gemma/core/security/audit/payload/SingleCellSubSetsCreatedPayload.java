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

import ubic.gemma.core.security.audit.AuditEventPayload;

import java.util.List;

/**
 * Structured payload for {@code SingleCellSubSetsCreatedEvent} writes.
 * Captures the same structured fields the legacy free-form {@code DETAIL}
 * string encoded:
 * <ul>
 *   <li>{@code cellTypes} — pretty-printed cell-type characteristics ({@code [label] (URI)} or bare label).</li>
 *   <li>{@code cellTypeFactor} — display string of the experimental factor.</li>
 *   <li>{@code cellTypeToFactorValueMapping} — pretty-printed mapping (one line per characteristic → factor-value).</li>
 *   <li>{@code subsets} — display strings of the created {@code ExpressionExperimentSubSet}s.</li>
 * </ul>
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d.
 */
public record SingleCellSubSetsCreatedPayload(
        List<String> cellTypes,
        String cellTypeFactor,
        String cellTypeToFactorValueMapping,
        List<String> subsets
) implements AuditEventPayload {
}
