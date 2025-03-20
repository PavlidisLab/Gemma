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
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;

/**
 * Event types that can change {@link CurationDetails} of {@link Curatable} objects.
 *
 * @author tesarst
 */
public abstract class CurationDetailsEvent extends AuditEventType {

    /**
     * This method should be overloaded in all of the extensions of this class to do the specific actions they wre designed for.
     *
     * @param curatable  the curatable object to do the curation action on.
     * @param auditEvent the audit event containing information about the action that should be made.
     */
    public abstract void updateCurationDetails( CurationDetails curatable, AuditEvent auditEvent );
}