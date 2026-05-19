/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;

/**
 * Indicates that previous validation is being invalidated.
 *
 * @author Paul
 * @deprecated open a
 * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketType#GENERIC GENERIC} ticket
 * (or {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketType#BATCH_INFO_NEEDED}
 * when the trigger is missing batch information) via
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService#openTicket}
 * instead. See {@link CurationDetailsEvent} for the full migration map.
 */
@Deprecated
public class NeedsAttentionEvent extends NeedsAttentionAlteringEvent {

    @Override
    public void updateCurationDetails( CurationDetails curatable, AuditEvent auditEvent ) {
        curatable.setNeedsAttention( true );
        curatable.setLastNeedsAttentionEvent( auditEvent );
    }
}