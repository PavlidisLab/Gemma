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

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.AuditEvent</code>.
 * </p>
 *
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 */
public abstract class AuditEventDaoBase extends VoEnabledDao<AuditEvent, AuditEventValueObject>
        implements AuditEventDao {

    protected AuditEventDaoBase( SessionFactory sessionFactory ) {
        super( AuditEvent.class, sessionFactory );
    }

    @Override
    public List<AuditEvent> getEvents( Auditable auditable ) {
        return this.handleGetEvents( auditable );
    }

    @Override
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type ) {
        return this.handleGetLastEvent( auditable, type );
    }

    @Override
    public Map<Auditable, AuditEvent> getLastEvent( Collection<? extends Auditable> auditables,
            Class<? extends AuditEventType> type ) {
        return this.handleGetLastEvent( auditables, type );
    }

    @Override
    public Collection<Auditable> getNewSinceDate( Date date ) {
        try {
            return this.handleGetNewSinceDate( date );
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'AuditEventDao.getNewSinceDate(Date date)' --> " + th, th );
        }
    }

    @Override
    public Collection<Auditable> getUpdatedSinceDate( final Date date ) {
        try {
            return this.handleGetUpdatedSinceDate( date );
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'AuditEventDao.getUpdatedSinceDate(Date date)' --> " + th,
                    th );
        }
    }

    @Override
    public void thaw( AuditEvent auditEvent ) {
        Hibernate.initialize( auditEvent.getAction() );
        Hibernate.initialize( auditEvent.getPerformer() );
        Hibernate.initialize( auditEvent.getPerformer().getLastName() );
    }

    protected abstract List<AuditEvent> handleGetEvents( Auditable auditable );

    protected abstract AuditEvent handleGetLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    protected abstract Map<Auditable, AuditEvent> handleGetLastEvent( Collection<? extends Auditable> auditables,
            Class<? extends AuditEventType> type );

    /**
     * Performs the core logic for {@link #getNewSinceDate(Date)}
     */
    protected abstract Collection<Auditable> handleGetNewSinceDate( Date date ) throws Exception;

    /**
     * Performs the core logic for {@link #getUpdatedSinceDate(Date)}
     */
    protected abstract Collection<Auditable> handleGetUpdatedSinceDate( Date date ) throws Exception;

}