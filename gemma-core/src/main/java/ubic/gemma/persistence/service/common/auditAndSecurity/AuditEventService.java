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

import gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationCollectionFilteringProvider;
import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
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

    /**
     * @see AuditEventDao#getEvents(Auditable)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<AuditEvent> getEvents( Auditable auditable );

    /**
     * @see AuditEventDao#getEvents(Auditable, Class)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    List<AuditEvent> getEvents( Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * @see AuditEventDao#getCreateEvents(Collection)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    <T extends Auditable> Map<T, AuditEvent> getCreateEvents( Collection<T> auditable );

    /**
     * @see AuditEventDao#getLastEvent(Auditable)
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    AuditEvent getLastEvent( Auditable auditable );

    /**
     * @see AuditEventDao#getLastEvent(Auditable, Class)
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * @see AuditEventDao#getLastEvent(Auditable, Class, Collection)
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type, Collection<Class<? extends AuditEventType>> excludedTypes );

    /**
     * @see AuditEventDao#getLastEvents(Class, Class)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    <T extends Auditable> Map<T, AuditEvent> getLastEvents( Class<T> auditableClass, Class<? extends AuditEventType> type );

    /**
     * For each event type, retrieve the latest event.
     *
     * @see AuditEventDao#getLastEvent(Auditable, Class)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    <T extends Auditable> Map<Class<? extends AuditEventType>, AuditEvent> getLastEvents( T auditable, Collection<Class<? extends AuditEventType>> types );

    /**
     * For each auditable and event type, retrieve the latest event.
     *
     * @see AuditEventDao#getLastEvents(Collection, Class)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    <T extends Auditable> Map<Class<? extends AuditEventType>, Map<T, AuditEvent>> getLastEvents(
            Collection<T> auditables, Collection<Class<? extends AuditEventType>> types );

    /**
     * Note that this security setting works even though auditables aren't necessarily securable; non-securable
     * auditables will be returned. See {@link AclEntryAfterInvocationCollectionFilteringProvider} and
     * {@code applicationContext-security.xml}.
     * @see AuditEventDao#getNewSinceDate(Class, Date)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    <T extends Auditable> Collection<T> getNewSinceDate( Class<T> auditableClass, Date date );

    /**
     * Note that this security setting works even though auditables aren't necessarily securable; non-securable
     * auditables will be returned. See {@link AclEntryAfterInvocationCollectionFilteringProvider} and
     * {@code applicationContext-security.xml}.
     * @see AuditEventDao#getUpdatedSinceDate(Class, Date)
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    <T extends Auditable> Collection<T> getUpdatedSinceDate( Class<T> auditableClass, Date date );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    boolean hasEvent( Auditable a, Class<? extends AuditEventType> type );

    void retainHavingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );

    void retainLackingEvent( Collection<? extends Auditable> a, Class<? extends AuditEventType> type );
}
