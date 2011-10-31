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

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.auditAndSecurity.AuditTrailService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService
 */
public abstract class AuditTrailServiceBase implements ubic.gemma.model.common.auditAndSecurity.AuditTrailService {

    @Autowired
    private ubic.gemma.model.common.auditAndSecurity.AuditTrailDao auditTrailDao;

    @Autowired
    private ubic.gemma.model.common.auditAndSecurity.UserDao userDao;

    @Autowired
    private ubic.gemma.model.common.auditAndSecurity.AuditEventDao auditEventDao;

    @Autowired
    private StatusDao statusDao;

    public StatusDao getStatusDao() {
        return statusDao;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addComment(ubic.gemma.model.common.Auditable,
     *      java.lang.String, java.lang.String)
     */
    public void addComment( final ubic.gemma.model.common.Auditable auditable, final java.lang.String comment,
            final java.lang.String detail ) {
        try {
            this.handleAddComment( auditable, comment, detail );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.addComment(ubic.gemma.model.common.Auditable auditable, java.lang.String comment, java.lang.String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addOkFlag(ubic.gemma.model.common.Auditable,
     *      java.lang.String, java.lang.String)
     */
    public void addOkFlag( final ubic.gemma.model.common.Auditable auditable, final java.lang.String comment,
            final java.lang.String detail ) {
        try {
            this.handleAddOkFlag( auditable, comment, detail );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.addOkFlag(ubic.gemma.model.common.Auditable auditable, java.lang.String comment, java.lang.String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addTroubleFlag(ubic.gemma.model.common.Auditable,
     *      java.lang.String, java.lang.String)
     */
    public void addTroubleFlag( final ubic.gemma.model.common.Auditable auditable, final java.lang.String comment,
            final java.lang.String detail ) {
        try {
            this.handleAddTroubleFlag( auditable, comment, detail );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.addTroubleFlag(ubic.gemma.model.common.Auditable auditable, java.lang.String comment, java.lang.String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addUpdateEvent(ubic.gemma.model.common.Auditable,
     *      java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent addUpdateEvent(
            final ubic.gemma.model.common.Auditable auditable, final java.lang.String note ) {
        try {
            return this.handleAddUpdateEvent( auditable, note );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.addUpdateEvent(ubic.gemma.model.common.Auditable auditable, java.lang.String note)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addUpdateEvent(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType, java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent addUpdateEvent(
            final ubic.gemma.model.common.Auditable auditable,
            final ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType auditEventType,
            final java.lang.String note ) {
        try {
            return this.handleAddUpdateEvent( auditable, auditEventType, note );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.addUpdateEvent(ubic.gemma.model.common.Auditable auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType auditEventType, java.lang.String note)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addUpdateEvent(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType, java.lang.String, java.lang.String)
     */
    @Override
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent addUpdateEvent(
            final ubic.gemma.model.common.Auditable auditable,
            final ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType auditEventType,
            final java.lang.String note, final java.lang.String detail ) {
        try {
            return this.handleAddUpdateEvent( auditable, auditEventType, note, detail );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.addUpdateEvent(ubic.gemma.model.common.Auditable auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType auditEventType, java.lang.String note, java.lang.String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#addValidatedFlag(ubic.gemma.model.common.Auditable,
     *      java.lang.String, java.lang.String)
     */
    public void addValidatedFlag( final ubic.gemma.model.common.Auditable auditable, final java.lang.String comment,
            final java.lang.String detail ) {
        try {
            this.handleAddValidatedFlag( auditable, comment, detail );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.addValidatedFlag(ubic.gemma.model.common.Auditable auditable, java.lang.String comment, java.lang.String detail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#audit(ubic.gemma.model.common.Auditable,
     *      ubic.gemma.model.common.auditAndSecurity.AuditEvent)
     */
    public void audit( final ubic.gemma.model.common.Auditable entity,
            final ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent ) {
        try {
            this.handleAudit( entity, auditEvent );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.audit(ubic.gemma.model.common.Auditable entity, ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#create(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditTrail create(
            final ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        try {
            return this.handleCreate( auditTrail );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.create(ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#getLastTroubleEvent(ubic.gemma.model.common.Auditable)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastTroubleEvent(
            final ubic.gemma.model.common.Auditable auditable ) {
        try {
            return this.handleGetLastTroubleEvent( auditable );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.getLastTroubleEvent(ubic.gemma.model.common.Auditable auditable)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#getLastValidationEvent(ubic.gemma.model.common.Auditable)
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastValidationEvent(
            final ubic.gemma.model.common.Auditable auditable ) {
        try {
            return this.handleGetLastValidationEvent( auditable );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditTrailService.getLastValidationEvent(ubic.gemma.model.common.Auditable auditable)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>auditEvent</code>'s DAO.
     */
    public void setAuditEventDao( ubic.gemma.model.common.auditAndSecurity.AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    /**
     * Sets the reference to <code>auditTrail</code>'s DAO.
     */
    public void setAuditTrailDao( ubic.gemma.model.common.auditAndSecurity.AuditTrailDao auditTrailDao ) {
        this.auditTrailDao = auditTrailDao;
    }

    /**
     * Sets the reference to <code>user</code>'s DAO.
     */
    public void setUserDao( ubic.gemma.model.common.auditAndSecurity.UserDao userDao ) {
        this.userDao = userDao;
    }

    /**
     * Gets the reference to <code>auditEvent</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.AuditEventDao getAuditEventDao() {
        return this.auditEventDao;
    }

    /**
     * Gets the reference to <code>auditTrail</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.AuditTrailDao getAuditTrailDao() {
        return this.auditTrailDao;
    }

    /**
     * Gets the reference to <code>user</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.UserDao getUserDao() {
        return this.userDao;
    }

    /**
     * Performs the core logic for
     * {@link #addComment(ubic.gemma.model.common.Auditable, java.lang.String, java.lang.String)}
     */
    protected abstract void handleAddComment( ubic.gemma.model.common.Auditable auditable, java.lang.String comment,
            java.lang.String detail ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #addOkFlag(ubic.gemma.model.common.Auditable, java.lang.String, java.lang.String)}
     */
    protected abstract void handleAddOkFlag( ubic.gemma.model.common.Auditable auditable, java.lang.String comment,
            java.lang.String detail ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #addTroubleFlag(ubic.gemma.model.common.Auditable, java.lang.String, java.lang.String)}
     */
    protected abstract void handleAddTroubleFlag( ubic.gemma.model.common.Auditable auditable,
            java.lang.String comment, java.lang.String detail ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #addUpdateEvent(ubic.gemma.model.common.Auditable, java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleAddUpdateEvent(
            ubic.gemma.model.common.Auditable auditable, java.lang.String note ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #addUpdateEvent(ubic.gemma.model.common.Auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType, java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleAddUpdateEvent(
            ubic.gemma.model.common.Auditable auditable,
            ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType auditEventType, java.lang.String note )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #addUpdateEvent(ubic.gemma.model.common.Auditable, ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType, java.lang.String, java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleAddUpdateEvent(
            ubic.gemma.model.common.Auditable auditable,
            ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType auditEventType, java.lang.String note,
            java.lang.String detail ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #addValidatedFlag(ubic.gemma.model.common.Auditable, java.lang.String, java.lang.String)}
     */
    protected abstract void handleAddValidatedFlag( ubic.gemma.model.common.Auditable auditable,
            java.lang.String comment, java.lang.String detail ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #audit(ubic.gemma.model.common.Auditable, ubic.gemma.model.common.auditAndSecurity.AuditEvent)}
     */
    protected abstract void handleAudit( ubic.gemma.model.common.Auditable entity,
            ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.auditAndSecurity.AuditTrail)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditTrail handleCreate(
            ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastTroubleEvent(ubic.gemma.model.common.Auditable)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastTroubleEvent(
            ubic.gemma.model.common.Auditable auditable ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getLastValidationEvent(ubic.gemma.model.common.Auditable)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.AuditEvent handleGetLastValidationEvent(
            ubic.gemma.model.common.Auditable auditable ) throws java.lang.Exception;

}