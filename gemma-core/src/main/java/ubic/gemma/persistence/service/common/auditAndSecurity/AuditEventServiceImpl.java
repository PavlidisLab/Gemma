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

import org.springframework.beans.factory.annotation.Autowired;
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
public class AuditEventServiceImpl implements AuditEventService {

    private final AuditEventDao auditEventDao;

    @Autowired
    public AuditEventServiceImpl( AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEvent> getEvents( Auditable auditable ) {
        return this.auditEventDao.getEvents( auditable );
    }

    @Override
    @Transactional(readOnly = true)
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type ) {
        return this.auditEventDao.getLastEvent( auditable, type );
    }

    @Override
    @Transactional(readOnly = true)
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type, Collection<Class<? extends AuditEventType>> excludedTypes ) {
        return auditEventDao.getLastEvent( auditable, type, excludedTypes );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
        Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types ) {
        return this.auditEventDao.getLastEventsByType( auditables, types );
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Auditable> getNewSinceDate( java.util.Date date ) {
        return this.auditEventDao.getNewSinceDate( date );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Auditable> getUpdatedSinceDate( java.util.Date date ) {
        return this.auditEventDao.getUpdatedSinceDate( date );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEvent( Auditable a, Class<? extends AuditEventType> type ) {
        return this.auditEventDao.hasEvent( a, type );
    }

    @Override
    @Transactional(readOnly = true)
    public void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type ) {
        this.auditEventDao.retainHavingEvent( a, type );
    }

    @Override
    @Transactional(readOnly = true)
    public void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type ) {
        this.auditEventDao.retainLackingEvent( a, type );
    }

}