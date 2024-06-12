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

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.BaseDao;

import javax.annotation.Nullable;
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
     * Obtain the audit events associated to a given auditable.
     * <p>
     * Events are sorted by date in ascending order.
     */
    List<AuditEvent> getEvents( Auditable auditable );

    /**
     * Obtain the creation events for the given auditables.
     * <p>
     * If an auditable has more than one creation event (which is in itself a bug), the earliest one is returned.
     */
    <T extends Auditable> Map<T, AuditEvent> getCreateEvents( Collection<T> auditables );

    /**
     * Obtain the latest event of a given type for a given auditable.
     * @see #getLastEvent(Auditable, Class)
     */
    @Nullable
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * Obtain the latest event of a given type, excluding a certain number of types.
     * @param type          type of event to retrieve, augmented by its hierarchy
     * @param excludedTypes excluded event types (their hierarchy is also excluded)
     */
    @Nullable
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type, Collection<Class<? extends AuditEventType>> excludedTypes );

    /**
     * Obtain the latest events of a specified type for all given auditables.
     * @see #getLastEvent(Auditable, Class)
     */
    <T extends Auditable> Map<T, AuditEvent> getLastEvents( Collection<T> auditables, Class<? extends AuditEventType> type );

    /**
     * Obtain the latest events of a specified type for all auditable of a given type.
     * @see #getLastEvent(Auditable, Class)
     */
    <T extends Auditable> Map<T, AuditEvent> getLastEvents( Class<T> auditableClass, Class<? extends AuditEventType> type );

    /**
     * Get auditables that have been created since the given date.
     */
    <T extends Auditable> Collection<T> getNewSinceDate( Class<T> auditableClass, Date date );

    /**
     * Get auditables that have been updated since the given date.
     */
    <T extends Auditable> Collection<T> getUpdatedSinceDate( Class<T> auditableClass, Date date );
}
