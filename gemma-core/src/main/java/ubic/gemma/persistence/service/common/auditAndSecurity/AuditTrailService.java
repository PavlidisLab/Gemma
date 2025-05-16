/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.BaseImmutableService;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Create and manipulate audit trails.
 * <p>
 * This service is mainly intended to create audit events manually. Part of the auditing is done automatically using
 * aspects via {@link ubic.gemma.core.security.audit.AuditAdvice}.
 *
 * @author kelsey
 */
public interface AuditTrailService extends BaseImmutableService<AuditTrail> {

    /**
     * Add an update event defined by the given parameters, to the given auditable. Returns the generated event.
     *
     * @param auditable the entity
     * @param note      a short note (optional)
     * @return the newly created event, which will be somewhere in the auditable's {@link AuditTrail#getEvents()}
     * collection.
     */
    @Secured({ "GROUP_AGENT" })
    AuditEvent addUpdateEvent( Auditable auditable, String note );

    /**
     * @see #addUpdateEvent(Auditable, Class, String, String, Date)
     */
    @Secured({ "GROUP_AGENT" })
    AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, String note );

    /**
     * @see #addUpdateEvent(Auditable, Class, String, String, Date)
     */
    @Secured({ "GROUP_AGENT" })
    AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, @Nullable String detail );

    /**
     * Add an update audit event with an exception.
     * <p>
     * Unlike {@link #addUpdateEvent(Auditable, Class, String, String, Date)}, the audit event produced by this method
     * cannot be rolled back, so you may freely raise the exception. This is achieved by using {@link Propagation#REQUIRES_NEW}.
     *
     * @see #addUpdateEvent(Auditable, Class, String, String, Date)
     */
    @Secured({ "GROUP_AGENT" })
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, Throwable throwable );

    /**
     * Add an update audit event of a specific type to the passed auditable entity.
     *
     * @param auditable     the entity being audited
     * @param type          the audit event type
     * @param note          a short note (optional)
     * @param detail        full details for that event
     * @param performedDate the moment the audit was performed (must not be in the future!)
     * @return the newly created event, which will be somewhere in the auditable's {@link AuditTrail#getEvents()}
     * collection.
     */
    @Secured({ "GROUP_AGENT" })
    AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, @Nullable String note, @Nullable String detail, Date performedDate );

    @Override
    @Secured({ "GROUP_USER" })
    AuditTrail create( AuditTrail auditTrail );
}
