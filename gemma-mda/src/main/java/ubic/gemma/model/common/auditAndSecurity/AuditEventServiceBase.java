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

import java.util.Collection;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.Auditable;

/**
 * Spring Service base class for <code>AuditEventService</code>, provides access to all services and entities referenced
 * by this service.
 * 
 * @version $Id$
 * @see AuditEventService
 */
public abstract class AuditEventServiceBase implements AuditEventService {

    @Autowired
    private AuditEventDao auditEventDao;

    /**
     * @see AuditEventService#getNewSinceDate(Date)
     */
    @Override
    public Collection<Auditable> getNewSinceDate( final Date date ) {
        try {
            return this.handleGetNewSinceDate( date );
        } catch ( Throwable th ) {
            throw new AuditEventServiceException(
                    "Error performing 'AuditEventService.getNewSinceDate(Date date)' --> " + th, th );
        }
    }

    /**
     * @see AuditEventService#getUpdatedSinceDate(Date)
     */
    @Override
    public Collection<Auditable> getUpdatedSinceDate( final Date date ) {
        try {
            return this.handleGetUpdatedSinceDate( date );
        } catch ( Throwable th ) {
            throw new AuditEventServiceException(
                    "Error performing 'AuditEventService.getUpdatedSinceDate(Date date)' --> " + th, th );
        }
    }

    /**
     * Sets the reference to <code>auditEvent</code>'s DAO.
     */
    public void setAuditEventDao( AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    /**
     * @see AuditEventService#thaw(AuditEvent)
     */
    @Override
    public void thaw( final AuditEvent auditEvent ) {
        try {
            this.handleThaw( auditEvent );
        } catch ( Throwable th ) {
            throw new AuditEventServiceException(
                    "Error performing 'AuditEventService.thaw(AuditEvent auditEvent)' --> " + th, th );
        }
    }

    /**
     * Gets the reference to <code>auditEvent</code>'s DAO.
     */
    protected AuditEventDao getAuditEventDao() {
        return this.auditEventDao;
    }

    /**
     * Performs the core logic for {@link #getNewSinceDate(Date)}
     */
    protected abstract Collection<Auditable> handleGetNewSinceDate( Date date ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getUpdatedSinceDate(Date)}
     */
    protected abstract Collection<Auditable> handleGetUpdatedSinceDate( Date date ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(AuditEvent)}
     */
    protected abstract void handleThaw( AuditEvent auditEvent ) throws java.lang.Exception;

}