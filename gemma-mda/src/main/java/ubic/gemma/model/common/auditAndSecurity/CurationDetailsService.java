/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;

/**
 *
 * Service handling manipulation with Curation Details.
 *
 * This service does not handle Audit Trail processing of the events, and thus should only be accessed from the AuditTrailService
 * after a decision is made that an event might have changed the curation details.
 *
 * @author tesarst
 */
@Service
public class CurationDetailsService {

    @Autowired
    CurationDetailsDao curationDetailsDao;

    @Autowired
    AuditTrailService auditTrailService;

    /**
     * Processes the given audit event and updates the CurationDetails on the given curatable object. If necessary, sends
     * the event to the AuditTrail service to be added to the Curatables audit trail first.
     *
     * This method should only be called from {@link AuditTrailService}, as the passed event has to already exist in the
     * audit trail of the curatable object.
     * Only use this method directly if you do not want the event to show up in the curatable objects audit trail.
     *
     * @param auditEvent the event containing information about the update. Method only accepts audit events whose type
     *                   is one of {@link CurationDetailsEvent} extensions.
     */
    // @PreAuthorize("hasPermission(#auditable, 'write') or hasPermission(#auditable, 'administration')")
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( Curatable curatable, AuditEvent auditEvent ) {
        throw new NotYetImplementedException(  "Curation details service not yet implemented" );
        //TODO implement
    }

}
