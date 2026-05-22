/*
 * The Gemma project
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDraft;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDraftDispositions;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CurationDraftService;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST surface for the unified {@link CurationDraft} buffer.
 *
 * <p>Two URL families resolve to the same entity:</p>
 * <ul>
 *   <li>{@code /datasets/{id}/draft*} — the curator's draft viewed by
 *       investigation. PUT upserts, DELETE discards, POST .../finalize
 *       stamps {@code finalizedAt}, POST .../commit attempts the apply
 *       chain (currently 501 with pointer — see commit handler doc).</li>
 *   <li>{@code /drafts}, {@code /drafts/inflight} — the curator's drafts
 *       across all EEs.</li>
 *   <li>{@code /proposals/{proposalId}/reviews*} — the same drafts viewed
 *       through the proposal that seeded them. PATCH accepts partial
 *       updates (payload-only or parked-only).</li>
 * </ul>
 *
 * <p>The {@code reviewer} path segment in the {@code /proposals/...}
 * family must resolve to the current user unless the caller holds
 * {@code GROUP_ADMIN} — anyone else gets a 403.</p>
 *
 * <p>Draft state changes are NOT audited; the commit step is where typed
 * audit events are emitted (via the existing design / annotation write
 * endpoints).</p>
 */
@Service
@Hidden
@Path("/")
@Tag(name = "Drafts", description = "Per-curator curation draft buffer")
public class DraftsWebService {

    /**
     * Mapper used to round-trip the finalisation request body. Reused
     * statically — Jackson's ObjectMapper is thread-safe once configured.
     */
    private static final ObjectMapper FINALISATION_MAPPER = new ObjectMapper();

    private final CurationDraftService curationDraftService;
    private final AgentProposalService agentProposalService;
    private final UserManager userManager;
    private final UserReadService userReadService;

    @Autowired
    public DraftsWebService( CurationDraftService curationDraftService,
            AgentProposalService agentProposalService,
            UserManager userManager,
            UserReadService userReadService ) {
        this.curationDraftService = curationDraftService;
        this.agentProposalService = agentProposalService;
        this.userManager = userManager;
        this.userReadService = userReadService;
    }

    /* ============== /datasets/{id}/draft* family ============== */

    /**
     * Fetch this curator's draft for the given investigation. 404 if none.
     */
    @GET
    @Hidden
    @Path("/datasets/{id}/draft")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(hidden = true, summary = "Fetch the current curator's draft for a dataset")
    public DraftResponse getDraftForDataset( @PathParam("id") Long id ) {
        User curator = requireCurrentUser();
        return curationDraftService.findForCurator( id, curator )
                .map( this::toDraftResponse )
                .orElseThrow( () -> new NotFoundException( "No draft for dataset " + id ) );
    }

    /**
     * Upsert this curator's draft. Body: {@link UpsertDraftRequest}.
     */
    @PUT
    @Hidden
    @Path("/datasets/{id}/draft")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true, summary = "Save / update the current curator's draft for a dataset",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated.", content = @Content()),
                    @ApiResponse(responseCode = "201", description = "Created.", content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Missing payload.", content = @Content())
            })
    public Response upsertDraftForDataset( @PathParam("id") Long id,
            @Nullable UpsertDraftRequest req ) {
        if ( req == null || req.payloadJson == null ) {
            throw new BadRequestException( "Request body must include `payload_json`." );
        }
        User curator = requireCurrentUser();
        boolean isNew = curationDraftService.findForCurator( id, curator ).isEmpty();
        CurationDraft d = curationDraftService.saveOrUpdate( id, curator, req.payloadJson,
                req.proposalId, req.parkedElementsJson );
        Response.Status status = isNew ? Response.Status.CREATED : Response.Status.OK;
        return Response.status( status ).entity( toDraftResponse( d ) ).build();
    }

    /**
     * Discard this curator's draft for a dataset.
     */
    @DELETE
    @Hidden
    @Path("/datasets/{id}/draft")
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true, summary = "Discard the current curator's draft for a dataset")
    public Response deleteDraftForDataset( @PathParam("id") Long id ) {
        User curator = requireCurrentUser();
        curationDraftService.delete( id, curator );
        return Response.noContent().build();
    }

    /**
     * Stamp {@code finalizedAt}. Lighter than commit: row stays intact.
     */
    @POST
    @Hidden
    @Path("/datasets/{id}/draft/finalize")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true, summary = "Stamp finalizedAt on the current curator's draft",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Finalized.", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "No draft to finalize.", content = @Content())
            })
    public DraftResponse finalizeDraftForDataset( @PathParam("id") Long id ) {
        User curator = requireCurrentUser();
        try {
            return toDraftResponse( curationDraftService.finalize( id, curator ) );
        } catch ( IllegalStateException e ) {
            throw new NotFoundException( e.getMessage() );
        }
    }

    /**
     * Commit the draft contents to the dataset. Currently returns 501 with a
     * pointer to the design / annotation write endpoints — server-side
     * commit dispatch is deferred (the curation-UI applies the draft
     * contents through the existing {@code PUT /datasets/{id}/design} and
     * annotation endpoints; this handler is here as the formal commit-point
     * the UI calls after those succeed, so the draft row can be deleted).
     *
     * <p>The deferred behavior follows the same pattern as
     * {@code PreboardedWebService.promotePreboarded}'s
     * {@code apply_latest_proposal} flag: accept the call, document the
     * gap, return a structured response the UI can react to.</p>
     */
    @POST
    @Hidden
    @Path("/datasets/{id}/draft/commit")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true,
            summary = "Commit the draft (deferred: 501 pointer to design/annotation endpoints)")
    public Response commitDraftForDataset( @PathParam("id") Long id ) {
        User curator = requireCurrentUser();
        Optional<CurationDraft> maybe = curationDraftService.findForCurator( id, curator );
        if ( maybe.isEmpty() ) {
            throw new NotFoundException( "No draft to commit for dataset " + id );
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put( "error", "Server-side commit dispatch deferred" );
        body.put( "draft_id", maybe.get().getId() );
        body.put( "redirect_to", List.of(
                "PUT /datasets/" + id + "/design",
                "PUT /datasets/" + id + "/annotations" ) );
        body.put( "after_dispatch", "DELETE /datasets/" + id + "/draft to clear the buffer" );
        return Response.status( Response.Status.NOT_IMPLEMENTED ).entity( body ).build();
    }

    /**
     * Finalisation submission endpoint. The curation-UI sends the curator's
     * edited proposal payload plus the list of element keys the curator
     * parked. Server overwrites the draft's {@code payloadJson} +
     * {@code parkedElements}, then diff-derives the per-element disposition
     * map against the original {@code proposalSnapshotJson} captured at
     * seed-time.
     *
     * <p>Distinct from {@code PUT /datasets/{id}/draft}: that endpoint takes
     * raw JSON strings and returns the full draft row. This one takes
     * structured JSON ({@code payload} as an object, {@code parked_elements}
     * as a string array) and returns only the dispositions, translated to
     * the curator wire vocab ({@code accepted}/{@code accepted_with_edits}/
     * {@code rejected}/{@code parked}).</p>
     *
     * <p>PUT semantics (full replace of the draft's payload + parked list);
     * see {@link FinalisationRequest}.</p>
     */
    @PUT
    @Hidden
    @Path("/datasets/{id}/curation-draft")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true,
            summary = "Finalisation submit: replace draft payload + parked list, get dispositions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Saved.", content = @Content()),
                    @ApiResponse(responseCode = "400", description = "Malformed body.", content = @Content()),
                    @ApiResponse(responseCode = "404", description = "Unknown dataset.", content = @Content())
            })
    public FinalisationResponse submitFinalisedDraft( @PathParam("id") Long id,
            @Nullable FinalisationRequest req ) {
        if ( req == null || req.payload == null ) {
            throw new BadRequestException( "Request body must include `payload`." );
        }
        User curator = requireCurrentUser();
        String payloadJson;
        String parkedJson;
        try {
            payloadJson = FINALISATION_MAPPER.writeValueAsString( req.payload );
            List<String> parked = req.parkedElements != null ? req.parkedElements : Collections.emptyList();
            parkedJson = FINALISATION_MAPPER.writeValueAsString( parked );
        } catch ( JsonProcessingException e ) {
            throw new BadRequestException( "Malformed JSON in request body: " + e.getOriginalMessage() );
        }
        CurationDraft d;
        try {
            d = curationDraftService.saveOrUpdate( id, curator, payloadJson, null, parkedJson );
        } catch ( IllegalArgumentException e ) {
            // saveOrUpdate throws IAE when the investigation id is unknown.
            throw new NotFoundException( e.getMessage() );
        }
        Map<String, CurationDraftDispositions.Disposition> derived =
                CurationDraftDispositions.derive( d );
        FinalisationResponse out = new FinalisationResponse();
        out.draftId = d.getId();
        out.dispositions = new LinkedHashMap<>();
        for ( Map.Entry<String, CurationDraftDispositions.Disposition> e : derived.entrySet() ) {
            out.dispositions.put( e.getKey(), toWireVocab( e.getValue() ) );
        }
        return out;
    }

    /* ============== /drafts family ============== */

    /**
     * List the current curator's drafts. Pagination + optional
     * {@code since=<epoch ms>} bound.
     */
    @GET
    @Hidden
    @Path("/drafts")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(hidden = true, summary = "List the current curator's drafts")
    public List<DraftResponse> listMyDrafts(
            @QueryParam("since") @Nullable Long sinceEpochMs,
            @QueryParam("offset") @Nullable Integer offset,
            @QueryParam("limit") @Nullable Integer limit ) {
        User curator = requireCurrentUser();
        Date since = sinceEpochMs == null ? null : new Date( sinceEpochMs );
        int o = offset == null ? 0 : offset;
        int l = limit == null ? 50 : limit;
        return curationDraftService.findByCurator( curator, since, o, l ).stream()
                .map( this::toDraftResponse )
                .collect( Collectors.toList() );
    }

    /**
     * List in-flight (un-finalized) drafts owned by the current curator.
     * Currently a thin alias for {@code /drafts} — finalized-vs-in-flight
     * filtering is done in the response payload (callers filter on
     * {@code finalized_at IS NULL} client-side). A dedicated DAO method
     * can land if call volume warrants it.
     */
    @GET
    @Hidden
    @Path("/drafts/inflight")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(hidden = true, summary = "List the current curator's un-finalized drafts")
    public List<DraftResponse> listMyInflightDrafts() {
        User curator = requireCurrentUser();
        return curationDraftService.findByCurator( curator, null, 0, 0 ).stream()
                .filter( d -> d.getFinalizedAt() == null )
                .map( this::toDraftResponse )
                .collect( Collectors.toList() );
    }

    /* ============== /proposals/{id}/reviews* family ============== */

    /**
     * All drafts (one per reviewer) linked to a given proposal.
     */
    @GET
    @Hidden
    @Path("/proposals/{proposalId}/reviews")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(hidden = true, summary = "List drafts linked to a proposal, one per reviewer")
    public List<DraftResponse> listReviewsOfProposal( @PathParam("proposalId") Long proposalId ) {
        // Existence check: 404 when proposal not known.
        AgentProposal p = agentProposalService.load( proposalId );
        if ( p == null ) {
            throw new NotFoundException( "No proposal with id " + proposalId );
        }
        return curationDraftService.findByProposal( proposalId ).stream()
                .map( this::toDraftResponse )
                .collect( Collectors.toList() );
    }

    /**
     * A single curator's review of a proposal. Returns the draft plus the
     * derived disposition map. {@code reviewer} is a user id; it must
     * resolve to the current user unless the caller holds
     * {@code GROUP_ADMIN}.
     */
    @GET
    @Hidden
    @Path("/proposals/{proposalId}/reviews/{reviewer}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(hidden = true, summary = "Fetch one curator's review of a proposal")
    public ReviewResponse getReviewOfProposal( @PathParam("proposalId") Long proposalId,
            @PathParam("reviewer") Long reviewerId ) {
        AgentProposal p = agentProposalService.load( proposalId );
        if ( p == null ) {
            throw new NotFoundException( "No proposal with id " + proposalId );
        }
        User reviewer = resolveReviewer( reviewerId );
        Long invId = p.getInvestigation() != null ? p.getInvestigation().getId() : null;
        if ( invId == null ) {
            // Proposal lacks an investigation — degenerate.
            throw new NotFoundException( "Proposal " + proposalId + " is not bound to an investigation." );
        }
        CurationDraft d = curationDraftService.findForCurator( invId, reviewer )
                .orElseThrow( () -> new NotFoundException(
                        "No review by user " + reviewerId + " for proposal " + proposalId ) );
        return toReviewResponse( d );
    }

    /**
     * Upsert one curator's review. Partial body: any of {@code payloadJson}
     * and {@code parkedElementsJson} may be omitted (server preserves the
     * existing value). When the draft does NOT yet exist, {@code payloadJson}
     * is required to seed it.
     */
    @PATCH
    @Hidden
    @Path("/proposals/{proposalId}/reviews/{reviewer}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true, summary = "Upsert one curator's review of a proposal (partial body)")
    public ReviewResponse patchReviewOfProposal( @PathParam("proposalId") Long proposalId,
            @PathParam("reviewer") Long reviewerId,
            @Nullable PatchReviewRequest req ) {
        if ( req == null ) {
            throw new BadRequestException( "Request body is required." );
        }
        AgentProposal p = agentProposalService.load( proposalId );
        if ( p == null ) {
            throw new NotFoundException( "No proposal with id " + proposalId );
        }
        User reviewer = resolveReviewer( reviewerId );
        Long invId = p.getInvestigation() != null ? p.getInvestigation().getId() : null;
        if ( invId == null ) {
            throw new NotFoundException( "Proposal " + proposalId + " is not bound to an investigation." );
        }
        Optional<CurationDraft> existing = curationDraftService.findForCurator( invId, reviewer );
        // If new and no payloadJson supplied, seed via seedFromProposal so
        // the snapshot baseline gets captured from the proposal payload.
        if ( existing.isEmpty() ) {
            String seedPayload = req.payloadJson != null ? req.payloadJson
                    : p.getPayloadJson() != null ? p.getPayloadJson() : "{}";
            CurationDraft seeded = curationDraftService.seedFromProposal( invId, reviewer, p, seedPayload );
            // Apply parkedElementsJson on the same turn if supplied.
            if ( req.parkedElementsJson != null ) {
                seeded = curationDraftService.saveOrUpdate( invId, reviewer,
                        seeded.getPayloadJson(), p.getId(), req.parkedElementsJson );
            }
            return toReviewResponse( seeded );
        }
        // Existing — only mutate what was supplied. payloadJson omitted ->
        // re-use the current payload (so saveOrUpdate's not-null contract
        // is satisfied).
        String payloadJson = req.payloadJson != null ? req.payloadJson
                : existing.get().getPayloadJson();
        CurationDraft d = curationDraftService.saveOrUpdate( invId, reviewer,
                payloadJson, p.getId(), req.parkedElementsJson );
        return toReviewResponse( d );
    }

    /**
     * Stamp {@code finalizedAt} on a curator's review, resolved via the
     * proposal's investigation.
     */
    @POST
    @Hidden
    @Path("/proposals/{proposalId}/reviews/{reviewer}/finalize")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true, summary = "Stamp finalizedAt on a curator's review")
    public ReviewResponse finalizeReviewOfProposal( @PathParam("proposalId") Long proposalId,
            @PathParam("reviewer") Long reviewerId ) {
        AgentProposal p = agentProposalService.load( proposalId );
        if ( p == null ) {
            throw new NotFoundException( "No proposal with id " + proposalId );
        }
        User reviewer = resolveReviewer( reviewerId );
        Long invId = p.getInvestigation() != null ? p.getInvestigation().getId() : null;
        if ( invId == null ) {
            throw new NotFoundException( "Proposal " + proposalId + " is not bound to an investigation." );
        }
        try {
            return toReviewResponse( curationDraftService.finalize( invId, reviewer ) );
        } catch ( IllegalStateException e ) {
            throw new NotFoundException( e.getMessage() );
        }
    }

    /* ============== helpers ============== */

    private User requireCurrentUser() {
        User u = userManager.getCurrentUser();
        if ( u == null ) {
            throw new BadRequestException( "No authenticated user resolved." );
        }
        return u;
    }

    /**
     * {@code reviewerId} must match the current user unless the caller has
     * {@code GROUP_ADMIN}. Admins get read/write access to any curator's
     * drafts.
     */
    private User resolveReviewer( Long reviewerId ) {
        User current = requireCurrentUser();
        if ( reviewerId == null ) {
            throw new BadRequestException( "reviewer id is required." );
        }
        if ( reviewerId.equals( current.getId() ) ) {
            return current;
        }
        if ( !hasAdminAuthority() ) {
            throw new ForbiddenException(
                    "Curators can only access their own reviews; admin authority required for cross-curator access." );
        }
        User resolved = userReadService.load( reviewerId );
        if ( resolved == null ) {
            throw new NotFoundException( "No user with id " + reviewerId );
        }
        return resolved;
    }

    private boolean hasAdminAuthority() {
        org.springframework.security.core.Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        if ( auth == null ) return false;
        for ( GrantedAuthority a : auth.getAuthorities() ) {
            if ( "GROUP_ADMIN".equals( a.getAuthority() ) ) return true;
        }
        return false;
    }

    private DraftResponse toDraftResponse( CurationDraft d ) {
        DraftResponse r = new DraftResponse();
        r.draftId = d.getId();
        r.investigationId = d.getInvestigation() != null ? d.getInvestigation().getId() : null;
        r.curatorId = d.getCurator() != null ? d.getCurator().getId() : null;
        r.payloadJson = d.getPayloadJson();
        r.proposalId = d.getProposal() != null ? d.getProposal().getId() : null;
        r.proposalSnapshotJson = d.getProposalSnapshotJson();
        r.parkedElementsJson = d.getParkedElements();
        r.startedAt = d.getStartedAt();
        r.lastEditedAt = d.getLastEditedAt();
        r.finalizedAt = d.getFinalizedAt();
        return r;
    }

    /**
     * Translate a domain {@link CurationDraftDispositions.Disposition} to
     * the curator-facing wire string. Lives at the REST boundary so the
     * domain enum stays unchanged — only the wire shape uses the curator
     * vocab.
     */
    static String toWireVocab( CurationDraftDispositions.Disposition d ) {
        switch ( d ) {
            case RETAINED:
                return "accepted";
            case EDITED:
                return "accepted_with_edits";
            case REJECTED:
                return "rejected";
            case PARKED:
                return "parked";
            default:
                throw new IllegalStateException( "Unhandled disposition: " + d );
        }
    }

    private ReviewResponse toReviewResponse( CurationDraft d ) {
        ReviewResponse r = new ReviewResponse();
        r.draft = toDraftResponse( d );
        // Map<String, Disposition> serializes as JSON object of "key" -> "ENUM_NAME".
        r.dispositions = CurationDraftDispositions.derive( d );
        return r;
    }

    /* ===================== DTOs ===================== */

    /** Body of PUT /datasets/{id}/draft. */
    public static class UpsertDraftRequest {
        @JsonProperty("payload_json")
        public String payloadJson;
        @JsonProperty("proposal_id")
        @Nullable
        public Long proposalId;
        @JsonProperty("parked_elements_json")
        @Nullable
        public String parkedElementsJson;
    }

    /** Body of PATCH /proposals/{id}/reviews/{reviewer}. */
    public static class PatchReviewRequest {
        @JsonProperty("payload_json")
        @Nullable
        public String payloadJson;
        @JsonProperty("parked_elements_json")
        @Nullable
        public String parkedElementsJson;
    }

    /** Single-draft response shape. */
    public static class DraftResponse {
        @JsonProperty("draft_id")
        public Long draftId;
        @JsonProperty("investigation_id")
        public Long investigationId;
        @JsonProperty("curator_id")
        public Long curatorId;
        @JsonProperty("payload_json")
        public String payloadJson;
        @JsonProperty("proposal_id")
        @Nullable
        public Long proposalId;
        @JsonProperty("proposal_snapshot_json")
        @Nullable
        public String proposalSnapshotJson;
        @JsonProperty("parked_elements_json")
        @Nullable
        public String parkedElementsJson;
        @JsonProperty("started_at")
        public Date startedAt;
        @JsonProperty("last_edited_at")
        public Date lastEditedAt;
        @JsonProperty("finalized_at")
        @Nullable
        public Date finalizedAt;
    }

    /** Draft + derived disposition map (one wire object for the review GET). */
    public static class ReviewResponse {
        public DraftResponse draft;
        public Map<String, CurationDraftDispositions.Disposition> dispositions;
    }

    /**
     * Body of PUT /datasets/{id}/curation-draft.
     *
     * <p>{@code payload} is the curator's edited proposal envelope as a
     * structured JSON object (server serialises it back into the draft's
     * {@code payloadJson} string column). {@code parked_elements} is the
     * list of element keys the curator chose to defer.</p>
     */
    public static class FinalisationRequest {
        @JsonProperty("payload")
        public JsonNode payload;

        @JsonProperty("parked_elements")
        @Nullable
        public List<String> parkedElements;
    }

    /**
     * Response of PUT /datasets/{id}/curation-draft. Carries the persisted
     * draft id plus the per-element dispositions in the curator wire vocab
     * ({@code accepted}/{@code accepted_with_edits}/{@code rejected}/
     * {@code parked}).
     */
    public static class FinalisationResponse {
        @JsonProperty("draft_id")
        public Long draftId;

        @JsonProperty("dispositions")
        public Map<String, String> dispositions;
    }
}
