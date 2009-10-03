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
package ubic.gemma.model.common;

import java.util.Map;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;

/**
 * @version $Id$
 */
public interface AuditableService {

    /**
     * 
     */
    public java.util.Collection<AuditEvent> getEvents( ubic.gemma.model.common.Auditable auditable );

    /**
     * <p>
     * Return a map of Auditables to AuditEvents for the given AuditEventType.
     * </p>
     */
    public Map<Auditable, AuditEvent> getLastAuditEvent( java.util.Collection<? extends Auditable> auditables,
            ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type );

    /**
     * <p>
     * Returns the last AuditEvent of the specified type.
     * </p>
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastAuditEvent(
            ubic.gemma.model.common.Auditable auditable,
            ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType type );

}
