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
 * Structured payload for the {@code DifferentialExpressionAnalysisEvent}
 * written at the end of
 * {@code DifferentialExpressionAnalyzerServiceImpl#persistAnalysis}. Captures
 * the {@code analysis.getDescription()} value that the legacy 4-arg
 * {@code addUpdateEvent} call put in the free-form {@code DETAIL} column.
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d.
 */
public record DifferentialExpressionAnalysisPayload(
        @Nullable String analysisDescription
) implements AuditEventPayload {
}
