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
package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.payload.ProcessedVectorComputationPayload;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link ProcessedExpressionDataVectorAuditService}: a thin
 * co-bean whose only purpose is to expose an {@link Audited}-annotated method
 * through a Spring proxy. The {@code AuditedAspect} fires on return, picks up
 * the {@link ProcessedVectorComputationPayload} argument, serialises it to
 * JSON and writes it to {@code AUDIT_EVENT.PAYLOAD} via
 * {@code addUpdateEventWithPayload}.
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d. Replaces the
 * legacy imperative
 * {@code auditTrailService.addUpdateEvent(ee, ProcessedVectorComputationEvent.class, note, detail)}
 * 4-arg call that lived inline in
 * {@code ProcessedExpressionDataVectorServiceImpl#createProcessedDataVectors}.
 */
@Service
@Slf4j
public class ProcessedExpressionDataVectorAuditServiceImpl implements ProcessedExpressionDataVectorAuditService {

    @Override
    @Transactional
    @Audited(value = ProcessedVectorComputationEvent.class,
            messageSpel = "'Created processed expression data for ' + #ee + '.'")
    public void recordProcessedVectorComputation( ExpressionExperiment ee, ProcessedVectorComputationPayload payload ) {
        log.debug( "Recording ProcessedVectorComputationEvent for {}", ee );
    }
}
