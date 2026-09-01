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
import ubic.gemma.model.common.auditAndSecurity.curation.TicketSearchHitValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.BaseDao;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
     * Paged, filtered list query for the REST surface — extended filter set
     * (queue-filter expansion). All filter arguments are independently
     * optional; passing {@code null} disables that filter. Results are
     * ordered by {@code updatedAt} desc.
     *
     * <p>When {@code state} is non-null it OVERRIDES the {@code openOnly}
     * predicate; pass any boolean for {@code openOnly} and it is ignored.
     * When {@code state} is null, {@code openOnly == true} continues to
     * restrict to OPEN/IN_PROGRESS (the legacy behaviour).</p>
     *
     * @param openOnly     if true and {@code state} is null, restrict to OPEN/IN_PROGRESS
     * @param assigneeId   filter by current assignee {@link Contact#getId()} (nullable)
     * @param priority     filter by priority (nullable)
     * @param type         filter by {@link TicketType} (nullable)
     * @param state        filter by exact {@link TicketState}; overrides {@code openOnly} when non-null
     * @param targetType   filter to tickets whose target collection includes a target of this type (nullable)
     * @param updatedSince filter to tickets with {@code updatedAt >= updatedSince} (nullable)
     * @param offset       first row to return (0-based)
     * @param limit        max rows to return; values &lt;= 0 are treated as "no limit"
     */
    List<Ticket> findTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince, int offset, int limit );

    /**
     * Count tickets matching the same filters as
     * {@link #findTickets(boolean, Long, TicketPriority, int, int)}.
     */
    long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority );

    /**
     * Count tickets matching the same extended filter set as
     * {@link #findTickets(boolean, Long, TicketPriority, TicketType, TicketState, TicketTargetType, Date, int, int)}.
     */
    long countTickets( boolean openOnly, @Nullable Long assigneeId, @Nullable TicketPriority priority,
            @Nullable TicketType type, @Nullable TicketState state, @Nullable TicketTargetType targetType,
            @Nullable Date updatedSince );

    /**
     * Keyset-pagination counterpart to {@link #findOpenForTarget(TicketTargetType, Long)}
     * &mdash; cursor mode for {@code GET /datasets/{dataset}/tickets} and the
     * {@code GET /platforms/{platform}/tickets} sibling (step 1p of
     * {@code CURSOR_PAGINATION_STEP1_PLAN.md}).
     * <p>
     * Same scope as {@link #findOpenForTarget}: tickets in a non-terminal state
     * ({@code OPEN}/{@code IN_PROGRESS}) whose {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget} matches
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
     * Extended-filter cursor-mode counterpart to
     * {@link #findTickets(boolean, Long, TicketPriority, TicketType, TicketState, TicketTargetType, Date, int, int)}.
     * Same id-asc single-component cursor semantics as
     * {@link #findTicketsByCursor(boolean, Long, TicketPriority, Cursor, int)};
     * accepts the same extended filter set as the offset variant, with identical
     * {@code state}-overrides-{@code openOnly} precedence when {@code state} is non-null.
     *
     * @see #findTicketsByCursor(boolean, Long, TicketPriority, Cursor, int)
     */
    CursorPage<Ticket> findTicketsByCursor( boolean openOnly, @Nullable Long assigneeId,
            @Nullable TicketPriority priority, @Nullable TicketType type, @Nullable TicketState state,
            @Nullable TicketTargetType targetType, @Nullable Date updatedSince,
            @Nullable Cursor cursor, int limit );

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

    /**
     * Count open ({@code OPEN} + {@code IN_PROGRESS}) tickets grouped by
     * {@link TicketType}. Used by the admin curation-status surface.
     */
    Map<TicketType, Long> countOpenByType();

    /**
     * Count tickets in a non-terminal state ({@code OPEN} + {@code IN_PROGRESS}).
     */
    long countOpen();

    /**
     * @return the earliest {@code createdAt} across every ticket in a
     *         non-terminal state, or {@code null} if no open tickets exist.
     *         Used to surface "oldest open ticket age" on the admin
     *         curation-status surface.
     */
    @Nullable
    Date findOldestOpenCreatedAt();

    /**
     * Look one ticket up by id and project it to a {@link TicketSearchHitValueObject}, honouring the
     * same visibility rules as {@link #findSearchHitsByTitle}. Backs the "typed a ticket number"
     * half of {@code GET /tickets/search}.
     * <p>
     * Separate from the title query rather than folded into its {@code ORDER BY}: the ticket a
     * curator names by number can be older than the {@code limit} most recently touched title
     * matches, and a hit pushed off the end of that window is indistinguishable from one that does
     * not exist.
     *
     * @param openOnly            if true, only OPEN/IN_PROGRESS tickets are hits
     * @param scratchpadOwnerId   contact id of the caller, whose own {@link TicketType#SCRATCHPAD}
     *                            tickets are hits; null admits nobody's
     * @return the hit, or {@code null} when no ticket has that id or it is filtered out. An id that
     *         names no ticket is a non-hit, never an error.
     */
    @Nullable
    TicketSearchHitValueObject findSearchHitById( Long id, boolean openOnly, @Nullable Long scratchpadOwnerId );

    /**
     * Find tickets whose title contains {@code titleFragment}, case-insensitively, projected to
     * {@link TicketSearchHitValueObject} and ordered by {@code updatedAt} descending. Backs the
     * "typed some of the title" half of {@code GET /tickets/search}.
     * <p>
     * {@code targetCount} on each hit is counted by the database; no {@code TicketTarget} row is
     * fetched, which is the point of the endpoint.
     *
     * @param titleFragment     matched as a substring; its {@code LIKE} wildcards are escaped, so a
     *                          title fragment containing {@code %} or {@code _} matches literally
     * @param openOnly          if true, only OPEN/IN_PROGRESS tickets are hits
     * @param scratchpadOwnerId contact id of the caller, whose own {@link TicketType#SCRATCHPAD}
     *                          tickets are hits; null admits nobody's
     * @param limit             max rows to return; values &lt;= 0 are treated as "no limit"
     */
    List<TicketSearchHitValueObject> findSearchHitsByTitle( String titleFragment, boolean openOnly,
            @Nullable Long scratchpadOwnerId, int limit );
}
