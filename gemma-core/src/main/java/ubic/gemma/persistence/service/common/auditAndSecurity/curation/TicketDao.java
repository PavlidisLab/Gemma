/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.persistence.service.BaseDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import java.util.List;

/**
 * Persistence operations for {@link Ticket} (Phase B-1 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}).
 *
 * @author paul
 */
public interface TicketDao extends BaseDao<Ticket> {

    /**
     * Find all tickets that are not in a terminal state and target the
     * given entity. Used to answer "are there any open curation tickets
     * for this EE / array design?". Implements the lookup path described
     * in Decision 2: a composite index over {@code (target_type, target_id)}
     * serves it without joining through {@code ticket_id}.
     */
    List<Ticket> findOpenForTarget( TicketTargetType targetType, Long targetId );

    /**
     * Find all tickets currently assigned to the given contact, regardless
     * of state. Used by curator dashboards.
     */
    List<Ticket> findAssignedTo( Contact assignee );

    /**
     * Paged, filtered list query for the REST surface (Phase B-2). All filter
     * arguments are independently optional; passing {@code null} for each one
     * disables that filter. Results are ordered by {@code updatedAt} desc.
     *
     * @param openOnly      if true, only OPEN/IN_PROGRESS tickets
     * @param assigneeId    filter by current assignee {@link Contact#getId()} (nullable)
     * @param priority      filter by priority (nullable)
     * @param offset        first row to return (0-based)
     * @param limit         max rows to return; values &lt;= 0 are treated as "no limit"
     */
    List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority, int offset, int limit );

    /**
     * Count tickets matching the same filters as
     * {@link #findTickets(boolean, Long, TicketPriority, int, int)}.
     */
    long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority );

    /**
     * Keyset-pagination counterpart to {@link #findOpenForTarget(TicketTargetType, Long)}
     * &mdash; cursor mode for {@code GET /datasets/{dataset}/tickets} and the
     * {@code GET /platforms/{platform}/tickets} sibling (step 1p of
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md}).
     * <p>
     * Same scope as {@link #findOpenForTarget}: tickets in a non-terminal state
     * ({@code OPEN}/{@code IN_PROGRESS}) whose {@link TicketTarget} matches
     * {@code (targetType, targetId)}. Cursor mode forces a single-component
     * ascending {@code t.id} sort; the {@code targetType}/{@code targetId}
     * scope is preserved across pages. Fetches {@code limit+1} rows internally
     * to detect {@code hasMore} without a separate {@code COUNT(*)};
     * {@code totalElements} on the returned page is {@code null} by default.
     *
     * @param targetType the {@link TicketTargetType} (e.g. EXPRESSION_EXPERIMENT, ARRAY_DESIGN)
     * @param targetId   the target entity id
     * @param cursor     previous-response cursor token (nullable for the first page);
     *                   must have {@code sortSpec == "+id"} and a single-component
     *                   numeric {@code keyTuple} or the call throws
     *                   {@link IllegalArgumentException}.
     * @param limit      page size; must be {@code > 0}
     */
    CursorPage<Ticket> findOpenForTargetByCursor( TicketTargetType targetType, Long targetId,
            @Nullable Cursor cursor, int limit );

    /**
     * Keyset-pagination counterpart to {@link #findTickets(boolean, Long, TicketPriority, int, int)}
     * &mdash; cursor mode for {@code GET /tickets} (step 1o of
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md}).
     * <p>
     * Unlike the offset variant (ordered by {@code t.updatedAt desc} for human-readable
     * dashboards), cursor mode forces a single-component ascending {@code t.id} sort
     * because the cursor DAO restricts cursors to id-only sorts until the indexed-column
     * audit in phase B lands. The filter triple ({@code openOnly}, {@code assigneeId},
     * {@code priority}) is shared with the offset variant so cursor mode honours the
     * same scope. Fetches {@code limit+1} rows internally to detect {@code hasMore}
     * without a separate {@code COUNT(*)}; {@code totalElements} on the returned page
     * is {@code null} by default.
     *
     * @param openOnly    if true, only OPEN/IN_PROGRESS tickets (same as offset variant)
     * @param assigneeId  filter by current assignee id (nullable, same as offset variant)
     * @param priority    filter by priority (nullable, same as offset variant)
     * @param cursor      previous-response cursor token (nullable for the first page);
     *                    must have {@code sortSpec == "+id"} and a single-component
     *                    numeric {@code keyTuple} or the call throws
     *                    {@link IllegalArgumentException}.
     * @param limit       page size; must be {@code > 0}
     */
    CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable Cursor cursor, int limit );

    /**
     * Keyset-pagination counterpart to the {@code Ticket.events} collection on a
     * single ticket &mdash; cursor mode for {@code GET /tickets/{id}/events}
     * (step 1r of {@code CURSOR_PAGINATION_STEP1_PLAN.md}).
     * <p>
     * Same scope as iterating {@link Ticket#getEvents()}: every
     * {@link TicketEvent} whose {@code ticket} FK matches the supplied ticket.
     * Cursor mode forces a single-component ascending {@code id} sort
     * (different from the legacy {@code occurredAt} ordering &mdash; {@code id}
     * is the unique primary key and the only column safe for keyset pagination
     * under the step 1b single-component-sort restriction; events on a ticket
     * are appended monotonically over time so {@code id} order tracks
     * {@code occurredAt} order in practice). The ticket scope is preserved
     * across pages. Fetches {@code limit+1} rows internally to detect
     * {@code hasMore} without a separate {@code COUNT(*)};
     * {@code totalElements} on the returned page is {@code null} by default.
     *
     * @param ticket the ticket whose events are being browsed; must have a
     *               persistent id
     * @param cursor previous-response cursor token (nullable for the first
     *               page); must have {@code sortSpec == "+id"} and a
     *               single-component numeric {@code keyTuple} or the call
     *               throws {@link IllegalArgumentException}.
     * @param limit  page size; must be {@code > 0}
     */
    CursorPage<TicketEvent> findEventsByCursor( Ticket ticket, @Nullable Cursor cursor, int limit );
}
