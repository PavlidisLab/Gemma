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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardingExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.PreboardingExperimentService;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST surface for the proposed-experiment workflow.
 *
 * <p>Six endpoints per {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md}:</p>
 * <ul>
 *   <li>{@code POST   /preboarding} — create a preboarding from an accession.
 *       409 with existing entity id+type if accession is already known
 *       (either as a preboarding or a loaded EE).</li>
 *   <li>{@code GET    /preboarding/{id}} — fetch preboarding + latest proposal.</li>
 *   <li>{@code GET    /preboarding?accession=X} — resolve accession to id.</li>
 *   <li>{@code POST   /preboarding/{id}/proposals} — append an agent proposal,
 *       idempotent on {@code runId}.</li>
 *   <li>{@code POST   /preboarding/{id}/promote} — promote to a loaded EE.
 *       Restricted to GROUP_CURATOR / GROUP_ADMIN; agents cannot promote.</li>
 *   <li>{@code GET    /preboarding?state=...} — list preboarding by workflow state
 *       (curator triage view).</li>
 * </ul>
 *
 * <p>Auth model (handoff §"Authorization"):</p>
 * <ul>
 *   <li>POST preboarding + POST proposals: GROUP_CURATOR / GROUP_ADMIN /
 *       GROUP_AGENT. Agents need to create preboarding and attach proposals.</li>
 *   <li>POST promote: GROUP_CURATOR / GROUP_ADMIN only. Agents MUST get 403.</li>
 *   <li>GET endpoints: any authenticated user (no @PreAuthorize on reads).</li>
 * </ul>
 *
 * <p>Fine-grained authorities (e.g. {@code preboarding:write}, {@code preboarding:promote})
 * are deferred per the spec recommendation; the coarse group-based gates match
 * the design-write and annotations-write conventions already in this codebase.</p>
 */
@Service
@Path("/")
@Tag(name = "Preboarding", description = "Proposed-but-not-loaded experiments + agent proposals")
public class PreboardingWebService {

    private final PreboardingExperimentService preboardingService;
    private final AgentProposalService agentProposalService;
    private final ExpressionExperimentService expressionExperimentService;

    @Autowired
    public PreboardingWebService( PreboardingExperimentService preboardingService,
            AgentProposalService agentProposalService,
            ExpressionExperimentService expressionExperimentService ) {
        this.preboardingService = preboardingService;
        this.agentProposalService = agentProposalService;
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Create a new {@link PreboardingExperiment} for the supplied accession.
     * Returns 409 with the existing id+type if the accession already exists
     * (as a preboarding OR an EE).
     */
    @POST
    @Path("/preboarding")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Create a preboarding for a previously-unknown accession",
            description = "Body: `{\"accession\":\"GSE12345\",\"source\":\"GEO\","
                    + "\"identifying_metadata\":{...}}`. Returns 409 with the existing entity's id "
                    + "and type when the accession is already known to Gemma.",
            responses = {
                    @ApiResponse(responseCode = "201", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Missing accession.",
                            content = @Content()),
                    @ApiResponse(responseCode = "409", description = "An entity with this accession already exists.",
                            content = @Content())
            })
    public Response createPreboarding( @Nullable CreatePreboardingRequest req ) {
        if ( req == null || req.accession == null || req.accession.trim().isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-blank `accession`." );
        }
        String accession = req.accession.trim();
        try {
            PreboardingExperiment skel = preboardingService.createPreboarding( accession,
                    req.source, req.identifyingMetadata );
            PreboardingResponse body = toPreboardingResponse( skel, null, 0L );
            return Response.status( Response.Status.CREATED ).entity( body ).build();
        } catch ( PreboardingExperimentService.AccessionAlreadyExistsException e ) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put( "error", "Accession already exists" );
            body.put( "accession", accession );
            body.put( "existing_id", e.getExistingId() );
            body.put( "existing_type", e.getExistingType() );
            return Response.status( Response.Status.CONFLICT ).entity( body ).build();
        }
    }

    /**
     * Fetch a preboarding by id + its latest proposal.
     */
    @GET
    @Path("/preboarding/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Fetch a preboarding + latest proposal",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No preboarding with that id.",
                            content = @Content())
            })
    public PreboardingResponse getPreboarding( @PathParam("id") Long id ) {
        PreboardingExperiment skel = loadPreboardingOrThrow( id );
        AgentProposal latest = agentProposalService.findLatestByInvestigation( skel );
        long total = agentProposalService.countByInvestigation( skel );
        return toPreboardingResponse( skel, latest, total );
    }

    /**
     * Listing endpoint: by accession (returns the single matching preboarding,
     * 404 if none) or by state (queue view, returns the most-recent
     * preboarding in that state — full pagination deferred).
     *
     * <p>The spec lists both {@code GET /preboarding?accession=...} and
     * {@code GET /preboarding?state=...} under the same path; this single
     * handler dispatches on the query parameter. Only one of the two filters
     * may be supplied per request.</p>
     */
    @GET
    @Path("/preboarding")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Resolve accession -> preboarding, or list preboarding in a state",
            description = "Exactly one of `accession` or `state` must be supplied.")
    public Response listOrResolvePreboarding(
            @Parameter(description = "Resolve accession -> preboarding id.")
            @QueryParam("accession") @Nullable String accession,
            @Parameter(description = "Filter by WorkflowState (e.g. Preboarding, Candidate, Discovery).")
            @QueryParam("state") @Nullable String stateName ) {
        if ( ( accession == null || accession.isEmpty() )
                && ( stateName == null || stateName.isEmpty() ) ) {
            throw new BadRequestException( "Exactly one of `accession` or `state` must be supplied." );
        }
        if ( accession != null && !accession.isEmpty() ) {
            PreboardingExperiment skel = preboardingService.findByAccession( accession );
            if ( skel == null ) {
                throw new NotFoundException( "No preboarding with accession " + accession );
            }
            AgentProposal latest = agentProposalService.findLatestByInvestigation( skel );
            long total = agentProposalService.countByInvestigation( skel );
            return Response.ok( toPreboardingResponse( skel, latest, total ) ).build();
        }
        // Queue-by-state path: defer the full implementation to the existing
        // WorkflowWebService /workflow/queue endpoint, which already lists by
        // WorkflowState and (per the workflow handoff §"polymorphism prep")
        // will return preboarding once dataset_type=preboarding_experiment
        // joins land. For now return a deterministic 501-with-pointer rather
        // than a quiet empty list.
        WorkflowState s;
        try {
            s = WorkflowState.valueOf( stateName );
        } catch ( IllegalArgumentException ex ) {
            throw new BadRequestException( "Unknown state: " + stateName );
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put( "error", "Queue-by-state listing is served by /workflow/queue" );
        body.put( "state", s.name() );
        body.put( "redirect_to",
                "/workflow/queue?state=" + s.name() + "&dataset_type=preboarding_experiment" );
        return Response.status( Response.Status.NOT_IMPLEMENTED ).entity( body ).build();
    }

    /**
     * Attach a new agent proposal to a preboarding. Idempotent on {@code run_id}:
     * if a proposal with the same {@code (preboarding_id, run_id)} already
     * exists, returns 200 OK with the existing row rather than 201 Created.
     */
    @POST
    @Path("/preboarding/{id}/proposals")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Attach an agent proposal payload to a preboarding",
            description = "Idempotent on `run_id`: a retry returns 200 with the existing proposal.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "New proposal attached.",
                            content = @Content()),
                    @ApiResponse(responseCode = "200", description = "Proposal with this run_id already attached.",
                            content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Missing run_id.",
                            content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Preboarding not found.",
                            content = @Content())
            })
    public Response attachProposal( @PathParam("id") Long id,
            @Nullable AttachProposalRequest req ) {
        if ( req == null || req.runId == null || req.runId.trim().isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-blank `run_id`." );
        }
        PreboardingExperiment skel = loadPreboardingOrThrow( id );
        AgentProposalService.AttachedProposal attached = agentProposalService.attach( skel,
                req.runId.trim(), req.agentVersion, req.model, req.ranAt, req.payloadJson );
        ProposalResponse body = toProposalResponse( attached.getProposal(), skel.getId() );
        Response.Status status = attached.isCreated()
                ? Response.Status.CREATED
                : Response.Status.OK;
        return Response.status( status ).entity( body ).build();
    }

    /**
     * Promote a preboarding to a loaded {@link ExpressionExperiment}.
     * <p>
     * Curator-only (agents get 403). Rebinds the preboarding's
     * {@code AgentProposal} rows to the EE and advances both rows' workflow
     * state. The {@code apply_latest_proposal} flag is accepted but ignored
     * for the first cut — the curator path applies the proposal interactively
     * through the existing design-write / annotation-write endpoints, and
     * deferring the server-side apply chain keeps this commit's scope minimal.
     * The flag is reflected back in the response as
     * {@code applied_proposal_id = null} so the caller knows the apply did
     * NOT happen.
     */
    @POST
    @Path("/preboarding/{id}/promote")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Promote a preboarding to a loaded ExpressionExperiment",
            description = "Curator-only. Rebinds AgentProposal rows from the preboarding to the EE; "
                    + "advances both rows' workflow state to Loaded (when not further along). "
                    + "`apply_latest_proposal` is accepted for forward-compatibility but currently "
                    + "a no-op; the curator applies the proposal via the design-write / "
                    + "annotation-write endpoints. See STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Promoted.", content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Missing ee_id.", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Preboarding or EE not found.",
                            content = @Content()),
                    @ApiResponse(responseCode = "409", description = "Preboarding already promoted.",
                            content = @Content())
            })
    public Response promotePreboarding( @PathParam("id") Long id,
            @Nullable PromoteRequest req ) {
        if ( req == null || req.eeId == null ) {
            throw new BadRequestException( "Request body must include `ee_id`." );
        }
        PreboardingExperiment skel = loadPreboardingOrThrow( id );
        ExpressionExperiment ee = expressionExperimentService.load( req.eeId );
        if ( ee == null ) {
            throw new NotFoundException( "No ExpressionExperiment with id " + req.eeId );
        }
        PreboardingExperimentService.PromotionResult result;
        try {
            result = preboardingService.promote( ee, skel );
        } catch ( PreboardingExperimentService.PreboardingAlreadyPromotedException ex ) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put( "error", "Preboarding already promoted" );
            body.put( "preboarding_id", id );
            return Response.status( Response.Status.CONFLICT ).entity( body ).build();
        }
        PromoteResponse body = new PromoteResponse();
        body.preboardingId = result.getPreboardingId();
        body.eeId = result.getEeId();
        body.promotedAt = new Date();
        body.proposalsRebound = result.getProposalsRebound();
        // apply_latest_proposal=true is accepted but the server-side apply
        // chain is out of scope per the STATUS file. Reflect that in the
        // response so the caller knows the apply did not happen.
        body.appliedProposalId = null;
        return Response.ok( body ).build();
    }

    private PreboardingExperiment loadPreboardingOrThrow( Long id ) {
        if ( id == null ) {
            throw new BadRequestException( "Preboarding id is required." );
        }
        PreboardingExperiment skel = preboardingService.load( id );
        if ( skel == null ) {
            throw new NotFoundException( "No preboarding with id " + id );
        }
        return skel;
    }

    private PreboardingResponse toPreboardingResponse( PreboardingExperiment skel,
            @Nullable AgentProposal latest, long proposalCount ) {
        PreboardingResponse r = new PreboardingResponse();
        r.preboardingId = skel.getId();
        r.accession = skel.getAccession();
        r.source = skel.getSource();
        r.identifyingMetadata = skel.getIdentifyingMetadata();
        WorkflowState ws = skel.getWorkflowState();
        r.state = ws != null ? ws.name() : null;
        r.enteredCurrentStateAt = skel.getWorkflowStateEnteredAt();
        r.proposalCount = proposalCount;
        r.latestProposal = latest == null ? null : toProposalResponse( latest, skel.getId() );
        r.auditTrailUrl = "/preboarding/" + skel.getId() + "/auditEvents";
        return r;
    }

    private ProposalResponse toProposalResponse( AgentProposal p, Long preboardingId ) {
        ProposalResponse r = new ProposalResponse();
        r.proposalId = p.getId();
        r.preboardingId = preboardingId;
        r.runId = p.getRunId();
        r.agentVersion = p.getAgentVersion();
        r.model = p.getModel();
        r.ranAt = p.getRanAt();
        r.payloadJson = p.getPayloadJson();
        return r;
    }

    /* ===================== DTOs ===================== */

    /** Body of {@link #createPreboarding}. */
    public static class CreatePreboardingRequest {
        @JsonProperty("accession")
        public String accession;
        @JsonProperty("source")
        @Nullable
        public String source;
        /**
         * Identifying metadata as a JSON string (title, summary, submitter,
         * pubmed_id, ...). Stored verbatim in
         * {@code INVESTIGATION.PREBOARDING_IDENTIFYING_METADATA}. Accepting it
         * as a raw String here keeps the public surface flexible — the
         * agents-side shape can evolve without a Gemma deploy.
         */
        @JsonProperty("identifying_metadata")
        @Nullable
        public String identifyingMetadata;
    }

    /** Body of {@link #attachProposal}. */
    public static class AttachProposalRequest {
        @JsonProperty("run_id")
        public String runId;
        @JsonProperty("agent_version")
        @Nullable
        public String agentVersion;
        @JsonProperty("model")
        @Nullable
        public String model;
        @JsonProperty("ran_at")
        @Nullable
        public Date ranAt;
        @JsonProperty("payload_json")
        @Nullable
        public String payloadJson;
    }

    /** Body of {@link #promotePreboarding}. */
    public static class PromoteRequest {
        @JsonProperty("ee_id")
        public Long eeId;
        @JsonProperty("apply_latest_proposal")
        @Nullable
        public Boolean applyLatestProposal;
    }

    /** Response of get/create preboarding. */
    public static class PreboardingResponse {
        @JsonProperty("preboarding_id")
        public Long preboardingId;
        public String accession;
        public String source;
        @JsonProperty("identifying_metadata")
        @Nullable
        public String identifyingMetadata;
        public String state;
        @JsonProperty("entered_current_state_at")
        @Nullable
        public Date enteredCurrentStateAt;
        @JsonProperty("proposal_count")
        public long proposalCount;
        @JsonProperty("latest_proposal")
        @Nullable
        public ProposalResponse latestProposal;
        @JsonProperty("audit_trail_url")
        public String auditTrailUrl;
    }

    /** Response of attach-proposal + nested in preboarding GET. */
    public static class ProposalResponse {
        @JsonProperty("proposal_id")
        public Long proposalId;
        @JsonProperty("preboarding_id")
        public Long preboardingId;
        @JsonProperty("run_id")
        public String runId;
        @JsonProperty("agent_version")
        public String agentVersion;
        public String model;
        @JsonProperty("ran_at")
        public Date ranAt;
        @JsonProperty("payload_json")
        public String payloadJson;
    }

    /** Response of promote. */
    public static class PromoteResponse {
        @JsonProperty("preboarding_id")
        public Long preboardingId;
        @JsonProperty("ee_id")
        public Long eeId;
        @JsonProperty("promoted_at")
        public Date promotedAt;
        @JsonProperty("proposals_rebound")
        public int proposalsRebound;
        @JsonProperty("applied_proposal_id")
        @Nullable
        public Long appliedProposalId;
    }
}
