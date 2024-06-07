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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author paul
 */
public interface AuditEventService {

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<AuditEvent> getEvents( Auditable auditable );

    @Transactional(readOnly = true)
    Map<Auditable, AuditEvent> getCreateEvents( Collection<? extends Auditable> auditable );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type, Collection<Class<? extends AuditEventType>> excludedTypes );

    /**
     * Fast method to retrieve auditEventTypes of multiple classes.
     *
     * @param types      types
     * @param auditables auditables
     * @return map of AuditEventType to a Map of Auditable to the AuditEvent matching that type.
     * Note: cannot secure this very easily since map key is a Class.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Map<Class<? extends AuditEventType>, Map<Auditable, AuditEvent>> getLastEvents(
            Collection<? extends Auditable> auditables, Collection<Class<? extends AuditEventType>> types );

    /**
     * @param date date
     * @return a collection of Auditables created since the date given.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<Auditable> getNewSinceDate( Date date );

    /**
     * @param date date
     * @return a collection of Auditable objects that were updated since the date entered.
     * Note that this security setting works even though auditables aren't necessarily securable; non-securable
     * auditables will be returned. See AclEntryAfterInvocationCollectionFilteringProvider and
     * applicationContext-security.xml
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<Auditable> getUpdatedSinceDate( Date date );

    boolean hasEvent( Auditable a, Class<? extends AuditEventType> type );

    void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

    void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

}
