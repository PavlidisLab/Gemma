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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import java.util.List;

/**
 * @author kelsey
 */
public interface AuditTrailService {

    /*
     * Pre-post must happen at a different phase, because we end up with no transaction for checking security
     */

    // @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void addComment( Auditable auditable, String comment, String detail );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
        // @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    AuditEvent addUpdateEvent( Auditable auditable, AuditEventType auditEventType, String note );

    // @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    AuditEvent addUpdateEvent( Auditable auditable, AuditEventType auditEventType, String note, String detail );

    // @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, String note,
            String detail );

    /**
     * Add an update event defined by the given parameters, to the given auditable. Returns the generated event.
     */
    // @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    AuditEvent addUpdateEvent( Auditable auditable, String note );

    @Secured({ "GROUP_USER" })
    AuditTrail create( AuditTrail auditTrail );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    List<Auditable> getEntitiesWithEvent( Class<Auditable> entityClass,
            Class<? extends AuditEventType> auditEventClass );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<AuditEvent> getEvents( Auditable auditable );

}
