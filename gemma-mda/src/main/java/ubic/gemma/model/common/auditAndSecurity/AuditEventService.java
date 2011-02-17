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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

/**
 * @author paul
 * @version $Id$
 */
public interface AuditEventService {

    /**
     * Returns a collection of Auditables created since the date given.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<Auditable> getNewSinceDate( java.util.Date date );

    /**
     * Returns a collection of Auditable objects that were updated since the date entered.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<Auditable> getUpdatedSinceDate( java.util.Date date );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY" })
    public void thaw( ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent );

    /**
     * @param auditable
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY" })
    public List<AuditEvent> getEvents( Auditable auditable );

    /**
     * @param auditable
     * @param type
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * Return a map of Auditables to AuditEvents for the given AuditEventType.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public Map<Auditable, AuditEvent> getLastEvent( java.util.Collection<? extends Auditable> auditables,
            Class<? extends AuditEventType> type );

    /**
     * Fast method to retrieve auditEventTypes of multiple classes.
     * 
     * @param auditables
     * @param types
     * @return map of AuditEventType to a Map of Auditable to the AuditEvent matching that type.
     *         <p>
     *         Note: cannot secure this very easily since map key is a Class.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY" })
    public Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types );

    /**
     * @param events
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY" })
    public AuditEvent getLastOutstandingTroubleEvent( Collection<AuditEvent> events );

}
