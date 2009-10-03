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

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.AuditableService</code>, provides access to all services
 * and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.AuditableService
 */
public abstract class AuditableServiceBase implements ubic.gemma.model.common.AuditableService {

    private ubic.gemma.model.common.AuditableDao auditableDao;

    /**
     * @see ubic.gemma.model.common.AuditableService#getEvents(ubic.gemma.model.common.Auditable)
     */
    public java.util.Collection<AuditEvent> getEvents( final ubic.gemma.model.common.Auditable auditable ) {
        try {
            return this.handleGetEvents( auditable );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.AuditableServiceException(
                    "Error performing 'ubic.gemma.model.common.AuditableService.getEvents(ubic.gemma.model.common.Auditable auditable)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.AuditableService#getLastAuditEvent(java.util.Collection,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
     */
    public java.util.Map<Auditable, AuditEvent> getLastAuditEvent( final java.util.Collection<? extends Auditable> auditables,
            final ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type ) {
        try {
            return this.handleGetLastAuditEvent( auditables, type );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.AuditableServiceException(
                    "Error performing 'ubic.gemma.model.common.AuditableService.getLastAuditEvent(java.util.Collection auditables, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.AuditableService#getLastAuditEvent(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastAuditEvent(
            final ubic.gemma.model.common.Auditable auditable,
            final ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type ) {
        try {
            return this.handleGetLastAuditEvent( auditable, type );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.AuditableServiceException(
                    "Error performing 'ubic.gemma.model.common.AuditableService.getLastAuditEvent(ubic.gemma.model.common.Auditable auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>auditable</code>'s DAO.
     */
    public void setAuditableDao( ubic.gemma.model.common.AuditableDao auditableDao ) {
        this.auditableDao = auditableDao;
    }

    /**
     * Gets the reference to <code>auditable</code>'s DAO.
     */
    protected ubic.gemma.model.common.AuditableDao getAuditableDao() {
        return this.auditableDao;
    }

    /**
     * Performs the core logic for {@link #getEvents(ubic.gemma.model.common.Auditable)}
     */
    protected abstract java.util.Collection<AuditEvent> handleGetEvents( ubic.gemma.model.common.Auditable auditable )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getLastAuditEvent(java.util.Collection, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)}
     */
    protected abstract java.util.Map<Auditable, AuditEvent> handleGetLastAuditEvent( java.util.Collection<? extends Auditable> auditables,
            ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getLastAuditEvent(ubic.gemma.model.common.Auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastAuditEvent(
            ubic.gemma.model.common.Auditable auditable,
            ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type ) throws java.lang.Exception;

}