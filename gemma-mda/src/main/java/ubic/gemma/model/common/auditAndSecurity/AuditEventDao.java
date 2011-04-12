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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.BaseDao;

/**
 * @see AuditEvent
 * @see AuditEventService
 * @version $Id$
 */
public interface AuditEventDao extends BaseDao<AuditEvent> {
    /**
     * @param a
     * @param type
     * @return
     */
    public void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

    /**
     * @param a
     * @param type
     * @return
     */
    public void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

    /**
     * @param a
     * @param type
     * @return
     */
    public boolean hasEvent( Auditable a, Class<? extends AuditEventType> type );

    /**
     * Get auditables that have been Created since the given date
     * 
     * @return
     */
    public Collection<Auditable> getNewSinceDate( Date date );

    /**
     * Get auditables that have been Updated since the given date
     * 
     * @return
     */
    public Collection<Auditable> getUpdatedSinceDate( Date date );

    /**
     * 
     */
    public void thaw( AuditEvent auditEvent );

    /**
     * @return events for the given auditable.
     */
    public List<AuditEvent> getEvents( Auditable auditable );

    /**
     * Return a map of Auditables to AuditEvents for the given AuditEventType.
     */
    public Map<Auditable, AuditEvent> getLastEvent( Collection<? extends Auditable> auditables,
            Class<? extends AuditEventType> type );

    /**
     * Returns the last AuditEvent of the specified type from the given auditable.
     */
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * Get all of the most recent AuditEvents for the given auditables, where the events have types. Return value is a
     * map of AuditEventType.classes -> Auditable -> AuditEven
     */
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastTypedAuditEvents(
            Collection<? extends Auditable> auditables );

    // not implementing yet.
    //
    // /**
    // * @param <T>
    // * @param clazz
    // * @param type
    // * @param limit
    // * @return
    // */
    // public <T extends Auditable> java.util.Collection<T> getHavingEvent( Class<T> clazz,
    // Class<? extends AuditEventType> type, int limit );
    //
    // /**
    // * @param <T>
    // * @param clazz
    // * @param type
    // * @param limit
    // * @return
    // */
    // public <T extends Auditable> java.util.Collection<T> getLackingEvent( Class<T> clazz,
    // Class<? extends AuditEventType> type, int limit );

    /**
     * @param events
     * @return
     */
    public AuditEvent getLastOutstandingTroubleEvent( Collection<AuditEvent> events );

    /**
     * @param auditables
     * @return map of Auditable to AuditEvent. NOTE: for EEs, this DOES look at the ADs.
     */
    public Map<Auditable, AuditEvent> getLastOutstandingTroubleEvents( Collection<? extends Auditable> auditables );

    /**
     * @param auditables
     * @param types
     * @return
     */
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types );

}
