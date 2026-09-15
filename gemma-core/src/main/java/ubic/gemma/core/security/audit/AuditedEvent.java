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
package ubic.gemma.core.security.audit;

import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * Spring {@link ApplicationEvent} published by {@link AuditedAspect} every
 * time an {@link Audited}-annotated method completes successfully. Lets
 * downstream concerns (notification dispatch, analysis re-run triggers, cache
 * eviction, etc.) react to a typed audit-trail write without bolting more
 * logic into the aspect itself.
 *
 * <p>Subscribe with {@code @TransactionalEventListener} (or
 * {@code @EventListener} if you do not need transaction-boundary semantics).
 *
 * <p>Phase A of {@code AUDIT_SYSTEM_AUDIT.md}.
 */
public class AuditedEvent extends ApplicationEvent {

    private final Auditable target;
    private final AuditEventType eventType;
    @Nullable
    private final AuditEventPayload payload;
    private final AuditEvent auditEvent;

    /**
     * @param source     the originating object (typically the aspect bean
     *                   itself; required by the {@link ApplicationEvent}
     *                   contract).
     * @param target     the Auditable to which the new {@link AuditEvent}
     *                   was added.
     * @param eventType  the resolved concrete type instance (same one stored
     *                   on the {@link AuditEvent}).
     * @param payload    the typed payload prior to JSON serialisation, or
     *                   {@code null} when the {@link Audited} method declared
     *                   no {@link AuditEventPayload} parameter.
     * @param auditEvent the persisted (or at least persisted-to-the-trail)
     *                   {@link AuditEvent} row.
     */
    public AuditedEvent( Object source, Auditable target, AuditEventType eventType,
            @Nullable AuditEventPayload payload, AuditEvent auditEvent ) {
        super( source );
        this.target = target;
        this.eventType = eventType;
        this.payload = payload;
        this.auditEvent = auditEvent;
    }

    public Auditable getTarget() {
        return target;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    @Nullable
    public AuditEventPayload getPayload() {
        return payload;
    }

    public AuditEvent getAuditEvent() {
        return auditEvent;
    }
}
