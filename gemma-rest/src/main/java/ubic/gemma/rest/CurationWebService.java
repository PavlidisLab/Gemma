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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.Responders;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
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
 * Curation API for the curation-UI (Gemma 2.0 modern surface).
 * <p>
 * Two flavors of route live here:
 * <ul>
 *   <li><b>Per-dataset routes</b> ({@code /datasets/{id}/curation-proposals},
 *       {@code /datasets/{id}/audits}). The JAX-RS annotations for these
 *       endpoints are declared on {@link DatasetsWebService} because Jersey
 *       resolves {@code /datasets/*} against the class-level
 *       {@code @Path("/datasets")} and never falls through to this resource's
 *       class-level {@code @Path("/")}. The handler bodies live here; the
 *       DatasetsWebService methods delegate.</li>
 *   <li><b>Cross-experiment routes</b> ({@code /curation-proposals},
 *       {@code /audits}, plus their {@code /{id}} + lifecycle mutations).
 *       Declared here directly.</li>
 * </ul>
 */
@Service
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
    @Path("/candidates")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Screening queue (alias of /datasets?filter=curationDetails.needsAttention=true)",
            description = "Redirects to /datasets with `curationDetails.needsAttention = true` applied. Implemented as a 302 so any query parameters the caller supplies (limit, offset, sort, etc.) pass through verbatim.",
            responses = { @ApiResponse(responseCode = "302", description = "Redirection to /datasets with the needs-attention filter applied.") })
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
        /** Lifecycle status: {@code OPEN}, {@code FINALIZED}, {@code REOPENED}. */
        @JsonProperty("status")
        @Nullable
        public String status;
        /** Curator-chosen disposition wire string; null until set. */
        @JsonProperty("disposition")
        @Nullable
        public String disposition;
        @JsonProperty("disposition_note")
        @Nullable
        public String dispositionNote;
        @JsonProperty("finalized_at")
        @Nullable
        public Date finalizedAt;
        @JsonProperty("last_updated")
        @Nullable
        public Date lastUpdated;
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
     * <p>
     * Routing lives on {@link DatasetsWebService#submitDatasetCurationProposal} — Jersey resolves
     * {@code /datasets/*} against the {@code @Path("/datasets")} class and never falls through to this resource's
     * class-level {@code @Path("/")}, so the JAX-RS annotations are declared there. This method is the
     * implementation the delegator invokes.
     */
    public Response submitCurationProposal(
            DatasetArg<?> datasetArg,
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
     * {@code kind} param filters by discriminator; {@code shape} selects the
     * response shape (default {@code full} preserves the legacy wire shape;
     * {@code meta} returns the thin {@link CurationProposalSummaryResponse}
     * projection that omits {@code payload_json}).
     * <p>
     * Routing on {@link DatasetsWebService#listDatasetCurationProposals}.
     */
    public Response listCurationProposals(
            DatasetArg<?> datasetArg,
            @Nullable String kind,
            @Nullable String shape
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
     * <p>
     * Routing on {@link DatasetsWebService#submitDatasetAudit}.
     */
    public Response submitAudit(
            DatasetArg<?> datasetArg,
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
     * {@link AgentCurationKind#AUDIT}. {@code shape=meta|full} (default {@code full}) is honoured.
     * See GEMMA_UI_ENDPOINT_GAP §3f.
     * <p>
     * Routing on {@link DatasetsWebService#listDatasetAudits}.
     */
    public Response listAudits(
            DatasetArg<?> datasetArg,
            String shape
    ) {
        return listCurationProposals( datasetArg, AgentCurationKind.AUDIT.getDbValue(), shape );
    }

    // ------------------------------------------------------------------
    // Cross-experiment inbox endpoints (curation-UI)
    //
    // The four read endpoints below — GET /curation-proposals (+ /{id}) and
    // GET /audits (+ /{id}) — back the curation-UI's "inbox" pages: a single
    // list of all PROPOSAL-kind (or AUDIT-kind) rows the curator can
    // disposition, regardless of which dataset they target.
    //
    // The mutation/state-machine endpoints (PATCH /curation-proposals/{id},
    // PATCH /audits/{id}, POST /audits/{id}/finalize, POST /audits/{id}/reopen)
    // flip the AgentProposal lifecycle columns added in Flyway mysql/V15 +
    // h2/V17 (STATUS / DISPOSITION / DISPOSITION_NOTE / FINALIZED_AT /
    // LAST_UPDATED). Note: per Paul's directive, the same dispositions ALSO
    // exist on CurationDraft via diff-derive in the local-api eval path —
    // the AgentProposal-side disposition is a complementary surface for the
    // post-evaluation phase, not a replacement. See
    // handoffs/RECCE_AGENT_CURATION_UNIFICATION.md §4 +
    // handoffs/CURATION_TO_GEMMA_2_0_HANDOFF.md.
    // ------------------------------------------------------------------

    /**
     * Cross-experiment inbox of {@code kind=PROPOSAL} agent curation rows,
     * paginated. Thin metadata projection (no {@code payload_json}).
     * <p>
     * ACL note: ACL is at the entity level — {@link AgentProposal} rows hang
     * off an {@link ubic.gemma.model.analysis.Investigation} but the DAO
     * doesn't apply ACL filtering here. The {@code @PreAuthorize} on this
     * method gates access to curators / admins / agents; per-row visibility
     * filtering is a TODO once the schema settles.
     */
    @GET
    @Path("/curation-proposals")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    public PaginatedResponseDataObject<AgentCurationSummaryValueObject> listProposalsInbox(
            @Parameter(description = "Reserved for future status filter — IGNORED today (no status column on AgentProposal yet).")
            @QueryParam("status") @Nullable String status,
            @Parameter(description = "Restrict to a comma-separated list of dataset (investigation) ids.")
            @QueryParam("datasetIds") @Nullable String datasetIds,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        return inboxList( AgentCurationKind.PROPOSAL, datasetIds, offsetArg, limitArg );
    }

    /**
     * Single {@link AgentProposal} (full payload). Returns 404 if not a
     * proposal-kind row.
     */
    @GET
    @Path("/curation-proposals/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    public ResponseDataObject<CurationProposalResponse> getProposal(
            @PathParam("id") Long id
    ) {
        AgentProposal p = loadByIdOfKindOrThrow( id, AgentCurationKind.PROPOSAL );
        Long invId = p.getInvestigation() != null ? p.getInvestigation().getId() : null;
        return Responders.respond( toProposalResponse( p, invId ) );
    }

    /**
     * Set the curator disposition on a {@code kind=PROPOSAL} row. Wire
     * vocabulary: {@code accept}, {@code reject}, {@code edit}, {@code park}
     * (see {@code handoffs/RECCE_AGENT_CURATION_UNIFICATION.md} §4.1).
     * Stamps {@code lastUpdated}; does NOT change lifecycle status (use
     * finalize / reopen for that). 404 if id resolves to an audit row.
     * <p>
     * TODO(@Audited): no {@code AgentProposalDispositionEvent} type exists
     * yet; emitting an audit row from here can come once the curation event
     * type hierarchy is in place.
     */
    @PATCH
    @Path("/curation-proposals/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    public Response patchProposal(
            @PathParam("id") Long id,
            @Nullable DispositionPatchRequest body
    ) {
        loadByIdOfKindOrThrow( id, AgentCurationKind.PROPOSAL );
        String disposition = validateDispositionOrThrow( body, /* audit */ false );
        String note = body != null ? body.note : null;
        AgentProposal updated = agentProposalService.updateDisposition( id, disposition, note );
        if ( updated == null ) {
            throw new NotFoundException( "No proposal with id " + id );
        }
        Long invId = updated.getInvestigation() != null ? updated.getInvestigation().getId() : null;
        return Response.ok( toProposalResponse( updated, invId ) ).build();
    }

    /**
     * Cross-experiment inbox of {@code kind=AUDIT} agent curation rows,
     * paginated. Thin metadata projection (no {@code payload_json}).
     */
    @GET
    @Path("/audits")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    public PaginatedResponseDataObject<AgentCurationSummaryValueObject> listAuditsInbox(
            @Parameter(description = "Reserved for future status filter — IGNORED today (no status column on AgentProposal yet).")
            @QueryParam("status") @Nullable String status,
            @Parameter(description = "Restrict to a comma-separated list of dataset (investigation) ids.")
            @QueryParam("datasetIds") @Nullable String datasetIds,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        return inboxList( AgentCurationKind.AUDIT, datasetIds, offsetArg, limitArg );
    }

    /**
     * Single audit row (full payload). Returns 404 if not an audit-kind row.
     */
    @GET
    @Path("/audits/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    public ResponseDataObject<CurationProposalResponse> getAudit(
            @PathParam("id") Long id
    ) {
        AgentProposal p = loadByIdOfKindOrThrow( id, AgentCurationKind.AUDIT );
        Long invId = p.getInvestigation() != null ? p.getInvestigation().getId() : null;
        return Responders.respond( toProposalResponse( p, invId ) );
    }

    /**
     * Set the curator disposition on a {@code kind=AUDIT} row. Wire
     * vocabulary as {@link #patchProposal}, plus {@code accepted_with_edits}.
     * Stamps {@code lastUpdated}; lifecycle status is unchanged here (use
     * finalize / reopen). 404 if id resolves to a proposal row.
     */
    @PATCH
    @Path("/audits/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    public Response patchAudit(
            @PathParam("id") Long id,
            @Nullable DispositionPatchRequest body
    ) {
        loadByIdOfKindOrThrow( id, AgentCurationKind.AUDIT );
        String disposition = validateDispositionOrThrow( body, /* audit */ true );
        if ( disposition == null ) {
            // Audit PATCH with no body-level disposition is a no-op (per-element
            // map is opaque pass-through at this layer); echo the row back.
            AgentProposal p = loadByIdOfKindOrThrow( id, AgentCurationKind.AUDIT );
            Long invId = p.getInvestigation() != null ? p.getInvestigation().getId() : null;
            return Response.ok( toProposalResponse( p, invId ) ).build();
        }
        String note = body != null ? body.note : null;
        AgentProposal updated = agentProposalService.updateDisposition( id, disposition, note );
        if ( updated == null ) {
            throw new NotFoundException( "No audit with id " + id );
        }
        Long invId = updated.getInvestigation() != null ? updated.getInvestigation().getId() : null;
        return Response.ok( toProposalResponse( updated, invId ) ).build();
    }

    /**
     * Mark an audit FINALIZED. Idempotent: second call returns 200 with no
     * state change. Returns 409 if the audit has no disposition set yet (an
     * unresponded audit can't be finalized).
     */
    @POST
    @Path("/audits/{id}/finalize")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    public Response finalizeAudit(
            @PathParam("id") Long id
    ) {
        AgentProposal p = loadByIdOfKindOrThrow( id, AgentCurationKind.AUDIT );
        if ( p.getDisposition() == null || p.getDisposition().trim().isEmpty() ) {
            return Response.status( Response.Status.CONFLICT )
                    .entity( conflictBody( "audit has no disposition; PATCH /audits/{id} first" ) )
                    .build();
        }
        AgentProposal updated = agentProposalService.finalizeProposal( id );
        if ( updated == null ) {
            throw new NotFoundException( "No audit with id " + id );
        }
        Long invId = updated.getInvestigation() != null ? updated.getInvestigation().getId() : null;
        return Response.ok( toProposalResponse( updated, invId ) ).build();
    }

    /**
     * Reopen a FINALIZED audit (sets status to REOPENED, clears
     * {@code finalizedAt}). Idempotent counterpart of {@link #finalizeAudit}.
     */
    @POST
    @Path("/audits/{id}/reopen")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    public Response reopenAudit(
            @PathParam("id") Long id
    ) {
        loadByIdOfKindOrThrow( id, AgentCurationKind.AUDIT );
        AgentProposal updated = agentProposalService.reopenProposal( id );
        if ( updated == null ) {
            throw new NotFoundException( "No audit with id " + id );
        }
        Long invId = updated.getInvestigation() != null ? updated.getInvestigation().getId() : null;
        return Response.ok( toProposalResponse( updated, invId ) ).build();
    }

    /**
     * Body shape for the PATCH disposition endpoints. Allow-list validation
     * runs at the handler boundary so a malformed body returns 400 before
     * touching the service layer.
     */
    public static class DispositionPatchRequest {
        @JsonProperty("disposition")
        @Nullable
        public String disposition;
        @JsonProperty("note")
        @Nullable
        public String note;
        /** Audit-only — per-element dispositions map. Opaque pass-through. */
        @JsonProperty("dispositions")
        @Nullable
        public Object dispositions;
    }

    /**
     * Shared helper: validate the disposition string against the allow-list.
     * Wire vocabulary from {@code handoffs/RECCE_AGENT_CURATION_UNIFICATION.md}
     * §4.1: proposal PATCH allows {@code accept}, {@code reject}, {@code edit},
     * {@code park}; audit PATCH additionally allows {@code accepted_with_edits}.
     * Returns the normalized lowercase string for storage, or {@code null} if
     * the body carries no disposition (audit PATCH only).
     */
    @Nullable
    private static String validateDispositionOrThrow( @Nullable DispositionPatchRequest body, boolean audit ) {
        if ( body == null || body.disposition == null || body.disposition.trim().isEmpty() ) {
            // Body-level disposition is optional on the audit PATCH (per-element
            // map can carry the change instead); required on the proposal PATCH.
            if ( !audit ) {
                throw new BadRequestException( "Request body must include a `disposition`." );
            }
            return null;
        }
        String d = body.disposition.trim().toLowerCase();
        switch ( d ) {
            case "accept":
            case "reject":
            case "edit":
            case "park":
                return d;
            case "accepted_with_edits":
                if ( audit ) return d;
                throw new BadRequestException( "Unknown disposition: " + body.disposition
                        + " (expected one of: accept, reject, edit, park)" );
            default:
                throw new BadRequestException( "Unknown disposition: " + body.disposition
                        + " (expected one of: accept, reject, edit, park"
                        + ( audit ? ", accepted_with_edits)" : ")" ) );
        }
    }

    /**
     * Shared inbox list helper: parse the comma-separated {@code datasetIds}
     * param, page via the DAO, wrap in a {@link Slice} + paginated response.
     */
    private PaginatedResponseDataObject<AgentCurationSummaryValueObject> inboxList(
            AgentCurationKind kind, @Nullable String datasetIds,
            OffsetArg offsetArg, LimitArg limitArg ) {
        List<Long> invIds = parseDatasetIdsOrThrow( datasetIds );
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        List<AgentCurationSummaryValueObject> rows = agentProposalService.listSummaries(
                kind, invIds, offset, limit );
        long total = agentProposalService.countSummaries( kind, invIds );
        Slice<AgentCurationSummaryValueObject> slice = new Slice<>( rows, null, offset, limit, total );
        return Responders.paginate( slice, new String[]{ "kind" } );
    }

    @Nullable
    private static List<Long> parseDatasetIdsOrThrow( @Nullable String s ) {
        if ( s == null || s.trim().isEmpty() ) {
            return null;
        }
        String[] parts = s.split( "," );
        List<Long> ids = new ArrayList<>( parts.length );
        for ( String part : parts ) {
            String t = part.trim();
            if ( t.isEmpty() ) continue;
            try {
                ids.add( Long.parseLong( t ) );
            } catch ( NumberFormatException e ) {
                throw new BadRequestException( "datasetIds must be a comma-separated list of integers; got: " + t );
            }
        }
        return ids.isEmpty() ? null : ids;
    }

    private AgentProposal loadByIdOfKindOrThrow( Long id, AgentCurationKind expectedKind ) {
        if ( id == null ) {
            throw new BadRequestException( "Path id is required." );
        }
        AgentProposal p = agentProposalService.load( id );
        if ( p == null ) {
            throw new NotFoundException( "No " + expectedKind.getDbValue() + " with id " + id );
        }
        AgentCurationKind k = p.getKind() != null ? p.getKind() : AgentCurationKind.PROPOSAL;
        if ( k != expectedKind ) {
            // A row exists but is the wrong kind — treat as 404 from this URL's
            // perspective (the row is reachable via the other endpoint).
            throw new NotFoundException( "No " + expectedKind.getDbValue() + " with id " + id
                    + " (id " + id + " is a " + k.getDbValue() + ")" );
        }
        return p;
    }

    /**
     * Small JSON envelope returned with a 409 Conflict so the curation-UI can
     * surface a specific reason ("audit has no disposition yet") rather than
     * a generic error.
     */
    private static Object conflictBody( String reason ) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put( "conflict", true );
        body.put( "reason", reason );
        return body;
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
        r.status = p.getStatus();
        r.disposition = p.getDisposition();
        r.dispositionNote = p.getDispositionNote();
        r.finalizedAt = p.getFinalizedAt();
        r.lastUpdated = p.getLastUpdated();
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
