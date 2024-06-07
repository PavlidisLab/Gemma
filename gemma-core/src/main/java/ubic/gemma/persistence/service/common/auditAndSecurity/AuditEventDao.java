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

import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @see AuditEvent
 * @see AuditEventService
 */
public interface AuditEventDao extends BaseDao<AuditEvent> {

    /**
     * @param auditable auditable
     * @return events for the given auditable.
     */
    List<AuditEvent> getEvents( Auditable auditable );

    /**
     * @param auditable auditable
     * @param type      type
     * @return the last AuditEvent of the specified type from the given auditable.
     */
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * Obtain the latest {@link AuditEvent} of a specified type, excluding a certain number of types.
     */
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type, Collection<Class<? extends AuditEventType>> excludedTypes );

    Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEventsByType(
        Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types );

    /**
     * Get auditables that have been Created since the given date
     *
     * @param date date
     * @return auditables
     */
    Collection<Auditable> getNewSinceDate( Date date );

    /**
     * Get auditables that have been Updated since the given date
     *
     * @param date date
     * @return auditables
     */
    Collection<Auditable> getUpdatedSinceDate( Date date );

    boolean hasEvent( Auditable a, Class<? extends AuditEventType> type );

    void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

    void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

    Map<Auditable, AuditEvent> getCreateEvents( Collection<? extends Auditable> auditables );
}
