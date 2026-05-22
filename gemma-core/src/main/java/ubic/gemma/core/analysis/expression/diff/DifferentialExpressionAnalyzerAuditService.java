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
package ubic.gemma.core.analysis.expression.diff;

import ubic.gemma.core.security.audit.payload.DifferentialExpressionAnalysisPayload;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean facade for the {@code DifferentialExpressionAnalysisEvent} audit
 * emission carried out at the end of
 * {@link DifferentialExpressionAnalyzerServiceImpl#persistAnalysis}. The hop
 * through a proxy is required so the {@code AuditedAspect} can intercept the
 * {@code @Audited}-annotated method on the implementation bean.
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d.
 */
public interface DifferentialExpressionAnalyzerAuditService {

    /**
     * Emit a {@code DifferentialExpressionAnalysisEvent} for {@code ee} with
     * the given payload.
     */
    void recordAnalysisPersisted( ExpressionExperiment ee, String note, DifferentialExpressionAnalysisPayload payload );
}
