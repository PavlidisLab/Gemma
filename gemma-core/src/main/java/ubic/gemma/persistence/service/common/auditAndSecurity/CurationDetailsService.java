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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;

/**
 * Service handling manipulation with Curation Details.
 * This service does not handle Audit Trail processing of the events, and thus should only be accessed from the AuditTrailService
 * after a decision is made that an event might have changed the curation details.
 *
 * @author tesarst
 */
public interface CurationDetailsService {

    /**
     * This method should only be called from {@link AuditTrailService}, as the passed event has to already exist in the
     * audit trail of the curatable object.
     * Only use this method directly if you do not want the event to show up in the curatable objects audit trail.
     *
     * @param auditEvent the event containing information about the update. Method only accepts audit events whose type
     *                   is one of {@link CurationDetailsEvent} extensions.
     * @param curatable  curatable
     */
    @Secured({ "GROUP_AGENT", "ACL_SECURABLE_EDIT" })
    void update( Curatable curatable, AuditEvent auditEvent );
}
