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

import org.springframework.lang.Nullable;
import ubic.gemma.core.security.audit.payload.ReleaseDetailsUpdatePayload;
import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * Co-bean carrying the {@code @Audited} hook for
 * {@code ReleaseDetailsUpdateEvent} writes. Exists so the audit aspect can
 * intercept emission for the two release-update entry points on
 * {@link ExternalDatabaseService} -- the previous imperative
 * {@code auditTrailService.addUpdateEvent(ed, ReleaseDetailsUpdateEvent.class,
 * note, detail, lastUpdated)} 5-arg form is not expressible through the
 * declarative aspect today (the aspect writes via
 * {@code addUpdateEventWithPayload} which sets {@code performedDate=now()}).
 *
 * <p>Phase C bucket 2g (handoffs/AUDIT_RESIDUAL_INVENTORY.md #9 + #10):
 * migrates the imperative call to an {@code @Audited}-decorated method on
 * this helper bean. The legacy 5-arg form's explicit
 * {@code performedDate=lastUpdated} semantics shift to {@code now()} for
 * the audit row; the original {@code lastUpdated} is preserved verbatim
 * inside the JSON payload (and on the entity itself, via
 * {@code ExternalDatabase.lastUpdated}).
 */
public interface ExternalDatabaseReleaseAuditService {

    /**
     * Emit a {@code ReleaseDetailsUpdateEvent} for the given external
     * database. The aspect picks the {@link ExternalDatabase} as the audit
     * target, serialises {@code payload} into {@code AUDIT_EVENT.PAYLOAD},
     * and stores {@code note} in {@code AUDIT_EVENT.NOTE}.
     *
     * @param ed      the auditable target
     * @param note    short note (may be {@code null} -- e.g. background
     *                refreshes from {@code GeneMultifunctionalityPopulationServiceImpl}
     *                supply a descriptive note; manual updates may omit it)
     * @param payload structured release context; carries
     *                {@code releaseVersion}, {@code releaseUrl},
     *                {@code lastUpdated} and the transition {@code detail}
     */
    void recordReleaseDetailsUpdate( ExternalDatabase ed, @Nullable String note, ReleaseDetailsUpdatePayload payload );
}
