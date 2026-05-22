/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Private curation API for the curation-UI.
 * <p>
 * Every handler in this class is annotated {@link Hidden} so the entire surface is excluded from the public
 * OpenAPI / Swagger documentation. The endpoints are still callable — they're just not advertised. This is the
 * "private namespace" the curation-UI uses for curator-only operations that the public REST surface should not
 * advertise.
 * <p>
 * Round 3 scope (per GEMMA_UI_ENDPOINT_GAP.md):
 * <ul>
 *   <li>{@code GET  /candidates} — screening queue (datasets needing curation attention).
 *       Implemented as a 302 redirect to {@code /datasets?filter=needsAttention=true} since the underlying
 *       query is already supported by the existing dataset filter.</li>
 *   <li>{@code POST /datasets/{id}/curation-proposals} — attach an {@code AgentProposal} to a loaded EE
 *       (consolidated with the preboarded path's surface per STATUS_CURATION_PROPOSALS.md).</li>
 *   <li>{@code GET  /datasets/{id}/curation-proposals} — list proposals attached to a loaded EE,
 *       newest first.</li>
 *   <li>{@code POST /datasets/{id}/audits} + {@code GET /datasets/{id}/audits} — thin URL aliases for
 *       the {@code kind=AUDIT} flavor of curation proposals (per GEMMA_UI_ENDPOINT_GAP §3f). Delegate to
 *       the unified handlers with {@code kind} pre-bound to {@link AgentCurationKind#AUDIT}.</li>
 * </ul>
 */
@Service
@Hidden
@Path("/")
@Slf4j
public class CurationWebService {

    @Autowired
    private DatasetArgService datasetArgService;

    @Autowired
    private AgentProposalService agentProposalService;

    /**
     * Screening queue: redirect to the existing {@code /datasets?filter=needsAttention=true} query. Implemented
     * as a 302 so any query parameters the caller supplies (limit, offset, sort, etc.) pass through verbatim.
     */
    @GET
    @Hidden
    @Path("/candidates")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true)
    public Response getCandidates( @Context UriInfo uriInfo ) {
        UriBuilder builder = uriInfo.getBaseUriBuilder()
                .scheme( null ).host( null ).port( -1 )
                .path( "/datasets" )
                .queryParam( "filter", "curationDetails.needsAttention = true" );
        uriInfo.getQueryParameters().forEach( ( k, vs ) -> {
            if ( !"filter".equals( k ) ) {
                vs.forEach( v -> builder.queryParam( k, v ) );
            }
        } );
        return Response.status( Response.Status.FOUND ).location( builder.build() ).build();
    }

    /**
     * Request body for {@link #submitCurationProposal}. Mirrors
     * {@link PreboardedWebService.AttachProposalRequest} so the two endpoints accept the same payload shape — the
     * underlying {@link AgentProposalService#attach} is the same call. See STATUS_CURATION_PROPOSALS.md.
     */
    public static class CurationProposalRequest {
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
        /**
         * Discriminator: {@code "proposal"} (default if absent) or {@code "audit"}.
         * Case-insensitive. Unknown values cause 400.
         */
        @JsonProperty("kind")
        @Nullable
        public String kind;
    }

    /**
     * Response of {@link #submitCurationProposal} and per-row of {@link #listCurationProposals}
     * when {@code shape=full} (the default).
     */
    public static class CurationProposalResponse {
        @JsonProperty("proposal_id")
        public Long proposalId;
        @JsonProperty("dataset_id")
        public Long datasetId;
        @JsonProperty("kind")
        public String kind;
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

    /**
     * Per-row response of {@link #listCurationProposals} under
     * {@code ?shape=meta}. Mirrors {@link AgentCurationSummaryValueObject}
     * with snake_case wire names + {@code dataset_id} aliasing.
     */
    public static class CurationProposalSummaryResponse {
        @JsonProperty("proposal_id")
        public Long proposalId;
        @JsonProperty("dataset_id")
        public Long datasetId;
        @JsonProperty("kind")
        public String kind;
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
        @JsonProperty("payload_size")
        @Nullable
        public Long payloadSize;
    }

    /**
     * Attach an {@link AgentProposal} to a loaded EE. Idempotent on {@code run_id}: a retry returns the existing
     * row as 200 OK rather than 201 Created. The underlying service is the same one the public
     * {@link PreboardedWebService#attachProposal} uses; this endpoint just provides a dataset-centric URL pattern
     * the curation-UI prefers.
     */
    @POST
    @Hidden
    @Path("/datasets/{dataset}/curation-proposals")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(hidden = true)
    public Response submitCurationProposal(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable CurationProposalRequest body
    ) {
        if ( body == null || body.runId == null || body.runId.trim().isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-blank `run_id`." );
        }
        AgentCurationKind kind = parseKindOrThrow( body.kind, AgentCurationKind.PROPOSAL );
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        AgentProposalService.AttachedProposal attached = agentProposalService.attach( ee, kind,
                body.runId.trim(), body.agentVersion, body.model, body.ranAt, body.payloadJson );
        CurationProposalResponse resp = toProposalResponse( attached.getProposal(), ee.getId() );
        Response.Status status = attached.isCreated()
                ? Response.Status.CREATED
                : Response.Status.OK;
        return Response.status( status ).entity( resp ).build();
    }

    /**
     * List {@link AgentProposal}s attached to a loaded EE, newest first. The
     * {@code ?kind=} param filters by discriminator; {@code ?shape=} selects
     * the response shape (default {@code full} preserves the legacy wire
     * shape; {@code meta} returns the thin
     * {@link CurationProposalSummaryResponse} projection that omits
     * {@code payload_json}).
     */
    @GET
    @Hidden
    @Path("/datasets/{dataset}/curation-proposals")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(hidden = true)
    public Response listCurationProposals(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Filter by discriminator: `proposal`, `audit`, or `all` (default).")
            @QueryParam("kind") @Nullable String kind,
            @Parameter(description = "Response shape: `full` (default; carries payload_json) "
                    + "or `meta` (thin projection, payload_size only).")
            @QueryParam("shape") @Nullable String shape
    ) {
        AgentCurationKind kindFilter = parseKindFilter( kind );
        boolean metaShape = parseShapeIsMeta( shape );
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( metaShape ) {
            List<AgentCurationSummaryValueObject> summaries =
                    agentProposalService.findSummariesByInvestigation( ee, kindFilter );
            List<CurationProposalSummaryResponse> rows = new ArrayList<>( summaries.size() );
            for ( AgentCurationSummaryValueObject s : summaries ) {
                rows.add( toSummaryResponse( s ) );
            }
            return Response.ok( rows ).build();
        }
        List<AgentProposal> proposals = agentProposalService.findByInvestigation( ee );
        List<CurationProposalResponse> rows = new ArrayList<>( proposals.size() );
        for ( AgentProposal p : proposals ) {
            if ( kindFilter != null && p.getKind() != kindFilter ) continue;
            rows.add( toProposalResponse( p, ee.getId() ) );
        }
        return Response.ok( rows ).build();
    }

    /**
     * Thin alias for {@link #submitCurationProposal} with {@code kind} pre-bound to
     * {@link AgentCurationKind#AUDIT}. The body field {@code kind} (if present) is ignored — the path
     * is the discriminator. See GEMMA_UI_ENDPOINT_GAP §3f.
     */
    @POST
    @Hidden
    @Path("/datasets/{dataset}/audits")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(hidden = true)
    public Response submitAudit(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable CurationProposalRequest body
    ) {
        if ( body == null || body.runId == null || body.runId.trim().isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-blank `run_id`." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        AgentProposalService.AttachedProposal attached = agentProposalService.attach( ee, AgentCurationKind.AUDIT,
                body.runId.trim(), body.agentVersion, body.model, body.ranAt, body.payloadJson );
        CurationProposalResponse resp = toProposalResponse( attached.getProposal(), ee.getId() );
        Response.Status status = attached.isCreated()
                ? Response.Status.CREATED
                : Response.Status.OK;
        return Response.status( status ).entity( resp ).build();
    }

    /**
     * Thin alias for {@link #listCurationProposals} with {@code kind} pre-bound to
     * {@link AgentCurationKind#AUDIT}. {@code ?shape=meta|full} (default {@code full}) is honoured.
     * See GEMMA_UI_ENDPOINT_GAP §3f.
     */
    @GET
    @Hidden
    @Path("/datasets/{dataset}/audits")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(hidden = true)
    public Response listAudits(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Parameter(description = "Response shape: `full` (default; carries payload_json) "
                    + "or `meta` (thin projection, payload_size only).")
            @QueryParam("shape") @DefaultValue("full") String shape
    ) {
        return listCurationProposals( datasetArg, AgentCurationKind.AUDIT.getDbValue(), shape );
    }

    private static CurationProposalResponse toProposalResponse( AgentProposal p, Long datasetId ) {
        CurationProposalResponse r = new CurationProposalResponse();
        r.proposalId = p.getId();
        r.datasetId = datasetId;
        r.kind = p.getKind() != null ? p.getKind().getDbValue() : AgentCurationKind.PROPOSAL.getDbValue();
        r.runId = p.getRunId();
        r.agentVersion = p.getAgentVersion();
        r.model = p.getModel();
        r.ranAt = p.getRanAt();
        r.payloadJson = p.getPayloadJson();
        return r;
    }

    private static CurationProposalSummaryResponse toSummaryResponse( AgentCurationSummaryValueObject s ) {
        CurationProposalSummaryResponse r = new CurationProposalSummaryResponse();
        r.proposalId = s.getId();
        r.datasetId = s.getInvestigationId();
        r.kind = s.getKind() != null ? s.getKind().getDbValue() : AgentCurationKind.PROPOSAL.getDbValue();
        r.runId = s.getRunId();
        r.agentVersion = s.getAgentVersion();
        r.model = s.getModel();
        r.ranAt = s.getRanAt();
        r.payloadSize = s.getPayloadSize();
        return r;
    }

    /**
     * Parse the {@code ?kind=} query param into a filter value. {@code null}
     * / empty / {@code "all"} -> {@code null} (no filter); anything else is
     * passed through {@link AgentCurationKind#fromDbValue(String)} (case
     * insensitive), throwing 400 on unknown values.
     */
    @Nullable
    static AgentCurationKind parseKindFilter( @Nullable String kind ) {
        if ( kind == null || kind.isEmpty() || "all".equalsIgnoreCase( kind ) ) {
            return null;
        }
        try {
            return AgentCurationKind.fromDbValue( kind );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown kind: " + kind + " (expected 'proposal', 'audit', or 'all')" );
        }
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

    /**
     * @return true iff the {@code ?shape=} param requests the thin meta
     *         projection. Default (null / blank) is {@code full} per Paul's
     *         resolution on the recce open question.
     */
    static boolean parseShapeIsMeta( @Nullable String shape ) {
        if ( shape == null || shape.isEmpty() || "full".equalsIgnoreCase( shape ) ) {
            return false;
        }
        if ( "meta".equalsIgnoreCase( shape ) ) {
            return true;
        }
        throw new BadRequestException( "Unknown shape: " + shape + " (expected 'meta' or 'full')" );
    }
}
