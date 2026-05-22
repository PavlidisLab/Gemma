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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.DatasetArg;
import ubic.gemma.rest.util.args.DatasetArgService;

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
 *   <li>{@code GET  /candidates} - screening queue (datasets needing curation attention).
 *       Implemented as a 302 redirect to {@code /datasets?filter=needsAttention=true} since the underlying
 *       query is already supported by the existing dataset filter.</li>
 *   <li>{@code POST /datasets/{id}/audits} - curator manual audit submission (creates a
 *       {@link CurationNoteUpdateEvent} audit event with the supplied note + detail).</li>
 *   <li>{@code POST /datasets/{id}/curation-proposals} - <b>recce-only</b>. Returns 501 Not Implemented.
 *       Requires a new {@code CurationProposal} entity that does not yet exist in the gemd schema.</li>
 *   <li>{@code GET  /datasets/{id}/curation-proposals} - <b>recce-only</b>. Returns 501 Not Implemented.</li>
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
     * Recce stub for {@code POST /datasets/{id}/curation-proposals}. Returns 501 Not Implemented.
     * <p>
     * <b>Recce note</b>: a real implementation needs a new {@code CurationProposal} entity (with fields like
     * {@code experimentId}, {@code proposerId}, {@code proposalText}, {@code status},
     * {@code createdAt}, {@code reviewedBy}, {@code reviewedAt}), a Hibernate mapping, a Flyway migration, a
     * {@code CurationProposalService} + DAO, and a value object. None of those exist today. The closest existing
     * concept is {@link ubic.gemma.model.common.auditAndSecurity.curation.Ticket}, which models curator-workflow
     * tickets but is shaped around troubled/needs-attention state machines rather than free-text AI-generated
     * proposals. Decision needed: extend Ticket, add a sibling CurationProposal entity, or keep proposals in the
     * gemma-curation-agents FastAPI service (per GEMMA_UI_FEATURE_CATALOG.md B12 dubious bits).
     */
    @POST
    @Hidden
    @Path("/datasets/{dataset}/curation-proposals")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true)
    public Response submitCurationProposal(
            @PathParam("dataset") DatasetArg<?> datasetArg,
            @Nullable Object body
    ) {
        return curationProposalsNotImplemented();
    }

    /**
     * Recce stub for {@code GET /datasets/{id}/curation-proposals}. Returns 501 Not Implemented.
     * See {@link #submitCurationProposal} for the recce note.
     */
    @GET
    @Hidden
    @Path("/datasets/{dataset}/curation-proposals")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(hidden = true)
    public Response listCurationProposals(
            @PathParam("dataset") DatasetArg<?> datasetArg
    ) {
        return curationProposalsNotImplemented();
    }

    private Response curationProposalsNotImplemented() {
        return Response.status( Response.Status.NOT_IMPLEMENTED )
                .type( MediaType.APPLICATION_JSON )
                .entity( "{\"error\":\"curation-proposals require a new CurationProposal entity that does not exist yet; see CurationWebService javadoc for the recce.\"}" )
                .build();
    }
}
