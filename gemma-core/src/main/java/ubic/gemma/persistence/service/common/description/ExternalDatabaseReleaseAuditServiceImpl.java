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
package ubic.gemma.persistence.service.common.description;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.payload.ReleaseDetailsUpdatePayload;
import ubic.gemma.model.common.auditAndSecurity.eventType.ReleaseDetailsUpdateEvent;
import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * Implementation of {@link ExternalDatabaseReleaseAuditService}. The
 * {@link Audited} aspect intercepts {@link #recordReleaseDetailsUpdate}
 * on return, picks up the {@link ReleaseDetailsUpdatePayload} argument,
 * serialises it to JSON and writes it to {@code AUDIT_EVENT.PAYLOAD}
 * via {@code addUpdateEventWithPayload}. The {@code note} parameter is
 * forwarded into {@code AUDIT_EVENT.NOTE} via
 * {@code messageSpel = "#note"}.
 *
 * <p>Default propagation (REQUIRED) so the audit row joins the caller's
 * surrounding transaction -- the callers ({@code updateReleaseDetails}
 * and {@code updateReleaseLastUpdated} on
 * {@link ExternalDatabaseServiceImpl}) are themselves {@code @Transactional}
 * and the audit event must mutate the same in-memory audit trail bag
 * so existing assertions on {@code ed.getAuditTrail().getEvents()} continue
 * to see the new row immediately. Contrast with
 * {@code OutlierFlaggingAuditServiceImpl} (which uses REQUIRES_NEW because
 * its caller runs with {@code Propagation.NEVER}).
 *
 * <p>Phase C bucket 2g — see {@code handoffs/AUDIT_RESIDUAL_INVENTORY.md}
 * #9 + #10. Replaces the legacy imperative 5-arg
 * {@code auditTrailService.addUpdateEvent(ed, ReleaseDetailsUpdateEvent.class,
 * note, detail, lastUpdated)} calls.
 */
@Service
@Slf4j
public class ExternalDatabaseReleaseAuditServiceImpl implements ExternalDatabaseReleaseAuditService {

    @Override
    @Transactional
    @Audited(value = ReleaseDetailsUpdateEvent.class, messageSpel = "#note")
    public void recordReleaseDetailsUpdate( ExternalDatabase ed, @Nullable String note, ReleaseDetailsUpdatePayload payload ) {
        log.debug( "Recording ReleaseDetailsUpdateEvent for {}: {}", ed, note );
    }
}
