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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author pavlidis
 * @see AuditEventService
 */
@Service("auditEventService")
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
    public <T extends Auditable> Map<T, AuditEvent> getCreateEvents( Collection<T> auditables ) {
        return this.auditEventDao.getCreateEvents( auditables );
    }

    @Override
    @Transactional(readOnly = true)
    public AuditEvent getLastEvent( Auditable auditable ) {
        return auditEventDao.getLastEvent( auditable );
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
    public <T extends Auditable> Map<T, AuditEvent> getLastEvents( Class<T> auditableClass, Class<? extends AuditEventType> type ) {
        return auditEventDao.getLastEvents( auditableClass, type );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Auditable> Map<Class<? extends AuditEventType>, Map<T, AuditEvent>> getLastEvents(
            Collection<T> auditables, Collection<Class<? extends AuditEventType>> types ) {
        Map<Class<? extends AuditEventType>, Map<T, AuditEvent>> results = new HashMap<>();
        for ( Class<? extends AuditEventType> ti : types ) {
            Map<T, AuditEvent> results2 = auditEventDao.getLastEvents( auditables, ti );
            results.put( ti, results2.entrySet().stream()
                    .filter( e -> ti.isAssignableFrom( e.getValue().getEventType().getClass() ) )
                    .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Auditable> Collection<T> getNewSinceDate( Class<T> auditableClass, Date date ) {
        return this.auditEventDao.getNewSinceDate( auditableClass, date );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Auditable> Collection<T> getUpdatedSinceDate( Class<T> auditableClass, Date date ) {
        return this.auditEventDao.getUpdatedSinceDate( auditableClass, date );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEvent( Auditable a, Class<? extends AuditEventType> type ) {
        return this.auditEventDao.getLastEvent( a, type ) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type ) {
        final Map<? extends Auditable, AuditEvent> events = auditEventDao.getLastEvents( a, type );
        CollectionUtils.filter( a, events::containsKey );
    }

    @Override
    @Transactional(readOnly = true)
    public void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type ) {
        final Map<? extends Auditable, AuditEvent> events = auditEventDao.getLastEvents( a, type );
        CollectionUtils.filter( a, ( Predicate<Auditable> ) arg0 -> !events.containsKey( arg0 ) );
    }
}