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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.BaseService;

import java.util.List;

/**
 * @author kelsey
 */
public interface AuditTrailService extends BaseService<AuditTrail> {

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    AuditEvent addUpdateEvent( Auditable auditable, AuditEventType auditEventType, String note );

    @Secured({ "GROUP_AGENT", "ACL_SECURABLE_EDIT" })
    AuditEvent addUpdateEvent( Auditable auditable, AuditEventType auditEventType, String note, String detail );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    AuditEvent addUpdateEvent( Auditable auditable, Class<? extends AuditEventType> type, String note, String detail );

    /**
     * Add an update event defined by the given parameters, to the given auditable. Returns the generated event.
     *
     * @param auditable auditable
     * @param note      note
     * @return created event
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    AuditEvent addUpdateEvent( Auditable auditable, String note );

    @Override
    @Secured({ "GROUP_USER" })
    void create( AuditTrail auditTrail );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<AuditEvent> getEvents( Auditable auditable );

}
