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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.auditAndSecurity.AuditEventService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService
 */
public abstract class AuditEventServiceBase implements ubic.gemma.model.common.auditAndSecurity.AuditEventService {

    private ubic.gemma.model.common.auditAndSecurity.AuditEventDao auditEventDao;

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService#getNewSinceDate(java.util.Date)
     */
    public java.util.Collection getNewSinceDate( final java.util.Date date ) {
        try {
            return this.handleGetNewSinceDate( date );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditEventServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditEventService.getNewSinceDate(java.util.Date date)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService#getUpdatedSinceDate(java.util.Date)
     */
    public java.util.Collection getUpdatedSinceDate( final java.util.Date date ) {
        try {
            return this.handleGetUpdatedSinceDate( date );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditEventServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditEventService.getUpdatedSinceDate(java.util.Date date)' --> "
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
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventService#thaw(ubic.gemma.model.common.auditAndSecurity.AuditEvent)
     */
    public void thaw( final ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent ) {
        try {
            this.handleThaw( auditEvent );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.AuditEventServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.AuditEventService.thaw(ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>auditEvent</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.AuditEventDao getAuditEventDao() {
        return this.auditEventDao;
    }

    /**
     * Performs the core logic for {@link #getNewSinceDate(java.util.Date)}
     */
    protected abstract java.util.Collection handleGetNewSinceDate( java.util.Date date ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getUpdatedSinceDate(java.util.Date)}
     */
    protected abstract java.util.Collection handleGetUpdatedSinceDate( java.util.Date date ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.common.auditAndSecurity.AuditEvent)}
     */
    protected abstract void handleThaw( ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent )
            throws java.lang.Exception;

}