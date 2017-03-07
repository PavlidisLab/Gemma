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
package ubic.gemma.model.common.auditAndSecurity;

import ubic.gemma.model.common.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.BaseDao;

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
     * @return events for the given auditable.
     */
    List<AuditEvent> getEvents( AbstractAuditable auditable );

    /**
     * Returns the last AuditEvent of the specified type from the given auditable.
     */
    AuditEvent getLastEvent( AbstractAuditable auditable, Class<? extends AuditEventType> type );

    /**
     * Return a map of Auditables to AuditEvents for the given AuditEventType.
     */
    Map<AbstractAuditable, AuditEvent> getLastEvent( Collection<? extends AbstractAuditable> auditables,
            Class<? extends AuditEventType> type );

    Map<Class<? extends AuditEventType>, Map<AbstractAuditable, AuditEvent>> getLastEvents(
            Collection<? extends AbstractAuditable> auditables, Collection<Class<? extends AuditEventType>> types );

    /**
     * Get auditables that have been Created since the given date
     */
    Collection<AbstractAuditable> getNewSinceDate( Date date );

    /**
     * Get auditables that have been Updated since the given date
     */
    Collection<AbstractAuditable> getUpdatedSinceDate( Date date );

    boolean hasEvent( AbstractAuditable a, Class<? extends AuditEventType> type );

    void retainHavingEvent( Collection<? extends AbstractAuditable> a, Class<? extends AuditEventType> type );

    void retainLackingEvent( Collection<? extends AbstractAuditable> a, Class<? extends AuditEventType> type );

    /**
     *
     */
    void thaw( AuditEvent auditEvent );

}
