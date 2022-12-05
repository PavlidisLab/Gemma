/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.common.auditAndSecurity;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.User;

/**
 * This is to allow for a single way to deal with audit events and status update. Note: Security is not checked. Note: Only used from
 * AuditAdvice for now.
 *
 * @author anton
 */
public interface AuditHelper {

    /**
     * Add AuditAction.UPDATE event and update Status.
     *
     * @param auditable auditable
     * @param note      note
     * @param user      user
     * @return new event
     */
    @SuppressWarnings("UnusedReturnValue")
    // Possible external use
    AuditEvent addUpdateAuditEvent( Auditable auditable, String note, User user );

}
