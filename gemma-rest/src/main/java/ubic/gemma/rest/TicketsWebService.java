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
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketMode;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetStatus;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.CursorPaginatedResponseDataObject;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.CursorArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.paginateByCursor;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for curation tickets (Phase B-2 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}). Read endpoints are open to any caller
 * the rest of the v2 surface accepts; write endpoints (POST/PUT/DELETE)
 * require an authenticated principal — anonymous callers get a 401/403 via
 * {@link PreAuthorize}.
 *
 * <p>DELETE is a SOFT close: the ticket is transitioned to
 * {@link TicketState#CANCELLED} and a CANCELLED event is appended. The
 * ticket row + its event log are preserved (Decision 4 of the recce:
 * append-only).</p>
 *
 * @author paul
 */
@Service
@Path("/tickets")
@Tag(name = "Tickets", description = "Curation workflow tickets")
public class TicketsWebService {

    private final TicketService ticketService;
    private final UserManager userManager;
    private final UserReadService userReadService;

    @Autowired
    public TicketsWebService( TicketService ticketService, UserManager userManager, UserReadService userReadService ) {
        this.ticketService = ticketService;
        this.userManager = userManager;
        this.userReadService = userReadService;
    }

    /**
     * List tickets with optional filters and offset/limit or cursor pagination.
     * <p>
     * Step 1o of {@code CURSOR_PAGINATION_STEP1_PLAN.md} adds opt-in keyset
     * (cursor) pagination alongside the legacy offset path. Legacy mode keeps
     * the {@code t.updatedAt desc} ordering used for human-readable
     * dashboards; cursor mode forces a single-component ascending {@code id}
     * sort because the cursor DAO restricts cursors to id-only sorts until
     * the phase-B indexed-column audit lands.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List curation tickets",
            description = "Filterable, paginated list of curation tickets. "
                    + "Event logs are NOT included for payload economy; fetch /{id} for events.\n\n"
                    + "Supports two pagination modes. Legacy mode: pass `offset` (and `limit`); "
                    + "response includes `offset` and `totalElements`; ordering is "
                    + "`updatedAt` desc (the dashboard-friendly default). "
                    + "Cursor mode (recommended for deep listings and consistency under writes "
                    + "as the ticket table grows): pass an opaque `cursor` token from a "
                    + "previous response's `nextCursor` / `prevCursor` field. "
                    + "`offset` and `cursor` are mutually exclusive — passing a non-null "
                    + "`cursor` selects cursor mode. In cursor mode the result is always "
                    + "sorted by ascending `id` (cursor mode forces a single-component id "
                    + "sort pending the indexed-column audit in phase B); the full filter set "
                    + "(`openOnly`, `assignee`, `priority`, `type`, `state`, `targetType`, "
                    + "`updatedSince`) is honoured identically in both modes; `totalElements` "
                    + "is `null` by default (no count query per request).\n\n"
                    + "Filter precedence note: `state` and `openOnly` are mutually exclusive — "
                    + "a passed `state` pins the predicate to that single state and the "
                    + "`openOnly` flag is ignored. With no `state` parameter, `openOnly=true` "
                    + "retains its legacy OPEN+IN_PROGRESS semantics.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    PaginatedResponseDataObject.class,
                                    CursorPaginatedResponseDataObject.class
                            }))),
            })
    public Object getTickets(
            @Parameter(description = "If true, restrict to OPEN/IN_PROGRESS tickets. Ignored when `state` is supplied.")
            @QueryParam("openOnly") @DefaultValue("false") boolean openOnly,
            @Parameter(description = "Filter by current assignee (Contact id).")
            @QueryParam("assignee") @Nullable Long assigneeId,
            @Parameter(description = "Filter by priority.")
            @QueryParam("priority") @Nullable TicketPriority priority,
            @Parameter(description = "Filter by ticket type (e.g. GENERIC, AGENT_PROPOSAL, NEEDS_ATTENTION).")
            @QueryParam("type") @Nullable TicketType type,
            @Parameter(description = "Filter by exact ticket state (OPEN, IN_PROGRESS, RESOLVED, CANCELLED). When supplied this overrides `openOnly`.")
            @QueryParam("state") @Nullable TicketState state,
            @Parameter(description = "Filter to tickets whose target collection includes a target of this type (EXPRESSION_EXPERIMENT, ARRAY_DESIGN, etc).")
            @QueryParam("targetType") @Nullable TicketTargetType targetType,
            @Parameter(description = "ISO-8601 date/time; restrict to tickets with `updatedAt >=` this value.")
            @QueryParam("updatedSince") @Nullable Date updatedSince,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg,
            @Parameter(description = "Opaque keyset-pagination cursor token; mutually exclusive with `offset`.")
            @QueryParam("cursor") CursorArg cursorArg
    ) {
        int limit = limitArg.getValue();
        if ( cursorArg != null ) {
            // Mutual-exclusion: a non-null cursor selects cursor mode. The default offset=0 is
            // not considered user-supplied (parallels GET /genes step 1b and the other 1c-1n
            // conversions). In cursor mode the full filter set (openOnly, assigneeId, priority,
            // type, state, targetType, updatedSince) is honoured identically; the legacy
            // updatedAt-desc sort is swapped for a single-component +id ascending sort because
            // the DAO restricts cursors to id-only sorts until the phase-B index audit lands.
            CursorPage<Ticket> page = ticketService.findTicketsByCursor(
                    openOnly, assigneeId, priority, type, state, targetType, updatedSince,
                    cursorArg.getValue(), limit );
            CursorPage<TicketValueObject> voPage = page.map( TicketValueObject::from );
            return paginateByCursor( voPage, new String[] { "id" } );
        }
        int offset = offsetArg.getValue();
        List<Ticket> tickets = ticketService.findTickets( openOnly, assigneeId, priority,
                type, state, targetType, updatedSince, offset, limit );
        long total = ticketService.countTickets( openOnly, assigneeId, priority,
                type, state, targetType, updatedSince );
        List<TicketValueObject> vos = tickets.stream()
                .map( TicketValueObject::from )
                .collect( Collectors.toList() );
        Slice<TicketValueObject> slice = new Slice<>( vos, null, offset, limit, total );
        return paginate( slice, new String[] { "id" } );
    }

    /**
     * Calling admin's own ticket queue: assigned-to-me + the few cheap counters that
     * back a "My Queue" card in the curation-UI. Splits assigned tickets into open
     * (OPEN + IN_PROGRESS) and recently-resolved buckets — both capped per request
     * to keep the response compact.
     */
    @GET
    @Path("/mine")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "List tickets assigned to the calling admin",
            description = "Convenience view over `GET /tickets?assignee={me}`. Returns two lists: `open` (OPEN + IN_PROGRESS, capped at `limit`, default 50) sorted by `updatedAt desc`; and `recentlyResolved` (RESOLVED + CANCELLED with updatedAt within the last `resolvedWithinDays` days, default 7, capped at `limit`). Sorted by updatedAt desc. Both lists carry the lightweight TicketValueObject (no event log). Use `GET /tickets/{id}` for the full ticket including events.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<MyQueueResponse> getMyQueue(
            @Parameter(description = "Max items in each list. Defaults to 50, capped at 200.")
            @QueryParam("limit") @DefaultValue("50") int limit,
            @Parameter(description = "Window for `recentlyResolved`, in days. Defaults to 7.")
            @QueryParam("resolvedWithinDays") @DefaultValue("7") int resolvedWithinDays
    ) {
        Long meId = currentUserContactId();
        int cappedLimit = Math.min( Math.max( limit, 1 ), 200 );

        // Open + InProgress: reuse findTickets(openOnly=true, assignee=me)
        List<Ticket> open = ticketService.findTickets( true, meId, null, 0, cappedLimit );

        // Recently-resolved: pull a generous superset of resolved-by-me, filter by date in-memory.
        // The dao doesn't expose a state+since filter today; pulling cappedLimit*3 keeps the
        // payload bounded while still letting us trim to the time window. If this proves too
        // coarse the next iteration can add a TicketDao.findResolvedSince(assignee, since, limit).
        List<Ticket> resolvedSuperset = ticketService.findTickets( false, meId, null, 0, cappedLimit * 3 );
        long cutoffMillis = System.currentTimeMillis() - resolvedWithinDays * 24L * 3600L * 1000L;
        List<Ticket> recentlyResolved = new ArrayList<>();
        for ( Ticket t : resolvedSuperset ) {
            if ( t.getState() == TicketState.RESOLVED || t.getState() == TicketState.CANCELLED ) {
                Date updated = t.getUpdatedAt();
                if ( updated != null && updated.getTime() >= cutoffMillis ) {
                    recentlyResolved.add( t );
                    if ( recentlyResolved.size() >= cappedLimit ) break;
                }
            }
        }

        MyQueueResponse body = new MyQueueResponse();
        body.assigneeContactId = meId;
        body.openLimit = cappedLimit;
        body.resolvedWithinDays = resolvedWithinDays;
        body.open = open.stream().map( TicketValueObject::from ).collect( Collectors.toList() );
        body.recentlyResolved = recentlyResolved.stream().map( TicketValueObject::from ).collect( Collectors.toList() );
        body.openCount = body.open.size();
        body.recentlyResolvedCount = body.recentlyResolved.size();
        return respond( body );
    }

    /**
     * Lightweight counters about the calling admin's ticket workload. Cheap (two count
     * queries + one find for oldest-open); intended for top-of-page badges.
     */
    @GET
    @Path("/summary/me")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Workload summary for the calling admin",
            description = "Returns counts of open (OPEN+IN_PROGRESS) and total tickets assigned to the calling admin, plus the age of the oldest open ticket in days. No event logs, no list of tickets — see `/tickets/mine` for those.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<MyQueueSummaryResponse> getMyQueueSummary() {
        Long meId = currentUserContactId();
        long openCount = ticketService.countTickets( true, meId, null );
        long totalCount = ticketService.countTickets( false, meId, null );
        // Oldest open: ask for one ticket sorted ascending by updatedAt? The current dao sorts
        // desc; for the dashboard we read the top-1 of a generous slice and pick the min created.
        Long oldestOpenAgeDays = null;
        if ( openCount > 0 ) {
            List<Ticket> openSlice = ticketService.findTickets( true, meId, null, 0, 200 );
            long oldestCreated = Long.MAX_VALUE;
            for ( Ticket t : openSlice ) {
                if ( t.getCreatedAt() != null && t.getCreatedAt().getTime() < oldestCreated ) {
                    oldestCreated = t.getCreatedAt().getTime();
                }
            }
            if ( oldestCreated != Long.MAX_VALUE ) {
                oldestOpenAgeDays = ( System.currentTimeMillis() - oldestCreated ) / ( 24L * 3600L * 1000L );
            }
        }
        MyQueueSummaryResponse body = new MyQueueSummaryResponse();
        body.assigneeContactId = meId;
        body.openCount = openCount;
        body.totalCount = totalCount;
        body.oldestOpenAgeDays = oldestOpenAgeDays;
        return respond( body );
    }

    private Long currentUserContactId() {
        User u = userManager.getCurrentUser();
        if ( u == null ) {
            throw new jakarta.ws.rs.NotAuthorizedException( "anonymous callers have no ticket queue" );
        }
        return u.getId();
    }

    public static class MyQueueResponse {
        public Long assigneeContactId;
        public int openLimit;
        public int resolvedWithinDays;
        public int openCount;
        public int recentlyResolvedCount;
        public List<TicketValueObject> open;
        public List<TicketValueObject> recentlyResolved;
    }

    public static class MyQueueSummaryResponse {
        public Long assigneeContactId;
        public long openCount;
        public long totalCount;
        @Nullable
        public Long oldestOpenAgeDays;
    }

    /**
     * Global open-ticket roll-up for the admin dashboard's TicketsSection. Sums
     * {@link TicketService#countOpenByType()} for the total, exposes the per-type
     * breakdown for at-a-glance triage. Single DAO call; intended to fire on a
     * dashboard refetch interval.
     */
    @GET
    @Path("/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Open-ticket roll-up across the corpus",
            description = "Returns the total open ticket count and a per-{@link TicketType} breakdown. Cheap (single grouped count query); intended for the admin Systems Monitoring dashboard panel.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ResponseDataObject.class))),
                    @ApiResponse(responseCode = "401", description = "Not authenticated.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<OpenTicketSummaryResponse> getOpenTicketSummary() {
        java.util.Map<TicketType, Long> byType = ticketService.countOpenByType();
        long total = 0;
        for ( Long v : byType.values() ) {
            if ( v != null ) total += v;
        }
        OpenTicketSummaryResponse body = new OpenTicketSummaryResponse();
        body.totalOpen = total;
        body.byType = new java.util.EnumMap<>( TicketType.class );
        // Ensure every enum value appears in the map even if count is zero — UI table
        // doesn't have to handle "missing key" vs "zero" specially.
        for ( TicketType t : TicketType.values() ) {
            body.byType.put( t, byType.getOrDefault( t, 0L ) );
        }
        return respond( body );
    }

    public static class OpenTicketSummaryResponse {
        public long totalOpen;
        public java.util.Map<TicketType, Long> byType;
    }

    /**
     * Retrieve a single ticket, including its full event log.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve a single ticket by id, with full event log")
    public ResponseDataObject<TicketValueObject> getTicket(
            @PathParam("id") Long id
    ) {
        // Use the service-side VO projection so the lazy collections (reporter, events,
        // each event's actor) initialize INSIDE the service's @Transactional rather than
        // raising LazyInitializationException once the session closes. See
        // TicketServiceImpl.loadValueObject + the regression IT.
        TicketValueObject vo = ticketService.loadValueObject( id, true );
        if ( vo == null ) {
            throw new NotFoundException( "No ticket with id " + id );
        }
        return respond( vo );
    }

    /**
     * Retrieve only the event log for a ticket. Intended for client-side
     * polling — cheaper than re-fetching the whole ticket.
     * <p>
     * Step 1r of {@code CURSOR_PAGINATION_STEP1_PLAN.md} adds opt-in
     * keyset (cursor) pagination alongside the legacy unpaginated path.
     * Legacy mode (no {@code cursor}) returns the full event list as a
     * {@link ResponseDataObject} (occurredAt-asc ordering); cursor mode
     * pages by ascending {@code id} (the cursor DAO restricts cursors to
     * single-component id sorts until the phase-B indexed-column audit
     * lands &mdash; events on a ticket are appended monotonically so
     * id-asc tracks occurredAt-asc in practice).
     */
    @GET
    @Path("/{id}/events")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve only the event log for a ticket",
            description = "Legacy mode (no `cursor` parameter): returns the full unpaginated event "
                    + "list in the existing shape (no count query, full result set, sorted by `occurredAt`). "
                    + "Cursor mode (recommended for tickets accumulating long workflow histories): "
                    + "pass an opaque `cursor` token from a previous response's `nextCursor` / `prevCursor` "
                    + "field along with a `limit`. In cursor mode the result is always sorted by ascending `id` "
                    + "(cursor mode forces a single-component id sort pending the indexed-column audit in phase B; "
                    + "ticket events are append-only so id-asc tracks occurredAt-asc in practice); the path-derived "
                    + "ticket scope is preserved; `totalElements` is `null` by default (no count query per request).",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(oneOf = {
                                    ResponseDataObject.class,
                                    CursorPaginatedResponseDataObject.class
                            }))),
                    @ApiResponse(responseCode = "404", description = "The ticket does not exist.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Object getTicketEvents(
            @PathParam("id") Long id,
            @Parameter(description = "Opaque keyset-pagination cursor token.")
            @QueryParam("cursor") CursorArg cursorArg,
            @Parameter(description = "Page size for cursor mode (ignored when no `cursor` is supplied).")
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        Ticket t = ticketService.load( id );
        if ( t == null ) {
            throw new NotFoundException( "No ticket with id " + id );
        }
        if ( cursorArg != null ) {
            CursorPage<TicketEventValueObject> page = ticketService
                    .findEventsByCursor( t, cursorArg.getValue(), limitArg.getValue() )
                    .map( TicketEventValueObject::from );
            return paginateByCursor( page, new String[] { "id" } );
        }
        TicketValueObject vo = TicketValueObject.from( t, true );
        return respond( vo.getEvents() );
    }

    /**
     * Open a new ticket. The current authenticated user is recorded as the
     * reporter. Per Decision 3 of {@code AUDIT_AS_WORKFLOW_RECCE.md}, any
     * authenticated principal may create a ticket; anonymous callers are
     * denied at the {@link PreAuthorize} layer.
     *
     * @return 201 Created with the new {@link TicketValueObject} (event log
     * included so the caller can see the seeded OPENED event without a
     * follow-up GET).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Open a new curation ticket",
            description = "Creates a new ticket; the authenticated user is recorded as the reporter. "
                    + "The response includes the seeded OPENED event.")
    public Response createTicket( CreateTicketRequest req ) {
        if ( req == null ) {
            throw new BadRequestException( "Request body is required." );
        }
        if ( req.getType() == null ) {
            throw new BadRequestException( "type is required." );
        }
        if ( req.getTitle() == null || req.getTitle().trim().isEmpty() ) {
            throw new BadRequestException( "title is required." );
        }
        if ( req.getTargets() == null || req.getTargets().isEmpty() ) {
            throw new BadRequestException( "At least one target is required." );
        }
        User reporter = userManager.getCurrentUser();
        if ( reporter == null ) {
            // Defensive — @PreAuthorize already excludes anonymous, but the
            // user-manager contract permits null on edge cases.
            throw new BadRequestException( "No authenticated user resolved." );
        }
        Set<TicketTarget> targets = new HashSet<>();
        for ( TicketTargetRequest tr : req.getTargets() ) {
            if ( tr.getTargetType() == null || tr.getTargetId() == null ) {
                throw new BadRequestException( "Each target requires targetType and targetId." );
            }
            TicketTarget tt = TicketTarget.Factory.newInstance( tr.getTargetType(), tr.getTargetId() );
            if ( tr.getStatus() != null ) {
                tt.setStatus( tr.getStatus() );
            }
            targets.add( tt );
        }
        Ticket created = ticketService.openTicket( reporter, req.getType(), req.getTitle(), targets );

        // Optional follow-up mutations seeded from the create payload.
        List<String> changedFields = new ArrayList<>();
        if ( req.getPriority() != null ) {
            created.setPriority( req.getPriority() );
            changedFields.add( "priority" );
        }
        if ( req.getDueDate() != null ) {
            created.setDueDate( req.getDueDate() );
            changedFields.add( "dueDate" );
        }
        if ( req.getBody() != null ) {
            created.setBody( req.getBody() );
            changedFields.add( "body" );
        }
        if ( req.getMode() != null ) {
            created.setMode( req.getMode() );
            changedFields.add( "mode" );
        }
        if ( !changedFields.isEmpty() ) {
            // No TicketEvent for metadata edits, but the governance audit
            // trail picks up the change via @Audited(TicketMetadataChangedEvent).
            ticketService.updateMetadata( created, String.join( ", ", changedFields ) );
        }
        if ( req.getAssigneeId() != null ) {
            User assignee = userReadService.load( req.getAssigneeId() );
            if ( assignee == null ) {
                throw new BadRequestException( "No user with id " + req.getAssigneeId() );
            }
            created = ticketService.assign( created, reporter, assignee );
        }
        return Response.status( Response.Status.CREATED )
                .entity( new ResponseDataObject<>( TicketValueObject.from( created, true ) ) )
                .build();
    }

    /**
     * Update mutable fields of a ticket. Any combination of the following
     * may be supplied in the body:
     * <ul>
     *   <li>{@code state} — transition; appends a STATE_CHANGED / RESOLVED /
     *       CANCELLED / REOPENED event.</li>
     *   <li>{@code assigneeId} — assign or re-assign (use a {@code null}
     *       JSON value to clear); appends an ASSIGNED event.</li>
     *   <li>{@code comment} — append a COMMENTED event with the supplied
     *       free-form body.</li>
     *   <li>{@code priority}, {@code dueDate}, {@code title}, {@code body},
     *       {@code mode} — metadata; no TicketEvent log entry, but a
     *       {@code TicketMetadataChangedEvent} is appended to the governance
     *       audit trail with the list of changed fields in NOTE.</li>
     * </ul>
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a ticket (state, assignee, comment, metadata)",
            description = "Partial update: any subset of state / assigneeId / comment / priority / dueDate "
                    + "may be supplied. Each populated field triggers the corresponding service call and event.")
    public ResponseDataObject<TicketValueObject> updateTicket(
            @PathParam("id") Long id,
            UpdateTicketRequest req
    ) {
        if ( req == null ) {
            throw new BadRequestException( "Request body is required." );
        }
        Ticket ticket = ticketService.load( id );
        if ( ticket == null ) {
            throw new NotFoundException( "No ticket with id " + id );
        }
        User actor = userManager.getCurrentUser();
        if ( actor == null ) {
            throw new BadRequestException( "No authenticated user resolved." );
        }

        // Metadata first (priority, due date, title, body, mode) — no
        // TicketEvent log spam, but TicketMetadataChangedEvent on the
        // governance audit trail picks up the diff via updateMetadata().
        List<String> changedFields = new ArrayList<>();
        if ( req.getPriority() != null ) {
            ticket.setPriority( req.getPriority() );
            changedFields.add( "priority" );
        }
        if ( req.isDueDateSet() ) {
            ticket.setDueDate( req.getDueDate() );
            changedFields.add( "dueDate" );
        }
        if ( req.getTitle() != null ) {
            ticket.setTitle( req.getTitle() );
            changedFields.add( "title" );
        }
        if ( req.isBodySet() ) {
            ticket.setBody( req.getBody() );
            changedFields.add( "body" );
        }
        if ( req.getMode() != null ) {
            ticket.setMode( req.getMode() );
            changedFields.add( "mode" );
        }
        if ( !changedFields.isEmpty() ) {
            ticket = ticketService.updateMetadata( ticket, String.join( ", ", changedFields ) );
        }

        // Assignee
        if ( req.isAssigneeIdSet() ) {
            User assignee = null;
            if ( req.getAssigneeId() != null ) {
                assignee = userReadService.load( req.getAssigneeId() );
                if ( assignee == null ) {
                    throw new BadRequestException( "No user with id " + req.getAssigneeId() );
                }
            }
            ticket = ticketService.assign( ticket, actor, assignee );
        }

        // Comment
        if ( req.getComment() != null && !req.getComment().isEmpty() ) {
            ticket = ticketService.addComment( ticket, actor, req.getComment() );
        }

        // State transition — last, so any companion edits land first.
        if ( req.getState() != null ) {
            ticket = ticketService.transition( ticket, req.getState(), actor, req.getReason() );
        }

        // Project the response through the service so reporter + events lazy-load inside
        // the service's @Transactional. Building from the handler-side `ticket` reference
        // would raise LazyInitializationException — same bug class as the GET path
        // (see TicketServiceImpl.loadValueObject + DetachedEntityRegression tests).
        TicketValueObject vo = ticketService.loadValueObject( id, true );
        if ( vo == null ) {
            // Race: ticket deleted between the mutation and the read. Surface as 404.
            throw new NotFoundException( "Ticket " + id + " disappeared after update." );
        }
        return respond( vo );
    }

    /**
     * PATCH alias for {@link #updateTicket(Long, UpdateTicketRequest)}. Same semantics —
     * Gemma's PUT has always behaved as a partial update (only fields explicitly set in
     * the request body are touched), which is exactly what PATCH expresses semantically.
     * The alias exists so callers (notably gemma-curation-ui) can use the verb that
     * matches their intent without forcing every existing PUT consumer to switch.
     */
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a ticket (PATCH alias for PUT)",
            description = "Partial update; same fields and semantics as PUT /tickets/{id}. "
                    + "Provided so PATCH-leaning callers can use the verb that matches their intent.")
    public ResponseDataObject<TicketValueObject> patchTicket(
            @PathParam("id") Long id,
            UpdateTicketRequest req
    ) {
        return updateTicket( id, req );
    }

    /**
     * Update the status of a single {@link TicketTarget} on a multi-target
     * ticket — the canonical agent-workflow move. The agent picks up an
     * open ticket, marks each target {@link TicketTargetStatus#UNDERWAY}
     * when it starts work on it, then {@link TicketTargetStatus#DONE} when
     * it finishes. Appends a
     * {@link ubic.gemma.model.common.auditAndSecurity.curation.TicketEventType#TARGET_STATUS_CHANGED} event and a
     * {@code TicketTargetStatusChangedEvent} audit-trail row in lockstep.
     * No-op when the target is already at {@code status}.
     *
     * <p>The {@code targetRowId} path param is the {@code TicketTarget}
     * primary key (the row id), NOT the {@code targetId} field (which is
     * the FK to the targeted entity like an EE).</p>
     */
    @PATCH
    @Path("/{id}/targets/{targetRowId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update one target's status on a multi-target ticket",
            description = "Sets status on the specified TicketTarget row. Idempotent on no-op; "
                    + "writes a TARGET_STATUS_CHANGED event + audit row when status actually changes.")
    public ResponseDataObject<TicketValueObject> updateTargetStatus(
            @PathParam("id") Long id,
            @PathParam("targetRowId") Long targetRowId,
            UpdateTargetStatusRequest req
    ) {
        if ( req == null || ( req.getStatus() == null && !req.hasScreeningResult() ) ) {
            throw new BadRequestException( "Request body with `status` and/or `screeningResult` is required." );
        }
        Ticket ticket = ticketService.load( id );
        if ( ticket == null ) {
            throw new NotFoundException( "No ticket with id " + id );
        }
        User actor = userManager.getCurrentUser();
        if ( actor == null ) {
            throw new BadRequestException( "No authenticated user resolved." );
        }
        try {
            if ( req.getStatus() != null ) {
                ticketService.updateTargetStatus( ticket, targetRowId, req.getStatus(), actor );
            }
            if ( req.hasScreeningResult() ) {
                ticketService.updateTargetScreeningResult( ticket, targetRowId, req.getScreeningResult(), req.getScreeningResultReason(), actor );
            }
        } catch ( IllegalArgumentException e ) {
            // the update methods throw IAE when the targetRowId isn't on this ticket.
            throw new NotFoundException( e.getMessage() );
        }
        TicketValueObject vo = ticketService.loadValueObject( id, true );
        if ( vo == null ) {
            throw new NotFoundException( "Ticket " + id + " disappeared after target update." );
        }
        return respond( vo );
    }

    /**
     * Soft-close a ticket: transition to {@link TicketState#CANCELLED} and
     * append a CANCELLED event. The row itself is NOT hard-deleted — the
     * ticket and its event log remain queryable for audit (Decision 4 of
     * the recce). Calling DELETE on an already-terminal ticket is a no-op
     * but still returns 204.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel (soft-close) a ticket",
            description = "Transitions the ticket to CANCELLED. Append-only: the row and event log are preserved.")
    public Response deleteTicket(
            @PathParam("id") Long id,
            @Parameter(description = "Optional human-readable reason; recorded on the CANCELLED event.")
            @QueryParam("reason") @Nullable String reason
    ) {
        Ticket ticket = ticketService.load( id );
        if ( ticket == null ) {
            throw new NotFoundException( "No ticket with id " + id );
        }
        User actor = userManager.getCurrentUser();
        if ( actor == null ) {
            throw new BadRequestException( "No authenticated user resolved." );
        }
        if ( ticket.getState() != TicketState.CANCELLED ) {
            ticketService.transition( ticket, TicketState.CANCELLED, actor, reason );
        }
        return Response.noContent().build();
    }

    private List<TicketValueObject> loadOpenTargetingVOs( TicketTargetType type, Long targetId ) {
        List<Ticket> tickets = ticketService.findOpenForTarget( type, targetId );
        List<TicketValueObject> out = new ArrayList<>( tickets.size() );
        for ( Ticket t : tickets ) {
            out.add( TicketValueObject.from( t ) );
        }
        return out;
    }

    /**
     * Public hook for {@link DatasetsWebService} so the dataset-tickets route
     * can delegate here without duplicating the Ticket→VO mapping logic.
     */
    public List<TicketValueObject> openTicketsForExpressionExperiment( Long eeId ) {
        return loadOpenTargetingVOs( TicketTargetType.EXPRESSION_EXPERIMENT, eeId );
    }

    /**
     * Public hook for {@link PlatformsWebService} so the platform-tickets
     * route can delegate here without duplicating the Ticket→VO mapping
     * logic.
     */
    public List<TicketValueObject> openTicketsForArrayDesign( Long adId ) {
        return loadOpenTargetingVOs( TicketTargetType.ARRAY_DESIGN, adId );
    }

    /**
     * Cursor-mode counterpart to {@link #openTicketsForExpressionExperiment(Long)}
     * (step 1p of {@code CURSOR_PAGINATION_STEP1_PLAN.md}). Returns a
     * {@link CursorPage} of {@link TicketValueObject} so the
     * {@link DatasetsWebService} can forward it directly through
     * {@link ubic.gemma.rest.util.Responders#paginateByCursor}.
     */
    public CursorPage<TicketValueObject> openTicketsForExpressionExperimentByCursor( Long eeId,
            @org.springframework.lang.Nullable ubic.gemma.persistence.util.Cursor cursor, int limit ) {
        CursorPage<Ticket> page = ticketService.findOpenForTargetByCursor(
                TicketTargetType.EXPRESSION_EXPERIMENT, eeId, cursor, limit );
        return page.map( TicketValueObject::from );
    }

    /**
     * Cursor-mode counterpart to {@link #openTicketsForArrayDesign(Long)}
     * (step 1s of {@code CURSOR_PAGINATION_STEP1_PLAN.md}). Returns a
     * {@link CursorPage} of {@link TicketValueObject} so the
     * {@link PlatformsWebService} can forward it directly through
     * {@link ubic.gemma.rest.util.Responders#paginateByCursor}. Mirrors
     * {@link #openTicketsForExpressionExperimentByCursor(Long, ubic.gemma.persistence.util.Cursor, int)}
     * but bound to {@link TicketTargetType#ARRAY_DESIGN}.
     */
    public CursorPage<TicketValueObject> openTicketsForArrayDesignByCursor( Long adId,
            @org.springframework.lang.Nullable ubic.gemma.persistence.util.Cursor cursor, int limit ) {
        CursorPage<Ticket> page = ticketService.findOpenForTargetByCursor(
                TicketTargetType.ARRAY_DESIGN, adId, cursor, limit );
        return page.map( TicketValueObject::from );
    }

    /* ====== Request DTOs (kept inner-class for proximity to the endpoints) ====== */

    /**
     * Body for {@link #createTicket(CreateTicketRequest)}. All fields except
     * {@code type}, {@code title}, and {@code targets} are optional.
     */
    public static class CreateTicketRequest {
        private TicketType type;
        private String title;
        private List<TicketTargetRequest> targets;
        @Nullable
        private TicketPriority priority;
        @Nullable
        private Date dueDate;
        @Nullable
        private Long assigneeId;
        /** Curator-facing instructions; optional, defaults to empty body. */
        @Nullable
        private String body;
        /** Advance mode; optional, defaults to {@link TicketMode#MANUAL}. */
        @Nullable
        private TicketMode mode;

        public TicketType getType() { return type; }
        public void setType( TicketType type ) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle( String title ) { this.title = title; }

        public List<TicketTargetRequest> getTargets() { return targets; }
        public void setTargets( List<TicketTargetRequest> targets ) { this.targets = targets; }

        @Nullable
        public TicketPriority getPriority() { return priority; }
        public void setPriority( @Nullable TicketPriority priority ) { this.priority = priority; }

        @Nullable
        public Date getDueDate() { return dueDate; }
        public void setDueDate( @Nullable Date dueDate ) { this.dueDate = dueDate; }

        @Nullable
        public Long getAssigneeId() { return assigneeId; }
        public void setAssigneeId( @Nullable Long assigneeId ) { this.assigneeId = assigneeId; }

        @Nullable
        public String getBody() { return body; }
        public void setBody( @Nullable String body ) { this.body = body; }

        @Nullable
        public TicketMode getMode() { return mode; }
        public void setMode( @Nullable TicketMode mode ) { this.mode = mode; }
    }

    public static class TicketTargetRequest {
        private TicketTargetType targetType;
        private Long targetId;
        /** Initial status for the target; optional, defaults to {@link TicketTargetStatus#NOT_DONE}. */
        @Nullable
        private TicketTargetStatus status;

        public TicketTargetType getTargetType() { return targetType; }
        public void setTargetType( TicketTargetType targetType ) { this.targetType = targetType; }

        public Long getTargetId() { return targetId; }
        public void setTargetId( Long targetId ) { this.targetId = targetId; }

        @Nullable
        public TicketTargetStatus getStatus() { return status; }
        public void setStatus( @Nullable TicketTargetStatus status ) { this.status = status; }
    }

    /**
     * Body for {@link #updateTicket(Long, UpdateTicketRequest)}. Distinguishes
     * "field absent" (no change) from "field present and null" (clear) for
     * {@code assigneeId} and {@code dueDate} via setter-tracking flags.
     */
    public static class UpdateTicketRequest {
        @Nullable
        private TicketState state;
        @Nullable
        private String reason;
        @Nullable
        private String comment;
        @Nullable
        private TicketPriority priority;

        @Nullable
        private Long assigneeId;
        private boolean assigneeIdSet = false;

        @Nullable
        private Date dueDate;
        private boolean dueDateSet = false;

        @Nullable
        private String title;
        @Nullable
        private String body;
        private boolean bodySet = false;
        @Nullable
        private TicketMode mode;

        @Nullable
        public TicketState getState() { return state; }
        public void setState( @Nullable TicketState state ) { this.state = state; }

        @Nullable
        public String getReason() { return reason; }
        public void setReason( @Nullable String reason ) { this.reason = reason; }

        @Nullable
        public String getComment() { return comment; }
        public void setComment( @Nullable String comment ) { this.comment = comment; }

        @Nullable
        public TicketPriority getPriority() { return priority; }
        public void setPriority( @Nullable TicketPriority priority ) { this.priority = priority; }

        @Nullable
        public Long getAssigneeId() { return assigneeId; }
        public void setAssigneeId( @Nullable Long assigneeId ) {
            this.assigneeId = assigneeId;
            this.assigneeIdSet = true;
        }
        public boolean isAssigneeIdSet() { return assigneeIdSet; }

        @Nullable
        public Date getDueDate() { return dueDate; }
        public void setDueDate( @Nullable Date dueDate ) {
            this.dueDate = dueDate;
            this.dueDateSet = true;
        }
        public boolean isDueDateSet() { return dueDateSet; }

        @Nullable
        public String getTitle() { return title; }
        public void setTitle( @Nullable String title ) { this.title = title; }

        @Nullable
        public String getBody() { return body; }
        public void setBody( @Nullable String body ) {
            this.body = body;
            this.bodySet = true;
        }
        public boolean isBodySet() { return bodySet; }

        @Nullable
        public TicketMode getMode() { return mode; }
        public void setMode( @Nullable TicketMode mode ) { this.mode = mode; }
    }

    /**
     * Body for {@link #updateTargetStatus(Long, Long, UpdateTargetStatusRequest)}.
     * Single field, but kept as a DTO so the JSON shape can grow later
     * (e.g. a per-target note) without a v2 endpoint.
     */
    public static class UpdateTargetStatusRequest {
        @Schema(description = "Desired status for this target. Omit to leave unchanged.", example = "DONE")
        private TicketTargetStatus status;

        @Schema(description = "Screening decision for this target (INCLUDE / REJECT / UNDECIDED). "
                + "Omit to leave unchanged; send null to clear.", example = "INCLUDE")
        private ubic.gemma.model.common.auditAndSecurity.curation.ScreeningResult screeningResult;
        private boolean screeningResultSet = false;

        public TicketTargetStatus getStatus() { return status; }
        public void setStatus( TicketTargetStatus status ) { this.status = status; }

        public ubic.gemma.model.common.auditAndSecurity.curation.ScreeningResult getScreeningResult() { return screeningResult; }

        public void setScreeningResult( ubic.gemma.model.common.auditAndSecurity.curation.ScreeningResult screeningResult ) {
            this.screeningResult = screeningResult;
            this.screeningResultSet = true;
        }

        /** True once the JSON carried a {@code screeningResult} key, even if its value was null. */
        public boolean hasScreeningResult() { return screeningResultSet; }

        @Schema(description = "Free-text reason for the screening decision; rides with `screeningResult`. "
                + "Omit or null to clear.", example = "Superseded by GSE99999")
        private String screeningResultReason;

        public String getScreeningResultReason() { return screeningResultReason; }
        public void setScreeningResultReason( String screeningResultReason ) { this.screeningResultReason = screeningResultReason; }
    }
}
