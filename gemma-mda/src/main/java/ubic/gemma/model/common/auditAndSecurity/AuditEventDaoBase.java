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

import java.util.Collection;
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
public abstract class AuditEventDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.common.auditAndSecurity.AuditEventDao {

    @Override
    public java.util.Collection<? extends AuditEvent> create(
            final java.util.Collection<? extends AuditEvent> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditEvent.create - 'entities' can not be null" );
        }

        for ( AuditEvent auditEvent : entities ) {
            create( auditEvent );
        }
        return entities;
    }

    @Override
    public AuditEvent create( final ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent ) {
        if ( auditEvent == null ) {
            throw new IllegalArgumentException( "AuditEvent.create - 'auditEvent' can not be null" );
        }
        this.getHibernateTemplate().save( auditEvent );
        return auditEvent;
    }

    @Override
    public List<AuditEvent> getEvents( AbstractAuditable auditable ) {
        return this.handleGetEvents( auditable );
    }

    @Override
    public AuditEvent getLastEvent( AbstractAuditable auditable, Class<? extends AuditEventType> type ) {
        return this.handleGetLastEvent( auditable, type );
    }

    @Override
    public Map<AbstractAuditable, AuditEvent> getLastEvent( Collection<? extends AbstractAuditable> auditables,
            Class<? extends AuditEventType> type ) {
        return this.handleGetLastEvent( auditables, type );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getNewSinceDate(java.util.Date)
     */
    @Override
    public java.util.Collection<AbstractAuditable> getNewSinceDate( final java.util.Date date ) {
        try {
            return this.handleGetNewSinceDate( date );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditEventDao.getNewSinceDate(java.util.Date date)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getUpdatedSinceDate(java.util.Date)
     */
    @Override
    public java.util.Collection<AbstractAuditable> getUpdatedSinceDate( final java.util.Date date ) {
        try {
            return this.handleGetUpdatedSinceDate( date );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditEventDao.getUpdatedSinceDate(java.util.Date date)' --> "
                            + th, th );
        }
    }

    @Override
    public Collection<? extends AuditEvent> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from AuditEventImpl where id in (:ids)", "ids", ids );
    }

    @Override
    public AuditEvent load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditEvent.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( ubic.gemma.model.common.auditAndSecurity.AuditEventImpl.class, id );
    }

    @Override
    public java.util.Collection<? extends AuditEvent> loadAll() {
        return this.getHibernateTemplate().loadAll( ubic.gemma.model.common.auditAndSecurity.AuditEventImpl.class );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "AuditEvent.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.auditAndSecurity.AuditEvent entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends AuditEvent> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditEvent.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent ) {
        if ( auditEvent == null ) {
            throw new IllegalArgumentException( "AuditEvent.remove - 'auditEvent' can not be null" );
        }
        this.getHibernateTemplate().delete( auditEvent );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#thaw(ubic.gemma.model.common.auditAndSecurity.AuditEvent)
     */
    @Override
    public void thaw( final ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent ) {
        try {
            this.handleThaw( auditEvent );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditEventDao.thaw(ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends AuditEvent> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "AuditEvent.update - 'entities' can not be null" );
        }
        for ( AuditEvent auditEvent : entities ) {
            update( auditEvent );
        }
    }

    @Override
    public void update( ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent ) {
        if ( auditEvent == null ) {
            throw new IllegalArgumentException( "AuditEvent.update - 'auditEvent' can not be null" );
        }
        this.getHibernateTemplate().update( auditEvent );
    }

    protected abstract List<AuditEvent> handleGetEvents( AbstractAuditable auditable );

    protected abstract AuditEvent handleGetLastEvent( AbstractAuditable auditable,
            Class<? extends AuditEventType> type );

    protected abstract Map<AbstractAuditable, AuditEvent> handleGetLastEvent(
            Collection<? extends AbstractAuditable> auditables, Class<? extends AuditEventType> type );

    /**
     * Performs the core logic for {@link #getNewSinceDate(java.util.Date)}
     */
    protected abstract java.util.Collection<AbstractAuditable> handleGetNewSinceDate( java.util.Date date )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getUpdatedSinceDate(java.util.Date)}
     */
    protected abstract java.util.Collection<AbstractAuditable> handleGetUpdatedSinceDate( java.util.Date date )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.common.auditAndSecurity.AuditEvent)}
     */
    protected abstract void handleThaw( ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent )
            throws java.lang.Exception;

}