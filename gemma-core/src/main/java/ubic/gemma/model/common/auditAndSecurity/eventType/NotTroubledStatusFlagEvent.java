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
 * This event type resets the trouble flag of curation details of a curatable object.
 *
 * @author Paul
 */
public class NotTroubledStatusFlagEvent extends TroubledStatusFlagAlteringEvent {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8586752080144045085L;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public NotTroubledStatusFlagEvent() {
    }

    @Override
    public void updateCurationDetails( CurationDetails curatable, AuditEvent auditEvent ) {
        if ( !curatable.getTroubled() ) {
            throw new IllegalArgumentException( "Cannot mark an already non-troubled curatable as non-troubled." );
        }
        curatable.setTroubled( false );
        curatable.setLastTroubledEvent( auditEvent );
    }

}