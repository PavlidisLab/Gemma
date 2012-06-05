/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService
 * @author pavlidis
 * @version $Id$
 */
@Service
public class AuditEventServiceImpl extends AuditEventServiceBase {

    @Override
    public List<AuditEvent> getEvents( Auditable auditable ) {
        return this.getAuditEventDao().getEvents( auditable );
    }

    @Override
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type ) {
        return this.getAuditEventDao().getLastEvent( auditable, type );
    }

    @Override
    public Map<Auditable, AuditEvent> getLastEvent( Collection<? extends Auditable> auditables,
            Class<? extends AuditEventType> type ) {
        return this.getAuditEventDao().getLastEvent( auditables, type );
    }

    @Override
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types ) {
        return this.getAuditEventDao().getLastEvents( auditables, types );
    }

    @Override
    public AuditEvent getLastOutstandingTroubleEvent( Collection<AuditEvent> events ) {
        return this.getAuditEventDao().getLastOutstandingTroubleEvent( events );
    }

    @Override
    public boolean hasEvent( Auditable a, Class<? extends AuditEventType> type ) {
        return this.getAuditEventDao().hasEvent( a, type );
    }

    @Override
    public boolean lacksEvent( Auditable a, Class<? extends AuditEventType> type ) {
        return !this.hasEvent( a, type );
    }

    @Override
    public void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type ) {
        this.getAuditEventDao().retainHavingEvent( a, type );
    }

    @Override
    public void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type ) {
        this.getAuditEventDao().retainLackingEvent( a, type );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService#getNewSinceDate(java.util.Date)
     */
    @Override
    protected java.util.Collection<Auditable> handleGetNewSinceDate( java.util.Date date ) {
        return this.getAuditEventDao().getNewSinceDate( date );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService#getUpdatedSinceDate(java.util.Date)
     */
    @Override
    protected Collection<Auditable> handleGetUpdatedSinceDate( java.util.Date date ) {
        return this.getAuditEventDao().getUpdatedSinceDate( date );
    }

    @Override
    protected void handleThaw( AuditEvent auditEvent ) {
        this.getAuditEventDao().thaw( auditEvent );
    }

}