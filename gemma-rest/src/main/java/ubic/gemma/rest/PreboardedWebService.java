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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.PreboardedExperimentService;
import ubic.gemma.rest.util.ResponseErrorObject;

/**
 * REST surface for the proposed-experiment workflow.
 *
 * <p>Six endpoints per {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md}:</p>
 * <ul>
 *   <li>{@code POST   /preboarded} — create a preboarded from an accession.
 *       409 with existing entity id+type if accession is already known
 *       (either as a preboarded or a loaded EE).</li>
 *   <li>{@code GET    /preboarded/{id}} — fetch preboarded + latest annotation set.</li>
 *   <li>{@code GET    /preboarded?accession=X} — resolve accession to id.</li>
 *   <li>{@code POST   /preboarded/{id}/annotation-sets} — attach an annotation
 *       set (typically a PROPOSAL role from an agent run), idempotent on
 *       {@code (role, runId)}.</li>
 *   <li>{@code POST   /preboarded/{id}/promote} — promote to a loaded EE.
 *       Restricted to GROUP_CURATOR / GROUP_ADMIN; agents cannot promote.</li>
 *   <li>{@code GET    /preboarded?state=...} — list preboarded by workflow state
 *       (curator triage view).</li>
 * </ul>
 *
 * <p>Auth model (handoff §"Authorization"):</p>
 * <ul>
 *   <li>POST preboarded + POST annotation-sets: GROUP_CURATOR / GROUP_ADMIN /
 *       GROUP_AGENT. Agents need to create preboarded and attach annotation
 *       sets.</li>
 *   <li>POST promote: GROUP_CURATOR / GROUP_ADMIN only. Agents MUST get 403.</li>
 *   <li>GET endpoints: any authenticated user (no @PreAuthorize on reads).</li>
 * </ul>
 */
@Service
@Path("/")
@Tag(name = "Preboarded", description = "Proposed-but-not-loaded experiments + agent annotation sets")
public class PreboardedWebService {

    private final PreboardedExperimentService preboardedService;
    private final AnnotationSetService annotationSetService;
    private final ExpressionExperimentService expressionExperimentService;

    @Autowired
    public PreboardedWebService( PreboardedExperimentService preboardedService,
            AnnotationSetService annotationSetService,
            ExpressionExperimentService expressionExperimentService ) {
        this.preboardedService = preboardedService;
        this.annotationSetService = annotationSetService;
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Create a new {@link PreboardedExperiment} for the supplied accession.
     * Returns 409 with the existing id+type if the accession already exists
     * (as a preboarded OR an EE).
     */
    @POST
    @Path("/preboarded")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Create a preboarded for a previously-unknown accession",
            description = "Body: `{\"accession\":\"GSE12345\",\"source\":\"GEO\","
                    + "\"identifyingMetadata\":{...}}`. Returns 409 with the existing entity's id "
                    + "and type when the accession is already known to Gemma.",
            responses = {
                    @ApiResponse(responseCode = "201", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Missing accession.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409",
                            description = "An entity with this accession already exists. Body includes the existing entity's id and type.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(description = "{ error, accession, existingId, existingType }")))
            })
    public Response createPreboarded( @Nullable CreatePreboardedRequest req ) {
        if ( req == null || req.accession == null || req.accession.trim().isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-blank `accession`." );
        }
        String accession = req.accession.trim();
        try {
            PreboardedExperiment skel = preboardedService.createPreboarded( accession,
                    req.source, req.identifyingMetadata );
            PreboardedResponse body = toPreboardedResponse( skel, null, 0L );
            return Response.status( Response.Status.CREATED ).entity( body ).build();
        } catch ( PreboardedExperimentService.AccessionAlreadyExistsException e ) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put( "error", "Accession already exists" );
            body.put( "accession", accession );
            body.put( "existingId", e.getExistingId() );
            body.put( "existingType", e.getExistingType() );
            return Response.status( Response.Status.CONFLICT ).entity( body ).build();
        }
    }

    /**
     * Fetch a preboarded by id + its latest annotation set (proposal role).
     */
    @GET
    @Path("/preboarded/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Fetch a preboarded + latest annotation set",
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No preboarded with that id.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public PreboardedResponse getPreboarded( @PathParam("id") Long id ) {
        PreboardedExperiment skel = loadPreboardedOrThrow( id );
        AnnotationSet latest = annotationSetService.findLatestByInvestigation( skel, AnnotationSetRole.PROPOSAL );
        long total = annotationSetService.countByInvestigation( skel, AnnotationSetRole.PROPOSAL );
        return toPreboardedResponse( skel, latest, total );
    }

    /**
     * Listing endpoint: by accession (returns the single matching preboarded,
     * 404 if none) or by state (queue view, returns the most-recent
     * preboarded in that state — full pagination deferred).
     */
    @GET
    @Path("/preboarded")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Resolve accession -> preboarded, or list preboarded in a state",
            description = "Exactly one of `accession` or `state` must be supplied.")
    public Response listOrResolvePreboarded(
            @Parameter(description = "Resolve accession -> preboarded id.")
            @QueryParam("accession") @Nullable String accession,
            @Parameter(description = "Filter by WorkflowState (e.g. Preboarded, Candidate, Discovery).")
            @QueryParam("state") @Nullable String stateName ) {
        if ( ( accession == null || accession.isEmpty() )
                && ( stateName == null || stateName.isEmpty() ) ) {
            throw new BadRequestException( "Exactly one of `accession` or `state` must be supplied." );
        }
        if ( accession != null && !accession.isEmpty() ) {
            PreboardedExperiment skel = preboardedService.findByAccession( accession );
            if ( skel == null ) {
                throw new NotFoundException( "No preboarded with accession " + accession );
            }
            AnnotationSet latest = annotationSetService.findLatestByInvestigation( skel, AnnotationSetRole.PROPOSAL );
            long total = annotationSetService.countByInvestigation( skel, AnnotationSetRole.PROPOSAL );
            return Response.ok( toPreboardedResponse( skel, latest, total ) ).build();
        }
        WorkflowState s;
        try {
            s = WorkflowState.valueOf( stateName );
        } catch ( IllegalArgumentException ex ) {
            throw new BadRequestException( "Unknown state: " + stateName );
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put( "error", "Queue-by-state listing is served by /workflow/queue" );
        body.put( "state", s.name() );
        body.put( "redirectTo",
                "/workflow/queue?state=" + s.name() + "&datasetType=preboarded_experiment" );
        return Response.status( Response.Status.NOT_IMPLEMENTED ).entity( body ).build();
    }

    /**
     * Attach a new annotation set to a preboarded. Idempotent on
     * {@code (role, runId)}: if a row with the same triple already exists,
     * returns 200 OK with the existing row rather than 201 Created.
     */
    @POST
    @Path("/preboarded/{id}/annotation-sets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Attach an annotation set to a preboarded",
            description = "Idempotent on `(role, runId)`: a retry returns 200 with the existing row.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "New annotation set attached.",
                            content = @Content()),
                    @ApiResponse(responseCode = "200", description = "An annotation set with this (role, runId) already attached.",
                            content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Missing runId.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "Preboarded not found.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseErrorObject.class)))
            })
    public Response attachAnnotationSet( @PathParam("id") Long id,
            @Nullable AttachAnnotationSetRequest req ) {
        if ( req == null || req.runId == null || req.runId.trim().isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-blank `runId`." );
        }
        AgentCurationKind kind = parseKindOrThrow( req.kind, AgentCurationKind.PROPOSAL );
        AnnotationSetRole role = parseRoleOrDefault( req.role, AnnotationSetRole.PROPOSAL );
        AnnotationSetSource source = parseSourceOrDefault( req.source, AnnotationSetSource.AGENT );
        PreboardedExperiment skel = loadPreboardedOrThrow( id );
        AnnotationSetService.AttachedAnnotationSet attached = annotationSetService.attach(
                skel, role, source, role == AnnotationSetRole.PROPOSAL ? kind : null,
                req.runId.trim(), req.createdBy,
                req.agentVersion, req.model, req.ranAt, req.payloadJson, null );
        AnnotationSetSnapshotResponse body = toSnapshotResponse( attached.getAnnotationSet(), skel.getId() );
        Response.Status status = attached.isCreated()
                ? Response.Status.CREATED
                : Response.Status.OK;
        return Response.status( status ).entity( body ).build();
    }

    /**
     * Promote a preboarded to a loaded {@link ExpressionExperiment}.
     */
    @POST
    @Path("/preboarded/{id}/promote")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Promote a preboarded to a loaded ExpressionExperiment",
            description = "Curator-only. Rebinds AnnotationSet rows from the preboarded to the EE; "
                    + "advances both rows' workflow state to Loaded (when not further along). "
                    + "`applyLatestProposal` is accepted for forward-compatibility but currently "
                    + "a no-op; the curator applies the proposal via the design-write / "
                    + "annotation-write endpoints. See STATUS_PROPOSED_EXPERIMENT_WORKFLOW.md.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Promoted.", content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Missing eeId.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "Preboarded or EE not found.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "409", description = "Preboarded already promoted.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(description = "{ error, preboardedId }")))
            })
    public Response promotePreboarded( @PathParam("id") Long id,
            @Nullable PromoteRequest req ) {
        if ( req == null || req.eeId == null ) {
            throw new BadRequestException( "Request body must include `eeId`." );
        }
        PreboardedExperiment skel = loadPreboardedOrThrow( id );
        ExpressionExperiment ee = expressionExperimentService.load( req.eeId );
        if ( ee == null ) {
            throw new NotFoundException( "No ExpressionExperiment with id " + req.eeId );
        }
        PreboardedExperimentService.PromotionResult result;
        try {
            result = preboardedService.promote( ee, skel );
        } catch ( PreboardedExperimentService.PreboardedAlreadyPromotedException ex ) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put( "error", "Preboarded already promoted" );
            body.put( "preboardedId", id );
            return Response.status( Response.Status.CONFLICT ).entity( body ).build();
        }
        PromoteResponse body = new PromoteResponse();
        body.preboardedId = result.getPreboardedId();
        body.eeId = result.getEeId();
        body.promotedAt = new Date();
        body.annotationSetsRebound = result.getAnnotationSetsRebound();
        body.appliedProposalId = null;
        return Response.ok( body ).build();
    }

    private PreboardedExperiment loadPreboardedOrThrow( Long id ) {
        if ( id == null ) {
            throw new BadRequestException( "Preboarded id is required." );
        }
        PreboardedExperiment skel = preboardedService.load( id );
        if ( skel == null ) {
            throw new NotFoundException( "No preboarded with id " + id );
        }
        return skel;
    }

    private PreboardedResponse toPreboardedResponse( PreboardedExperiment skel,
            @Nullable AnnotationSet latest, long proposalCount ) {
        PreboardedResponse r = new PreboardedResponse();
        r.preboardedId = skel.getId();
        r.accession = skel.getAccession();
        r.source = skel.getSource();
        // The payload moved up to Investigation.sourceMetadata (it is wanted for imported
        // experiments too, not only preboarded ones). The RESPONSE field keeps its name: it is on
        // the wire and renaming it would break clients.
        r.identifyingMetadata = skel.getSourceMetadata();
        WorkflowState ws = skel.getWorkflowState();
        r.state = ws != null ? ws.name() : null;
        r.enteredCurrentStateAt = skel.getWorkflowStateEnteredAt();
        r.proposalCount = proposalCount;
        r.latestAnnotationSet = latest == null ? null : toSnapshotResponse( latest, skel.getId() );
        r.auditTrailUrl = "/preboarded/" + skel.getId() + "/auditEvents";
        return r;
    }

    private static AnnotationSetSnapshotResponse toSnapshotResponse( AnnotationSet a, Long preboardedId ) {
        AnnotationSetSnapshotResponse r = new AnnotationSetSnapshotResponse();
        r.annotationSetId = a.getId();
        r.preboardedId = preboardedId;
        r.role = a.getRole() != null ? a.getRole().getDbValue() : null;
        r.kind = a.getKind() != null ? a.getKind().getDbValue() : null;
        r.runId = a.getRunId();
        r.agentVersion = a.getAgentVersion();
        r.model = a.getModel();
        r.ranAt = a.getRanAt();
        r.payloadJson = a.getPayloadJson();
        return r;
    }

    /**
     * Parse the body-side {@code kind} for a POST. Accepts null/blank as the
     * caller-supplied default; throws 400 on unknown values.
     */
    static AgentCurationKind parseKindOrThrow( @Nullable String kind, AgentCurationKind defaultKind ) {
        if ( kind == null || kind.trim().isEmpty() ) {
            return defaultKind;
        }
        try {
            return AgentCurationKind.fromDbValue( kind.trim() );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown kind: " + kind + " (expected 'proposal' or 'audit')" );
        }
    }

    private static AnnotationSetRole parseRoleOrDefault( @Nullable String role, AnnotationSetRole fallback ) {
        if ( role == null || role.trim().isEmpty() ) {
            return fallback;
        }
        try {
            return AnnotationSetRole.fromDbValue( role.trim() );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown role: " + role
                    + " (expected 'proposal', 'draft', or 'snapshot')" );
        }
    }

    private static AnnotationSetSource parseSourceOrDefault( @Nullable String source,
            AnnotationSetSource fallback ) {
        if ( source == null || source.trim().isEmpty() ) {
            return fallback;
        }
        try {
            return AnnotationSetSource.fromDbValue( source.trim() );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown source: " + source );
        }
    }

    /* ===================== DTOs ===================== */

    /** Body of {@link #createPreboarded}. */
    public static class CreatePreboardedRequest {
        @JsonProperty("accession")
        public String accession;
        @JsonProperty("source")
        @Nullable
        public String source;
        @JsonProperty("identifyingMetadata")
        @JsonAlias("identifying_metadata")
        @Nullable
        public String identifyingMetadata;
    }

    /** Body of {@link #attachAnnotationSet}. */
    public static class AttachAnnotationSetRequest {
        @JsonProperty("runId")
        @JsonAlias("run_id")
        public String runId;
        @JsonProperty("agentVersion")
        @JsonAlias("agent_version")
        @Nullable
        public String agentVersion;
        @JsonProperty("model")
        @Nullable
        public String model;
        @JsonProperty("ranAt")
        @JsonAlias("ran_at")
        @Nullable
        public Date ranAt;
        @JsonProperty("payloadJson")
        @JsonAlias("payload_json")
        @Nullable
        public String payloadJson;
        /**
         * Sub-discriminator on PROPOSAL rows. {@code "proposal"} (default) or
         * {@code "audit"}. Case-insensitive.
         */
        @JsonProperty("kind")
        @Nullable
        public String kind;
        /**
         * Annotation set role; default {@code "proposal"} matches the original
         * /proposals endpoint semantics.
         */
        @JsonProperty("role")
        @Nullable
        public String role;
        /**
         * Annotation set source; default {@code "agent"} for the preboarded
         * surface.
         */
        @JsonProperty("source")
        @Nullable
        public String source;
        @JsonProperty("createdBy")
        @JsonAlias("created_by")
        @Nullable
        public String createdBy;
    }

    /** Body of {@link #promotePreboarded}. */
    public static class PromoteRequest {
        @JsonProperty("eeId")
        @JsonAlias("ee_id")
        public Long eeId;
        @JsonProperty("applyLatestProposal")
        @JsonAlias("apply_latest_proposal")
        @Nullable
        public Boolean applyLatestProposal;
    }

    /** Response of get/create preboarded. */
    public static class PreboardedResponse {
        @JsonProperty("preboardedId")
        public Long preboardedId;
        public String accession;
        public String source;
        @JsonProperty("identifyingMetadata")
        @Nullable
        public String identifyingMetadata;
        public String state;
        @JsonProperty("enteredCurrentStateAt")
        @Nullable
        public Date enteredCurrentStateAt;
        @JsonProperty("proposalCount")
        public long proposalCount;
        @JsonProperty("latestAnnotationSet")
        @Nullable
        public AnnotationSetSnapshotResponse latestAnnotationSet;
        @JsonProperty("auditTrailUrl")
        public String auditTrailUrl;
    }

    /** Thin annotation-set summary shown inline on the preboarded GET. */
    public static class AnnotationSetSnapshotResponse {
        @JsonProperty("annotationSetId")
        public Long annotationSetId;
        @JsonProperty("preboardedId")
        public Long preboardedId;
        @JsonProperty("role")
        public String role;
        @JsonProperty("kind")
        @Nullable
        public String kind;
        @JsonProperty("runId")
        public String runId;
        @JsonProperty("agentVersion")
        @Nullable
        public String agentVersion;
        @Nullable
        public String model;
        @JsonProperty("ranAt")
        @Nullable
        public Date ranAt;
        @JsonProperty("payloadJson")
        @Nullable
        public String payloadJson;
    }

    /** Response of promote. */
    public static class PromoteResponse {
        @JsonProperty("preboardedId")
        public Long preboardedId;
        @JsonProperty("eeId")
        public Long eeId;
        @JsonProperty("promotedAt")
        public Date promotedAt;
        @JsonProperty("annotationSetsRebound")
        public int annotationSetsRebound;
        @JsonProperty("appliedProposalId")
        @Nullable
        public Long appliedProposalId;
    }
}
