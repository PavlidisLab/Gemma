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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationNoteUpdateEvent;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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

import static ubic.gemma.rest.util.Responders.respond;

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
 *   <li>{@code POST /datasets/{id}/audits} — curator manual audit submission (creates a
 *       {@link CurationNoteUpdateEvent} audit event with the supplied note + detail).</li>
 *   <li>{@code POST /datasets/{id}/curation-proposals} — attach an {@code AgentProposal} to a loaded EE
 *       (consolidated with the skeleton path's surface per STATUS_CURATION_PROPOSALS.md).</li>
 *   <li>{@code GET  /datasets/{id}/curation-proposals} — list proposals attached to a loaded EE,
 *       newest first.</li>
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
    private AuditTrailService auditTrailService;

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
     * Request body for {@link #submitAudit}. {@code note} is short text; {@code detail} is optional long-form.
     */
    public static class AuditSubmissionRequest {
        @Nullable
        private String note;
        @Nullable
        private String detail;

        @Nullable
        public String getNote() {
            return note;
        }

        public void setNote( @Nullable String note ) {
            this.note = note;
        }

        @Nullable
        public String getDetail() {
            return detail;
        }

        public void setDetail( @Nullable String detail ) {
            this.detail = detail;
        }
    }

    /**
     * Submit a curator audit on a dataset. Creates a {@link CurationNoteUpdateEvent} audit event with the supplied
     * note/detail. The persistent storage is the existing audit-trail table; no new entity is needed.
     */
    @POST
    @Hidden
    @Path("/datasets/{dataset}/audits")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true)
    public ResponseDataObject<AuditEventValueObject> submitAudit(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable AuditSubmissionRequest body
    ) {
        if ( body == null || body.getNote() == null || body.getNote().trim().isEmpty() ) {
            throw new BadRequestException( "Request body must include a non-blank `note`." );
        }
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        //noinspection deprecation
        AuditEvent event = auditTrailService.addUpdateEvent( ee, CurationNoteUpdateEvent.class,
                body.getNote().trim(), body.getDetail() );
        return respond( new AuditEventValueObject( event ) );
    }

    /**
     * Request body for {@link #submitCurationProposal}. Mirrors
     * {@link SkeletonsWebService.AttachProposalRequest} so the two endpoints accept the same payload shape — the
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
    }

    /**
     * Response of {@link #submitCurationProposal} and per-row of {@link #listCurationProposals}.
     */
    public static class CurationProposalResponse {
        @JsonProperty("proposal_id")
        public Long proposalId;
        @JsonProperty("dataset_id")
        public Long datasetId;
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
     * Attach an {@link AgentProposal} to a loaded EE. Idempotent on {@code run_id}: a retry returns the existing
     * row as 200 OK rather than 201 Created. The underlying service is the same one the public
     * {@link SkeletonsWebService#attachProposal} uses; this endpoint just provides a dataset-centric URL pattern
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
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        AgentProposalService.AttachedProposal attached = agentProposalService.attach( ee,
                body.runId.trim(), body.agentVersion, body.model, body.ranAt, body.payloadJson );
        CurationProposalResponse resp = toProposalResponse( attached.getProposal(), ee.getId() );
        Response.Status status = attached.isCreated()
                ? Response.Status.CREATED
                : Response.Status.OK;
        return Response.status( status ).entity( resp ).build();
    }

    /**
     * List {@link AgentProposal}s attached to a loaded EE, newest first.
     */
    @GET
    @Hidden
    @Path("/datasets/{dataset}/curation-proposals")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_CURATOR') or hasAuthority('GROUP_ADMIN') or hasAuthority('GROUP_AGENT')")
    @Operation(hidden = true)
    public Response listCurationProposals(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        ExpressionExperiment ee = datasetArgService.getEntity( datasetArg );
        List<AgentProposal> proposals = agentProposalService.findByInvestigation( ee );
        List<CurationProposalResponse> rows = new ArrayList<>( proposals.size() );
        for ( AgentProposal p : proposals ) {
            rows.add( toProposalResponse( p, ee.getId() ) );
        }
        return Response.ok( rows ).build();
    }

    private static CurationProposalResponse toProposalResponse( AgentProposal p, Long datasetId ) {
        CurationProposalResponse r = new CurationProposalResponse();
        r.proposalId = p.getId();
        r.datasetId = datasetId;
        r.runId = p.getRunId();
        r.agentVersion = p.getAgentVersion();
        r.model = p.getModel();
        r.ranAt = p.getRanAt();
        r.payloadJson = p.getPayloadJson();
        return r;
    }
}
