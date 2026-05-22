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
package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.payload.SingleCellAggregationPayload;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link SingleCellExpressionExperimentAggregateAuditService}.
 * The {@link Audited} aspect intercepts the method on return, picks up the
 * {@link SingleCellAggregationPayload} argument, serialises it to JSON and
 * writes it to {@code AUDIT_EVENT.PAYLOAD} via
 * {@code addUpdateEventWithPayload}. The {@code note} parameter is exposed via
 * {@code messageSpel = "#note"} so the caller controls the
 * {@code AUDIT_EVENT.NOTE} column verbatim.
 *
 * <p>Phase C bucket 2f — see {@code AUDIT_PHASE_C_RECCE.md} §4d. Replaces the
 * legacy imperative
 * {@code auditTrailService.addUpdateEvent(ee, DataAddedEvent.class, note, detail)}
 * 4-arg call that lived at the bottom of
 * {@code SingleCellExpressionExperimentAggregateServiceImpl#aggregateVectors}.
 */
@Service
@Slf4j
public class SingleCellExpressionExperimentAggregateAuditServiceImpl
        implements SingleCellExpressionExperimentAggregateAuditService {

    @Override
    @Transactional
    @Audited(value = DataAddedEvent.class, messageSpel = "#note")
    public void recordAggregateCreated( ExpressionExperiment ee, String note, SingleCellAggregationPayload payload ) {
        log.debug( "Recording DataAddedEvent for {}: {}", ee, note );
    }
}
