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
package ubic.gemma.core.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.payload.SampleRemovalPayload;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalReversionEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link OutlierFlaggingAuditService}. The {@link Audited}
 * aspect intercepts each method on return, picks up the
 * {@link SampleRemovalPayload} argument, serialises it to JSON and writes it
 * to {@code AUDIT_EVENT.PAYLOAD} via {@code addUpdateEventWithPayload}. The
 * {@code note} parameter is forwarded into the {@code AUDIT_EVENT.NOTE}
 * column via {@code messageSpel = "#note"}.
 *
 * <p>{@code Propagation.REQUIRES_NEW} is used because the calling
 * {@link OutlierFlaggingServiceImpl} methods run with
 * {@code Propagation.NEVER}; the audit row needs its own short-lived
 * transaction.
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d. Replaces the
 * legacy imperative
 * {@code auditTrailService.addUpdateEvent(ee, SampleRemovalEvent.class, note, detail)}
 * 4-arg calls in {@code OutlierFlaggingServiceImpl#markAsMissing} and
 * {@code OutlierFlaggingServiceImpl#unmarkAsMissing}.
 */
@Service
@Slf4j
public class OutlierFlaggingAuditServiceImpl implements OutlierFlaggingAuditService {

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @Audited(value = SampleRemovalEvent.class, messageSpel = "#note")
    public void recordSampleRemoval( ExpressionExperiment ee, String note, SampleRemovalPayload payload ) {
        log.debug( "Recording SampleRemovalEvent for {}: {}", ee, note );
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @Audited(value = SampleRemovalReversionEvent.class, messageSpel = "#note")
    public void recordSampleRemovalReversion( ExpressionExperiment ee, String note, SampleRemovalPayload payload ) {
        log.debug( "Recording SampleRemovalReversionEvent for {}: {}", ee, note );
    }
}
