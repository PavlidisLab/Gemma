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
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.expression.experiment.DisallowedWorkflowTransitionException;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.WorkflowQueueEntry;
import ubic.gemma.persistence.service.expression.experiment.WorkflowService;
import ubic.gemma.persistence.service.expression.experiment.WorkflowTransition;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for the 8-state workflow lifecycle
 * (HANDOFF_WORKFLOW_STATE_STORAGE.md).
 *
 * <p>Three endpoints:</p>
 * <ul>
 *   <li>{@code GET  /datasets/{id}/workflow} — current state + history.</li>
 *   <li>{@code PUT  /datasets/{id}/workflow} — advance the state.</li>
 *   <li>{@code GET  /workflow/queue?state=...} — curator worklist.</li>
 * </ul>
 *
 * <p>The endpoints are deliberately partitioned across two path prefixes
 * ({@code /datasets/{id}/workflow} and {@code /workflow/queue}); JAX-RS
 * dispatch is by full path, so a single resource class can serve both.
 * Per the handoff the queue endpoint is also a forward-compat hook for
 * {@code PreboardingExperiment} -- {@link WorkflowService} is what gates
 * dataset_type today; the REST surface is type-agnostic.</p>
 *
 * @author paul
 */
@Service
@Path("/")
@Tag(name = "Workflow", description = "8-state experiment lifecycle")
public class WorkflowWebService {

    private final WorkflowService workflowService;
    private final ExpressionExperimentService expressionExperimentService;

    @Autowired
    public WorkflowWebService( WorkflowService workflowService,
            ExpressionExperimentService expressionExperimentService ) {
        this.workflowService = workflowService;
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Retrieve the current workflow state and full transition history of a
     * dataset.
     *
     * <p>The handoff is explicit that "history" is derived from the
     * AUDIT_EVENT stream filtered to {@code WorkflowStateChangedEvent} --
     * no separate history table.</p>
     */
    @GET
    @Path("/datasets/{id}/workflow")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve a dataset's current workflow state + history",
            description = "Current workflow position and the full chronological history of "
                    + "transitions (oldest first). History is derived from AUDIT_EVENT filtered "
                    + "to WorkflowStateChangedEvent rows.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true,
                            content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content())
            })
    public ResponseDataObject<WorkflowStateResponse> getDatasetWorkflow(
            @PathParam("id") Long datasetId
    ) {
        ExpressionExperiment ee = loadDatasetOrThrow( datasetId );
        WorkflowState current = workflowService.getCurrentState( ee );
        List<AuditEvent> events = workflowService.getHistory( ee );
        List<WorkflowHistoryEntry> history = new ArrayList<>( events.size() );
        for ( AuditEvent ev : events ) {
            WorkflowHistoryEntry h = new WorkflowHistoryEntry();
            h.enteredAt = ev.getDate();
            User actor = ev.getPerformer();
            h.actor = actor != null ? actor.getUserName() : null;
            h.note = ev.getNote();
            history.add( h );
        }
        WorkflowStateResponse body = new WorkflowStateResponse();
        body.datasetId = datasetId;
        body.datasetType = "expression_experiment";
        body.currentState = current.name();
        body.enteredCurrentStateAt = ee.getWorkflowStateEnteredAt();
        body.history = history;
        return respond( body );
    }

    /**
     * Advance a dataset to a new workflow state. PUT body must include
     * {@code target_state}; {@code reason} and {@code ticket_id} are
     * optional.
     *
     * <p>Per the handoff Open Question 1 ("per-transition role granularity")
     * recommendation, the first cut is permissive: any curator or admin
     * may advance any allowed transition. The {@code Public -&gt; Curate}
     * transition additionally requires admin role + a non-empty reason
     * (Open Question 5 recommendation).</p>
     */
    @PUT
    @Path("/datasets/{id}/workflow")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Advance a dataset to a new workflow state",
            description = "Body: `{\"target_state\": \"Audit\", \"reason\": \"...\", \"ticket_id\": 9001?}`. "
                    + "Idempotent: PUTting current_state is a 200 no-op with previous_state == current_state. "
                    + "Disallowed transitions return 409 with the list of allowed next states. "
                    + "Unknown target_state returns 400. Public -> Curate additionally requires admin role + non-empty reason.",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true,
                            content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Missing or unknown target_state.",
                            content = @Content()),
                    @ApiResponse(responseCode = "403", description = "Insufficient role for this transition.",
                            content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The dataset does not exist.",
                            content = @Content()),
                    @ApiResponse(responseCode = "409", description = "Disallowed transition; body lists allowed next states.",
                            content = @Content())
            })
    public Response advanceDatasetWorkflow(
            @PathParam("id") Long datasetId,
            @Nullable AdvanceWorkflowRequest req
    ) {
        if ( req == null || req.targetState == null || req.targetState.isEmpty() ) {
            throw new BadRequestException( "Request body must include target_state." );
        }
        WorkflowState target;
        try {
            target = WorkflowState.valueOf( req.targetState );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown target_state: " + req.targetState );
        }
        ExpressionExperiment ee = loadDatasetOrThrow( datasetId );

        // Open Question 5: Public -> Curate requires admin + non-empty reason.
        WorkflowState current = workflowService.getCurrentState( ee );
        if ( current == WorkflowState.Public && target == WorkflowState.Curate ) {
            if ( req.reason == null || req.reason.trim().isEmpty() ) {
                throw new BadRequestException(
                        "Public -> Curate requires a non-empty `reason`." );
            }
            // Note: the @PreAuthorize gate is broader than admin (curator OK
            // for most transitions). For Public -> Curate we additionally
            // require admin; the SecurityContext is consulted via the
            // method-level @PreAuthorize once we add a second method, or
            // we inline-check here. Defensive inline check:
            if ( !isAdminInRequest() ) {
                throw new ClientErrorException(
                        "Public -> Curate requires admin role.", Response.Status.FORBIDDEN );
            }
        }

        WorkflowTransition transition;
        try {
            transition = workflowService.advance( ee, target, req.reason, req.ticketId );
        } catch ( DisallowedWorkflowTransitionException ex ) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put( "error", "Disallowed transition" );
            body.put( "current_state", ex.getCurrentState().name() );
            body.put( "target_state", ex.getTargetState().name() );
            List<String> allowed = new ArrayList<>();
            for ( WorkflowState s : ex.getAllowedNextStates() ) {
                allowed.add( s.name() );
            }
            body.put( "allowed_next_states", allowed );
            return Response.status( Response.Status.CONFLICT )
                    .entity( body )
                    .type( MediaType.APPLICATION_JSON )
                    .build();
        }

        WorkflowTransitionResponse resp = new WorkflowTransitionResponse();
        resp.datasetId = transition.getDatasetId();
        resp.previousState = transition.getPreviousState().name();
        resp.currentState = transition.getCurrentState().name();
        resp.enteredCurrentStateAt = transition.getEnteredCurrentStateAt();
        resp.auditEventId = transition.getAuditEventId();
        return Response.ok( respond( resp ) ).build();
    }

    /**
     * Curator worklist: datasets currently in a given workflow state.
     */
    @GET
    @Path("/workflow/queue")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List datasets currently in a given workflow state",
            description = "The curator triage view. Oldest entry first by workflowStateEnteredAt. "
                    + "Optional filters: dataset_type (currently only `expression_experiment` is "
                    + "implemented; `preboarding_experiment` returns empty pending the subclass), "
                    + "assignee (returns empty pending the Ticket-layer join; see TODO(ticket-integration)), "
                    + "since (ISO-8601; restrict to rows that entered the state on or after this).",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true,
                            content = @Content(schema = @Schema(implementation = PaginatedResponseDataObject.class))),
                    @ApiResponse(responseCode = "400", description = "Missing or unknown state.",
                            content = @Content())
            })
    public PaginatedResponseDataObject<WorkflowQueueEntryResponse> getWorkflowQueue(
            @Parameter(description = "Required; one of the 8 WorkflowState constants.")
            @QueryParam("state") @Nullable String stateName,
            @Parameter(description = "Optional; expression_experiment | preboarding_experiment.")
            @QueryParam("dataset_type") @Nullable String datasetType,
            @Parameter(description = "Optional; restrict to datasets with an OPEN ticket assigned to this user.")
            @QueryParam("assignee") @Nullable String assignee,
            @Parameter(description = "Optional ISO-8601 timestamp; restrict to entries on or after.")
            @QueryParam("since") @Nullable Date since,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        if ( stateName == null || stateName.isEmpty() ) {
            throw new BadRequestException( "Query parameter `state` is required." );
        }
        WorkflowState state;
        try {
            state = WorkflowState.valueOf( stateName );
        } catch ( IllegalArgumentException ex ) {
            throw new BadRequestException( "Unknown state: " + stateName );
        }
        Slice<WorkflowQueueEntry> slice = workflowService.queue( state, datasetType, assignee, since,
                offsetArg.getValue(), limitArg.getValue() );
        List<WorkflowQueueEntryResponse> mapped = new ArrayList<>( slice.size() );
        for ( WorkflowQueueEntry e : slice ) {
            WorkflowQueueEntryResponse r = new WorkflowQueueEntryResponse();
            r.datasetId = e.getDatasetId();
            r.datasetType = e.getDatasetType();
            r.accession = e.getAccession();
            r.enteredCurrentStateAt = e.getEnteredCurrentStateAt();
            r.currentAssignee = e.getCurrentAssignee();
            r.ticketCountOpen = e.getTicketCountOpen();
            mapped.add( r );
        }
        Slice<WorkflowQueueEntryResponse> mappedSlice = new Slice<>( mapped, null,
                offsetArg.getValue(), limitArg.getValue(),
                slice.getTotalElements() );
        return paginate( mappedSlice, new String[] { "datasetId" } );
    }

    private ExpressionExperiment loadDatasetOrThrow( Long id ) {
        if ( id == null ) {
            throw new BadRequestException( "Dataset id is required." );
        }
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new NotFoundException( "No dataset with id " + id );
        }
        return ee;
    }

    /**
     * Defensive admin-role check used by the Public -&gt; Curate guard.
     * Reads the {@link org.springframework.security.core.context.SecurityContextHolder}
     * authorities; returns true iff one of them is {@code GROUP_ADMIN}.
     */
    private boolean isAdminInRequest() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if ( auth == null ) return false;
        for ( org.springframework.security.core.GrantedAuthority a : auth.getAuthorities() ) {
            if ( "GROUP_ADMIN".equals( a.getAuthority() ) ) return true;
        }
        return false;
    }

    /* ====== DTOs ====== */

    /** Body of {@link #advanceDatasetWorkflow(Long, AdvanceWorkflowRequest)}. */
    public static class AdvanceWorkflowRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("target_state")
        private String targetState;
        @com.fasterxml.jackson.annotation.JsonProperty("reason")
        @Nullable
        private String reason;
        @com.fasterxml.jackson.annotation.JsonProperty("ticket_id")
        @Nullable
        private Long ticketId;

        public String getTargetState() { return targetState; }
        public void setTargetState( String targetState ) { this.targetState = targetState; }

        @Nullable
        public String getReason() { return reason; }
        public void setReason( @Nullable String reason ) { this.reason = reason; }

        @Nullable
        public Long getTicketId() { return ticketId; }
        public void setTicketId( @Nullable Long ticketId ) { this.ticketId = ticketId; }
    }

    /** Response of {@link #getDatasetWorkflow(Long)}. */
    public static class WorkflowStateResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("dataset_id")
        public Long datasetId;
        @com.fasterxml.jackson.annotation.JsonProperty("dataset_type")
        public String datasetType;
        @com.fasterxml.jackson.annotation.JsonProperty("current_state")
        public String currentState;
        @com.fasterxml.jackson.annotation.JsonProperty("entered_current_state_at")
        @Nullable
        public Date enteredCurrentStateAt;
        public List<WorkflowHistoryEntry> history = Collections.emptyList();
    }

    public static class WorkflowHistoryEntry {
        @com.fasterxml.jackson.annotation.JsonProperty("entered_at")
        @Nullable
        public Date enteredAt;
        @com.fasterxml.jackson.annotation.JsonProperty("actor")
        @Nullable
        public String actor;
        /**
         * The audit event NOTE, populated by the @AuditedConditional SpEL
         * expression on WorkflowServiceImpl.advance(). Carries the encoded
         * previous -&gt; target transition, optional reason, optional ticket.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("note")
        @Nullable
        public String note;
    }

    /** Response of {@link #advanceDatasetWorkflow(Long, AdvanceWorkflowRequest)}. */
    public static class WorkflowTransitionResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("dataset_id")
        public Long datasetId;
        @com.fasterxml.jackson.annotation.JsonProperty("previous_state")
        public String previousState;
        @com.fasterxml.jackson.annotation.JsonProperty("current_state")
        public String currentState;
        @com.fasterxml.jackson.annotation.JsonProperty("entered_current_state_at")
        @Nullable
        public Date enteredCurrentStateAt;
        @com.fasterxml.jackson.annotation.JsonProperty("audit_event_id")
        @Nullable
        public Long auditEventId;
    }

    /** Per-row response of {@link #getWorkflowQueue}. */
    public static class WorkflowQueueEntryResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("dataset_id")
        public Long datasetId;
        @com.fasterxml.jackson.annotation.JsonProperty("dataset_type")
        public String datasetType;
        @com.fasterxml.jackson.annotation.JsonProperty("accession")
        @Nullable
        public String accession;
        @com.fasterxml.jackson.annotation.JsonProperty("entered_current_state_at")
        @Nullable
        public Date enteredCurrentStateAt;
        @com.fasterxml.jackson.annotation.JsonProperty("current_assignee")
        @Nullable
        public String currentAssignee;
        @com.fasterxml.jackson.annotation.JsonProperty("ticket_count_open")
        public int ticketCountOpen;
    }

    /* Exposed for tests so they can assert without re-stringifying the enum. */
    static Set<WorkflowState> allowedNext( WorkflowState s ) {
        return s.allowedNextStates();
    }
}
