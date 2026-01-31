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
 * Event indicating that a curation note has added.
 *
 * @author Paul
 */
public class CurationNoteUpdateEvent extends CurationDetailsEvent {

    /**
     * @param curationDetails the curation details do the curation action on.
     * @param auditEvent the audit event containing information about the action that should be made.
     *                   With the CurationNoteUpdateEvent, this method expects the auditEvent Note property to
     *                   contain the new text of the note. This can be any String object, including a null object.
     */
    @Override
    public void updateCurationDetails( CurationDetails curationDetails, AuditEvent auditEvent ) {
        curationDetails.setCurationNote( auditEvent.getNote() );
        curationDetails.setLastNoteUpdateEvent( auditEvent );
    }

}