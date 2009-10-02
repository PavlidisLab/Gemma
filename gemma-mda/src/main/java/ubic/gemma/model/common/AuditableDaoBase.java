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
package ubic.gemma.model.common;

import java.util.Map;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.Auditable</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.Auditable
 */
public abstract class AuditableDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport implements
        ubic.gemma.model.common.AuditableDao {

    /**
     * @see ubic.gemma.model.common.AuditableDao#getAuditEvents(ubic.gemma.model.common.Auditable)
     */
    public java.util.Collection<AuditEvent> getAuditEvents( final ubic.gemma.model.common.Auditable auditable ) {
        try {
            return this.handleGetAuditEvents( auditable );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.AuditableDao.getAuditEvents(ubic.gemma.model.common.Auditable auditable)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.AuditableDao#getLastAuditEvent(java.util.Collection,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
     */
    public java.util.Map<Auditable, AuditEvent> getLastAuditEvent(
            final java.util.Collection<? extends Auditable> auditables,
            final ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type ) {
        try {
            return this.handleGetLastAuditEvent( auditables, type );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.AuditableDao.getLastAuditEvent(java.util.Collection auditables, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.AuditableDao#getLastAuditEvent(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastAuditEvent(
            final ubic.gemma.model.common.Auditable auditable,
            final ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type ) {
        try {
            return this.handleGetLastAuditEvent( auditable, type );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.AuditableDao.getLastAuditEvent(ubic.gemma.model.common.Auditable auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.AuditableDao#getLastTypedAuditEvents(java.util.Collection)
     */
    public java.util.Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastTypedAuditEvents(
            final java.util.Collection<? extends Auditable> auditables ) {
        try {
            return this.handleGetLastTypedAuditEvents( auditables );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.AuditableDao.getLastTypedAuditEvents(java.util.Collection auditables)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.AuditableDao#update(ubic.gemma.model.common.Auditable)
     */
    public void update( ubic.gemma.model.common.Auditable auditable ) {
        if ( auditable == null ) {
            throw new IllegalArgumentException( "Auditable.update - 'auditable' can not be null" );
        }
        this.getHibernateTemplate().update( auditable );
    }

    /**
     * Performs the core logic for {@link #getAuditEvents(ubic.gemma.model.common.Auditable)}
     */
    protected abstract java.util.Collection<AuditEvent> handleGetAuditEvents(
            ubic.gemma.model.common.Auditable auditable ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getLastAuditEvent(java.util.Collection, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)}
     */
    protected abstract java.util.Map<Auditable, AuditEvent> handleGetLastAuditEvent(
            java.util.Collection<? extends Auditable> auditables,
            ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getLastAuditEvent(ubic.gemma.model.common.Auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent(
            ubic.gemma.model.common.Auditable auditable,
            ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastTypedAuditEvents(java.util.Collection)}
     */
    protected abstract java.util.Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> handleGetLastTypedAuditEvents(
            java.util.Collection<? extends Auditable> auditables ) throws java.lang.Exception;

}