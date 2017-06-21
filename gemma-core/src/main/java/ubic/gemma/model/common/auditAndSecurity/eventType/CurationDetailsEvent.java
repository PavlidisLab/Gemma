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

/**
 * Event types that can change CurationDetails of Curatable objects.
 * Anytime a new extension of this event type is implemented, add a new handler to the
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao#update(ubic.gemma.model.common.auditAndSecurity.curation.Curatable, ubic.gemma.model.common.auditAndSecurity.AuditEvent)}
 * method.
 *
 * @author tesarst
 */
public abstract class CurationDetailsEvent
        extends ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6621758826080039878L;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public CurationDetailsEvent() {
    }

    /**
     * This method should be overloaded in all of the extensions of this class to do the specific actions they wre designed for.
     *
     * @param curatable  the curatable object to do the curation action on.
     * @param auditEvent the audit event containing information about the action that should be made.
     */
    public abstract void setCurationDetails( Curatable curatable, AuditEvent auditEvent );

    /**
     * Throws an exception, as CurationDetailsEvent can not be instantiated.
     */
    public static final class Factory {
        /**
         * Throws an UnsupportedOperationException
         * {@link CurationDetailsEvent}.
         */
        public static CurationDetailsEvent newInstance() {
            throw new UnsupportedOperationException(
                    "CurationDetailsEvent can not be instantiated. It only serves as a wrapper class for its descendants. Use a more specific event type." );
        }

    }

}