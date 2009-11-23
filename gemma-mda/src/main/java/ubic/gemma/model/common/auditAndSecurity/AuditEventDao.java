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
import java.util.Map;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 */
public interface AuditEventDao extends BaseDao<AuditEvent> {

    /**
     * 
     */
    public java.util.Collection<Auditable> getNewSinceDate( java.util.Date date );

    /**
     * 
     */
    public java.util.Collection<Auditable> getUpdatedSinceDate( java.util.Date date );

    /**
     * 
     */
    public void thaw( ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent );

    /**
     * 
     */
    public java.util.List<AuditEvent> getEvents( ubic.gemma.model.common.Auditable auditable );

    /**
     * Return a map of Auditables to AuditEvents for the given AuditEventType.
     */
    public Map<Auditable, AuditEvent> getLastEvent( java.util.Collection<? extends Auditable> auditables,
            Class<? extends AuditEventType> type );

    /**
     * Returns the last AuditEvent of the specified type from the given auditable.
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastEvent(
            ubic.gemma.model.common.Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * Get all of the most recent AuditEvents for the given auditables, where the events have types. Return value is a
     * map of AuditEventType.classes -> Auditable -> AuditEven
     */
    public java.util.Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastTypedAuditEvents(
            java.util.Collection<? extends Auditable> auditables );
    
    public AuditEvent getLastOutstandingTroubleEvent(Collection<AuditEvent> events);

}
