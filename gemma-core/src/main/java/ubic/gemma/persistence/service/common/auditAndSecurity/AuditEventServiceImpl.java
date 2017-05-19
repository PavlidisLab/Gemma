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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author pavlidis
 * @see AuditEventService
 */
@Service
public class AuditEventServiceImpl extends AuditEventServiceBase {

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getEvents( Auditable auditable ) {
        return this.getAuditEventDao().getEvents( auditable );
    }

    @Override
    @Transactional(readOnly = true)
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type ) {
        return this.getAuditEventDao().getLastEvent( auditable, type );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types ) {
        return this.getAuditEventDao().getLastEvents( auditables, types );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEvent( Auditable a, Class<? extends AuditEventType> type ) {
        return this.getAuditEventDao().hasEvent( a, type );
    }

    @Override
    @Transactional(readOnly = true)
    public void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type ) {
        this.getAuditEventDao().retainHavingEvent( a, type );
    }

    @Override
    @Transactional(readOnly = true)
    public void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type ) {
        this.getAuditEventDao().retainLackingEvent( a, type );
    }

    /**
     * @see AuditEventService#getNewSinceDate(java.util.Date)
     */
    @Override
    protected java.util.Collection<Auditable> handleGetNewSinceDate( java.util.Date date ) {
        return this.getAuditEventDao().getNewSinceDate( date );
    }

    /**
     * @see AuditEventService#getUpdatedSinceDate(java.util.Date)
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