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
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketPriority;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for curation tickets (Phase B-2 of
 * {@code AUDIT_AS_WORKFLOW_RECCE.md}). Read-only surface: GET only. Mutating
 * endpoints (POST/PUT/DELETE) land in a follow-up phase.
 *
 * @author paul
 */
@Service
@Path("/tickets")
@Tag(name = "Tickets", description = "Curation workflow tickets")
public class TicketsWebService {

    private final TicketService ticketService;

    @Autowired
    public TicketsWebService( TicketService ticketService ) {
        this.ticketService = ticketService;
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
}
