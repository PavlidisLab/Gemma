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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.payload.DifferentialExpressionAnalysisPayload;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link DifferentialExpressionAnalyzerAuditService}. The
 * {@link Audited} aspect intercepts the method on return, picks up the
 * {@link DifferentialExpressionAnalysisPayload} argument, serialises it to
 * JSON and writes it to {@code AUDIT_EVENT.PAYLOAD} via
 * {@code addUpdateEventWithPayload}. The {@code note} parameter is exposed via
 * {@code messageSpel = "#note"} so the caller controls the
 * {@code AUDIT_EVENT.NOTE} column verbatim.
 *
 * <p>{@code Propagation.REQUIRES_NEW} is used because the calling
 * {@link DifferentialExpressionAnalyzerServiceImpl} is class-level
 * {@code Propagation.NEVER}; the audit row needs its own short-lived
 * transaction.
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d. Replaces the
 * legacy imperative
 * {@code auditTrailService.addUpdateEvent(ee, DifferentialExpressionAnalysisEvent.class, note, detail)}
 * 4-arg call wrapped in a defensive try/catch at the bottom of
 * {@code DifferentialExpressionAnalyzerServiceImpl#persistAnalysis}.
 */
@Service
@Slf4j
public class DifferentialExpressionAnalyzerAuditServiceImpl
        implements DifferentialExpressionAnalyzerAuditService {

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Audited(value = DifferentialExpressionAnalysisEvent.class, messageSpel = "#note")
    public void recordAnalysisPersisted( ExpressionExperiment ee, String note, DifferentialExpressionAnalysisPayload payload ) {
        log.debug( "Recording DifferentialExpressionAnalysisEvent for {}: {}", ee, note );
    }
}
