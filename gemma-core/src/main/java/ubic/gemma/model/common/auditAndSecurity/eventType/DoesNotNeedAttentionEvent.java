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
 * An event that occurs when a curator has validated the entity and indicated that it is "approved". This could be used
 * to indicate that all preprocessing is finished, for example.
 *
 * @author Paul
 */
public class DoesNotNeedAttentionEvent extends CurationDetailsEvent {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6425018808816856629L;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public DoesNotNeedAttentionEvent() {
    }

    @Override
    public void updateCurationDetails( CurationDetails curatable, AuditEvent auditEvent ) {
        curatable.setNeedsAttention( false );
        curatable.setLastNeedsAttentionEvent( auditEvent );
    }
}