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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
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
    private final UserService userService;

    @Autowired
    public TicketsWebService( TicketService ticketService, UserManager userManager, UserService userService ) {
        this.ticketService = ticketService;
        this.userManager = userManager;
        this.userService = userService;
    }

    /**
     * List tickets with optional filters and offset/limit pagination.
     * <p>
     * Cursor pagination is a separate roadmap item
     * ({@code CURSOR_PAGINATION_RECCE.md}); for now we use the same
     * offset/limit shape as the rest of the v2 surface.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List curation tickets",
            description = "Filterable, paginated list of curation tickets. "
                    + "Event logs are NOT included for payload economy; fetch /{id} for events.")
    public PaginatedResponseDataObject<TicketValueObject> getTickets(
            @Parameter(description = "If true, restrict to OPEN/IN_PROGRESS tickets.")
            @QueryParam("openOnly") @DefaultValue("false") boolean openOnly,
            @Parameter(description = "Filter by current assignee (Contact id).")
            @QueryParam("assignee") @Nullable Long assigneeId,
            @Parameter(description = "Filter by priority.")
            @QueryParam("priority") @Nullable TicketPriority priority,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        List<Ticket> tickets = ticketService.findTickets( openOnly, assigneeId, priority, offset, limit );
        long total = ticketService.countTickets( openOnly, assigneeId, priority );
        List<TicketValueObject> vos = tickets.stream()
                .map( TicketValueObject::from )
                .collect( Collectors.toList() );
        Slice<TicketValueObject> slice = new Slice<>( vos, null, offset, limit, total );
        return paginate( slice, new String[] { "id" } );
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
        Ticket t = ticketService.load( id );
        if ( t == null ) {
            throw new NotFoundException( "No ticket with id " + id );
        }
        return respond( TicketValueObject.from( t, true ) );
    }

    /**
     * Retrieve only the event log for a ticket. Intended for client-side
     * polling — cheaper than re-fetching the whole ticket.
     */
    @GET
    @Path("/{id}/events")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve only the event log for a ticket")
    public ResponseDataObject<List<TicketEventValueObject>> getTicketEvents(
            @PathParam("id") Long id
    ) {
        Ticket t = ticketService.load( id );
        if ( t == null ) {
            throw new NotFoundException( "No ticket with id " + id );
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
            targets.add( TicketTarget.Factory.newInstance( tr.getTargetType(), tr.getTargetId() ) );
        }
        Ticket created = ticketService.openTicket( reporter, req.getType(), req.getTitle(), targets );

        // Optional follow-up mutations seeded from the create payload.
        if ( req.getPriority() != null ) {
            created.setPriority( req.getPriority() );
        }
        if ( req.getDueDate() != null ) {
            created.setDueDate( req.getDueDate() );
        }
        if ( req.getPriority() != null || req.getDueDate() != null ) {
            // Persist non-state edits without spamming the event log.
            created.setUpdatedAt( new Date() );
            ticketService.update( created );
        }
        if ( req.getAssigneeId() != null ) {
            User assignee = userService.load( req.getAssigneeId() );
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
     *   <li>{@code priority}, {@code dueDate} — metadata; no event log
     *       entry (audit trail still captures the row mutation).</li>
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

        // Metadata first (priority, due date) — no events, but updatedAt bumps via update().
        boolean metadataChanged = false;
        if ( req.getPriority() != null ) {
            ticket.setPriority( req.getPriority() );
            metadataChanged = true;
        }
        if ( req.isDueDateSet() ) {
            ticket.setDueDate( req.getDueDate() );
            metadataChanged = true;
        }
        if ( metadataChanged ) {
            ticket.setUpdatedAt( new Date() );
            ticketService.update( ticket );
        }

        // Assignee
        if ( req.isAssigneeIdSet() ) {
            User assignee = null;
            if ( req.getAssigneeId() != null ) {
                assignee = userService.load( req.getAssigneeId() );
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

        return respond( TicketValueObject.from( ticket, true ) );
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
    }

    public static class TicketTargetRequest {
        private TicketTargetType targetType;
        private Long targetId;

        public TicketTargetType getTargetType() { return targetType; }
        public void setTargetType( TicketTargetType targetType ) { this.targetType = targetType; }

        public Long getTargetId() { return targetId; }
        public void setTargetId( Long targetId ) { this.targetId = targetId; }
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
    }
}
