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

import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * @author kelsey
 * @version $Id$
 */
public interface AuditTrailService {

    /**
     * 
     */

    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public void addComment( Auditable auditable, java.lang.String comment, java.lang.String detail );

    /**
     * 
     */
    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public void addOkFlag( Auditable auditable, java.lang.String comment, java.lang.String detail );

    /**
     * 
     */
    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public void addTroubleFlag( Auditable auditable, java.lang.String comment, java.lang.String detail );

    /**
     * <p>
     * Add an update event defined by the given parameters, to the given auditable. Returns the generated event.
     * </p>
     */
    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public AuditEvent addUpdateEvent( Auditable auditable, java.lang.String note );

    /**
     * 
     */
    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public AuditEvent addUpdateEvent( Auditable auditable, AuditEventType auditEventType, java.lang.String note );

    /**
     * 
     */
    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, java.lang.String note,
            java.lang.String detail );

    /**
     * 
     */
    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public AuditEvent addUpdateEvent( Auditable auditable, AuditEventType auditEventType, java.lang.String note,
            java.lang.String detail );

    /**
     * 
     */
    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public void addValidatedFlag( Auditable auditable, java.lang.String comment, java.lang.String detail );

    /**
     * 
     */
    @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    public void audit( Auditable entity, AuditEvent auditEvent );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public AuditTrail create( AuditTrail auditTrail );

    /**
     * Return the last 'trouble' event (if any), if it was after the last 'ok' or 'validated' event (if any). Return
     * null otherwise (indicating there is no trouble).
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public AuditEvent getLastTroubleEvent( Auditable auditable );

    /**
     * Return the last validation event (if any).
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public AuditEvent getLastValidationEvent( Auditable auditable );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public List<AuditEvent> getEvents( Auditable auditable );

}
