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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSummaryValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageVerdict;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetTriageService;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.Responders;
import ubic.gemma.core.security.util.SecurityUtil;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

/**
 * REST surface for the unified {@link AnnotationSet} entity.
 *
 * <p>Two URL families:</p>
 * <ul>
 *   <li><b>Per-dataset</b> &mdash;
 *       {@code POST /datasets/{id}/annotation-sets},
 *       {@code GET /datasets/{id}/annotation-sets},
 *       {@code GET /datasets/{id}/annotation-sets/draft},
 *       {@code PUT /datasets/{id}/annotation-sets/draft}.
 *       JAX-RS annotations are declared on {@link DatasetsWebService}
 *       because Jersey resolves {@code /datasets/*} against the
 *       class-level {@code @Path("/datasets")} and never falls through
 *       to this resource's class-level {@code @Path("/")}. The handler
 *       bodies live here; the DatasetsWebService methods delegate.</li>
 *   <li><b>Cross-dataset</b> &mdash;
 *       {@code GET /annotation-sets},
 *       {@code GET /annotation-sets/{id}},
 *       {@code POST /annotation-sets/{id}/finalize},
 *       {@code POST /annotation-sets/{id}/reopen},
 *       {@code DELETE /annotation-sets/{id}}.
 *       Declared here directly.</li>
 * </ul>
 *
 * <p>Idempotency on create is {@code (investigation, role, runId)}. The
 * service-level {@code attach} carries the audit-conditional aspect so
 * an event row is emitted only on actual insert, not on retry.</p>
 */
@Service
@Path("/")
@Slf4j
public class AnnotationSetsWebService {

    @Autowired
    private DatasetArgService datasetArgService;

    @Autowired
    private AnnotationSetService annotationSetService;

    @Autowired
    private UserManager userManager;

    @Autowired
    private AnnotationSetTriageService annotationSetTriageService;

    /**
     * Screening queue: redirect to the existing
     * {@code /datasets?filter=curationDetails.needsAttention=true} query.
     * Implemented as a 302 so any query parameters the caller supplies
     * (limit, offset, sort, etc.) pass through verbatim.
     */
    @GET
    @Path("/candidates")
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Screening queue (alias of /datasets?filter=curationDetails.needsAttention=true)",
            description = "Redirects to /datasets with `curationDetails.needsAttention = true` applied.",
            responses = { @ApiResponse(responseCode = "302",
                    description = "Redirection to /datasets with the needs-attention filter applied.") })
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

    /* ============== per-dataset (delegated from DatasetsWebService) ============== */

    /**
     * Create or upsert an annotation set on a dataset. The body's
     * {@code role} determines the lifecycle shape:
     * {@code PROPOSAL} requires a {@code runId}; {@code DRAFT} derives
     * one from the current user (use the dedicated draft PUT instead for
     * upsert semantics); {@code SNAPSHOT} generates a UUID when
     * {@code runId} is omitted.
     */
    public Response submitAnnotationSet( DatasetArg<?> datasetArg,
            @Nullable AnnotationSetRequest body ) {
        if ( body == null ) {
            throw new BadRequestException( "Request body is required." );
        }
        AnnotationSetRole role = parseRoleOrThrow( body.role,
                "Request body must include `role` (proposal|draft|snapshot|commit)." );
        AnnotationSetSource source = parseSourceOrThrow( body.source, defaultSourceForRole( role ) );
        AgentCurationKind kind = parseKindOrThrow( body.kind, role == AnnotationSetRole.PROPOSAL
                ? AgentCurationKind.PROPOSAL : null );
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        AnnotationSet parent = body.parentId != null
                ? requireLoad( body.parentId, "parentId" )
                : null;
        AnnotationSetService.AttachedAnnotationSet attached = annotationSetService.attach(
                ee, role, source, kind,
                body.runId, body.createdBy,
                new AnnotationSetService.RunProvenance( body.agentVersion, body.model, body.runSha,
                        body.agentName, body.ranAt ),
                body.payloadJson, parent );
        Response.Status status = attached.isCreated()
                ? Response.Status.CREATED : Response.Status.OK;
        return Response.status( status )
                .entity( toResponse( attached.getAnnotationSet() ) )
                .build();
    }

    /**
     * List annotation sets attached to a dataset, newest first. Filter
     * by {@code role}, {@code source}, {@code createdBy}; choose
     * response shape with {@code shape=full|meta} (default {@code full}).
     */
    public Response listAnnotationSets( DatasetArg<?> datasetArg,
            @Nullable String role, @Nullable String source, @Nullable String createdBy,
            @Nullable String shape ) {
        AnnotationSetRole roleFilter = parseRoleFilter( role );
        AnnotationSetSource sourceFilter = parseSourceFilter( source );
        boolean metaShape = parseShapeIsMeta( shape );
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        if ( metaShape ) {
            List<AnnotationSetSummaryValueObject> summaries =
                    annotationSetService.findSummariesByInvestigation( ee, roleFilter );
            List<AnnotationSetSummaryResponse> rows = new ArrayList<>( summaries.size() );
            for ( AnnotationSetSummaryValueObject s : summaries ) {
                if ( sourceFilter != null && s.getSource() != sourceFilter ) continue;
                if ( createdBy != null && !createdBy.equals( s.getCreatedBy() ) ) continue;
                rows.add( toSummaryResponse( s ) );
            }
            return Response.ok( rows ).build();
        }
        List<AnnotationSet> sets = annotationSetService.findByInvestigation( ee, roleFilter );
        List<AnnotationSetResponse> rows = new ArrayList<>( sets.size() );
        for ( AnnotationSet a : sets ) {
            if ( sourceFilter != null && a.getSource() != sourceFilter ) continue;
            if ( createdBy != null && !createdBy.equals( a.getCreatedBy() ) ) continue;
            rows.add( toResponse( a ) );
        }
        return Response.ok( rows ).build();
    }

    /**
     * Fetch a curator's {@code DRAFT} for the given dataset. 404 if no draft
     * exists. Convenience over the role-filtered list because the curation
     * client hits this on every dataset open.
     *
     * @param onBehalfOf whose draft to read; see
     *                   {@link #resolveCurator(String)}. Reading is delegated
     *                   for the same reason writing is — an agent fetching
     *                   "the draft" without saying whose would get its own.
     */
    public Response getDraftForDataset( DatasetArg<?> datasetArg, @Nullable String onBehalfOf ) {
        String curator = resolveCurator( onBehalfOf );
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        AnnotationSet draft = annotationSetService.findByInvestigationAndRoleAndRunId(
                ee, AnnotationSetRole.DRAFT, draftRunId( curator ) );
        if ( draft == null ) {
            throw new NotFoundException( "No draft for dataset " + ee.getId()
                    + " belonging to " + curator + "." );
        }
        return Response.ok( toResponse( draft ) ).build();
    }

    /**
     * Upsert a curator's {@code DRAFT} for the given dataset.
     * One DRAFT per (dataset, curator); body's {@code payloadJson} +
     * optional {@code parkedElements} + optional {@code parentId}
     * (the PROPOSAL the draft was seeded from). Returns 201 on create,
     * 200 on update.
     *
     * @param onBehalfOf which curator this draft belongs to; see
     *                   {@link #resolveCurator(String)}
     */
    public Response upsertDraftForDataset( DatasetArg<?> datasetArg,
            @Nullable String onBehalfOf, @Nullable UpsertDraftRequest body ) {
        if ( body == null || body.payloadJson == null ) {
            throw new BadRequestException( "Request body must include `payloadJson`." );
        }
        String curator = resolveCurator( onBehalfOf );
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        AnnotationSet parent = body.parentId != null
                ? requireLoad( body.parentId, "parentId" )
                : null;
        boolean preExisted = annotationSetService.findByInvestigationAndRoleAndRunId(
                ee, AnnotationSetRole.DRAFT, draftRunId( curator ) ) != null;
        AnnotationSet draft = annotationSetService.upsertDraft( ee, curator,
                body.payloadJson, body.parkedElements, parent );
        Response.Status status = preExisted ? Response.Status.OK : Response.Status.CREATED;
        return Response.status( status ).entity( toResponse( draft ) ).build();
    }

    /**
     * Whose draft this is.
     *
     * <p>🛑 Not the authenticated principal. Curation reaches Gemma through
     * the curation agent rather than from a curator's browser, so the
     * principal on these calls is normally the agent acting for someone else.
     * The curator's own name is part of a {@code DRAFT}'s run id
     * ({@code "draft-{curator}"}), and that run id is inside
     * {@code UNIQUE(investigation, ROLE, RUN_ID)} — so attributing a draft to
     * the caller does not merely mis-label it. Every curator's draft on a
     * dataset would key to {@code draft-agent}, one row, and the second
     * autosave would overwrite the first with no error.
     *
     * <p>Delegation is refused rather than ignored for a caller who is neither
     * agent nor admin: silently substituting their own name would store a
     * different fact from the one they asked for.
     */
    private String resolveCurator( @Nullable String onBehalfOf ) {
        if ( onBehalfOf == null || onBehalfOf.isBlank() ) {
            return requireCurrentUser().getUserName();
        }
        return SecurityUtil.resolveActingIdentity( onBehalfOf );
    }

    /* ============== cross-dataset routes ============== */

    /**
     * Cross-experiment list of annotation sets, paginated. Thin
     * metadata projection (no {@code payloadJson}).
     */
    @GET
    @Path("/annotation-sets")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Cross-experiment list of annotation sets (thin projection)")
    public PaginatedResponseDataObject<AnnotationSetSummaryResponse> listAnnotationSetsAcross(
            @Parameter(description = "Filter by role: `proposal`, `draft`, `snapshot`, `commit`, or `all` (default).")
            @QueryParam("role") @Nullable String role,
            @Parameter(description = "Filter by source: `agent`, `curator`, `gemma_intake`, `external_import`, or `all` (default).")
            @QueryParam("source") @Nullable String source,
            @Parameter(description = "Filter by createdBy (username or agent run identifier).")
            @QueryParam("createdBy") @Nullable String createdBy,
            @Parameter(description = "Restrict to a comma-separated list of dataset (investigation) ids.")
            @QueryParam("datasetIds") @Nullable String datasetIds,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        AnnotationSetRole roleFilter = parseRoleFilter( role );
        AnnotationSetSource sourceFilter = parseSourceFilter( source );
        List<Long> invIds = parseDatasetIdsOrThrow( datasetIds );
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        List<AnnotationSetSummaryValueObject> vos = annotationSetService.listSummaries(
                roleFilter, sourceFilter, createdBy, invIds, offset, limit );
        long total = annotationSetService.countSummaries( roleFilter, sourceFilter, createdBy, invIds );
        List<AnnotationSetSummaryResponse> rows = new ArrayList<>( vos.size() );
        for ( AnnotationSetSummaryValueObject vo : vos ) {
            rows.add( toSummaryResponse( vo ) );
        }
        Slice<AnnotationSetSummaryResponse> slice = new Slice<>( rows, null, offset, limit, total );
        return Responders.paginate( slice, new String[]{ "role" } );
    }

    /**
     * Single annotation set (full payload).
     */
    @GET
    @Path("/annotation-sets/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Fetch a single annotation set (full payload)")
    public ResponseDataObject<AnnotationSetResponse> getAnnotationSet(
            @PathParam("id") Long id
    ) {
        AnnotationSet a = requireLoad( id, "id" );
        return Responders.respond( toResponse( a ) );
    }

    /**
     * Mark an annotation set finalized. For DRAFT this means "done
     * editing"; for SNAPSHOT this is the curator's bless to mark the
     * row as the polished canonical view. Idempotent: second call
     * returns 200 with no state change.
     */
    @POST
    @Path("/annotation-sets/{id}/finalize")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark an annotation set finalized (done editing / polished)",
            description = "`?onBehalfOf=` records the finalizing curator when the call is relayed "
                    + "by an agent, so `finalizedBy` names the person who decided rather than the "
                    + "software that transmitted it. Honoured only for `GROUP_AGENT` / "
                    + "`GROUP_ADMIN`.")
    public Response finalizeAnnotationSet(
            @PathParam("id") Long id,
            @Parameter(description = "Who is finalizing. Agents and admins only.")
            @QueryParam("onBehalfOf") @Nullable String onBehalfOf
    ) {
        String by = resolveCurator( onBehalfOf );
        AnnotationSet updated = annotationSetService.finalizeSet( id, by );
        if ( updated == null ) {
            throw new NotFoundException( "No annotation set with id " + id );
        }
        return Response.ok( toResponse( updated ) ).build();
    }

    /* ============== triage ============== */

    /**
     * Record or replace the caller's triage ruling on an annotation set.
     *
     * <p>Idempotent per judge: a second call from the same judge updates their
     * standing ruling rather than adding one, so a curator changing their mind
     * leaves one row and not a history.</p>
     */
    @PATCH
    @Path("/annotation-sets/{id}/triage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Rule on how much an annotation set matters",
            description = "`triage` is one of `fine`, `wont_fix`, `might_fix`, `must_fix`. There is no "
                    + "`pending` -- a set nobody has ruled on simply has no triage row, so absence is the "
                    + "state.\n\n"
                    + "One standing ruling per judge; several judges coexist, and the effective verdict is "
                    + "the most recent. `?onBehalfOf=` records the ruling curator when the call is relayed "
                    + "by an agent, and is honoured only for `GROUP_AGENT` / `GROUP_ADMIN`.\n\n"
                    + "🛑 Not the per-finding audit disposition (`accepted` / `dismissed` / ...), which "
                    + "answers whether a curator agrees with one finding. This answers how much the whole "
                    + "set matters, and a finding can be dismissed inside a set ruled `must_fix`.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The ruling was recorded."),
                    @ApiResponse(responseCode = "400", description = "`triage` is missing or names no verdict.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))),
                    @ApiResponse(responseCode = "404", description = "No annotation set with that id.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response triageAnnotationSet(
            @PathParam("id") Long id,
            @Parameter(description = "Who is ruling. Agents and admins only.")
            @QueryParam("onBehalfOf") @Nullable String onBehalfOf,
            @Nullable TriageRequest body
    ) {
        if ( body == null || body.triage == null || body.triage.isBlank() ) {
            throw new BadRequestException( "Request body must include `triage`"
                    + " (fine|wont_fix|might_fix|must_fix)." );
        }
        TriageVerdict verdict;
        try {
            verdict = TriageVerdict.fromDbValue( body.triage );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( e.getMessage() );
        }
        AnnotationSet set = requireLoad( id, "id" );
        String judgedBy = resolveCurator( onBehalfOf );
        // The kind follows the delegation, not the transport. A ruling relayed for a named curator is that
        // curator's; only a caller ruling as itself, and being an agent, produces an AGENT verdict. Backwards,
        // this would make "has a person looked at this" answer false for every ruling a curator made through
        // the agent -- which, now that curation is relayed, is all of them.
        boolean ruledForSomeoneNamed = onBehalfOf != null && !onBehalfOf.isBlank();
        TriageJudgeKind kind = !ruledForSomeoneNamed && SecurityUtil.isUserAgent()
                ? TriageJudgeKind.AGENT : TriageJudgeKind.CURATOR;
        AnnotationSetTriage t = annotationSetTriageService.judge( set, verdict, judgedBy, kind, body.note );
        return Response.ok( toTriageResponse( t ) ).build();
    }

    /**
     * Withdraw the caller's ruling, returning the set to un-triaged if it was
     * the only one. 204 whether or not a row was there -- a withdrawal that
     * finds nothing has still achieved what it asked for.
     */
    @DELETE
    @Path("/annotation-sets/{id}/triage")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(summary = "Withdraw a triage ruling",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Withdrawn, or there was nothing to withdraw."),
                    @ApiResponse(responseCode = "404", description = "No annotation set with that id.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response withdrawTriage(
            @PathParam("id") Long id,
            @Parameter(description = "Whose ruling to withdraw. Agents and admins only.")
            @QueryParam("onBehalfOf") @Nullable String onBehalfOf
    ) {
        AnnotationSet set = requireLoad( id, "id" );
        annotationSetTriageService.withdraw( set, resolveCurator( onBehalfOf ) );
        return Response.noContent().build();
    }

    /**
     * Every ruling on an annotation set, most recent first. The head of the
     * list is the effective verdict.
     */
    @GET
    @Path("/annotation-sets/{id}/triage")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Read the triage rulings on an annotation set",
            description = "Most recent first, so the head of the list is the effective verdict. An empty "
                    + "list means nobody has ruled.")
    public Response getTriage( @PathParam("id") Long id ) {
        AnnotationSet set = requireLoad( id, "id" );
        List<TriageResponse> rows = new ArrayList<>();
        for ( AnnotationSetTriage t : annotationSetTriageService.findBySet( set ) ) {
            rows.add( toTriageResponse( t ) );
        }
        return Response.ok( rows ).build();
    }

    private static TriageResponse toTriageResponse( AnnotationSetTriage t ) {
        TriageResponse r = new TriageResponse();
        r.id = t.getId();
        r.annotationSetId = t.getAnnotationSet() != null ? t.getAnnotationSet().getId() : null;
        r.triage = t.getVerdict() != null ? t.getVerdict().getDbValue() : null;
        r.judgedBy = t.getJudgedBy();
        r.judgeKind = t.getJudgeKind() != null ? t.getJudgeKind().getDbValue() : null;
        r.judgedAt = t.getJudgedAt();
        r.note = t.getNote();
        return r;
    }

    /** Body for {@link #triageAnnotationSet}. */
    public static class TriageRequest {
        @JsonProperty("triage")
        public String triage;
        @JsonProperty("note")
        public String note;
    }

    /** Wire shape of one triage ruling. */
    public static class TriageResponse {
        public Long id;
        @JsonProperty("annotationSetId")
        public Long annotationSetId;
        public String triage;
        @JsonProperty("judgedBy")
        public String judgedBy;
        @JsonProperty("judgeKind")
        public String judgeKind;
        @JsonProperty("judgedAt")
        public Date judgedAt;
        public String note;
    }

    /**
     * Clear finalized status on an annotation set. Idempotent.
     */
    @POST
    @Path("/annotation-sets/{id}/reopen")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clear finalized status on an annotation set")
    public Response reopenAnnotationSet(
            @PathParam("id") Long id
    ) {
        AnnotationSet updated = annotationSetService.reopenSet( id );
        if ( updated == null ) {
            throw new NotFoundException( "No annotation set with id " + id );
        }
        return Response.ok( toResponse( updated ) ).build();
    }

    /**
     * Delete an annotation set by id. Descendants survive with their
     * {@code parent} cleared via FK {@code ON DELETE SET NULL}.
     */
    @DELETE
    @Path("/annotation-sets/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Delete an annotation set by id",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Annotation set deleted."),
                    @ApiResponse(responseCode = "404", description = "No annotation set with that id.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public Response deleteAnnotationSet(
            @PathParam("id") Long id
    ) {
        boolean removed = annotationSetService.delete( id );
        if ( !removed ) {
            throw new NotFoundException( "No annotation set with id " + id );
        }
        return Response.noContent().build();
    }

    /* ============== request / response shapes ============== */

    /**
     * Body for {@link #submitAnnotationSet}.
     */
    public static class AnnotationSetRequest {
        @JsonProperty("role")
        public String role;
        @JsonProperty("source")
        @Nullable
        public String source;
        @JsonProperty("kind")
        @Nullable
        public String kind;
        @JsonProperty("runId")
        @JsonAlias("run_id")
        @Nullable
        public String runId;
        @JsonProperty("createdBy")
        @JsonAlias("created_by")
        @Nullable
        public String createdBy;
        @JsonProperty("agentVersion")
        @JsonAlias("agent_version")
        @Nullable
        public String agentVersion;
        @JsonProperty("model")
        @Nullable
        public String model;
        /**
         * The producing repository's git head sha. Not redundant with {@link #model} — behaviour differs
         * between shas at one model, so the model alone does not identify the build.
         */
        @JsonProperty("runSha")
        @JsonAlias("run_sha")
        @Nullable
        public String runSha;
        /** Which specialist produced it ({@code cell_type}, {@code disease}, …); "the agent" is a fleet. */
        @JsonProperty("agentName")
        @JsonAlias("agent_name")
        @Nullable
        public String agentName;
        @JsonProperty("ranAt")
        @JsonAlias("ran_at")
        @Nullable
        public Date ranAt;
        @JsonProperty("payloadJson")
        @JsonAlias("payload_json")
        @Nullable
        public String payloadJson;
        @JsonProperty("parentId")
        @JsonAlias("parent_id")
        @Nullable
        public Long parentId;
    }

    /**
     * Body for {@link #upsertDraftForDataset}.
     */
    public static class UpsertDraftRequest {
        @JsonProperty("payloadJson")
        @JsonAlias("payload_json")
        public String payloadJson;
        @JsonProperty("parkedElements")
        @JsonAlias("parked_elements")
        @Nullable
        public String parkedElements;
        @JsonProperty("parentId")
        @JsonAlias("parent_id")
        @Nullable
        public Long parentId;
    }

    /**
     * Full-payload response shape.
     */
    public static class AnnotationSetResponse {
        @JsonProperty("id")
        public Long id;
        @JsonProperty("datasetId")
        public Long datasetId;
        @JsonProperty("role")
        public String role;
        @JsonProperty("source")
        public String source;
        @JsonProperty("kind")
        @Nullable
        public String kind;
        @JsonProperty("runId")
        public String runId;
        @JsonProperty("parentId")
        @Nullable
        public Long parentId;
        @JsonProperty("createdBy")
        @Nullable
        public String createdBy;
        @JsonProperty("createdAt")
        public Date createdAt;
        @JsonProperty("updatedAt")
        public Date updatedAt;
        @JsonProperty("finalizedAt")
        @Nullable
        public Date finalizedAt;
        @JsonProperty("finalizedBy")
        @Nullable
        public String finalizedBy;
        @JsonProperty("agentVersion")
        @Nullable
        public String agentVersion;
        @JsonProperty("model")
        @Nullable
        public String model;
        /** Round-tripped so a trace does not stop at Gemma's boundary; see the request DTO for why it matters. */
        @JsonProperty("runSha")
        @Nullable
        public String runSha;
        @JsonProperty("agentName")
        @Nullable
        public String agentName;
        @JsonProperty("ranAt")
        @Nullable
        public Date ranAt;
        @JsonProperty("payloadJson")
        @Nullable
        public String payloadJson;
        @JsonProperty("parkedElements")
        @Nullable
        public String parkedElements;
    }

    /**
     * Thin metadata response shape (no payloadJson).
     */
    public static class AnnotationSetSummaryResponse {
        @JsonProperty("id")
        public Long id;
        @JsonProperty("datasetId")
        public Long datasetId;
        @JsonProperty("role")
        public String role;
        @JsonProperty("source")
        public String source;
        @JsonProperty("kind")
        @Nullable
        public String kind;
        @JsonProperty("runId")
        public String runId;
        @JsonProperty("parentId")
        @Nullable
        public Long parentId;
        @JsonProperty("createdBy")
        @Nullable
        public String createdBy;
        @JsonProperty("createdAt")
        public Date createdAt;
        @JsonProperty("updatedAt")
        public Date updatedAt;
        @JsonProperty("finalizedAt")
        @Nullable
        public Date finalizedAt;
        @JsonProperty("finalizedBy")
        @Nullable
        public String finalizedBy;
        @JsonProperty("agentVersion")
        @Nullable
        public String agentVersion;
        @JsonProperty("model")
        @Nullable
        public String model;
        /**
         * Present on the thin shape too: the cross-experiment {@code role=commit} listing is the
         * "which agent applied this, from which build" query, and answering it must not require an
         * N+1 into the full endpoint for the two fields that carry the answer.
         */
        @JsonProperty("runSha")
        @Nullable
        public String runSha;
        @JsonProperty("agentName")
        @Nullable
        public String agentName;
        @JsonProperty("ranAt")
        @Nullable
        public Date ranAt;
        @JsonProperty("payloadSize")
        @Nullable
        public Long payloadSize;
    }

    /* ============== helpers ============== */

    private AnnotationSet requireLoad( Long id, String label ) {
        if ( id == null ) {
            throw new BadRequestException( label + " is required." );
        }
        AnnotationSet a = annotationSetService.load( id );
        if ( a == null ) {
            throw new NotFoundException( "No annotation set with " + label + " " + id );
        }
        return a;
    }

    private User requireCurrentUser() {
        User u = userManager.getCurrentUser();
        if ( u == null ) {
            throw new BadRequestException( "No authenticated user resolved." );
        }
        return u;
    }

    private static String draftRunId( String curatorUserName ) {
        return "draft-" + curatorUserName;
    }

    private static AnnotationSetSource defaultSourceForRole( AnnotationSetRole role ) {
        switch ( role ) {
            case DRAFT:
                return AnnotationSetSource.CURATOR;
            case PROPOSAL:
            case SNAPSHOT:
            case COMMIT:
            default:
                return AnnotationSetSource.AGENT;
        }
    }

    private static AnnotationSetRole parseRoleOrThrow( @Nullable String role, String emptyMsg ) {
        if ( role == null || role.trim().isEmpty() ) {
            throw new BadRequestException( emptyMsg );
        }
        try {
            return AnnotationSetRole.fromDbValue( role.trim() );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown role: " + role
                    + " (expected 'proposal', 'draft', 'snapshot', or 'commit')" );
        }
    }

    @Nullable
    private static AnnotationSetRole parseRoleFilter( @Nullable String role ) {
        if ( role == null || role.isEmpty() || "all".equalsIgnoreCase( role ) ) {
            return null;
        }
        try {
            return AnnotationSetRole.fromDbValue( role );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown role: " + role
                    + " (expected 'proposal', 'draft', 'snapshot', 'commit', or 'all')" );
        }
    }

    private static AnnotationSetSource parseSourceOrThrow( @Nullable String source,
            AnnotationSetSource fallback ) {
        if ( source == null || source.trim().isEmpty() ) {
            return fallback;
        }
        try {
            return AnnotationSetSource.fromDbValue( source.trim() );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown source: " + source
                    + " (expected 'agent', 'curator', 'gemma_intake', or 'external_import')" );
        }
    }

    @Nullable
    private static AnnotationSetSource parseSourceFilter( @Nullable String source ) {
        if ( source == null || source.isEmpty() || "all".equalsIgnoreCase( source ) ) {
            return null;
        }
        try {
            return AnnotationSetSource.fromDbValue( source );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown source: " + source );
        }
    }

    @Nullable
    private static AgentCurationKind parseKindOrThrow( @Nullable String kind,
            @Nullable AgentCurationKind fallback ) {
        if ( kind == null || kind.trim().isEmpty() ) {
            return fallback;
        }
        try {
            return AgentCurationKind.fromDbValue( kind.trim() );
        } catch ( IllegalArgumentException e ) {
            throw new BadRequestException( "Unknown kind: " + kind
                    + " (expected 'proposal' or 'audit')" );
        }
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
                throw new BadRequestException(
                        "datasetIds must be a comma-separated list of integers; got: " + t );
            }
        }
        return ids.isEmpty() ? null : ids;
    }

    private static boolean parseShapeIsMeta( @Nullable String shape ) {
        if ( shape == null || shape.isEmpty() || "full".equalsIgnoreCase( shape ) ) {
            return false;
        }
        if ( "meta".equalsIgnoreCase( shape ) ) {
            return true;
        }
        throw new BadRequestException( "Unknown shape: " + shape + " (expected 'meta' or 'full')" );
    }

    private static AnnotationSetResponse toResponse( AnnotationSet a ) {
        AnnotationSetResponse r = new AnnotationSetResponse();
        r.id = a.getId();
        r.datasetId = a.getInvestigation() != null ? a.getInvestigation().getId() : null;
        r.role = a.getRole() != null ? a.getRole().getDbValue() : null;
        r.source = a.getSource() != null ? a.getSource().getDbValue() : null;
        r.kind = a.getKind() != null ? a.getKind().getDbValue() : null;
        r.runId = a.getRunId();
        r.parentId = a.getParent() != null ? a.getParent().getId() : null;
        r.createdBy = a.getCreatedBy();
        r.createdAt = a.getCreatedAt();
        r.updatedAt = a.getUpdatedAt();
        r.finalizedAt = a.getFinalizedAt();
        r.finalizedBy = a.getFinalizedBy();
        r.agentVersion = a.getAgentVersion();
        r.model = a.getModel();
        r.runSha = a.getRunSha();
        r.agentName = a.getAgentName();
        r.ranAt = a.getRanAt();
        r.payloadJson = a.getPayloadJson();
        r.parkedElements = a.getParkedElements();
        return r;
    }

    private static AnnotationSetSummaryResponse toSummaryResponse( AnnotationSetSummaryValueObject s ) {
        AnnotationSetSummaryResponse r = new AnnotationSetSummaryResponse();
        r.id = s.getId();
        r.datasetId = s.getInvestigationId();
        r.role = s.getRole() != null ? s.getRole().getDbValue() : null;
        r.source = s.getSource() != null ? s.getSource().getDbValue() : null;
        r.kind = s.getKind() != null ? s.getKind().getDbValue() : null;
        r.runId = s.getRunId();
        r.parentId = s.getParentId();
        r.createdBy = s.getCreatedBy();
        r.createdAt = s.getCreatedAt();
        r.updatedAt = s.getUpdatedAt();
        r.finalizedAt = s.getFinalizedAt();
        r.finalizedBy = s.getFinalizedBy();
        r.agentVersion = s.getAgentVersion();
        r.model = s.getModel();
        r.runSha = s.getRunSha();
        r.agentName = s.getAgentName();
        r.ranAt = s.getRanAt();
        r.payloadSize = s.getPayloadSize();
        return r;
    }
}
