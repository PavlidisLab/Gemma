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
}
