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

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.BaseDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see AuditEvent
 * @see AuditEventService
 */
public interface AuditEventDao extends BaseDao<AuditEvent> {

    /**
     * Obtain the audit events associated to a given auditable.
     * <p>
     * Events are sorted by date in ascending order.
     */
    List<AuditEvent> getEvents( Auditable auditable );

    /**
     * Obtain all events with a non-null event type for a given auditable.
     * <p>
     * Events are sorted by date in ascending order.
     */
    List<AuditEvent> getEventsWithType( Auditable auditable );

    /**
     * Keyset-pagination counterpart to {@link #getEvents(Auditable)} &mdash;
     * cursor mode for {@code GET /datasets/{dataset}/auditEvents} (step 1q of
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md}).
     * <p>
     * Same scope as {@link #getEvents(Auditable)}: every {@link AuditEvent} on
     * the supplied {@link Auditable}'s {@link ubic.gemma.model.common.auditAndSecurity.AuditTrail}. Cursor mode forces a
     * single-component ascending {@code id} sort (different from the legacy
     * {@code date, id} ordering &mdash; {@code id} is the unique primary key
     * and the only column safe for keyset pagination under the step 1b
     * single-component-sort restriction; events on a trail are appended over
     * time so {@code id} order tracks {@code date} order in practice). The
     * auditable-trail scope is preserved across pages. Fetches {@code limit+1}
     * rows internally to detect {@code hasMore} without a separate
     * {@code COUNT(*)}; {@code totalElements} on the returned page is
     * {@code null} by default.
     *
     * @param auditable the auditable whose trail is being browsed; must have a
     *                  persistent {@link ubic.gemma.model.common.auditAndSecurity.AuditTrail}
     * @param cursor    previous-response cursor token (nullable for the first
     *                  page); must have {@code sortSpec == "+id"} and a
     *                  single-component numeric {@code keyTuple} or the call
     *                  throws {@link IllegalArgumentException}.
     * @param limit     page size; must be {@code > 0}
     */
    CursorPage<AuditEvent> getEventsByCursor( Auditable auditable, @Nullable Cursor cursor, int limit );

    /**
     * Obtain the creation events for the given auditables.
     * <p>
     * If an auditable has more than one creation event (which is in itself a bug), the earliest one is returned.
     */
    <T extends Auditable> Map<T, AuditEvent> getCreateEvents( Collection<T> auditables );

    /**
     * Obtain the latest event for a given auditable.
     */
    @Nullable
    AuditEvent getLastEvent( Auditable auditable );

    /**
     * Obtain the latest event of a given type for a given auditable.
     */
    @Nullable
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type );

    /**
     * Obtain the latest event of a given type, excluding a certain number of types.
     *
     * @param type          type of event to retrieve, augmented by its hierarchy
     * @param excludedTypes excluded event types (their hierarchy is also excluded)
     */
    @Nullable
    AuditEvent getLastEvent( Auditable auditable, Class<? extends AuditEventType> type, Collection<Class<? extends AuditEventType>> excludedTypes );

    /**
     * Obtain the latest event of any type for all given auditables — the batch counterpart of
     * {@link #getLastEvent(Auditable)}.
     * <p>
     * Same "latest" definition as the typed overloads (max date, max id on tie) and the same
     * requirement that the event carry an {@link AuditEventType}: untyped rows (generic
     * {@code action='U'} auto-updates and the {@code action='C'} creation row) are skipped, so an
     * auditable whose trail holds nothing but those is absent from the result map. Use
     * {@link #getCreateEvents(Collection)} to cover that case.
     * <p>
     * Prefer this over passing {@link AuditEventType} to
     * {@link #getLastEvents(Collection, Class)}: the typed form expands the requested class into
     * its subclass closure and emits a {@code type(et) in (...)} predicate three times over, which
     * for the root type is every mapped event class and filters nothing.
     */
    <T extends Auditable> Map<T, AuditEvent> getLastEvents( Collection<T> auditables );

    /**
     * Obtain the latest events of a specified type for all given auditables.
     *
     * @see #getLastEvent(Auditable, Class)
     */
    <T extends Auditable> Map<T, AuditEvent> getLastEvents( Collection<T> auditables, Class<? extends AuditEventType> type );

    /**
     * Obtain the latest events of a specified type for all auditable of a given type.
     * <p>
     * 🛑 This resolves through the denormalised {@code AuditTrail.lastEvent} pointer, so it answers
     * "which auditables have an event of this type as their LATEST event, and what is it" — not
     * "what is each auditable's latest event of this type". An auditable whose newest event is of
     * some other type is absent from the map even when it does carry an event of the requested
     * type. {@link #getLastEvents(Collection, Class)} is the overload with per-type semantics; it
     * takes the max over the type-filtered events of each trail.
     *
     * @see #getLastEvent(Auditable, Class)
     */
    <T extends Auditable> Map<T, AuditEvent> getLastEvents( Class<T> auditableClass, Class<? extends AuditEventType> type );

    /**
     * Obtain the ids of every auditable of a class whose trail carries at least one event of any of
     * the given types.
     * <p>
     * One query for the whole corpus, whatever the number of types: the type closures are unioned
     * into a single {@code type(et) in (...)} predicate. Unlike
     * {@link #getLastEvents(Class, Class)} this does not go through {@code AuditTrail.lastEvent},
     * so an event buried anywhere in the trail counts.
     * <p>
     * 🛑 The result is NOT ACL-filtered — it is a set of bare ids, which carries no securable
     * object for an ACL check to act on. It is a narrowing step: intersect it with an
     * ACL-filtered load before any of these ids reaches a caller.
     *
     * @param types event types; each is expanded to its subclass closure, as elsewhere in this DAO
     */
    <T extends Auditable> Set<Long> getIdsHavingEvent( Class<T> auditableClass, Collection<Class<? extends AuditEventType>> types );

    /**
     * Get auditables that have been created since the given date.
     */
    <T extends Auditable> Collection<T> getNewSinceDate( Class<T> auditableClass, Date date );

    /**
     * Get auditables that have been updated since the given date.
     * <p>
     * "Updated" here means the auditable received at least one typed {@link AuditEvent} (i.e.
     * {@code eventType IS NOT NULL}) in the window. Generic auto-UPDATE rows
     * ({@code action='U'}, {@code eventType=null}) are intentionally NOT counted: that machinery
     * is being retired (see {@code AUDIT_SYSTEM_AUDIT.md} Section 5, risk #1), so this query
     * defines "updated" purely in terms of semantic, typed events.
     */
    <T extends Auditable> Collection<T> getUpdatedSinceDate( Class<T> auditableClass, Date date );
}
